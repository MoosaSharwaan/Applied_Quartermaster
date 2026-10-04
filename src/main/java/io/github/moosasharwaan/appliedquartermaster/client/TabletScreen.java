package io.github.moosasharwaan.appliedquartermaster.client;

import io.github.moosasharwaan.appliedquartermaster.network.DevicesViewPayload;
import io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModules;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletNetwork;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
 * The opened ME Tablet.
 * <ul>
 * <li>Tab bar: one tab per installed module, then Library, Armory and Tools when their block is on the linked
 * network; the pinned default tab has a gold pin; the Modules gear sits at the top right.</li>
 * <li>Modules page: module slots and the player inventory.</li>
 * <li>Storage pages: AE2 terminal style grid of stored items with names, boxes only for free spots, scrolling,
 * a search box, and sort / filter / view-size buttons on the left; the player inventory below for storing.</li>
 * </ul>
 */
public class TabletScreen extends AbstractContainerScreen<TabletMenu> {

    // ---------------------------------------------------------------- layout
    private static final int W = 338;
    private static final int TAB_W = 26;
    private static final int TAB_H = 22;
    private static final int PANEL_Y = TAB_H - 2;
    private static final int HEADER_H = 18;
    private static final int GRID_X = 8;
    private static final int GRID_W = 17 * 18;
    private static final int SCROLL_W = 12;
    private static final int INV_SECTION_H = 10 + 3 * 18 + 4 + 18 + 6;
    private static final int BOX = 36;
    private static final int MODULES_CONTENT_H = 62;
    private static final int TOOL_X = -22;
    private static final int UPGRADE_X = 196;
    private static final int BATTERY_X = 276;

    /** Large, Medium, Small: columns, cell size, item scale, rows at most. */
    /** Index 3: Automation and the Devices overview (big icons). Index 4: the device list (one row per device). */
    private static final int[] COLS = {5, 8, 17, 5, 1};
    private static final int[] CELL_W = {61, 38, 18, 61, 17 * 18};
    private static final int[] CELL_H = {66, 38, 18, 66, 20};
    private static final int[] SCALE = {3, 2, 1, 3, 1};
    private static final int[] MAX_ROWS = {3, 4, 10, 3, 9};
    private static final int SIZE_AUTOMATION = 3;
    private static final int SIZE_LIST = 4;
    private static final int GREEN = 0xFF2E9A44;
    private static final int RED = 0xFFBE2828;
    private static final String[] SIZE_KEYS = {"large", "medium", "small"};

    private static final int SORT_STORAGE = 0;
    private static final int SORT_AZ = 1;
    private static final int SORT_ZA = 2;

    // ---------------------------------------------------------------- palette (AE2 / vanilla GUI)
    private static final int OUTLINE = 0xFF413F54;
    private static final int FACE = 0xFFCBCCD4;
    private static final int LIGHT = 0xFFF2F2F2;
    private static final int SHADOW = 0xFF878FA5;
    private static final int SLOT = 0xFFADB0C4;
    private static final int SLOT_DARK = 0xFF878FA5;
    private static final int TAB_IDLE = 0xFFADB0C4;
    private static final int GRID_BG = 0xFFBDBFCC;
    private static final int BUTTON = 0xFF4D4D67;
    private static final int BUTTON_HOVER = 0xFF63637F;
    private static final int HOVER = 0x60FFFFFF;
    private static final int GOLD = 0xFFE8B53A;
    private static final int GOLD_DARK = 0xFF8A5E12;
    private static final int TEXT = 0xFF413F54;
    private static final int TEXT_DIM = 0xFF7A7D93;
    private static final int HINT = 0xFF7FD7FF;

    /** A tab: a module slot (module >= 0), or a page (Library, Armory, Tools or Automation). */
    private record Tab(int module, int page) {
        int pinValue() {
            return module >= 0 ? module : TabletModules.PIN_STORAGE + page;
        }
    }

    /** One grid cell: a stored item (entry index into the server list) or a free box (entry -1). */
    private record Cell(int entry, ItemStack stack) {
    }

    private int panelH;
    private int rows;
    private int scroll;
    private int sort = SORT_STORAGE;
    private boolean showFree = true;
    private final int[] viewSize = new int[StorageKind.values().length];
    private final List<Cell> cells = new ArrayList<>();
    private int builtVersion = -1;
    private String builtSearch = "";
    private EditBox search;
    private EditBox rename;
    private int lastDeviceSize = -1;
    /** What the rename box renames: an entry index, -1 for the open farm. */
    private int renameEntry = Integer.MIN_VALUE;

    public TabletScreen(TabletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, 200);
        for (var kind : StorageKind.values()) {
            viewSize[kind.ordinal()] = menu.getInitialViewSize(kind);
        }
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, 0, 0, 90, 10, Component.translatable("gui.appliedquartermaster.tablet.search"));
        search.setBordered(false);
        search.setMaxLength(50);
        search.setTextColor(0xFFFFFFFF);
        search.setHint(Component.translatable("gui.appliedquartermaster.tablet.search").withColor(0xFFA0A0B0));
        search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);
        rename = new EditBox(font, 0, 0, 150, 12, Component.translatable("gui.appliedquartermaster.automation.rename"));
        rename.setMaxLength(40);
        rename.visible = false;
        addRenderableWidget(rename);
        relayout();
    }

    private StorageKind kind() {
        return menu.getPageKind();
    }

    private boolean isModulesPage() {
        return menu.getPage() == TabletMenu.PAGE_MODULES;
    }

    private boolean isAutomationPage() {
        return menu.getPage() == TabletMenu.PAGE_AUTOMATION;
    }

    private boolean isDevicesPage() {
        return menu.getPage() == TabletMenu.PAGE_DEVICES;
    }

    private boolean inDeviceList() {
        var view = menu.getDevices();
        return isDevicesPage() && view != null && view.inType();
    }

    private int size() {
        if (isAutomationPage()) {
            return SIZE_AUTOMATION;
        }
        if (isDevicesPage()) {
            return inDeviceList() ? SIZE_LIST : SIZE_AUTOMATION;
        }
        var kind = kind();
        return kind == null ? 0 : viewSize[kind.ordinal()];
    }

    private int gridY() {
        return PANEL_Y + HEADER_H;
    }

    private int gridH() {
        return rows * CELL_H[size()];
    }

    /** The player inventory, centred under the page. */
    private int inventoryX() {
        return (W - 162) / 2;
    }

    private int inventoryY() {
        return panelH + PANEL_Y - INV_SECTION_H + 10;
    }

    /** Sizes the panel for the open page and screen height, then moves the slots to match. */
    private void relayout() {
        int contentH;
        if (isModulesPage()) {
            rows = 0;
            contentH = HEADER_H + MODULES_CONTENT_H;
        } else {
            int s = size();
            int available = height - 4 - PANEL_Y - HEADER_H - INV_SECTION_H - 4;
            rows = Math.max(1, Math.min(MAX_ROWS[s], available / CELL_H[s]));
            contentH = HEADER_H + rows * CELL_H[s] + 2;
        }
        panelH = contentH + INV_SECTION_H;
        int totalH = PANEL_Y + panelH;
        leftPos = (width - W) / 2;
        topPos = Math.max(2, (height - totalH) / 2);

        int moduleX0 = 14 + (BOX - 16) / 2;
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var slot = menu.slots.get(i);
            slot.x = moduleX0 + i * 40;
            slot.y = PANEL_Y + HEADER_H + (BOX - 16) / 2 + 4;
        }
        for (int i = 0; i < TabletModules.UPGRADE_SLOTS; i++) {
            var slot = menu.slots.get(TabletModules.SLOTS + i);
            slot.x = UPGRADE_X + i * 20;
            slot.y = PANEL_Y + HEADER_H + (BOX - 16) / 2 + 4;
        }
        int ix = inventoryX();
        int iy = inventoryY();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                var slot = menu.slots.get(TabletMenu.FIRST_PLAYER_SLOT + row * 9 + col);
                slot.x = ix + col * 18;
                slot.y = iy + row * 18;
            }
        }
        for (int col = 0; col < 9; col++) {
            var slot = menu.slots.get(TabletMenu.FIRST_PLAYER_SLOT + 27 + col);
            slot.x = ix + col * 18;
            slot.y = iy + 58;
        }
        if (search != null) {
            search.visible = !isModulesPage();
            search.setX(leftPos + W - 8 - 92 + 2);
            search.setY(topPos + PANEL_Y + 6);
            if (isModulesPage()) {
                search.setFocused(false);
            }
        }
        cancelRename();
        builtVersion = -1;
        clampScroll();
    }

    // ---------------------------------------------------------------- tabs

    private List<Tab> tabs() {
        var list = new ArrayList<Tab>();
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            if (!menu.getModule(i).isEmpty()) {
                list.add(new Tab(i, TabletMenu.PAGE_MODULES));
            }
        }
        for (int page = 0; page < TabletModules.PAGES; page++) {
            if (menu.isPageUnlocked(page) || menu.getPage() == page) {
                list.add(new Tab(-1, page));
            }
        }
        return list;
    }

    private static int tabX(int index) {
        return 4 + index * (TAB_W + 2);
    }

    private static int gearX() {
        return W - 4 - TAB_W;
    }

    private static ItemStack tabIcon(int page) {
        ItemLike item = switch (page) {
            case 0 -> ModItems.ME_LIBRARY.get();
            case 1 -> ModItems.ME_ARMORY.get();
            case 2 -> ModItems.ME_TOOL_RACK.get();
            case 3 -> ModItems.ME_FARM_CONTROLLER.get(); // Automation
            default -> appeng.core.definitions.AEBlocks.CONTROLLER.asItem();
        };
        return new ItemStack(item);
    }

    private static Component pageTitle(int page) {
        var kind = StorageKind.byIndex(page);
        if (kind != null) {
            return kind.title();
        }
        return Component.translatable(page == TabletMenu.PAGE_DEVICES
                ? "gui.appliedquartermaster.tab.devices" : "gui.appliedquartermaster.tab.automation");
    }

    private ItemStack tabStack(Tab tab) {
        return tab.module() >= 0 ? menu.getModule(tab.module()) : tabIcon(tab.page());
    }

    private boolean isOpen(Tab tab) {
        return tab.module() < 0 && menu.getPage() == tab.page();
    }

    private void switchPage(int page) {
        if (menu.getPage() == page) {
            return;
        }
        menu.setPageClient(page);
        press(TabletMenu.BUTTON_PAGE + page + 1);
        scroll = 0;
        if (search != null) {
            search.setValue("");
        }
        relayout();
    }

    // ---------------------------------------------------------------- grid contents

    private void rebuildCells() {
        String text = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (builtVersion == menu.getViewVersion() && text.equals(builtSearch)) {
            return;
        }
        builtVersion = menu.getViewVersion();
        builtSearch = text;
        cells.clear();
        if (isDevicesPage()) {
            // The overview and the device list use different layouts.
            if (rows == 0 || lastDeviceSize != size()) {
                lastDeviceSize = size();
                scroll = 0;
                relayout();
                builtVersion = menu.getViewVersion();
            }
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
        if (isAutomationPage()) {
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
        int cols = COLS[size()];
        return (cells.size() + cols - 1) / cols;
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, totalRows() - rows));
    }

    /** Index into {@link #cells} under the mouse (screen-relative coordinates), or -1. */
    private int cellAt(int mx, int my) {
        if (isModulesPage()) {
            return -1;
        }
        int s = size();
        int gx = mx - GRID_X;
        int gy = my - gridY();
        if (gx < 0 || gy < 0 || gx >= COLS[s] * CELL_W[s] || gy >= rows * CELL_H[s]) {
            return -1;
        }
        int index = (scroll + gy / CELL_H[s]) * COLS[s] + gx / CELL_W[s];
        return index < cells.size() ? index : -1;
    }

    private boolean inGrid(int mx, int my) {
        return !isModulesPage() && inside(mx, my, GRID_X, gridY(), GRID_W, gridH());
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        rebuildCells();
        int x = leftPos;
        int y = topPos;
        int mx = mouseX - x;
        int my = mouseY - y;
        int pinned = menu.getDefaultTab();
        var tabs = tabs();

        // Inactive tabs sit behind the panel.
        for (int t = 0; t < tabs.size(); t++) {
            if (!isOpen(tabs.get(t))) {
                bevel(g, x + tabX(t), y + 3, TAB_W, TAB_H - 1, TAB_IDLE);
            }
        }
        if (!isModulesPage()) {
            bevel(g, x + gearX(), y + 3, TAB_W, TAB_H - 1, TAB_IDLE);
        }
        // AE2-style backing strip behind the left toolbar.
        if (!isModulesPage()) {
            int stripH = toolCount() * 20 + 6;
            g.fill(x + TOOL_X - 3, y + PANEL_Y + 1, x + 1, y + PANEL_Y + 1 + stripH, OUTLINE);
            g.fill(x + TOOL_X - 2, y + PANEL_Y + 2, x + 1, y + PANEL_Y + stripH, FACE);
            g.fill(x + TOOL_X - 2, y + PANEL_Y + 2, x + 1, y + PANEL_Y + 3, LIGHT);
            g.fill(x + TOOL_X - 2, y + PANEL_Y + 2, x + TOOL_X - 1, y + PANEL_Y + stripH - 1, LIGHT);
            g.fill(x + TOOL_X - 1, y + PANEL_Y + stripH - 1, x + 1, y + PANEL_Y + stripH, SHADOW);
        }

        panel(g, x, y + PANEL_Y, W, panelH);

        // Open tab joined to the panel.
        for (int t = 0; t < tabs.size(); t++) {
            if (isOpen(tabs.get(t))) {
                openTab(g, x + tabX(t), y);
            }
        }
        if (isModulesPage()) {
            openTab(g, x + gearX(), y);
        }
        gear(g, x + gearX() + 9, y + (isModulesPage() ? 7 : 9), 0xFF505050);

        for (int t = 0; t < tabs.size(); t++) {
            var tab = tabs.get(t);
            int ty = isOpen(tab) ? y + 3 : y + 4;
            g.item(tabStack(tab), x + tabX(t) + 5, ty);
            if (tab.pinValue() == pinned) {
                pin(g, x + tabX(t) + TAB_W - 8, y + 3, true);
            }
        }

        // Left toolbar (storage pages; Automation has sort only), drawn like AE2's terminal buttons.
        if (!isModulesPage()) {
            for (int i = 0; i < toolCount(); i++) {
                int bx = x + TOOL_X;
                int by = y + PANEL_Y + 4 + i * 20;
                g.fill(bx, by, bx + 18, by + 18, OUTLINE);
                g.fill(bx + 1, by + 1, bx + 17, by + 17,
                        inside(mx, my, TOOL_X, PANEL_Y + 4 + i * 20, 18, 18) ? BUTTON_HOVER : BUTTON);
                String[] icon = i == 0 ? SORT_ICONS[sort] : i == 1 ? (showFree ? FILTER_OFF : FILTER_ON) : SIZE_ICONS[size()];
                pixels(g, icon, bx + 3, by + 3, 0xFFF2F2F2);
            }
        }

        if (isModulesPage()) {
            drawModulesPage(g, x, y, pinned);
        } else if (isAutomationPage()) {
            drawAutomationPage(g, x, y, mx, my);
        } else if (isDevicesPage()) {
            drawDevicesPage(g, x, y, mx, my);
        } else {
            drawStoragePage(g, x, y, mx, my);
        }

        // Inventory.
        g.text(font, playerInventoryTitle, x + inventoryX(), y + inventoryY() - 10, TEXT, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                inset(g, x + inventoryX() - 1 + col * 18, y + inventoryY() - 1 + row * 18, 18, 18, SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            inset(g, x + inventoryX() - 1 + col * 18, y + inventoryY() + 57, 18, 18, SLOT);
        }
    }

    private void drawModulesPage(GuiGraphicsExtractor g, int x, int y, int pinned) {
        g.text(font, Component.translatable("gui.appliedquartermaster.tablet.modules"), x + 8, y + PANEL_Y + 7, TEXT, false);
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var slot = menu.slots.get(i);
            int bx = x + slot.x - (BOX - 16) / 2;
            int by = y + slot.y - (BOX - 16) / 2;
            inset(g, bx, by, BOX, BOX, i == pinned ? 0xFFE3D6B0 : SLOT);
            if (i == pinned) {
                g.outline(bx - 1, by - 1, BOX + 2, BOX + 2, GOLD);
            }
            inset(g, x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT);
            var module = menu.getModule(i);
            int cx = bx + BOX / 2;
            if (module.isEmpty()) {
                centered(g, Component.translatable("gui.appliedquartermaster.tablet.empty_slot"), cx, by + BOX + 3, TEXT_DIM);
            } else {
                pin(g, bx + BOX - 9, by + 2, i == pinned);
                smallLines(g, module.getHoverName().getString(), cx, by + BOX + 3, BOX + 2, 2, i == pinned ? GOLD_DARK : TEXT);
            }
        }
        // Upgrades (wireless boosters) and the terminal battery.
        var first = menu.slots.get(TabletModules.SLOTS);
        int ux = x + first.x;
        int uy = y + first.y;
        centered(g, Component.translatable("gui.appliedquartermaster.tablet.upgrades"), ux + 18, uy - 21, TEXT);
        for (int i = 0; i < TabletModules.UPGRADE_SLOTS; i++) {
            var slot = menu.slots.get(TabletModules.SLOTS + i);
            inset(g, x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT);
        }
        smallLines(g, rangeText(), ux + 19, uy + 21, 60, 2, TEXT_DIM);
        int bx = x + BATTERY_X;
        centered(g, Component.translatable("gui.appliedquartermaster.tablet.battery"), bx + 6, uy - 21, TEXT);
        double level = batteryLevel();
        inset(g, bx, uy - 10, 12, 38, 0xFF2A2938);
        if (level >= 0) {
            int h = (int) Math.round(36 * level);
            int color = level > 0.5 ? 0xFF5AE66E : level > 0.15 ? 0xFFE8B53A : 0xFFEB3C32;
            g.fill(bx + 1, uy - 9 + 36 - h, bx + 11, uy + 27, color);
        }
        smallLines(g, level < 0 ? "-" : Math.round(level * 100) + "%", bx + 6, uy + 31, 40, 1, TEXT_DIM);
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
        int boosters = TabletModules.boosters(tablet);
        return Component.translatable("gui.appliedquartermaster.tablet.range", 1 + boosters).getString();
    }

    private void drawStoragePage(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var kind = kind();
        int s = size();
        g.text(font, kind.title(), x + 8, y + PANEL_Y + 7, TEXT, false);
        drawGridFrame(g, x, y);
        int gx = x + GRID_X;
        int gy = y + gridY();
        String message = statusMessage();
        if (message == null && !menu.isUnlocked(kind)) {
            message = "gui.appliedquartermaster.tablet.no_block." + kind.id();
        }
        if (message != null) {
            g.textWithWordWrap(font, Component.translatable(message), gx + 20, gy + gridH() / 2 - 8, GRID_W - 40, TEXT);
            return;
        }

        int cols = COLS[s];
        int hovered = cellAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = (scroll + r) * cols + c;
                if (index >= cells.size()) {
                    break;
                }
                var cell = cells.get(index);
                int cx = gx + c * CELL_W[s];
                int cy = gy + r * CELL_H[s];
                int iconSize = 16 * SCALE[s];
                int ix = cx + (CELL_W[s] - iconSize) / 2;
                int iy = cy + (s == 0 ? 2 : (CELL_H[s] - iconSize) / 2);
                if (index == hovered) {
                    g.fill(cx + 1, cy + 1, cx + CELL_W[s] - 1, cy + CELL_H[s] - 1, HOVER);
                }
                if (cell.entry() < 0) {
                    // Only real free spots get a box: a light, sunken outline the size of an item.
                    freeSpot(g, ix, iy, iconSize);
                    continue;
                }
                g.pose().pushMatrix();
                g.pose().translate(ix, iy);
                g.pose().scale(SCALE[s], SCALE[s]);
                g.item(cell.stack(), 0, 0);
                g.itemDecorations(font, cell.stack(), 0, 0);
                g.pose().popMatrix();
                if (s == 0) {
                    smallLines(g, cell.stack().getHoverName().getString(), cx + CELL_W[s] / 2, iy + iconSize + 2,
                            CELL_W[s] - 2, 2, TEXT);
                }
            }
        }
        String count = menu.getViewItems().size() + " / " + (menu.getViewItems().size() + menu.getViewFree());
        g.text(font, count, x + W - 8 - 92 - 6 - font.width(count), y + PANEL_Y + 7, TEXT_DIM, false);
    }

    private int toolCount() {
        return isAutomationPage() || isDevicesPage() ? 1 : 3;
    }

    // ---------------------------------------------------------------- Devices page

    private static String formatAe(double value) {
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

    private void drawDevicesPage(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var view = menu.getDevices();
        boolean list = view != null && view.inType();
        int hy = y + PANEL_Y + 4;
        if (list) {
            smallButton(g, x + BACK_X, hy, BACK_W, "<", inside(mx, my, BACK_X, PANEL_Y + 4, BACK_W, 13));
            int tx = x + BACK_X + BACK_W + 4;
            g.pose().pushMatrix();
            g.pose().translate(tx, hy + 1);
            g.pose().scale(0.75f, 0.75f);
            g.item(view.titleIcon(), 0, 0);
            g.pose().popMatrix();
            var title = view.title() + " (" + view.entries().size() + ")";
            g.text(font, shortName(title, W - 8 - 92 - 12 - (tx - x) - 15), tx + 15, y + PANEL_Y + 7, TEXT, false);
        } else {
            g.text(font, Component.translatable("gui.appliedquartermaster.tab.devices"), x + 8, y + PANEL_Y + 7, TEXT, false);
            if (view != null && statusMessage() == null) {
                var summary = Component.translatable("gui.appliedquartermaster.devices.summary",
                        view.summary().devices(), view.summary().channels());
                g.text(font, summary, x + W - 8 - 92 - 6 - font.width(summary), y + PANEL_Y + 7, TEXT_DIM, false);
            }
        }
        drawGridFrame(g, x, y);
        int gx = x + GRID_X;
        int gy = y + gridY();
        String message = statusMessage();
        if (message != null || view == null) {
            if (message != null) {
                g.textWithWordWrap(font, Component.translatable(message), gx + 20, gy + gridH() / 2 - 8, GRID_W - 40, TEXT);
            }
            return;
        }
        int s = size();
        int hovered = cellAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS[s]; c++) {
                int index = (scroll + r) * COLS[s] + c;
                if (index >= cells.size()) {
                    break;
                }
                var e = view.entries().get(cells.get(index).entry());
                int cx = gx + c * CELL_W[s];
                int cy = gy + r * CELL_H[s];
                if (index == hovered) {
                    g.fill(cx + 1, cy + 1, cx + CELL_W[s] - 1, cy + CELL_H[s] - 1, HOVER);
                }
                if (list) {
                    drawDeviceRow(g, e, cx, cy, r);
                } else {
                    drawDeviceKind(g, e, cx, cy, s);
                }
            }
        }
    }

    private void drawDeviceKind(GuiGraphicsExtractor g, DevicesViewPayload.Entry e, int cx, int cy, int s) {
        int ix = cx + (CELL_W[s] - 48) / 2;
        int iy = cy + 2;
        g.pose().pushMatrix();
        g.pose().translate(ix, iy);
        g.pose().scale(3, 3);
        g.item(e.icon(), 0, 0);
        g.pose().popMatrix();
        dot(g, ix + 42.5f, iy + 42.5f, 8, e.active() == e.count());
        smallLines(g, e.name(), cx + CELL_W[s] / 2, iy + 50, CELL_W[s] - 2, 1, TEXT);
        int offline = e.count() - e.active();
        var sub = offline == 0
                ? Component.translatable("gui.appliedquartermaster.devices.count", e.count())
                : Component.translatable("gui.appliedquartermaster.devices.count_offline", e.count(), offline);
        smallLines(g, sub.getString(), cx + CELL_W[s] / 2, iy + 57, CELL_W[s] - 2, 1, offline == 0 ? GREEN : RED);
    }

    private void drawDeviceRow(GuiGraphicsExtractor g, DevicesViewPayload.Entry e, int cx, int cy, int row) {
        if (row % 2 == 1) {
            g.fill(cx + 1, cy, cx + CELL_W[SIZE_LIST] - 1, cy + CELL_H[SIZE_LIST], 0x18413F54);
        }
        g.item(e.icon(), cx + 3, cy + 2);
        var pos = e.pos();
        String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        g.text(font, where, cx + 24, cy + 6, TEXT, false);
        if (!e.dimension().isEmpty() && !e.dimension().equals("minecraft:overworld")) {
            String dim = e.dimension().contains(":") ? e.dimension().substring(e.dimension().indexOf(':') + 1) : e.dimension();
            smallLines(g, dim, cx + 24 + font.width(where) + 4 + font.width(dim) * 3 / 8, cy + 7, 80, 1, TEXT_DIM);
        }
        // State chip
        var state = Component.translatable("gui.appliedquartermaster.devices.state." + stateKey(e.state())).getString();
        int chipX = cx + 150;
        dot(g, chipX + 3, cy + 8, 6, e.state() == DevicesViewPayload.ACTIVE);
        g.text(font, state, chipX + 9, cy + 6, TEXT, false);
        String right = (e.channels() > 0 ? e.channels() + " ch · " : "") + formatAe(e.power()) + " AE/t";
        g.text(font, right, cx + CELL_W[SIZE_LIST] - 6 - font.width(right), cy + 6, TEXT_DIM, false);
    }

    private boolean devicesClicked(int mx, int my, int button, MouseButtonEvent event) {
        var view = menu.getDevices();
        if (inside(mx, my, TOOL_X, PANEL_Y + 4, 18, 18)) {
            sort = (sort + (button == 1 ? 2 : 1)) % 3;
            builtVersion = -1;
            playClick();
            return true;
        }
        if (view != null && view.inType() && inside(mx, my, BACK_X, PANEL_Y + 4, BACK_W, 13)) {
            send(TabletActionPayload.DEVICE_BACK, -1, ItemStack.EMPTY, 0);
            playClick();
            return true;
        }
        if (inside(mx, my, GRID_X + GRID_W + 4, gridY(), SCROLL_W, gridH())) {
            scrollTo(my);
            return true;
        }
        if (view == null || !inGrid(mx, my) || menu.getStatus() != TabletNetwork.OK) {
            return false;
        }
        int index = cellAt(mx, my);
        if (index < 0) {
            return true;
        }
        int entry = cells.get(index).entry();
        send(view.inType() ? TabletActionPayload.DEVICE_LOCATE : TabletActionPayload.DEVICE_OPEN, entry, ItemStack.EMPTY, 0);
        playClick();
        return true;
    }

    private List<Component> deviceTooltip(DevicesViewPayload view, DevicesViewPayload.Entry e) {
        var lines = new ArrayList<Component>();
        lines.add(Component.literal(e.name()));
        if (view.inType()) {
            lines.add(Component.translatable("gui.appliedquartermaster.devices.position",
                    e.pos().getX(), e.pos().getY(), e.pos().getZ(), e.dimension()).withColor(TEXT_DIM));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.state." + stateKey(e.state())).withColor(TEXT_DIM));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.usage", e.channels(), formatAe(e.power())).withColor(TEXT_DIM));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.hint_locate").withColor(HINT));
        } else {
            lines.add(Component.translatable("gui.appliedquartermaster.devices.kind_detail", e.count(), e.active(),
                    e.channels(), formatAe(e.power())).withColor(TEXT_DIM));
            lines.add(Component.translatable("gui.appliedquartermaster.devices.hint_open").withColor(HINT));
        }
        return lines;
    }

    /** Grid well, scroll bar and search well shared by the storage and Automation pages. */
    private void drawGridFrame(GuiGraphicsExtractor g, int x, int y) {
        inset(g, x + W - 8 - 92, y + PANEL_Y + 4, 92, 13, 0xFF9A9FB4);
        int gx = x + GRID_X;
        int gy = y + gridY();
        inset(g, gx - 1, gy - 1, GRID_W + 2, gridH() + 2, GRID_BG);
        int barX = gx + GRID_W + 4;
        inset(g, barX, gy - 1, SCROLL_W, gridH() + 2, 0xFF9A9AA4);
        int total = totalRows();
        int knobH = Math.max(12, total <= rows ? gridH() : gridH() * rows / total);
        int knobY = total <= rows ? 0 : (gridH() - knobH) * scroll / (total - rows);
        bevel(g, barX + 1, gy + knobY, SCROLL_W - 2, knobH, total <= rows ? 0xFFB0B0B0 : FACE);
    }

    private String statusMessage() {
        return switch (menu.getStatus()) {
            case TabletNetwork.NO_TERMINAL -> "gui.appliedquartermaster.tablet.no_terminal";
            case TabletNetwork.NOT_LINKED -> "gui.appliedquartermaster.tablet.not_linked";
            case TabletNetwork.OUT_OF_RANGE -> "gui.appliedquartermaster.tablet.out_of_range";
            default -> null;
        };
    }

    /** Header buttons on the Automation page: back, All on, All off (inside a farm). */
    private static final int BACK_X = 8;
    private static final int BACK_W = 14;

    private int allOnX() {
        return W - 8 - 92 - 4 - 2 * 40;
    }

    private void drawAutomationPage(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var view = menu.getAutomation();
        boolean inFarm = view != null && view.inFarm();
        int hy = y + PANEL_Y + 4;
        if (inFarm) {
            smallButton(g, x + BACK_X, hy, BACK_W, "<", inside(mx, my, BACK_X, PANEL_Y + 4, BACK_W, 13));
            if (renameEntry != -1) {
                int tx = x + BACK_X + BACK_W + 4;
                g.pose().pushMatrix();
                g.pose().translate(tx, hy + 1);
                g.pose().scale(0.75f, 0.75f);
                g.item(view.titleIcon(), 0, 0);
                g.pose().popMatrix();
                g.text(font, shortName(view.title(), allOnX() - BACK_X - BACK_W - 24), tx + 15, y + PANEL_Y + 7, TEXT, false);
            }
            smallButton(g, x + allOnX(), hy, 38, Component.translatable("gui.appliedquartermaster.automation.all_on").getString(),
                    inside(mx, my, allOnX(), PANEL_Y + 4, 38, 13));
            smallButton(g, x + allOnX() + 40, hy, 38, Component.translatable("gui.appliedquartermaster.automation.all_off").getString(),
                    inside(mx, my, allOnX() + 40, PANEL_Y + 4, 38, 13));
        } else {
            g.text(font, Component.translatable("gui.appliedquartermaster.automation.farms"), x + 8, y + PANEL_Y + 7, TEXT, false);
        }
        drawGridFrame(g, x, y);

        int gx = x + GRID_X;
        int gy = y + gridY();
        String message = statusMessage();
        if (message == null && !menu.isPageUnlocked(TabletMenu.PAGE_AUTOMATION)) {
            message = "gui.appliedquartermaster.tablet.no_block.automation";
        } else if (message == null && view != null && view.entries().isEmpty()) {
            message = inFarm ? "gui.appliedquartermaster.automation.no_plates" : "gui.appliedquartermaster.tablet.no_block.automation";
        }
        if (message != null || view == null) {
            if (message != null) {
                g.textWithWordWrap(font, Component.translatable(message), gx + 20, gy + gridH() / 2 - 8, GRID_W - 40, TEXT);
            }
            return;
        }
        if (!inFarm) {
            int plates = 0;
            int offline = 0;
            for (var e : view.entries()) {
                plates += e.total();
                if (e.state() == 0) {
                    offline++;
                }
            }
            var summary = Component.translatable("gui.appliedquartermaster.automation.summary", view.entries().size(), plates, offline);
            g.text(font, summary, x + W - 8 - 92 - 6 - font.width(summary), y + PANEL_Y + 7, TEXT_DIM, false);
        }

        int s = SIZE_AUTOMATION;
        int hovered = cellAt(mx, my);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS[s]; c++) {
                int index = (scroll + r) * COLS[s] + c;
                if (index >= cells.size()) {
                    break;
                }
                var e = view.entries().get(cells.get(index).entry());
                int cx = gx + c * CELL_W[s];
                int cy = gy + r * CELL_H[s];
                int ix = cx + (CELL_W[s] - 48) / 2;
                int iy = cy + 2;
                if (index == hovered) {
                    g.fill(cx + 1, cy + 1, cx + CELL_W[s] - 1, cy + CELL_H[s] - 1, HOVER);
                }
                g.pose().pushMatrix();
                g.pose().translate(ix, iy);
                g.pose().scale(3, 3);
                if (inFarm && !e.customIcon()) {
                    // A plate without its own icon shows its light ring in the current state.
                    var face = AppliedQuartermaster.id("textures/part/redstone_plate_face_"
                            + (e.state() == 2 ? "on" : e.state() == 1 ? "off" : "offline") + ".png");
                    g.blit(RenderPipelines.GUI_TEXTURED, face, 0, 0, 0f, 0f, 16, 16, 16, 16);
                } else {
                    g.item(e.icon(), 0, 0);
                }
                g.pose().popMatrix();
                // Status light: green when running, red when off or offline.
                boolean good = inFarm ? e.state() == 2 : e.state() != 0 && e.on() > 0;
                dot(g, ix + 42.5f, iy + 42.5f, 8, good);
                smallLines(g, e.name(), cx + CELL_W[s] / 2, iy + 50, CELL_W[s] - 2, 1, TEXT);
                Component sub;
                int color;
                if (inFarm) {
                    sub = e.state() == 0 ? Component.translatable("gui.appliedquartermaster.automation.offline")
                            : e.state() == 2 ? Component.translatable("gui.appliedquartermaster.automation.on_strength", e.strength())
                            : Component.translatable("gui.appliedquartermaster.automation.off");
                    color = e.state() == 0 ? TEXT_DIM : e.state() == 2 ? GREEN : RED;
                } else if (e.state() == 0) {
                    sub = Component.translatable("gui.appliedquartermaster.automation.offline");
                    color = TEXT_DIM;
                } else if (e.total() > 0 && e.on() == e.total()) {
                    sub = Component.translatable("gui.appliedquartermaster.automation.all_on_count", e.on(), e.total());
                    color = GREEN;
                } else if (e.on() == 0) {
                    sub = Component.translatable("gui.appliedquartermaster.automation.all_off_state");
                    color = RED;
                } else {
                    sub = Component.translatable("gui.appliedquartermaster.automation.some_on", e.on(), e.total());
                    color = GREEN;
                }
                smallLines(g, sub.getString(), cx + CELL_W[s] / 2, iy + 57, CELL_W[s] - 2, 1, color);
            }
        }
    }

    private static final float SMALL = 0.75f;

    /** Centered text at 3/4 size, wrapped to at most {@code maxLines} lines (the last one cut with an ellipsis). */
    private void smallLines(GuiGraphicsExtractor g, String text, int centerX, int y, int width, int maxLines, int color) {
        int scaledWidth = (int) (width / SMALL);
        var lines = new ArrayList<String>();
        var words = text.split(" ");
        var line = new StringBuilder();
        for (var word : words) {
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
        g.fill(x0, y0, x0 + s, y0 + 1, 0xFF9A9FB4);
        g.fill(x0, y0, x0 + 1, y0 + s, 0xFF9A9FB4);
        g.fill(x0 + 1, y0 + s - 1, x0 + s, y0 + s, 0xFFDCDDE6);
        g.fill(x0 + s - 1, y0 + 1, x0 + s, y0 + s, 0xFFDCDDE6);
    }

    /** The card to the right of the inventory: network status and what this page can do. */
    private void smallButton(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean hover) {
        g.fill(x, y, x + w, y + 13, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + 12, hover ? BUTTON_HOVER : BUTTON);
        g.text(font, label, x + (w - font.width(label)) / 2, y + 3, 0xFFF2F2F2, false);
    }

    private static final int LIGHT_GREEN = 0xFF5AE66E;
    private static final int LIGHT_RED = 0xFFEB3C32;

    /** A round status light: green when ok, red otherwise. Drawn at quarter-pixel resolution so it looks round. */
    private static void dot(GuiGraphicsExtractor g, float centerX, float centerY, float diameter, boolean ok) {
        g.pose().pushMatrix();
        g.pose().translate(centerX, centerY);
        g.pose().scale(0.25f, 0.25f);
        int outer = Math.round(diameter * 2);
        circle(g, outer, OUTLINE);
        circle(g, outer - 3, ok ? LIGHT_GREEN : LIGHT_RED);
        // Small highlight so it reads as a light
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

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // All labels are drawn with the page in extractBackground.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        int cell = cellAt(mx, my);
        if (isDevicesPage()) {
            var view = menu.getDevices();
            if (cell >= 0 && view != null) {
                g.setComponentTooltipForNextFrame(font, deviceTooltip(view, view.entries().get(cells.get(cell).entry())), mouseX, mouseY);
                return;
            }
            var tooltip = tooltipAt(mx, my);
            if (tooltip != null) {
                g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
            return;
        }
        if (isAutomationPage()) {
            var view = menu.getAutomation();
            if (cell >= 0 && view != null) {
                var e = view.entries().get(cells.get(cell).entry());
                var lines = new ArrayList<Component>();
                lines.add(Component.literal(e.name()));
                if (view.inFarm()) {
                    lines.add(Component.translatable(e.state() == 0 ? "gui.appliedquartermaster.automation.offline_hint"
                            : e.state() == 2 ? "gui.appliedquartermaster.automation.on_strength" : "gui.appliedquartermaster.automation.off_strength",
                            e.strength()).withColor(TEXT_DIM));
                    lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_toggle").withColor(HINT));
                    lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_strength").withColor(HINT));
                } else {
                    lines.add(Component.translatable(e.state() == 0 ? "gui.appliedquartermaster.automation.offline_hint"
                            : "gui.appliedquartermaster.automation.some_on", e.on(), e.total()).withColor(TEXT_DIM));
                    lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_open").withColor(HINT));
                }
                lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_rename").withColor(HINT));
                lines.add(Component.translatable("gui.appliedquartermaster.automation.hint_icon").withColor(HINT));
                g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
                return;
            }
            var tooltip = tooltipAt(mx, my);
            if (tooltip != null) {
                g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
            return;
        }
        if (cell >= 0 && cells.get(cell).entry() >= 0 && menu.getStatus() == TabletNetwork.OK) {
            var stack = cells.get(cell).stack();
            var lines = new ArrayList<Component>(getTooltipFromContainerItem(stack));
            if (kind() == StorageKind.LIBRARY) {
                lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_read").withColor(HINT));
                lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_pickup_right").withColor(HINT));
            } else {
                lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_pickup").withColor(HINT));
            }
            lines.add(Component.translatable("gui.appliedquartermaster.tablet.hint_take").withColor(HINT));
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        var tooltip = tooltipAt(mx, my);
        if (tooltip != null) {
            g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(int mx, int my) {
        var tabs = tabs();
        int pinned = menu.getDefaultTab();
        for (int t = 0; t < tabs.size(); t++) {
            if (inside(mx, my, tabX(t), 2, TAB_W, TAB_H - 4)) {
                var tab = tabs.get(t);
                var name = tab.module() >= 0 ? menu.getModule(tab.module()).getHoverName() : pageTitle(tab.page());
                return List.of(name,
                        Component.translatable("gui.appliedquartermaster.tablet.tab_open").withColor(HINT),
                        Component.translatable(tab.pinValue() == pinned
                                ? "gui.appliedquartermaster.tablet.unpin"
                                : "gui.appliedquartermaster.tablet.tab_pin").withColor(HINT));
            }
        }
        if (inside(mx, my, gearX(), 0, TAB_W, TAB_H)) {
            return List.of(Component.translatable("gui.appliedquartermaster.tablet.modules"));
        }
        if (isAutomationPage() || isDevicesPage()) {
            if (inside(mx, my, TOOL_X, PANEL_Y + 4, 18, 18)) {
                if (inDeviceList()) {
                    return List.of(Component.translatable("gui.appliedquartermaster.devices.sort."
                            + (sort == SORT_STORAGE ? "position" : sort == SORT_AZ ? "problems_first" : "problems_last")));
                }
                return List.of(Component.translatable("gui.appliedquartermaster.tablet.sort."
                        + (sort == SORT_STORAGE ? "storage" : sort == SORT_AZ ? "az" : "za")));
            }
            if (inDeviceList() && inside(mx, my, BACK_X, PANEL_Y + 4, BACK_W, 13)) {
                return List.of(Component.translatable("gui.appliedquartermaster.devices.back"));
            }
            var view = isAutomationPage() ? menu.getAutomation() : null;
            if (view != null && view.inFarm()) {
                if (inside(mx, my, BACK_X, PANEL_Y + 4, BACK_W, 13)) {
                    return List.of(Component.translatable("gui.appliedquartermaster.automation.back"));
                }
                if (inside(mx, my, BACK_X + BACK_W, PANEL_Y + 4, allOnX() - BACK_X - BACK_W - 4, 13)) {
                    return List.of(Component.literal(view.title()),
                            Component.translatable("gui.appliedquartermaster.automation.hint_rename").withColor(HINT),
                            Component.translatable("gui.appliedquartermaster.automation.hint_icon").withColor(HINT));
                }
            }
        } else if (!isModulesPage()) {
            String[] keys = {
                    "gui.appliedquartermaster.tablet.sort." + (sort == SORT_STORAGE ? "storage" : sort == SORT_AZ ? "az" : "za"),
                    "gui.appliedquartermaster.tablet.free." + (showFree ? "shown" : "hidden"),
                    "gui.appliedquartermaster.tablet.size." + SIZE_KEYS[size()]};
            for (int i = 0; i < keys.length; i++) {
                if (inside(mx, my, TOOL_X, PANEL_Y + 4 + i * 20, 18, 18)) {
                    return List.of(Component.translatable(keys[i]));
                }
            }
        } else {
            var first = menu.slots.get(TabletModules.SLOTS);
            if (inside(mx, my, BATTERY_X, first.y - 10, 12, 38)) {
                return List.of(Component.translatable("gui.appliedquartermaster.tablet.battery_tooltip"));
            }
            if (menu.getCarried().isEmpty() && inside(mx, my, first.x - 1, first.y - 1, 40, 18)
                    && menu.slots.get(TabletModules.SLOTS + (mx - first.x + 1) / 20).getItem().isEmpty()) {
                return List.of(Component.translatable("gui.appliedquartermaster.tablet.upgrades_tooltip"));
            }
            for (int i = 0; i < TabletModules.SLOTS; i++) {
                if (!menu.getModule(i).isEmpty() && onModulePin(mx, my, i)) {
                    return List.of(Component.translatable(i == pinned
                            ? "gui.appliedquartermaster.tablet.unpin"
                            : "gui.appliedquartermaster.tablet.pin"));
                }
            }
        }
        return null;
    }

    private boolean onModulePin(int mx, int my, int module) {
        var slot = menu.slots.get(module);
        int bx = slot.x - (BOX - 16) / 2;
        int by = slot.y - (BOX - 16) / 2;
        return inside(mx, my, bx + BOX - 10, by + 1, 10, 10);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x() - leftPos;
        int my = (int) event.y() - topPos;
        int button = event.button();
        boolean carrying = !menu.getCarried().isEmpty();

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

        if (!carrying) {
            var tabs = tabs();
            for (int t = 0; t < tabs.size(); t++) {
                if (inside(mx, my, tabX(t), 2, TAB_W, TAB_H - 4)) {
                    var tab = tabs.get(t);
                    if (button == 1) {
                        press(TabletMenu.BUTTON_PIN + tab.pinValue());
                    } else if (button == 0) {
                        if (tab.module() >= 0) {
                            press(TabletMenu.BUTTON_OPEN + tab.module());
                        } else {
                            switchPage(tab.page());
                        }
                    }
                    return true;
                }
            }
            if (inside(mx, my, gearX(), 0, TAB_W, TAB_H)) {
                switchPage(TabletMenu.PAGE_MODULES);
                return true;
            }
        }

        if (rename.visible && !rename.isMouseOver(event.x(), event.y())) {
            confirmRename();
        }
        if (isAutomationPage()) {
            if (automationClicked(mx, my, button, event.hasShiftDown(), carrying, event)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (isDevicesPage()) {
            if (devicesClicked(mx, my, button, event)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        var kind = kind();
        if (kind == null) {
            for (int i = 0; i < TabletModules.SLOTS; i++) {
                if (!carrying && !menu.getModule(i).isEmpty() && onModulePin(mx, my, i)) {
                    press(TabletMenu.BUTTON_PIN + i);
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        // Left toolbar.
        for (int i = 0; i < 3; i++) {
            if (inside(mx, my, TOOL_X, PANEL_Y + 4 + i * 20, 18, 18)) {
                if (i == 0) {
                    sort = (sort + (button == 1 ? 2 : 1)) % 3;
                } else if (i == 1) {
                    showFree = !showFree;
                } else {
                    int next = (size() + (button == 1 ? 2 : 1)) % 3;
                    viewSize[kind.ordinal()] = next;
                    send(TabletActionPayload.SET_VIEW, kind.ordinal(), ItemStack.EMPTY, next);
                    scroll = 0;
                    relayout();
                }
                builtVersion = -1;
                playClick();
                return true;
            }
        }

        // Scroll bar.
        if (inside(mx, my, GRID_X + GRID_W + 4, gridY(), SCROLL_W, gridH())) {
            scrollTo(my);
            return true;
        }

        // Grid.
        if (inGrid(mx, my) && menu.getStatus() == TabletNetwork.OK) {
            int index = cellAt(mx, my);
            var cell = index >= 0 ? cells.get(index) : null;
            if (cell == null || cell.entry() < 0) {
                if (carrying) {
                    send(TabletActionPayload.STORE, -1, ItemStack.EMPTY, 0);
                }
                return true;
            }
            if (event.hasShiftDown()) {
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

    private boolean automationClicked(int mx, int my, int button, boolean shift, boolean carrying, MouseButtonEvent event) {
        var view = menu.getAutomation();
        if (rename.visible && rename.isMouseOver(event.x(), event.y())) {
            return false;
        }
        if (inside(mx, my, TOOL_X, PANEL_Y + 4, 18, 18)) {
            sort = (sort + (button == 1 ? 2 : 1)) % 3;
            builtVersion = -1;
            playClick();
            return true;
        }
        if (view != null && view.inFarm()) {
            if (inside(mx, my, BACK_X, PANEL_Y + 4, BACK_W, 13)) {
                send(TabletActionPayload.BACK, -1, ItemStack.EMPTY, 0);
                scroll = 0;
                playClick();
                return true;
            }
            if (inside(mx, my, allOnX(), PANEL_Y + 4, 38, 13) || inside(mx, my, allOnX() + 40, PANEL_Y + 4, 38, 13)) {
                boolean allOn = mx < allOnX() + 40;
                send(allOn ? TabletActionPayload.ALL_ON : TabletActionPayload.ALL_OFF, -1, ItemStack.EMPTY, 0);
                playClick();
                return true;
            }
            if (inside(mx, my, BACK_X + BACK_W, PANEL_Y + 4, allOnX() - BACK_X - BACK_W - 4, 13)) {
                if (carrying || (shift && button == 1)) {
                    send(TabletActionPayload.SET_ICON, -1, ItemStack.EMPTY, 0);
                } else if (button == 1) {
                    startRename(-1, view.title());
                }
                return true;
            }
        }
        if (inside(mx, my, GRID_X + GRID_W + 4, gridY(), SCROLL_W, gridH())) {
            scrollTo(my);
            return true;
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
        if (carrying || (shift && button == 1)) {
            // Holding an item: use it as the icon (the item is not used up). Sneak-right-click clears the icon.
            send(TabletActionPayload.SET_ICON, entry, ItemStack.EMPTY, 0);
        } else if (button == 1) {
            startRename(entry, e.name());
        } else if (view.inFarm()) {
            send(TabletActionPayload.TOGGLE, entry, ItemStack.EMPTY, 0);
        } else {
            send(TabletActionPayload.OPEN_FARM, entry, ItemStack.EMPTY, 0);
            scroll = 0;
        }
        playClick();
        return true;
    }

    /** A farm or plate (entry, or -1 for the open farm's title) that can take an icon dragged from JEI. */
    public record IconTarget(int entry, net.minecraft.client.renderer.Rect2i area) {
    }

    /** Screen areas outside the panel (the left toolbar), for JEI. */
    public List<net.minecraft.client.renderer.Rect2i> getExtraAreas() {
        if (isModulesPage()) {
            return List.of();
        }
        return List.of(new net.minecraft.client.renderer.Rect2i(leftPos + TOOL_X - 3, topPos + PANEL_Y, -TOOL_X + 3,
                toolCount() * 20 + 8));
    }

    public List<IconTarget> getIconTargets() {
        var view = menu.getAutomation();
        var list = new ArrayList<IconTarget>();
        if (!isAutomationPage() || view == null) {
            return list;
        }
        int s = SIZE_AUTOMATION;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS[s]; c++) {
                int index = (scroll + r) * COLS[s] + c;
                if (index >= cells.size()) {
                    break;
                }
                list.add(new IconTarget(cells.get(index).entry(), new net.minecraft.client.renderer.Rect2i(
                        leftPos + GRID_X + c * CELL_W[s], topPos + gridY() + r * CELL_H[s], CELL_W[s], CELL_H[s])));
            }
        }
        if (view.inFarm()) {
            list.add(new IconTarget(-1, new net.minecraft.client.renderer.Rect2i(leftPos + BACK_X + BACK_W, topPos + PANEL_Y + 4,
                    allOnX() - BACK_X - BACK_W - 4, 13)));
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
        var view = menu.getAutomation();
        if (entry == -1 || view == null) {
            rename.setX(leftPos + BACK_X + BACK_W + 4);
            rename.setY(topPos + PANEL_Y + 4);
            rename.setWidth(allOnX() - BACK_X - BACK_W - 8);
        } else {
            rename.setX(leftPos + 8);
            rename.setY(topPos + PANEL_Y + 4);
            rename.setWidth(W - 16 - 92 - 6);
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
        if (!isModulesPage() && inside(mx, my, GRID_X + GRID_W + 2, gridY() - 4, SCROLL_W + 4, gridH() + 8)) {
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
        var view = menu.getAutomation();
        if (isAutomationPage() && view != null && view.inFarm() && cellAt(mx, my) >= 0) {
            // Scroll over a plate: change its signal strength.
            int entry = cells.get(cellAt(mx, my)).entry();
            send(TabletActionPayload.STRENGTH, entry, ItemStack.EMPTY, scrollY > 0 ? 1 : -1);
            return true;
        }
        if (!isModulesPage() && inGrid(mx, my)) {
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
        boolean inPanel = rx >= 0 && ry >= 0 && rx < W && ry < PANEL_Y + panelH;
        boolean inToolbar = !isModulesPage() && rx >= TOOL_X - 3 && rx < 0 && ry >= PANEL_Y && ry < PANEL_Y + toolCount() * 20 + 8;
        return !inPanel && !inToolbar;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // The tablet's view sizes may arrive after the screen opened.
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

    // ---------------------------------------------------------------- helpers

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private Component shortName(ItemStack stack, int width) {
        var name = stack.getHoverName().getString();
        if (font.width(name) <= width) {
            return Component.literal(name);
        }
        return Component.literal(font.plainSubstrByWidth(name, width - font.width("…")) + "…");
    }

    private void centered(GuiGraphicsExtractor g, Component text, int cx, int y, int color) {
        g.text(font, text, cx - font.width(text) / 2, y, color, false);
    }

    private void openTab(GuiGraphicsExtractor g, int x, int y) {
        bevel(g, x, y, TAB_W, TAB_H + 1, FACE);
        g.fill(x + 1, y + TAB_H - 2, x + TAB_W - 1, y + TAB_H + 1, FACE);
    }

    /** Raised panel in AE2's terminal colours: slate outline, light top-left edge, blue-grey shadow. */
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, LIGHT);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, SHADOW);
    }

    /** Raised box (tabs, buttons). */
    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, LIGHT);
    }

    /** Sunken box (slots, free spots, the grid). */
    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, face);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    /** Gold pin marking the default tab (grey when not pinned). */
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
        pixels(g, GEAR, x, y, color);
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
