package com.cavmkii.goblincraft.entity;

import com.cavmkii.goblincraft.GoblinCraft;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<Joe> JOE = register("joe",
            EntityType.Builder.<Joe>of(Joe::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10));

    public static final EntityType<Sean> SEAN = register("sean",
            EntityType.Builder.<Sean>of(Sean::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10));

    public static final EntityType<BabyGoblin> BABY_COMMANDO = register("baby_commando",
            EntityType.Builder.<BabyGoblin>of(BabyGoblin::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(8));

    public static final EntityType<BabyGoblin> BABY_GRENADIER = register("baby_grenadier",
            EntityType.Builder.<BabyGoblin>of(BabyGoblin::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(8));

    public static final EntityType<BabyGoblin> BABY_BERSERKER = register("baby_berserker",
            EntityType.Builder.<BabyGoblin>of(BabyGoblin::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(8));

    public static final EntityType<SeanSecurity> SEAN_SECURITY = register("sean_security",
            EntityType.Builder.<SeanSecurity>of(SeanSecurity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(8));

    public static final EntityType<SpellProjectile> SPELL = register("spell",
            EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(8).updateInterval(2));

    public static final EntityType<BabyBottle> BABY_BOTTLE = register("baby_bottle",
            EntityType.Builder.<BabyBottle>of(BabyBottle::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(10));

    private ModEntities() {}

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, GoblinCraft.id(name), builder.build(GoblinCraft.id(name).toString()));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(JOE, Joe.createAttributes());
        FabricDefaultAttributeRegistry.register(SEAN, Sean.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_COMMANDO, BabyGoblin.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_GRENADIER, BabyGoblin.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_BERSERKER, BabyGoblin.createAttributes());
        FabricDefaultAttributeRegistry.register(SEAN_SECURITY, SeanSecurity.createAttributes());
    }

    public static boolean isBaby(EntityType<?> type) {
        return type == BABY_COMMANDO || type == BABY_GRENADIER || type == BABY_BERSERKER;
    }
}
