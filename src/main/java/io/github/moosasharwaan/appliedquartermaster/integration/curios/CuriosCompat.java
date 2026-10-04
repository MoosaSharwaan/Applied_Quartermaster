package io.github.moosasharwaan.appliedquartermaster.integration.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Curios support: the ME Tablet can be worn in a curio slot and opened with the "Open ME Tablet" key.
 * Curios classes are only used from {@link CuriosTablets}, which loads after the installed check.
 * <p>
 * Curios slots are numbered -1, -2, -3… in a fixed order (slot types by name, then index), so they fit in the same
 * slot number the tablet already uses for inventory slots.
 */
public final class CuriosCompat {

    private static Boolean loaded;

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded("curios");
        }
        return loaded;
    }

    public static ItemStack get(Player player, int slot) {
        return CuriosTablets.get(player, slot);
    }

    /** The curio slot holding a tablet, or {@link Integer#MIN_VALUE}. */
    public static int find(Player player) {
        return CuriosTablets.find(player);
    }

    /** Puts a stack in a curio slot type (the self-test uses this); returns the slot number or MIN_VALUE. */
    public static int equip(Player player, String slotType, ItemStack stack) {
        return CuriosTablets.equip(player, slotType, stack);
    }

    /** The curio slot types the player has, for the self-test log. */
    public static String describe(Player player) {
        return CuriosTablets.describe(player);
    }
}
