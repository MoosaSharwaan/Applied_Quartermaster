package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the Devices page. The overview lists one entry per kind of AE2 device on the network; opening a
 * kind ({@code inType}) lists each device of that kind with its position and state. {@code summary} describes the
 * whole network either way.
 */
public record DevicesViewPayload(int containerId, boolean inType, String title, ItemStack titleIcon, Summary summary,
                                 List<Entry> entries) implements CustomPacketPayload {

    /** Device states, used for single devices; kinds use {@code active} of {@code count}. */
    public static final int ACTIVE = 0;
    public static final int NO_CHANNEL = 1;
    public static final int NO_POWER = 2;
    public static final int BOOTING = 3;

    /** Controller states. */
    public static final int NO_CONTROLLER = 0;
    public static final int CONTROLLER_ONLINE = 1;
    public static final int CONTROLLER_CONFLICT = 2;

    public record Summary(double stored, double maxStored, double usage, double injection, int channels,
                          int devices, int controller, boolean powered) {
        static final Summary EMPTY = new Summary(0, 0, 0, 0, 0, 0, NO_CONTROLLER, false);

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeDouble(stored);
            buf.writeDouble(maxStored);
            buf.writeDouble(usage);
            buf.writeDouble(injection);
            buf.writeVarInt(channels);
            buf.writeVarInt(devices);
            buf.writeVarInt(controller);
            buf.writeBoolean(powered);
        }

        static Summary read(RegistryFriendlyByteBuf buf) {
            return new Summary(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
        }

        boolean sameAs(Summary o) {
            // Energy figures move every tick; only resend when they change visibly.
            return Math.abs(stored - o.stored) < Math.max(1, maxStored / 200) && maxStored == o.maxStored
                    && Math.abs(usage - o.usage) < 0.05 && Math.abs(injection - o.injection) < 0.05
                    && channels == o.channels && devices == o.devices && controller == o.controller && powered == o.powered;
        }
    }

    /**
     * A kind of device (overview) or one device (inside a kind).
     *
     * @param count    overview: number of devices of this kind; device: 1
     * @param active   overview: how many are active; device: 1 when active
     * @param state    device: {@link #ACTIVE}, {@link #NO_CHANNEL}, {@link #NO_POWER} or {@link #BOOTING}
     * @param channels channels used
     * @param power    idle power use in AE/t
     * @param pos      device: where it is
     * @param dimension device: which dimension, e.g. {@code minecraft:overworld}
     */
    public record Entry(ItemStack icon, String name, int count, int active, int state, int channels, double power,
                        BlockPos pos, String dimension) {
        static void write(RegistryFriendlyByteBuf buf, Entry e) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon);
            buf.writeUtf(e.name);
            buf.writeVarInt(e.count);
            buf.writeVarInt(e.active);
            buf.writeVarInt(e.state);
            buf.writeVarInt(e.channels);
            buf.writeDouble(e.power);
            buf.writeBlockPos(e.pos);
            buf.writeUtf(e.dimension);
        }

        static Entry read(RegistryFriendlyByteBuf buf) {
            return new Entry(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readBlockPos(), buf.readUtf());
        }

        boolean sameAs(Entry o) {
            return ItemStack.matches(icon, o.icon) && name.equals(o.name) && count == o.count && active == o.active
                    && state == o.state && channels == o.channels && Math.abs(power - o.power) < 0.01
                    && pos.equals(o.pos) && dimension.equals(o.dimension);
        }
    }

    public static final Type<DevicesViewPayload> TYPE = new Type<>(AppliedQuartermaster.id("devices_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DevicesViewPayload> STREAM_CODEC = StreamCodec.ofMember(
            DevicesViewPayload::write, DevicesViewPayload::read);

    public static DevicesViewPayload empty(int containerId) {
        return new DevicesViewPayload(containerId, false, "", ItemStack.EMPTY, Summary.EMPTY, List.of());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeBoolean(inType);
        buf.writeUtf(title);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, titleIcon);
        summary.write(buf);
        buf.writeVarInt(entries.size());
        for (var e : entries) {
            Entry.write(buf, e);
        }
    }

    private static DevicesViewPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean inType = buf.readBoolean();
        String title = buf.readUtf();
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        Summary summary = Summary.read(buf);
        int n = buf.readVarInt();
        var entries = new ArrayList<Entry>(n);
        for (int i = 0; i < n; i++) {
            entries.add(Entry.read(buf));
        }
        return new DevicesViewPayload(id, inType, title, icon, summary, entries);
    }

    public boolean sameAs(DevicesViewPayload o) {
        if (o == null || inType != o.inType || !title.equals(o.title) || !ItemStack.matches(titleIcon, o.titleIcon)
                || !summary.sameAs(o.summary) || entries.size() != o.entries.size()) {
            return false;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).sameAs(o.entries.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
