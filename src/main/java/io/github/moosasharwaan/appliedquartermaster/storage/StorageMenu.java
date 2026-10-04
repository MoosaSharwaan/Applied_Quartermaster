package io.github.moosasharwaan.appliedquartermaster.storage;

import io.github.moosasharwaan.appliedquartermaster.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Right-clicking a Library, Armory or Tool Rack: its 8 slots and the player inventory. */
public class StorageMenu extends AbstractContainerMenu {

    public static final int SLOT_Y = 20;

    private final @Nullable StorageBlockEntity blockEntity;
    private final StorageKind kind;

    /** Server side. */
    public StorageMenu(int id, Inventory inventory, StorageBlockEntity be) {
        this(id, inventory, be, be.getKind());
    }

    /** Client side. */
    public static StorageMenu fromNetwork(int id, Inventory inventory, BlockPos pos) {
        var be = inventory.player.level().getBlockEntity(pos) instanceof StorageBlockEntity s ? s : null;
        // The client only needs the kind (for slot rules); slot contents come from the server.
        return new StorageMenu(id, inventory, null, be != null ? be.getKind() : StorageKind.TOOLS);
    }

    private StorageMenu(int id, Inventory inventory, @Nullable StorageBlockEntity be, StorageKind kind) {
        super(ModMenus.STORAGE.get(), id);
        this.blockEntity = be;
        this.kind = kind;
        var container = new SimpleContainer(StorageKind.SLOTS) {
            @Override
            public ItemStack getItem(int slot) {
                return be != null ? be.getItem(slot) : super.getItem(slot);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                if (be != null) {
                    be.setItem(slot, stack);
                } else {
                    super.setItem(slot, stack);
                }
            }

            @Override
            public ItemStack removeItem(int slot, int count) {
                if (be == null) {
                    return super.removeItem(slot, count);
                }
                var stack = be.getItem(slot);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                var taken = stack.split(count);
                be.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
                return taken;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                if (be == null) {
                    return super.removeItemNoUpdate(slot);
                }
                var stack = be.getItem(slot);
                be.setItem(slot, ItemStack.EMPTY);
                return stack;
            }

            @Override
            public boolean stillValid(Player player) {
                return be == null || be.stillValid(player);
            }
        };
        for (int i = 0; i < StorageKind.SLOTS; i++) {
            addSlot(new Slot(container, i, 17 + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return kind.accepts(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 51);
    }

    public StorageKind getKind() {
        return kind;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var original = stack.copy();
        if (index < StorageKind.SLOTS) {
            if (!moveItemStackTo(stack, StorageKind.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!kind.accepts(stack) || !moveItemStackTo(stack, 0, StorageKind.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || blockEntity.stillValid(player);
    }
}
