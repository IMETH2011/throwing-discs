package com.throwingdiscs.item;

import com.throwingdiscs.DiscTier;
import com.throwingdiscs.ModEntities;
import com.throwingdiscs.entity.ThrownDiscEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ThrowingDiscItem extends Item {
	private final DiscTier tier;

	public ThrowingDiscItem(Settings settings, DiscTier tier) {
		super(settings);
		this.tier = tier;
	}

	public DiscTier getTier() {
		return tier;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (world instanceof ServerWorld serverWorld) {
			boolean creative = user.getAbilities().creativeMode;
			ThrownDiscEntity disc = new ThrownDiscEntity(ModEntities.THROWN_DISC, serverWorld);
			disc.setup(user, stack.copyWithCount(1), tier, hand, creative);
			serverWorld.spawnEntity(disc);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), tier.throwSound(),
					SoundCategory.PLAYERS, 1.0f, 1.0f);
			if (!creative) {
				stack.decrement(1);
			}
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		return ActionResult.SUCCESS;
	}
}
