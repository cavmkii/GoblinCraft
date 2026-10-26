package com.cavmkii.seancraft.world;

import com.cavmkii.seancraft.Cfg;
import com.cavmkii.seancraft.SeanCraft;
import com.cavmkii.seancraft.entity.Joe;
import com.cavmkii.seancraft.entity.ModEntities;
import com.cavmkii.seancraft.entity.Sean;
import com.cavmkii.seancraft.entity.Wander;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side. Keeps exactly one Joe and one Sean in the overworld: remembers which entity is the real
 * one, notices deaths, respawns after the configured delay, and brings them back near players when
 * they've been left alone in some far-off (or unloaded) corner of the world. Also holds the rage state
 * and owns the Baby Wars.
 */
public final class WorldDirector {
    public enum Kind { JOE, SEAN }

    private static WorldDirector instance;

    public final MinecraftServer server;
    public final BabyWar war;

    private long nextTick;

    // Rage: anyone saying a rage phrase makes Joe hostile to all players for a while.
    private long rageUntil;
    private UUID rageTarget;
    private Vec3 rageOrigin = Vec3.ZERO;

    private WorldDirector(MinecraftServer server) {
        this.server = server;
        this.war = new BabyWar(this);
    }

    public static WorldDirector get() {
        return instance;
    }

    public static void start(MinecraftServer server) {
        instance = new WorldDirector(server);
        instance.war.cleanupLeftovers();
    }

    public static void stop() {
        if (instance != null) {
            instance.war.shutdown();
        }
        instance = null;
    }

    public ServerLevel overworld() {
        return server.overworld();
    }

    public GoblinData data() {
        return GoblinData.get(overworld());
    }

    public long now() {
        return overworld().getGameTime();
    }

    private GoblinData.NpcSlot slot(Kind kind) {
        return kind == Kind.JOE ? data().joe : data().sean;
    }

    // ------------------------------------------------------------------ identity

    /**
     * Called by every Joe/Sean as it ticks. True if it's the real one. An untracked copy is adopted
     * only when the slot is empty and not waiting on a respawn timer; any other copy should remove itself.
     */
    public boolean claim(Kind kind, Mob mob) {
        if (mob.level() != overworld()) {
            return false;
        }
        GoblinData.NpcSlot slot = slot(kind);
        if (mob.getUUID().equals(slot.uuid)) {
            return true;
        }
        if (slot.uuid == null && now() >= slot.respawnAt) {
            slot.uuid = mob.getUUID();
            slot.lastSeen = now();
            slot.lastPos = mob.blockPosition();
            data().setDirty();
            return true;
        }
        return false;
    }

    public void onDeath(Kind kind, Mob mob) {
        GoblinData.NpcSlot slot = slot(kind);
        if (!mob.getUUID().equals(slot.uuid)) {
            return;
        }
        double minutes = kind == Kind.JOE ? Cfg.joeRespawnMinutes : Cfg.seanRespawnMinutes;
        slot.uuid = null;
        slot.respawnAt = now() + (long) (minutes * 60 * 20);
        data().setDirty();
        SeanCraft.LOG.info("{} died; respawning in {} minutes.", kind, minutes);
    }

    public Entity findLoaded(Kind kind) {
        UUID uuid = slot(kind).uuid;
        return uuid != null ? overworld().getEntity(uuid) : null;
    }

    public Joe joe() {
        return findLoaded(Kind.JOE) instanceof Joe joe ? joe : null;
    }

    public Sean sean() {
        return findLoaded(Kind.SEAN) instanceof Sean sean ? sean : null;
    }

    public boolean joeAlive() {
        return data().joe.uuid != null;
    }

    public BlockPos seanLastPos() {
        return data().sean.uuid != null ? data().sean.lastPos : null;
    }

    /** Replace Joe or Sean with a fresh one at a spot (admin summon, and the Baby Wars). */
    public Mob replace(Kind kind, BlockPos pos) {
        Entity old = findLoaded(kind);
        if (old != null) {
            old.discard();
        }
        return spawn(kind, pos);
    }

    private Mob spawn(Kind kind, BlockPos pos) {
        ServerLevel level = overworld();
        Mob mob = kind == Kind.JOE ? ModEntities.JOE.create(level) : ModEntities.SEAN.create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        GoblinData.NpcSlot slot = slot(kind);
        slot.uuid = mob.getUUID();
        slot.respawnAt = 0;
        slot.leaveAt = now() + minutes(Cfg.stayMinutesMin, Cfg.stayMinutesMax);
        slot.lastSeen = now();
        slot.lastPos = pos;
        data().setDirty();
        if (mob instanceof Sean sean) {
            sean.applyConfiguredHealth();
        }
        level.addFreshEntity(mob);
        SeanCraft.LOG.info("Spawned {} at {}.", kind, pos.toShortString());
        return mob;
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        long now = now();
        if (now < nextTick && now >= nextTick - 20) {
            war.tick();
            return;
        }
        nextTick = now + 20;
        tickSlot(Kind.JOE, Cfg.joeEnabled);
        tickSlot(Kind.SEAN, Cfg.seanEnabled);
        war.tick();
    }

    private void tickSlot(Kind kind, boolean enabled) {
        ServerLevel level = overworld();
        GoblinData data = data();
        GoblinData.NpcSlot slot = slot(kind);
        long now = now();

        if (!enabled) {
            Entity existing = findLoaded(kind);
            if (existing != null) {
                existing.discard();
            }
            if (slot.uuid != null) {
                slot.uuid = null;
                data.setDirty();
            }
            return;
        }

        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator() && p.isAlive()).toList();

        if (slot.uuid == null) {
            if (slot.respawnAt == 0 && slot.lastPos == null && away()) {
                // A new world: they turn up a little while in, not right away.
                slot.respawnAt = now + minutes(5, 20);
                data.setDirty();
            }
            if (now >= slot.respawnAt && !players.isEmpty()) {
                ServerPlayer near = players.get(level.random.nextInt(players.size()));
                BlockPos spot = Wander.pickSpawnPoint(level, near.position(), Cfg.spawnDistanceMin, Cfg.spawnDistanceMax, level.random);
                if (spot != null) {
                    spawn(kind, spot);
                }
            }
            return;
        }

        Entity entity = level.getEntity(slot.uuid);
        if (away() && slot.leaveAt == 0) {
            slot.leaveAt = now + minutes(Cfg.stayMinutesMin, Cfg.stayMinutesMax);
            data.setDirty();
        }
        if (away() && now >= slot.leaveAt && !busy(now) && noPlayerNear(entity, players)) {
            // Visit's over: they wander off and come back later.
            if (entity != null) {
                entity.discard();
            }
            slot.uuid = null;
            slot.leaveAt = 0;
            slot.respawnAt = now + minutes(Cfg.awayMinutesMin, Cfg.awayMinutesMax);
            data.setDirty();
            SeanCraft.LOG.info("{} wandered off; back in about {} minutes.", kind, (slot.respawnAt - now) / 1200);
            return;
        }
        if (entity != null && entity.isAlive()) {
            slot.lastPos = entity.blockPosition();
            data.setDirty();
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(entity) < 128 * 128) {
                    slot.lastSeen = now;
                    break;
                }
            }
        }

        if (players.isEmpty()) {
            // Nobody around to miss them; don't count this as being stranded.
            slot.lastSeen = now;
            return;
        }

        if (now - slot.lastSeen > (long) (Cfg.strandedMinutes * 60 * 20)) {
            ServerPlayer near = players.get(level.random.nextInt(players.size()));
            BlockPos spot = Wander.pickSpawnPoint(level, near.position(), Cfg.spawnDistanceMin, Cfg.spawnDistanceMax, level.random);
            if (spot == null) {
                return;
            }
            if (entity instanceof Mob mob && entity.isAlive()) {
                mob.getNavigation().stop();
                mob.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                slot.lastSeen = now;
                slot.lastPos = spot;
                data.setDirty();
                SeanCraft.LOG.info("{} was stranded; moved near {}.", kind, near.getName().getString());
            } else {
                // Stuck in an unloaded chunk. Start a new one here; the old copy removes itself if it ever loads.
                spawn(kind, spot);
            }
        }
    }

    /** Whether Joe and Sean come and go at all (AwayMinutes above 0). */
    private static boolean away() {
        return Cfg.awayMinutesMax > 0;
    }

    private long minutes(double min, double max) {
        double m = min + overworld().random.nextDouble() * Math.max(0, max - min);
        return (long) (m * 60 * 20);
    }

    /** Never leave mid-fight or during the Baby Wars. */
    private boolean busy(long now) {
        Joe joe = joe();
        return war.isActive() || (joe != null && joe.fight().isActive(now));
    }

    /** Only vanish when nobody's close enough to watch. */
    private static boolean noPlayerNear(Entity entity, List<ServerPlayer> players) {
        if (entity == null) {
            return true;
        }
        for (ServerPlayer p : players) {
            if (p.distanceToSqr(entity) < 48 * 48) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ rage

    public void noteRage(ServerPlayer speaker) {
        rageUntil = now() + (long) (Cfg.rageSeconds * 20);
        rageTarget = speaker.getUUID();
        rageOrigin = speaker.position();
        Joe joe = joe();
        if (joe != null) {
            joe.onRage(speaker);
        }
        war.noteRage(speaker);
    }

    public boolean isRaging() {
        return now() < rageUntil;
    }

    public UUID rageTarget() {
        return rageTarget;
    }

    public Vec3 rageOrigin() {
        return rageOrigin;
    }

    // ------------------------------------------------------------------ info

    public String describe() {
        StringBuilder sb = new StringBuilder();
        describe(sb, "Joe", data().joe);
        sb.append('\n');
        describe(sb, "Sean", data().sean);
        sb.append("\nJoe enraged: ").append(isRaging() ? "yes (" + (rageUntil - now()) / 20 + "s left)" : "no");
        sb.append("\nBaby Wars: ").append(war.describe());
        return sb.toString();
    }

    private void describe(StringBuilder sb, String name, GoblinData.NpcSlot slot) {
        sb.append(name).append(": ");
        if (slot.uuid == null) {
            long wait = Math.max(0, slot.respawnAt - now()) / 20;
            sb.append(wait > 0 ? "away (or dead), back in " + wait + "s" : "not spawned yet (waiting for a player in the overworld)");
            return;
        }
        sb.append(slot.lastPos != null ? "last seen at " + slot.lastPos.toShortString() : "position unknown");
        sb.append(overworld().getEntity(slot.uuid) != null ? " (loaded)" : " (in an unloaded chunk)");
    }
}
