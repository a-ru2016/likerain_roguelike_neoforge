/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.inventory.MenuType
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
 *  net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package com.likerain.roguelike;

import com.mojang.blaze3d.platform.InputConstants;
import com.likerain.roguelike.LikerainRoguelike;
import com.likerain.roguelike.gui.CarryoverSelectScreen;
import com.likerain.roguelike.gui.KitSelectScreen;
import com.likerain.roguelike.gui.MasterBonusSelectScreen;
import com.likerain.roguelike.gui.StartGameScreen;
import com.likerain.roguelike.network.OpenCarryoverScreenPayload;
import com.likerain.roguelike.network.OpenKitSelectPayload;
import com.likerain.roguelike.network.OpenMasterBonusSelectPayload;
import com.likerain.roguelike.network.OpenStartGameScreenPayload;
import com.likerain.roguelike.network.SyncRunStatePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="likerain_roguelike", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class LikerainRoguelikeClient {
    public static KeyMapping openCarryoverKey;

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register((MenuType)LikerainRoguelike.CARRYOVER_SCREEN_HANDLER.get(), CarryoverSelectScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openCarryoverKey = new KeyMapping("key.likerain_roguelike.open_carryover", InputConstants.Type.KEYSYM, 79, "category.likerain_roguelike.keys");
        event.register(openCarryoverKey);
        NeoForge.EVENT_BUS.addListener(LikerainRoguelikeClient::onClientTick);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (openCarryoverKey != null) {
            while (openCarryoverKey.consumeClick()) {
                if (!(Minecraft.getInstance().screen instanceof CarryoverSelectScreen)) {
                    PacketDistributor.sendToServer((CustomPacketPayload)new OpenCarryoverScreenPayload(), (CustomPacketPayload[])new CustomPacketPayload[0]);
                }
            }
        }
    }

    public static void handleOpenKitSelect(OpenKitSelectPayload payload) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen((Screen)new KitSelectScreen(payload.kits())));
    }

    public static void handleOpenMasterBonusSelect(OpenMasterBonusSelectPayload payload) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen((Screen)new MasterBonusSelectScreen(payload.masterKitNames(), payload.masterKitItemLines())));
    }

    public static void handleOpenStartGameScreen(OpenStartGameScreenPayload payload) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen((Screen)new StartGameScreen(payload.kits())));
    }

    public static void handleSyncRunState(SyncRunStatePayload payload) {
    }
}

