package com.likerain.roguelike.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lootables mod との互換レイヤー。
 */
public class LootablesCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(LootablesCompat.class);

    /** LootablesData クラスが存在するか確認済みフラグ */
    private static boolean checked = false;
    private static boolean available = false;

    // UsageData クラスと、そのフィールド/メソッドをキャッシュ
    private static Class<?> usageDataClass = null;
    private static Field usesMapField = null;
    private static Field keyMapField = null;
    private static Method markDirtyMethod = null;

    // LootablesData インスタンスと getUsageData メソッド
    private static Object lootablesDataInstance = null;
    private static Method getUsageDataMethod = null;
    private static Field abortedChoicesField = null;

    // AttribRewards 関連
    private static Class<?> attribRewardsClass = null;
    private static Object levelDataAttachmentType = null;
    private static Class<?> levelDataClass = null;
    private static Field activeRequestsField = null;
    private static Field notifiedPlayersField = null;
    private static Field abortCooldownsField = null;

    private static void init() {
        if (checked) return;
        checked = true;
        try {
            // LootablesData$UsageData クラスを取得
            usageDataClass = Class.forName("me.fzzyhmstrs.lootables.data.LootablesData$UsageData");

            // usesMap / keyMap フィールドを取得
            for (Field f : usageDataClass.getDeclaredFields()) {
                f.setAccessible(true);
                if ("usesMap".equals(f.getName())) usesMapField = f;
                if ("keyMap".equals(f.getName())) keyMapField = f;
            }

            // markDirty() メソッド
            try {
                markDirtyMethod = usageDataClass.getMethod("setDirty");
            } catch (Exception e1) {
                try {
                    markDirtyMethod = usageDataClass.getMethod("markDirty");
                } catch (Exception e2) {
                    LOGGER.warn("[LootablesCompat] markDirty/setDirty が見つかりません。");
                }
            }

            // LootablesData (object)
            try {
                Class<?> lootablesDataClass = Class.forName("me.fzzyhmstrs.lootables.data.LootablesData");
                Field instanceField = lootablesDataClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                lootablesDataInstance = instanceField.get(null);

                getUsageDataMethod = lootablesDataClass.getDeclaredMethod("getUsageData", MinecraftServer.class);
                getUsageDataMethod.setAccessible(true);

                abortedChoicesField = lootablesDataClass.getDeclaredField("abortedChoices");
                abortedChoicesField.setAccessible(true);
            } catch (Exception e) {
                LOGGER.warn("[LootablesCompat] LootablesData の内部フィールド取得に失敗しました: {}", e.getMessage());
            }

            // AttribRewardsNeoforge (object) の初期化
            try {
                attribRewardsClass = Class.forName("me.fzzyhmstrs.lootables.AttribRewardsNeoforge");
                Field instanceField = attribRewardsClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                Object rewardsInstance = instanceField.get(null);

                // LEVEL_DATA Holder から AttachmentType を取得
                Field levelDataField = attribRewardsClass.getDeclaredField("LEVEL_DATA");
                levelDataField.setAccessible(true);
                Object levelDataHolder = levelDataField.get(null);
                Method getMethod = levelDataHolder.getClass().getMethod("get");
                levelDataAttachmentType = getMethod.invoke(levelDataHolder);

                levelDataClass = Class.forName("me.fzzyhmstrs.lootables.LevelData");

                activeRequestsField = attribRewardsClass.getDeclaredField("activeRequests");
                activeRequestsField.setAccessible(true);
                notifiedPlayersField = attribRewardsClass.getDeclaredField("notifiedPlayers");
                notifiedPlayersField.setAccessible(true);
                abortCooldownsField = attribRewardsClass.getDeclaredField("abortCooldowns");
                abortCooldownsField.setAccessible(true);

                LOGGER.info("[LootablesCompat] AttribRewards 関連の初期化に成功しました。");
            } catch (Exception e) {
                LOGGER.warn("[LootablesCompat] AttribRewards の初期化に失敗しました (非NeoForge環境か、Modバージョン違いの可能性): {}", e.getMessage());
            }

            available = (usesMapField != null || keyMapField != null) && (getUsageDataMethod != null);
        } catch (Exception e) {
            LOGGER.info("[LootablesCompat] Lootables mod が見つからないか、互換性がありません: {}", e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    /**
     * プレイヤーの使用履歴、属性ボーナス、報酬ポイントを全てリセットする。
     */
    public static void resetUsageData(UUID playerUuid, MinecraftServer server) {
        if (!isAvailable()) return;
        if (server == null) return;

        // 1. サーバー側の使用履歴 (UsageData) リセット
        resetInternalUsageData(playerUuid, server);

        // 2. プレイヤーエンティティの状態 (属性・アタッチメント) リセット
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            resetPlayerAttributes(player);
            resetPlayerRewards(player);
        }
    }

    private static void resetInternalUsageData(UUID playerUuid, MinecraftServer server) {
        try {
            Object usageDataInstance = getUsageDataInstance(server);
            if (usageDataInstance == null) return;

            // abortedChoices もクリア
            if (abortedChoicesField != null) {
                try {
                    ((Set<UUID>) abortedChoicesField.get(null)).remove(playerUuid);
                } catch (Exception e) {}
            }

            boolean changed = false;
            if (usesMapField != null) {
                @SuppressWarnings("unchecked")
                Map<Object, Map<Object, ?>> usesMap = (Map<Object, Map<Object, ?>>) usesMapField.get(usageDataInstance);
                if (usesMap != null) {
                    for (Map<Object, ?> innerMap : usesMap.values()) {
                        if (innerMap != null) {
                            boolean r1 = innerMap.remove(playerUuid) != null;
                            boolean r2 = innerMap.remove(playerUuid.toString()) != null;
                            if (r1 || r2) changed = true;
                        }
                    }
                }
            }
            if (keyMapField != null) {
                @SuppressWarnings("unchecked")
                Map<Object, Map<Object, ?>> keyMap = (Map<Object, Map<Object, ?>>) keyMapField.get(usageDataInstance);
                if (keyMap != null) {
                    for (Map<Object, ?> innerMap : keyMap.values()) {
                        if (innerMap != null) {
                            boolean r1 = innerMap.remove(playerUuid) != null;
                            boolean r2 = innerMap.remove(playerUuid.toString()) != null;
                            if (r1 || r2) changed = true;
                        }
                    }
                }
            }
            if (changed && markDirtyMethod != null) {
                markDirtyMethod.invoke(usageDataInstance);
            }
            LOGGER.info("[LootablesCompat] UsageData をリセットしました: {}", playerUuid);
        } catch (Exception e) {
            LOGGER.error("[LootablesCompat] UsageData リセット失敗", e);
        }
    }

    private static void resetPlayerAttributes(ServerPlayer player) {
        try {
            Collection<AttributeInstance> attributes = player.getAttributes().getSyncableAttributes();
            for (AttributeInstance instance : attributes) {
                List<AttributeModifier> toRemove = instance.getModifiers().stream()
                        .filter(mod -> "lootables".equals(mod.id().getNamespace()))
                        .toList();
                for (AttributeModifier mod : toRemove) {
                    instance.removeModifier(mod.id());
                    LOGGER.info("[LootablesCompat] 属性修飾子を削除しました: {} from {}", mod.id(), player.getName().getString());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[LootablesCompat] 属性リセット中にエラーが発生しました: {}", e.getMessage());
        }
    }

    private static void resetPlayerRewards(ServerPlayer player) {
        if (levelDataAttachmentType == null || levelDataClass == null) return;
        try {
            // LevelData を初期化 (cumulativeLevel=0, lastKnownLevel=0, pendingRewards=0)
            Object newLevelData = levelDataClass.getConstructor(int.class, int.class, int.class).newInstance(0, 0, 0);
            
            // player.setData(levelDataAttachmentType, newLevelData) を呼び出す
            // NeoForge の AttachmentHolder メソッド
            Method setDataMethod = player.getClass().getMethod("setData", levelDataAttachmentType.getClass(), Object.class);
            setDataMethod.invoke(player, levelDataAttachmentType, newLevelData);

            // AttribRewardsNeoforge の内部状態もクリア
            UUID uuid = player.getUUID();
            if (activeRequestsField != null) ((Set<UUID>) activeRequestsField.get(null)).remove(uuid);
            if (notifiedPlayersField != null) ((Set<UUID>) notifiedPlayersField.get(null)).remove(uuid);
            if (abortCooldownsField != null) ((Map<UUID, ?>) abortCooldownsField.get(null)).remove(uuid);

            LOGGER.info("[LootablesCompat] 報酬ポイント (LevelData) をリセットしました: {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.warn("[LootablesCompat] 報酬ポイントのリセットに失敗しました: {}", e.getMessage());
        }
    }

    /**
     * LootablesData.getUsageData(server) を呼び出して UsageData インスタンスを取得する。
     */
    private static Object getUsageDataInstance(MinecraftServer server) {
        if (lootablesDataInstance == null || getUsageDataMethod == null) {
            return null;
        }
        try {
            return getUsageDataMethod.invoke(lootablesDataInstance, server);
        } catch (Exception e) {
            LOGGER.warn("[LootablesCompat] LootablesData.getUsageData の呼び出しに失敗しました: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 後方互換: server なしの旧シグネチャ（警告ログのみ）。
     * @deprecated server を渡すオーバーロードを使ってください。
     */
    @Deprecated
    public static void resetUsageData(UUID playerUuid) {
        LOGGER.warn("[LootablesCompat] server なしの resetUsageData が呼ばれました。" +
                "MinecraftServer を渡すオーバーロードを使用してください。リセットをスキップします。");
    }
}
