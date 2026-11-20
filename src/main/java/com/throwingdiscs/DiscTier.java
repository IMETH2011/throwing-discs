package com.throwingdiscs;

import java.util.function.Supplier;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * One entry per disc. Damage / durability / enchantability match the sword of the same material.
 * Sounds are vanilla sound ids, so no extra .ogg files are needed.
 */
public enum DiscTier {
	COPPER("copper_throwing_disc", 5.0f, 190, 13, () -> Items.COPPER_INGOT,
			"block.copper.hit", "block.copper.break"),
	IRON("iron_throwing_disc", 6.0f, 250, 14, () -> Items.IRON_INGOT,
			"item.armor.equip_iron", "item.shield.block"),
	GOLD("golden_throwing_disc", 4.0f, 32, 22, () -> Items.GOLD_INGOT,
			"item.armor.equip_gold", "block.note_block.chime"),
	DIAMOND("diamond_throwing_disc", 7.0f, 1561, 10, () -> Items.DIAMOND,
			"item.armor.equip_diamond", "block.amethyst_block.chime"),
	// netherite = brick-like sound
	NETHERITE("netherite_throwing_disc", 8.0f, 2031, 15, () -> Items.NETHERITE_INGOT,
			"block.nether_bricks.break", "block.nether_bricks.hit");

	public final String id;
	public final float damage;
	public final int durability;
	public final int enchantability;
	public final Supplier<Item> repairItem;
	private final String throwSoundId;
	private final String hitSoundId;

	DiscTier(String id, float damage, int durability, int enchantability, Supplier<Item> repairItem,
			String throwSoundId, String hitSoundId) {
		this.id = id;
		this.damage = damage;
		this.durability = durability;
		this.enchantability = enchantability;
		this.repairItem = repairItem;
		this.throwSoundId = throwSoundId;
		this.hitSoundId = hitSoundId;
	}

	public SoundEvent throwSound() {
		return sound(throwSoundId);
	}

	public SoundEvent hitSound() {
		return sound(hitSoundId);
	}

	public static SoundEvent sound(String vanillaPath) {
		SoundEvent e = Registries.SOUND_EVENT.get(Identifier.ofVanilla(vanillaPath));
		return e != null ? e : SoundEvents.ENTITY_ARROW_SHOOT;
	}

	public static DiscTier byIndex(int i) {
		DiscTier[] v = values();
		return v[Math.floorMod(i, v.length)];
	}
}
