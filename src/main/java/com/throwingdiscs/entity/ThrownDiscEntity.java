package com.throwingdiscs.entity;

import com.throwingdiscs.DiscTier;
import com.throwingdiscs.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The flying disc.
 *
 * Flight: goes straight (no gravity), pierces every living entity on its path, ricochets off
 * blocks (each bounce = faster + longer flight), then after a while turns around and homes back to
 * the thrower like a boomerang (damaging again on the way back). Returning discs pass through blocks.
 */
public class ThrownDiscEntity extends ProjectileEntity implements FlyingItemEntity {
	private static final TrackedData<Integer> TIER =
			DataTracker.registerData(ThrownDiscEntity.class, TrackedDataHandlerRegistry.INTEGER);

	// ---- tweak these to taste ----
	private static final float START_SPEED = 1.4f;       // blocks per tick
	private static final float MAX_SPEED = 2.6f;
	private static final int BASE_OUTWARD_TICKS = 12;    // ticks flying away before turning back
	private static final int RICOCHET_EXTRA_TICKS = 6;   // extra outward time per ricochet
	private static final float RICOCHET_SPEED_MULT = 1.12f;
	private static final int MAX_RICOCHETS = 8;
	private static final int RICOCHET_BONUS_THRESHOLD = 3; // bounces needed for the bonus
	private static final float RICOCHET_BONUS_DAMAGE = 2.5f;
	private static final int REHIT_DELAY_TICKS = 8;
	private static final int MAX_LIFETIME = 200;

	private final World discWorld;

	private ItemStack discStack = ItemStack.EMPTY;
	private Hand hand = Hand.MAIN_HAND;
	private boolean creative;
	private boolean returning;
	private boolean finished;
	private int flightTicks;
	private int outwardTicks = BASE_OUTWARD_TICKS;
	private int ricochets;
	private float speed = START_SPEED;
	private final Map<Integer, Integer> lastHit = new HashMap<>();

	public ThrownDiscEntity(EntityType<? extends ThrownDiscEntity> type, World world) {
		super(type, world);
		this.discWorld = world;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(TIER, 0);
	}

	public void setup(LivingEntity owner, ItemStack stack, DiscTier tier, Hand hand, boolean creative) {
		this.discStack = stack;
		this.hand = hand;
		this.creative = creative;
		this.dataTracker.set(TIER, tier.ordinal());
		this.setOwner(owner);

		Vec3d look = owner.getRotationVec(1.0f);
		Vec3d pos = owner.getEyePos().add(look.multiply(0.5)).subtract(0.0, 0.25, 0.0);
		this.setPosition(pos.x, pos.y, pos.z);
		this.setVelocity(look.multiply(speed));
	}

	public DiscTier getTier() {
		return DiscTier.byIndex(this.dataTracker.get(TIER));
	}

	/** Used by the vanilla item-style renderer on both client and server. */
	@Override
	public ItemStack getStack() {
		if (!discStack.isEmpty()) {
			return discStack;
		}
		return new ItemStack(ModItems.DISCS.get(getTier()));
	}

	// ------------------------------------------------------------------ tick

	@Override
	public void tick() {
		super.tick();
		this.age++; // drives the spin animation on the client

		if (!(discWorld instanceof ServerWorld world)) {
			return; // client just follows server position updates
		}
		if (finished) {
			return;
		}
		if (discStack.isEmpty() && !creative) {
			discard(); // e.g. reloaded from disk; nothing to return
			return;
		}

		flightTicks++;
		Entity owner = getOwner();
		LivingEntity livingOwner = owner instanceof LivingEntity l ? l : null;

		if (!returning && (flightTicks >= outwardTicks || ricochets >= MAX_RICOCHETS)) {
			returning = true;
			lastHit.clear(); // can hit everything again on the way back
		}
		if (flightTicks > MAX_LIFETIME) {
			finish(world, owner);
			return;
		}

		Vec3d start = new Vec3d(getX(), getY(), getZ());
		Vec3d vel = getVelocity();

		if (returning) {
			if (owner == null || owner.isRemoved() || !owner.isAlive()) {
				dropAndDiscard(world);
				return;
			}
			Vec3d target = new Vec3d(owner.getX(), owner.getBodyY(0.6), owner.getZ());
			Vec3d toTarget = target.subtract(start);
			double dist = toTarget.length();
			double step = speed * 1.1;
			if (dist <= Math.max(1.3, step)) {
				finish(world, owner);
				return;
			}
			vel = toTarget.normalize().multiply(step);
		}

		Vec3d end = start.add(vel);
		BlockHitResult blockHit = null;
		if (!returning) {
			BlockHitResult r = world.raycast(new RaycastContext(start, end,
					RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
			if (r.getType() != HitResult.Type.MISS) {
				blockHit = r;
				end = r.getPos();
			}
		}

		hitEntities(world, owner, livingOwner, start, end, vel);

		if (discStack.isEmpty() && !creative) { // the disc broke
			world.playSound(null, getX(), getY(), getZ(), DiscTier.sound("entity.item.break"),
					SoundCategory.PLAYERS, 0.8f, 1.0f);
			discard();
			return;
		}

		if (blockHit != null) {
			ricochet(world, blockHit, vel);
		} else {
			setVelocity(vel);
			setPosition(end.x, end.y, end.z);
		}

		if (flightTicks % 5 == 0) {
			world.playSound(null, getX(), getY(), getZ(), DiscTier.sound("entity.player.attack.sweep"),
					SoundCategory.PLAYERS, 0.35f, 1.7f);
		}
	}

	// ------------------------------------------------------------------ hits

	private void hitEntities(ServerWorld world, Entity owner, LivingEntity livingOwner,
			Vec3d start, Vec3d end, Vec3d vel) {
		Box sweep = new Box(start, end).expand(0.6);
		for (Entity e : world.getOtherEntities(this, sweep,
				c -> c instanceof LivingEntity && c.isAlive() && !c.isSpectator() && c != owner)) {
			Box hitBox = e.getBoundingBox().expand(0.3);
			Optional<Vec3d> hit = hitBox.raycast(start, end);
			if (hit.isEmpty() && !hitBox.contains(start)) {
				continue;
			}
			Integer last = lastHit.get(e.getId());
			if (last != null && flightTicks - last < REHIT_DELAY_TICKS) {
				continue;
			}
			lastHit.put(e.getId(), flightTicks);
			damageTarget(world, owner, livingOwner, (LivingEntity) e, vel);
			if (discStack.isEmpty() && !creative) {
				return;
			}
		}
	}

	private void damageTarget(ServerWorld world, Entity owner, LivingEntity livingOwner,
			LivingEntity target, Vec3d vel) {
		DamageSource source = world.getDamageSources().thrown(this, owner);
		boolean hurt = target.damage(world, source, computeDamage());
		if (!hurt) {
			return;
		}

		int fire = level(Enchantments.FIRE_ASPECT);
		if (fire > 0) {
			target.setOnFireFor(4 * fire);
		}
		int knockback = level(Enchantments.KNOCKBACK);
		if (knockback > 0) {
			target.takeKnockback(0.5 * knockback, -vel.x, -vel.z);
		}

		world.playSound(null, target.getX(), target.getY(), target.getZ(), getTier().hitSound(),
				SoundCategory.PLAYERS, 1.0f, 1.0f);

		if (!creative && livingOwner != null) {
			EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
			discStack.damage(1, livingOwner, slot); // Unbreaking is handled in here
		}
	}

	/** Same numbers as the sword of that material (+ Sharpness), +2.5 after enough ricochets. */
	private float computeDamage() {
		float dmg = getTier().damage;
		int sharp = level(Enchantments.SHARPNESS);
		if (sharp > 0) {
			dmg += 0.5f * sharp + 0.5f;
		}
		if (ricochets >= RICOCHET_BONUS_THRESHOLD) {
			dmg += RICOCHET_BONUS_DAMAGE;
		}
		return dmg;
	}

	private int level(RegistryKey<Enchantment> key) {
		ItemEnchantmentsComponent c =
				discStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		for (RegistryEntry<Enchantment> e : c.getEnchantments()) {
			if (e.matchesKey(key)) {
				return c.getLevel(e);
			}
		}
		return 0;
	}

	private void ricochet(ServerWorld world, BlockHitResult hit, Vec3d vel) {
		Direction side = hit.getSide();
		Vec3d normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
		Vec3d reflected = vel.subtract(normal.multiply(2.0 * vel.dotProduct(normal)));

		ricochets++;
		outwardTicks += RICOCHET_EXTRA_TICKS;
		speed = Math.min(speed * RICOCHET_SPEED_MULT, MAX_SPEED);

		if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.DECORATED_POT)) {
			world.breakBlock(hit.getBlockPos(), true, getOwner());
		}

		Vec3d p = hit.getPos().add(normal.multiply(0.1));
		setPosition(p.x, p.y, p.z);
		setVelocity(reflected.normalize().multiply(speed));

		world.playSound(null, p.x, p.y, p.z, getTier().hitSound(), SoundCategory.PLAYERS, 0.9f,
				0.9f + 0.1f * Math.min(ricochets, 5));
	}

	// ------------------------------------------------------------------ ending

	/** Give the disc back (offhand throws return to the offhand) and remove the entity. */
	private void finish(ServerWorld world, Entity owner) {
		if (finished) {
			return;
		}
		finished = true;
		if (!creative && !discStack.isEmpty()) {
			ItemStack back = discStack;
			if (owner instanceof PlayerEntity player && player.isAlive()) {
				if (player.getStackInHand(hand).isEmpty()) {
					player.setStackInHand(hand, back);
				} else if (!player.getInventory().insertStack(back)) {
					this.dropStack(world, back);
				}
			} else {
				this.dropStack(world, back);
			}
		}
		world.playSound(null, getX(), getY(), getZ(), getTier().hitSound(), SoundCategory.PLAYERS, 0.7f, 1.4f);
		discard();
	}

	private void dropAndDiscard(ServerWorld world) {
		if (finished) {
			return;
		}
		finished = true;
		if (!creative && !discStack.isEmpty()) {
			this.dropStack(world, discStack);
		}
		discard();
	}
}
