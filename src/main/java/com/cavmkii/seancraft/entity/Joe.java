package com.cavmkii.seancraft.entity;

import com.cavmkii.seancraft.Cfg;
import com.cavmkii.seancraft.Lines;
import com.cavmkii.seancraft.PowerLevel;
import com.cavmkii.seancraft.Speech;
import com.cavmkii.seancraft.item.ModItems;
import com.cavmkii.seancraft.rpg.Rpg;
import com.cavmkii.seancraft.rpg.RpgContent;
import com.cavmkii.seancraft.world.GoblinData;
import com.cavmkii.seancraft.world.WorldDirector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Joe: a small goblin who roams looking for Sean, insults people, throws up, picks pockets, and only
 * fights players who hit him (or everyone, if someone says the phrase). Everything here runs on the server.
 */
public class Joe extends PathfinderMob {
    public static final String NAME = "Joe";
    static final double BASE_HEALTH = 40;
    static final double BASE_DAMAGE = 6;

    private final Wander wander = new Wander(this);
    private final Puke puke = new Puke(this);
    private final Fight fight = new Fight(this);
    private final Map<UUID, Long> provokers = new HashMap<>();

    private long nextThink;
    private long nextTaunt;
    private long nextScale;
    private long nextPuke;
    private long fleeUntil;
    private long nextRestrainedLine;
    private long stealCooldownUntil;
    private int displayLevel = -1;

    public Joe(EntityType<? extends Joe> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (!level.isClientSide) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
            setDropChance(EquipmentSlot.MAINHAND, 0f);
            AttributeInstance scale = getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(Mth.clamp(Cfg.joeScale, 0.3, 3.0));
            }
            setCustomName(Component.literal(NAME));
            long now = level.getGameTime();
            nextTaunt = now + 100 + getRandom().nextInt(140);
            nextPuke = now + 400 + getRandom().nextInt(400);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, BASE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, Sean.class, BabyGoblin.class, SeanSecurity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target instanceof Player player && isHostileTo(player)));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    WorldDirector director() {
        return WorldDirector.get();
    }

    Wander wander() {
        return wander;
    }

    public Fight fight() {
        return fight;
    }

    public int displayLevel() {
        return displayLevel;
    }

    void say(String line, boolean shout) {
        Speech.say(this, NAME, ChatFormatting.GREEN, line, shout);
    }

    // ---------------------------------------------------------------- think

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        WorldDirector director = director();
        if (director == null || !isAlive()) {
            return;
        }
        long now = level().getGameTime();
        puke.tick();
        if (fight.isActive(now)) {
            fight.tickCasts(now);
        }
        if (now < nextThink) {
            return;
        }
        nextThink = now + 10;

        if (!director.claim(WorldDirector.Kind.JOE, this)) {
            discard();
            return;
        }

        updateScaling(now);
        dropTargetIfNotHostile();

        if (fight.update(now)) {
            return;
        }
        fight.tryStart(now);

        if (director.war.isActive()) {
            updateWar(now);
            return;
        }

        if (director.isRaging()) {
            updateRage();
        } else if (!hasActiveProvokers(now)) {
            if (updatePuke(now)) {
                return;
            }
            updateCalm(now);
        } else if (getTarget() == null) {
            wander.step(1.0);
        }

        updateTaunts(now);
    }

    private void dropTargetIfNotHostile() {
        if (getTarget() instanceof Player player && !isHostileTo(player)) {
            setTarget(null);
        }
    }

    private boolean updatePuke(long now) {
        if (puke.active()) {
            return true;
        }
        if (!Cfg.pukeEnabled || now < nextPuke) {
            return false;
        }
        double min = Math.max(5, Cfg.pukeIntervalMin);
        nextPuke = now + secondsToTicks(randomBetween(min, Math.max(min, Cfg.pukeIntervalMax)));
        if (level().getNearestPlayer(this, 40) == null) {
            return false; // Nobody to gross out.
        }
        puke.start();
        say(Lines.pick(Lines.joePuke()), false);
        return true;
    }

    // ---------------------------------------------------------------- hostility

    /** Joe ignores players unless provoked, at war, or enraged. */
    public boolean isHostileTo(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        WorldDirector director = director();
        if (director == null) {
            return false;
        }
        Long until = provokers.get(player.getUUID());
        if (until != null && until > level().getGameTime()) {
            return true;
        }
        if (director.war.isActive()) {
            // In the war, Joe fights everyone except his Bonnet-wearing veterans.
            return !wearsBonnet(player);
        }
        return director.isRaging();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (source.getEntity() instanceof ServerPlayer attacker && !attacker.isCreative()) {
            onStruckBy(attacker);
        }
        if (source.getEntity() instanceof Sean) {
            amount = Fight.clampToDuelFloor(this, amount);
            if (amount <= 0) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    private void onStruckBy(ServerPlayer attacker) {
        Rpg.addClassXp(attacker, 5);
        long now = level().getGameTime();
        Long until = provokers.get(attacker.getUUID());
        boolean wasHostile = until != null && until > now;
        provokers.put(attacker.getUUID(), now + secondsToTicks(Cfg.provokeSeconds));
        if (!wasHostile && director() != null && !director().isRaging()) {
            say(Lines.pick(Lines.joeProvoked(), attacker.getName().getString()), true);
        }
    }

    private boolean hasActiveProvokers(long now) {
        provokers.values().removeIf(until -> until <= now);
        return !provokers.isEmpty();
    }

    public void onRage(ServerPlayer speaker) {
        say(Lines.pick(Lines.joeRage(), speaker.getName().getString()), true);
        Speech.announce((ServerLevel) level(), position(), Speech.SHOUT_RANGE, "Joe heard that. Joe is FURIOUS.");
    }

    /** War announcements: Joe shouts a line to whoever's nearest. */
    public void onWarEvent(List<String> lines) {
        Player nearest = level().getNearestPlayer(this, 80);
        say(Lines.pick(lines, nearest != null ? nearest.getName().getString() : null), true);
    }

    /** During the Baby Wars Joe holds his camp and shouts orders; his AI fights whoever comes. */
    private void updateWar(long now) {
        Vec3 camp = director().war.camp();
        wander.clearDestination();
        wander.setAnchor(camp, 12);
        if (getTarget() == null) {
            wander.step(1.0);
        }
        if (now < nextTaunt) {
            return;
        }
        nextTaunt = now + secondsToTicks(randomBetween(10, 20));
        Player nearest = level().getNearestPlayer(this, 60);
        if (nearest != null) {
            say(Lines.pick(Lines.joeWar(), nearest.getName().getString()), true);
        }
    }

    // ---------------------------------------------------------------- calm: wander, look for Sean, pickpocket

    private void updateCalm(long now) {
        if (getTarget() != null) {
            return;
        }
        if (now < fleeUntil) {
            wander.step(1.3);
            return;
        }
        if (keepRestrainingDistance(now)) {
            return;
        }
        if (Cfg.stealEnabled && now >= stealCooldownUntil) {
            ServerPlayer mark = closestUnprotectedPlayer(30);
            if (mark != null) {
                stalkAndSteal(mark, now);
                return;
            }
        }
        if (wander.step(1.0)) {
            pickDestination();
        }
    }

    /** Nearest player in range who isn't carrying the Restraining Order. */
    private ServerPlayer closestUnprotectedPlayer(double range) {
        ServerPlayer best = null;
        double bestDistance = range * range;
        for (ServerPlayer player : ((ServerLevel) level()).players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive() || carries(player, ModItems.RESTRAINING_ORDER)) {
                continue;
            }
            double d = player.distanceToSqr(this);
            if (d <= bestDistance) {
                best = player;
                bestDistance = d;
            }
        }
        return best;
    }

    /**
     * If Joe is closer than the legal distance to someone carrying the Restraining Order, he backs
     * off (complaining). Returns true while he's busy doing that.
     */
    private boolean keepRestrainingDistance(long now) {
        double limit = Cfg.restrainingDistance;
        for (ServerPlayer player : ((ServerLevel) level()).players()) {
            if (player.isSpectator() || !carries(player, ModItems.RESTRAINING_ORDER)) {
                continue;
            }
            Vec3 away = position().subtract(player.position()).multiply(1, 0, 1);
            if (away.length() >= limit) {
                continue;
            }
            if (away.lengthSqr() < 0.01) {
                double angle = getRandom().nextDouble() * Math.PI * 2;
                away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            }
            Vec3 retreat = player.position().add(away.normalize().scale(limit + 15));
            wander.setDestination(retreat);
            fleeUntil = now + 120;
            wander.step(1.3);
            if (now >= nextRestrainedLine) {
                nextRestrainedLine = now + 500;
                say(Lines.pick(Lines.joeRestrained(), player.getName().getString()), false);
            }
            return true;
        }
        return false;
    }

    private void stalkAndSteal(ServerPlayer mark, long now) {
        if (distanceTo(mark) > 2.5) {
            getNavigation().moveTo(mark, 1.1);
            return;
        }

        ItemStack loot = pickPocket(mark);
        stealCooldownUntil = now + secondsToTicks(Cfg.stealCooldownSeconds);
        if (!loot.isEmpty()) {
            GoblinData data = director().data();
            data.stash.add(loot);
            data.setDirty();
            Speech.notify(mark, "Joe stole " + loot.getCount() + "x " + loot.getHoverName().getString() + "!");
        }
        say(Lines.pick(Lines.joeSteal(), mark.getName().getString()), false);

        // Bolt away from the victim.
        Vec3 away = position().subtract(mark.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.01) {
            away = new Vec3(1, 0, 0);
        }
        wander.setDestination(position().add(away.normalize().scale(40)));
        fleeUntil = now + 400;
    }

    /** Takes up to StealMaxStack items from one random stack that isn't in the player's hand or worn. */
    private static ItemStack pickPocket(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (i != inventory.selected && !inventory.items.get(i).isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = inventory.items.get(slots.get(player.getRandom().nextInt(slots.size())));
        ItemStack taken = stack.split(Math.max(1, Math.min(Cfg.stealMaxStack, stack.getCount())));
        inventory.setChanged();
        return taken;
    }

    private void pickDestination() {
        BlockPos seanPos = director().seanLastPos();
        if (seanPos != null && getRandom().nextDouble() < Cfg.seanSeekChance) {
            Vec3 target = Vec3.atBottomCenterOf(seanPos);
            if (target.distanceTo(position()) < 3000) {
                wander.setDestination(target);
                return;
            }
        }
        Vec3 point = Wander.pickLandPoint((ServerLevel) level(), position(), 60, 200, getRandom());
        if (point != null) {
            wander.setDestination(point);
        }
    }

    /** A rage phrase sends Joe charging at whoever said it. */
    private void updateRage() {
        if (getTarget() != null) {
            return;
        }
        UUID targetId = director().rageTarget();
        Player target = targetId != null ? level().getPlayerByUUID(targetId) : null;
        Vec3 goal = target != null ? target.position() : director().rageOrigin();
        if (goal.distanceTo(position()) < 400) {
            wander.setDestination(goal);
            wander.step(1.3);
        } else if (wander.step(1.0)) {
            pickDestination();
        }
    }

    private void updateTaunts(long now) {
        if (now < nextTaunt) {
            return;
        }
        nextTaunt = now + secondsToTicks(randomBetween(Cfg.tauntIntervalMin, Math.max(Cfg.tauntIntervalMin, Cfg.tauntIntervalMax)));
        ServerPlayer nearest = closestUnprotectedPlayer(40);
        if (nearest != null) {
            say(Lines.pick(Lines.joeAmbient(), nearest.getName().getString()), false);
        }
    }

    // ---------------------------------------------------------------- scaling

    private void updateScaling(long now) {
        if (now < nextScale) {
            return;
        }
        nextScale = now + 300;

        float level = PowerLevel.evaluate((ServerLevel) level(), position());
        double hpMult = PowerLevel.lerp(Cfg.healthMultMin, Cfg.healthMultMax, level);
        double dmgMult = PowerLevel.lerp(Cfg.damageMultMin, Cfg.damageMultMax, level);
        applyScaling(this, BASE_HEALTH * hpMult, BASE_DAMAGE * dmgMult);

        int shown = Math.round(level * 100);
        if (shown != displayLevel || getCustomName() == null) {
            displayLevel = shown;
            setCustomName(Component.literal(NAME + " (Lv " + shown + ")"));
        }
    }

    /** Sets max health (keeping the current health fraction) and attack damage. */
    static void applyScaling(PathfinderMob mob, double maxHealth, double damage) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        double newMax = Mth.clamp(maxHealth, 4, 1024);
        if (health != null && Math.abs(health.getBaseValue() - newMax) > 0.5) {
            float ratio = mob.getMaxHealth() > 0 ? Mth.clamp(mob.getHealth() / mob.getMaxHealth(), 0f, 1f) : 1f;
            health.setBaseValue(newMax);
            mob.setHealth((float) (mob.getMaxHealth() * ratio));
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(Math.max(0.5, damage));
        }
    }

    // ---------------------------------------------------------------- death

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !isRemoved() && !dead) {
            WorldDirector director = director();
            if (director != null && director.claim(WorldDirector.Kind.JOE, this)) {
                say(Lines.pick(Lines.joeDeath()), true);
                dropStash(director);
                if (getRandom().nextFloat() < 0.25f) {
                    spawnAtLocation(new ItemStack(RpgContent.ORB_OF_REGRET));
                }
                if (source.getEntity() instanceof ServerPlayer killer) {
                    Rpg.addClassXp(killer, 500);
                }
                director.onDeath(WorldDirector.Kind.JOE, this);
            }
        }
        super.die(source);
    }

    /** Drop everything Joe stole, at its original quality (enchantments, durability and all). */
    private void dropStash(WorldDirector director) {
        GoblinData data = director.data();
        for (ItemStack stack : data.stash) {
            spawnAtLocation(stack);
        }
        data.stash.clear();
        data.setDirty();
    }

    // ---------------------------------------------------------------- helpers

    public static boolean carries(Player player, Item item) {
        for (ItemStack stack : com.cavmkii.seancraft.tetris.Tetris.carried(player)) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean wearsBonnet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BABY_BONNET);
    }

    public static boolean wearsMerch(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MERCH);
    }

    double randomBetween(double min, double max) {
        return min + getRandom().nextDouble() * Math.max(0, max - min);
    }

    static long secondsToTicks(double seconds) {
        return (long) (seconds * 20);
    }
}
