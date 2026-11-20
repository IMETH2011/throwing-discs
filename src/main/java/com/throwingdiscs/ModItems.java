package com.throwingdiscs;

import com.throwingdiscs.item.ThrowingDiscItem;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModItems {
	public static final Map<DiscTier, Item> DISCS = new EnumMap<>(DiscTier.class);

	private ModItems() {}

	public static void register() {
		for (DiscTier tier : DiscTier.values()) {
			RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, ThrowingDiscsMod.id(tier.id));
			Item.Settings settings = new Item.Settings()
					.registryKey(key)
					.maxDamage(tier.durability)
					.enchantable(tier.enchantability)
					.repairable(tier.repairItem.get());
			if (tier == DiscTier.NETHERITE) {
				settings = settings.fireproof();
			}
			DISCS.put(tier, Registry.register(Registries.ITEM, key, new ThrowingDiscItem(settings, tier)));
		}

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			for (Item disc : DISCS.values()) {
				entries.add(disc);
			}
		});
	}
}
