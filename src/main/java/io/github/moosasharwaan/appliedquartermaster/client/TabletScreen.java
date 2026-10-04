package io.github.moosasharwaan.appliedquartermaster.client;

import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The opened ME Tablet: a tab for each installed module along the top (the pinned default tab has a gold pin),
 * the Modules gear at the top right, and the Modules page with the module slots and the player inventory.
 */
public class TabletScreen extends AbstractContainerScreen<TabletMenu> {

    private static final int TAB_W = 26;
    private static final int TAB_H = 20;
    private static final int PANEL_Y = TAB_H - 2;
    private static final int BOX = 36;

    // AE2 / vanilla GUI palette
    private static final int OUTLINE = 0xFF000000;
    private static final int FACE = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int TAB_IDLE = 0xFFA8A8A8;
    private static final int GOLD = 0xFFE8B53A;
    private static final int GOLD_DARK = 0xFF8A5E12;
    private static final int TEXT = 0xFF404040;
    private static final int TEXT_DIM = 0xFF707070;

    public TabletScreen(TabletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, TabletMenu.INVENTORY_Y + 58 + 24);
        this.inventoryLabelY = TabletMenu.INVENTORY_Y - 11;
    }

    /** Installed modules in slot order: one tab each, no empty tabs. */
    private List<Integer> tabs() {
        var list = new ArrayList<Integer>();
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            if (!menu.getModule(i).isEmpty()) {
                list.add(i);
            }
        }
        return list;
    }

    private int tabX(int index) {
        return 4 + index * (TAB_W + 2);
    }

    private int gearX() {
        return imageWidth - 4 - TAB_W;
    }

    private int boxX(int module) {
        return TabletMenu.moduleSlotX(module) - (BOX - 16) / 2;
    }

    private int boxY() {
        return TabletMenu.MODULE_Y - (BOX - 16) / 2;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        int pinned = menu.getDefaultModule();

        // Inactive tabs sit behind the panel.
        var tabs = tabs();
        for (int t = 0; t < tabs.size(); t++) {
            int tx = x + tabX(t);
            bevel(g, tx, y + 2, TAB_W, TAB_H, TAB_IDLE);
        }

        panel(g, x, y + PANEL_Y, imageWidth, imageHeight - PANEL_Y);

        // The Modules page is the open page, so its gear tab is drawn joined to the panel.
        int gx = x + gearX();
        bevel(g, gx, y, TAB_W, TAB_H + 1, FACE);
        g.fill(gx + 1, y + TAB_H - 2, gx + TAB_W - 1, y + TAB_H + 1, FACE);
        gear(g, gx + 8, y + 5, 0xFF505050);

        for (int t = 0; t < tabs.size(); t++) {
            int module = tabs.get(t);
            int tx = x + tabX(t);
            g.item(menu.getModule(module), tx + 5, y + 4);
            if (module == pinned) {
                pin(g, tx + TAB_W - 7, y + 3, true);
            }
        }

        // Module boxes: each installed module, then an empty box for every free slot.
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            int bx = x + boxX(i);
            int by = y + boxY();
            inset(g, bx, by, BOX, BOX, i == pinned ? 0xFFD9C79A : SLOT);
            if (i == pinned) {
                g.outline(bx - 1, by - 1, BOX + 2, BOX + 2, GOLD);
            }
            int sx = x + TabletMenu.moduleSlotX(i);
            int sy = y + TabletMenu.MODULE_Y;
            inset(g, sx - 1, sy - 1, 18, 18, SLOT);
            if (!menu.getModule(i).isEmpty()) {
                pin(g, bx + BOX - 9, by + 2, i == pinned);
            }
        }

        // Player inventory slots.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                inset(g, x + 7 + col * 18, y + TabletMenu.INVENTORY_Y - 1 + row * 18, 18, 18, SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            inset(g, x + 7 + col * 18, y + TabletMenu.INVENTORY_Y + 57, 18, 18, SLOT);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("gui.appliedquartermaster.tablet.modules"), 8, PANEL_Y + 6, TEXT, false);
        int pinned = menu.getDefaultModule();
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var module = menu.getModule(i);
            int cx = boxX(i) + BOX / 2;
            int ty = boxY() + BOX + 3;
            if (module.isEmpty()) {
                centered(g, Component.translatable("gui.appliedquartermaster.tablet.empty_slot"), cx, ty, TEXT_DIM);
            } else {
                centered(g, shortName(module), cx, ty, i == pinned ? GOLD_DARK : TEXT);
            }
        }
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        var tooltip = tooltipAt(mouseX - leftPos, mouseY - topPos);
        if (tooltip != null) {
            g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(int mx, int my) {
        var tabs = tabs();
        int pinned = menu.getDefaultModule();
        for (int t = 0; t < tabs.size(); t++) {
            if (inside(mx, my, tabX(t), 2, TAB_W, TAB_H - 4)) {
                int module = tabs.get(t);
                return List.of(menu.getModule(module).getHoverName(),
                        Component.translatable("gui.appliedquartermaster.tablet.tab_open").withColor(0xFF7FD7FF),
                        Component.translatable(module == pinned
                                ? "gui.appliedquartermaster.tablet.unpin"
                                : "gui.appliedquartermaster.tablet.tab_pin").withColor(0xFF7FD7FF));
            }
        }
        if (inside(mx, my, gearX(), 0, TAB_W, TAB_H)) {
            return List.of(Component.translatable("gui.appliedquartermaster.tablet.modules"));
        }
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            if (!menu.getModule(i).isEmpty() && inside(mx, my, boxX(i) + BOX - 10, boxY() + 1, 10, 10)) {
                return List.of(Component.translatable(i == pinned
                        ? "gui.appliedquartermaster.tablet.unpin"
                        : "gui.appliedquartermaster.tablet.pin"));
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (menu.getCarried().isEmpty()) {
            int mx = (int) event.x() - leftPos;
            int my = (int) event.y() - topPos;
            var tabs = tabs();
            for (int t = 0; t < tabs.size(); t++) {
                if (inside(mx, my, tabX(t), 2, TAB_W, TAB_H - 4)) {
                    int module = tabs.get(t);
                    if (event.button() == 1) {
                        press(TabletMenu.BUTTON_PIN + module);
                    } else if (event.button() == 0) {
                        press(TabletMenu.BUTTON_OPEN + module);
                    }
                    return true;
                }
            }
            if (inside(mx, my, gearX(), 0, TAB_W, TAB_H)) {
                return true; // the Modules page is already open
            }
            for (int i = 0; i < TabletModules.SLOTS; i++) {
                if (!menu.getModule(i).isEmpty() && inside(mx, my, boxX(i) + BOX - 10, boxY() + 1, 10, 10)) {
                    press(TabletMenu.BUTTON_PIN + i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void press(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private Component shortName(ItemStack stack) {
        var name = stack.getHoverName().getString();
        if (font.width(name) <= BOX + 2) {
            return Component.literal(name);
        }
        var cut = font.plainSubstrByWidth(name, BOX - 4);
        return Component.literal(cut + "…");
    }

    private void centered(GuiGraphicsExtractor g, Component text, int cx, int y, int color) {
        g.text(font, text, cx - font.width(text) / 2, y, color, false);
    }

    /** Raised vanilla-style panel with a black outline and cut corners. */
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + 1, OUTLINE);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, OUTLINE);
        g.fill(x, y + 2, x + 1, y + h - 2, OUTLINE);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, OUTLINE);
        g.fill(x + 1, y + 1, x + 2, y + 2, OUTLINE);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, OUTLINE);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, OUTLINE);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, FACE);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, FACE);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);
    }

    /** Raised box (tabs). */
    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, LIGHT);
    }

    /** Sunken box (slots and module boxes). */
    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, face);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    /** Gold pin marking the default tab (hollow when not pinned). */
    private static void pin(GuiGraphicsExtractor g, int x, int y, boolean on) {
        int head = on ? GOLD : 0xFF9A9A9A;
        int dark = on ? GOLD_DARK : 0xFF5A5A5A;
        g.fill(x + 1, y, x + 6, y + 4, dark);
        g.fill(x + 2, y + 1, x + 5, y + 3, head);
        g.fill(x, y + 4, x + 7, y + 5, dark);
        g.fill(x + 3, y + 5, x + 4, y + 8, dark);
    }

    private static final String[] GEAR = {
            "...##...",
            ".#.##.#.",
            "..####..",
            "###..###",
            "###..###",
            "..####..",
            ".#.##.#.",
            "...##...",
    };

    private static void gear(GuiGraphicsExtractor g, int x, int y, int color) {
        for (int row = 0; row < GEAR.length; row++) {
            for (int col = 0; col < GEAR[row].length(); col++) {
                if (GEAR[row].charAt(col) == '#') {
                    g.fill(x + col * 1, y + row * 1, x + col + 1, y + row + 1, color);
                }
            }
        }
    }
}
