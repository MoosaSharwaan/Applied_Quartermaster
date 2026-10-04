package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.api.networking.IGrid;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.storage.ArmoryBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.LibraryBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import io.github.moosasharwaan.appliedquartermaster.storage.ToolRackBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the AE2 network a tablet is linked to: the network of its terminal module, reached through
 * a powered Wireless Access Point in range, just like the terminal itself.
 */
public final class TabletNetwork {

    public static final int OK = 0;
    public static final int NO_TERMINAL = 1;
    public static final int NOT_LINKED = 2;
    public static final int OUT_OF_RANGE = 3;

    public record Result(@Nullable IGrid grid, int status) {
    }

    private TabletNetwork() {
    }

    public static Result find(Player player, ItemStack tablet) {
        boolean anyTerminal = false;
        boolean anyLinked = false;
        for (var module : TabletModules.read(tablet)) {
            if (!(module.getItem() instanceof WirelessTerminalItem terminal)) {
                continue;
            }
            anyTerminal = true;
            var grid = terminal.getLinkedGrid(module, player.level(), null);
            if (grid == null) {
                continue;
            }
            anyLinked = true;
            if (inRange(player, grid)) {
                return new Result(grid, OK);
            }
        }
        return new Result(null, !anyTerminal ? NO_TERMINAL : !anyLinked ? NOT_LINKED : OUT_OF_RANGE);
    }

    private static boolean inRange(Player player, IGrid grid) {
        for (var wap : grid.getMachines(WirelessAccessPointBlockEntity.class)) {
            if (!wap.isActive()) {
                continue;
            }
            var location = wap.getLocation();
            if (location.getLevel() != player.level()) {
                continue;
            }
            var pos = location.getPos();
            double dx = pos.getX() + 0.5 - player.getX();
            double dy = pos.getY() + 0.5 - player.getY();
            double dz = pos.getZ() + 0.5 - player.getZ();
            double range = wap.getRange();
            if (dx * dx + dy * dy + dz * dz < range * range) {
                return true;
            }
        }
        return false;
    }

    public static Class<? extends StorageBlockEntity> blockEntityClass(StorageKind kind) {
        return switch (kind) {
            case LIBRARY -> LibraryBlockEntity.class;
            case ARMORY -> ArmoryBlockEntity.class;
            case TOOLS -> ToolRackBlockEntity.class;
        };
    }

    /** True when at least one block of this kind is on the grid (unlocks its tab). */
    public static boolean present(IGrid grid, StorageKind kind) {
        return grid.getMachineNodes(blockEntityClass(kind)).iterator().hasNext();
    }

    /** Active (powered, with a channel) blocks of this kind on the grid, in a stable order. */
    public static List<StorageBlockEntity> blocks(IGrid grid, StorageKind kind) {
        var list = new ArrayList<StorageBlockEntity>(grid.getActiveMachines(blockEntityClass(kind)));
        list.sort(Comparator.comparingLong((StorageBlockEntity be) -> be.getBlockPos().asLong()));
        return list;
    }
}
