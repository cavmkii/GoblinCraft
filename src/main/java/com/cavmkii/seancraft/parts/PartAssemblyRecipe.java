package com.cavmkii.seancraft.parts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Metal weapons and armor are crafted from their parts, in any arrangement:
 * <ul>
 * <li>sword: blade + guard of the same metal, a handle and a wrap</li>
 * <li>axe: axe head, a handle and a wrap</li>
 * <li>crossbow: limbs, a handle and a wrap</li>
 * <li>shield: a guard (rim), a handle and a wrap</li>
 * <li>armor: 1 plate (boots), 2 (helmet), 3 (leggings) or 4 (chestplate) of one metal, padding and a wrap</li>
 * </ul>
 * Copper plates make chainmail. The parts go into the item at their levels; with several of a kind, the best one does.
 */
public class PartAssemblyRecipe extends CustomRecipe {
    public PartAssemblyRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Plan(Item result, Family family, Map<PartSlot, Parts.Installed> parts) {}

    private static Plan plan(CraftingInput input) {
        List<ItemStack> parts = new ArrayList<>();
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof PartItem)) {
                return null;
            }
            parts.add(stack);
        }
        if (parts.size() < 3) {
            return null;
        }
        Map<PartKind, List<ItemStack>> byKind = new EnumMap<>(PartKind.class);
        for (ItemStack stack : parts) {
            byKind.computeIfAbsent(((PartItem) stack.getItem()).def.kind(), k -> new ArrayList<>()).add(stack);
        }
        int handles = count(byKind, PartKind.HANDLE);
        int wraps = count(byKind, PartKind.WRAP);
        int guards = count(byKind, PartKind.GUARD);
        int paddings = count(byKind, PartKind.PADDING);
        int plates = count(byKind, PartKind.PLATE);
        int heads = count(byKind, PartKind.BLADE) + count(byKind, PartKind.AXE_HEAD) + count(byKind, PartKind.LIMBS);

        if (plates > 0) {
            if (plates > 4 || paddings != 1 || wraps != 1 || handles + guards + heads > 0 || !sameTier(byKind.get(PartKind.PLATE))) {
                return null;
            }
            Family family = new Family[] {Family.BOOTS, Family.HELMET, Family.LEGS, Family.CHEST}[plates - 1];
            int tier = tier(byKind.get(PartKind.PLATE).get(0));
            return build(Family.itemFor(family, tier), family, byKind);
        }
        if (paddings > 0 || handles != 1 || wraps != 1) {
            return null;
        }
        if (heads == 0 && guards == 1) {
            return build(Items.SHIELD, Family.SHIELD, byKind);
        }
        if (heads != 1) {
            return null;
        }
        if (count(byKind, PartKind.BLADE) == 1 && guards == 1) {
            int tier = tier(byKind.get(PartKind.BLADE).get(0));
            if (tier != tier(byKind.get(PartKind.GUARD).get(0))) {
                return null;
            }
            return build(Family.itemFor(Family.SWORD, tier), Family.SWORD, byKind);
        }
        if (count(byKind, PartKind.AXE_HEAD) == 1 && guards == 0) {
            return build(Family.itemFor(Family.AXE, tier(byKind.get(PartKind.AXE_HEAD).get(0))), Family.AXE, byKind);
        }
        if (count(byKind, PartKind.LIMBS) == 1 && guards == 0) {
            return build(Items.CROSSBOW, Family.CROSSBOW, byKind);
        }
        return null;
    }

    private static Plan build(Item result, Family family, Map<PartKind, List<ItemStack>> byKind) {
        if (result == null) {
            return null; // e.g. a copper blade: no copper sword to make
        }
        Map<PartSlot, Parts.Installed> installed = new EnumMap<>(PartSlot.class);
        byKind.forEach((kind, stacks) -> {
            ItemStack best = stacks.get(0);
            for (ItemStack s : stacks) {
                if (Parts.level(s) > Parts.level(best)) {
                    best = s;
                }
            }
            installed.put(kind.slot, new Parts.Installed(((PartItem) best.getItem()).def, Parts.level(best)));
        });
        return new Plan(result, family, installed);
    }

    private static int count(Map<PartKind, List<ItemStack>> byKind, PartKind kind) {
        List<ItemStack> list = byKind.get(kind);
        return list == null ? 0 : list.size();
    }

    private static int tier(ItemStack stack) {
        return ((PartItem) stack.getItem()).def.tier();
    }

    private static boolean sameTier(List<ItemStack> stacks) {
        int t = tier(stacks.get(0));
        for (ItemStack s : stacks) {
            if (tier(s) != t) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return plan(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Plan plan = plan(input);
        if (plan == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(plan.result());
        Parts.setInstalled(result, plan.parts());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return PartItems.ASSEMBLY;
    }
}
