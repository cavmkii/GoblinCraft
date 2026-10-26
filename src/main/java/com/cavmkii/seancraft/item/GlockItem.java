package com.cavmkii.seancraft.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * "Glock": Valheim's Arbalest with the reload removed. Here, a crossbow-strength shot every few ticks
 * for as long as you have arrows (plain or tipped). Valheim's bolts become arrows.
 */
public class GlockItem extends Item {
    private static final int COOLDOWN_TICKS = 4;

    public GlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack glock = player.getItemInHand(hand);
        boolean creative = player.getAbilities().instabuild;
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !creative) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
            return InteractionResultHolder.fail(glock);
        }

        if (!level.isClientSide) {
            ItemStack shot = ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo.copyWithCount(1);
            Arrow arrow = new Arrow(level, player, shot, glock);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 3.15f, 1f);
            arrow.setCritArrow(true);
            arrow.pickup = creative ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
            level.addFreshEntity(arrow);
            glock.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            if (!creative) {
                ammo.shrink(1);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f,
                1.3f + level.random.nextFloat() * 0.2f);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(glock);
    }

    private static ItemStack findAmmo(Player player) {
        if (isAmmo(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : com.cavmkii.seancraft.tetris.Tetris.carried(player)) {
            if (isAmmo(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isAmmo(ItemStack stack) {
        return stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW);
    }
}
