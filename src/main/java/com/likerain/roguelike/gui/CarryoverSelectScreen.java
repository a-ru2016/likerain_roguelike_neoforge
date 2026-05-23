/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package com.likerain.roguelike.gui;

import com.likerain.roguelike.gui.CarryoverSelectScreenHandler;
import com.likerain.roguelike.network.CarryoverSelectPayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CarryoverSelectScreen
extends AbstractContainerScreen<CarryoverSelectScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace((String)"textures/gui/container/generic_54.png");
    private Button doneButton;

    public CarryoverSelectScreen(CarryoverSelectScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    protected void init() {
        super.init();
        this.doneButton = Button.builder((Component)Component.literal((String)"\u8ee2\u751f\u5b8c\u4e86"), btn -> {
            PacketDistributor.sendToServer((CustomPacketPayload)new CarryoverSelectPayload(List.of()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            this.onClose();
        }).bounds(this.leftPos + this.imageWidth + 10, this.topPos + 180, 80, 20).build();
        this.addRenderableWidget(this.doneButton);
    }

    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        context.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        super.renderLabels(context, mouseX, mouseY);
        double pts = ((CarryoverSelectScreenHandler)this.menu).getPoints();
        String ptsStr = String.format("\u6b8b\u308a: %.2f pt", pts);
        int stringWidth = this.font.width(ptsStr);
        context.drawString(this.font, ptsStr, this.imageWidth - stringWidth - 8, 6, 0x404040, false);
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.renderTooltip(context, mouseX, mouseY);
    }

    protected void renderTooltip(GuiGraphics context, int x, int y) {
        ItemStack stack;
        if (((CarryoverSelectScreenHandler)this.menu).getCarried().isEmpty() && this.hoveredSlot != null && (stack = this.hoveredSlot.getItem()).isEmpty()) {
            int index = this.hoveredSlot.getContainerSlot();
            MutableComponent tooltip = null;
            if (index >= 36 && index <= 39) {
                String part = index == 36 ? "\u982d" : (index == 37 ? "\u80f8" : (index == 38 ? "\u811a" : "\u8db3"));
                tooltip = Component.literal((String)("\u00a7e\u5f15\u304d\u7d99\u3050\u9632\u5177 (" + part + ") \u3092\u3053\u3053\u306b\u7f6e\u304f"));
            } else if (index == 40) {
                tooltip = Component.literal((String)"\u00a7e\u5f15\u304d\u7d99\u3050\u30a2\u30af\u30bb\u30b5\u30ea\u30fc\u3092\u3053\u3053\u306b\u7f6e\u304f");
            } else if (index == 41) {
                tooltip = Component.literal((String)"\u00a7a\u7121\u6761\u4ef6\u5f15\u304d\u7d99\u304e\u67a0 (\u73fe\u5728\u306e\u30a2\u30a4\u30c6\u30e0\u30921\u3064\u5f15\u304d\u7d99\u3052\u307e\u3059)");
            } else if (index >= 45 && index <= 53) {
                tooltip = Component.literal((String)("\u00a7e\u5f15\u304d\u7d99\u304e\u6301\u3061\u51fa\u3057\u30a2\u30a4\u30c6\u30e0\u3092\u3053\u3053\u306b\u7f6e\u304f (\u67a0 " + (index - 44) + ")"));
            }
            if (tooltip != null) {
                context.renderTooltip(this.font, (Component)tooltip, x, y);
                return;
            }
        }
        super.renderTooltip(context, x, y);
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }
}

