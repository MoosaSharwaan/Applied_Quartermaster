package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.menu.locator.ItemMenuHostLocator;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tells AE2 where to find a terminal that is installed in a tablet module slot, so the real AE2 terminal
 * screen can be opened from the tablet.
 * <p>
 * AE2 changes the terminal stack in place (power use, crafting grid, settings), but stacks inside the
 * tablet are stored as an immutable component. So each player gets one live copy of the open module,
 * and every change to it is written back into the tablet.
 */
public record TabletModuleLocator(int tabletSlot, int moduleSlot) implements ItemMenuHostLocator {

    private static final Map<Player, Live> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class Live {
        final int tabletSlot;
        final int moduleSlot;
        final ItemStack stack;
        ItemStack saved;

        Live(int tabletSlot, int moduleSlot, ItemStack stack) {
            this.tabletSlot = tabletSlot;
            this.moduleSlot = moduleSlot;
            this.stack = stack;
            this.saved = stack.copy();
        }
    }

    @Override
    public ItemStack locateItem(Player player) {
        var tablet = player.getInventory().getItem(tabletSlot);
        if (!tablet.is(ModItems.ME_TABLET.get())) {
            LIVE.remove(player);
            return ItemStack.EMPTY;
        }
        var stored = TabletModules.get(tablet, moduleSlot);
        var live = LIVE.get(player);
        if (live != null && live.tabletSlot == tabletSlot && live.moduleSlot == moduleSlot
                && !stored.isEmpty() && live.stack.is(stored.getItem())) {
            save(player, tablet, live);
            return live.stack;
        }
        if (stored.isEmpty()) {
            LIVE.remove(player);
            return ItemStack.EMPTY;
        }
        live = new Live(tabletSlot, moduleSlot, stored);
        LIVE.put(player, live);
        return live.stack;
    }

    private static void save(Player player, ItemStack tablet, Live live) {
        if (player.level().isClientSide() || ItemStack.matches(live.stack, live.saved)) {
            return;
        }
        live.saved = live.stack.copy();
        TabletModules.set(tablet, live.moduleSlot, live.saved.copy());
    }

    /** Writes any last changes back into the tablet and forgets the live copy (when the terminal closes). */
    public static void flush(Player player) {
        var live = LIVE.remove(player);
        if (live == null) {
            return;
        }
        var tablet = player.getInventory().getItem(live.tabletSlot);
        if (tablet.is(ModItems.ME_TABLET.get())
                && TabletModules.get(tablet, live.moduleSlot).is(live.stack.getItem())) {
            save(player, tablet, live);
        }
    }

    @Override
    public @Nullable BlockHitResult hitResult() {
        return null;
    }

    @Override
    public Integer getPlayerInventorySlot() {
        // AE2 locks this slot while the terminal is open, so the tablet can't be moved away.
        return tabletSlot;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeVarInt(tabletSlot);
        buf.writeVarInt(moduleSlot);
    }

    public static TabletModuleLocator readFromPacket(FriendlyByteBuf buf) {
        return new TabletModuleLocator(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public String toString() {
        return "tablet in slot " + tabletSlot + ", module " + moduleSlot;
    }
}
