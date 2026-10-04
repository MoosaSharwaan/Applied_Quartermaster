package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.registry.ModComponents;
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
        if (pinned >= 0 && modules.get(pinned).isEmpty()) {
            tablet.remove(ModComponents.TABLET_DEFAULT_MODULE);
        }
    }

    public static void set(ItemStack tablet, int slot, ItemStack module) {
        var modules = read(tablet);
        modules.set(slot, module);
        write(tablet, modules);
    }

    /** @return the pinned module slot, or -1 if none. */
    public static int getDefault(ItemStack tablet) {
        Integer value = tablet.get(ModComponents.TABLET_DEFAULT_MODULE);
        return value == null || value < 0 || value >= SLOTS ? -1 : value;
    }

    /** Pins the module slot, or unpins it if it was already pinned. */
    public static void togglePin(ItemStack tablet, int slot) {
        if (getDefault(tablet) == slot || get(tablet, slot).isEmpty()) {
            tablet.remove(ModComponents.TABLET_DEFAULT_MODULE);
        } else {
            tablet.set(ModComponents.TABLET_DEFAULT_MODULE, slot);
        }
    }
}
