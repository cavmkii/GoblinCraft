package com.cavmkii.seancraft.entity;

import com.cavmkii.seancraft.Cfg;
import com.cavmkii.seancraft.Lines;
import com.cavmkii.seancraft.Speech;
import com.cavmkii.seancraft.item.ModItems;
import com.cavmkii.seancraft.rpg.RpgContent;
import com.cavmkii.seancraft.world.WorldDirector;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Sean: a travelling merchant. Walks to a far spot, camps there for a few minutes, moves on; stops
 * whenever a customer is near. Right-click for the normal villager trade screen (paid in emeralds).
 * Very tanky, and shrugs off most damage from ordinary mobs.
 */
public class Sean extends AbstractVillager {
    public static final String NAME = "Sean";

    private final Wander wander = new Wander(this);
    private final Map<UUID, Long> greeted = new HashMap<>();
    private long nextThink;
    private long campUntil;
    private long nextPitch;

    public Sean(EntityType<? extends Sean> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (!level.isClientSide) {
            setCustomName(Component.literal(NAME));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    public void applyConfiguredHealth() {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Mth.clamp(Cfg.seanHealth, 20, 1024));
        }
        setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            SpawnGroupData spawnGroupData) {
        // Never a baby Sean.
        return super.finalizeSpawn(level, difficulty, spawnType, new AgeableMob.AgeableMobGroupData(false));
    }

    void say(String line, boolean shout) {
        Speech.say(this, NAME, ChatFormatting.AQUA, line, shout);
    }

    // ---------------------------------------------------------------- trading

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            player.awardStat(Stats.TALKED_TO_VILLAGER);
        }
        if (!getOffers().isEmpty()) {
            setTradingPlayer(player);
            openTradingScreen(player, Component.literal(NAME), 1);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Sean's stock. Valheim coin prices converted to emeralds at roughly 25 coins each; 67 keeps its joke.
     * Uses are effectively unlimited so the shop never sells out.
     */
    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        offers.add(offer(new ItemCost(Items.EMERALD, 1), new ItemStack(ModItems.SODA, 5)));
        offers.add(offer(new ItemCost(Items.EMERALD, 3), new ItemStack(ModItems.MERCH)));
        offers.add(offer(new ItemCost(Items.EMERALD, 30), new ItemStack(ModItems.GLOCK)));
        offers.add(offer(new ItemCost(Items.EMERALD, 12), new ItemStack(ModItems.RESTRAINING_ORDER)));
        offers.add(offer(new ItemCost(Items.EMERALD, 5), new ItemStack(ModItems.DIDDY_OIL, 3)));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD_BLOCK, 6), Optional.of(new ItemCost(Items.DIAMOND, 7)),
                new ItemStack(ModItems.SIXTY_SEVEN), 9999, 0, 0f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD_BLOCK, 32), Optional.of(new ItemCost(Items.DIAMOND_BLOCK, 2)),
                new ItemStack(RpgContent.ORB_OF_REGRET), 9999, 0, 0f));
    }

    private static MerchantOffer offer(ItemCost cost, ItemStack result) {
        return new MerchantOffer(cost, Optional.empty(), result, 9999, 0, 0f);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // Sean doesn't level up.
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    // ---------------------------------------------------------------- damage

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source.getEntity() instanceof Joe) {
                amount = Fight.clampToDuelFloor(this, amount);
                if (amount <= 0) {
                    return false;
                }
            } else if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)) {
                amount *= (float) Cfg.seanNonPlayerDamageTaken;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !isRemoved() && !dead) {
            WorldDirector director = WorldDirector.get();
            if (director != null) {
                director.onDeath(WorldDirector.Kind.SEAN, this);
            }
        }
        super.die(source);
    }

    // ---------------------------------------------------------------- behaviour

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        WorldDirector director = WorldDirector.get();
        long now = level().getGameTime();
        if (director == null || !isAlive() || now < nextThink) {
            return;
        }
        nextThink = now + 10;

        if (!director.claim(WorldDirector.Kind.SEAN, this)) {
            discard();
            return;
        }

        Vec3 pos = position();
        if (isInFight(now) || isTrading()) {
            getNavigation().stop();
            return;
        }

        if (director.war.isActive()) {
            // Hold position behind his Security and shout at everyone.
            wander.hold();
            if (now >= nextPitch && level().getNearestPlayer(this, 60) != null) {
                nextPitch = now + 240 + getRandom().nextInt(200);
                say(Lines.pick(Lines.SEAN_WAR), true);
            }
            return;
        }

        Player customer = level().getNearestPlayer(this, 15);
        if (customer != null) {
            // Open for business: stand still while anyone is browsing.
            wander.hold();
            campUntil = Math.max(campUntil, now + 600);
            greet(customer, now);
            pitch(now);
            return;
        }

        if (now < campUntil) {
            wander.step(0.6);
            return;
        }

        if (wander.step(0.7)) {
            // Arrived (or gave up): camp for a while, then move on.
            if (campUntil > 0) {
                campUntil = 0;
                Vec3 next = Wander.pickLandPoint((ServerLevel) level(), pos, 120, 300, getRandom());
                if (next != null) {
                    wander.setDestination(next);
                }
            } else {
                campUntil = now + 3600 + getRandom().nextInt(3600);
                wander.setAnchor(pos, 3);
            }
        }
    }

    private boolean isInFight(long now) {
        for (Joe joe : level().getEntitiesOfClass(Joe.class, getBoundingBox().inflate(Cfg.fightTriggerRange * 2.5))) {
            if (joe.fight().isActive(now)) {
                return true;
            }
        }
        return false;
    }

    private void greet(Player customer, long now) {
        Long last = greeted.get(customer.getUUID());
        if (last != null && now - last < 3600) {
            return;
        }
        greeted.put(customer.getUUID(), now);
        say(Lines.pick(Lines.SEAN_GREET, customer.getName().getString()), false);
        nextPitch = now + 500 + getRandom().nextInt(400);
    }

    private void pitch(long now) {
        if (now < nextPitch) {
            return;
        }
        nextPitch = now + 600 + getRandom().nextInt(600);
        say(Lines.pick(Lines.SEAN_PITCH), false);
    }
}
