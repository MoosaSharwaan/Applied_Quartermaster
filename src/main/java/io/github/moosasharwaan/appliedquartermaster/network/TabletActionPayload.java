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
public record TabletActionPayload(int containerId, int action, int entry, ItemStack expected, int arg)
        implements CustomPacketPayload {

    public static final int PICKUP = 0;
    public static final int TAKE = 1;
    public static final int STORE = 2;
    public static final int READ = 3;
    public static final int SET_VIEW = 4;

    public static final Type<TabletActionPayload> TYPE = new Type<>(AppliedQuartermaster.id("tablet_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabletActionPayload> STREAM_CODEC = StreamCodec.ofMember(
            TabletActionPayload::write, TabletActionPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(action);
        buf.writeVarInt(entry);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, expected);
        buf.writeVarInt(arg);
    }

    private static TabletActionPayload read(RegistryFriendlyByteBuf buf) {
        return new TabletActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt());
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
