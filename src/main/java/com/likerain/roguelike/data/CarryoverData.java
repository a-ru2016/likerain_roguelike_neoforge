/*
 * Decompiled with CFR 0.152.
 */
package com.likerain.roguelike.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CarryoverData {
    private UUID playerUuid;
    private long bestClearTime = Long.MAX_VALUE;
    private String lockedStarterKit = "";
    private Map<String, List<String>> kitExtraItems = new HashMap<String, List<String>>();
    private List<String> extraStarterItems = new ArrayList<String>();
    private double availablePoints = 0.0;
    private int unlockedCarryoverSlots = 0;
    private List<String> carryoverItems = new ArrayList<String>();
    private boolean[] unlockedArmorSlots = new boolean[4];
    private List<String> armorCarryoverItems = new ArrayList<String>();
    private boolean unlockedAccessorySlot = false;
    private String accessoryCarryoverItem = "";
    private String freeCarryoverItem = "";
    private List<String> masterKits = new ArrayList<String>();
    private String masterBonusItemSnbt = "";
    private String masterBonusSourceKit = "";

    public CarryoverData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.unlockedArmorSlots = new boolean[4];
        this.unlockedAccessorySlot = false;
        this.armorCarryoverItems = new ArrayList<String>(List.of("", "", "", ""));
        this.accessoryCarryoverItem = "";
        this.freeCarryoverItem = "";
    }

    public void migrateIfNeeded() {
        if (this.extraStarterItems != null && !this.extraStarterItems.isEmpty() && (this.kitExtraItems == null || this.kitExtraItems.isEmpty())) {
            String kit;
            String string = kit = this.lockedStarterKit != null && !this.lockedStarterKit.isEmpty() ? this.lockedStarterKit : "_legacy";
            if (this.kitExtraItems == null) {
                this.kitExtraItems = new HashMap<String, List<String>>();
            }
            this.kitExtraItems.put(kit, new ArrayList<String>(this.extraStarterItems));
            this.extraStarterItems = new ArrayList<String>();
        }
        if (this.kitExtraItems == null) {
            this.kitExtraItems = new HashMap<String, List<String>>();
        }
        if (this.unlockedArmorSlots == null || this.unlockedArmorSlots.length < 4) {
            boolean[] newArr = new boolean[4];
            if (this.unlockedArmorSlots != null) {
                System.arraycopy(this.unlockedArmorSlots, 0, newArr, 0, Math.min(this.unlockedArmorSlots.length, 4));
            }
            this.unlockedArmorSlots = newArr;
        }
        if (this.armorCarryoverItems == null) {
            this.armorCarryoverItems = new ArrayList<String>(List.of("", "", "", ""));
        } else {
            while (this.armorCarryoverItems.size() < 4) {
                this.armorCarryoverItems.add("");
            }
        }
        if (this.accessoryCarryoverItem == null) {
            this.accessoryCarryoverItem = "";
        }
        if (this.freeCarryoverItem == null) {
            this.freeCarryoverItem = "";
        }
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public long getBestClearTime() {
        return this.bestClearTime;
    }

    public void setBestClearTime(long bestClearTime) {
        this.bestClearTime = bestClearTime;
    }

    public String getLockedStarterKit() {
        return this.lockedStarterKit;
    }

    public void setLockedStarterKit(String lockedStarterKit) {
        this.lockedStarterKit = lockedStarterKit;
    }

    public List<String> getExtraStarterItems(String kitName) {
        if (this.kitExtraItems == null) {
            this.kitExtraItems = new HashMap<String, List<String>>();
        }
        return this.kitExtraItems.getOrDefault(kitName, new ArrayList());
    }

    public void addExtraStarterItem(String kitName, String itemLine) {
        if (this.kitExtraItems == null) {
            this.kitExtraItems = new HashMap<String, List<String>>();
        }
        this.kitExtraItems.computeIfAbsent(kitName, k -> new ArrayList()).add(itemLine);
    }

    public double getAvailablePoints() {
        return this.availablePoints;
    }

    public void setAvailablePoints(double availablePoints) {
        this.availablePoints = Math.max(0.0, availablePoints);
    }

    public int getUnlockedCarryoverSlots() {
        return this.unlockedCarryoverSlots;
    }

    public void setUnlockedCarryoverSlots(int unlockedCarryoverSlots) {
        this.unlockedCarryoverSlots = unlockedCarryoverSlots;
    }

    public List<String> getCarryoverItems() {
        return this.carryoverItems;
    }

    public void setCarryoverItems(List<String> carryoverItems) {
        this.carryoverItems = carryoverItems;
    }

    public boolean isArmorSlotUnlocked(int index) {
        if (this.unlockedArmorSlots == null || this.unlockedArmorSlots.length <= index) {
            boolean[] newArr = new boolean[4];
            if (this.unlockedArmorSlots != null) {
                System.arraycopy(this.unlockedArmorSlots, 0, newArr, 0, this.unlockedArmorSlots.length);
            }
            this.unlockedArmorSlots = newArr;
        }
        return this.unlockedArmorSlots[index];
    }

    public void unlockArmorSlot(int index) {
        if (this.unlockedArmorSlots == null || this.unlockedArmorSlots.length <= index) {
            this.isArmorSlotUnlocked(index);
        }
        this.unlockedArmorSlots[index] = true;
    }

    public boolean isAccessorySlotUnlocked() {
        return this.unlockedAccessorySlot;
    }

    public void unlockAccessorySlot() {
        this.unlockedAccessorySlot = true;
    }

    public List<String> getArmorCarryoverItems() {
        if (this.armorCarryoverItems == null) {
            this.armorCarryoverItems = new ArrayList<String>();
        }
        while (this.armorCarryoverItems.size() < 4) {
            this.armorCarryoverItems.add("");
        }
        return this.armorCarryoverItems;
    }

    public void setArmorCarryoverItems(List<String> items) {
        this.armorCarryoverItems = items;
    }

    public String getAccessoryCarryoverItem() {
        return this.accessoryCarryoverItem == null ? "" : this.accessoryCarryoverItem;
    }

    public void setAccessoryCarryoverItem(String item) {
        this.accessoryCarryoverItem = item;
    }

    public String getFreeCarryoverItem() {
        return this.freeCarryoverItem == null ? "" : this.freeCarryoverItem;
    }

    public void setFreeCarryoverItem(String item) {
        this.freeCarryoverItem = item;
    }

    public List<String> getMasterKits() {
        if (this.masterKits == null) {
            this.masterKits = new ArrayList<String>();
        }
        return this.masterKits;
    }

    public void addMasterKit(String kitName) {
        if (this.masterKits == null) {
            this.masterKits = new ArrayList<String>();
        }
        if (!this.masterKits.contains(kitName)) {
            this.masterKits.add(kitName);
        }
    }

    public boolean hasMasterKit(String kitName) {
        if (this.masterKits == null) {
            return false;
        }
        return this.masterKits.contains(kitName);
    }

    public String getMasterBonusItemSnbt() {
        return this.masterBonusItemSnbt == null ? "" : this.masterBonusItemSnbt;
    }

    public void setMasterBonusItemSnbt(String snbt) {
        this.masterBonusItemSnbt = snbt;
    }

    public String getMasterBonusSourceKit() {
        return this.masterBonusSourceKit == null ? "" : this.masterBonusSourceKit;
    }

    public void setMasterBonusSourceKit(String kit) {
        this.masterBonusSourceKit = kit;
    }

    public void resetAll() {
        this.availablePoints = 0.0;
        this.unlockedCarryoverSlots = 0;
        this.carryoverItems = new ArrayList<String>();
        this.unlockedArmorSlots = new boolean[4];
        this.armorCarryoverItems = new ArrayList<String>(List.of("", "", "", ""));
        this.unlockedAccessorySlot = false;
        this.accessoryCarryoverItem = "";
        this.freeCarryoverItem = "";
        this.kitExtraItems = new HashMap<String, List<String>>();
        this.masterKits = new ArrayList<String>();
        this.masterBonusItemSnbt = "";
        this.masterBonusSourceKit = "";
    }
}

