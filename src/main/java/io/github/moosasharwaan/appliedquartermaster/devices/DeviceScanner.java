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

    /** One device on the network. */
    public record Device(IGridNode node, AEItemKey kind, BlockPos pos, String dimension) {
    }

    private DeviceScanner() {
    }

    /** All devices that show as an item, in a stable order (by kind name, then position). */
    public static List<Device> devices(IGrid grid) {
        var list = new ArrayList<Device>();
        for (var node : grid.getNodes()) {
            var kind = node.getVisualRepresentation();
            if (kind == null) {
                continue;
            }
            var pos = position(node);
            String dimension = node.getLevel() != null ? node.getLevel().dimension().identifier().toString() : "";
            list.add(new Device(node, kind, pos == null ? BlockPos.ZERO : pos, dimension));
        }
        list.sort(Comparator.comparing((Device d) -> d.kind().getDisplayName().getString())
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

    /** The overview: one entry per kind of device. */
    public static List<DevicesViewPayload.Entry> kinds(List<Device> devices, List<AEItemKey> kindRefs) {
        var groups = new LinkedHashMap<AEItemKey, List<Device>>();
        for (var d : devices) {
            groups.computeIfAbsent(d.kind(), k -> new ArrayList<>()).add(d);
        }
        var entries = new ArrayList<DevicesViewPayload.Entry>();
        kindRefs.clear();
        for (var group : groups.entrySet()) {
            int active = 0;
            int channels = 0;
            double power = 0;
            for (var d : group.getValue()) {
                if (d.node().isActive()) {
                    active++;
                }
                channels += d.node().getUsedChannels();
                power += d.node().getIdlePowerUsage();
            }
            kindRefs.add(group.getKey());
            entries.add(new DevicesViewPayload.Entry(group.getKey().toStack(), name(group.getKey()), group.getValue().size(),
                    active, DevicesViewPayload.ACTIVE, channels, power, BlockPos.ZERO, ""));
        }
        return entries;
    }

    /** Inside a kind: one entry per device. */
    public static List<DevicesViewPayload.Entry> devicesOf(List<Device> devices, AEItemKey kind, List<Device> deviceRefs) {
        var entries = new ArrayList<DevicesViewPayload.Entry>();
        deviceRefs.clear();
        for (var d : devices) {
            if (!d.kind().equals(kind)) {
                continue;
            }
            deviceRefs.add(d);
            var node = d.node();
            entries.add(new DevicesViewPayload.Entry(kind.toStack(), name(kind), 1, node.isActive() ? 1 : 0, state(node),
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
