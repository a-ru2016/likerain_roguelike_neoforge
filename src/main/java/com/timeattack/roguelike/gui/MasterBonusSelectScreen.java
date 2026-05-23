package com.timeattack.roguelike.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.RegistryAccess;
import com.timeattack.roguelike.network.MasterBonusSelectedPayload;
import com.timeattack.roguelike.util.StarterKitParser;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 達人ボーナス選択画面（NeoForge版）
 */
public class MasterBonusSelectScreen extends Screen {

    private final List<String> masterKitNames;
    private final List<List<String>> masterKitItemLines;

    private String selectedKit = null;
    private int selectedItemIndex = -1;

    private final Map<String, List<ItemStack>> kitItemStacks = new LinkedHashMap<>();
    private final Map<String, List<String>> kitItemLinesMap = new LinkedHashMap<>();

    private Phase phase = Phase.KIT_SELECT;

    private enum Phase { KIT_SELECT, ITEM_SELECT }

    private static final int SLOT_SIZE = 18;
    private static final int ITEMS_PER_ROW = 9;

    public MasterBonusSelectScreen(List<String> masterKitNames, List<List<String>> masterKitItemLines) {
        super(Component.literal("§6§l達人ボーナス選択"));
        this.masterKitNames = masterKitNames;
        this.masterKitItemLines = masterKitItemLines;
    }

    @Override
    protected void init() {
        super.init();
        parseItems();
        buildButtons();
    }

    private void parseItems() {
        kitItemStacks.clear();
        kitItemLinesMap.clear();
        if (this.minecraft == null || this.minecraft.level == null) return;
        RegistryAccess registries = this.minecraft.level.registryAccess();

        for (int i = 0; i < masterKitNames.size(); i++) {
            String kit = masterKitNames.get(i);
            List<String> lines = (i < masterKitItemLines.size()) ? masterKitItemLines.get(i) : new ArrayList<>();
            List<ItemStack> stacks = new ArrayList<>();
            List<String> validLines = new ArrayList<>();
            for (String line : lines) {
                ItemStack stack = StarterKitParser.parseItem(line, registries);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                    validLines.add(line);
                }
            }
            kitItemStacks.put(kit, stacks);
            kitItemLinesMap.put(kit, validLines);
        }
    }

    private void buildButtons() {
        this.clearWidgets();
        if (phase == Phase.KIT_SELECT) {
            buildKitSelectButtons();
        } else {
            buildItemSelectButtons();
        }
    }

    private void buildKitSelectButtons() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - (masterKitNames.size() * 22) / 2;

        for (String kit : masterKitNames) {
            int y = startY;
            startY += 22;
            Button btn = Button.builder(Component.literal("§e★ " + kit + " の達人"), b -> {
                this.selectedKit = kit;
                this.phase = Phase.ITEM_SELECT;
                this.selectedItemIndex = -1;
                buildButtons();
            }).bounds(centerX - 80, y, 160, 20).build();
            this.addRenderableWidget(btn);
        }
    }

    private void buildItemSelectButtons() {
        Button backBtn = Button.builder(Component.literal("§7← 戻る"), b -> {
            this.phase = Phase.KIT_SELECT;
            this.selectedKit = null;
            this.selectedItemIndex = -1;
            buildButtons();
        }).bounds(8, 8, 60, 18).build();
        this.addRenderableWidget(backBtn);

        Button confirmBtn = Button.builder(Component.literal("§a決定"), b -> {
            if (selectedKit != null && selectedItemIndex >= 0) {
                List<String> lines = kitItemLinesMap.get(selectedKit);
                if (lines != null && selectedItemIndex < lines.size()) {
                    String line = lines.get(selectedItemIndex);
                    PacketDistributor.sendToServer(new MasterBonusSelectedPayload(selectedKit, line));
                    this.onClose();
                }
            }
        }).bounds(this.width / 2 - 40, this.height - 30, 80, 20).build();
        this.addRenderableWidget(confirmBtn);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        if (phase == Phase.KIT_SELECT) {
            renderKitSelectPhase(context, mouseX, mouseY);
        } else {
            renderItemSelectPhase(context, mouseX, mouseY);
        }
    }

    private void renderKitSelectPhase(GuiGraphics context, int mouseX, int mouseY) {
        context.drawCenteredString(this.font,
                Component.literal("§f達人実績を持つキットを選択"),
                this.width / 2, this.height / 2 - (masterKitNames.size() * 22) / 2 - 20, 0xFFFFFF);
        context.drawCenteredString(this.font,
                Component.literal("§7選択したキットのアイテムを1つ引き継げます"),
                this.width / 2, this.height / 2 - (masterKitNames.size() * 22) / 2 - 10, 0xAAAAAA);
    }

    private void renderItemSelectPhase(GuiGraphics context, int mouseX, int mouseY) {
        context.drawCenteredString(this.font,
                Component.literal("§e★ " + selectedKit + " §fから引き継ぐアイテムを選択"),
                this.width / 2, 30, 0xFFFFFF);
        context.drawCenteredString(this.font,
                Component.literal("§7クリックで選択、もう一度クリックで確定"),
                this.width / 2, 42, 0xAAAAAA);

        List<ItemStack> stacks = kitItemStacks.getOrDefault(selectedKit, new ArrayList<>());
        List<String> lines = kitItemLinesMap.getOrDefault(selectedKit, new ArrayList<>());

        int totalItems = stacks.size();
        int rows = (totalItems + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int gridWidth = Math.min(totalItems, ITEMS_PER_ROW) * SLOT_SIZE;
        int gridX = this.width / 2 - gridWidth / 2;
        int gridY = 60;

        ItemStack hoveredStack = ItemStack.EMPTY;
        int tooltipX = -1, tooltipY = -1;

        for (int i = 0; i < stacks.size(); i++) {
            int col = i % ITEMS_PER_ROW;
            int row = i / ITEMS_PER_ROW;
            int x = gridX + col * SLOT_SIZE;
            int y = gridY + row * SLOT_SIZE;

            boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
            boolean selected = (i == selectedItemIndex);

            if (selected) {
                context.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFDD00);
            } else if (hovered) {
                context.fill(x, y, x + 16, y + 16, 0xA0FFFFFF);
            } else {
                context.fill(x, y, x + 16, y + 16, 0x808B8B8B);
            }

            ItemStack stack = stacks.get(i);
            context.renderItem(stack, x, y);
            context.renderItemDecorations(this.font, stack, x, y);

            if (hovered) {
                hoveredStack = stack;
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
        }

        if (selectedItemIndex >= 0 && selectedItemIndex < stacks.size()) {
            context.drawCenteredString(this.font,
                    Component.literal("§a選択中: §f" + stacks.get(selectedItemIndex).getHoverName().getString()),
                    this.width / 2, gridY + rows * SLOT_SIZE + 10, 0xFFFFFF);
        }

        if (!hoveredStack.isEmpty()) {
            context.renderTooltip(this.font, hoveredStack, tooltipX, tooltipY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (phase == Phase.ITEM_SELECT && button == 0) {
            List<ItemStack> stacks = kitItemStacks.getOrDefault(selectedKit, new ArrayList<>());
            List<String> lines = kitItemLinesMap.getOrDefault(selectedKit, new ArrayList<>());

            int gridWidth = Math.min(stacks.size(), ITEMS_PER_ROW) * SLOT_SIZE;
            int gridX = this.width / 2 - gridWidth / 2;
            int gridY = 60;

            for (int i = 0; i < stacks.size(); i++) {
                int col = i % ITEMS_PER_ROW;
                int row = i / ITEMS_PER_ROW;
                int x = gridX + col * SLOT_SIZE;
                int y = gridY + row * SLOT_SIZE;

                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    if (selectedItemIndex == i) {
                        if (i < lines.size()) {
                            PacketDistributor.sendToServer(new MasterBonusSelectedPayload(selectedKit, lines.get(i)));
                            this.onClose();
                            return true;
                        }
                    } else {
                        selectedItemIndex = i;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
