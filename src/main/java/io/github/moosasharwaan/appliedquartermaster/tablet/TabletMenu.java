package io.github.moosasharwaan.appliedquartermaster.tablet;

import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import io.github.moosasharwaan.appliedquartermaster.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The tablet screen's menu: the tab bar plus the Modules page (module slots and the player inventory).
 * Buttons: {@code 0..3} open a module's tab, {@code 100..103} pin or unpin a module as the default tab.
 */
public class TabletMenu extends AbstractContainerMenu {

    public static final int BUTTON_OPEN = 0;
    public static final int BUTTON_PIN = 100;

    /** Layout shared with the screen. */
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

    public TabletMenu(int id, Inventory inventory, int tabletSlot) {
        super(ModMenus.TABLET.get(), id);
        this.playerInventory = inventory;
        this.tabletSlot = tabletSlot;

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

    public int getDefaultModule() {
        return TabletModules.getDefault(getTablet());
    }

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
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (id >= BUTTON_PIN && id < BUTTON_PIN + TabletModules.SLOTS) {
            TabletModules.togglePin(getTablet(), id - BUTTON_PIN);
            return true;
        }
        if (id >= BUTTON_OPEN && id < BUTTON_OPEN + TabletModules.SLOTS) {
            onModulesChanged(modules);
            return TabletItem.openModule(serverPlayer, tabletSlot, id - BUTTON_OPEN);
        }
        return false;
    }

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
        var original = stack.copy();
        int moduleEnd = TabletModules.SLOTS;
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

    private static class ModuleSlot extends Slot {
        ModuleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return TabletModules.isModule(stack);
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
