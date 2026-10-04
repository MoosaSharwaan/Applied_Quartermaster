package io.github.moosasharwaan.appliedquartermaster.block;

import io.github.moosasharwaan.appliedquartermaster.storage.StorageBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Shared behaviour of the ME Library, ME Armory and ME Tool Rack blocks. */
public interface StorageMachine {

    /** True while the block has power and a channel; the lights glow only then. */
    BooleanProperty POWERED = BooleanProperty.create("powered");

    static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof StorageBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inventory, p) -> new StorageMenu(id, inventory, be),
                            level.getBlockState(pos).getBlock().getName()),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    static void placedBy(Level level, BlockPos pos, LivingEntity by) {
        if (!level.isClientSide() && by instanceof Player player
                && level.getBlockEntity(pos) instanceof StorageBlockEntity be) {
            be.getMainNode().setOwningPlayer(player);
        }
    }
}
