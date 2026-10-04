package io.github.moosasharwaan.appliedquartermaster.tablet;

import io.github.moosasharwaan.appliedquartermaster.integration.curios.CuriosCompat;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Where a player's tablet is. A slot number of 0 or more is a player inventory slot (as Minecraft numbers them);
 * a negative number is a Curios slot (see {@link CuriosCompat}), so a tablet worn as a curio works the same way.
 */
public final class TabletSlots {

    private TabletSlots() {
    }

    public static boolean isCurio(int slot) {
        return slot < 0;
    }

    /** The stack in the given slot, or empty. */
    public static ItemStack get(Player player, int slot) {
        if (slot >= 0) {
            var inventory = player.getInventory();
            return slot < inventory.getContainerSize() ? inventory.getItem(slot) : ItemStack.EMPTY;
        }
        return CuriosCompat.isLoaded() ? CuriosCompat.get(player, slot) : ItemStack.EMPTY;
    }

    /** The tablet in the given slot, or empty if that slot no longer holds a tablet. */
    public static ItemStack tablet(Player player, int slot) {
        var stack = get(player, slot);
        return stack.is(ModItems.ME_TABLET.get()) ? stack : ItemStack.EMPTY;
    }

    /**
     * Finds a tablet to open with the key: the main hand first, then the off hand, the rest of the inventory, and
     * last any Curios slot. Returns the slot, or {@link Integer#MIN_VALUE} when the player carries no tablet.
     */
    public static int find(Player player) {
        var inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (inventory.getItem(selected).is(ModItems.ME_TABLET.get())) {
            return selected;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.ME_TABLET.get()) && inventory.getItem(i) == player.getOffhandItem()) {
                return i;
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.ME_TABLET.get())) {
                return i;
            }
        }
        if (CuriosCompat.isLoaded()) {
            int slot = CuriosCompat.find(player);
            if (slot != Integer.MIN_VALUE) {
                return slot;
            }
        }
        return Integer.MIN_VALUE;
    }
}
