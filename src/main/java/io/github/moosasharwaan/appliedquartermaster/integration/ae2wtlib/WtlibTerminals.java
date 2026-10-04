package io.github.moosasharwaan.appliedquartermaster.integration.ae2wtlib;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Uses AE2WTLib classes; only reached through {@link WtlibCompat} when AE2WTLib is installed. */
final class WtlibTerminals {

    private WtlibTerminals() {
    }

    static @Nullable Boolean tryOpen(WirelessTerminalItem terminal, Player player, ItemMenuHostLocator locator) {
        if (terminal instanceof ItemWT wt) {
            return wt.tryOpen(player, locator, false);
        }
        return null;
    }
}
