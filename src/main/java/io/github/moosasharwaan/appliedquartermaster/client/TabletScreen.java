package io.github.moosasharwaan.appliedquartermaster.client;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.network.DevicesViewPayload;
import io.github.moosasharwaan.appliedquartermaster.network.NetworkStatsPayload;
import io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModules;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The opened ME Tablet, drawn as a tablet device in AE2's Wireless Terminal colours.
 * <ul>
 * <li>Casing with a power light, camera and home button; a purple status bar with signal, game time and battery.</li>
 * <li>Home screen: clock and app icons (terminal modules, Network, Farms, Library, Armory, Tools, Settings).</li>
 * <li>Every app: an app bar (back, title, search, tools) and a dock at the bottom to switch apps in one click.</li>
 * <li>Storage apps and Settings show the player's inventory as a "Pockets" pane on the right.</li>
 * </ul>
 */
public class TabletScreen extends AbstractContainerScreen<TabletMenu> {

    // ---------------------------------------------------------------- palette (AE2 Wireless Terminal)
    private static final int CASE_OUTLINE = 0xFF2B2B2B;
    private static final int FRAME = 0xFF4D4D67;
    private static final int FACE = 0xFFCBCCD4;
    private static final int LIGHT = 0xFFF2F2F2;
    private static final int SHADOW = 0xFF878FA5;
    private static final int SHADOW_2 = 0xFF9A9FB4;
    private static final int SLOT = 0xFFADB0C4;
    private static final int SLOT_DARK = 0xFF696D88;
    private static final int RAISED = 0xFFD6D7DF;
    private static final int POCKETS = 0xFFC0C2CE;
    private static final int PURPLE = 0xFF5A479E;
    private static final int PURPLE_LIGHT = 0xFF915DCD;
    private static final int PURPLE_DARK = 0xFF2F2560;
    private static final int SELECT = 0xFFCDBFF0;
    private static final int TEXT = 0xFF413F54;
    private static final int TEXT_STRONG = 0xFF2F2D40;
    private static final int TEXT_DIM = 0xFF55526D;
    private static final int TEXT_ON_PURPLE = 0xFFF2F2F2;
    private static final int GREEN_TEXT = 0xFF1F7A33;
    private static final int RED_TEXT = 0xFFA52A24;
    private static final int LIGHT_GREEN = 0xFF3FC95A;
    private static final int LIGHT_RED = 0xFFD8302A;
    private static final int SIGNAL_ON = 0xFF7DFF8A;
    private static final int SIGNAL_OFF = 0xFF7C6CB8;
    private static final int SWITCH_ON = 0xFF3FAE55;
    private static final int GOLD = 0xFFD9B44A;
    private static final int GOLD_DARK = 0xFF8A5E12;
    private static final int HOVER = 0x50FFFFFF;
    private static final int HINT = 0xFF7FD7FF;

    // ---------------------------------------------------------------- geometry (GUI pixels)
    private static final int BEZEL_X = 12;
    private static final int BEZEL_Y = 10;
    private static final int STATUS_H = 11;
    private static final int APPBAR_H = 20;
    private static final int DOCK_H = 26;
    private static final int POCKETS_W = 174;
    private static final int SCROLL_W = 8;
    private static final int TOOL = 14;
    private static final int SEARCH_W = 76;

    /** View sizes: 0-2 storage Large/Medium/Small, 3 device kinds, 4 device rows, 5 farm cards, 6 plate rows. */
    private static final int S_DEVICE_KINDS = 3;
    private static final int S_DEVICE_ROWS = 4;
    private static final int S_FARMS = 5;
    private static final int S_PLATES = 6;
    private static final int[] CELL_W = {58, 36, 18, 58, 0, 140, 0};
    private static final int[] CELL_H = {60, 36, 18, 52, 20, 40, 24};
    private static final int[] SCALE = {3, 2, 1, 2, 1, 1, 1};
    private static final String[] SIZE_KEYS = {"large", "medium", "small"};

    private static final int SORT_STORAGE = 0;
    private static final int SORT_AZ = 1;
    private static final int SORT_ZA = 2;
    private static final float SMALL = 0.75f;

    /** An app: a terminal module (module >= 0), or a page (Network, Farms, Library, Armory, Tools, Settings). */
    private record App(int module, int page) {
        int pinValue() {
            return module >= 0 ? module : TabletModules.PIN_STORAGE + page;
        }

        boolean pinnable() {
            return module >= 0 || (page >= 0 && page < TabletModules.PAGES);
        }
    }

    /** One grid cell: a stored item / entry (entry index into the server list) or a free spot (entry -1). */
    private record Cell(int entry, ItemStack stack) {
    }

    private int cw = 460;
    private int ch = 260;
    private int rows;
    private int scroll;
    private int sort = SORT_STORAGE;
    private boolean showFree = true;
    private final int[] viewSize = new int[StorageKind.values().length];
    private final List<Cell> cells = new ArrayList<>();
    private int builtVersion = -1;
    private String builtSearch = "";
    private int builtSize = -1;
    private EditBox search;
    private EditBox rename;
    /** What the rename box renames: an entry index, -1 for the open farm. */
    private int renameEntry = Integer.MIN_VALUE;

    public TabletScreen(TabletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 520, 330);
        for (var kind : StorageKind.values()) {
            viewSize[kind.ordinal()] = menu.getInitialViewSize(kind);
        }
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, 0, 0, SEARCH_W - 6, 10, Component.translatable("gui.appliedquartermaster.tablet.search"));
        search.setBordered(false);
        search.setMaxLength(50);
        search.setTextColor(0xFFFFFFFF);
        search.setHint(Component.translatable("gui.appliedquartermaster.tablet.search").withColor(0xFFECEEF4));
        search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);
        rename = new EditBox(font, 0, 0, 150, 12, Component.translatable("gui.appliedquartermaster.automation.rename"));
        rename.setMaxLength(40);
        rename.visible = false;
        addRenderableWidget(rename);
        relayout();
    }

    // ---------------------------------------------------------------- page state

    private int page() {
        return menu.getPage();
    }

    private StorageKind kind() {
        return menu.getPageKind();
    }

    private boolean isHome() {
        return page() == TabletMenu.PAGE_HOME;
    }

    private boolean isSettings() {
        return page() == TabletMenu.PAGE_MODULES;
    }

    private boolean isFarms() {
        return page() == TabletMenu.PAGE_AUTOMATION;
    }

    private boolean isDevices() {
        return page() == TabletMenu.PAGE_DEVICES;
    }

    private boolean isStats() {
        return page() == TabletMenu.PAGE_STATS;
    }

    private boolean isNetwork() {
        return isDevices() || isStats();
    }

    private boolean inDeviceList() {
        var view = menu.getDevices();
        return isDevices() && view != null && view.inType();
    }

    private boolean inFarm() {
        var view = menu.getAutomation();
        return isFarms() && view != null && view.inFarm();
    }

    /** Pages that show a grid or list of cells. */
    private boolean hasGrid() {
        return kind() != null || isFarms() || isDevices();
    }

    private boolean hasPockets() {
        return TabletMenu.hasPockets(page());
    }

    private int size() {
        if (isFarms()) {
            return inFarm() ? S_PLATES : S_FARMS;
        }
        if (isDevices()) {
            return inDeviceList() ? S_DEVICE_ROWS : S_DEVICE_KINDS;
        }
        var kind = kind();
        return kind == null ? 0 : viewSize[kind.ordinal()];
    }

    private String statusMessage() {
        return switch (menu.getStatus()) {
            case TabletNetwork.NO_TERMINAL -> "gui.appliedquartermaster.tablet.no_terminal";
            case TabletNetwork.NOT_LINKED -> "gui.appliedquartermaster.tablet.not_linked";
            case TabletNetwork.OUT_OF_RANGE -> "gui.appliedquartermaster.tablet.out_of_range";
            default -> null;
        };
    }

    // ---------------------------------------------------------------- layout

    private int sx() {
        return BEZEL_X;
    }

    private int sy() {
        return BEZEL_Y;
    }

    private int sw() {
        return cw - 2 * BEZEL_X;
    }

    private int sh() {
        return ch - 2 * BEZEL_Y;
    }

    private int appbarY() {
        return sy() + STATUS_H;
    }

    private int bodyY() {
        return appbarY() + APPBAR_H;
    }

    private int bodyH() {
        return sh() - STATUS_H - APPBAR_H - DOCK_H;
    }

    private int pocketsX() {
        return sx() + sw() - POCKETS_W;
    }

    private int contentX() {
        return sx() + 5;
    }

    private int contentW() {
        return (hasPockets() ? pocketsX() - 4 : sx() + sw() - 5) - contentX();
    }

    private int gridX() {
        return contentX();
    }

    private int gridY() {
        return bodyY() + 2;
    }

    private int gridW() {
        return contentW() - SCROLL_W - 3;
    }

    private int cols(int s) {
        if (s == S_DEVICE_ROWS || s == S_PLATES) {
            return 1;
        }
        return Math.max(1, gridW() / CELL_W[s]);
    }

    private int cellW(int s) {
        if (s == 2) {
            return 18;
        }
        return gridW() / cols(s);
    }

    private int gridH() {
        return rows * CELL_H[size()];
    }

    /** Sizes the tablet for the window, then moves the slots to match the page. */
    private void relayout() {
        cw = Math.max(360, Math.min(520, width - 12));
        ch = Math.max(220, Math.min(330, height - 6));
        // imageWidth/imageHeight are fixed at construction in 26.1; the casing uses cw/ch instead.
        leftPos = (width - cw) / 2;
        topPos = Math.max(0, (height - ch) / 2);
        rows = hasGrid() ? Math.max(1, (bodyH() - 4) / CELL_H[size()]) : 0;
        builtSize = size();

        // Settings: module boxes and range upgrade slots.
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var slot = menu.slots.get(i);
            slot.x = moduleBoxX(i) + 8;
            slot.y = moduleBoxY() + 8;
        }
        for (int i = 0; i < TabletModules.UPGRADE_SLOTS; i++) {
            var slot = menu.slots.get(TabletModules.SLOTS + i);
            slot.x = contentX() + 5 + i * 20;
            slot.y = upgradesY() + 1;
        }
        // Pockets: the player inventory, right-hand pane.
        int px = pocketsX() + (POCKETS_W - 162) / 2 + 1;
        int py = bodyY() + 15;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                var slot = menu.slots.get(TabletMenu.FIRST_PLAYER_SLOT + row * 9 + col);
                slot.x = px + col * 18;
                slot.y = py + row * 18;
            }
        }
        for (int col = 0; col < 9; col++) {
            var slot = menu.slots.get(TabletMenu.FIRST_PLAYER_SLOT + 27 + col);
            slot.x = px + col * 18;
            slot.y = py + 58;
        }
        if (search != null) {
            search.visible = hasGrid();
            search.setX(leftPos + searchX() + 4);
            search.setY(topPos + appbarY() + 6);
            if (!search.visible) {
                search.setFocused(false);
            }
        }
        cancelRename();
        builtVersion = -1;
        clampScroll();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        relayout();
    }

    // ---------------------------------------------------------------- apps

    /** Apps for the home screen: terminal modules, then Network, Farms, Library, Armory, Tools and Settings. */
    private List<App> homeApps() {
        var list = new ArrayList<App>();
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            if (!menu.getModule(i).isEmpty()) {
                list.add(new App(i, TabletMenu.PAGE_HOME));
            }
        }
        list.add(new App(-1, TabletMenu.PAGE_DEVICES));
        list.add(new App(-1, TabletMenu.PAGE_AUTOMATION));
        for (var kind : StorageKind.values()) {
            list.add(new App(-1, kind.ordinal()));
        }
        list.add(new App(-1, TabletMenu.PAGE_MODULES));
        return list;
    }

    /** Apps for the dock: the same order, only those that can be used now, without Settings. */
    private List<App> dockApps() {
        var list = new ArrayList<App>();
        for (var app : homeApps()) {
            if (app.module() >= 0 || isOpen(app) || (app.page() >= 0 && menu.isPageUnlocked(app.page()))) {
                if (app.page() != TabletMenu.PAGE_MODULES || isOpen(app)) {
                    list.add(app);
                }
            }
        }
        return list;
    }

    private boolean isOpen(App app) {
        if (app.module() >= 0) {
            return false;
        }
        return page() == app.page() || (app.page() == TabletMenu.PAGE_DEVICES && isStats());
    }

    private boolean isLocked(App app) {
        return app.module() < 0 && app.page() >= 0 && !menu.isPageUnlocked(app.page());
    }

    private static ItemStack pageIcon(int page) {
        ItemLike item = page == 0 ? ModItems.ME_LIBRARY.get()
                : page == 1 ? ModItems.ME_ARMORY.get()
                : page == 2 ? ModItems.ME_TOOL_RACK.get()
                : page == TabletMenu.PAGE_AUTOMATION ? ModItems.ME_FARM_CONTROLLER.get()
                : appeng.core.definitions.AEBlocks.CONTROLLER.asItem();
        return new ItemStack(item);
    }

    private static Component pageTitle(int page) {
        var kind = StorageKind.byIndex(page);
        if (kind != null) {
            return kind.title();
        }
        return Component.translatable(page == TabletMenu.PAGE_AUTOMATION ? "gui.appliedquartermaster.tab.automation"
                : page == TabletMenu.PAGE_MODULES ? "gui.appliedquartermaster.tab.settings"
                : page == TabletMenu.PAGE_HOME ? "gui.appliedquartermaster.tab.home"
                : "gui.appliedquartermaster.tab.network");
    }

    private ItemStack appIcon(App app) {
        return app.module() >= 0 ? menu.getModule(app.module()) : pageIcon(app.page());
    }

    private Component appName(App app) {
        return app.module() >= 0 ? menu.getModule(app.module()).getHoverName() : pageTitle(app.page());
    }

    private void openApp(App app) {
        if (app.module() >= 0) {
            press(TabletMenu.BUTTON_OPEN + app.module());
        } else {
            switchPage(app.page());
        }
    }

    private void switchPage(int page) {
        if (page() == page) {
            return;
        }
        menu.setPageClient(page);
        press(TabletMenu.pageButton(page));
        scroll = 0;
        if (search != null) {
            search.setValue("");
        }
        relayout();
    }

    // ---------------------------------------------------------------- grid contents

    private void rebuildCells() {
        if (builtSize != size()) {
            // The device list and plate list use other layouts than their overviews.
            scroll = 0;
            relayout();
        }
        String text = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (builtVersion == menu.getViewVersion() && text.equals(builtSearch)) {
            return;
        }
        builtVersion = menu.getViewVersion();
        builtSearch = text;
        cells.clear();
        if (isDevices()) {
            var view = menu.getDevices();
            if (view != null) {
                var list = new ArrayList<Cell>();
                for (int i = 0; i < view.entries().size(); i++) {
                    var e = view.entries().get(i);
                    String key = view.inType() ? e.pos().getX() + ", " + e.pos().getY() + ", " + e.pos().getZ() : e.name();
                    if (text.isEmpty() || key.toLowerCase(Locale.ROOT).contains(text)
                            || e.name().toLowerCase(Locale.ROOT).contains(text)) {
                        list.add(new Cell(i, e.icon()));
                    }
                }
                if (sort != SORT_STORAGE && !view.inType()) {
                    Comparator<Cell> byName = Comparator.comparing(c -> view.entries().get(c.entry()).name().toLowerCase(Locale.ROOT));
                    list.sort(sort == SORT_AZ ? byName : byName.reversed());
                } else if (sort != SORT_STORAGE) {
                    // In the list, "sort" puts problems first (A-Z) or last (Z-A).
                    Comparator<Cell> byState = Comparator.comparingInt(c -> -view.entries().get(c.entry()).state());
                    list.sort(sort == SORT_AZ ? byState : byState.reversed());
                }
                cells.addAll(list);
            }
            clampScroll();
            return;
        }
        if (isFarms()) {
            var view = menu.getAutomation();
            if (view != null) {
                var list = new ArrayList<Cell>();
                for (int i = 0; i < view.entries().size(); i++) {
                    var e = view.entries().get(i);
                    if (text.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(text)) {
                        list.add(new Cell(i, e.icon()));
                    }
                }
                if (sort != SORT_STORAGE) {
                    Comparator<Cell> byName = Comparator.comparing(c -> view.entries().get(c.entry()).name().toLowerCase(Locale.ROOT));
                    list.sort(sort == SORT_AZ ? byName : byName.reversed());
                }
                cells.addAll(list);
            }
            clampScroll();
            return;
        }
        if (kind() == null) {
            return;
        }
        var items = menu.getViewItems();
        var stored = new ArrayList<Cell>();
        for (int i = 0; i < items.size(); i++) {
            var stack = items.get(i);
            if (text.isEmpty() || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(text)) {
                stored.add(new Cell(i, stack));
            }
        }
        if (sort != SORT_STORAGE) {
            Comparator<Cell> byName = Comparator.comparing(c -> c.stack().getHoverName().getString().toLowerCase(Locale.ROOT));
            stored.sort(sort == SORT_AZ ? byName : byName.reversed());
        }
        cells.addAll(stored);
        if (showFree && text.isEmpty()) {
            for (int i = 0; i < menu.getViewFree(); i++) {
                cells.add(new Cell(-1, ItemStack.EMPTY));
            }
        }
        clampScroll();
    }

    private int totalRows() {
        int c = cols(size());
        return (cells.size() + c - 1) / c;
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, totalRows() - rows));
    }

    /** Index into {@link #cells} under the mouse (screen-relative coordinates), or -1. */
    private int cellAt(int mx, int my) {
        if (!hasGrid() || statusMessage() != null) {
            return -1;
        }
        int s = size();
        int gx = mx - gridX();
        int gy = my - gridY();
        if (gx < 0 || gy < 0 || gx >= cols(s) * cellW(s) || gy >= rows * CELL_H[s]) {
            return -1;
        }
        int index = (scroll + gy / CELL_H[s]) * cols(s) + gx / cellW(s);
        return index < cells.size() ? index : -1;
    }

    private boolean inGrid(int mx, int my) {
        return hasGrid() && inside(mx, my, gridX(), gridY(), gridW(), gridH());
    }

    // ---------------------------------------------------------------- drawing: frame

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        rebuildCells();
        int x = leftPos;
        int y = topPos;
        int mx = mouseX - x;
        int my = mouseY - y;

        drawCasing(g, x, y, mx, my);
        drawStatusBar(g, x, y);
        if (isHome()) {
            drawHome(g, x, y, mx, my);
        } else {
            drawAppBar(g, x, y, mx, my);
            if (hasPockets()) {
                drawPockets(g, x, y);
            }
            String message = statusMessage();
            if (isSettings()) {
                drawSettings(g, x, y, mx, my);
            } else if (message != null) {
                drawNoNetwork(g, x, y, mx, my, message);
            } else if (isStats()) {
                drawStats(g, x, y, mx, my);
            } else if (isDevices()) {
                drawDevices(g, x, y, mx, my);
            } else if (isFarms()) {
                drawFarms(g, x, y, mx, my);
            } else {
                drawStorage(g, x, y, mx, my);
            }
        }
        drawDock(g, x, y, mx, my);
    }

    /** The tablet body: stepped corners, bevelled casing, power light, camera, home button and screen frame. */
    private void drawCasing(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        // Outline with stepped (pixel-rounded) corners.
        g.fill(x + 3, y, x + cw - 3, y + ch, CASE_OUTLINE);
        g.fill(x, y + 3, x + cw, y + ch - 3, CASE_OUTLINE);
        g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, CASE_OUTLINE);
        // Body.
        g.fill(x + 3, y + 1, x + cw - 3, y + ch - 1, FACE);
        g.fill(x + 1, y + 3, x + cw - 1, y + ch - 3, FACE);
        g.fill(x + 2, y + 2, x + cw - 2, y + ch - 2, FACE);
        // Bevels: light top/left, shadow bottom/right.
        g.fill(x + 3, y + 1, x + cw - 3, y + 2, LIGHT);
        g.fill(x + 1, y + 3, x + 2, y + ch - 3, LIGHT);
        g.fill(x + 3, y + ch - 2, x + cw - 3, y + ch - 1, SHADOW);
        g.fill(x + cw - 2, y + 3, x + cw - 1, y + ch - 3, SHADOW);
        // Power light: green when the network is reached, red otherwise.
        boolean ok = menu.getStatus() == TabletNetwork.OK;
        g.fill(x + 7, y + 4, x + 11, y + 8, CASE_OUTLINE);
        g.fill(x + 8, y + 5, x + 10, y + 7, ok ? 0xFF00FF00 : 0xFFFF3B30);
        // Camera.
        g.fill(x + cw / 2 - 2, y + 3, x + cw / 2 + 2, y + 7, 0xFF17171A);
        g.fill(x + cw / 2 - 1, y + 4, x + cw / 2, y + 5, 0xFF1E5F6B);
        // Home button on the right bezel.
        int hx = cw - BEZEL_X + 2;
        int hy = ch / 2 - 4;
        boolean hover = inside(mx, my, hx - 1, hy - 1, 10, 10);
        g.fill(x + hx, y + hy, x + hx + 8, y + hy + 8, hover ? PURPLE : FRAME);
        g.fill(x + hx + 2, y + hy + 2, x + hx + 6, y + hy + 6, hover ? SELECT : SLOT);
        // Screen.
        g.fill(x + sx() - 2, y + sy() - 2, x + sx() + sw() + 2, y + sy() + sh() + 2, FRAME);
        g.fill(x + sx(), y + sy(), x + sx() + sw(), y + sy() + sh(), FACE);
    }

    private void drawStatusBar(GuiGraphicsExtractor g, int x, int y) {
        int bx = x + sx();
        int by = y + sy();
        g.fill(bx, by, bx + sw(), by + STATUS_H, PURPLE);
        g.fill(bx, by + STATUS_H - 1, bx + sw(), by + STATUS_H, TEXT);
        // Signal bars.
        boolean ok = menu.getStatus() == TabletNetwork.OK;
        for (int i = 0; i < 4; i++) {
            int h = 2 + i * 2;
            g.fill(bx + 4 + i * 3, by + 9 - h, bx + 6 + i * 3, by + 9, ok && i < 3 ? SIGNAL_ON : SIGNAL_OFF);
        }
        var left = Component.translatable(ok ? "gui.appliedquartermaster.status.connected" : "gui.appliedquartermaster.status.no_network");
        small(g, left.getString(), bx + 18, by + 3, ok ? TEXT_ON_PURPLE : 0xFFFFD0CC);
        // Game day and time.
        String clock = clockText(true);
        small(g, clock, bx + sw() / 2 - (int) (font.width(clock) * SMALL / 2), by + 3, TEXT_ON_PURPLE);
        // Battery of the first terminal module.
        double level = batteryLevel();
        int ix = bx + sw() - 16;
        g.fill(ix, by + 3, ix + 11, by + 8, TEXT_ON_PURPLE);
        g.fill(ix + 1, by + 4, ix + 10, by + 7, PURPLE);
        g.fill(ix + 11, by + 4, ix + 12, by + 7, TEXT_ON_PURPLE);
        if (level > 0) {
            int w = (int) Math.round(9 * level);
            g.fill(ix + 1, by + 4, ix + 1 + w, by + 7, level > 0.15 ? LIGHT_GREEN : LIGHT_RED);
        }
        String pct = level < 0 ? "-" : Math.round(level * 100) + "%";
        small(g, pct, ix - 3 - (int) (font.width(pct) * SMALL), by + 3, TEXT_ON_PURPLE);
    }

    /** "Day 14 · 07:40" from the overworld clock (morning starts at 06:00). */
    private String clockText(boolean withDay) {
        if (minecraft == null || minecraft.level == null) {
            return "";
        }
        long time = minecraft.level.getOverworldClockTime();
        long day = time / 24000 + 1;
        long tod = (time + 6000) % 24000;
        String hm = String.format(Locale.ROOT, "%02d:%02d", tod / 1000, (tod % 1000) * 60 / 1000);
        return withDay ? Component.translatable("gui.appliedquartermaster.status.day", day).getString() + " · " + hm : hm;
    }

    // ---------------------------------------------------------------- drawing: app bar, dock, pockets

    private int backX() {
        return sx() + 4;
    }

    private int toolY() {
        return appbarY() + 3;
    }

    /** Right edge of the app bar's controls; tools sit from the right, the search box to their left. */
    private int toolsRight() {
        return sx() + sw() - 4;
    }

    private int toolCount() {
        if (kind() != null) {
            return 3;
        }
        return isFarms() && inFarm() ? 0 : hasGrid() ? 1 : 0;
    }

    private int toolX(int i) {
        return toolsRight() - (toolCount() - i) * (TOOL + 2) + 2;
    }

    private int searchX() {
        return toolsRight() - toolCount() * (TOOL + 2) - SEARCH_W - (toolCount() > 0 ? 2 : 0);
    }

    private int titleX() {
        return backX() + TOOL + 4;
    }

    private void drawAppBar(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        int by = y + appbarY();
        // Back.
        button(g, x + backX(), y + toolY(), TOOL, TOOL, inside(mx, my, backX(), toolY(), TOOL, TOOL), false);
        pixels(g, BACK_ICON, x + backX() + 4, y + toolY() + 3, TEXT);
        // Icon and title.
        ItemStack icon;
        String title;
        String count = "";
        if (inDeviceList()) {
            var view = menu.getDevices();
            icon = view.titleIcon();
            title = view.title();
            count = Component.translatable("gui.appliedquartermaster.devices.on_network", view.entries().size()).getString();
        } else if (inFarm()) {
            var view = menu.getAutomation();
            icon = view.titleIcon();
            title = view.title();
        } else {
            icon = isSettings() ? ItemStack.EMPTY : pageIcon(isStats() ? TabletMenu.PAGE_DEVICES : page());
            title = pageTitle(isStats() ? TabletMenu.PAGE_DEVICES : page()).getString();
            count = headerCount();
        }
        int tx = x + titleX();
        if (isSettings()) {
            pixels(g, GEAR, tx + 4, by + 6, TEXT);
        } else {
            g.item(icon, tx, by + 2);
        }
        int textX = tx + 19;
        int maxTitle = (isNetwork() && !inDeviceList() ? 52 : 150);
        g.text(font, shortName(title, maxTitle), textX, by + 6, TEXT_STRONG, false);
        int after = textX + Math.min(font.width(title), maxTitle) + 6;

        if (isNetwork() && !inDeviceList()) {
            // Devices | Statistics switch.
            String a = Component.translatable("gui.appliedquartermaster.network.devices").getString();
            String b = Component.translatable("gui.appliedquartermaster.network.statistics").getString();
            int wa = font.width(a) + 10;
            int wb = font.width(b) + 10;
            segmented(g, x + after, y + toolY(), new String[] {a, b}, new int[] {wa, wb}, isDevices() ? 0 : 1, mx - after, my - toolY());
            after += wa + wb + 8;
        } else if (inFarm()) {
            var view = menu.getAutomation();
            boolean online = view.farmOnline();
            dot(g, after + x + 4, by + 10, 6, online);
            small(g, Component.translatable(online ? "gui.appliedquartermaster.info.farm_online_short"
                    : "gui.appliedquartermaster.info.farm_offline_short").getString(), x + after + 10, by + 7, online ? GREEN_TEXT : RED_TEXT);
            // All on / All off.
            String on = Component.translatable("gui.appliedquartermaster.automation.all_on").getString();
            String off = Component.translatable("gui.appliedquartermaster.automation.all_off").getString();
            int woff = font.width(off) + 10;
            int won = font.width(on) + 10;
            int offX = (hasGrid() ? searchX() - 4 : toolsRight()) - woff;
            int onX = offX - 3 - won;
            textButton(g, x + onX, y + toolY(), won, on, inside(mx, my, onX, toolY(), won, TOOL));
            textButton(g, x + offX, y + toolY(), woff, off, inside(mx, my, offX, toolY(), woff, TOOL));
        }
        if (!count.isEmpty()) {
            int limit = (hasGrid() ? searchX() : toolsRight()) - 6;
            int w = (int) (font.width(count) * SMALL);
            if (x + after + w < x + limit) {
                small(g, count, x + after, by + 7, TEXT_DIM);
            }
        }
        if (isStats()) {
            String hint = Component.translatable("gui.appliedquartermaster.stats.updates").getString();
            small(g, hint, x + toolsRight() - (int) (font.width(hint) * SMALL), by + 7, TEXT_DIM);
        }

        // Search box and tools.
        if (hasGrid()) {
            inset(g, x + searchX(), y + toolY(), SEARCH_W, TOOL, SHADOW_2);
        }
        for (int i = 0; i < toolCount(); i++) {
            int bx = toolX(i);
            button(g, x + bx, y + toolY(), TOOL, TOOL, inside(mx, my, bx, toolY(), TOOL, TOOL), i == 1 && !showFree);
            String[] art = i == 0 ? SORT_ICONS[sort] : i == 1 ? (showFree ? FILTER_OFF : FILTER_ON) : SIZE_ICONS[size()];
            pixels(g, art, x + bx + 1, y + toolY() + 1, TEXT);
        }
        g.fill(x + sx(), by + APPBAR_H - 1, x + sx() + sw(), by + APPBAR_H, SHADOW_2);
    }

    private String headerCount() {
        if (statusMessage() != null) {
            return "";
        }
        if (kind() != null) {
            return menu.getViewItems().size() + " / " + (menu.getViewItems().size() + menu.getViewFree());
        }
        if (isDevices() && menu.getDevices() != null) {
            int devices = 0;
            int problems = 0;
            for (var e : menu.getDevices().entries()) {
                devices += e.count();
                problems += e.count() - e.active();
            }
            return Component.translatable(problems == 0 ? "gui.appliedquartermaster.network.summary"
                    : "gui.appliedquartermaster.network.summary_problems", devices, problems).getString();
        }
        if (isFarms() && menu.getAutomation() != null && !inFarm()) {
            int plates = 0;
            int offline = 0;
            for (var e : menu.getAutomation().entries()) {
                plates += e.total();
                if (e.state() == 0) {
                    offline++;
                }
            }
            return Component.translatable("gui.appliedquartermaster.automation.summary",
                    menu.getAutomation().entries().size(), plates, offline).getString();
        }
        return "";
    }

    private int dockY() {
        return sy() + sh() - DOCK_H + 3;
    }

    private int dockX(int count) {
        int w = (count + 1) * 22 + 6;
        return sx() + (sw() - w) / 2;
    }

    private void drawDock(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var apps = dockApps();
        int dx = dockX(apps.size());
        int dy = dockY();
        int w = (apps.size() + 1) * 22 + 6;
        g.fill(x + sx(), y + dy - 3, x + sx() + sw(), y + dy - 2, SHADOW_2);
        raised(g, x + dx, y + dy, w, 22, FACE);
        int pinned = menu.getDefaultTab();
        for (int i = -1; i < apps.size(); i++) {
            int ix = dx + 4 + (i + 1) * 22;
            boolean hover = inside(mx, my, ix, dy + 2, 18, 18);
            boolean current = i < 0 ? isHome() : isOpen(apps.get(i));
            inset(g, x + ix, y + dy + 2, 18, 18, current ? SELECT : hover ? 0xFFBEC1D2 : SLOT);
            if (current) {
                g.outline(x + ix, y + dy + 2, 18, 18, PURPLE);
            }
            if (i < 0) {
                pixels(g, HOME_ICON, x + ix + 4, y + dy + 6, FRAME);
            } else {
                g.item(appIcon(apps.get(i)), x + ix + 1, y + dy + 3);
                if (apps.get(i).pinValue() == pinned) {
                    g.fill(x + ix + 13, y + dy + 2, x + ix + 18, y + dy + 7, GOLD_DARK);
                    g.fill(x + ix + 14, y + dy + 3, x + ix + 17, y + dy + 6, GOLD);
                }
            }
        }
    }

    /** Which dock entry is under the mouse: -1 home, an app index, or {@link Integer#MIN_VALUE}. */
    private int dockAt(int mx, int my) {
        var apps = dockApps();
        int dx = dockX(apps.size());
        for (int i = -1; i < apps.size(); i++) {
            if (inside(mx, my, dx + 4 + (i + 1) * 22, dockY() + 2, 18, 18)) {
                return i;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void drawPockets(GuiGraphicsExtractor g, int x, int y) {
        int px = x + pocketsX();
        int py = y + bodyY();
        g.fill(px, py, px + POCKETS_W, py + bodyH(), POCKETS);
        g.fill(px, py, px + 1, py + bodyH(), SHADOW_2);
        g.text(font, Component.translatable("gui.appliedquartermaster.pockets"), px + 7, py + 4, TEXT, false);
        int tablet = menu.getTabletSlot();
        for (int i = 0; i < 36; i++) {
            var slot = menu.slots.get(TabletMenu.FIRST_PLAYER_SLOT + i);
            boolean locked = slot.getContainerSlot() == tablet;
            inset(g, x + slot.x - 1, y + slot.y - 1, 18, 18, locked ? SELECT : SLOT);
            if (locked) {
                g.outline(x + slot.x - 1, y + slot.y - 1, 18, 18, PURPLE);
            }
        }
        String hint = Component.translatable(kind() != null ? "gui.appliedquartermaster.pockets.hint_store"
                : "gui.appliedquartermaster.pockets.hint_settings").getString();
        smallWrapped(g, hint, px + 7, py + 15 + 80, POCKETS_W - 14, Math.max(1, (bodyH() - 98) / 7), TEXT_DIM);
    }

    // ---------------------------------------------------------------- drawing: home

    private static final int TILE = 34;
    private static final int TILE_STEP = 52;

    private int homeTilesY() {
        return sy() + STATUS_H + Math.max(44, (sh() - STATUS_H - DOCK_H) / 2 - 10);
    }

    private int homeTileX(int index, int count) {
        int perRow = Math.max(1, (sw() - 16) / TILE_STEP);
        int inRow = Math.min(perRow, count - (index / perRow) * perRow);
        int rowW = inRow * TILE_STEP;
        return sx() + (sw() - rowW) / 2 + (index % perRow) * TILE_STEP + (TILE_STEP - TILE) / 2;
    }

    private int homeTileY(int index) {
        int perRow = Math.max(1, (sw() - 16) / TILE_STEP);
        return homeTilesY() + (index / perRow) * (TILE + 22);
    }

    private void drawHome(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        // Wallpaper: a faint grid, like an ME network's circuitry.
        for (int gx = sx() + 15; gx < sx() + sw(); gx += 16) {
            g.fill(x + gx, y + sy() + STATUS_H, x + gx + 1, y + sy() + sh() - DOCK_H, 0xFFC4C6D1);
        }
        for (int gy = sy() + STATUS_H + 15; gy < sy() + sh() - DOCK_H; gy += 16) {
            g.fill(x + sx(), y + gy, x + sx() + sw(), y + gy + 1, 0xFFC4C6D1);
        }
        // Clock.
        String time = clockText(false);
        g.pose().pushMatrix();
        g.pose().translate(x + sx() + 14, y + sy() + STATUS_H + 8);
        g.pose().scale(3, 3);
        g.text(font, time, 0, 0, TEXT_STRONG, false);
        g.pose().popMatrix();
        String day = clockText(true);
        if (day.contains(" · ")) {
            day = day.substring(0, day.indexOf(" · "));
        }
        small(g, day, x + sx() + 15, y + sy() + STATUS_H + 37, TEXT_DIM);
        String hint = Component.translatable("gui.appliedquartermaster.home.hint").getString();
        small(g, hint, x + sx() + sw() - 10 - (int) (font.width(hint) * SMALL), y + sy() + STATUS_H + 37, TEXT_DIM);

        var apps = homeApps();
        int pinned = menu.getDefaultTab();
        for (int i = 0; i < apps.size(); i++) {
            var app = apps.get(i);
            int tx = homeTileX(i, apps.size());
            int ty = homeTileY(i);
            boolean hover = inside(mx, my, tx, ty, TILE, TILE);
            boolean locked = isLocked(app);
            inset(g, x + tx, y + ty, TILE, TILE, hover ? 0xFFBEC1D2 : SLOT);
            if (app.pinValue() == pinned && app.pinnable()) {
                g.outline(x + tx - 1, y + ty - 1, TILE + 2, TILE + 2, GOLD);
                g.fill(x + tx + TILE - 6, y + ty - 2, x + tx + TILE + 2, y + ty + 6, GOLD_DARK);
                g.fill(x + tx + TILE - 5, y + ty - 1, x + tx + TILE + 1, y + ty + 5, GOLD);
            }
            if (app.page() == TabletMenu.PAGE_MODULES && app.module() < 0) {
                g.pose().pushMatrix();
                g.pose().translate(x + tx + 9, y + ty + 9);
                g.pose().scale(2, 2);
                pixels(g, GEAR, 0, 0, FRAME);
                g.pose().popMatrix();
            } else {
                g.pose().pushMatrix();
                g.pose().translate(x + tx + 5, y + ty + 5);
                g.pose().scale(1.5f, 1.5f);
                g.item(appIcon(app), 0, 0);
                g.pose().popMatrix();
                if (locked) {
                    g.fill(x + tx + 1, y + ty + 1, x + tx + TILE - 1, y + ty + TILE - 1, 0x99ADB0C4);
                }
            }
            smallLines(g, appName(app).getString(), x + tx + TILE / 2, y + ty + TILE + 3, TILE_STEP - 4, 2,
                    locked ? TEXT_DIM : TEXT);
        }
    }

    private int homeAppAt(int mx, int my) {
        var apps = homeApps();
        for (int i = 0; i < apps.size(); i++) {
            if (inside(mx, my, homeTileX(i, apps.size()), homeTileY(i), TILE, TILE)) {
                return i;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- drawing: settings

    private int moduleBoxX(int i) {
        return contentX() + 4 + i * 44;
    }

    private int moduleBoxY() {
        return bodyY() + 16;
    }

    private int upgradesY() {
        return moduleBoxY() + 32 + 30;
    }

    private void drawSettings(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        int pinned = menu.getDefaultTab();
        g.text(font, Component.translatable("gui.appliedquartermaster.settings.terminals"), x + contentX() + 2, y + bodyY() + 4, TEXT_STRONG, false);
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            int bx = x + moduleBoxX(i);
            int by = y + moduleBoxY();
            boolean isPinned = i == pinned;
            inset(g, bx, by, 32, 32, isPinned ? 0xFFE3D6B0 : SLOT);
            if (isPinned) {
                g.outline(bx - 1, by - 1, 34, 34, GOLD);
            }
            inset(g, bx + 7, by + 7, 18, 18, SLOT);
            var module = menu.getModule(i);
            if (module.isEmpty()) {
                smallLines(g, Component.translatable("gui.appliedquartermaster.tablet.empty_slot").getString(), bx + 16, by + 35, 42, 1, TEXT_DIM);
            } else {
                pin(g, bx + 25, by + 2, isPinned);
                smallLines(g, module.getHoverName().getString(), bx + 16, by + 35, 42, 2, isPinned ? GOLD_DARK : TEXT);
            }
        }
        // Range upgrades.
        int uy = upgradesY();
        g.text(font, Component.translatable("gui.appliedquartermaster.tablet.upgrades"), x + contentX() + 2, y + uy - 11, TEXT_STRONG, false);
        for (int i = 0; i < TabletModules.UPGRADE_SLOTS; i++) {
            var slot = menu.slots.get(TabletModules.SLOTS + i);
            inset(g, x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT);
        }
        small(g, rangeText(), x + contentX() + 50, y + uy + 6, TEXT_DIM);
        // Battery.
        int bx = contentX() + 110;
        g.text(font, Component.translatable("gui.appliedquartermaster.tablet.battery"), x + bx, y + uy - 11, TEXT_STRONG, false);
        double level = batteryLevel();
        inset(g, x + bx, y + uy + 3, 64, 10, SHADOW_2);
        if (level > 0) {
            int w = (int) Math.round(62 * level);
            g.fill(x + bx + 1, y + uy + 4, x + bx + 1 + w, y + uy + 12, level > 0.5 ? LIGHT_GREEN : level > 0.15 ? GOLD : LIGHT_RED);
        }
        small(g, level < 0 ? "-" : Math.round(level * 100) + "%", x + bx + 68, y + uy + 5, TEXT);
        // Open key.
        int ky = uy + 26;
        if (ky + 20 < bodyY() + bodyH()) {
            g.text(font, Component.translatable("gui.appliedquartermaster.settings.open_key"), x + contentX() + 2, y + ky, TEXT_STRONG, false);
            var key = TabletKeys.OPEN_TABLET.isUnbound()
                    ? Component.translatable("gui.appliedquartermaster.settings.key_unset").getString()
                    : TabletKeys.OPEN_TABLET.getTranslatedKeyMessage().getString();
            smallWrapped(g, Component.translatable("gui.appliedquartermaster.settings.key_hint", key).getString(),
                    x + contentX() + 2, y + ky + 11, contentW() - 4, 3, TEXT_DIM);
        }
    }

    private boolean onModulePin(int mx, int my, int module) {
        return inside(mx, my, moduleBoxX(module) + 23, moduleBoxY(), 10, 10);
    }

    // ---------------------------------------------------------------- drawing: no network

    private int noNetButtonsY() {
        return bodyY() + bodyH() / 2 + 30;
    }

    private void drawNoNetwork(GuiGraphicsExtractor g, int x, int y, int mx, int my, String message) {
        int cx = x + contentX() + contentW() / 2;
        int top = y + bodyY() + bodyH() / 2 - 52;
        inset(g, cx - 18, top, 36, 36, SLOT);
        g.pose().pushMatrix();
        g.pose().translate(cx - 12, top + 6);
        g.pose().scale(1.5f, 1.5f);
        g.item(new ItemStack(appeng.core.definitions.AEBlocks.WIRELESS_ACCESS_POINT.asItem()), 0, 0);
        g.pose().popMatrix();
        dot(g, cx + 17, top + 35, 10, false);
        var title = Component.translatable(message + ".title");
        g.text(font, title, cx - font.width(title) / 2, top + 42, TEXT_STRONG, false);
        var lines = font.split(Component.translatable(message), Math.min(260, contentW() - 20));
        int ly = top + 54;
        for (var line : lines) {
            g.text(font, line, cx - font.width(line) / 2, ly, TEXT_DIM, false);
            ly += 10;
        }
        String settings = Component.translatable("gui.appliedquartermaster.tab.settings").getString();
        int w = font.width(settings) + 14;
        int bx = contentX() + contentW() / 2 - w / 2;
        primaryButton(g, x + bx, y + Math.max(noNetButtonsY(), ly - y + 4), w, settings,
                inside(mx, my, bx, Math.max(noNetButtonsY(), ly - y + 4), w, TOOL));
    }

    // ---------------------------------------------------------------- drawing: storage apps

    private void drawGridWell(GuiGraphicsExtractor g, int x, int y) {
        int barX = gridX() + gridW() + 3;
        inset(g, x + barX, y + gridY() - 1, SCROLL_W, gridH() + 2, SHADOW_2);
        int total = totalRows();
        int knobH = Math.max(10, total <= rows ? gridH() : gridH() * rows / total);
        int knobY = total <= rows ? 0 : (gridH() - knobH) * scroll / (total - rows);
        raised(g, x + barX + 1, y + gridY() + knobY, SCROLL_W - 2, knobH, total <= rows ? SLOT : FACE);
    }

    private void drawStorage(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var kind = kind();
        if (!menu.isUnlocked(kind)) {
            centeredMessage(g, x, y, Component.translatable("gui.appliedquartermaster.tablet.no_block." + kind.id()));
            return;
        }
        drawGridWell(g, x, y);
        int s = size();
        int cols = cols(s);
        int cw = cellW(s);
        int hovered = cellAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = (scroll + r) * cols + c;
                if (index >= cells.size()) {
                    break;
                }
                var cell = cells.get(index);
                int cx = x + gridX() + c * cw;
                int cy = y + gridY() + r * CELL_H[s];
                int iconSize = 16 * SCALE[s];
                int ix = cx + (cw - iconSize) / 2;
                int iy = cy + (s == 0 ? 2 : (CELL_H[s] - iconSize) / 2);
                if (cell.entry() < 0) {
                    freeSpot(g, ix, iy, iconSize);
                    continue;
                }
                if (s < 2) {
                    inset(g, cx + 1, cy + 1, cw - 2, CELL_H[s] - 2, index == hovered ? SELECT : SLOT);
                } else if (index == hovered) {
                    g.fill(cx, cy, cx + 18, cy + 18, HOVER);
                }
                g.pose().pushMatrix();
                g.pose().translate(ix, iy);
                g.pose().scale(SCALE[s], SCALE[s]);
                g.item(cell.stack(), 0, 0);
                g.itemDecorations(font, cell.stack(), 0, 0);
                g.pose().popMatrix();
                if (s == 0) {
                    smallLines(g, cell.stack().getHoverName().getString(), cx + cw / 2, iy + iconSize + 1, cw - 4, 1, TEXT);
                }
            }
        }
    }

    // ---------------------------------------------------------------- drawing: Network (devices)

    private static String formatAe(double value) {
        if (value >= 1e15) {
            return String.format(Locale.ROOT, "%.1fP", value / 1e15);
        }
        if (value >= 1e12) {
            return String.format(Locale.ROOT, "%.1fT", value / 1e12);
        }
        if (value >= 1_000_000_000) {
            return String.format(Locale.ROOT, "%.1fG", value / 1_000_000_000);
        }
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        }
        if (value >= 10_000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000);
        }
        return String.format(Locale.ROOT, value >= 100 ? "%.0f" : "%.1f", value);
    }

    private static String stateKey(int state) {
        return switch (state) {
            case DevicesViewPayload.ACTIVE -> "active";
            case DevicesViewPayload.NO_CHANNEL -> "no_channel";
            case DevicesViewPayload.BOOTING -> "booting";
            default -> "no_power";
        };
    }

    private void drawDevices(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var view = menu.getDevices();
        if (view == null) {
            return;
        }
        drawGridWell(g, x, y);
        boolean list = view.inType();
        int s = size();
        int cw = cellW(s);
        int hovered = cellAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols(s); c++) {
                int index = (scroll + r) * cols(s) + c;
                if (index >= cells.size()) {
                    break;
                }
                var e = view.entries().get(cells.get(index).entry());
                int cx = x + gridX() + c * cw;
                int cy = y + gridY() + r * CELL_H[s];
                if (list) {
                    drawDeviceRow(g, e, cx, cy, cw, index == hovered);
                } else {
                    drawDeviceKind(g, e, cx, cy, cw, index == hovered);
                }
            }
        }
    }

    private void drawDeviceKind(GuiGraphicsExtractor g, DevicesViewPayload.Entry e, int cx, int cy, int cw, boolean hover) {
        boolean ok = e.active() == e.count();
        inset(g, cx + 1, cy + 1, cw - 2, CELL_H[S_DEVICE_KINDS] - 2, hover ? SELECT : SLOT);
        if (!ok) {
            g.outline(cx + 1, cy + 1, cw - 2, CELL_H[S_DEVICE_KINDS] - 2, 0xFFC0453E);
        }
        int ix = cx + (cw - 32) / 2;
        int iy = cy + 4;
        g.pose().pushMatrix();
        g.pose().translate(ix, iy);
        g.pose().scale(2, 2);
        g.item(e.icon(), 0, 0);
        g.pose().popMatrix();
        dot(g, cx + cw - 7.5f, cy + 7.5f, 6, ok);
        smallLines(g, e.name(), cx + cw / 2, iy + 34, cw - 4, 1, TEXT);
        int offline = e.count() - e.active();
        var sub = offline == 0
                ? Component.translatable("gui.appliedquartermaster.devices.count", e.count())
                : Component.translatable("gui.appliedquartermaster.devices.count_offline", e.count(), offline);
        smallLines(g, sub.getString(), cx + cw / 2, iy + 41, cw - 4, 1, offline == 0 ? GREEN_TEXT : RED_TEXT);
    }

    private void drawDeviceRow(GuiGraphicsExtractor g, DevicesViewPayload.Entry e, int cx, int cy, int cw, boolean hover) {
        boolean ok = e.state() == DevicesViewPayload.ACTIVE;
        raised(g, cx, cy + 1, cw, CELL_H[S_DEVICE_ROWS] - 2, hover ? SELECT : RAISED);
        if (!ok) {
            g.outline(cx, cy + 1, cw, CELL_H[S_DEVICE_ROWS] - 2, 0xFFC0453E);
        }
        g.item(e.icon(), cx + 3, cy + 2);
        var pos = e.pos();
        String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        g.text(font, where, cx + 24, cy + 6, TEXT, false);
        if (!e.dimension().isEmpty() && !e.dimension().equals("minecraft:overworld")) {
            String dim = e.dimension().contains(":") ? e.dimension().substring(e.dimension().indexOf(':') + 1) : e.dimension();
            small(g, dim, cx + 28 + font.width(where), cy + 7, TEXT_DIM);
        }
        String locate = Component.translatable("gui.appliedquartermaster.devices.locate_button").getString();
        int lw = font.width(locate) + 10;
        textButton(g, cx + cw - lw - 3, cy + 3, lw, locate, hover);
        String right = (e.channels() > 0 ? e.channels() + " ch · " : "") + formatAe(e.power()) + " AE/t";
        int rightX = cx + cw - lw - 8 - (int) (font.width(right) * SMALL);
        small(g, right, rightX, cy + 7, TEXT_DIM);
        var state = Component.translatable("gui.appliedquartermaster.devices.state." + stateKey(e.state()));
        int chipX = Math.min(cx + Math.max(120, cw / 2 - 20), rightX - font.width(state) - 18);
        dot(g, chipX + 3, cy + 10, 6, ok);
        g.text(font, state, chipX + 9, cy + 6, ok ? GREEN_TEXT : RED_TEXT, false);
    }

    private List<Component> deviceTooltip(DevicesViewPayload view, DevicesViewPayload.Entry e) {
        var lines = new ArrayList<Component>();
        lines.add(Component.literal(e.name()));
        if (view.inType()) {
            lines.add(Component.translatable("gui.appliedquartermaster.devices.position",
                    e.pos().getX(), e.pos().getY(), e.pos().getZ(), e.dimension()).withColor(0xFFAAAAAA));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.state." + stateKey(e.state())).withColor(0xFFAAAAAA));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.usage", e.channels(), formatAe(e.power())).withColor(0xFFAAAAAA));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.hint_locate").withColor(HINT));
        } else {
            lines.add(Component.translatable("gui.appliedquartermaster.devices.kind_detail", e.count(), e.active(),
                    e.channels(), formatAe(e.power())).withColor(0xFFAAAAAA));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.hint_open").withColor(HINT));
        }
        return lines;
    }

    // ---------------------------------------------------------------- drawing: Network (statistics)

    private static String formatCount(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fG", value / 1e9);
        }
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1e6);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1e3);
        }
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String formatMinutes(long minutes) {
        if (minutes <= 0) {
            return Component.translatable("gui.appliedquartermaster.stats.empty_now").getString();
        }
        if (minutes < 60) {
            return "~" + minutes + " min";
        }
        if (minutes < 60 * 24) {
            return String.format(Locale.ROOT, "~%.1f h", minutes / 60.0);
        }
        return Component.translatable("gui.appliedquartermaster.stats.over_day").getString();
    }

    private static final String[] PERIOD_KEYS = {"10m", "1h", "1d"};

    private int periodX(int i) {
        int right = contentX() + contentW() - 4;
        return right - (3 - i) * 24;
    }

    private int statsPanelsY() {
        return bodyY() + 48;
    }

    private void drawStats(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var stats = menu.getStats();
        if (stats == null) {
            centeredMessage(g, x, y, Component.translatable("gui.appliedquartermaster.stats.loading"));
            return;
        }
        var st = stats.storage();
        int cardsY = bodyY() + 3;
        int gap = 4;
        int cardW = (contentW() - 3 * gap) / 4;
        // Storage used
        statCard(g, x + contentX(), y + cardsY, cardW,
                Component.translatable("gui.appliedquartermaster.stats.storage").getString(),
                st.totalBytes() > 0 ? formatCount(st.usedBytes()) + " / " + formatCount(st.totalBytes()) : "-",
                st.totalBytes() > 0 ? st.usedBytes() / (double) st.totalBytes() : -1, PURPLE,
                Component.translatable("gui.appliedquartermaster.stats.storage_hint",
                        formatCount(Math.max(0, st.totalBytes() - st.usedBytes())), st.freeCellSlots()).getString());
        // Item types
        statCard(g, x + contentX() + (cardW + gap), y + cardsY, cardW,
                Component.translatable("gui.appliedquartermaster.stats.types").getString(),
                st.totalTypes() > 0 ? st.usedTypes() + " / " + st.totalTypes() : String.valueOf(st.itemTypes()),
                st.totalTypes() > 0 ? st.usedTypes() / (double) st.totalTypes() : -1, PURPLE,
                Component.translatable("gui.appliedquartermaster.stats.types_hint", Math.max(0, st.totalTypes() - st.usedTypes())).getString());
        // Items stored
        statCard(g, x + contentX() + 2 * (cardW + gap), y + cardsY, cardW,
                Component.translatable("gui.appliedquartermaster.stats.items").getString(),
                String.format(Locale.ROOT, "%,d", st.items()), -1, PURPLE,
                Component.translatable("gui.appliedquartermaster.stats.items_hint", formatCount(Math.round(st.fluidBuckets())),
                        st.cells(), st.drives() + st.chests()).getString());
        // Energy
        var en = stats.energy();
        var grid = stats.grid();
        statCard(g, x + contentX() + 3 * (cardW + gap), y + cardsY, cardW,
                Component.translatable("gui.appliedquartermaster.stats.energy").getString(),
                formatAe(en.stored()) + " AE",
                en.max() > 0 ? en.stored() / en.max() : -1, SWITCH_ON,
                Component.translatable(grid.controller() == DevicesViewPayload.CONTROLLER_ONLINE
                        ? "gui.appliedquartermaster.stats.controller_online" : "gui.appliedquartermaster.stats.controller_off",
                        grid.channels()).getString());

        // Items by mod
        int py = statsPanelsY();
        int ph = bodyY() + bodyH() - py - 3;
        int modW = Math.min(170, contentW() * 2 / 5);
        int px = contentX();
        raised(g, x + px, y + py, modW, ph, RAISED);
        g.text(font, Component.translatable("gui.appliedquartermaster.stats.by_mod"), x + px + 5, y + py + 4, TEXT_STRONG, false);
        long top = 1;
        for (var m : stats.mods()) {
            top = Math.max(top, m.items());
        }
        int ry = py + 16;
        for (var m : stats.mods()) {
            if (ry + 8 > py + ph - 2) {
                break;
            }
            String name = m.name().startsWith("#")
                    ? Component.translatable("gui.appliedquartermaster.stats.other_mods", m.name().substring(1)).getString()
                    : m.name();
            smallClipped(g, name, x + px + 5, y + ry, 52, TEXT);
            int barX = px + 60;
            int barW = modW - 60 - 56;
            g.fill(x + barX, y + ry, x + barX + barW, y + ry + 5, SLOT);
            g.fill(x + barX, y + ry, x + barX + Math.max(1, (int) (barW * m.items() / (double) top)), y + ry + 5,
                    m.name().startsWith("#") ? SHADOW : PURPLE_LIGHT);
            String count = formatCount(m.items());
            small(g, count, x + px + modW - 26 - (int) (font.width(count) * SMALL), y + ry, TEXT);
            String share = st.items() > 0 ? Math.round(m.items() * 100.0 / st.items()) + "%" : "";
            small(g, share, x + px + modW - 5 - (int) (font.width(share) * SMALL), y + ry, TEXT_DIM);
            ry += 10;
        }
        if (stats.mods().isEmpty()) {
            smallWrapped(g, Component.translatable("gui.appliedquartermaster.stats.no_items").getString(), x + px + 5, y + py + 16, modW - 10, 3, TEXT_DIM);
        }

        // Falling stock
        int fx = px + modW + 4;
        int fw = contentX() + contentW() - fx;
        raised(g, x + fx, y + py, fw, ph, RAISED);
        g.text(font, Component.translatable("gui.appliedquartermaster.stats.falling"), x + fx + 5, y + py + 4, TEXT_STRONG, false);
        for (int i = 0; i < 3; i++) {
            int bx = periodX(i);
            boolean on = stats.period() == i;
            boolean hover = inside(mx, my, bx, py + 3, 22, 11);
            g.fill(x + bx, y + py + 3, x + bx + 22, y + py + 14, on ? PURPLE : hover ? SELECT : SLOT);
            String label = Component.translatable("gui.appliedquartermaster.stats.period." + PERIOD_KEYS[i]).getString();
            small(g, label, x + bx + 11 - (int) (font.width(label) * SMALL / 2), y + py + 6, on ? 0xFFFFFFFF : TEXT);
        }
        if (!stats.hasHistory()) {
            smallWrapped(g, Component.translatable("gui.appliedquartermaster.stats.collecting").getString(),
                    x + fx + 5, y + py + 18, fw - 10, 4, TEXT_DIM);
            return;
        }
        long periodTicks = io.github.moosasharwaan.appliedquartermaster.devices.NetworkStats.PERIODS[stats.period()];
        String span = stats.spanTicks() + 600 < periodTicks
                ? Component.translatable("gui.appliedquartermaster.stats.span", Math.max(1, stats.spanTicks() / 1200)).getString()
                : "";
        // Column headings
        int colName = fx + 15;
        int colChange = fx + fw - 132;
        int colPct = fx + fw - 100;
        int colLeft = fx + fw - 64;
        int colEmpty = fx + fw - 5;
        int hy = py + 17;
        small(g, span.isEmpty() ? Component.translatable("gui.appliedquartermaster.stats.col_item").getString() : span, x + colName, y + hy, TEXT_DIM);
        smallRight(g, Component.translatable("gui.appliedquartermaster.stats.col_change").getString(), x + colPct - 4, y + hy, TEXT_DIM);
        smallRight(g, Component.translatable("gui.appliedquartermaster.stats.col_left").getString(), x + colLeft + 18, y + hy, TEXT_DIM);
        smallRight(g, Component.translatable("gui.appliedquartermaster.stats.col_empty").getString(), x + colEmpty, y + hy, TEXT_DIM);
        if (stats.falling().isEmpty()) {
            smallWrapped(g, Component.translatable("gui.appliedquartermaster.stats.nothing_falling").getString(),
                    x + fx + 5, y + hy + 12, fw - 10, 3, TEXT_DIM);
            return;
        }
        int rowY = hy + 9;
        for (var f : stats.falling()) {
            if (rowY + 10 > py + ph - 2) {
                break;
            }
            boolean soon = f.minutesToEmpty() < 120;
            g.fill(x + fx + 3, y + rowY - 1, x + fx + fw - 3, y + rowY + 9, soon ? 0xFFF1D2CF : SLOT);
            pixels(g, DOWN_ICON, x + fx + 4, y + rowY + 3, RED_TEXT);
            g.pose().pushMatrix();
            g.pose().translate(x + fx + 11, y + rowY);
            g.pose().scale(0.5f, 0.5f);
            g.item(f.icon(), 0, 0);
            g.pose().popMatrix();
            smallClipped(g, f.name(), x + colName + 5, y + rowY + 1, colChange - colName - 26, TEXT);
            smallRight(g, "-" + formatCount(f.before() - f.now()), x + colPct - 4, y + rowY + 1, RED_TEXT);
            String pct = f.before() > 0 ? "-" + Math.round((f.before() - f.now()) * 100.0 / f.before()) + "%" : "";
            smallRight(g, pct, x + colLeft - 10, y + rowY + 1, RED_TEXT);
            smallRight(g, formatCount(f.now()), x + colLeft + 18, y + rowY + 1, TEXT);
            smallRight(g, formatMinutes(f.minutesToEmpty()), x + colEmpty, y + rowY + 1, soon ? RED_TEXT : TEXT);
            rowY += 11;
        }
    }

    private void statCard(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, double fill, int barColor, String hint) {
        raised(g, x, y, w, 42, RAISED);
        small(g, label, x + 4, y + 3, TEXT_DIM);
        g.text(font, shortName(value, w - 8), x + 4, y + 11, TEXT_STRONG, false);
        if (fill >= 0) {
            inset(g, x + 4, y + 22, w - 8, 6, SHADOW_2);
            g.fill(x + 5, y + 23, x + 5 + (int) Math.round((w - 10) * Math.min(1, fill)), y + 27, barColor);
        }
        smallClipped(g, hint, x + 4, y + 32, w - 8, TEXT_DIM);
    }

    // ---------------------------------------------------------------- drawing: Farms

    private void drawFarms(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var view = menu.getAutomation();
        if (!menu.isPageUnlocked(TabletMenu.PAGE_AUTOMATION)) {
            centeredMessage(g, x, y, Component.translatable("gui.appliedquartermaster.tablet.no_block.automation"));
            return;
        }
        if (view == null) {
            return;
        }
        if (view.entries().isEmpty()) {
            centeredMessage(g, x, y, Component.translatable(view.inFarm() ? "gui.appliedquartermaster.automation.no_plates"
                    : "gui.appliedquartermaster.tablet.no_block.automation"));
            return;
        }
        drawGridWell(g, x, y);
        int s = size();
        int cw = cellW(s);
        int hovered = cellAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols(s); c++) {
                int index = (scroll + r) * cols(s) + c;
                if (index >= cells.size()) {
                    break;
                }
                var e = view.entries().get(cells.get(index).entry());
                int cx = x + gridX() + c * cw;
                int cy = y + gridY() + r * CELL_H[s];
                if (view.inFarm()) {
                    drawPlateRow(g, e, cx, cy, cw, index == hovered, mx - (cx - x), my - (cy - y));
                } else {
                    drawFarmCard(g, e, cx, cy, cw, index == hovered);
                }
            }
        }
    }

    private void drawFarmCard(GuiGraphicsExtractor g, io.github.moosasharwaan.appliedquartermaster.network.AutomationViewPayload.Entry e,
                              int cx, int cy, int cw, boolean hover) {
        int h = CELL_H[S_FARMS] - 3;
        boolean offline = e.state() == 0;
        inset(g, cx + 1, cy + 1, cw - 3, h, hover ? SELECT : SLOT);
        if (offline) {
            g.outline(cx + 1, cy + 1, cw - 3, h, 0xFFC0453E);
        }
        g.pose().pushMatrix();
        g.pose().translate(cx + 5, cy + 4);
        g.pose().scale(1.5f, 1.5f);
        g.item(e.icon(), 0, 0);
        g.pose().popMatrix();
        g.text(font, shortName(e.name(), cw - 70), cx + 33, cy + 5, TEXT, false);
        String plates = Component.translatable("gui.appliedquartermaster.automation.plates_on", e.total(), e.on()).getString();
        small(g, plates, cx + 33, cy + 16, TEXT_DIM);
        boolean allOn = e.total() > 0 && e.on() == e.total();
        drawSwitch(g, cx + cw - 30, cy + 7, e.on() > 0, offline);
        boolean good = !offline && e.on() > 0;
        dot(g, cx + 8, cy + 31, 6, good);
        String status = offline ? Component.translatable("gui.appliedquartermaster.automation.offline").getString()
                : allOn ? Component.translatable("gui.appliedquartermaster.automation.running").getString()
                : e.on() == 0 ? Component.translatable("gui.appliedquartermaster.automation.all_off_state").getString()
                : Component.translatable("gui.appliedquartermaster.automation.some_on", e.on(), e.total()).getString();
        smallClipped(g, status, cx + 14, cy + 28, cw - 20, good ? GREEN_TEXT : RED_TEXT);
    }

    /** Plate row layout (x offsets inside the row). */
    private static final int PLATE_SWITCH_X = 150;
    private static final int PLATE_SEG_X = 230;

    private void drawPlateRow(GuiGraphicsExtractor g, io.github.moosasharwaan.appliedquartermaster.network.AutomationViewPayload.Entry e,
                              int cx, int cy, int cw, boolean hover, int rmx, int rmy) {
        int h = CELL_H[S_PLATES] - 2;
        raised(g, cx, cy + 1, cw, h, hover ? SELECT : RAISED);
        if (!e.customIcon()) {
            var face = AppliedQuartermaster.id("textures/part/redstone_plate_face_"
                    + (e.state() == 2 ? "on" : e.state() == 1 ? "off" : "offline") + ".png");
            g.blit(RenderPipelines.GUI_TEXTURED, face, cx + 4, cy + 4, 0f, 0f, 16, 16, 16, 16);
        } else {
            g.item(e.icon(), cx + 4, cy + 4);
        }
        g.text(font, shortName(e.name(), PLATE_SWITCH_X - 34), cx + 24, cy + 4, TEXT, false);
        boolean offline = e.state() == 0;
        small(g, Component.translatable(offline ? "gui.appliedquartermaster.automation.offline"
                : e.state() == 2 ? "gui.appliedquartermaster.automation.on" : "gui.appliedquartermaster.automation.off").getString(),
                cx + 24, cy + 14, offline ? TEXT_DIM : e.state() == 2 ? GREEN_TEXT : RED_TEXT);
        drawSwitch(g, cx + PLATE_SWITCH_X, cy + 6, e.state() == 2 || (offline && e.on() > 0), offline);
        small(g, Component.translatable("gui.appliedquartermaster.automation.strength").getString(), cx + PLATE_SWITCH_X + 30, cy + 9, TEXT_DIM);
        int segX = cx + Math.min(PLATE_SEG_X, cw - 90);
        for (int i = 0; i < 15; i++) {
            boolean filled = i < e.strength();
            boolean segHover = inside(rmx, rmy, segX - cx + i * 4, 4, 4, 14);
            g.fill(segX + i * 4, cy + 6, segX + i * 4 + 3, cy + 18, filled ? (e.state() == 2 ? LIGHT_RED : 0xFFD9908C) : segHover ? SELECT : SHADOW_2);
        }
        String strength = String.valueOf(e.strength());
        small(g, strength, segX + 64, cy + 9, TEXT);
        // Rename
        int rx = cx + cw - 18;
        button(g, rx, cy + 4, 14, 14, inside(rmx, rmy, cw - 18, 4, 14, 14), false);
        pixels(g, PENCIL_ICON, rx + 3, cy + 7, TEXT);
    }

    private int plateSegX(int cw) {
        return Math.min(PLATE_SEG_X, cw - 90);
    }

    private void drawSwitch(GuiGraphicsExtractor g, int x, int y, boolean on, boolean disabled) {
        int w = 22;
        int h = 11;
        inset(g, x, y, w, h, disabled ? SLOT : on ? SWITCH_ON : SHADOW_2);
        int kx = on ? x + w - 10 : x + 2;
        g.fill(kx, y + 2, kx + 8, y + h - 2, disabled ? 0xFFD6D7DF : LIGHT);
        g.fill(kx + 7, y + 2, kx + 8, y + h - 2, SHADOW);
    }

    // ---------------------------------------------------------------- small drawing helpers

    private void centeredMessage(GuiGraphicsExtractor g, int x, int y, Component message) {
        var lines = font.split(message, Math.min(280, contentW() - 30));
        int ly = y + bodyY() + bodyH() / 2 - lines.size() * 5;
        int cx = x + contentX() + contentW() / 2;
        for (var line : lines) {
            g.text(font, line, cx - font.width(line) / 2, ly, TEXT, false);
            ly += 10;
        }
    }

    private void small(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(SMALL, SMALL);
        g.text(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private void smallRight(GuiGraphicsExtractor g, String text, int right, int y, int color) {
        small(g, text, right - (int) Math.ceil(font.width(text) * SMALL), y, color);
    }

    private void smallClipped(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        int scaled = (int) (width / SMALL);
        if (font.width(text) > scaled) {
            text = font.plainSubstrByWidth(text, scaled - font.width("…")) + "…";
        }
        small(g, text, x, y, color);
    }

    private void smallWrapped(GuiGraphicsExtractor g, String text, int x, int y, int width, int maxLines, int color) {
        var lines = font.split(Component.literal(text), (int) (width / SMALL));
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(SMALL, SMALL);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.text(font, lines.get(i), 0, i * 9, color, false);
        }
        g.pose().popMatrix();
    }

    /** Centered text at 3/4 size, wrapped to at most {@code maxLines} lines (the last one cut with an ellipsis). */
    private void smallLines(GuiGraphicsExtractor g, String text, int centerX, int y, int width, int maxLines, int color) {
        int scaledWidth = (int) (width / SMALL);
        var lines = new ArrayList<String>();
        var line = new StringBuilder();
        for (var word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) <= scaledWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        g.pose().pushMatrix();
        g.pose().translate(centerX, y);
        g.pose().scale(SMALL, SMALL);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            String l = lines.get(i);
            boolean last = i == maxLines - 1 && lines.size() > maxLines;
            if (font.width(l) > scaledWidth || last) {
                l = font.plainSubstrByWidth(l, scaledWidth - font.width("…")) + "…";
            }
            g.text(font, l, -font.width(l) / 2, i * 9, color, false);
        }
        g.pose().popMatrix();
    }

    private static void freeSpot(GuiGraphicsExtractor g, int x, int y, int size) {
        int pad = size >= 32 ? 6 : size >= 24 ? 3 : 0;
        int x0 = x + pad;
        int y0 = y + pad;
        int s = size - pad * 2;
        g.fill(x0, y0, x0 + s, y0 + s, 0xFFB3B6C8);
        g.fill(x0, y0, x0 + s, y0 + 1, SHADOW_2);
        g.fill(x0, y0, x0 + 1, y0 + s, SHADOW_2);
        g.fill(x0 + 1, y0 + s - 1, x0 + s, y0 + s, 0xFFDCDDE6);
        g.fill(x0 + s - 1, y0 + 1, x0 + s, y0 + s, 0xFFDCDDE6);
    }

    /** A round status light: green when ok, red otherwise. Drawn at quarter-pixel resolution so it looks round. */
    private static void dot(GuiGraphicsExtractor g, float centerX, float centerY, float diameter, boolean ok) {
        g.pose().pushMatrix();
        g.pose().translate(centerX, centerY);
        g.pose().scale(0.25f, 0.25f);
        int outer = Math.round(diameter * 2);
        circle(g, outer, CASE_OUTLINE);
        circle(g, outer - 3, ok ? LIGHT_GREEN : LIGHT_RED);
        int h = Math.max(2, outer / 4);
        g.fill(-outer / 2, -outer / 2, -outer / 2 + h, -outer / 2 + h, 0x66FFFFFF);
        g.pose().popMatrix();
    }

    private static void circle(GuiGraphicsExtractor g, int radius, int color) {
        for (int y = -radius; y < radius; y++) {
            double dy = y + 0.5;
            int half = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            if (half > 0) {
                g.fill(-half, y, half, y + 1, color);
            }
        }
    }

    private Component shortName(String name, int width) {
        if (font.width(name) <= width) {
            return Component.literal(name);
        }
        return Component.literal(font.plainSubstrByWidth(name, width - font.width("…")) + "…");
    }

    /** AE2-style raised button (light top-left, slate bottom-right); pressed draws it sunken and purple. */
    private static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover, boolean pressed) {
        if (pressed) {
            inset(g, x, y, w, h, SELECT);
            return;
        }
        raised(g, x, y, w, h, hover ? 0xFFDCDDE6 : FACE);
    }

    private void textButton(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean hover) {
        button(g, x, y, w, TOOL, hover, false);
        g.text(font, label, x + (w - font.width(label)) / 2, y + 3, TEXT, false);
    }

    private void primaryButton(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean hover) {
        g.fill(x, y, x + w, y + TOOL, PURPLE_DARK);
        g.fill(x, y, x + w - 1, y + TOOL - 1, PURPLE_LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + TOOL - 1, hover ? 0xFF6B57B5 : PURPLE);
        g.text(font, label, x + (w - font.width(label)) / 2, y + 3, 0xFFFFFFFF, false);
    }

    /** Two-or-more-way switch; {@code rmx}/{@code rmy} are the mouse relative to its top-left. */
    private void segmented(GuiGraphicsExtractor g, int x, int y, String[] labels, int[] widths, int selected, int rmx, int rmy) {
        int total = 2;
        for (int w : widths) {
            total += w;
        }
        inset(g, x, y, total, TOOL, SLOT);
        int ox = 1;
        for (int i = 0; i < labels.length; i++) {
            boolean on = i == selected;
            boolean hover = inside(rmx, rmy, ox, 0, widths[i], TOOL);
            if (on || hover) {
                g.fill(x + ox, y + 1, x + ox + widths[i], y + TOOL - 1, on ? PURPLE : SELECT);
            }
            g.text(font, labels[i], x + ox + (widths[i] - font.width(labels[i])) / 2, y + 3, on ? 0xFFFFFFFF : TEXT, false);
            ox += widths[i];
        }
    }

    /** Which part of the Devices | Statistics switch is under the mouse: 0, 1 or -1. */
    private int segmentAt(int mx, int my) {
        if (!isNetwork() || inDeviceList()) {
            return -1;
        }
        String title = pageTitle(TabletMenu.PAGE_DEVICES).getString();
        int after = titleX() + 19 + Math.min(font.width(title), 52) + 6;
        int wa = font.width(Component.translatable("gui.appliedquartermaster.network.devices").getString()) + 10;
        int wb = font.width(Component.translatable("gui.appliedquartermaster.network.statistics").getString()) + 10;
        if (inside(mx, my, after + 1, toolY(), wa, TOOL)) {
            return 0;
        }
        if (inside(mx, my, after + 1 + wa, toolY(), wb, TOOL)) {
            return 1;
        }
        return -1;
    }

    /** Raised box in AE2's colours: slate outline, light top-left edge, shadow bottom-right. */
    private static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, face);
        g.fill(x, y, x + w - 1, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + h - 1, LIGHT);
        g.fill(x + 1, y + h - 1, x + w, y + h, SLOT_DARK);
        g.fill(x + w - 1, y + 1, x + w, y + h, SLOT_DARK);
    }

    /** Sunken box (slots, free spots, wells). */
    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, face);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    /** Gold pin marking the pinned app (grey when not pinned). */
    private static void pin(GuiGraphicsExtractor g, int x, int y, boolean on) {
        int head = on ? GOLD : 0xFF9A9A9A;
        int dark = on ? GOLD_DARK : 0xFF5A5A5A;
        g.fill(x + 1, y, x + 6, y + 4, dark);
        g.fill(x + 2, y + 1, x + 5, y + 3, head);
        g.fill(x, y + 4, x + 7, y + 5, dark);
        g.fill(x + 3, y + 5, x + 4, y + 8, dark);
    }

    private static void pixels(GuiGraphicsExtractor g, String[] art, int x, int y, int color) {
        for (int row = 0; row < art.length; row++) {
            for (int col = 0; col < art[row].length(); col++) {
                if (art[row].charAt(col) == '#') {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    /** Charge of the first terminal module (0..1), or -1 without one. */
    private double batteryLevel() {
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var module = menu.getModule(i);
            if (module.getItem() instanceof appeng.items.tools.powered.powersink.AEBasePoweredItem powered) {
                double max = powered.getAEMaxPower(module);
                return max <= 0 ? 0 : Math.min(1, powered.getAECurrentPower(module) / max);
            }
        }
        return -1;
    }

    private String rangeText() {
        var tablet = menu.getTablet();
        if (TabletModules.worksAcrossDimensions(tablet)) {
            return Component.translatable("gui.appliedquartermaster.tablet.range_any_dimension").getString();
        }
        if (TabletModules.hasInfiniteRange(tablet)) {
            return Component.translatable("gui.appliedquartermaster.tablet.range_infinite").getString();
        }
        return Component.translatable("gui.appliedquartermaster.tablet.range", 1 + TabletModules.boosters(tablet)).getString();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // All labels are drawn with the page in extractBackground.
    }

    // ---------------------------------------------------------------- tooltips

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        var tooltip = tooltipAt(mx, my);
        if (tooltip != null) {
            g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private List<Component> appTooltip(App app) {
        int pinned = menu.getDefaultTab();
        var lines = new ArrayList<Component>();
        lines.add(appName(app));
        if (isLocked(app)) {
            lines.add(Component.translatable("gui.appliedquartermaster.tablet.no_block."
                    + (app.page() == TabletMenu.PAGE_AUTOMATION ? "automation" : StorageKind.byIndex(app.page()).id())).withColor(0xFFAAAAAA));
        }
        lines.add(Component.translatable("gui.appliedquartermaster.tablet.tab_open").withColor(HINT));
        if (app.pinnable()) {
            lines.add(Component.translatable(app.pinValue() == pinned
                    ? "gui.appliedquartermaster.tablet.unpin" : "gui.appliedquartermaster.tablet.tab_pin").withColor(HINT));
        }
        return lines;
    }

    private List<Component> tooltipAt(int mx, int my) {
        int dock = dockAt(mx, my);
        if (dock == -1) {
            return List.of(Component.translatable("gui.appliedquartermaster.tab.home"));
        }
        if (dock >= 0) {
            return appTooltip(dockApps().get(dock));
        }
        if (inside(mx, my, cw - BEZEL_X + 1, ch / 2 - 5, 10, 10)) {
            return List.of(Component.translatable("gui.appliedquartermaster.tab.home"));
        }
        if (isHome()) {
            int app = homeAppAt(mx, my);
            return app >= 0 ? appTooltip(homeApps().get(app)) : null;
        }
        if (inside(mx, my, backX(), toolY(), TOOL, TOOL)) {
            return List.of(Component.translatable(inDeviceList() ? "gui.appliedquartermaster.devices.back"
                    : inFarm() ? "gui.appliedquartermaster.automation.back" : "gui.appliedquartermaster.tablet.back_home"));
        }
        for (int i = 0; i < toolCount(); i++) {
            if (inside(mx, my, toolX(i), toolY(), TOOL, TOOL)) {
                if (i == 0) {
                    if (inDeviceList()) {
                        return List.of(Component.translatable("gui.appliedquartermaster.devices.sort."
                                + (sort == SORT_STORAGE ? "position" : sort == SORT_AZ ? "problems_first" : "problems_last")));
                    }
                    return List.of(Component.translatable("gui.appliedquartermaster.tablet.sort."
                            + (sort == SORT_STORAGE ? "storage" : sort == SORT_AZ ? "az" : "za")));
                }
                return List.of(Component.translatable(i == 1 ? "gui.appliedquartermaster.tablet.free." + (showFree ? "shown" : "hidden")
                        : "gui.appliedquartermaster.tablet.size." + SIZE_KEYS[size()]));
            }
        }
        if (inFarm() && inside(mx, my, titleX(), toolY(), 150, TOOL)) {
            return List.of(Component.literal(menu.getAutomation().title()),
                    Component.translatable("gui.appliedquartermaster.automation.hint_rename").withColor(HINT),
                    Component.translatable("gui.appliedquartermaster.automation.hint_icon").withColor(HINT));
        }
        if (isSettings()) {
            if (inside(mx, my, contentX() + 110, upgradesY() + 3, 64, 10)) {
                return List.of(Component.translatable("gui.appliedquartermaster.tablet.battery_tooltip"));
            }
            var first = menu.slots.get(TabletModules.SLOTS);
            if (menu.getCarried().isEmpty() && inside(mx, my, first.x - 1, first.y - 1, 38, 18)
                    && menu.slots.get(TabletModules.SLOTS + Math.min(1, (mx - first.x + 1) / 20)).getItem().isEmpty()) {
                return List.of(Component.translatable("gui.appliedquartermaster.tablet.upgrades_tooltip"));
            }
            int pinned = menu.getDefaultTab();
            for (int i = 0; i < TabletModules.SLOTS; i++) {
                if (!menu.getModule(i).isEmpty() && onModulePin(mx, my, i)) {
                    return List.of(Component.translatable(i == pinned
                            ? "gui.appliedquartermaster.tablet.unpin" : "gui.appliedquartermaster.tablet.pin"));
                }
            }
            return null;
        }
        if (isStats()) {
            for (int i = 0; i < 3; i++) {
                if (inside(mx, my, periodX(i), statsPanelsY() + 3, 22, 11)) {
                    return List.of(Component.translatable("gui.appliedquartermaster.stats.period_tooltip." + PERIOD_KEYS[i]));
                }
            }
            return null;
        }
        int cell = cellAt(mx, my);
        if (cell < 0) {
            return null;
        }
        if (isDevices()) {
            var view = menu.getDevices();
            return view == null ? null : deviceTooltip(view, view.entries().get(cells.get(cell).entry()));
        }
        if (isFarms()) {
            var view = menu.getAutomation();
            if (view == null) {
                return null;
            }
            var e = view.entries().get(cells.get(cell).entry());
            var lines = new ArrayList<Component>();
            lines.add(Component.literal(e.name()));
            if (view.inFarm()) {
                lines.add(Component.translatable(e.state() == 0 ? "gui.appliedquartermaster.automation.offline_hint"
                        : e.state() == 2 ? "gui.appliedquartermaster.automation.on_strength" : "gui.appliedquartermaster.automation.off_strength",
                        e.strength()).withColor(0xFFAAAAAA));
                lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_switch").withColor(HINT));
                lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_strength_bar").withColor(HINT));
            } else {
                lines.add(Component.translatable(e.state() == 0 ? "gui.appliedquartermaster.automation.offline_hint"
                        : "gui.appliedquartermaster.automation.some_on", e.on(), e.total()).withColor(0xFFAAAAAA));
                lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_open").withColor(HINT));
            }
            lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_rename").withColor(HINT));
            lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_icon").withColor(HINT));
            return lines;
        }
        var c = cells.get(cell);
        if (c.entry() < 0) {
            return null;
        }
        var lines = new ArrayList<Component>(getTooltipFromContainerItem(c.stack()));
        if (kind() == StorageKind.LIBRARY) {
            lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_read").withColor(HINT));
            lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_pickup_right").withColor(HINT));
        } else {
            lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_pickup").withColor(HINT));
        }
        lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_take").withColor(HINT));
        return lines;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x() - leftPos;
        int my = (int) event.y() - topPos;
        int button = event.button();
        boolean carrying = !menu.getCarried().isEmpty();
        boolean shift = event.hasShiftDown();

        if (search != null && search.visible && search.isMouseOver(event.x(), event.y())) {
            if (button == 1) {
                search.setValue("");
            }
            search.setFocused(true);
            setFocused(search);
            return true;
        } else if (search != null) {
            search.setFocused(false);
        }
        if (rename.visible) {
            if (rename.isMouseOver(event.x(), event.y())) {
                return super.mouseClicked(event, doubleClick);
            }
            confirmRename();
        }

        // Home button on the bezel, and the dock.
        if (!carrying) {
            if (inside(mx, my, cw - BEZEL_X + 1, ch / 2 - 5, 10, 10)) {
                switchPage(TabletMenu.PAGE_HOME);
                playClick();
                return true;
            }
            int dock = dockAt(mx, my);
            if (dock == -1) {
                switchPage(TabletMenu.PAGE_HOME);
                playClick();
                return true;
            }
            if (dock >= 0) {
                var app = dockApps().get(dock);
                if (button == 1 && app.pinnable()) {
                    press(TabletMenu.BUTTON_PIN + app.pinValue());
                } else if (button == 0) {
                    openApp(app);
                }
                playClick();
                return true;
            }
        }

        if (isHome()) {
            int index = homeAppAt(mx, my);
            if (index >= 0 && !carrying) {
                var app = homeApps().get(index);
                if (button == 1 && app.pinnable()) {
                    press(TabletMenu.BUTTON_PIN + app.pinValue());
                } else if (button == 0) {
                    openApp(app);
                }
                playClick();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        // App bar: back.
        if (inside(mx, my, backX(), toolY(), TOOL, TOOL)) {
            if (inDeviceList()) {
                send(TabletActionPayload.DEVICE_BACK, -1, ItemStack.EMPTY, 0);
            } else if (inFarm()) {
                send(TabletActionPayload.BACK, -1, ItemStack.EMPTY, 0);
            } else {
                switchPage(TabletMenu.PAGE_HOME);
            }
            scroll = 0;
            playClick();
            return true;
        }
        int segment = segmentAt(mx, my);
        if (segment >= 0) {
            switchPage(segment == 0 ? TabletMenu.PAGE_DEVICES : TabletMenu.PAGE_STATS);
            playClick();
            return true;
        }
        for (int i = 0; i < toolCount(); i++) {
            if (inside(mx, my, toolX(i), toolY(), TOOL, TOOL)) {
                if (i == 0) {
                    sort = (sort + (button == 1 ? 2 : 1)) % 3;
                } else if (i == 1) {
                    showFree = !showFree;
                } else if (kind() != null) {
                    int next = (size() + (button == 1 ? 2 : 1)) % 3;
                    viewSize[kind().ordinal()] = next;
                    send(TabletActionPayload.SET_VIEW, kind().ordinal(), ItemStack.EMPTY, next);
                    scroll = 0;
                    relayout();
                }
                builtVersion = -1;
                playClick();
                return true;
            }
        }

        if (isSettings()) {
            for (int i = 0; i < TabletModules.SLOTS; i++) {
                if (!carrying && !menu.getModule(i).isEmpty() && onModulePin(mx, my, i)) {
                    press(TabletMenu.BUTTON_PIN + i);
                    playClick();
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        if (statusMessage() != null) {
            String settings = Component.translatable("gui.appliedquartermaster.tab.settings").getString();
            int w = font.width(settings) + 14;
            int bx = contentX() + contentW() / 2 - w / 2;
            if (inside(mx, my, bx, noNetButtonsY() - 20, w, 60)) {
                switchPage(TabletMenu.PAGE_MODULES);
                playClick();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        if (isStats()) {
            for (int i = 0; i < 3; i++) {
                if (inside(mx, my, periodX(i), statsPanelsY() + 3, 22, 11)) {
                    send(TabletActionPayload.STATS_PERIOD, -1, ItemStack.EMPTY, i);
                    playClick();
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        // Scroll bar.
        if (hasGrid() && inside(mx, my, gridX() + gridW() + 3, gridY(), SCROLL_W, gridH())) {
            scrollTo(my);
            return true;
        }
        if (isFarms()) {
            if (farmsClicked(mx, my, button, shift, carrying)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (isDevices()) {
            var view = menu.getDevices();
            int index = cellAt(mx, my);
            if (view != null && index >= 0) {
                send(view.inType() ? TabletActionPayload.DEVICE_LOCATE : TabletActionPayload.DEVICE_OPEN,
                        cells.get(index).entry(), ItemStack.EMPTY, 0);
                if (!view.inType()) {
                    scroll = 0;
                }
                playClick();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        // Storage grid.
        var kind = kind();
        if (kind != null && inGrid(mx, my)) {
            int index = cellAt(mx, my);
            var cell = index >= 0 ? cells.get(index) : null;
            if (cell == null || cell.entry() < 0) {
                if (carrying) {
                    send(TabletActionPayload.STORE, -1, ItemStack.EMPTY, 0);
                }
                return true;
            }
            if (shift) {
                send(TabletActionPayload.TAKE, cell.entry(), cell.stack(), 0);
            } else if (kind == StorageKind.LIBRARY && !carrying && button == 0) {
                read(cell);
            } else {
                send(TabletActionPayload.PICKUP, cell.entry(), cell.stack(), 0);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean farmsClicked(int mx, int my, int button, boolean shift, boolean carrying) {
        var view = menu.getAutomation();
        if (view != null && view.inFarm()) {
            String on = Component.translatable("gui.appliedquartermaster.automation.all_on").getString();
            String off = Component.translatable("gui.appliedquartermaster.automation.all_off").getString();
            int woff = font.width(off) + 10;
            int won = font.width(on) + 10;
            int offX = (hasGrid() ? searchX() - 4 : toolsRight()) - woff;
            int onX = offX - 3 - won;
            if (inside(mx, my, onX, toolY(), won, TOOL) || inside(mx, my, offX, toolY(), woff, TOOL)) {
                send(mx < offX ? TabletActionPayload.ALL_ON : TabletActionPayload.ALL_OFF, -1, ItemStack.EMPTY, 0);
                playClick();
                return true;
            }
            if (inside(mx, my, titleX(), toolY(), 150, TOOL)) {
                if (carrying || (shift && button == 1)) {
                    send(TabletActionPayload.SET_ICON, -1, ItemStack.EMPTY, 0);
                } else if (button == 1) {
                    startRename(-1, view.title());
                }
                return true;
            }
        }
        if (view == null || !inGrid(mx, my)) {
            return false;
        }
        int index = cellAt(mx, my);
        if (index < 0) {
            return true;
        }
        int entry = cells.get(index).entry();
        var e = view.entries().get(entry);
        int s = size();
        int cw = cellW(s);
        int col = (mx - gridX()) / cw;
        int rmx = mx - gridX() - col * cw;
        int rmy = my - gridY() - ((my - gridY()) / CELL_H[s]) * CELL_H[s];
        if (carrying || (shift && button == 1)) {
            // Holding an item: use it as the icon (the item is not used up). Sneak-right-click clears the icon.
            send(TabletActionPayload.SET_ICON, entry, ItemStack.EMPTY, 0);
        } else if (button == 1) {
            startRename(entry, e.name());
        } else if (view.inFarm()) {
            int segX = plateSegX(cw);
            if (inside(rmx, rmy, cw - 18, 4, 14, 14)) {
                startRename(entry, e.name());
            } else if (inside(rmx, rmy, segX, 2, 60, 18)) {
                int strength = Math.max(1, Math.min(15, (rmx - segX) / 4 + 1));
                send(TabletActionPayload.SET_STRENGTH, entry, ItemStack.EMPTY, strength);
            } else {
                send(TabletActionPayload.TOGGLE, entry, ItemStack.EMPTY, 0);
            }
        } else if (inside(rmx, rmy, cw - 30, 6, 24, 13)) {
            // The farm card's switch: all on, or all off when any is on.
            send(TabletActionPayload.OPEN_FARM, entry, ItemStack.EMPTY, 0);
            send(e.on() > 0 ? TabletActionPayload.ALL_OFF : TabletActionPayload.ALL_ON, -1, ItemStack.EMPTY, 0);
            send(TabletActionPayload.BACK, -1, ItemStack.EMPTY, 0);
        } else {
            send(TabletActionPayload.OPEN_FARM, entry, ItemStack.EMPTY, 0);
            scroll = 0;
        }
        playClick();
        return true;
    }

    /** A farm or plate (entry, or -1 for the open farm's title) that can take an icon dragged from JEI. */
    public record IconTarget(int entry, Rect2i area) {
    }

    /** Screen areas outside the tablet that JEI should keep clear: none, everything is inside the casing. */
    public List<Rect2i> getExtraAreas() {
        return List.of();
    }

    public List<IconTarget> getIconTargets() {
        var view = menu.getAutomation();
        var list = new ArrayList<IconTarget>();
        if (!isFarms() || view == null) {
            return list;
        }
        int s = size();
        int cw = cellW(s);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols(s); c++) {
                int index = (scroll + r) * cols(s) + c;
                if (index >= cells.size()) {
                    break;
                }
                list.add(new IconTarget(cells.get(index).entry(), new Rect2i(
                        leftPos + gridX() + c * cw, topPos + gridY() + r * CELL_H[s], cw, CELL_H[s])));
            }
        }
        if (view.inFarm()) {
            list.add(new IconTarget(-1, new Rect2i(leftPos + titleX(), topPos + toolY(), 150, TOOL)));
        }
        return list;
    }

    /** Sets a farm or plate icon to the given item (from JEI); the item is not used up. */
    public void setIconFromItem(int entry, ItemStack stack) {
        send(TabletActionPayload.SET_ICON, entry, stack.copyWithCount(1), 0);
    }

    private void startRename(int entry, String current) {
        renameEntry = entry;
        rename.setValue(current);
        rename.visible = true;
        if (entry == -1 || !hasGrid()) {
            rename.setX(leftPos + titleX() + 18);
            rename.setY(topPos + toolY() + 1);
            rename.setWidth(140);
        } else {
            rename.setX(leftPos + contentX());
            rename.setY(topPos + toolY() + 1);
            rename.setWidth(Math.max(80, searchX() - contentX() - 6));
        }
        rename.setFocused(true);
        setFocused(rename);
    }

    private void confirmRename() {
        if (renameEntry != Integer.MIN_VALUE) {
            send(TabletActionPayload.RENAME, renameEntry, ItemStack.EMPTY, 0, rename.getValue());
        }
        cancelRename();
    }

    private void cancelRename() {
        renameEntry = Integer.MIN_VALUE;
        if (rename != null) {
            rename.visible = false;
            rename.setFocused(false);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int mx = (int) event.x() - leftPos;
        int my = (int) event.y() - topPos;
        if (hasGrid() && inside(mx, my, gridX() + gridW() + 1, gridY() - 4, SCROLL_W + 4, gridH() + 8)) {
            scrollTo(my);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private void scrollTo(int my) {
        int total = totalRows();
        if (total <= rows) {
            return;
        }
        float f = (my - gridY()) / (float) Math.max(1, gridH());
        scroll = Math.round(f * (total - rows));
        clampScroll();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int mx = (int) x - leftPos;
        int my = (int) y - topPos;
        if (inFarm() && cellAt(mx, my) >= 0) {
            // Scroll over a plate: change its signal strength.
            int entry = cells.get(cellAt(mx, my)).entry();
            send(TabletActionPayload.STRENGTH, entry, ItemStack.EMPTY, scrollY > 0 ? 1 : -1);
            return true;
        }
        if (inGrid(mx, my)) {
            scroll -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (rename != null && rename.visible) {
            if (event.isEscape()) {
                cancelRename();
            } else if (event.key() == 257 || event.key() == 335) {
                confirmRename();
            } else {
                rename.keyPressed(event);
            }
            return true;
        }
        if (search != null && search.isFocused() && search.visible && !event.isEscape()) {
            search.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        double rx = mx - xo;
        double ry = my - yo;
        return rx < 0 || ry < 0 || rx >= cw || ry >= ch;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (builtVersion != menu.getViewVersion()) {
            clampScroll();
        }
    }

    /** Opens a stored book: written books are read right here; other guide books are opened like a right-click. */
    private void read(Cell cell) {
        var stack = cell.stack();
        if (stack.has(DataComponents.WRITTEN_BOOK_CONTENT) || stack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            var access = BookViewScreen.BookAccess.fromItem(stack);
            if (access != null && minecraft != null) {
                minecraft.setScreen(new BookViewScreen(access));
            }
            return;
        }
        // Some guide books open on the client (GuideME, AE2's guide), others on the server (Patchouli, Modonomicon).
        if (minecraft != null && minecraft.player != null && minecraft.level != null) {
            var player = minecraft.player;
            var inventory = player.getInventory();
            int hand = inventory.getSelectedSlot();
            var previous = inventory.getItem(hand);
            inventory.setItem(hand, stack.copy());
            try {
                stack.getItem().use(minecraft.level, player, InteractionHand.MAIN_HAND);
            } catch (RuntimeException ignored) {
                // Fall back to the server below.
            } finally {
                inventory.setItem(hand, previous);
            }
            if (minecraft.screen != this) {
                return;
            }
        }
        send(TabletActionPayload.READ, cell.entry(), stack, 0);
    }

    private void send(int action, int entry, ItemStack expected, int arg) {
        send(action, entry, expected, arg, "");
    }

    private void send(int action, int entry, ItemStack expected, int arg, String text) {
        ClientPacketDistributor.sendToServer(new TabletActionPayload(menu.containerId, action, entry, expected.copy(), arg, text));
    }

    private void press(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void playClick() {
        if (minecraft != null) {
            net.minecraft.client.gui.components.AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // ---------------------------------------------------------------- pixel icons

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
    private static final String[] BACK_ICON = {
            "...##...",
            "..##....",
            ".##.....",
            "##......",
            ".##.....",
            "..##....",
            "...##...",
            "........",
    };
    private static final String[] HOME_ICON = {
            "###.###.",
            "###.###.",
            "###.###.",
            "........",
            "###.###.",
            "###.###.",
            "###.###.",
            "........",
            "........",
            "........",
    };
    private static final String[] DOWN_ICON = {
            "#####",
            ".###.",
            "..#..",
    };
    private static final String[] PENCIL_ICON = {
            "......#.",
            ".....###",
            "....###.",
            "...###..",
            "..###...",
            ".###....",
            "##......",
            "........",
    };

    // 12x12 toolbar icons: sort (storage order, A-Z, Z-A), free-spot filter, view size (Large, Medium, Small).
    private static final String[][] SORT_ICONS = {
            {"............", ".####.####..", ".#..#.#..#..", ".####.####..", "............", ".####.####..",
                    ".#..#.#..#..", ".####.####..", "............", "............", "............", "............"},
            {".##.....###.", "#..#......#.", "####.....#..", "#..#....#...", "#..#....###.", "............",
                    ".....#......", ".....#......", "...#####....", "....###.....", ".....#......", "............"},
            {"###......##.", "..#.....#..#", ".#......####", "#.......#..#", "###.....#..#", "............",
                    ".....#......", ".....#......", "...#####....", "....###.....", ".....#......", "............"},
    };
    private static final String[] FILTER_OFF = {"############", "#..........#", ".#........#.", "..#......#..", "...#....#...",
            "....#..#....", "....#..#....", "....#..#....", "....#..#....", "....#..#....", ".....##.....", "............"};
    private static final String[] FILTER_ON = {"############", "############", ".##########.", "..########..", "...######...",
            "....####....", "....####....", "....####....", "....####....", "....####....", ".....##.....", "............"};
    private static final String[][] SIZE_ICONS = {
            {"#####.#####.", "#...#.#...#.", "#...#.#...#.", "#...#.#...#.", "#####.#####.", "............",
                    "#####.#####.", "#...#.#...#.", "#...#.#...#.", "#...#.#...#.", "#####.#####.", "............"},
            {"###.###.###.", "#.#.#.#.#.#.", "###.###.###.", "............", "###.###.###.", "#.#.#.#.#.#.",
                    "###.###.###.", "............", "###.###.###.", "#.#.#.#.#.#.", "###.###.###.", "............"},
            {"##.##.##.##.", "##.##.##.##.", "............", "##.##.##.##.", "##.##.##.##.", "............",
                    "##.##.##.##.", "##.##.##.##.", "............", "##.##.##.##.", "##.##.##.##.", "............"},
    };
}
