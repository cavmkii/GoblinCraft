package com.cavmkii.seancraft.tarkov;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A bigger secure pouch. Use it to install it; the pouch grid grows and keeps what is in it. */
public class SecureItem extends Item {
    public final SecureTier tier;

    public SecureItem(SecureTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            TarkovData data = Tarkov.data(serverPlayer);
            if (data.tier.ordinal() >= tier.ordinal()) {
                serverPlayer.displayClientMessage(Component.literal("Your secure pouch is already " + data.tier.label + "."), true);
                return InteractionResultHolder.fail(stack);
            }
            data.tier = tier;
            stack.shrink(1);
            serverPlayer.displayClientMessage(Component.literal("Secure pouch upgraded to " + tier.label + " ("
                    + tier.width + "x" + tier.height + ")."), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
