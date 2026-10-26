package com.cavmkii.seancraft.rpg;

import com.cavmkii.seancraft.SeanCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class RpgContent {
    public static final Block ALTAR_OF_REGRET = Registry.register(BuiltInRegistries.BLOCK, SeanCraft.id("altar_of_regret"),
            new AltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5f, 1200f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(state -> 7).noOcclusion()));

    public static final Item ALTAR_ITEM = Registry.register(BuiltInRegistries.ITEM, SeanCraft.id("altar_of_regret"),
            new BlockItem(ALTAR_OF_REGRET, new Item.Properties()));

    /** Needed to respec above level 40. Joe sometimes drops one; Sean sells them at a steep price. */
    public static final Item ORB_OF_REGRET = Registry.register(BuiltInRegistries.ITEM, SeanCraft.id("orb_of_regret"),
            new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));

    public static final EntityType<SkillProjectile> SKILL_PROJECTILE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            SeanCraft.id("skill_projectile"),
            EntityType.Builder.<SkillProjectile>of(SkillProjectile::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(8).updateInterval(2).build(SeanCraft.id("skill_projectile").toString()));

    private RpgContent() {}

    static void init() {
        // Loading the class registers everything.
    }
}
