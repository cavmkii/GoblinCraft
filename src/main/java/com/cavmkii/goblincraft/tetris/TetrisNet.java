package com.cavmkii.goblincraft.tetris;

import com.cavmkii.goblincraft.GoblinCraft;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public final class TetrisNet {
    private TetrisNet() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(GoblinCraft.id(name));
    }

    private static void writeList(RegistryFriendlyByteBuf buf, List<Placed> list) {
        buf.writeVarInt(list.size());
        list.forEach(p -> p.write(buf));
    }

    private static List<Placed> readList(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Placed> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(Placed.read(buf));
        }
        return list;
    }

    /** One container shown on the right of GEAR: an opened chest, or one stash chest. */
    public record ContainerView(BlockPos pos, String name, List<ItemStack> stacks) {
        void write(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(name);
            buf.writeVarInt(stacks.size());
            stacks.forEach(s -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s));
        }

        static ContainerView read(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            String name = buf.readUtf();
            int n = buf.readVarInt();
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
            return new ContainerView(pos, name, stacks);
        }
    }

    /** Server to client: the whole Tetris inventory and whatever container or stash is in reach. */
    public record Sync(ItemStack rig, ItemStack backpack, List<Placed> pockets, List<Placed> rigItems, List<Placed> backpackItems,
            List<Placed> pouch, int pouchW, int pouchH, List<ItemStack> unsorted, boolean stash, List<ContainerView> containers)
            implements CustomPacketPayload {
        public static final Type<Sync> TYPE = payloadType("inv_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of((buf, p) -> {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.rig);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.backpack);
            writeList(buf, p.pockets);
            writeList(buf, p.rigItems);
            writeList(buf, p.backpackItems);
            writeList(buf, p.pouch);
            buf.writeVarInt(p.pouchW);
            buf.writeVarInt(p.pouchH);
            buf.writeVarInt(p.unsorted.size());
            p.unsorted.forEach(s -> ItemStack.STREAM_CODEC.encode(buf, s));
            buf.writeBoolean(p.stash);
            buf.writeVarInt(p.containers.size());
            p.containers.forEach(c -> c.write(buf));
        }, buf -> {
            ItemStack rig = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ItemStack backpack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            List<Placed> pockets = readList(buf);
            List<Placed> rigItems = readList(buf);
            List<Placed> backpackItems = readList(buf);
            List<Placed> pouch = readList(buf);
            int pw = buf.readVarInt();
            int ph = buf.readVarInt();
            int n = buf.readVarInt();
            List<ItemStack> unsorted = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                unsorted.add(ItemStack.STREAM_CODEC.decode(buf));
            }
            boolean stash = buf.readBoolean();
            int c = buf.readVarInt();
            List<ContainerView> containers = new ArrayList<>();
            for (int i = 0; i < c; i++) {
                containers.add(ContainerView.read(buf));
            }
            return new Sync(rig, backpack, pockets, rigItems, backpackItems, pouch, pw, ph, unsorted, stash, containers);
        });
        @Override public Type<Sync> type() { return TYPE; }
    }

    /** Server to client: open the Tarkov screen. */
    public record Open(int tab) implements CustomPacketPayload {
        public static final Type<Open> TYPE = payloadType("inv_open");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC =
                StreamCodec.of((buf, p) -> buf.writeVarInt(p.tab), buf -> new Open(buf.readVarInt()));
        @Override public Type<Open> type() { return TYPE; }
    }

    /** Client to server: move (part of) an item. */
    public record Move(Loc from, Loc to, boolean rotated, int count) implements CustomPacketPayload {
        public static final Type<Move> TYPE = payloadType("inv_move");
        public static final StreamCodec<RegistryFriendlyByteBuf, Move> CODEC = StreamCodec.of((buf, p) -> {
            p.from.write(buf);
            p.to.write(buf);
            buf.writeBoolean(p.rotated);
            buf.writeVarInt(p.count);
        }, buf -> new Move(Loc.read(buf), Loc.read(buf), buf.readBoolean(), buf.readVarInt()));
        @Override public Type<Move> type() { return TYPE; }
    }

    /** Client to server: Ctrl+click transfer. */
    public record QuickMove(Loc from) implements CustomPacketPayload {
        public static final Type<QuickMove> TYPE = payloadType("inv_quick");
        public static final StreamCodec<RegistryFriendlyByteBuf, QuickMove> CODEC =
                StreamCodec.of((buf, p) -> p.from.write(buf), buf -> new QuickMove(Loc.read(buf)));
        @Override public Type<QuickMove> type() { return TYPE; }
    }

    /** Client to server: buttons. 0 sort, 1 transfer all to stash, 2 drop all unsorted, 3 close container, 4 screen opened, 5 screen closed. */
    public record Action(int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = payloadType("inv_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
                StreamCodec.of((buf, p) -> buf.writeVarInt(p.action), buf -> new Action(buf.readVarInt()));
        @Override public Type<Action> type() { return TYPE; }
    }

    public record CraftEntry(String id, ItemStack result, int times) {}

    /** Server to client: recipes you can make now from everything you carry (and the stash). */
    public record CraftList(List<CraftEntry> entries) implements CustomPacketPayload {
        public static final Type<CraftList> TYPE = payloadType("craft_list");
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftList> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.entries.size());
            for (CraftEntry e : p.entries) {
                buf.writeUtf(e.id);
                ItemStack.STREAM_CODEC.encode(buf, e.result);
                buf.writeVarInt(e.times);
            }
        }, buf -> {
            int n = buf.readVarInt();
            List<CraftEntry> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(new CraftEntry(buf.readUtf(), ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt()));
            }
            return new CraftList(list);
        });
        @Override public Type<CraftList> type() { return TYPE; }
    }

    /** Client to server: craft a recipe (empty id: just refresh the list). */
    public record Craft(String id, int times) implements CustomPacketPayload {
        public static final Type<Craft> TYPE = payloadType("craft");
        public static final StreamCodec<RegistryFriendlyByteBuf, Craft> CODEC =
                StreamCodec.of((buf, p) -> { buf.writeUtf(p.id); buf.writeVarInt(p.times); }, buf -> new Craft(buf.readUtf(), buf.readVarInt()));
        @Override public Type<Craft> type() { return TYPE; }
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
        PayloadTypeRegistry.playS2C().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.playS2C().register(CraftList.TYPE, CraftList.CODEC);
        PayloadTypeRegistry.playC2S().register(Move.TYPE, Move.CODEC);
        PayloadTypeRegistry.playC2S().register(QuickMove.TYPE, QuickMove.CODEC);
        PayloadTypeRegistry.playC2S().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.playC2S().register(Craft.TYPE, Craft.CODEC);
    }
}
