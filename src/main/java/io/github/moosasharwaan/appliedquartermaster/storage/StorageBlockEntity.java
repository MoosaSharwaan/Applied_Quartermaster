package io.github.moosasharwaan.appliedquartermaster.storage;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import io.github.moosasharwaan.appliedquartermaster.block.LibraryBlock;
import io.github.moosasharwaan.appliedquartermaster.block.StorageMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.ContainerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * An ME Library, ME Armory or ME Tool Rack: 8 item slots and an AE2 grid node that needs one channel.
 * The node is exposed on every side, so touching blocks pass the network (and up to 8 channels) along.
 */
public abstract class StorageBlockEntity extends BlockEntity implements IInWorldGridNodeHost {

    private static final IGridNodeListener<StorageBlockEntity> LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(StorageBlockEntity owner, IGridNode node) {
            owner.setChanged();
        }

        @Override
        public void onStateChanged(StorageBlockEntity owner, IGridNode node, State state) {
            owner.updateBlockState();
        }

        @Override
        public void onGridChanged(StorageBlockEntity owner, IGridNode node) {
            owner.updateBlockState();
        }
    };

    private final StorageKind kind;
    private final NonNullList<ItemStack> items = NonNullList.withSize(StorageKind.SLOTS, ItemStack.EMPTY);
    private final IManagedGridNode mainNode;

    protected StorageBlockEntity(BlockEntityType<?> type, StorageKind kind, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.kind = kind;
        this.mainNode = GridHelper.createManagedNode(this, LISTENER)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(1.0)
                .setInWorldNode(true)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setVisualRepresentation(state.getBlock().asItem())
                .setTagName("proxy");
    }

    public StorageKind getKind() {
        return kind;
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    public boolean isActive() {
        return mainNode.isActive();
    }

    // ------------------------------------------------------------ items

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    public boolean canHold(ItemStack stack) {
        return kind.accepts(stack);
    }

    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        onContentsChanged();
    }

    public int firstFreeSlot() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int count() {
        int n = 0;
        for (var stack : items) {
            if (!stack.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public void onContentsChanged() {
        setChanged();
        updateBlockState();
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.isWithinBlockInteractionRange(worldPosition, 4.0);
    }

    /** Lights on with power and a channel; library shelves show how many books are inside. */
    protected void updateBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        var state = getBlockState();
        var updated = state;
        if (updated.hasProperty(StorageMachine.POWERED)) {
            updated = updated.setValue(StorageMachine.POWERED, mainNode.isActive());
        }
        if (updated.hasProperty(LibraryBlock.BOOKS)) {
            updated = updated.setValue(LibraryBlock.BOOKS, count());
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, 3);
        }
    }

    // ------------------------------------------------------------ AE2 node

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, be -> {
            be.mainNode.create(be.getLevel(), be.getBlockPos());
            be.updateBlockState();
        });
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        mainNode.destroy();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (var stack : items) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            items.clear();
        }
    }

    // ------------------------------------------------------------ save

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        mainNode.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        mainNode.serialize(output);
    }
}
