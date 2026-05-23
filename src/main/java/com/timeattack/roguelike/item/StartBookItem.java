/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package com.timeattack.roguelike.item;

import com.timeattack.roguelike.config.ModConfig;
import com.timeattack.roguelike.data.PlayerRunState;
import com.timeattack.roguelike.network.OpenStartGameScreenPayload;
import com.timeattack.roguelike.util.StarterKitParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class StartBookItem
extends Item {
    public StartBookItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            PlayerRunState runState = PlayerRunState.get(serverPlayer.serverLevel());
            if (runState.gameStarted) {
                player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u30b2\u30fc\u30e0\u306f\u3059\u3067\u306b\u958b\u59cb\u3055\u308c\u3066\u3044\u307e\u3059\u3002"));
                return InteractionResultHolder.fail(itemstack);
            }
            List<String> kits = StarterKitParser.getAvailableKits();
            if (kits.isEmpty()) {
                kits = new ArrayList<String>(ModConfig.load().kitInitialItems.keySet());
            }
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)new OpenStartGameScreenPayload(kits), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
        return InteractionResultHolder.sidedSuccess(itemstack, (boolean)level.isClientSide());
    }
}

