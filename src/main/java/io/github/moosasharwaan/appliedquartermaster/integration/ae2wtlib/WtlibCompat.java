package io.github.moosasharwaan.appliedquartermaster.integration.ae2wtlib;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Support for terminals built on AE2WTLib (AE2WTLib's own and those of addons such as AdvancedAE).
 * AE2WTLib's classes are only used from {@link WtlibTerminals}, which loads after the installed check.
 */
public final class WtlibCompat {

    private static Boolean loaded;

    private WtlibCompat() {
    }

    /** True when AE2WTLib's API is present: installed as a mod, or bundled inside an addon such as AdvancedAE. */
    public static boolean isLoaded() {
        if (loaded == null) {
            boolean found;
            try {
                Class.forName("de.mari_023.ae2wtlib.api.terminal.ItemWT", false, WtlibCompat.class.getClassLoader());
                found = true;
            } catch (ClassNotFoundException | LinkageError e) {
                found = false;
            }
            loaded = found;
        }
        return loaded;
    }

    /**
     * Opens an AE2WTLib terminal through its own open method, which chooses the right menu (AE2's generic open
     * would show the plain storage menu and crash on the terminal's extra settings).
     *
     * @return whether it opened, or null when the terminal isn't an AE2WTLib one
     */
    public static @Nullable Boolean tryOpen(WirelessTerminalItem terminal, Player player, ItemMenuHostLocator locator) {
        return WtlibTerminals.tryOpen(terminal, player, locator);
    }
}
