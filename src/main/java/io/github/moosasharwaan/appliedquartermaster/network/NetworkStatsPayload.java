package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server to client: the Network app's Statistics view. Sent about every 5 seconds while that view is open. */
public record NetworkStatsPayload(int containerId, Storage storage, Energy energy, Grid grid, List<ModShare> mods,
                                  int period, long spanTicks, boolean hasHistory, List<Falling> falling)
        implements CustomPacketPayload {

    /** Cell storage (bytes and types count only AE2-style cells), what is stored, and where. */
    public record Storage(long usedBytes, long totalBytes, long usedTypes, long totalTypes, int cells, int drives,
                          int chests, int freeCellSlots, long items, int itemTypes, double fluidBuckets) {
    }

    public record Energy(double stored, double max, double usage, double injection) {
    }

    public record Grid(int controller, int channels, int devices, int cpus, int busyCpus) {
    }

    public record ModShare(String name, long items, int types) {
    }

    public record Falling(ItemStack icon, String name, long before, long now, long minutesToEmpty) {
    }

    public static final Type<NetworkStatsPayload> TYPE = new Type<>(AppliedQuartermaster.id("network_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkStatsPayload> STREAM_CODEC = StreamCodec.ofMember(
            NetworkStatsPayload::write, NetworkStatsPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarLong(storage.usedBytes());
        buf.writeVarLong(storage.totalBytes());
        buf.writeVarLong(storage.usedTypes());
        buf.writeVarLong(storage.totalTypes());
        buf.writeVarInt(storage.cells());
        buf.writeVarInt(storage.drives());
        buf.writeVarInt(storage.chests());
        buf.writeVarInt(storage.freeCellSlots());
        buf.writeVarLong(storage.items());
        buf.writeVarInt(storage.itemTypes());
        buf.writeDouble(storage.fluidBuckets());
        buf.writeDouble(energy.stored());
        buf.writeDouble(energy.max());
        buf.writeDouble(energy.usage());
        buf.writeDouble(energy.injection());
        buf.writeVarInt(grid.controller());
        buf.writeVarInt(grid.channels());
        buf.writeVarInt(grid.devices());
        buf.writeVarInt(grid.cpus());
        buf.writeVarInt(grid.busyCpus());
        buf.writeVarInt(mods.size());
        for (var m : mods) {
            buf.writeUtf(m.name(), 64);
            buf.writeVarLong(m.items());
            buf.writeVarInt(m.types());
        }
        buf.writeVarInt(period);
        buf.writeVarLong(spanTicks);
        buf.writeBoolean(hasHistory);
        buf.writeVarInt(falling.size());
        for (var f : falling) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, f.icon());
            buf.writeUtf(f.name(), 128);
            buf.writeVarLong(f.before());
            buf.writeVarLong(f.now());
            buf.writeVarLong(f.minutesToEmpty());
        }
    }

    private static NetworkStatsPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        var storage = new Storage(buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt(),
                buf.readDouble());
        var energy = new Energy(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        var grid = new Grid(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        int n = buf.readVarInt();
        var mods = new ArrayList<ModShare>(n);
        for (int i = 0; i < n; i++) {
            mods.add(new ModShare(buf.readUtf(64), buf.readVarLong(), buf.readVarInt()));
        }
        int period = buf.readVarInt();
        long span = buf.readVarLong();
        boolean hasHistory = buf.readBoolean();
        int f = buf.readVarInt();
        var falling = new ArrayList<Falling>(f);
        for (int i = 0; i < f; i++) {
            falling.add(new Falling(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readUtf(128), buf.readVarLong(),
                    buf.readVarLong(), buf.readVarLong()));
        }
        return new NetworkStatsPayload(id, storage, energy, grid, mods, period, span, hasHistory, falling);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
