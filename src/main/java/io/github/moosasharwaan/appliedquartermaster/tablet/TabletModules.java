package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.registry.ModComponents;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Reads and writes the modules stored on a tablet stack. */
public final class TabletModules {

    /** Number of module slots on the Modules page. */
    public static final int SLOTS = 4;

    private TabletModules() {
    }

    /** Number of upgrade slots (range upgrades). */
    public static final int UPGRADE_SLOTS = 2;

    /** Each one adds the Wireless Access Point's range again (AE2's Wireless Booster). */
    public static final TagKey<Item> RANGE_BOOSTERS = tag("tablet_range_boosters");
    /** No range limit inside the access point's dimension (e.g. AEInfinityBooster's Infinity Card). */
    public static final TagKey<Item> INFINITE_RANGE = tag("tablet_infinite_range");
    /** No range limit and works from any dimension (e.g. AE2WTLib's Infinity Booster Card, AEInfinityBooster's Dimension Card). */
    public static final TagKey<Item> ANY_DIMENSION = tag("tablet_any_dimension");

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, AppliedQuartermaster.id(name));
    }

    /** Range upgrades from AE2 and any addon listed in the tablet's upgrade tags. */
    public static boolean isUpgrade(ItemStack stack) {
        return stack.is(RANGE_BOOSTERS) || stack.is(INFINITE_RANGE) || stack.is(ANY_DIMENSION);
    }

    public static NonNullList<ItemStack> readUpgrades(ItemStack tablet) {
        var list = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
        tablet.getOrDefault(ModComponents.TABLET_UPGRADES, ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }

    public static void writeUpgrades(ItemStack tablet, NonNullList<ItemStack> upgrades) {
        tablet.set(ModComponents.TABLET_UPGRADES, ItemContainerContents.fromItems(upgrades));
    }

    /** Number of range boosters installed. */
    public static int boosters(ItemStack tablet) {
        int n = 0;
        for (var stack : readUpgrades(tablet)) {
            if (stack.is(RANGE_BOOSTERS)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** True with an unlimited-range upgrade (same dimension, or any dimension). */
    public static boolean hasInfiniteRange(ItemStack tablet) {
        for (var stack : readUpgrades(tablet)) {
            if (stack.is(INFINITE_RANGE) || stack.is(ANY_DIMENSION)) {
                return true;
            }
        }
        return false;
    }

    /** True with an upgrade that also reaches access points in other dimensions. */
    public static boolean worksAcrossDimensions(ItemStack tablet) {
        for (var stack : readUpgrades(tablet)) {
            if (stack.is(ANY_DIMENSION)) {
                return true;
            }
        }
        return false;
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

    /** Number of pages after the Modules page: Library, Armory, Tools, Automation and Devices. */
    public static final int PAGES = StorageKind.values().length + 2;

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
        if (value >= PIN_STORAGE && value < PIN_STORAGE + PAGES) {
            return value;
        }
        return -1;
    }

    /** Pins the tab, or unpins it if it was already pinned. */
    public static void togglePin(ItemStack tablet, int pin) {
        boolean validModule = pin >= 0 && pin < SLOTS && !get(tablet, pin).isEmpty();
        boolean validStorage = pin >= PIN_STORAGE && pin < PIN_STORAGE + PAGES;
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
