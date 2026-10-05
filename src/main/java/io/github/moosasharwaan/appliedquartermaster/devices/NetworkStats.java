package io.github.moosasharwaan.appliedquartermaster.devices;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.me.cells.BasicCellInventory;
import io.github.moosasharwaan.appliedquartermaster.network.DevicesViewPayload;
import io.github.moosasharwaan.appliedquartermaster.network.NetworkStatsPayload;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Gathers the Network app's Statistics view. Only runs while someone has that view open, about every 5 seconds:
 * one pass over AE2's cached storage list (item types, not single items) plus a look at each drive and ME Chest.
 */
public final class NetworkStats {

    /** Comparison periods for Falling stock: 10 minutes, 1 hour, 1 day (in ticks). */
    public static final long[] PERIODS = {12_000, 72_000, 1_728_000};
    private static final int MOD_ROWS = 5;
    private static final int FALLING_ROWS = 6;

    private NetworkStats() {
    }

    public static NetworkStatsPayload build(int containerId, IGrid grid, int period, long now) {
        // Cells in drives and ME Chests
        long usedBytes = 0;
        long totalBytes = 0;
        long usedTypes = 0;
        long totalTypes = 0;
        int cells = 0;
        int drives = 0;
        int chests = 0;
        int freeSlots = 0;
        var holders = new ArrayList<IChestOrDrive>();
        for (var cls : grid.getMachineClasses()) {
            if (IChestOrDrive.class.isAssignableFrom(cls)) {
                for (var machine : grid.getMachines(cls)) {
                    holders.add((IChestOrDrive) machine);
                }
            }
        }
        for (var holder : holders) {
            if (holder instanceof appeng.blockentity.storage.MEChestBlockEntity) {
                chests++;
            } else {
                drives++;
            }
            for (int i = 0; i < holder.getCellCount(); i++) {
                var cell = holder.getOriginalCellInventory(i);
                if (cell == null) {
                    freeSlots++;
                    continue;
                }
                cells++;
                if (cell instanceof BasicCellInventory basic) {
                    usedBytes += basic.getUsedBytes();
                    totalBytes += basic.getTotalBytes();
                    usedTypes += basic.getStoredItemTypes();
                    totalTypes += basic.getTotalItemTypes();
                }
            }
        }

        // What is stored, by mod
        long items = 0;
        int itemTypes = 0;
        double fluidBuckets = 0;
        var byMod = new HashMap<String, long[]>();
        for (var entry : grid.getStorageService().getCachedInventory()) {
            var key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0) {
                continue;
            }
            if (key instanceof AEItemKey) {
                items += amount;
                itemTypes++;
                var counts = byMod.computeIfAbsent(key.getModId(), k -> new long[2]);
                counts[0] += amount;
                counts[1]++;
            } else if (key instanceof AEFluidKey) {
                fluidBuckets += amount / (double) key.getAmountPerUnit();
            }
        }
        var sorted = new ArrayList<>(byMod.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        var mods = new ArrayList<NetworkStatsPayload.ModShare>();
        long otherItems = 0;
        int otherTypes = 0;
        int otherMods = 0;
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            if (i < MOD_ROWS || sorted.size() == MOD_ROWS + 1) {
                mods.add(new NetworkStatsPayload.ModShare(modName(e.getKey()), e.getValue()[0], (int) e.getValue()[1]));
            } else {
                otherItems += e.getValue()[0];
                otherTypes += (int) e.getValue()[1];
                otherMods++;
            }
        }
        if (otherMods > 0) {
            // The client shows this row as "N other mods"; the name carries the count.
            mods.add(new NetworkStatsPayload.ModShare("#" + otherMods, otherItems, otherTypes));
        }

        // Energy and the grid
        var energy = grid.getEnergyService();
        var pathing = grid.getPathingService();
        int controller = pathing.getControllerState() == appeng.api.networking.pathing.ControllerState.CONTROLLER_ONLINE
                ? DevicesViewPayload.CONTROLLER_ONLINE
                : pathing.getControllerState() == appeng.api.networking.pathing.ControllerState.CONTROLLER_CONFLICT
                ? DevicesViewPayload.CONTROLLER_CONFLICT : DevicesViewPayload.NO_CONTROLLER;
        int devices = 0;
        for (var ignored : grid.getNodes()) {
            devices++;
        }
        int cpus = 0;
        int busy = 0;
        for (var cpu : grid.getCraftingService().getCpus()) {
            cpus++;
            if (cpu.isBusy()) {
                busy++;
            }
        }

        // Falling stock
        int p = Math.max(0, Math.min(PERIODS.length - 1, period));
        var result = StockHistory.track(grid, now).falling(grid, now, PERIODS[p], FALLING_ROWS);
        var falling = new ArrayList<NetworkStatsPayload.Falling>();
        for (var f : result.items()) {
            falling.add(new NetworkStatsPayload.Falling(f.key().toStack(), f.key().getDisplayName().getString(),
                    f.before(), f.now(), f.minutesToEmpty()));
        }

        return new NetworkStatsPayload(containerId,
                new NetworkStatsPayload.Storage(usedBytes, totalBytes, usedTypes, totalTypes, cells, drives, chests,
                        freeSlots, items, itemTypes, fluidBuckets),
                new NetworkStatsPayload.Energy(energy.getStoredPower(), energy.getMaxStoredPower(),
                        energy.getAvgPowerUsage(), energy.getAvgPowerInjection()),
                new NetworkStatsPayload.Grid(controller, pathing.getUsedChannels(), devices, cpus, busy),
                mods, p, result.spanTicks(), result.hasHistory(), falling);
    }

    private static String modName(String modId) {
        return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getDisplayName()).orElse(modId);
    }

    /** True when the two views would look the same (so nothing needs sending). */
    public static boolean same(NetworkStatsPayload a, NetworkStatsPayload b) {
        if (b == null) {
            return false;
        }
        return a.storage().equals(b.storage()) && a.grid().equals(b.grid()) && a.mods().equals(b.mods())
                && a.period() == b.period() && a.hasHistory() == b.hasHistory() && fallingSame(a.falling(), b.falling())
                && Math.abs(a.energy().stored() - b.energy().stored()) < Math.max(1, a.energy().max() * 0.005)
                && Math.abs(a.energy().usage() - b.energy().usage()) < 0.5;
    }

    private static boolean fallingSame(List<NetworkStatsPayload.Falling> a, List<NetworkStatsPayload.Falling> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            var x = a.get(i);
            var y = b.get(i);
            if (!x.name().equals(y.name()) || x.now() != y.now() || x.before() != y.before()) {
                return false;
            }
        }
        return true;
    }
}
