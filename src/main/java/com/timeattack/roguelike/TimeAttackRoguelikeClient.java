package com.timeattack.roguelike;

import com.timeattack.roguelike.gui.CarryoverSelectScreen;
import com.timeattack.roguelike.gui.KitSelectScreen;
import com.timeattack.roguelike.gui.MasterBonusSelectScreen;
import com.timeattack.roguelike.network.OpenKitSelectPayload;
import com.timeattack.roguelike.network.OpenMasterBonusSelectPayload;
import com.timeattack.roguelike.network.OpenCarryoverScreenPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * クライアントサイドのイベントハンドラ（NeoForge版）
 * MOD bus: RegisterMenuScreensEvent、RegisterKeyMappingsEvent
 * GAME bus: ClientTickEvent (キー入力処理)
 */
@EventBusSubscriber(modid = TimeAttackRoguelike.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TimeAttackRoguelikeClient {

    public static KeyMapping openCarryoverKey;

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TimeAttackRoguelike.CARRYOVER_SCREEN_HANDLER.get(), CarryoverSelectScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openCarryoverKey = new KeyMapping(
                "key.timeattackroguelike.open_carryover",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.timeattackroguelike.keys"
        );
        event.register(openCarryoverKey);

        // クライアントのGameバスにTickイベントを登録
        NeoForge.EVENT_BUS.addListener(TimeAttackRoguelikeClient::onClientTick);
    }

    /**
     * クライアントTickイベント: キーバインドのチェック
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openCarryoverKey != null) {
            while (openCarryoverKey.consumeClick()) {
                PacketDistributor.sendToServer(new OpenCarryoverScreenPayload());
            }
        }
    }

    // ========== S2Cパケット受信ハンドラ ==========
    // NeoForgeではS2Cパケットを受信する際、クライアントサイドでのみ実行するために
    // このメソッドをTimeAttackRoguelike#registerPayloadsから参照して呼び出す

    /**
     * キット選択画面を開く（S2Cパケット受信時）
     */
    public static void handleOpenKitSelect(OpenKitSelectPayload payload) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().setScreen(new KitSelectScreen(payload.kits())));
    }

    /**
     * 達人ボーナス選択画面を開く（S2Cパケット受信時）
     */
    public static void handleOpenMasterBonusSelect(OpenMasterBonusSelectPayload payload) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().setScreen(
                        new MasterBonusSelectScreen(payload.masterKitNames(), payload.masterKitItemLines())));
    }
}
