package com.cavmkii.seancraft.tarkov;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Chunks holding a FOB table, per dimension (2.6). One table per chunk. */
public final class Fobs extends SavedData {
    private static final String NAME = "seancraft_fobs";

    private final LongSet chunks = new LongOpenHashSet();

    public static Fobs get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Fobs::new, Fobs::load, null), NAME);
    }

    public boolean has(ChunkPos chunk) {
        return chunks.contains(chunk.toLong());
    }

    void add(ChunkPos chunk) {
        if (chunks.add(chunk.toLong())) {
            setDirty();
        }
    }

    void remove(ChunkPos chunk) {
        if (chunks.remove(chunk.toLong())) {
            setDirty();
        }
    }

    private static Fobs load(CompoundTag tag, HolderLookup.Provider registries) {
        Fobs data = new Fobs();
        for (long l : tag.getLongArray("chunks")) {
            data.chunks.add(l);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("chunks", new LongArrayTag(chunks.toLongArray()));
        return tag;
    }
}
