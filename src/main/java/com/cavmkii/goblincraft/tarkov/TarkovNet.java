package com.cavmkii.goblincraft.tarkov;

import com.cavmkii.goblincraft.GoblinCraft;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class TarkovNet {
    private TarkovNet() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(GoblinCraft.id(name));
    }

    /** Server to client once a second: 0 unknown, 1 hideout, 2 raid; raid time, threat, seconds to next threat (-1 none). */
    public record RaidState(int state, int raidSeconds, int threat, int nextThreat) implements CustomPacketPayload {
        public static final Type<RaidState> TYPE = payloadType("raid_state");
        public static final StreamCodec<FriendlyByteBuf, RaidState> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.state);
            buf.writeVarInt(p.raidSeconds);
            buf.writeVarInt(p.threat);
            buf.writeVarInt(p.nextThreat + 1);
        }, buf -> new RaidState(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt() - 1));
        @Override public Type<RaidState> type() { return TYPE; }
    }

    public record CostLine(String label, String icon, int have, int need) {
        void write(FriendlyByteBuf buf) {
            buf.writeUtf(label);
            buf.writeUtf(icon);
            buf.writeVarInt(have);
            buf.writeVarInt(need);
        }

        static CostLine read(FriendlyByteBuf buf) {
            return new CostLine(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt());
        }
    }

    /** One row on the Hideout screen: a module's next level or a recipe. */
    public record Entry(int kind, String id, String title, String subtitle, String icon, int status, String note,
            List<CostLine> costs, int progress) {
        public static final int MODULE = 0;
        public static final int RECIPE = 1;
        public static final int READY = 0;
        public static final int LOCKED = 1;
        public static final int MAXED = 2;
        public static final int BUILDING = 3;
        public static final int SHORT = 4;
        public static final int BUSY = 5;

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(kind);
            buf.writeUtf(id);
            buf.writeUtf(title);
            buf.writeUtf(subtitle);
            buf.writeUtf(icon);
            buf.writeVarInt(status);
            buf.writeUtf(note);
            buf.writeVarInt(costs.size());
            costs.forEach(c -> c.write(buf));
            buf.writeVarInt(progress);
        }

        static Entry read(FriendlyByteBuf buf) {
            int kind = buf.readVarInt();
            String id = buf.readUtf();
            String title = buf.readUtf();
            String subtitle = buf.readUtf();
            String icon = buf.readUtf();
            int status = buf.readVarInt();
            String note = buf.readUtf();
            int n = buf.readVarInt();
            List<CostLine> costs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                costs.add(CostLine.read(buf));
            }
            return new Entry(kind, id, title, subtitle, icon, status, note, costs, buf.readVarInt());
        }
    }

    /** Server to client: open or refresh the Hideout screen. */
    public record HideoutView(BlockPos pos, int radius, String summary, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<HideoutView> TYPE = payloadType("hideout_view");
        public static final StreamCodec<FriendlyByteBuf, HideoutView> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeBlockPos(p.pos);
            buf.writeVarInt(p.radius);
            buf.writeUtf(p.summary);
            buf.writeVarInt(p.entries.size());
            p.entries.forEach(e -> e.write(buf));
        }, buf -> {
            BlockPos pos = buf.readBlockPos();
            int radius = buf.readVarInt();
            String summary = buf.readUtf();
            int n = buf.readVarInt();
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                entries.add(Entry.read(buf));
            }
            return new HideoutView(pos, radius, summary, entries);
        });
        @Override public Type<HideoutView> type() { return TYPE; }
    }

    /** Client to server: build a module or craft a recipe at a bench (kind -1 just refreshes). */
    public record HideoutAction(BlockPos pos, int kind, String id) implements CustomPacketPayload {
        public static final Type<HideoutAction> TYPE = payloadType("hideout_action");
        public static final StreamCodec<FriendlyByteBuf, HideoutAction> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeBlockPos(p.pos);
            buf.writeVarInt(p.kind + 1);
            buf.writeUtf(p.id);
        }, buf -> new HideoutAction(buf.readBlockPos(), buf.readVarInt() - 1, buf.readUtf()));
        @Override public Type<HideoutAction> type() { return TYPE; }
    }

    /** Client to server: open my secure container. */
    public record OpenSecure() implements CustomPacketPayload {
        public static final Type<OpenSecure> TYPE = payloadType("open_secure");
        public static final StreamCodec<FriendlyByteBuf, OpenSecure> CODEC = StreamCodec.of((buf, p) -> { }, buf -> new OpenSecure());
        @Override public Type<OpenSecure> type() { return TYPE; }
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(RaidState.TYPE, RaidState.CODEC);
        PayloadTypeRegistry.playS2C().register(HideoutView.TYPE, HideoutView.CODEC);
        PayloadTypeRegistry.playC2S().register(HideoutAction.TYPE, HideoutAction.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenSecure.TYPE, OpenSecure.CODEC);
    }
}
