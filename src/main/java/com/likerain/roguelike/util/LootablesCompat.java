package com.likerain.roguelike.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lootables mod との互換レイヤー。
 *
 * Lootablesは UsageData を PersistentState として
 * server.overworld().getDataStorage() に "lootables_usage_data" キーで保存している。
 * UsageData 内部の usesMap は Map<Identifier, Map<UUID, Int>> 構造であり、
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

    // DimensionDataStorage.get(String) で UsageData インスタンスを取得するために使う
    // NeoForge: ServerLevel#getDataStorage() -> DimensionDataStorage
    // DimensionDataStorage#get(SavedData.Factory, String) は Nullable を返す
    private static Method dataStorageGetMethod = null;
    private static Object usageDataFactory = null; // UsageData.TYPE の NeoForge 等価物

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

            available = (usesMapField != null || keyMapField != null);
            LOGGER.info("[LootablesCompat] 初期化完了。usesMap={}, keyMap={}, available={}",
                    usesMapField != null, keyMapField != null, available);
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
     * @param server      MinecraftServer インスタンス（DimensionDataStorage アクセスに使用）
     */
    public static void resetUsageData(UUID playerUuid, MinecraftServer server) {
        if (!isAvailable()) return;
        if (server == null) {
            LOGGER.warn("[LootablesCompat] server が null のため resetUsageData をスキップします");
            return;
        }

        try {
            // DimensionDataStorage から "lootables_usage_data" キーで UsageData インスタンスを取得
            ServerLevel overworld = server.overworld();
            Object usageDataInstance = getUsageDataInstance(overworld);
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
                    for (Map<Object, ?> innerMap : usesMap.values()) {
                        if (innerMap != null) {
                            boolean removedUuid = innerMap.remove(playerUuid) != null;
                            boolean removedStr  = innerMap.remove(playerUuid.toString()) != null;
                            if (removedUuid || removedStr) changed = true;
                        }
                    }
                }
            }

            // keyMap: Map<Identifier, MutableMap<UUID, Int>>
            if (keyMapField != null) {
                @SuppressWarnings("unchecked")
                Map<Object, Map<Object, ?>> keyMap = (Map<Object, Map<Object, ?>>) keyMapField.get(usageDataInstance);
                if (keyMap != null) {
                    for (Map<Object, ?> innerMap : keyMap.values()) {
                        if (innerMap != null) {
                            boolean removedUuid = innerMap.remove(playerUuid) != null;
                            boolean removedStr  = innerMap.remove(playerUuid.toString()) != null;
                            if (removedUuid || removedStr) changed = true;
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
     * サーバーの DimensionDataStorage から UsageData インスタンスを取得する。
     * まだ保存されていない場合（一度も使用していないワールド）は null を返す可能性がある。
     */
    private static Object getUsageDataInstance(ServerLevel overworld) {
        try {
            // NeoForge: ServerLevel#getDataStorage() -> DimensionDataStorage
            Method getDataStorageMethod = overworld.getClass().getMethod("getDataStorage");
            Object dataStorage = getDataStorageMethod.invoke(overworld);
            if (dataStorage == null) return null;

            // DimensionDataStorage#get(SavedData.Factory, String)
            // Factory を作るのは難しいので、内部の map フィールドを直接アクセスする
            return getUsageDataFromStorage(dataStorage);
        } catch (Exception e) {
            LOGGER.warn("[LootablesCompat] DimensionDataStorage 取得失敗、フォールバック試行: {}", e.getMessage());
            return getUsageDataFallback(overworld);
        }
    }

    /**
     * DimensionDataStorage の内部 cache/map フィールドから "lootables_usage_data" を探す。
     */
    private static Object getUsageDataFromStorage(Object dataStorage) {
        try {
            // NeoForge DimensionDataStorage は内部に Map<String, SavedData> cache を持つ
            for (Field f : dataStorage.getClass().getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> cacheMap = (Map<String, Object>) f.get(dataStorage);
                if (cacheMap == null) continue;
                Object found = cacheMap.get("lootables_usage_data");
                if (found != null && usageDataClass != null && usageDataClass.isInstance(found)) {
                    return found;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[LootablesCompat] DataStorage cache 探索失敗: {}", e.getMessage());
        }
        return null;
    }

    /**
     * フォールバック: ServerLevel の persistentStateManager (Fabric系) から取得を試みる。
     */
    private static Object getUsageDataFallback(ServerLevel overworld) {
        try {
            for (Method m : overworld.getClass().getMethods()) {
                if (!m.getName().contains("StateManager") && !m.getName().contains("PersistentState")) continue;
                m.setAccessible(true);
                Object psm = m.invoke(overworld);
                if (psm == null) continue;
                // get(String) または get(Class, String) を呼ぶ
                for (Method gm : psm.getClass().getMethods()) {
                    if (!"get".equals(gm.getName())) continue;
                    Class<?>[] params = gm.getParameterTypes();
                    if (params.length == 1 && params[0] == String.class) {
                        Object result = gm.invoke(psm, "lootables_usage_data");
                        if (result != null && usageDataClass != null && usageDataClass.isInstance(result)) {
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[LootablesCompat] フォールバック取得失敗: {}", e.getMessage());
        }
        return null;
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
