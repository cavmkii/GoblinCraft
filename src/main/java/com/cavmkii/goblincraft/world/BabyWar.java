package com.cavmkii.goblincraft.world;

import com.cavmkii.goblincraft.Cfg;
import com.cavmkii.goblincraft.GoblinCraft;
import com.cavmkii.goblincraft.Lines;
import com.cavmkii.goblincraft.Speech;
import com.cavmkii.goblincraft.entity.BabyGoblin;
import com.cavmkii.goblincraft.entity.Joe;
import com.cavmkii.goblincraft.entity.ModEntities;
import com.cavmkii.goblincraft.entity.SeanSecurity;
import com.cavmkii.goblincraft.entity.Wander;
import com.cavmkii.goblincraft.item.ModItems;
import com.cavmkii.goblincraft.rpg.Rpg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Baby Wars. Saying the rage phrase enough times starts a war near whoever said it last: Joe
 * sets up a war camp and sends waves of goblin toddlers at the players, while Sean shows up with his
 * Security. Wars aren't saved: a server restart ends one, and leftover camp blocks are cleared on start-up.
 */
public final class BabyWar {
    /** Cauldron offerings and the curse each one puts on the next wave. */
    public static final Map<Item, Integer> OFFERINGS = Map.of(
            Items.POISONOUS_POTATO, BabyGoblin.CURSE_PUKE,
            Items.COBWEB, BabyGoblin.CURSE_SLOW,
            Items.LEATHER, BabyGoblin.CURSE_HEAVY);

    private final WorldDirector director;

    private final List<Long> triggers = new ArrayList<>();
    private final Set<UUID> speakers = new LinkedHashSet<>();
    private final Set<UUID> babies = new HashSet<>();
    private final Set<UUID> guards = new HashSet<>();
    private UUID lastTriggerPlayer;
    private long lastTriggerTime = Long.MIN_VALUE;

    private boolean active;
    private Vec3 camp = Vec3.ZERO;
    private BlockPos cauldron;
    private long startedAt;
    private long endsAt;
    private long cooldownUntil;
    private long nextWaveAt;
    private int wave;
    private int lastWaveSize;
    private int pendingCurses;
    private long nextTick;

    BabyWar(WorldDirector director) {
        this.director = director;
    }

    public boolean isActive() {
        return active;
    }

    public Vec3 camp() {
        return camp;
    }

    private ServerLevel level() {
        return director.overworld();
    }

    private long now() {
        return director.now();
    }

    // ================================================================== trigger

    /** Called whenever anyone says the rage phrase. */
    void noteRage(ServerPlayer speaker) {
        if (!Cfg.warEnabled) {
            return;
        }
        long now = now();
        // Chat and /say can both fire for one message; count it once.
        if (speaker.getUUID().equals(lastTriggerPlayer) && now - lastTriggerTime < 40) {
            return;
        }
        lastTriggerPlayer = speaker.getUUID();
        lastTriggerTime = now;

        speakers.add(speaker.getUUID());
        if (active) {
            giveBonnet(speaker);
            return;
        }
        if (now < cooldownUntil || speaker.level() != level()) {
            return;
        }

        triggers.add(now);
        long window = (long) (Cfg.warTriggerWindowMinutes * 60 * 20);
        triggers.removeIf(t -> now - t > window);
        if (triggers.size() >= Math.max(1, Cfg.warTriggerCount)) {
            triggers.clear();
            start(speaker.position());
        }
    }

    // ================================================================== lifecycle

    public boolean start(Vec3 origin) {
        if (active) {
            return false;
        }
        ServerLevel level = level();
        Vec3 pick = Wander.pickLandPoint(level, origin, 25, 40, level.random);
        BlockPos campPos = pick != null ? Wander.surface(level, pick) : null;
        if (campPos == null) {
            campPos = Wander.surface(level, origin.add(25, 0, 0));
        }
        if (campPos == null) {
            campPos = BlockPos.containing(origin);
        }

        long now = now();
        active = true;
        camp = Vec3.atBottomCenterOf(campPos);
        startedAt = now;
        wave = 0;
        lastWaveSize = 0;
        pendingCurses = 0;
        nextWaveAt = now + 300;
        endsAt = now + (long) (Cfg.warMaxMinutes * 60 * 20);
        babies.clear();
        guards.clear();

        buildCamp(campPos);

        // Joe takes command; Sean arrives on the far side with his Security.
        if (Cfg.joeEnabled) {
            BlockPos joeSpot = Wander.surface(level, camp.add(3, 0, 0));
            director.replace(WorldDirector.Kind.JOE, joeSpot != null ? joeSpot : campPos.above());
        }
        if (Cfg.seanEnabled) {
            Vec3 away = origin.subtract(camp).multiply(1, 0, 1);
            away = away.lengthSqr() > 0.01 ? away.normalize() : new Vec3(0, 0, -1);
            BlockPos seanSpot = Wander.surface(level, camp.add(away.scale(35)));
            if (seanSpot == null) {
                seanSpot = Wander.surface(level, camp.add(away.scale(12)));
            }
            if (seanSpot != null) {
                director.replace(WorldDirector.Kind.SEAN, seanSpot);
                for (int i = 0; i < Cfg.warSecurityCount; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    BlockPos spot = Wander.surface(level, Vec3.atBottomCenterOf(seanSpot).add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4));
                    Mob guard = spawn(ModEntities.SEAN_SECURITY, spot);
                    if (guard != null) {
                        guards.add(guard.getUUID());
                    }
                }
            }
        }

        level.setWeatherParameters(0, (int) (Cfg.warMaxMinutes * 60 * 20) + 1200, true, true);
        for (UUID id : speakers) {
            ServerPlayer player = director.server.getPlayerList().getPlayer(id);
            if (player != null) {
                giveBonnet(player);
            }
        }
        Speech.titleAll(director.server, "THE BABY WARS HAVE BEGUN");
        Joe joe = director.joe();
        if (joe != null) {
            joe.onWarEvent(Lines.joeWarStart());
        }
        GoblinCraft.LOG.info("Baby Wars started; camp at {}.", campPos.toShortString());
        return true;
    }

    public boolean end(String message) {
        if (!active) {
            return false;
        }
        active = false;
        cooldownUntil = now() + (long) (Cfg.warCooldownMinutes * 60 * 20);
        int removed = removeWarMobs();
        clearCamp();
        level().setWeatherParameters(6000, 0, false, false);
        Joe joe = director.joe();
        if (joe != null) {
            joe.onWarEvent(Lines.joeWarEnd());
        }
        for (ServerPlayer player : director.server.getPlayerList().getPlayers()) {
            Speech.title(player, message);
        }
        GoblinCraft.LOG.info("Baby Wars ended ({}); removed {} war mobs.", message, removed);
        return true;
    }

    void tick() {
        if (!active) {
            return;
        }
        long now = now();
        if (now < nextTick && now >= nextTick - 40) {
            return;
        }
        nextTick = now + 40;

        ServerLevel level = level();
        babies.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });

        if (Cfg.joeEnabled && !director.joeAlive() && now - startedAt > 400) {
            end("Joe has fallen! The babies scatter.");
        } else if (now > endsAt) {
            end("The Baby Wars end in a stalemate. Joe declares victory anyway.");
        } else if (wave < Cfg.warWaves) {
            boolean mostlyDead = wave > 0 && babies.size() <= Math.ceil(lastWaveSize * 0.25);
            if (now >= nextWaveAt || mostlyDead) {
                spawnWave();
            }
        } else if (babies.isEmpty()) {
            for (ServerPlayer player : level.players()) {
                if (player.position().distanceTo(camp) < 200) {
                    Rpg.addClassXp(player, 2000);
                }
            }
            end("The last baby has fallen. The Baby Wars are over... for now.");
        }
    }

    private void spawnWave() {
        ServerLevel level = level();
        wave++;
        int size = Math.max(1, Cfg.warWaveBaseSize + Cfg.warWaveGrowth * (wave - 1));
        lastWaveSize = size;
        int curses = pendingCurses;
        pendingCurses = 0;

        for (int i = 0; i < size; i++) {
            float roll = level.random.nextFloat();
            EntityType<BabyGoblin> type = wave >= 2 && roll < 0.15f ? ModEntities.BABY_BERSERKER
                    : roll < 0.4f ? ModEntities.BABY_GRENADIER : ModEntities.BABY_COMMANDO;
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 4 + level.random.nextDouble() * 6;
            BlockPos spot = Wander.surface(level, camp.add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            Mob baby = spawn(type, spot);
            if (baby instanceof BabyGoblin goblin) {
                goblin.applyCurses(curses);
                babies.add(goblin.getUUID());
            }
        }

        nextWaveAt = now() + (long) (Cfg.warWaveIntervalSeconds * 20);
        String cursed = curses == 0 ? "" : " (cursed: " + describeCurses(curses) + ")";
        Speech.announce(level, camp, 200, "Wave " + wave + "/" + Cfg.warWaves + ": " + size + " babies" + cursed);
        Joe joe = director.joe();
        if (joe != null) {
            joe.onWarEvent(Lines.joeWarWave());
        }
    }

    private <T extends Mob> T spawn(EntityType<T> type, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        ServerLevel level = level();
        T mob = type.create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        level.addFreshEntity(mob);
        return mob;
    }

    private int removeWarMobs() {
        ServerLevel level = level();
        int removed = 0;
        for (UUID id : babies) {
            removed += discard(level.getEntity(id));
        }
        for (UUID id : guards) {
            removed += discard(level.getEntity(id));
        }
        babies.clear();
        guards.clear();
        // Anything else (unloaded at the time) removes itself when it next ticks.
        return removed;
    }

    private static int discard(Entity entity) {
        if (entity == null) {
            return 0;
        }
        entity.discard();
        return 1;
    }

    // ================================================================== camp

    private void buildCamp(BlockPos ground) {
        GoblinData data = director.data();
        // The cauldron sits on a campfire so it glows and smokes.
        place(data, ground, Blocks.CAMPFIRE.defaultBlockState());
        cauldron = ground.above();
        place(data, cauldron, Blocks.CAULDRON.defaultBlockState());
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(i * 120 + 30);
            BlockPos spot = Wander.surface(level(), camp.add(Math.sin(angle) * 6, 0, Math.cos(angle) * 6));
            if (spot == null) {
                continue;
            }
            int rotation = Math.floorMod((int) Math.round((i * 120 + 30) / 22.5), 16);
            if (i == 0) {
                // Totem: two fence posts and a skull.
                place(data, spot, Blocks.OAK_FENCE.defaultBlockState());
                place(data, spot.above(), Blocks.OAK_FENCE.defaultBlockState());
                place(data, spot.above(2), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rotation));
            } else {
                place(data, spot, Blocks.RED_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, rotation));
            }
        }
        data.setDirty();
    }

    /** Places a camp block only where nothing solid is in the way, and remembers it for removal. */
    private void place(GoblinData data, BlockPos pos, BlockState state) {
        ServerLevel level = level();
        if (!level.getBlockState(pos).canBeReplaced()) {
            return;
        }
        level.setBlock(pos, state, 3);
        data.campBlocks.add(pos.immutable());
    }

    private void clearCamp() {
        ServerLevel level = level();
        GoblinData data = director.data();
        // Top-down, so skulls and banners don't pop off as items first.
        for (int i = data.campBlocks.size() - 1; i >= 0; i--) {
            BlockPos pos = data.campBlocks.get(i);
            level.removeBlock(pos, false);
        }
        data.campBlocks.clear();
        data.setDirty();
        cauldron = null;
    }

    public boolean isCampBlock(BlockPos pos) {
        return active && director.data().campBlocks.contains(pos);
    }

    /** Leftovers from a war cut short by a shutdown. */
    void cleanupLeftovers() {
        if (!director.data().campBlocks.isEmpty()) {
            GoblinCraft.LOG.info("Removing {} leftover Baby Wars camp blocks.", director.data().campBlocks.size());
            clearCamp();
        }
    }

    void shutdown() {
        if (active) {
            // Wars don't survive a restart; take the camp down now so the save is clean.
            removeWarMobs();
            clearCamp();
            active = false;
        }
    }

    // ================================================================== curses and bonnets

    /** A player used an item on the cauldron. Returns true if the use was handled. */
    public boolean onCauldronUse(ServerPlayer player, BlockPos pos, ItemStack stack) {
        if (cauldron == null || !cauldron.equals(pos)) {
            return false;
        }
        Integer curse = OFFERINGS.get(stack.getItem());
        if (curse == null) {
            Speech.notify(player, "Joe's War Cauldron: offer a Poisonous Potato (puke), Cobweb (slow) or Leather (heavy) to curse the next wave.");
            return true;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        pendingCurses |= curse;
        Speech.announce(level(), camp, 200, player.getName().getString() + " cursed Joe's next wave: " + describeCurses(curse) + "!");
        return true;
    }

    private static String describeCurses(int curses) {
        List<String> parts = new ArrayList<>();
        if ((curses & BabyGoblin.CURSE_PUKE) != 0) {
            parts.add("Puke");
        }
        if ((curses & BabyGoblin.CURSE_SLOW) != 0) {
            parts.add("Slow");
        }
        if ((curses & BabyGoblin.CURSE_HEAVY) != 0) {
            parts.add("Heavy");
        }
        return String.join(", ", parts);
    }

    private void giveBonnet(ServerPlayer player) {
        if (Joe.carries(player, ModItems.BABY_BONNET) || Joe.wearsBonnet(player)) {
            return;
        }
        if (player.getInventory().add(new ItemStack(ModItems.BABY_BONNET))) {
            Speech.notify(player, "Joe slipped a Baby Bonnet into your pack. Wear it and the babies are on your side.");
        }
    }

    // ================================================================== admin

    public String describe() {
        if (active) {
            return "active, wave " + wave + "/" + Cfg.warWaves + ", " + babies.size() + " babies left, camp at "
                    + BlockPos.containing(camp).toShortString();
        }
        long cooldown = Math.max(0, cooldownUntil - now()) / 20;
        return cooldown > 0 ? "over (cooldown " + cooldown + "s)" : "not running (" + triggers.size() + "/" + Cfg.warTriggerCount + " triggers)";
    }

    public void forceStart(ServerPlayer at) {
        speakers.add(at.getUUID());
        start(at.position());
    }
}
