package io.github.moosasharwaan.appliedquartermaster.automation;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import io.github.moosasharwaan.appliedquartermaster.block.FacingMachineBlock;
import io.github.moosasharwaan.appliedquartermaster.block.StorageMachine;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The ME Farm Controller. It has two separate AE2 nodes:
 * <ul>
 * <li>the <b>network port</b> (cyan, back and left side seen from the front): joins your main network and uses
 * one channel; this is how the tablet finds the farm;</li>
 * <li>the <b>farm port</b> (red, right side): the farm's own cables plug in here, and every ME Redstone Plate on
 * those cables belongs to this farm.</li>
 * </ul>
 */
public class FarmControllerBlockEntity extends BlockEntity implements IInWorldGridNodeHost {

    /** Owner of the farm-port node, so the farm grid and the main grid see different machine classes. */
    public static final class FarmPort {
        private final FarmControllerBlockEntity controller;

        FarmPort(FarmControllerBlockEntity controller) {
            this.controller = controller;
        }

        public FarmControllerBlockEntity controller() {
            return controller;
        }
    }

    private static final IGridNodeListener<FarmControllerBlockEntity> NETWORK_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(FarmControllerBlockEntity owner, IGridNode node) {
            owner.setChanged();
        }

        @Override
        public void onStateChanged(FarmControllerBlockEntity owner, IGridNode node, State state) {
            owner.updateBlockState();
        }
    };

    private static final IGridNodeListener<FarmPort> FARM_LISTENER = (owner, node) -> owner.controller.setChanged();

    private final IManagedGridNode networkNode;
    private final IManagedGridNode farmNode;
    private final FarmPort farmPort = new FarmPort(this);
    private @Nullable String name;
    private ItemStack icon = ItemStack.EMPTY;
    private boolean unloading;

    public FarmControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARM_CONTROLLER.get(), pos, state);
        this.networkNode = GridHelper.createManagedNode(this, NETWORK_LISTENER)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(1.0)
                .setInWorldNode(true)
                .setVisualRepresentation(state.getBlock().asItem())
                .setTagName("network");
        this.farmNode = GridHelper.createManagedNode(farmPort, FARM_LISTENER)
                .setFlags()
                .setIdlePowerUsage(0)
                .setInWorldNode(true)
                .setVisualRepresentation(state.getBlock().asItem())
                .setTagName("farm");
        updateExposedSides(state);
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(FacingMachineBlock.FACING) ? state.getValue(FacingMachineBlock.FACING) : Direction.NORTH;
    }

    /** Side the red farm port is on (right of the front, seen from the front). */
    public static Direction farmSide(BlockState state) {
        return facing(state).getClockWise();
    }

    private void updateExposedSides(BlockState state) {
        var facing = facing(state);
        networkNode.setExposedOnSides(EnumSet.of(facing.getOpposite(), facing.getCounterClockWise()));
        farmNode.setExposedOnSides(EnumSet.of(facing.getClockWise()));
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        updateExposedSides(state);
    }

    // ---------------------------------------------------------------- state

    /** Online = powered with a channel on the main network. */
    public boolean isOnline() {
        return networkNode.isActive();
    }

    public @Nullable IGrid getFarmGrid() {
        return farmNode.getGrid();
    }

    /** The plates on this farm's cables, in a stable order. */
    public List<RedstonePlatePart> getPlates() {
        var grid = getFarmGrid();
        if (grid == null) {
            return List.of();
        }
        var list = new ArrayList<>(grid.getMachines(RedstonePlatePart.class));
        list.sort(Comparator.comparingLong((RedstonePlatePart p) -> p.getBlockEntity().getBlockPos().asLong())
                .thenComparing(p -> p.getSide() == null ? -1 : p.getSide().ordinal()));
        return list;
    }

    public Component getDisplayName() {
        if (name != null && !name.isBlank()) {
            return Component.literal(name);
        }
        return Component.translatable("gui.appliedquartermaster.farm.default_name",
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    public void setName(@Nullable String name) {
        this.name = name == null || name.isBlank() ? null : name.substring(0, Math.min(40, name.length()));
        setChanged();
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon.isEmpty() ? ItemStack.EMPTY : icon.copyWithCount(1);
        setChanged();
    }

    public void setAll(boolean on) {
        for (var plate : getPlates()) {
            plate.setOn(on);
        }
    }

    public IManagedGridNode getNetworkNode() {
        return networkNode;
    }

    private void updateBlockState() {
        if (level == null || level.isClientSide() || isRemoved() || unloading || !level.isLoaded(worldPosition)) {
            return;
        }
        var state = level.getBlockState(worldPosition);
        if (state.is(getBlockState().getBlock()) && state.hasProperty(StorageMachine.POWERED)
                && state.getValue(StorageMachine.POWERED) != isOnline()) {
            level.setBlock(worldPosition, state.setValue(StorageMachine.POWERED, isOnline()), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FarmControllerBlockEntity be) {
        if (level.getGameTime() % 20 == 0) {
            be.updateBlockState();
        }
    }

    // ---------------------------------------------------------------- AE2 nodes

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return dir == farmSide(getBlockState()) ? farmNode.getNode() : networkNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        unloading = false;
        GridHelper.onFirstTick(this, be -> {
            be.updateExposedSides(be.getBlockState());
            be.networkNode.create(be.getLevel(), be.getBlockPos());
            be.farmNode.create(be.getLevel(), be.getBlockPos());
            be.updateBlockState();
        });
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        networkNode.destroy();
        farmNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloading = true;
        networkNode.destroy();
        farmNode.destroy();
    }

    // ---------------------------------------------------------------- save

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        networkNode.deserialize(input.childOrEmpty("network"));
        farmNode.deserialize(input.childOrEmpty("farm"));
        name = input.getString("farmName").orElse(null);
        icon = input.read("icon", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        networkNode.serialize(output.child("network"));
        farmNode.serialize(output.child("farm"));
        if (name != null) {
            output.putString("farmName", name);
        }
        if (!icon.isEmpty()) {
            output.store("icon", ItemStack.OPTIONAL_CODEC, icon);
        }
    }
}
