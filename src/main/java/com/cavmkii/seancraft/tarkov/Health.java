package com.cavmkii.seancraft.tarkov;

import com.cavmkii.seancraft.SeanCraft;
import com.cavmkii.seancraft.rpg.Rpg;
import com.cavmkii.seancraft.rpg.Skill;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Body-part health on the server: where hits land, bleeds and fractures, their effects, the Medstation's care,
 * and treating parts with meds. The parts are kept in step with the player's real health.
 */
public final class Health {
    public static final ResourceKey<DamageType> BLEED = ResourceKey.create(Registries.DAMAGE_TYPE, SeanCraft.id("bleed"));

    static final class Runtime {
        BodyPart bleeding;
        int fractureCareTicks;
        boolean bleedCareNoticed;
        boolean dirty = true;
        BodyPart chosen;
        long chosenUntil;
    }

    private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

    private Health() {}

    private static Runtime rt(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), id -> new Runtime());
    }

    static void init() {
        net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
            // Only a real night's sleep: you wake in the morning.
            if (entity instanceof ServerPlayer player && player.isAlive() && player.level().getDayTime() % 24000 < 1000) {
                restAfterSleep(player);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (entity instanceof ServerPlayer player && taken > 0 && player.isAlive()) {
                onHit(player, source, taken);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                Body.reset(Tarkov.data(newPlayer));
            }
            rt(newPlayer).dirty = true;
        });
        ServerTickEvents.END_SERVER_TICK.register(Health::tick);
        ServerPlayNetworking.registerGlobalReceiver(TarkovNet.TreatPart.TYPE, (payload, ctx) -> treatFromScreen(ctx.player(), payload.slot(), payload.part()));
    }

    // ================================================================== hits

    private static void onHit(ServerPlayer player, DamageSource source, float taken) {
        TarkovData d = Tarkov.data(player);
        Runtime rt = rt(player);
        float max = player.getMaxHealth();
        float frac = taken / Math.max(1, max);
        boolean fall = source.is(DamageTypeTags.IS_FALL);
        BodyPart part = source.is(BLEED) ? rt.bleeding : locate(player, source, fall);
        if (part == null) {
            reconcile(player); // Fire, poison and the like spread over every part.
            return;
        }
        Body.damage(d, part, taken, max);
        rt.dirty = true;
        reconcile(player);
        if (source.is(BLEED)) {
            return;
        }
        if (fall) {
            if (frac >= 0.1f && player.getRandom().nextFloat() < Math.min(0.9f, frac * 2.5f)) {
                inflict(player, Body.FRACTURE, part);
            }
            return;
        }
        Entity direct = source.getDirectEntity();
        if (direct == null) {
            return;
        }
        float cut = cutting(source);
        float crush = 1 - cut;
        if (frac >= 0.2f && player.getRandom().nextFloat() < Math.min(0.5f, (frac - 0.15f) * 2) * cut) {
            inflict(player, Body.HEAVY_BLEED, part);
        } else if (frac >= 0.06f && player.getRandom().nextFloat() < Math.min(0.6f, frac * 2.5f) * cut) {
            inflict(player, Body.LIGHT_BLEED, part);
        }
        if (part.limb() && frac >= 0.15f && player.getRandom().nextFloat() < Math.min(0.45f, (frac - 0.1f) * 1.5f) * crush) {
            inflict(player, Body.FRACTURE, part);
        }
    }

    /** How much of a hit is cutting (bleeds) rather than crushing (fractures). */
    private static float cutting(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile) {
            return 1;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return 0.2f;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES) || weapon.is(Items.TRIDENT)) {
                return 0.9f;
            }
            if (weapon.is(Items.MACE)) {
                return 0;
            }
            String type = attacker.getType().builtInRegistryHolder().key().location().getPath();
            if (type.contains("spider") || type.contains("wolf") || type.contains("cat") || type.contains("fox") || type.contains("bee")
                    || type.contains("phantom") || type.contains("guardian")) {
                return 0.8f;
            }
            return 0.25f;
        }
        return 0.5f;
    }

    /** The part a hit lands on, by height and side. Null for damage with no direction (it spreads). */
    static BodyPart locate(ServerPlayer player, DamageSource source, boolean fall) {
        if (fall) {
            return player.getRandom().nextBoolean() ? BodyPart.LEFT_LEG : BodyPart.RIGHT_LEG;
        }
        Entity direct = source.getDirectEntity();
        if (direct == null || direct.distanceToSqr(player) > 100) {
            return direct == null ? null : Body.weighted(player.getRandom());
        }
        double height;
        if (direct instanceof Projectile) {
            height = direct.getY() - player.getY();
        } else if (direct instanceof LivingEntity attacker) {
            height = attacker.getEyeY() - player.getY() - 0.3 + (player.getRandom().nextDouble() - 0.5) * 0.8;
        } else {
            height = direct.getY() - player.getY();
        }
        double h = height / Math.max(0.1, player.getBbHeight()) * 1.8;
        Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + 90);
        boolean onRight = direct.position().subtract(player.position()).dot(right) > 0;
        BodyPart arm = onRight ? BodyPart.RIGHT_ARM : BodyPart.LEFT_ARM;
        if (h > 1.55) {
            return BodyPart.HEAD;
        }
        if (h > 1.15) {
            return player.getRandom().nextFloat() < 0.3f ? arm : BodyPart.THORAX;
        }
        if (h > 0.85) {
            return player.getRandom().nextFloat() < 0.3f ? arm : BodyPart.STOMACH;
        }
        return onRight ? BodyPart.RIGHT_LEG : BodyPart.LEFT_LEG;
    }

    private static void inflict(ServerPlayer player, int flag, BodyPart part) {
        TarkovData d = Tarkov.data(player);
        if (Body.has(d, part, flag)) {
            return;
        }
        Body.set(d, part, flag, true);
        rt(player).dirty = true;
        player.displayClientMessage(Component.literal(conditionName(flag) + " - " + part.label).withStyle(ChatFormatting.RED), true);
    }

    public static String conditionName(int flag) {
        return flag == Body.HEAVY_BLEED ? "Heavy bleed" : flag == Body.LIGHT_BLEED ? "Light bleed" : "Fracture";
    }

    // ================================================================== tick

    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            TarkovData d = Tarkov.data(player);
            Runtime rt = rt(player);
            if (d.painkillerTicks > 0) {
                d.painkillerTicks--;
                if (d.painkillerTicks % 20 == 0) {
                    rt.dirty = true;
                }
            }
            if (t % 5 == 0) {
                reconcile(player);
            }
            if (t % 10 == 0) {
                applyEffects(player, d);
            }
            if (t % 40 == 0) {
                hideoutCare(player, d, rt);
                bleed(player, d, rt);
            }
            if (rt.dirty && t % 5 == 0) {
                rt.dirty = false;
                ServerPlayNetworking.send(player, new TarkovNet.BodySync(d.body.clone(), d.conditions.clone(), d.painkillerTicks));
            }
        }
    }

    /** A night's sleep heals every part that isn't destroyed by a share of its health. Destroyed parts still need surgery. */
    static void restAfterSleep(ServerPlayer player) {
        TarkovData d = Tarkov.data(player);
        float max = Math.max(1, player.getMaxHealth());
        float healed = 0;
        for (BodyPart p : BodyPart.values()) {
            healed += Body.heal(d, p, (float) (max * p.share * com.cavmkii.seancraft.Cfg.sleepHealPercent / 100), max, false);
        }
        if (healed > 0) {
            player.setHealth(Math.min(max * Body.capFraction(d.body), player.getHealth() + healed));
            rt(player).dirty = true;
            player.displayClientMessage(Component.literal("You wake up feeling better.").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /**
     * Vanilla healing (food, regeneration, potions) can't fill hearts that belong to destroyed parts. Without this
     * the reconcile below would still clamp it, but the hearts would flicker up and back down.
     */
    public static float capHeal(net.minecraft.world.entity.LivingEntity entity, float amount) {
        if (!(entity instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return amount;
        }
        float cap = player.getMaxHealth() * Body.capFraction(Tarkov.data(player).body);
        return Math.max(0, Math.min(amount, cap - player.getHealth()));
    }

    /** Keep the parts and the real health bar in step. Destroyed parts cap how high health can go. */
    static void reconcile(ServerPlayer player) {
        TarkovData d = Tarkov.data(player);
        float max = Math.max(1, player.getMaxHealth());
        float actual = Math.min(1, player.getHealth() / max);
        float expected = Body.total(d);
        float diff = actual - expected;
        if (Math.abs(diff) * max < 0.05f) {
            return;
        }
        Body.spread(d, diff, -1);
        rt(player).dirty = true;
        float now = Body.total(d);
        if (actual > now + 0.0005f) {
            player.setHealth(now * max);
        }
    }

    private static void bleed(ServerPlayer player, TarkovData d, Runtime rt) {
        float max = player.getMaxHealth();
        for (BodyPart p : BodyPart.values()) {
            float drain = 0;
            if (Body.has(d, p, Body.HEAVY_BLEED)) {
                drain += Math.max(0.5f, max * 0.02f);
            }
            if (Body.has(d, p, Body.LIGHT_BLEED)) {
                drain += Math.max(0.15f, max * 0.006f);
            }
            if (drain > 0 && player.isAlive()) {
                rt.bleeding = p;
                // Bleeding ignores the usual hurt cooldown so several parts can drain at once.
                player.invulnerableTime = 0;
                player.hurt(bleedSource(player), drain * 2);
                rt.bleeding = null;
            }
        }
    }

    private static DamageSource bleedSource(ServerPlayer player) {
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(BLEED)
                .map(h -> (Holder<DamageType>) h)
                .orElseGet(() -> player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC));
        return new DamageSource(type);
    }

    private static boolean limbHurts(TarkovData d, BodyPart p) {
        return Body.has(d, p, Body.FRACTURE) || Body.destroyed(d, p);
    }

    private static void applyEffects(ServerPlayer player, TarkovData d) {
        boolean pain = d.painkillerTicks > 0;
        boolean legs = !pain && (limbHurts(d, BodyPart.LEFT_LEG) || limbHurts(d, BodyPart.RIGHT_LEG));
        boolean arms = !pain && (limbHurts(d, BodyPart.LEFT_ARM) || limbHurts(d, BodyPart.RIGHT_ARM));
        modifier(player, Attributes.MOVEMENT_SPEED, "injured_leg", legs ? -0.35 : 0);
        modifier(player, Attributes.ATTACK_SPEED, "injured_arm", arms ? -0.3 : 0);
        modifier(player, Attributes.BLOCK_BREAK_SPEED, "injured_arm_mining", arms ? -0.3 : 0);
    }

    private static void modifier(ServerPlayer player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0) {
            instance.removeModifier(SeanCraft.id(name));
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(SeanCraft.id(name), amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Medstation 1: bleeding stops in the hideout. Medstation 2: fractures mend after a minute there. */
    private static void hideoutCare(ServerPlayer player, TarkovData d, Runtime rt) {
        Hideouts.Hideout home = Hideouts.at(player.serverLevel(), player.position());
        int med = home != null ? home.level(Module.MEDSTATION) : 0;
        if (med <= 0) {
            rt.fractureCareTicks = 0;
            return;
        }
        if (Body.hasAny(d, Body.LIGHT_BLEED | Body.HEAVY_BLEED)) {
            Body.clearAll(d, Body.LIGHT_BLEED | Body.HEAVY_BLEED);
            rt.dirty = true;
            player.displayClientMessage(Component.literal("The Medstation stops your bleeding.").withStyle(ChatFormatting.GREEN), true);
        }
        if (med >= 2 && Body.hasAny(d, Body.FRACTURE)) {
            rt.fractureCareTicks += 40;
            if (rt.fractureCareTicks >= 1200) {
                Body.clearAll(d, Body.FRACTURE);
                rt.fractureCareTicks = 0;
                rt.dirty = true;
                player.displayClientMessage(Component.literal("The Medstation sets your fractures.").withStyle(ChatFormatting.GREEN), true);
            }
        } else {
            rt.fractureCareTicks = 0;
        }
    }

    // ================================================================== meds

    /** What a med would do on a part, or false if nothing. */
    static boolean helps(TarkovData d, Med med, BodyPart p) {
        if (med.limbsOnly && !p.limb()) {
            return false;
        }
        if (Body.destroyed(d, p)) {
            // A destroyed part needs surgery to come back, but any bandage can still stop it bleeding.
            return med.revives || med.stopsLight && Body.has(d, p, Body.LIGHT_BLEED) || med.stopsHeavy && Body.has(d, p, Body.HEAVY_BLEED);
        }
        return med.stopsLight && Body.has(d, p, Body.LIGHT_BLEED)
                || med.stopsHeavy && Body.has(d, p, Body.HEAVY_BLEED)
                || med.fixesFracture && Body.has(d, p, Body.FRACTURE)
                || med.heal > 0 && d.body[p.ordinal()] < 0.999f;
    }

    /** The part to treat: the one picked on the STATUS screen in the last few seconds, else the one that needs it most. */
    public static BodyPart target(ServerPlayer player, Med med) {
        TarkovData d = Tarkov.data(player);
        if (med.painkillerTicks > 0) {
            return BodyPart.HEAD;
        }
        Runtime rt = rt(player);
        if (rt.chosen != null && player.serverLevel().getGameTime() < rt.chosenUntil && helps(d, med, rt.chosen)) {
            return rt.chosen;
        }
        BodyPart best = null;
        int bestScore = 0;
        for (BodyPart p : BodyPart.values()) {
            if (!helps(d, med, p)) {
                continue;
            }
            int score = (Body.destroyed(d, p) ? 1000 : 0) + (Body.has(d, p, Body.HEAVY_BLEED) ? 500 : 0)
                    + (Body.has(d, p, Body.LIGHT_BLEED) ? 200 : 0) + (Body.has(d, p, Body.FRACTURE) ? 100 : 0)
                    + (int) ((1 - d.body[p.ordinal()]) * 99);
            if (score > bestScore) {
                best = p;
                bestScore = score;
            }
        }
        return best;
    }

    /** Applies a med to a part. Brewing makes heals and painkillers stronger. Returns true if it did anything. */
    public static boolean treat(ServerPlayer player, Med med, BodyPart part) {
        if (part == null) {
            return false;
        }
        TarkovData d = Tarkov.data(player);
        double brewing = 1 + Skill.BREWING.effectValue(Rpg.data(player).skillLevel(Skill.BREWING)) / 100.0;
        boolean did = false;
        if (med.painkillerTicks > 0) {
            d.painkillerTicks = Math.max(d.painkillerTicks, (int) (med.painkillerTicks * brewing));
            did = true;
        } else {
            if (!helps(d, med, part)) {
                return false;
            }
            float max = player.getMaxHealth();
            if (med.revives && Body.destroyed(d, part)) {
                Body.heal(d, part, 0, max, true);
                did = true;
            }
            if (med.stopsLight && Body.has(d, part, Body.LIGHT_BLEED) || med.stopsHeavy && Body.has(d, part, Body.HEAVY_BLEED)) {
                Body.set(d, part, Body.LIGHT_BLEED, false);
                if (med.stopsHeavy) {
                    Body.set(d, part, Body.HEAVY_BLEED, false);
                }
                did = true;
            }
            if (med.fixesFracture && Body.has(d, part, Body.FRACTURE)) {
                Body.set(d, part, Body.FRACTURE, false);
                did = true;
            }
            if (med.heal > 0) {
                float used = Body.heal(d, part, (float) (max * med.heal * brewing), max, false);
                if (used > 0) {
                    player.setHealth(Math.min(max, player.getHealth() + used));
                    did = true;
                }
            }
        }
        if (did) {
            rt(player).dirty = true;
            Rpg.addSkillXp(player, Skill.BREWING, 5);
            player.displayClientMessage(Component.literal(med.label + (med.painkillerTicks > 0 ? " taken." : " used on " + part.label.toLowerCase() + "."))
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return did;
    }

    /** STATUS screen: use a carried med (index into Tetris.carried) on a chosen part right away. */
    private static void treatFromScreen(ServerPlayer player, int slot, int partIndex) {
        java.util.List<ItemStack> carried = com.cavmkii.seancraft.tetris.Tetris.carried(player);
        if (slot < 0 || slot >= carried.size() || partIndex < 0 || partIndex >= BodyPart.values().length) {
            return;
        }
        ItemStack stack = carried.get(slot);
        if (!(stack.getItem() instanceof MedItem medItem)) {
            return;
        }
        BodyPart part = BodyPart.values()[partIndex];
        if (treat(player, medItem.med, part) && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
