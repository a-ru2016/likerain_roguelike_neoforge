/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractSliderButton
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

import com.likerain.roguelike.config.ModConfig;
import com.likerain.roguelike.network.StartGamePayload;
import com.likerain.roguelike.util.StarterKitParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class StartGameScreen
extends Screen {
    private final List<String> kits;
    private String selectedKit = null;
    private float difficulty = 0.0f;
    private final Map<String, Map<String, ItemStack>> kitItems = new HashMap<String, Map<String, ItemStack>>();
    private String activePreviewKit = null;

    public StartGameScreen(List<String> kits) {
        super((Component)Component.literal((String)"\u30b2\u30fc\u30e0\u958b\u59cb\u8a2d\u5b9a"));
        this.kits = kits;
        if (!kits.isEmpty()) {
            this.selectedKit = kits.get(0);
            this.activePreviewKit = kits.get(0);
        }
    }

    protected void init() {
        super.init();
        int startY = this.height / 6;
        int listX = this.width / 4 - 60;
        for (String kit : this.kits) {
            Button btn2 = Button.builder((Component)Component.literal((String)kit), button -> {
                this.selectedKit = kit;
                this.activePreviewKit = kit;
            }).bounds(listX, startY, 120, 20).build();
            this.addRenderableWidget(btn2);
            startY += 24;
        }
        int sliderX = this.width / 4 - 60;
        int sliderY = startY + 10;
        DifficultySlider slider = new DifficultySlider(sliderX, sliderY, 120, 20, this, (double)this.difficulty / 9.0);
        this.addRenderableWidget(slider);
        int startButtonX = this.width / 4 - 60;
        int startButtonY = sliderY + 30;
        Button startBtn = Button.builder((Component)Component.literal((String)"\u30b2\u30fc\u30e0\u958b\u59cb"), btn -> {
            if (this.selectedKit != null) {
                PacketDistributor.sendToServer((CustomPacketPayload)new StartGamePayload(this.selectedKit, this.difficulty), (CustomPacketPayload[])new CustomPacketPayload[0]);
                this.onClose();
            }
        }).bounds(startButtonX, startButtonY, 120, 20).build();
        this.addRenderableWidget(startBtn);
        if (this.minecraft != null && this.minecraft.level != null) {
            RegistryAccess registries = this.minecraft.level.registryAccess();
            ModConfig config = ModConfig.load();
            for (String kit : this.kits) {
                List<String> list;
                HashMap<String, ItemStack> stacks = new HashMap<String, ItemStack>();
                Map<String, String> initials = StarterKitParser.getInitialKitItems(kit);
                if (initials.isEmpty() && (list = config.kitInitialItems.get(kit)) != null) {
                    for (int i = 0; i < list.size(); ++i) {
                        initials.put(String.valueOf(i), list.get(i));
                    }
                }
                for (Map.Entry<String, String> entry : initials.entrySet()) {
                    ItemStack stack = StarterKitParser.parseItem(entry.getValue(), registries);
                    if (stack.isEmpty()) continue;
                    stacks.put(entry.getKey(), stack);
                }
                this.kitItems.put(kit, stacks);
            }
        }
    }

    public void setDifficulty(float difficulty) {
        this.difficulty = difficulty;
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        String kitText = "\u9078\u629e\u4e2d\u306e\u30ad\u30c3\u30c8: " + (this.selectedKit != null ? this.selectedKit : "\u672a\u9078\u629e");
        context.drawString(this.font, kitText, this.width / 4 - 60, this.height / 6 - 15, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
        for (GuiEventListener element : this.children()) {
            String btnText;
            Button btn;
            if (!(element instanceof Button) || !(btn = (Button)element).isMouseOver((double)mouseX, (double)mouseY) || !this.kits.contains(btnText = btn.getMessage().getString())) continue;
            this.activePreviewKit = btnText;
        }
        if (this.activePreviewKit != null && this.kitItems.containsKey(this.activePreviewKit)) {
            int y;
            int x;
            int[] pos;
            int i;
            Map<String, ItemStack> items = this.kitItems.get(this.activePreviewKit);
            int previewX = this.width / 2;
            int previewY = this.height / 6;
            LinkedHashMap<String, int[]> slotPositions = new LinkedHashMap<String, int[]>();
            slotPositions.put("head", new int[]{0, 0});
            slotPositions.put("chest", new int[]{0, 18});
            slotPositions.put("legs", new int[]{0, 36});
            slotPositions.put("feet", new int[]{0, 54});
            slotPositions.put("offhand", new int[]{22, 54});
            for (i = 9; i < 36; ++i) {
                int row = (i - 9) / 9;
                int col = (i - 9) % 9;
                slotPositions.put(String.valueOf(i), new int[]{44 + col * 18, row * 18});
            }
            for (i = 0; i < 9; ++i) {
                slotPositions.put(String.valueOf(i), new int[]{44 + i * 18, 58});
            }
            ArrayList<String> unmappedKeys = new ArrayList<String>();
            for (String k : items.keySet()) {
                boolean mapped = false;
                if (slotPositions.containsKey(k)) {
                    mapped = true;
                } else if (k.equals("helmet")) {
                    mapped = true;
                    slotPositions.put("helmet", (int[])slotPositions.get("head"));
                } else if (k.equals("chestplate")) {
                    mapped = true;
                    slotPositions.put("chestplate", (int[])slotPositions.get("chest"));
                } else if (k.equals("leggings")) {
                    mapped = true;
                    slotPositions.put("leggings", (int[])slotPositions.get("legs"));
                } else if (k.equals("boots")) {
                    mapped = true;
                    slotPositions.put("boots", (int[])slotPositions.get("feet"));
                } else if (k.equals("shield")) {
                    mapped = true;
                    slotPositions.put("shield", (int[])slotPositions.get("offhand"));
                }
                if (mapped) continue;
                unmappedKeys.add(k);
            }
            int extraY = 80;
            int extraCol = 0;
            for (String unk : unmappedKeys) {
                slotPositions.put(unk, new int[]{44 + extraCol * 18, extraY});
                if (++extraCol < 9) continue;
                extraCol = 0;
                extraY += 18;
            }
            int padding = 4;
            int bgWidth = 206 + padding * 2;
            int bgHeight = Math.max(76, extraY + 18) + padding * 2;
            context.fill(previewX - 2, previewY - 14, previewX + bgWidth + 2, previewY + bgHeight + 2, -1072689136);
            context.drawString(this.font, this.activePreviewKit + " - \u30d7\u30ec\u30d3\u30e5\u30fc", previewX, previewY - 10, 0xAAAAAA);
            int tooltipX = -1;
            int tooltipY = -1;
            ItemStack hoveredStack = ItemStack.EMPTY;
            HashSet<String> baseSlots = new HashSet<String>();
            baseSlots.add("head");
            baseSlots.add("chest");
            baseSlots.add("legs");
            baseSlots.add("feet");
            baseSlots.add("offhand");
            for (int i2 = 0; i2 < 36; ++i2) {
                baseSlots.add(String.valueOf(i2));
            }
            for (String string : baseSlots) {
                pos = (int[])slotPositions.get(string);
                if (pos == null) continue;
                x = previewX + padding + pos[0];
                y = previewY + padding + pos[1];
                context.fill(x, y, x + 16, y + 16, -2138338421);
            }
            for (String string : unmappedKeys) {
                pos = (int[])slotPositions.get(string);
                x = previewX + padding + pos[0];
                y = previewY + padding + pos[1];
                context.fill(x, y, x + 16, y + 16, -2138338421);
            }
            for (Map.Entry entry : items.entrySet()) {
                String key = (String)entry.getKey();
                int[] pos2 = (int[])slotPositions.get(key);
                if (pos2 == null) continue;
                int x2 = previewX + padding + pos2[0];
                int y2 = previewY + padding + pos2[1];
                ItemStack stack = (ItemStack)entry.getValue();
                context.renderItem(stack, x2, y2);
                context.renderItemDecorations(this.font, stack, x2, y2);
                if (mouseX < x2 || mouseX >= x2 + 16 || mouseY < y2 || mouseY >= y2 + 16) continue;
                hoveredStack = stack;
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
            if (!hoveredStack.isEmpty()) {
                context.renderTooltip(this.font, hoveredStack, tooltipX, tooltipY);
            }
        }
    }

    private static class DifficultySlider
    extends AbstractSliderButton {
        private final StartGameScreen screen;

        public DifficultySlider(int x, int y, int width, int height, StartGameScreen screen, double initialValue) {
            super(x, y, width, height, (Component)Component.literal((String)("\u96e3\u6613\u5ea6: " + String.format("%.1f", initialValue * 9.0))), initialValue);
            this.screen = screen;
        }

        protected void updateMessage() {
            this.setMessage((Component)Component.literal((String)("\u96e3\u6613\u5ea6: " + String.format("%.1f", this.value * 9.0))));
        }

        protected void applyValue() {
            this.screen.setDifficulty((float)(this.value * 9.0));
        }

        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -13421773);
            int halfW = this.width / 2;
            guiGraphics.fillGradient(this.getX() + 1, this.getY() + 1, this.getX() + halfW, this.getY() + this.height - 1, -16755201, -256);
            guiGraphics.fillGradient(this.getX() + halfW, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, -256, -65536);
            int thumbX = this.getX() + (int)(this.value * (double)(this.width - 8));
            guiGraphics.fill(thumbX, this.getY(), thumbX + 8, this.getY() + this.height, -1);
            guiGraphics.fill(thumbX + 1, this.getY() + 1, thumbX + 7, this.getY() + this.height - 1, -16777216);
            Component message = this.getMessage();
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, message, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF);
        }
    }
}

