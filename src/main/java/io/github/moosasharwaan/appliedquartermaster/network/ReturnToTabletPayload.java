package io.github.moosasharwaan.appliedquartermaster.network;

import appeng.menu.AEBaseMenu;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletItem;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModuleLocator;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent when Esc is pressed in an AE2 terminal that was opened from the tablet: go back to the tablet. */
public record ReturnToTabletPayload() implements CustomPacketPayload {

    public static final Type<ReturnToTabletPayload> TYPE = new Type<>(AppliedQuartermaster.id("return_to_tablet"));
    public static final StreamCodec<ByteBuf, ReturnToTabletPayload> STREAM_CODEC = StreamCodec.unit(new ReturnToTabletPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReturnToTabletPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof AEBaseMenu menu
                && menu.getLocator() instanceof TabletModuleLocator locator) {
            TabletItem.openTablet(player, locator.tabletSlot());
        }
    }
}
