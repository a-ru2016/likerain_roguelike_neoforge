/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package com.likerain.roguelike.gui;

import com.likerain.roguelike.network.MasterBonusSelectedPayload;
import com.likerain.roguelike.util.StarterKitParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class MasterBonusSelectScreen
extends Screen {
    private final List<String> masterKitNames;
    private final List<List<String>> masterKitItemLines;
    private String selectedKit = null;
    private int selectedItemIndex = -1;
    private final Map<String, List<ItemStack>> kitItemStacks = new LinkedHashMap<String, List<ItemStack>>();
    private final Map<String, List<String>> kitItemLinesMap = new LinkedHashMap<String, List<String>>();
    private Phase phase = Phase.KIT_SELECT;
    private static final int SLOT_SIZE = 18;
    private static final int ITEMS_PER_ROW = 9;

    public MasterBonusSelectScreen(List<String> masterKitNames, List<List<String>> masterKitItemLines) {
        super((Component)Component.literal((String)"\u00a76\u00a7l\u9054\u4eba\u30dc\u30fc\u30ca\u30b9\u9078\u629e"));
        this.masterKitNames = masterKitNames;
        this.masterKitItemLines = masterKitItemLines;
    }

    protected void init() {
        super.init();
        this.parseItems();
        this.buildButtons();
    }

    private void parseItems() {
        this.kitItemStacks.clear();
        this.kitItemLinesMap.clear();
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        RegistryAccess registries = this.minecraft.level.registryAccess();
        for (int i = 0; i < this.masterKitNames.size(); ++i) {
            String kit = this.masterKitNames.get(i);
            List<String> lines = i < this.masterKitItemLines.size() ? this.masterKitItemLines.get(i) : new ArrayList<>();
            ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
            ArrayList<String> validLines = new ArrayList<String>();
            for (String line : lines) {
                ItemStack stack = StarterKitParser.parseItem(line, registries);
                if (stack.isEmpty()) continue;
                stacks.add(stack);
                validLines.add(line);
            }
            this.kitItemStacks.put(kit, stacks);
            this.kitItemLinesMap.put(kit, validLines);
        }
    }

    private void buildButtons() {
        this.clearWidgets();
        if (this.phase == Phase.KIT_SELECT) {
            this.buildKitSelectButtons();
        } else {
            this.buildItemSelectButtons();
        }
    }

    private void buildKitSelectButtons() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - this.masterKitNames.size() * 22 / 2;
        for (String kit : this.masterKitNames) {
            int y = startY;
            startY += 22;
            Button btn = Button.builder((Component)Component.literal((String)("\u00a7e\u2605 " + kit + " \u306e\u9054\u4eba")), b -> {
                this.selectedKit = kit;
                this.phase = Phase.ITEM_SELECT;
                this.selectedItemIndex = -1;
                this.buildButtons();
            }).bounds(centerX - 80, y, 160, 20).build();
            this.addRenderableWidget(btn);
        }
    }

    private void buildItemSelectButtons() {
        Button backBtn = Button.builder((Component)Component.literal((String)"\u00a77\u2190 \u623b\u308b"), b -> {
            this.phase = Phase.KIT_SELECT;
            this.selectedKit = null;
            this.selectedItemIndex = -1;
            this.buildButtons();
        }).bounds(8, 8, 60, 18).build();
        this.addRenderableWidget(backBtn);
        Button confirmBtn = Button.builder((Component)Component.literal((String)"\u00a7a\u6c7a\u5b9a"), b -> {
            List<String> lines;
            if (this.selectedKit != null && this.selectedItemIndex >= 0 && (lines = this.kitItemLinesMap.get(this.selectedKit)) != null && this.selectedItemIndex < lines.size()) {
                String line = lines.get(this.selectedItemIndex);
                PacketDistributor.sendToServer((CustomPacketPayload)new MasterBonusSelectedPayload(this.selectedKit, line), (CustomPacketPayload[])new CustomPacketPayload[0]);
                this.onClose();
            }
        }).bounds(this.width / 2 - 40, this.height - 30, 80, 20).build();
        this.addRenderableWidget(confirmBtn);
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        if (this.phase == Phase.KIT_SELECT) {
            this.renderKitSelectPhase(context, mouseX, mouseY);
        } else {
            this.renderItemSelectPhase(context, mouseX, mouseY);
        }
    }

    private void renderKitSelectPhase(GuiGraphics context, int mouseX, int mouseY) {
        context.drawCenteredString(this.font, (Component)Component.literal((String)"\u00a7f\u9054\u4eba\u5b9f\u7e3e\u3092\u6301\u3064\u30ad\u30c3\u30c8\u3092\u9078\u629e"), this.width / 2, this.height / 2 - this.masterKitNames.size() * 22 / 2 - 20, 0xFFFFFF);
        context.drawCenteredString(this.font, (Component)Component.literal((String)"\u00a77\u9078\u629e\u3057\u305f\u30ad\u30c3\u30c8\u306e\u30a2\u30a4\u30c6\u30e0\u30921\u3064\u5f15\u304d\u7d99\u3052\u307e\u3059"), this.width / 2, this.height / 2 - this.masterKitNames.size() * 22 / 2 - 10, 0xAAAAAA);
    }

    private void renderItemSelectPhase(GuiGraphics context, int mouseX, int mouseY) {
        context.drawCenteredString(this.font, (Component)Component.literal((String)("\u00a7e\u2605 " + this.selectedKit + " \u00a7f\u304b\u3089\u5f15\u304d\u7d99\u3050\u30a2\u30a4\u30c6\u30e0\u3092\u9078\u629e")), this.width / 2, 30, 0xFFFFFF);
        context.drawCenteredString(this.font, (Component)Component.literal((String)"\u00a77\u30af\u30ea\u30c3\u30af\u3067\u9078\u629e\u3001\u3082\u3046\u4e00\u5ea6\u30af\u30ea\u30c3\u30af\u3067\u78ba\u5b9a"), this.width / 2, 42, 0xAAAAAA);
        List stacks = this.kitItemStacks.getOrDefault(this.selectedKit, new ArrayList());
        List lines = this.kitItemLinesMap.getOrDefault(this.selectedKit, new ArrayList());
        int totalItems = stacks.size();
        int rows = (totalItems + 9 - 1) / 9;
        int gridWidth = Math.min(totalItems, 9) * 18;
        int gridX = this.width / 2 - gridWidth / 2;
        int gridY = 60;
        ItemStack hoveredStack = ItemStack.EMPTY;
        int tooltipX = -1;
        int tooltipY = -1;
        for (int i = 0; i < stacks.size(); ++i) {
            boolean selected;
            int col = i % 9;
            int row = i / 9;
            int x = gridX + col * 18;
            int y = gridY + row * 18;
            boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
            boolean bl = selected = i == this.selectedItemIndex;
            if (selected) {
                context.fill(x - 1, y - 1, x + 17, y + 17, -8960);
            } else if (hovered) {
                context.fill(x, y, x + 16, y + 16, -1593835521);
            } else {
                context.fill(x, y, x + 16, y + 16, -2138338421);
            }
            ItemStack stack = (ItemStack)stacks.get(i);
            context.renderItem(stack, x, y);
            context.renderItemDecorations(this.font, stack, x, y);
            if (!hovered) continue;
            hoveredStack = stack;
            tooltipX = mouseX;
            tooltipY = mouseY;
        }
        if (this.selectedItemIndex >= 0 && this.selectedItemIndex < stacks.size()) {
            context.drawCenteredString(this.font, (Component)Component.literal((String)("\u00a7a\u9078\u629e\u4e2d: \u00a7f" + ((ItemStack)stacks.get(this.selectedItemIndex)).getHoverName().getString())), this.width / 2, gridY + rows * 18 + 10, 0xFFFFFF);
        }
        if (!hoveredStack.isEmpty()) {
            context.renderTooltip(this.font, hoveredStack, tooltipX, tooltipY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.phase == Phase.ITEM_SELECT && button == 0) {
            List stacks = this.kitItemStacks.getOrDefault(this.selectedKit, new ArrayList());
            List lines = this.kitItemLinesMap.getOrDefault(this.selectedKit, new ArrayList());
            int gridWidth = Math.min(stacks.size(), 9) * 18;
            int gridX = this.width / 2 - gridWidth / 2;
            int gridY = 60;
            for (int i = 0; i < stacks.size(); ++i) {
                int col = i % 9;
                int row = i / 9;
                int x = gridX + col * 18;
                int y = gridY + row * 18;
                if (!(mouseX >= (double)x) || !(mouseX < (double)(x + 16)) || !(mouseY >= (double)y) || !(mouseY < (double)(y + 16))) continue;
                if (this.selectedItemIndex == i) {
                    if (i >= lines.size()) continue;
                    PacketDistributor.sendToServer((CustomPacketPayload)new MasterBonusSelectedPayload(this.selectedKit, (String)lines.get(i)), (CustomPacketPayload[])new CustomPacketPayload[0]);
                    this.onClose();
                    return true;
                }
                this.selectedItemIndex = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static enum Phase {
        KIT_SELECT,
        ITEM_SELECT;

    }
}

