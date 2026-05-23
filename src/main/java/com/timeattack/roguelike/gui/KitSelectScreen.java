package com.timeattack.roguelike.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.timeattack.roguelike.network.KitSelectedPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class KitSelectScreen extends Screen {
    private final List<String> kits;
    private final java.util.Map<String, java.util.Map<String, net.minecraft.world.item.ItemStack>> kitItems = new java.util.HashMap<>();
    private String activePreviewKit = null;

    public KitSelectScreen(List<String> kits) {
        super(Component.literal("初期キットを選択してください"));
        this.kits = kits;
        if (!kits.isEmpty()) {
            this.activePreviewKit = kits.get(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height / 4;
        int buttonX = this.width / 4 - 50;

        for (String kit : kits) {
            Button button = Button.builder(Component.literal(kit), btn -> {
                // NeoForge: クライアント→サーバーのパケット送信
                PacketDistributor.sendToServer(new KitSelectedPayload(kit));
                this.onClose();
            }).bounds(buttonX, y, 100, 20).build();
            this.addRenderableWidget(button);
            y += 24;
        }

        // Load items for all kits
        if (this.minecraft != null && this.minecraft.level != null) {
            net.minecraft.core.RegistryAccess registries = this.minecraft.level.registryAccess();
            com.timeattack.roguelike.config.ModConfig config = com.timeattack.roguelike.config.ModConfig.load();
            for (String kit : kits) {
                java.util.Map<String, net.minecraft.world.item.ItemStack> stacks = new java.util.HashMap<>();
                java.util.Map<String, String> initials = com.timeattack.roguelike.util.StarterKitParser.getInitialKitItems(kit);
                if (initials.isEmpty()) {
                    java.util.List<String> list = config.kitInitialItems.get(kit);
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            initials.put(String.valueOf(i), list.get(i));
                        }
                    }
                }
                for (java.util.Map.Entry<String, String> entry : initials.entrySet()) {
                    net.minecraft.world.item.ItemStack stack =
                            com.timeattack.roguelike.util.StarterKitParser.parseItem(entry.getValue(), registries);
                    if (!stack.isEmpty()) {
                        stacks.put(entry.getKey(), stack);
                    }
                }
                kitItems.put(kit, stacks);
            }
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 30, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);

        // Update active preview kit when hovering over a button
        for (net.minecraft.client.gui.components.events.GuiEventListener element : this.children()) {
            if (element instanceof Button btn && btn.isMouseOver(mouseX, mouseY)) {
                activePreviewKit = btn.getMessage().getString();
                break;
            }
        }

        // Draw preview for active preview kit
        if (activePreviewKit != null && kitItems.containsKey(activePreviewKit)) {
            java.util.Map<String, net.minecraft.world.item.ItemStack> items = kitItems.get(activePreviewKit);

            int previewX = this.width / 2;
            int previewY = this.height / 4;

            java.util.Map<String, int[]> slotPositions = new java.util.LinkedHashMap<>();
            slotPositions.put("head", new int[]{0, 0});
            slotPositions.put("chest", new int[]{0, 18});
            slotPositions.put("legs", new int[]{0, 36});
            slotPositions.put("feet", new int[]{0, 54});
            slotPositions.put("offhand", new int[]{22, 54});

            for (int i = 9; i < 36; i++) {
                int row = (i - 9) / 9;
                int col = (i - 9) % 9;
                slotPositions.put(String.valueOf(i), new int[]{44 + col * 18, row * 18});
            }
            for (int i = 0; i < 9; i++) {
                slotPositions.put(String.valueOf(i), new int[]{44 + i * 18, 58});
            }

            java.util.List<String> unmappedKeys = new java.util.ArrayList<>();
            for (String k : items.keySet()) {
                boolean mapped = false;
                if (slotPositions.containsKey(k)) mapped = true;
                else if (k.equals("helmet")) { mapped = true; slotPositions.put("helmet", slotPositions.get("head")); }
                else if (k.equals("chestplate")) { mapped = true; slotPositions.put("chestplate", slotPositions.get("chest")); }
                else if (k.equals("leggings")) { mapped = true; slotPositions.put("leggings", slotPositions.get("legs")); }
                else if (k.equals("boots")) { mapped = true; slotPositions.put("boots", slotPositions.get("feet")); }
                else if (k.equals("shield")) { mapped = true; slotPositions.put("shield", slotPositions.get("offhand")); }
                if (!mapped) unmappedKeys.add(k);
            }

            int extraY = 58 + 22;
            int extraCol = 0;
            for (String unk : unmappedKeys) {
                slotPositions.put(unk, new int[]{44 + extraCol * 18, extraY});
                extraCol++;
                if (extraCol >= 9) { extraCol = 0; extraY += 18; }
            }

            int padding = 4;
            int bgWidth = 44 + 9 * 18 + padding * 2;
            int bgHeight = Math.max(76, extraY + 18) + padding * 2;

            context.fill(previewX - 2, previewY - 14, previewX + bgWidth + 2, previewY + bgHeight + 2, 0xC0101010);
            context.drawString(this.font, activePreviewKit + " - プレビュー", previewX, previewY - 10, 0xAAAAAA);

            int tooltipX = -1, tooltipY = -1;
            net.minecraft.world.item.ItemStack hoveredStack = net.minecraft.world.item.ItemStack.EMPTY;

            java.util.Set<String> baseSlots = new java.util.HashSet<>();
            baseSlots.add("head"); baseSlots.add("chest"); baseSlots.add("legs");
            baseSlots.add("feet"); baseSlots.add("offhand");
            for (int i = 0; i < 36; i++) baseSlots.add(String.valueOf(i));

            for (String key : baseSlots) {
                int[] pos = slotPositions.get(key);
                if (pos == null) continue;
                int x = previewX + padding + pos[0];
                int y = previewY + padding + pos[1];
                context.fill(x, y, x + 16, y + 16, 0x808B8B8B);
            }

            for (String unk : unmappedKeys) {
                int[] pos = slotPositions.get(unk);
                int x = previewX + padding + pos[0];
                int y = previewY + padding + pos[1];
                context.fill(x, y, x + 16, y + 16, 0x808B8B8B);
            }

            for (java.util.Map.Entry<String, net.minecraft.world.item.ItemStack> entry : items.entrySet()) {
                String key = entry.getKey();
                int[] pos = slotPositions.get(key);
                if (pos == null) continue;

                int x = previewX + padding + pos[0];
                int y = previewY + padding + pos[1];

                net.minecraft.world.item.ItemStack stack = entry.getValue();
                context.renderItem(stack, x, y);
                context.renderItemDecorations(this.font, stack, x, y);

                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    hoveredStack = stack;
                    tooltipX = mouseX;
                    tooltipY = mouseY;
                }
            }

            if (!hoveredStack.isEmpty()) {
                context.renderTooltip(this.font, hoveredStack, tooltipX, tooltipY);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
