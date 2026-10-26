package com.cavmkii.goblincraft.item;

import com.cavmkii.goblincraft.GoblinCraft;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.DyedItemColor;

public final class ModItems {
    /** "Soda": a drink that counts as food. Valheim's +35 health / +150 stamina becomes Health Boost and a full meal. */
    public static final Item SODA = register("soda", new SodaItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.2f)
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 20 * 60 * 20, 1), 1f)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .build())));

    /** "Merch": a black leather tunic with no armor at all. Sean's Security leaves wearers alone. */
    public static final Item MERCH = register("merch", new NoArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1).component(DataComponents.DYED_COLOR, new DyedItemColor(0x151515, false))));

    /** "Glock": a crossbow with no reload. Fires arrows as fast as you click. */
    public static final Item GLOCK = register("glock", new GlockItem(new Item.Properties().durability(465)));

    /** "Restraining Order": carry it and Joe has to keep his distance. */
    public static final Item RESTRAINING_ORDER = register("restraining_order", new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** "Diddy Oil": Speed and Resistance for five minutes. */
    public static final Item DIDDY_OIL = register("diddy_oil", new DiddyOilItem(new Item.Properties().stacksTo(16)));

    /** "67": end-game fists. 13 attack damage (six plus seven), fast swings. */
    public static final Item SIXTY_SEVEN = register("sixty_seven", new SwordItem(Tiers.NETHERITE, new Item.Properties()
            .fireResistant()
            .rarity(Rarity.EPIC)
            .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 8, -1.0f))));

    /** "Baby Bonnet": Joe's gift to anyone who said the phrase. Babies leave wearers alone; Security doesn't. */
    public static final Item BABY_BONNET = register("baby_bonnet", new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET,
            new Item.Properties().stacksTo(1).component(DataComponents.DYED_COLOR, new DyedItemColor(0xF7A8C8, false))));

    /** Items that get a description line in their tooltip (lang key item.goblincraft.&lt;id&gt;.desc). */
    public static final List<Item> DESCRIBED = List.of(SODA, MERCH, GLOCK, RESTRAINING_ORDER, DIDDY_OIL, SIXTY_SEVEN, BABY_BONNET);

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GoblinCraft.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(SODA))
                    .title(Component.translatable("itemGroup.goblincraft"))
                    .displayItems((params, output) -> {
                        DESCRIBED.forEach(output::accept);
                        output.accept(com.cavmkii.goblincraft.rpg.RpgContent.ALTAR_ITEM);
                        output.accept(com.cavmkii.goblincraft.rpg.RpgContent.ORB_OF_REGRET);
                        output.accept(com.cavmkii.goblincraft.tarkov.TarkovContent.HIDEOUT_BENCH_ITEM);
                        output.accept(com.cavmkii.goblincraft.tarkov.TarkovContent.SECURE_BETA);
                        output.accept(com.cavmkii.goblincraft.tarkov.TarkovContent.SECURE_GAMMA);
                        output.accept(com.cavmkii.goblincraft.tarkov.TarkovContent.SECURE_KAPPA);
                        com.cavmkii.goblincraft.tarkov.TarkovContent.MEDS.values().forEach(output::accept);
                    })
                    .build());

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, GoblinCraft.id(name), item);
    }

    public static void init() {
        // Loading the class registers everything.
    }
}
