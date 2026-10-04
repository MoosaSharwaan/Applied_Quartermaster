package io.github.moosasharwaan.appliedquartermaster.client;

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
    private static final int TAB_H = 20;
    private static final int PANEL_Y = TAB_H - 2;
    private static final int HEADER_H = 20;
    private static final int GRID_X = 8;
    private static final int GRID_W = 17 * 18;
    private static final int SCROLL_W = 12;
    private static final int INV_SECTION_H = 12 + 3 * 18 + 4 + 18 + 8;
    private static final int BOX = 36;
    private static final int MODULES_CONTENT_H = 76;
    private static final int TOOL_X = -22;

    /** Large, Medium, Small: columns, cell size, item scale, rows at most. */
    private static final int[] COLS = {5, 8, 17};
    private static final int[] CELL_W = {61, 38, 18};
    private static final int[] CELL_H = {62, 38, 18};
    private static final int[] SCALE = {3, 2, 1};
    private static final int[] MAX_ROWS = {3, 4, 10};
    private static final String[] SIZE_KEYS = {"large", "medium", "small"};

    private static final int SORT_STORAGE = 0;
    private static final int SORT_AZ = 1;
    private static final int SORT_ZA = 2;

    // ---------------------------------------------------------------- palette (AE2 / vanilla GUI)
    private static final int OUTLINE = 0xFF000000;
    private static final int FACE = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int TAB_IDLE = 0xFFA8A8A8;
    private static final int GRID_BG = 0xFFB4B4BC;
    private static final int HOVER = 0x60FFFFFF;
    private static final int GOLD = 0xFFE8B53A;
    private static final int GOLD_DARK = 0xFF8A5E12;
    private static final int TEXT = 0xFF404040;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int HINT = 0xFF7FD7FF;

    private record Tab(int module, StorageKind kind) {
        int pinValue() {
            return kind == null ? module : TabletModules.PIN_STORAGE + kind.ordinal();
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

    public TabletScreen(TabletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, 200);
        var tablet = menu.getTablet();
        for (var kind : StorageKind.values()) {
            viewSize[kind.ordinal()] = TabletModules.getViewSize(tablet, kind);
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
        relayout();
    }

    private StorageKind kind() {
        return menu.getPageKind();
    }

    private int size() {
        var kind = kind();
        return kind == null ? 0 : viewSize[kind.ordinal()];
    }

    private int gridY() {
        return PANEL_Y + HEADER_H;
    }

    private int gridH() {
        return rows * CELL_H[size()];
    }

    private int inventoryX() {
        return (W - 162) / 2;
    }

    private int inventoryY() {
        return panelH + PANEL_Y - INV_SECTION_H + 12;
    }

    /** Sizes the panel for the open page and screen height, then moves the slots to match. */
    private void relayout() {
        var kind = kind();
        int contentH;
        if (kind == null) {
            rows = 0;
            contentH = HEADER_H + MODULES_CONTENT_H;
        } else {
            int s = size();
            int available = height - 8 - PANEL_Y - HEADER_H - INV_SECTION_H - 6;
            rows = Math.max(1, Math.min(MAX_ROWS[s], available / CELL_H[s]));
            contentH = HEADER_H + rows * CELL_H[s] + 4;
        }
        panelH = contentH + INV_SECTION_H;
        int totalH = PANEL_Y + panelH;
        leftPos = (width - W) / 2;
        topPos = Math.max(4, (height - totalH) / 2);

        int moduleX0 = (W - (TabletModules.SLOTS * 40 - 4)) / 2 + (BOX - 16) / 2;
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var slot = menu.slots.get(i);
            slot.x = moduleX0 + i * 40;
            slot.y = PANEL_Y + HEADER_H + (BOX - 16) / 2 + 4;
        }
        int ix = inventoryX();
        int iy = inventoryY();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                var slot = menu.slots.get(TabletModules.SLOTS + row * 9 + col);
                slot.x = ix + col * 18;
                slot.y = iy + row * 18;
            }
        }
        for (int col = 0; col < 9; col++) {
            var slot = menu.slots.get(TabletModules.SLOTS + 27 + col);
            slot.x = ix + col * 18;
            slot.y = iy + 58;
        }
        if (search != null) {
            search.visible = kind != null;
            search.setX(leftPos + W - 8 - 92 + 2);
            search.setY(topPos + PANEL_Y + 6);
            if (kind == null) {
                search.setFocused(false);
            }
        }
        builtVersion = -1;
        clampScroll();
    }

    // ---------------------------------------------------------------- tabs

    private List<Tab> tabs() {
        var list = new ArrayList<Tab>();
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            if (!menu.getModule(i).isEmpty()) {
                list.add(new Tab(i, null));
            }
        }
        for (var kind : StorageKind.values()) {
            if (menu.isUnlocked(kind) || menu.getPage() == kind.ordinal()) {
                list.add(new Tab(-1, kind));
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

    private static ItemStack tabIcon(StorageKind kind) {
        ItemLike item = switch (kind) {
            case LIBRARY -> ModItems.ME_LIBRARY.get();
            case ARMORY -> ModItems.ME_ARMORY.get();
            case TOOLS -> ModItems.ME_TOOL_RACK.get();
        };
        return new ItemStack(item);
    }

    private ItemStack tabStack(Tab tab) {
        return tab.kind() == null ? menu.getModule(tab.module()) : tabIcon(tab.kind());
    }

    private boolean isOpen(Tab tab) {
        return tab.kind() != null && menu.getPage() == tab.kind().ordinal();
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
        if (kind() == null) {
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
        return kind() != null && inside(mx, my, GRID_X, gridY(), GRID_W, gridH());
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
                bevel(g, x + tabX(t), y + 2, TAB_W, TAB_H, TAB_IDLE);
            }
        }
        if (kind() != null) {
            bevel(g, x + gearX(), y + 2, TAB_W, TAB_H, TAB_IDLE);
        }

        panel(g, x, y + PANEL_Y, W, panelH);

        // Open tab joined to the panel.
        for (int t = 0; t < tabs.size(); t++) {
            if (isOpen(tabs.get(t))) {
                openTab(g, x + tabX(t), y);
            }
        }
        if (kind() == null) {
            openTab(g, x + gearX(), y);
        }
        gear(g, x + gearX() + 9, y + (kind() == null ? 6 : 8), 0xFF505050);

        for (int t = 0; t < tabs.size(); t++) {
            var tab = tabs.get(t);
            int ty = isOpen(tab) ? y + 3 : y + 5;
            g.item(tabStack(tab), x + tabX(t) + 5, ty);
            if (tab.pinValue() == pinned) {
                pin(g, x + tabX(t) + TAB_W - 8, y + 3, true);
            }
        }

        // Left toolbar (storage pages).
        if (kind() != null) {
            String[] glyphs = {sort == SORT_STORAGE ? "#" : sort == SORT_AZ ? "A" : "Z", showFree ? "□" : "■",
                    String.valueOf(size() == 0 ? 'L' : size() == 1 ? 'M' : 'S')};
            for (int i = 0; i < glyphs.length; i++) {
                int bx = x + TOOL_X;
                int by = y + PANEL_Y + 4 + i * 20;
                bevel(g, bx, by, 18, 18, inside(mx, my, TOOL_X, PANEL_Y + 4 + i * 20, 18, 18) ? 0xFFD8D8E0 : FACE);
                g.text(font, glyphs[i], bx + 9 - font.width(glyphs[i]) / 2, by + 5, TEXT, false);
            }
        }

        if (kind() == null) {
            drawModulesPage(g, x, y, pinned);
        } else {
            drawStoragePage(g, x, y, mx, my);
        }

        // Inventory.
        g.text(font, playerInventoryTitle, x + inventoryX(), y + inventoryY() - 11, TEXT, false);
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
        g.text(font, Component.translatable("gui.appliedquartermaster.tablet.modules"), x + 8, y + PANEL_Y + 6, TEXT, false);
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            var slot = menu.slots.get(i);
            int bx = x + slot.x - (BOX - 16) / 2;
            int by = y + slot.y - (BOX - 16) / 2;
            inset(g, bx, by, BOX, BOX, i == pinned ? 0xFFD9C79A : SLOT);
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
                centered(g, shortName(module, BOX + 2), cx, by + BOX + 3, i == pinned ? GOLD_DARK : TEXT);
            }
        }
    }

    private void drawStoragePage(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        var kind = kind();
        int s = size();
        g.text(font, kind.title(), x + 8, y + PANEL_Y + 7, TEXT, false);
        // Search box well.
        int sx = x + W - 8 - 92;
        int sy = y + PANEL_Y + 4;
        inset(g, sx, sy, 92, 13, 0xFF2A2A36);

        int gx = x + GRID_X;
        int gy = y + gridY();
        inset(g, gx - 1, gy - 1, GRID_W + 2, gridH() + 2, GRID_BG);

        // Scroll bar.
        int barX = gx + GRID_W + 4;
        inset(g, barX, gy - 1, SCROLL_W, gridH() + 2, 0xFF9A9AA4);
        int total = totalRows();
        int knobH = Math.max(12, total <= rows ? gridH() : gridH() * rows / total);
        int knobY = total <= rows ? 0 : (gridH() - knobH) * scroll / (total - rows);
        bevel(g, barX + 1, gy + knobY, SCROLL_W - 2, knobH, total <= rows ? 0xFFB0B0B0 : FACE);

        int status = menu.getStatus();
        String message = null;
        if (status == TabletNetwork.NO_TERMINAL) {
            message = "gui.appliedquartermaster.tablet.no_terminal";
        } else if (status == TabletNetwork.NOT_LINKED) {
            message = "gui.appliedquartermaster.tablet.not_linked";
        } else if (status == TabletNetwork.OUT_OF_RANGE) {
            message = "gui.appliedquartermaster.tablet.out_of_range";
        } else if (!menu.isUnlocked(kind)) {
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
                int iy = cy + (s == 0 ? 3 : (CELL_H[s] - iconSize) / 2);
                if (index == hovered) {
                    g.fill(cx, cy, cx + CELL_W[s], cy + CELL_H[s], HOVER);
                }
                if (cell.entry() < 0) {
                    // Only real free spots get a box.
                    inset(g, ix - 1, iy - 1, iconSize + 2, iconSize + 2, SLOT);
                    continue;
                }
                g.pose().pushMatrix();
                g.pose().translate(ix, iy);
                g.pose().scale(SCALE[s], SCALE[s]);
                g.item(cell.stack(), 0, 0);
                g.itemDecorations(font, cell.stack(), 0, 0);
                g.pose().popMatrix();
                if (s == 0) {
                    var name = shortName(cell.stack(), CELL_W[s] - 3);
                    g.text(font, name, cx + (CELL_W[s] - font.width(name)) / 2, iy + iconSize + 3, TEXT, false);
                }
            }
        }
        String count = menu.getViewItems().size() + " / " + (menu.getViewItems().size() + menu.getViewFree());
        g.text(font, count, x + W - 8 - 92 - 6 - font.width(count), y + PANEL_Y + 7, TEXT_DIM, false);
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
                var name = tab.kind() == null ? menu.getModule(tab.module()).getHoverName() : tab.kind().title();
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
        if (kind() != null) {
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
                        if (tab.kind() == null) {
                            press(TabletMenu.BUTTON_OPEN + tab.module());
                        } else {
                            switchPage(tab.kind().ordinal());
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

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int mx = (int) event.x() - leftPos;
        int my = (int) event.y() - topPos;
        if (kind() != null && inside(mx, my, GRID_X + GRID_W + 2, gridY() - 4, SCROLL_W + 4, gridH() + 8)) {
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
        if (kind() != null && inGrid((int) x - leftPos, (int) y - topPos)) {
            scroll -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
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
        boolean inToolbar = kind() != null && rx >= TOOL_X && rx < 0 && ry >= PANEL_Y && ry < PANEL_Y + 64;
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
        ClientPacketDistributor.sendToServer(new TabletActionPayload(menu.containerId, action, entry, expected.copy(), arg));
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
        for (int row = 0; row < GEAR.length; row++) {
            for (int col = 0; col < GEAR[row].length(); col++) {
                if (GEAR[row].charAt(col) == '#') {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }
}
