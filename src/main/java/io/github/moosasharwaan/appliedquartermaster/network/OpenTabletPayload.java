package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletItem;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletSlots;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when the "Open ME Tablet" key is pressed: open the player's tablet from wherever it is (hand, inventory or a
 * Curios slot), as right-clicking it would. {@code modulesPage} is set when sneaking.
 */
public record OpenTabletPayload(boolean modulesPage) implements CustomPacketPayload {

    public static final Type<OpenTabletPayload> TYPE = new Type<>(AppliedQuartermaster.id("open_tablet"));
    public static final StreamCodec<ByteBuf, OpenTabletPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(OpenTabletPayload::new, OpenTabletPayload::modulesPage);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTabletPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        int slot = TabletSlots.find(player);
        if (slot == Integer.MIN_VALUE) {
            player.sendOverlayMessage(Component.translatable("gui.appliedquartermaster.tablet.not_carried"));
            return;
        }
        TabletItem.open(player, slot, payload.modulesPage());
    }
}
