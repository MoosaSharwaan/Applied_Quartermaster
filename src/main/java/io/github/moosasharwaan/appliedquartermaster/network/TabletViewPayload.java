package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server to client: what the open tablet page shows. {@code unlockMask} has a bit per storage kind whose block
 * is on the linked network; {@code items} are the stored items of the open storage page, {@code free} the number
 * of empty spots in active blocks.
 */
public record TabletViewPayload(int containerId, int page, int unlockMask, int status, List<ItemStack> items, int free)
        implements CustomPacketPayload {

    public static final Type<TabletViewPayload> TYPE = new Type<>(AppliedQuartermaster.id("tablet_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabletViewPayload> STREAM_CODEC = StreamCodec.ofMember(
            TabletViewPayload::write, TabletViewPayload::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEMS =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(page + 1);
        buf.writeVarInt(unlockMask);
        buf.writeVarInt(status);
        ITEMS.encode(buf, items);
        buf.writeVarInt(free);
    }

    private static TabletViewPayload read(RegistryFriendlyByteBuf buf) {
        return new TabletViewPayload(buf.readVarInt(), buf.readVarInt() - 1, buf.readVarInt(), buf.readVarInt(),
                ITEMS.decode(buf), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
