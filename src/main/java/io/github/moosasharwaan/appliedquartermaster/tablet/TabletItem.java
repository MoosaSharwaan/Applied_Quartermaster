package io.github.moosasharwaan.appliedquartermaster.tablet;

import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The ME Tablet. Right-click opens the pinned tab (for a terminal module, the real AE2 terminal; for a
 * storage tab, that page of the tablet); with nothing pinned, or while sneaking, it opens the Modules page.
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
        int slot = findSlot(player, hand);
        if (slot < 0 || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        open(serverPlayer, slot, player.isShiftKeyDown());
        return InteractionResult.SUCCESS;
    }

    /**
     * Opens the tablet in the given slot (inventory or Curios, see {@link TabletSlots}) the way right-click does:
     * the pinned tab, or the Modules page when nothing is pinned or {@code modulesPage} is set.
     */
    public static void open(ServerPlayer player, int slot, boolean modulesPage) {
        var stack = TabletSlots.tablet(player, slot);
        if (stack.isEmpty()) {
            return;
        }
        int pinned = TabletModules.getDefault(stack);
        if (!modulesPage) {
            if (pinned >= 0 && pinned < TabletModules.SLOTS && openModule(player, slot, pinned)) {
                return;
            }
            if (pinned >= TabletModules.PIN_STORAGE) {
                openTablet(player, slot, pinned - TabletModules.PIN_STORAGE);
                return;
            }
        }
        openTablet(player, slot, TabletMenu.PAGE_MODULES);
    }

    /** Opens the tablet screen for the tablet in the given slot (inventory or Curios). */
    public static void openTablet(ServerPlayer player, int tabletSlot, int page) {
        TabletModuleLocator.flush(player);
        player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new TabletMenu(id, inventory, tabletSlot, page),
                        Component.translatable("item.appliedquartermaster.me_tablet")),
                buf -> {
                    buf.writeVarInt(tabletSlot);
                    buf.writeVarInt(page + 1);
                    buf.writeVarInt(TabletSlots.get(player, tabletSlot)
                            .getOrDefault(ModComponents.TABLET_VIEW_SIZES, 0));
                });
    }

    /** Opens the module in the given slot. Terminal modules open the real AE2 terminal screen. */
    public static boolean openModule(ServerPlayer player, int tabletSlot, int moduleSlot) {
        var tablet = TabletSlots.tablet(player, tabletSlot);
        var module = TabletModules.get(tablet, moduleSlot);
        if (module.getItem() instanceof WirelessTerminalItem terminal) {
            TabletModuleLocator.flush(player);
            var locator = new TabletModuleLocator(tabletSlot, moduleSlot);
            // Terminals built on AE2WTLib (its own, AdvancedAE's and others) pick their menu themselves.
            if (io.github.moosasharwaan.appliedquartermaster.integration.ae2wtlib.WtlibCompat.isLoaded()) {
                var opened = io.github.moosasharwaan.appliedquartermaster.integration.ae2wtlib.WtlibCompat
                        .tryOpen(terminal, player, locator);
                if (opened != null) {
                    return opened;
                }
            }
            return terminal.openFromInventory(player, locator);
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
