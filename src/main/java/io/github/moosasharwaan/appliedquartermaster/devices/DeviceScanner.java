package io.github.moosasharwaan.appliedquartermaster.devices;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.stacks.AEItemKey;
import appeng.me.InWorldGridNode;
import appeng.parts.AEBasePart;
import io.github.moosasharwaan.appliedquartermaster.automation.FarmControllerBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.network.DevicesViewPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Reads what is on an AE2 network for the tablet's Devices page: every device grouped by kind (the item it shows as,
 * like AE2's network status screen), each device's position and state, and the network's energy and channels.
 */
public final class DeviceScanner {

    /**
     * One device on the network. Devices of the same kind in one scan share the same {@code kind} instance and
     * {@code name}, so grouping and sorting compare references and strings instead of item keys.
     */
    public record Device(IGridNode node, AEItemKey kind, String name, BlockPos pos, String dimension) {
    }

    private DeviceScanner() {
    }

    /** The last scan of each network, reused while no device is added or removed. */
    private record Snapshot(int count, long signature, long time, List<Device> devices) {
    }

    private static final java.util.Map<IGrid, Snapshot> SNAPSHOTS =
            java.util.Collections.synchronizedMap(new java.util.HashMap<>());
    private static final long MAX_AGE_NANOS = 10_000_000_000L;

    /**
     * Like {@link #scan} but reuses the last result for the same network while its set of devices is unchanged
     * (checked cheaply each time, and rebuilt at least every 10 seconds). Device states (active, channels, power)
     * are read live from the nodes, so they stay current either way.
     */
    public static List<Device> devices(IGrid grid) {
        int count = 0;
        long signature = 0;
        for (var node : grid.getNodes()) {
            count++;
            signature += System.identityHashCode(node) * 0x9E3779B97F4A7C15L;
        }
        long now = System.nanoTime();
        var snapshot = SNAPSHOTS.get(grid);
        if (snapshot != null && snapshot.count() == count && snapshot.signature() == signature
                && now - snapshot.time() < MAX_AGE_NANOS) {
            return snapshot.devices();
        }
        var devices = java.util.Collections.unmodifiableList(scan(grid));
        // A snapshot's nodes point back to their grid, so weak keys alone wouldn't free old networks: keep it small.
        if (SNAPSHOTS.size() >= 16) {
            SNAPSHOTS.clear();
        }
        SNAPSHOTS.put(grid, new Snapshot(count, signature, now, devices));
        return devices;
    }

    /** All devices that show as an item, freshly read, in a stable order (by kind name, then position). */
    public static List<Device> scan(IGrid grid) {
        var list = new ArrayList<Device>();
        // Each kind is looked up once per scan (building names and comparing item keys is slow, and a big network
        // has thousands of devices but only a few dozen kinds); devices then share that kind's instance and name.
        var kinds = new java.util.HashMap<AEItemKey, AEItemKey>();
        var names = new java.util.IdentityHashMap<AEItemKey, String>();
        var dimensions = new java.util.IdentityHashMap<Object, String>();
        for (var node : grid.getNodes()) {
            var visual = node.getVisualRepresentation();
            if (visual == null) {
                continue;
            }
            var kind = kinds.computeIfAbsent(visual, k -> k);
            var name = names.computeIfAbsent(kind, k -> k.getDisplayName().getString());
            var pos = position(node);
            var nodeLevel = node.getLevel();
            String dimension = nodeLevel == null ? ""
                    : dimensions.computeIfAbsent(nodeLevel, l -> nodeLevel.dimension().identifier().toString());
            list.add(new Device(node, kind, name, pos == null ? BlockPos.ZERO : pos, dimension));
        }
        // Same-named kinds (rare) stay apart through the identity tie-break, so each kind's devices are contiguous.
        list.sort(Comparator.comparing(Device::name)
                .thenComparingInt(d -> System.identityHashCode(d.kind()))
                .thenComparing(Device::dimension)
                .thenComparingLong(d -> d.pos().asLong()));
        return list;
    }

    /** Where a node is: its block, the block holding its cable part, or the block entity that owns it. */
    public static @Nullable BlockPos position(IGridNode node) {
        if (node instanceof InWorldGridNode inWorld) {
            return inWorld.getLocation();
        }
        var owner = node.getOwner();
        if (owner instanceof AEBasePart part && part.getBlockEntity() != null) {
            return part.getBlockEntity().getBlockPos();
        }
        if (owner instanceof BlockEntity be) {
            return be.getBlockPos();
        }
        if (owner instanceof FarmControllerBlockEntity.FarmPort port) {
            return port.controller().getBlockPos();
        }
        return null;
    }

    public static int state(IGridNode node) {
        if (!node.isPowered()) {
            return DevicesViewPayload.NO_POWER;
        }
        if (!node.meetsChannelRequirements()) {
            return DevicesViewPayload.NO_CHANNEL;
        }
        if (!node.hasGridBooted()) {
            return DevicesViewPayload.BOOTING;
        }
        return DevicesViewPayload.ACTIVE;
    }

    public static DevicesViewPayload.Summary summary(IGrid grid, int deviceCount) {
        var energy = grid.getEnergyService();
        var pathing = grid.getPathingService();
        int controller = pathing.getControllerState() == ControllerState.CONTROLLER_ONLINE
                ? DevicesViewPayload.CONTROLLER_ONLINE
                : pathing.getControllerState() == ControllerState.CONTROLLER_CONFLICT
                ? DevicesViewPayload.CONTROLLER_CONFLICT : DevicesViewPayload.NO_CONTROLLER;
        return new DevicesViewPayload.Summary(energy.getStoredPower(), energy.getMaxStoredPower(),
                energy.getAvgPowerUsage(), energy.getAvgPowerInjection(), pathing.getUsedChannels(), deviceCount,
                controller, energy.isNetworkPowered());
    }

    /** The overview: one entry per kind of device. Relies on {@link #devices} keeping each kind together. */
    public static List<DevicesViewPayload.Entry> kinds(List<Device> devices, List<AEItemKey> kindRefs) {
        var entries = new ArrayList<DevicesViewPayload.Entry>();
        kindRefs.clear();
        int i = 0;
        while (i < devices.size()) {
            var first = devices.get(i);
            int active = 0;
            int channels = 0;
            double power = 0;
            int count = 0;
            while (i < devices.size() && devices.get(i).kind() == first.kind()) {
                var node = devices.get(i).node();
                if (node.isActive()) {
                    active++;
                }
                channels += node.getUsedChannels();
                power += node.getIdlePowerUsage();
                count++;
                i++;
            }
            kindRefs.add(first.kind());
            entries.add(new DevicesViewPayload.Entry(first.kind().toStack(), first.name(), count,
                    active, DevicesViewPayload.ACTIVE, channels, power, BlockPos.ZERO, ""));
        }
        return entries;
    }

    /** Inside a kind: one entry per device. */
    public static List<DevicesViewPayload.Entry> devicesOf(List<Device> devices, AEItemKey kind, List<Device> deviceRefs) {
        var entries = new ArrayList<DevicesViewPayload.Entry>();
        deviceRefs.clear();
        AEItemKey match = null;
        ItemStack icon = ItemStack.EMPTY;
        String kindName = name(kind);
        for (var d : devices) {
            // Compare names first (cheap), the item key once, then by reference.
            if (match == null) {
                if (!d.name().equals(kindName) || !d.kind().equals(kind)) {
                    continue;
                }
                match = d.kind();
                icon = match.toStack();
            } else if (d.kind() != match) {
                if (!deviceRefs.isEmpty()) {
                    break; // devices of a kind are contiguous
                }
                continue;
            }
            deviceRefs.add(d);
            var node = d.node();
            entries.add(new DevicesViewPayload.Entry(icon, d.name(), 1, node.isActive() ? 1 : 0, state(node),
                    node.getUsedChannels(), node.getIdlePowerUsage(), d.pos(), d.dimension()));
        }
        return entries;
    }

    public static String name(AEItemKey kind) {
        Component name = kind.getDisplayName();
        return name.getString();
    }

    public static ItemStack icon(AEItemKey kind) {
        return kind.toStack();
    }
}
