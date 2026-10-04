package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the Automation page. Outside a farm, {@code entries} are the farms; inside a farm
 * ({@code inFarm}), they are that farm's plates and {@code title} is the farm's name.
 */
public record AutomationViewPayload(int containerId, boolean inFarm, String title, ItemStack titleIcon,
                                    boolean farmOnline, List<Entry> entries) implements CustomPacketPayload {

    /** Farm: offline (0) or online (1). Plate: offline (0), off (1) or on (2). */
    public static final int OFFLINE = 0;
    public static final int OFF = 1;
    public static final int ON = 2;

    /**
     * One farm or plate.
     *
     * @param state    see {@link #OFFLINE}, {@link #OFF}, {@link #ON}; for farms, 1 means online
     * @param on       farms: plates switched on; plates: 1 if on
     * @param total    farms: number of plates
     * @param strength plates: signal strength 1 to 15
     */
    public record Entry(ItemStack icon, boolean customIcon, String name, int state, int on, int total, int strength) {
        static void write(RegistryFriendlyByteBuf buf, Entry e) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon);
            buf.writeBoolean(e.customIcon);
            buf.writeUtf(e.name);
            buf.writeVarInt(e.state);
            buf.writeVarInt(e.on);
            buf.writeVarInt(e.total);
            buf.writeVarInt(e.strength);
        }

        static Entry read(RegistryFriendlyByteBuf buf) {
            return new Entry(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readUtf(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        boolean sameAs(Entry o) {
            return ItemStack.matches(icon, o.icon) && customIcon == o.customIcon && name.equals(o.name)
                    && state == o.state && on == o.on && total == o.total && strength == o.strength;
        }
    }

    public static final Type<AutomationViewPayload> TYPE = new Type<>(AppliedQuartermaster.id("automation_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutomationViewPayload> STREAM_CODEC = StreamCodec.ofMember(
            AutomationViewPayload::write, AutomationViewPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeBoolean(inFarm);
        buf.writeUtf(title);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, titleIcon);
        buf.writeBoolean(farmOnline);
        buf.writeVarInt(entries.size());
        for (var e : entries) {
            Entry.write(buf, e);
        }
    }

    private static AutomationViewPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean inFarm = buf.readBoolean();
        String title = buf.readUtf();
        ItemStack titleIcon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        boolean online = buf.readBoolean();
        int n = buf.readVarInt();
        var entries = new ArrayList<Entry>(n);
        for (int i = 0; i < n; i++) {
            entries.add(Entry.read(buf));
        }
        return new AutomationViewPayload(id, inFarm, title, titleIcon, online, entries);
    }

    public boolean sameAs(AutomationViewPayload o) {
        if (o == null || inFarm != o.inFarm || !title.equals(o.title) || !ItemStack.matches(titleIcon, o.titleIcon)
                || farmOnline != o.farmOnline || entries.size() != o.entries.size()) {
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
