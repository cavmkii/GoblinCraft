package com.cavmkii.goblincraft.tarkov;

import com.cavmkii.goblincraft.Cfg;
import com.cavmkii.goblincraft.GoblinCraft;
import com.cavmkii.goblincraft.entity.Wander;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;

/**
 * TarkoValheim's raid loop on the server. Outside every hideout is raid; the longer you're out, the higher the
 * threat (elite mobs, hunting parties). Dying in raid costs your kit, except the secure container. Hideout
 * benches hold modules and craft hideout-only items from what you carry plus the stash.
 */
public final class Tarkov {
    public static final int MAX_THREAT = 4;
    private static final int[] WAVE_SECONDS = {0, 150, 100, 70, 50};
    private static final int[] WAVE_SIZE = {0, 1, 2, 3, 3};
    private static final double[] ONE_STAR = {0, 0.10, 0.20, 0.35, 0.50};
    private static final double[] TWO_STAR = {0, 0, 0.05, 0.10, 0.20};
    private static final String ROLLED_TAG = "goblincraft_threat_rolled";

    /** Per-session raid state. */
    static final class Runtime {
        long graceUntil;
        Boolean inRaid;
        long raidStart;
        int announcedThreat;
        long nextWave;
        long lastRaidTicks;
    }

    private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

    private Tarkov() {}

    public static TarkovData data(Player player) {
        return player.getAttachedOrCreate(TarkovContent.DATA);
    }

    private static Runtime runtime(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), id -> new Runtime());
    }

    public static void init() {
        TarkovContent.init();
        TarkovNet.register();

        ServerTickEvents.END_SERVER_TICK.register(Tarkov::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Runtime rt = runtime(handler.player);
            rt.graceUntil = handler.player.serverLevel().getGameTime() + 100;
            rt.inRaid = null;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RUNTIME.remove(handler.player.getUUID()));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Runtime rt = runtime(newPlayer);
            rt.graceUntil = newPlayer.serverLevel().getGameTime() + 60;
            rt.inRaid = null;
            rt.announcedThreat = 0;
        });
        ServerEntityEvents.ENTITY_LOAD.register(Tarkov::rollElite);

        ServerPlayNetworking.registerGlobalReceiver(TarkovNet.HideoutAction.TYPE, (payload, ctx) -> {
            if (payload.kind() >= 0) {
                hideoutAction(ctx.player(), payload.pos(), payload.kind(), payload.id());
            }
            openHideout(ctx.player(), payload.pos());
        });
        ServerPlayNetworking.registerGlobalReceiver(TarkovNet.OpenSecure.TYPE, (payload, ctx) -> openSecure(ctx.player()));
    }

    // ================================================================== tick

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            tickHideouts(level);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive() && !player.isSpectator()) {
                tickPlayer(player);
            }
        }
    }

    private static void tickHideouts(ServerLevel level) {
        Hideouts data = Hideouts.get(level);
        long now = level.getGameTime();
        for (Hideouts.Hideout h : data.list) {
            if (h.building >= 0 && now >= h.buildDone) {
                Module module = Module.values()[h.building];
                h.levels[h.building]++;
                h.building = -1;
                data.setDirty();
                for (ServerPlayer p : level.players()) {
                    if (h.contains(p.position())) {
                        p.sendSystemMessage(Component.literal(module.label + " is now level " + h.level(module) + ".")
                                .withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        Runtime rt = runtime(player);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Hideouts.Hideout home = Hideouts.at(level, player.position());
        TarkovData data = data(player);

        if (now < rt.graceUntil) {
            ServerPlayNetworking.send(player, new TarkovNet.RaidState(0, 0, 0, -1));
            return;
        }

        boolean inRaid = home == null;
        if (rt.inRaid == null || rt.inRaid != inRaid) {
            if (inRaid) {
                rt.raidStart = now;
                rt.announcedThreat = 0;
                title(player, "RAID STARTED", "Die out here and you lose everything outside your secure container", ChatFormatting.RED);
            } else if (rt.inRaid != null) {
                title(player, "EXTRACTED", "Raid time " + clock(now - rt.raidStart), ChatFormatting.GREEN);
            }
            rt.inRaid = inRaid;
        }

        if (home != null) {
            data.lastIntel = home.level(Module.INTEL);
            if (home.level(Module.MEDSTATION) >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
            }
            ServerPlayNetworking.send(player, new TarkovNet.RaidState(1, 0, 0, -1));
            return;
        }

        long raidTicks = now - rt.raidStart;
        rt.lastRaidTicks = raidTicks;
        int threat = threat(raidTicks, data.lastIntel);
        if (threat > rt.announcedThreat) {
            String[] lines = {"", "The land stirs. Monsters grow bolder.", "You're being hunted.", "They're closing in.", "Get out. Now."};
            title(player, "THREAT " + threat, lines[threat], ChatFormatting.GOLD);
            rt.nextWave = now + 400;
        }
        rt.announcedThreat = threat;
        if (threat > 0 && Cfg.huntingParties && now >= rt.nextWave) {
            rt.nextWave = now + (long) (WAVE_SECONDS[threat] * 20 * (0.8 + player.getRandom().nextDouble() * 0.4));
            sendParty(player, threat, data.lastIntel);
        }
        ServerPlayNetworking.send(player, new TarkovNet.RaidState(2, (int) (raidTicks / 20), threat, secondsToNextThreat(raidTicks, data.lastIntel)));
    }

    static int threat(long raidTicks, int intel) {
        if (!Cfg.raidHostility) {
            return 0;
        }
        double minutes = raidTicks / 1200.0;
        double calm = Cfg.calmMinutes + intel * Module.INTEL_CALM_MINUTES;
        if (minutes < calm) {
            return 0;
        }
        return Math.min(MAX_THREAT, 1 + (int) Math.floor((minutes - calm) / Math.max(0.1, Cfg.threatStepMinutes)));
    }

    private static int secondsToNextThreat(long raidTicks, int intel) {
        int threat = threat(raidTicks, intel);
        if (!Cfg.raidHostility || threat >= MAX_THREAT) {
            return -1;
        }
        double calm = Cfg.calmMinutes + intel * Module.INTEL_CALM_MINUTES;
        double next = (calm + threat * Math.max(0.1, Cfg.threatStepMinutes)) * 60;
        return (int) Math.max(0, next - raidTicks / 20.0);
    }

    /** Current threat for a player, 0 if not in raid. */
    public static int threatFor(ServerPlayer player) {
        Runtime rt = RUNTIME.get(player.getUUID());
        if (rt == null || rt.inRaid == null || !rt.inRaid) {
            return 0;
        }
        return threat(player.serverLevel().getGameTime() - rt.raidStart, data(player).lastIntel);
    }

    public static boolean inRaid(ServerPlayer player) {
        Runtime rt = RUNTIME.get(player.getUUID());
        if (rt != null && rt.inRaid != null) {
            return rt.inRaid;
        }
        return Hideouts.at(player.serverLevel(), player.position()) == null;
    }

    // ================================================================== elites and hunting parties

    /** Monsters appearing near a player deep in a raid may be elite: +100% health and +50% damage per star. */
    private static void rollElite(Entity entity, ServerLevel level) {
        if (!(entity instanceof Monster mob) || entity.getTags().contains(ROLLED_TAG)) {
            return;
        }
        entity.addTag(ROLLED_TAG);
        int threat = 0;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) < 90 * 90) {
                threat = Math.max(threat, threatFor(player));
            }
        }
        if (threat > 0) {
            makeElite(mob, rollStars(threat, level.random.nextDouble()));
        }
    }

    private static int rollStars(int threat, double roll) {
        if (roll < TWO_STAR[threat]) {
            return 2;
        }
        return roll < TWO_STAR[threat] + ONE_STAR[threat] ? 1 : 0;
    }

    private static void makeElite(Mob mob, int stars) {
        if (stars <= 0) {
            return;
        }
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(GoblinCraft.id("elite_health"), stars, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.addOrReplacePermanentModifier(new AttributeModifier(GoblinCraft.id("elite_damage"), stars * 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        mob.setCustomName(Component.literal("★".repeat(stars) + " ").withStyle(ChatFormatting.GOLD).append(mob.getType().getDescription()));
    }

    /** A few of the local biome's monsters, 30-45 blocks away, sent straight at you. */
    private static void sendParty(ServerPlayer player, int threat, int intel) {
        ServerLevel level = player.serverLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        List<EntityType<? extends Mob>> pool = hunters(level, player.blockPosition());
        if (pool.isEmpty()) {
            return;
        }
        if (intel >= 2) {
            player.displayClientMessage(Component.literal("Intel: a hunting party is coming for you.").withStyle(ChatFormatting.RED), true);
        }
        for (int i = 0; i < WAVE_SIZE[threat]; i++) {
            BlockPos spot = findSpot(level, player);
            if (spot == null) {
                continue;
            }
            Mob mob = pool.get(level.random.nextInt(pool.size())).create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            mob.addTag(ROLLED_TAG);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
            if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                // Keeps undead from burning before they reach you.
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                mob.setDropChance(EquipmentSlot.HEAD, 0f);
            }
            AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null) {
                follow.addOrReplacePermanentModifier(new AttributeModifier(GoblinCraft.id("hunter"), 32, AttributeModifier.Operation.ADD_VALUE));
            }
            makeElite(mob, rollStars(threat, level.random.nextDouble()));
            level.addFreshEntity(mob);
            mob.setTarget(player);
        }
    }

    private static List<EntityType<? extends Mob>> hunters(ServerLevel level, BlockPos pos) {
        List<EntityType<? extends Mob>> pool = new ArrayList<>();
        if (level.dimension() == Level.NETHER) {
            pool.add(EntityType.WITHER_SKELETON);
            pool.add(EntityType.BLAZE);
            pool.add(EntityType.PIGLIN_BRUTE);
            return pool;
        }
        if (level.dimension() == Level.END) {
            pool.add(EntityType.ENDERMAN);
            return pool;
        }
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            pool.add(EntityType.DROWNED);
        } else if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
            pool.add(EntityType.HUSK);
            pool.add(EntityType.SKELETON);
        } else if (biome.value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW) {
            pool.add(EntityType.STRAY);
            pool.add(EntityType.ZOMBIE);
        } else if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
            pool.add(EntityType.WITCH);
            pool.add(EntityType.ZOMBIE);
            pool.add(EntityType.SLIME);
        } else if (biome.is(Biomes.DARK_FOREST)) {
            pool.add(EntityType.VINDICATOR);
            pool.add(EntityType.PILLAGER);
        } else if (biome.is(BiomeTags.IS_JUNGLE)) {
            pool.add(EntityType.SPIDER);
            pool.add(EntityType.ZOMBIE);
        } else {
            pool.add(EntityType.ZOMBIE);
            pool.add(EntityType.SKELETON);
            pool.add(EntityType.SPIDER);
        }
        return pool;
    }

    private static BlockPos findSpot(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 30 + level.random.nextDouble() * 15;
            Vec3 at = player.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            BlockPos ground = Wander.surface(level, at);
            if (ground != null && Math.abs(ground.getY() - player.getY()) < 25 && level.getFluidState(ground.below()).isEmpty()
                    && Hideouts.at(level, Vec3.atCenterOf(ground)) == null) {
                return ground;
            }
        }
        return null;
    }

    // ================================================================== death

    /** Called (from a mixin) just before a dying player's inventory would drop. */
    public static void beforeDeathDrops(ServerPlayer player) {
        boolean raid = inRaid(player);
        Runtime rt = runtime(player);
        String time = rt.inRaid != null && rt.inRaid ? " Raid time " + clock(player.serverLevel().getGameTime() - rt.raidStart) + "." : "";
        if (!raid) {
            return;
        }
        TarkovData data = data(player);
        switch (Cfg.raidDeathRule) {
            case LOSE_EVERYTHING -> {
                player.getInventory().clearContent();
                player.sendSystemMessage(Component.literal("Killed in action." + time + " Everything outside your secure container is gone.")
                        .withStyle(ChatFormatting.DARK_RED));
            }
            case KEEP_SECURE -> player.sendSystemMessage(Component.literal("Killed in action." + time + " Your secure container is safe.")
                    .withStyle(ChatFormatting.DARK_RED));
            case VANILLA -> {
                // Secure contents drop with everything else.
                for (int i = 0; i < data.tier.slots(); i++) {
                    if (!data.secure.get(i).isEmpty()) {
                        player.drop(data.secure.get(i), true, false);
                        data.secure.set(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    // ================================================================== secure container

    private static void openSecure(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }
        TarkovData data = data(player);
        SecureTier tier = data.tier;
        player.openMenu(new ExtendedScreenHandlerFactory<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer p) {
                return tier.ordinal();
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("Secure container (" + tier.label + ")");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new SecureMenu(id, inventory, tier, new SecureMenu.View(data.secure, tier.slots()));
            }
        });
    }

    // ================================================================== hideout bench

    public static void openHideout(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        Hideouts.Hideout h = Hideouts.byPos(level, pos);
        if (h == null || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64) {
            return;
        }
        long now = level.getGameTime();
        List<TarkovNet.Entry> entries = new ArrayList<>();
        for (Module module : Module.values()) {
            int current = h.level(module);
            String title = module.label + " " + current + "/" + module.max();
            String icon = id(module.icon);
            if (current >= module.max()) {
                entries.add(new TarkovNet.Entry(TarkovNet.Entry.MODULE, module.name(), title, module.levels.get(module.max() - 1).effect(),
                        icon, TarkovNet.Entry.MAXED, "Fully upgraded", List.of(), 0));
                continue;
            }
            Module.Level next = module.levels.get(current);
            List<TarkovNet.CostLine> costs = costLines(player, h, next.costs());
            int status;
            String note;
            int progress = 0;
            if (h.building == module.ordinal()) {
                status = TarkovNet.Entry.BUILDING;
                long total = Math.max(1, h.buildDone - h.buildStarted);
                progress = (int) Math.min(100, (now - h.buildStarted) * 100 / total);
                note = "Building, " + Math.max(0, (h.buildDone - now) / 20) + "s left";
            } else if (h.building >= 0) {
                status = TarkovNet.Entry.BUSY;
                note = "Another module is being built";
            } else if (next.needs() != null && h.level(next.needs()) < next.needsLevel()) {
                status = TarkovNet.Entry.LOCKED;
                note = "Needs " + next.needs().label + " " + next.needsLevel();
            } else if (!Stash.canAfford(player, h, next.costs())) {
                status = TarkovNet.Entry.SHORT;
                note = "Not enough materials";
            } else {
                status = TarkovNet.Entry.READY;
                note = "Build level " + (current + 1) + " (" + buildSeconds(h, next) + "s)";
            }
            entries.add(new TarkovNet.Entry(TarkovNet.Entry.MODULE, module.name(), title, "Next: " + next.effect(), icon, status, note, costs, progress));
        }
        for (HideoutRecipe recipe : HideoutRecipe.ALL) {
            ItemStack result = recipe.result().get();
            int status;
            String note;
            if (recipe.gate() != null && h.level(recipe.gate()) < recipe.gateLevel()) {
                status = TarkovNet.Entry.LOCKED;
                note = "Needs " + recipe.gate().label + " " + recipe.gateLevel();
            } else if (!Stash.canAfford(player, h, recipe.costs())) {
                status = TarkovNet.Entry.SHORT;
                note = "Not enough materials";
            } else {
                status = TarkovNet.Entry.READY;
                note = "Craft";
            }
            entries.add(new TarkovNet.Entry(TarkovNet.Entry.RECIPE, recipe.id(), result.getHoverName().getString()
                    + (result.getCount() > 1 ? " x" + result.getCount() : ""), describe(result), id(result.getItem()), status, note,
                    costLines(player, h, recipe.costs()), 0));
        }
        int chests = Stash.containers(level, h).size();
        String summary = "Radius " + (int) h.radius() + " blocks. Stash: " + chests + (chests == 1 ? " chest" : " chests") + " in range.";
        ServerPlayNetworking.send(player, new TarkovNet.HideoutView(pos, (int) h.radius(), summary, entries));
    }

    private static int buildSeconds(Hideouts.Hideout h, Module.Level level) {
        // The Workshop speeds up building, as it does crafting in TarkoValheim.
        return (int) Math.max(5, level.buildSeconds() * (1 - 0.25 * h.level(Module.WORKSHOP)));
    }

    private static void hideoutAction(ServerPlayer player, BlockPos pos, int kind, String id) {
        ServerLevel level = player.serverLevel();
        Hideouts.Hideout h = Hideouts.byPos(level, pos);
        if (h == null || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64) {
            return;
        }
        if (kind == TarkovNet.Entry.MODULE) {
            Module module;
            try {
                module = Module.valueOf(id);
            } catch (IllegalArgumentException e) {
                return;
            }
            int current = h.level(module);
            if (h.building >= 0 || current >= module.max()) {
                return;
            }
            Module.Level next = module.levels.get(current);
            if (next.needs() != null && h.level(next.needs()) < next.needsLevel() || !Stash.canAfford(player, h, next.costs())) {
                return;
            }
            Stash.consume(player, h, next.costs());
            long now = level.getGameTime();
            h.building = module.ordinal();
            h.buildStarted = now;
            h.buildDone = now + buildSeconds(h, next) * 20L;
            Hideouts.get(level).setDirty();
        } else if (kind == TarkovNet.Entry.RECIPE) {
            HideoutRecipe recipe = HideoutRecipe.byId(id);
            if (recipe == null || recipe.gate() != null && h.level(recipe.gate()) < recipe.gateLevel()
                    || !Stash.canAfford(player, h, recipe.costs())) {
                return;
            }
            Stash.consume(player, h, recipe.costs());
            ItemStack result = recipe.result().get();
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
    }

    private static List<TarkovNet.CostLine> costLines(ServerPlayer player, Hideouts.Hideout h, List<Cost> costs) {
        List<TarkovNet.CostLine> lines = new ArrayList<>();
        for (Cost cost : costs) {
            lines.add(new TarkovNet.CostLine(cost.label(), id(cost.icon()), Stash.count(player, h, cost), cost.count()));
        }
        return lines;
    }

    private static String describe(ItemStack stack) {
        if (stack.getItem() instanceof SecureItem secure) {
            return "Secure container, " + secure.tier.width + "x" + secure.tier.height + ". Use it to install.";
        }
        return "";
    }

    /** Furnaces inside a hideout cook faster with its Workshop: extra ticks of progress per tick. */
    public static double furnaceBonus(ServerLevel level, BlockPos pos) {
        Hideouts.Hideout h = Hideouts.at(level, Vec3.atCenterOf(pos));
        if (h == null) {
            return 0;
        }
        return switch (h.level(Module.WORKSHOP)) {
            case 1 -> 1 / 3.0;
            case 2 -> 1;
            case 3 -> 3;
            default -> 0;
        };
    }

    // ================================================================== helpers

    private static String id(net.minecraft.world.item.Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    static String clock(long ticks) {
        long total = Math.max(0, ticks / 20);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private static void title(ServerPlayer player, String title, String subtitle, ChatFormatting color) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 50, 15));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
    }
}
