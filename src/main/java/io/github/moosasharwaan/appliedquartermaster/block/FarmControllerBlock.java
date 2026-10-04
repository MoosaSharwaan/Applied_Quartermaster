package io.github.moosasharwaan.appliedquartermaster.block;

import io.github.moosasharwaan.appliedquartermaster.automation.FarmControllerBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The ME Farm Controller block. Right-click with an empty hand to see the farm's status; sneak-right-click to clear
 * its icon. Name it in an anvil or from the tablet; set its icon from the tablet.
 */
public class FarmControllerBlock extends FacingMachineBlock implements EntityBlock {

    public FarmControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(StorageMachine.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(StorageMachine.POWERED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FarmControllerBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.FARM_CONTROLLER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<FarmControllerBlockEntity>) FarmControllerBlockEntity::serverTick;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FarmControllerBlockEntity be)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            be.setIcon(ItemStack.EMPTY);
            player.sendOverlayMessage(Component.translatable("gui.appliedquartermaster.farm.icon_cleared"));
        } else {
            var plates = be.getPlates();
            long on = plates.stream().filter(p -> p.isOn()).count();
            player.sendOverlayMessage(Component.translatable(
                    be.isOnline() ? "gui.appliedquartermaster.farm.status" : "gui.appliedquartermaster.farm.status_offline",
                    be.getDisplayName(), on, plates.size()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FarmControllerBlockEntity be) {
            if (by instanceof Player player) {
                be.getNetworkNode().setOwningPlayer(player);
            }
            var name = stack.get(DataComponents.CUSTOM_NAME);
            if (name != null) {
                be.setName(name.getString());
            }
        }
    }
}
