package com.likerain.roguelike.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lootables mod との互換レイヤー。
 *
 * Lootablesは UsageData を PersistentState として
 * server.overworld().getDataStorage() に "lootables_usage_data" キーで保存している。
 * UsageData 内部 of usesMap は Map<Identifier, Map<UUID, Int>> 構造であり、
 * プレイヤーリセット時は「各 Identifier の内側 Map から UUID エントリを削除」する必要がある。
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

            // markDirty() メソッド（PersistentState / SavedData 共通）
            try {
                markDirtyMethod = usageDataClass.getMethod("setDirty");
            } catch (Exception e1) {
                try {
                    markDirtyMethod = usageDataClass.getMethod("markDirty");
                } catch (Exception e2) {
                    LOGGER.warn("[LootablesCompat] markDirty/setDirty が見つかりません。ダーティフラグなし。");
                }
            }

            // LootablesData (object) から INSTANCE と getUsageData メソッドを取得
            try {
                Class<?> lootablesDataClass = Class.forName("me.fzzyhmstrs.lootables.data.LootablesData");
                Field instanceField = lootablesDataClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                lootablesDataInstance = instanceField.get(null);

                getUsageDataMethod = lootablesDataClass.getDeclaredMethod("getUsageData", MinecraftServer.class);
                getUsageDataMethod.setAccessible(true);
            } catch (Exception e) {
                LOGGER.warn("[LootablesCompat] LootablesData.getUsageData メソッドの取得に失敗しました: {}", e.getMessage());
            }

            available = (usesMapField != null || keyMapField != null) && (getUsageDataMethod != null);
            LOGGER.info("[LootablesCompat] 初期化完了。usesMap={}, keyMap={}, getUsageDataMethod={}, available={}",
                    usesMapField != null, keyMapField != null, getUsageDataMethod != null, available);
        } catch (Exception e) {
            LOGGER.info("[LootablesCompat] Lootables mod が見つかりません: {}", e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    /**
     * プレイヤーの使用履歴を全てリセットする。
     *
     * @param playerUuid  対象プレイヤーの UUID
     * @param server      MinecraftServer インスタンス（UsageData 取得に使用）
     */
    public static void resetUsageData(UUID playerUuid, MinecraftServer server) {
        if (!isAvailable()) return;
        if (server == null) {
            LOGGER.warn("[LootablesCompat] server が null のため resetUsageData をスキップします");
            return;
        }

        try {
            // LootablesData.getUsageData(server) を呼び出して UsageData インスタンスを取得
            Object usageDataInstance = getUsageDataInstance(server);
            if (usageDataInstance == null) {
                LOGGER.warn("[LootablesCompat] UsageData インスタンスが取得できませんでした (未使用の可能性あり)");
                return;
            }

            boolean changed = false;

            // usesMap: Map<Identifier, MutableMap<UUID, Int>>
            // 各 Identifier の内側マップから UUID エントリを削除する
            if (usesMapField != null) {
                @SuppressWarnings("unchecked")
                Map<Object, Map<Object, ?>> usesMap = (Map<Object, Map<Object, ?>>) usesMapField.get(usageDataInstance);
                if (usesMap != null) {
                    LOGGER.info("[LootablesCompat] usesMap size={}", usesMap.size());
                    for (Map.Entry<Object, Map<Object, ?>> entry : usesMap.entrySet()) {
                        Object id = entry.getKey();
                        Map<Object, ?> innerMap = entry.getValue();
                        if (innerMap != null) {
                            LOGGER.info("[LootablesCompat] usesMap[{}] keys={}", id, innerMap.keySet());
                            for (Object key : innerMap.keySet()) {
                                LOGGER.info("[LootablesCompat]   key: {} (class: {})", key, key.getClass().getName());
                            }
                            boolean removedUuid = innerMap.remove(playerUuid) != null;
                            boolean removedStr  = innerMap.remove(playerUuid.toString()) != null;
                            if (removedUuid || removedStr) {
                                LOGGER.info("[LootablesCompat]   usesMap[{}] からプレイヤーデータを削除しました (removedUuid={}, removedStr={})", id, removedUuid, removedStr);
                                changed = true;
                            }
                        }
                    }
                }
            }

            // keyMap: Map<Identifier, MutableMap<UUID, Int>>
            if (keyMapField != null) {
                @SuppressWarnings("unchecked")
                Map<Object, Map<Object, ?>> keyMap = (Map<Object, Map<Object, ?>>) keyMapField.get(usageDataInstance);
                if (keyMap != null) {
                    LOGGER.info("[LootablesCompat] keyMap size={}", keyMap.size());
                    for (Map.Entry<Object, Map<Object, ?>> entry : keyMap.entrySet()) {
                        Object id = entry.getKey();
                        Map<Object, ?> innerMap = entry.getValue();
                        if (innerMap != null) {
                            LOGGER.info("[LootablesCompat] keyMap[{}] keys={}", id, innerMap.keySet());
                            for (Object key : innerMap.keySet()) {
                                LOGGER.info("[LootablesCompat]   key: {} (class: {})", key, key.getClass().getName());
                            }
                            boolean removedUuid = innerMap.remove(playerUuid) != null;
                            boolean removedStr  = innerMap.remove(playerUuid.toString()) != null;
                            if (removedUuid || removedStr) {
                                LOGGER.info("[LootablesCompat]   keyMap[{}] からプレイヤーデータを削除しました (removedUuid={}, removedStr={})", id, removedUuid, removedStr);
                                changed = true;
                            }
                        }
                    }
                }
            }

            if (changed && markDirtyMethod != null) {
                try {
                    markDirtyMethod.invoke(usageDataInstance);
                } catch (Exception ex) {
                    LOGGER.warn("[LootablesCompat] setDirty 呼び出し失敗: {}", ex.getMessage());
                }
            }

            LOGGER.info("[LootablesCompat] プレイヤー {} の Lootables 使用データをリセットしました (changed={})", playerUuid, changed);

        } catch (Exception e) {
            LOGGER.error("[LootablesCompat] resetUsageData 失敗", e);
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
