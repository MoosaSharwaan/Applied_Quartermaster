package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: an action on an item shown in a tablet storage page. */
public record TabletActionPayload(int containerId, int action, int entry, ItemStack expected, int arg, String text)
        implements CustomPacketPayload {

    public static final int PICKUP = 0;
    public static final int TAKE = 1;
    public static final int STORE = 2;
    public static final int READ = 3;
    public static final int SET_VIEW = 4;
    // Automation page
    public static final int OPEN_FARM = 10;
    public static final int BACK = 11;
    public static final int TOGGLE = 12;
    public static final int STRENGTH = 13;
    public static final int ALL_ON = 14;
    public static final int ALL_OFF = 15;
    /** Sets the icon of a farm or plate to the held (carried) item, or clears it when nothing is held. */
    public static final int SET_ICON = 16;
    public static final int RENAME = 17;
    /** Sets a plate's signal strength to {@code arg} (1 to 15). */
    public static final int SET_STRENGTH = 18;
    // Devices page
    public static final int DEVICE_OPEN = 20;
    public static final int DEVICE_BACK = 21;
    /** Shows where a device is: a light beam above it and its position and direction in the action bar. */
    public static final int DEVICE_LOCATE = 22;
    // Statistics view
    /** Falling stock period: {@code arg} 0 = 10 minutes, 1 = 1 hour, 2 = 1 day. */
    public static final int STATS_PERIOD = 30;

    public TabletActionPayload(int containerId, int action, int entry, ItemStack expected, int arg) {
        this(containerId, action, entry, expected, arg, "");
    }

    public static final Type<TabletActionPayload> TYPE = new Type<>(AppliedQuartermaster.id("tablet_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabletActionPayload> STREAM_CODEC = StreamCodec.ofMember(
            TabletActionPayload::write, TabletActionPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(action);
        buf.writeVarInt(entry);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, expected);
        buf.writeVarInt(arg);
        buf.writeUtf(text, 64);
    }

    private static TabletActionPayload read(RegistryFriendlyByteBuf buf) {
        return new TabletActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readUtf(64));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TabletActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof TabletMenu menu
                && menu.containerId == payload.containerId()) {
            menu.handleAction(player, payload);
        }
    }
}
