package io.github.moosasharwaan.appliedquartermaster.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.BiFunction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A facing machine block that merges with identical neighbours (same block, same facing):
 * the casing frame is left out on every edge that touches one, so stacks read as one cabinet.
 * LEFT and RIGHT are as seen when looking at the block's front.
 */
public class ConnectedMachineBlock extends FacingMachineBlock implements EntityBlock, StorageMachine {

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory;

    public ConnectedMachineBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
        this.registerDefaultState(this.defaultBlockState()
                .setValue(UP, false).setValue(DOWN, false).setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(POWERED, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.apply(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return StorageMachine.open(level, pos, player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        StorageMachine.placedBy(level, pos, by);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UP, DOWN, LEFT, RIGHT, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        return state == null ? null : this.connections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull LevelReader level, @NotNull ScheduledTickAccess scheduledTickAccess,
                                              @NotNull BlockPos pos, @NotNull Direction direction, @NotNull BlockPos neighborPos,
                                              @NotNull BlockState neighborState, @NotNull RandomSource random) {
        return this.connections(state, level, pos);
    }

    private BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        var facing = state.getValue(FACING);
        return state
                .setValue(UP, this.sameAs(level, pos.above(), facing))
                .setValue(DOWN, this.sameAs(level, pos.below(), facing))
                .setValue(LEFT, this.sameAs(level, pos.relative(facing.getClockWise()), facing))
                .setValue(RIGHT, this.sameAs(level, pos.relative(facing.getCounterClockWise()), facing));
    }

    private boolean sameAs(LevelReader level, BlockPos other, Direction facing) {
        var otherState = level.getBlockState(other);
        return otherState.is(this) && otherState.getValue(FACING) == facing;
    }
}
