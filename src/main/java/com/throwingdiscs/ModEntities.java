package com.throwingdiscs;

import com.throwingdiscs.entity.ThrownDiscEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModEntities {
	public static final RegistryKey<EntityType<?>> THROWN_DISC_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, ThrowingDiscsMod.id("thrown_disc"));

	public static final EntityType<ThrownDiscEntity> THROWN_DISC = Registry.register(
			Registries.ENTITY_TYPE,
			THROWN_DISC_KEY,
			EntityType.Builder.<ThrownDiscEntity>create(ThrownDiscEntity::new, SpawnGroup.MISC)
					.dimensions(0.6f, 0.2f)
					.maxTrackingRange(8)
					.trackingTickInterval(1)
					.build(THROWN_DISC_KEY));

	private ModEntities() {}

	public static void register() {
		// touching this class runs the static initializer above
	}
}
