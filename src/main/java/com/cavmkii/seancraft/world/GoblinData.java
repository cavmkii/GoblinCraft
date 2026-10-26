package com.cavmkii.seancraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world state saved with the overworld: which Joe and Sean are the real ones, respawn timers,
 * what Joe has stolen, and any war-camp blocks that still need removing. Times are overworld game
 * time in ticks.
 */
public final class GoblinData extends SavedData {
    private static final String NAME = "seancraft";

    public final NpcSlot joe = new NpcSlot();
    public final NpcSlot sean = new NpcSlot();

    /** Everything Joe has pickpocketed; dropped when he dies. Kept here so it survives him being replaced. */
    public final List<ItemStack> stash = new ArrayList<>();

    /** War-camp blocks placed by the Baby Wars, removed when the war ends or on the next start-up. */
    public final List<BlockPos> campBlocks = new ArrayList<>();

    public static final class NpcSlot {
        public UUID uuid;
        public long respawnAt;
        public long lastSeen;
        public BlockPos lastPos;
        /** When a present NPC wanders off (0 = not scheduled yet). */
        public long leaveAt;

        void save(CompoundTag tag) {
            if (uuid != null) {
                tag.putUUID("uuid", uuid);
            }
            tag.putLong("respawnAt", respawnAt);
            tag.putLong("lastSeen", lastSeen);
            tag.putLong("leaveAt", leaveAt);
            if (lastPos != null) {
                tag.putLong("lastPos", lastPos.asLong());
            }
        }

        void load(CompoundTag tag) {
            uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : null;
            respawnAt = tag.getLong("respawnAt");
            lastSeen = tag.getLong("lastSeen");
            leaveAt = tag.getLong("leaveAt");
            lastPos = tag.contains("lastPos") ? BlockPos.of(tag.getLong("lastPos")) : null;
        }
    }

    public static GoblinData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GoblinData::new, GoblinData::load, null), NAME);
    }

    private static GoblinData load(CompoundTag tag, HolderLookup.Provider registries) {
        GoblinData data = new GoblinData();
        data.joe.load(tag.getCompound("joe"));
        data.sean.load(tag.getCompound("sean"));
        for (Tag item : tag.getList("stash", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, item).ifPresent(data.stash::add);
        }
        for (Tag pos : tag.getList("campBlocks", Tag.TAG_LONG)) {
            data.campBlocks.add(BlockPos.of(((LongTag) pos).getAsLong()));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag joeTag = new CompoundTag();
        joe.save(joeTag);
        tag.put("joe", joeTag);
        CompoundTag seanTag = new CompoundTag();
        sean.save(seanTag);
        tag.put("sean", seanTag);

        ListTag stashTag = new ListTag();
        for (ItemStack stack : stash) {
            if (!stack.isEmpty()) {
                stashTag.add(stack.save(registries));
            }
        }
        tag.put("stash", stashTag);

        ListTag campTag = new ListTag();
        for (BlockPos pos : campBlocks) {
            campTag.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("campBlocks", campTag);
        return tag;
    }
}
