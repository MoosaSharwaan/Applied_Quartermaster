package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.registry.ModComponents;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Reads and writes the modules stored on a tablet stack. */
public final class TabletModules {

    /** Number of module slots on the Modules page. */
    public static final int SLOTS = 4;

    private TabletModules() {
    }

    /** Only terminals go in module slots: AE2 Wireless, Wireless Crafting and universal terminals. */
    public static boolean isModule(ItemStack stack) {
        return stack.getItem() instanceof WirelessTerminalItem;
    }

    public static NonNullList<ItemStack> read(ItemStack tablet) {
        var list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        tablet.getOrDefault(ModComponents.TABLET_MODULES, ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }

    public static ItemStack get(ItemStack tablet, int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return ItemStack.EMPTY;
        }
        return read(tablet).get(slot);
    }

    public static void write(ItemStack tablet, NonNullList<ItemStack> modules) {
        tablet.set(ModComponents.TABLET_MODULES, ItemContainerContents.fromItems(modules));
        var pinned = getDefault(tablet);
        if (pinned >= 0 && pinned < SLOTS && modules.get(pinned).isEmpty()) {
            tablet.remove(ModComponents.TABLET_DEFAULT_MODULE);
        }
    }

    public static void set(ItemStack tablet, int slot, ItemStack module) {
        var modules = read(tablet);
        modules.set(slot, module);
        write(tablet, modules);
    }

    /** Pin value for a storage tab (Library, Armory, Tools); module slots use 0..3. */
    public static final int PIN_STORAGE = 10;

    /**
     * @return the pinned tab: 0..3 for a module slot, {@link #PIN_STORAGE} + kind for a storage tab, or -1 if none.
     */
    public static int getDefault(ItemStack tablet) {
        Integer value = tablet.get(ModComponents.TABLET_DEFAULT_MODULE);
        if (value == null) {
            return -1;
        }
        if (value >= 0 && value < SLOTS) {
            return value;
        }
        if (value >= PIN_STORAGE && value < PIN_STORAGE + StorageKind.values().length) {
            return value;
        }
        return -1;
    }

    /** Pins the tab, or unpins it if it was already pinned. */
    public static void togglePin(ItemStack tablet, int pin) {
        boolean validModule = pin >= 0 && pin < SLOTS && !get(tablet, pin).isEmpty();
        boolean validStorage = pin >= PIN_STORAGE && pin < PIN_STORAGE + StorageKind.values().length;
        if (getDefault(tablet) == pin || (!validModule && !validStorage)) {
            tablet.remove(ModComponents.TABLET_DEFAULT_MODULE);
        } else {
            tablet.set(ModComponents.TABLET_DEFAULT_MODULE, pin);
        }
    }

    /** View size per storage tab: 0 = Large, 1 = Medium, 2 = Small. */
    public static int getViewSize(ItemStack tablet, StorageKind kind) {
        int packed = tablet.getOrDefault(ModComponents.TABLET_VIEW_SIZES, 0);
        return Math.min(2, (packed >> (kind.ordinal() * 2)) & 3);
    }

    public static void setViewSize(ItemStack tablet, StorageKind kind, int size) {
        int packed = tablet.getOrDefault(ModComponents.TABLET_VIEW_SIZES, 0);
        int shift = kind.ordinal() * 2;
        packed = (packed & ~(3 << shift)) | ((Math.max(0, Math.min(2, size)) & 3) << shift);
        tablet.set(ModComponents.TABLET_VIEW_SIZES, packed);
    }
}
