package io.github.moosasharwaan.appliedquartermaster.tablet;

import io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload;
import io.github.moosasharwaan.appliedquartermaster.network.TabletViewPayload;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import io.github.moosasharwaan.appliedquartermaster.registry.ModMenus;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The tablet screen's menu. Pages: the Modules page ({@link #PAGE_MODULES}) and one page per storage kind
 * (Library, Armory, Tools), whose items live in blocks on the linked AE2 network and are sent to the client
 * with {@link TabletViewPayload}.
 * <p>
 * Buttons: {@code 0..3} open a module, {@code 100 + pin} pins a tab, {@code 200 + page + 1} switches page.
 */
public class TabletMenu extends AbstractContainerMenu {

    public static final int PAGE_MODULES = -1;

    public static final int BUTTON_OPEN = 0;
    public static final int BUTTON_PIN = 100;
    public static final int BUTTON_PAGE = 200;

    /** Default slot layout (the screen moves slots to fit the page). */
    public static final int MODULE_Y = 50;
    public static final int MODULE_X0 = 20;
    public static final int MODULE_STEP = 40;
    public static final int INVENTORY_Y = 104;

    private final Inventory playerInventory;
    private final int tabletSlot;
    private final SimpleContainer modules = new SimpleContainer(TabletModules.SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            onModulesChanged(this);
        }
    };
    private boolean loading;
    private int page;

    // Server: what was last sent, and where each sent item lives.
    private record Ref(StorageBlockEntity be, int slot) {
    }

    private final List<Ref> refs = new ArrayList<>();
    private TabletViewPayload lastSent;
    private int ticks;

    // Client: the latest view from the server.
    private int unlockMask;
    private int status = TabletNetwork.OK;
    private List<ItemStack> viewItems = List.of();
    private int viewFree;
    private int viewVersion;

    public TabletMenu(int id, Inventory inventory, int tabletSlot, int page) {
        super(ModMenus.TABLET.get(), id);
        this.playerInventory = inventory;
        this.tabletSlot = tabletSlot;
        this.page = page;

        loading = true;
        var stored = TabletModules.read(getTablet());
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            modules.setItem(i, stored.get(i));
        }
        loading = false;

        for (int i = 0; i < TabletModules.SLOTS; i++) {
            addSlot(new ModuleSlot(modules, i, moduleSlotX(i), MODULE_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerSlot(inventory, col + (row + 1) * 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new PlayerSlot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    /** X of the 16px item area of a module slot. */
    public static int moduleSlotX(int i) {
        return MODULE_X0 + i * MODULE_STEP;
    }

    public ItemStack getTablet() {
        return playerInventory.getItem(tabletSlot);
    }

    public int getTabletSlot() {
        return tabletSlot;
    }

    public ItemStack getModule(int i) {
        return modules.getItem(i);
    }

    public int getDefaultTab() {
        return TabletModules.getDefault(getTablet());
    }

    public int getPage() {
        return page;
    }

    public StorageKind getPageKind() {
        return StorageKind.byIndex(page);
    }

    /** Client: switch page right away (the server is told with a button click). */
    public void setPageClient(int page) {
        this.page = page;
        this.viewItems = List.of();
        this.viewFree = 0;
        this.viewVersion++;
    }

    public boolean isUnlocked(StorageKind kind) {
        return (unlockMask & (1 << kind.ordinal())) != 0;
    }

    public int getStatus() {
        return status;
    }

    public List<ItemStack> getViewItems() {
        return viewItems;
    }

    public int getViewFree() {
        return viewFree;
    }

    /** Increases whenever new view data arrives, so the screen knows to rebuild its grid. */
    public int getViewVersion() {
        return viewVersion;
    }

    public void receiveView(TabletViewPayload payload) {
        this.unlockMask = payload.unlockMask();
        this.status = payload.status();
        if (payload.page() == page) {
            this.viewItems = payload.items();
            this.viewFree = payload.free();
        }
        this.viewVersion++;
    }

    // ------------------------------------------------------------------ modules

    private void onModulesChanged(Container container) {
        if (loading || playerInventory.player.level().isClientSide()) {
            return;
        }
        var tablet = getTablet();
        if (!tablet.is(ModItems.ME_TABLET.get())) {
            return;
        }
        var list = TabletModules.read(tablet);
        for (int i = 0; i < TabletModules.SLOTS; i++) {
            list.set(i, modules.getItem(i).copy());
        }
        TabletModules.write(tablet, list);
        lastSent = null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (id >= BUTTON_PAGE - 1 && id <= BUTTON_PAGE + StorageKind.values().length) {
            int newPage = id - BUTTON_PAGE - 1;
            if (newPage >= PAGE_MODULES && newPage < StorageKind.values().length) {
                page = newPage;
                lastSent = null;
                return true;
            }
            return false;
        }
        if (id >= BUTTON_PIN && id < BUTTON_PAGE - 1) {
            TabletModules.togglePin(getTablet(), id - BUTTON_PIN);
            return true;
        }
        if (id >= BUTTON_OPEN && id < BUTTON_OPEN + TabletModules.SLOTS) {
            onModulesChanged(modules);
            return TabletItem.openModule(serverPlayer, tabletSlot, id - BUTTON_OPEN);
        }
        return false;
    }

    // ------------------------------------------------------------------ server view sync

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (playerInventory.player instanceof ServerPlayer player && (lastSent == null || ++ticks % 5 == 0)) {
            sendView(player);
        }
    }

    private void sendView(ServerPlayer player) {
        var network = TabletNetwork.find(player, getTablet());
        int mask = 0;
        refs.clear();
        var items = new ArrayList<ItemStack>();
        int free = 0;
        if (network.grid() != null) {
            for (var kind : StorageKind.values()) {
                if (TabletNetwork.present(network.grid(), kind)) {
                    mask |= 1 << kind.ordinal();
                }
            }
            var kind = getPageKind();
            if (kind != null) {
                for (var be : TabletNetwork.blocks(network.grid(), kind)) {
                    for (int slot = 0; slot < StorageKind.SLOTS; slot++) {
                        var stack = be.getItem(slot);
                        if (stack.isEmpty()) {
                            free++;
                        } else {
                            refs.add(new Ref(be, slot));
                            items.add(stack.copy());
                        }
                    }
                }
            }
        }
        var payload = new TabletViewPayload(containerId, page, mask, network.status(), items, free);
        if (!same(payload, lastSent)) {
            lastSent = payload;
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static boolean same(TabletViewPayload a, TabletViewPayload b) {
        if (b == null || a.page() != b.page() || a.unlockMask() != b.unlockMask() || a.status() != b.status()
                || a.free() != b.free() || a.items().size() != b.items().size()) {
            return false;
        }
        for (int i = 0; i < a.items().size(); i++) {
            if (!ItemStack.matches(a.items().get(i), b.items().get(i))) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ server actions

    public void handleAction(ServerPlayer player, TabletActionPayload action) {
        var kind = getPageKind();
        if (action.action() == TabletActionPayload.SET_VIEW) {
            var target = StorageKind.byIndex(action.entry());
            if (target != null) {
                TabletModules.setViewSize(getTablet(), target, action.arg());
            }
            return;
        }
        if (kind == null) {
            return;
        }
        switch (action.action()) {
            case TabletActionPayload.STORE -> {
                var carried = getCarried();
                if (kind.accepts(carried)) {
                    setCarried(storeIntoNetwork(player, kind, carried));
                }
            }
            case TabletActionPayload.PICKUP, TabletActionPayload.TAKE, TabletActionPayload.READ -> {
                var ref = findRef(action.entry(), action.expected());
                if (ref == null) {
                    break;
                }
                var stored = ref.be().getItem(ref.slot());
                if (action.action() == TabletActionPayload.TAKE) {
                    var copy = stored.copy();
                    if (player.getInventory().add(copy) && copy.isEmpty()) {
                        ref.be().setItem(ref.slot(), ItemStack.EMPTY);
                    } else if (copy.getCount() != stored.getCount()) {
                        ref.be().setItem(ref.slot(), copy);
                    }
                } else if (action.action() == TabletActionPayload.PICKUP) {
                    var carried = getCarried();
                    if (carried.isEmpty()) {
                        setCarried(stored);
                        ref.be().setItem(ref.slot(), ItemStack.EMPTY);
                    } else if (carried.getCount() == 1 && kind.accepts(carried)) {
                        ref.be().setItem(ref.slot(), carried);
                        setCarried(stored);
                    }
                } else {
                    readBook(player, ref);
                }
            }
            default -> {
            }
        }
        lastSent = null;
        broadcastChanges();
    }

    private Ref findRef(int entry, ItemStack expected) {
        if (entry < 0 || entry >= refs.size()) {
            return null;
        }
        var ref = refs.get(entry);
        if (ref.be().isRemoved() || !ref.be().isActive()) {
            return null;
        }
        var stored = ref.be().getItem(ref.slot());
        return ItemStack.matches(stored, expected) ? ref : null;
    }

    /** Puts items into free spots of active blocks of this kind, one per spot. Returns what didn't fit. */
    private ItemStack storeIntoNetwork(ServerPlayer player, StorageKind kind, ItemStack stack) {
        var network = TabletNetwork.find(player, getTablet());
        if (network.grid() == null || stack.isEmpty() || !kind.accepts(stack)) {
            return stack;
        }
        var remaining = stack.copy();
        for (var be : TabletNetwork.blocks(network.grid(), kind)) {
            int slot;
            while (!remaining.isEmpty() && (slot = be.firstFreeSlot()) >= 0) {
                be.setItem(slot, remaining.split(1));
            }
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    /**
     * Opens a guide book that has no client-side reader by using it, as if right-clicked, from the main hand.
     * The tablet goes back into the hand straight after.
     */
    private void readBook(ServerPlayer player, Ref ref) {
        var book = ref.be().getItem(ref.slot());
        if (book.isEmpty() || book.has(DataComponents.WRITTEN_BOOK_CONTENT) || book.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            return;
        }
        var inventory = player.getInventory();
        int hand = inventory.getSelectedSlot();
        var previous = inventory.getItem(hand);
        inventory.setItem(hand, book.copy());
        try {
            book.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        } finally {
            var after = inventory.getItem(hand);
            inventory.setItem(hand, previous);
            if (!after.isEmpty() && after.is(book.getItem())) {
                ref.be().setItem(ref.slot(), after);
            }
        }
    }

    // ------------------------------------------------------------------ slots

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        // Number keys must not swap the tablet out of its slot while it is open.
        if (input == ContainerInput.SWAP && button == tabletSlot) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        int moduleEnd = TabletModules.SLOTS;
        var kind = getPageKind();
        if (index >= moduleEnd && kind != null) {
            // Storage page: shift-click from the inventory stores the item on the network.
            if (player instanceof ServerPlayer serverPlayer && kind.accepts(stack)) {
                var rest = storeIntoNetwork(serverPlayer, kind, stack);
                slot.setByPlayer(rest);
                lastSent = null;
            }
            return ItemStack.EMPTY;
        }
        var original = stack.copy();
        if (index < moduleEnd) {
            if (!moveItemStackTo(stack, moduleEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (TabletModules.isModule(stack)) {
            if (!moveItemStackTo(stack, 0, moduleEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return getTablet().is(ModItems.ME_TABLET.get());
    }

    private class ModuleSlot extends Slot {
        ModuleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return page == PAGE_MODULES && TabletModules.isModule(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return page == PAGE_MODULES;
        }

        @Override
        public boolean isActive() {
            return page == PAGE_MODULES;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private class PlayerSlot extends Slot {
        PlayerSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return getContainerSlot() != tabletSlot && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() != tabletSlot && super.mayPlace(stack);
        }
    }
}
