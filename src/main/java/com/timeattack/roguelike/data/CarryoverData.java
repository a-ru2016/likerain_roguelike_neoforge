package com.timeattack.roguelike.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CarryoverData {
    private UUID playerUuid;
    private long bestClearTime = Long.MAX_VALUE;
    private String lockedStarterKit = "";

    // キット別永続強化アイテム: {キット名 -> [itemLine...]}
    private Map<String, List<String>> kitExtraItems = new HashMap<>();

    // 旧フィールド（後方互換のためGSON読み取り用に残す）
    private List<String> extraStarterItems = new ArrayList<>();

    private double availablePoints = 0.0;
    private int unlockedCarryoverSlots = 0;
    private List<String> carryoverItems = new ArrayList<>();

    // 達人実績を獲得したキット名のリスト
    private List<String> masterKits = new ArrayList<>();

    // 達人ボーナスアイテム（他キットに持ち込む1アイテム。SNBTまたはline文字列で保存）
    private String masterBonusItemSnbt = "";
    // 達人ボーナスアイテムの元キット名
    private String masterBonusSourceKit = "";

    public CarryoverData(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    /**
     * 旧extraStarterItemsから新kitExtraItemsへの移行処理。
     * Gsonでロードした後に呼ぶ。
     */
    public void migrateIfNeeded() {
        if (extraStarterItems != null && !extraStarterItems.isEmpty()
                && (kitExtraItems == null || kitExtraItems.isEmpty())) {
            String kit = lockedStarterKit != null && !lockedStarterKit.isEmpty()
                    ? lockedStarterKit : "_legacy";
            if (kitExtraItems == null) kitExtraItems = new HashMap<>();
            kitExtraItems.put(kit, new ArrayList<>(extraStarterItems));
            extraStarterItems = new ArrayList<>();
        }
        if (kitExtraItems == null) kitExtraItems = new HashMap<>();
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public long getBestClearTime() { return bestClearTime; }
    public void setBestClearTime(long bestClearTime) { this.bestClearTime = bestClearTime; }
    public String getLockedStarterKit() { return lockedStarterKit; }
    public void setLockedStarterKit(String lockedStarterKit) { this.lockedStarterKit = lockedStarterKit; }

    public List<String> getExtraStarterItems(String kitName) {
        if (kitExtraItems == null) kitExtraItems = new HashMap<>();
        return kitExtraItems.getOrDefault(kitName, new ArrayList<>());
    }

    public void addExtraStarterItem(String kitName, String itemLine) {
        if (kitExtraItems == null) kitExtraItems = new HashMap<>();
        kitExtraItems.computeIfAbsent(kitName, k -> new ArrayList<>()).add(itemLine);
    }

    public double getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(double availablePoints) { this.availablePoints = Math.max(0.0, availablePoints); }
    public int getUnlockedCarryoverSlots() { return unlockedCarryoverSlots; }
    public void setUnlockedCarryoverSlots(int unlockedCarryoverSlots) { this.unlockedCarryoverSlots = unlockedCarryoverSlots; }
    public List<String> getCarryoverItems() { return carryoverItems; }
    public void setCarryoverItems(List<String> carryoverItems) { this.carryoverItems = carryoverItems; }

    public List<String> getMasterKits() {
        if (masterKits == null) masterKits = new ArrayList<>();
        return masterKits;
    }
    public void addMasterKit(String kitName) {
        if (masterKits == null) masterKits = new ArrayList<>();
        if (!masterKits.contains(kitName)) masterKits.add(kitName);
    }
    public boolean hasMasterKit(String kitName) {
        if (masterKits == null) return false;
        return masterKits.contains(kitName);
    }

    public String getMasterBonusItemSnbt() { return masterBonusItemSnbt == null ? "" : masterBonusItemSnbt; }
    public void setMasterBonusItemSnbt(String snbt) { this.masterBonusItemSnbt = snbt; }
    public String getMasterBonusSourceKit() { return masterBonusSourceKit == null ? "" : masterBonusSourceKit; }
    public void setMasterBonusSourceKit(String kit) { this.masterBonusSourceKit = kit; }
}
