package io.github.moosasharwaan.appliedquartermaster.client;

import io.github.moosasharwaan.appliedquartermaster.storage.StorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The block's own screen: its 8 slots above the player inventory. */
public class StorageScreen extends AbstractContainerScreen<StorageMenu> {

    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = 40;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        g.fill(x + 2, y, x + imageWidth - 2, y + imageHeight, 0xFF413F54);
        g.fill(x, y + 2, x + imageWidth, y + imageHeight - 2, 0xFF413F54);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFCBCCD4);
        g.fill(x + 2, y + 1, x + imageWidth - 3, y + 3, 0xFFF2F2F2);
        g.fill(x + 1, y + 2, x + 3, y + imageHeight - 3, 0xFFF2F2F2);
        g.fill(x + 3, y + imageHeight - 3, x + imageWidth - 2, y + imageHeight - 1, 0xFF878FA5);
        g.fill(x + imageWidth - 3, y + 3, x + imageWidth - 1, y + imageHeight - 2, 0xFF878FA5);
        for (var slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFFADB0C4);
            g.fill(sx, sy, sx + 17, sy + 1, 0xFF878FA5);
            g.fill(sx, sy, sx + 1, sy + 17, 0xFF878FA5);
            g.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFF2F2F2);
            g.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFF2F2F2);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        var hint = Component.translatable("gui.appliedquartermaster.storage.accepts." + menu.getKind().id());
        g.text(font, hint, imageWidth - 8 - font.width(hint), titleLabelY, 0xFF707070, false);
    }
}
