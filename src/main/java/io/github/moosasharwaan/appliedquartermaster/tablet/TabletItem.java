package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The ME Tablet. Right-click opens the pinned tab (for a terminal module, the real AE2 terminal);
 * with nothing pinned, or while sneaking, it opens the tablet screen.
 */
public class TabletItem extends Item {

    public TabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        var stack = player.getItemInHand(hand);
        int slot = findSlot(player, hand);
        if (slot < 0 || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        int pinned = TabletModules.getDefault(stack);
        if (pinned >= 0 && !player.isShiftKeyDown() && openModule(serverPlayer, slot, pinned)) {
            return InteractionResult.SUCCESS;
        }
        openTablet(serverPlayer, slot);
        return InteractionResult.SUCCESS;
    }

    /** Opens the tablet screen for the tablet in the given player inventory slot. */
    public static void openTablet(ServerPlayer player, int tabletSlot) {
        TabletModuleLocator.flush(player);
        player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new TabletMenu(id, inventory, tabletSlot),
                        Component.translatable("item.appliedquartermaster.me_tablet")),
                buf -> buf.writeVarInt(tabletSlot));
    }

    /** Opens the module in the given slot. Terminal modules open the real AE2 terminal screen. */
    public static boolean openModule(ServerPlayer player, int tabletSlot, int moduleSlot) {
        var tablet = player.getInventory().getItem(tabletSlot);
        var module = TabletModules.get(tablet, moduleSlot);
        if (module.getItem() instanceof WirelessTerminalItem terminal) {
            TabletModuleLocator.flush(player);
            return terminal.openFromInventory(player, new TabletModuleLocator(tabletSlot, moduleSlot));
        }
        return false;
    }

    private static int findSlot(Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == held) {
                return i;
            }
        }
        return -1;
    }
}
