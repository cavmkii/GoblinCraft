package com.cavmkii.goblincraft.tarkov;

import com.cavmkii.goblincraft.tetris.Tetris;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * The hideout stash: every chest and barrel within a hideout's radius counts as one pool. Hideout crafting
 * and module building draw from what you carry first, then the stash.
 */
public final class Stash {
    private Stash() {}

    public static List<Container> containers(ServerLevel level, Hideouts.Hideout hideout) {
        List<Container> out = new ArrayList<>();
        double r = hideout.radius();
        int minCx = (int) Math.floor((hideout.pos.getX() - r) / 16);
        int maxCx = (int) Math.floor((hideout.pos.getX() + r) / 16);
        int minCz = (int) Math.floor((hideout.pos.getZ() - r) / 16);
        int maxCz = (int) Math.floor((hideout.pos.getZ() + r) / 16);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if ((be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) && be instanceof Container container
                            && hideout.contains(Vec3.atCenterOf(be.getBlockPos()))) {
                        out.add(container);
                    }
                }
            }
        }
        return out;
    }

    /** How many of a cost the player carries plus what's in the stash (if there is one). */
    public static int count(ServerPlayer player, Hideouts.Hideout hideout, Cost cost) {
        int n = 0;
        for (ItemStack stack : Tetris.carried(player)) {
            if (cost.matches(stack)) {
                n += stack.getCount();
            }
        }
        if (hideout != null) {
            for (Container c : containers(player.serverLevel(), hideout)) {
                n += count(c, cost);
            }
        }
        return n;
    }

    public static boolean canAfford(ServerPlayer player, Hideouts.Hideout hideout, List<Cost> costs) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (Cost cost : costs) {
            if (count(player, hideout, cost) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    /** Takes the costs from the player first, then the stash. Call {@link #canAfford} first. */
    public static void consume(ServerPlayer player, Hideouts.Hideout hideout, List<Cost> costs) {
        if (player.getAbilities().instabuild) {
            return;
        }
        for (Cost cost : costs) {
            int left = cost.count();
            for (ItemStack stack : Tetris.carried(player)) {
                if (left <= 0) {
                    break;
                }
                if (cost.matches(stack)) {
                    int n = Math.min(left, stack.getCount());
                    stack.shrink(n);
                    left -= n;
                }
            }
            if (left > 0 && hideout != null) {
                for (Container c : containers(player.serverLevel(), hideout)) {
                    left = take(c, cost, left);
                    if (left <= 0) {
                        break;
                    }
                }
            }
        }
    }

    private static int count(Container container, Cost cost) {
        int n = 0;
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (cost.matches(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static int take(Container container, Cost cost, int amount) {
        int size = container.getContainerSize();
        for (int i = 0; i < size && amount > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (cost.matches(stack)) {
                int n = Math.min(amount, stack.getCount());
                stack.shrink(n);
                amount -= n;
                container.setChanged();
            }
        }
        return amount;
    }
}
