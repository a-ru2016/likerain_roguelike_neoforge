package com.timeattack.roguelike.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CarryoverSelectScreen extends AbstractContainerScreen<CarryoverSelectScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public CarryoverSelectScreen(CarryoverSelectScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        context.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}
