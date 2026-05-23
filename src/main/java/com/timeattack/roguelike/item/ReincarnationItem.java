/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 */
package com.timeattack.roguelike.item;

import com.timeattack.roguelike.data.PlayerRunState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ReincarnationItem
extends Item {
    public ReincarnationItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            PlayerRunState runState = PlayerRunState.get(serverPlayer.serverLevel());
            if (!runState.gameStarted) {
                player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u30b2\u30fc\u30e0\u304c\u958b\u59cb\u3055\u308c\u3066\u3044\u307e\u305b\u3093\u3002\u958b\u59cb\u306e\u672c\u3092\u4f7f\u7528\u3057\u3066\u304f\u3060\u3055\u3044\u3002"));
                return InteractionResultHolder.fail(itemstack);
            }
            runState.addVote(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(itemstack, (boolean)level.isClientSide());
    }
}

