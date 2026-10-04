package io.github.moosasharwaan.appliedquartermaster.client;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.block.AEBaseEntityBlock;
import appeng.core.definitions.AEParts;
import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Development self-test for other AE2 addons. Only does anything when addons are installed (the compatibility
 * test run downloads them into the mods folder): checks which of their items the tablet and storage blocks take,
 * puts their machines on the test network for the Devices tab, and puts one of their terminals in a module slot.
 */
final class SelfTestAddons {

    private static final Set<String> OWN = Set.of("minecraft", "ae2", AppliedQuartermaster.MOD_ID, "guideme",
            "neoforge", "c");

    /** Module slot that gets the addon terminal. */
    static final int MODULE_SLOT = 1;

    private SelfTestAddons() {
    }

    static boolean present() {
        return !namespaces().isEmpty();
    }

    static List<String> namespaces() {
        var set = new java.util.TreeSet<String>();
        for (var key : BuiltInRegistries.ITEM.keySet()) {
            if (!OWN.contains(key.getNamespace())) {
                set.add(key.getNamespace());
            }
        }
        return new ArrayList<>(set);
    }

    private static List<Item> items() {
        var list = new ArrayList<Item>();
        for (var item : BuiltInRegistries.ITEM) {
            if (!OWN.contains(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                list.add(item);
            }
        }
        return list;
    }

    /** Logs what the tablet and storage blocks accept, and builds the addon machines. */
    static void setUp(ServerPlayer player, int tabletSlot, BlockPos origin, BlockPos wap) {
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        AppliedQuartermaster.LOGGER.info("SELFTEST addons loaded: {}", namespaces());

        var terminals = new ArrayList<ItemStack>();
        for (var item : items()) {
            var stack = new ItemStack(item);
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (TabletModules.isModule(stack)) {
                terminals.add(stack);
                AppliedQuartermaster.LOGGER.info("SELFTEST addon module slot accepts {}", id);
            }
            if (TabletModules.isUpgrade(stack)) {
                AppliedQuartermaster.LOGGER.info("SELFTEST addon upgrade slot accepts {} (booster={} infinite={} anyDim={})", id,
                        stack.is(TabletModules.RANGE_BOOSTERS), stack.is(TabletModules.INFINITE_RANGE),
                        stack.is(TabletModules.ANY_DIMENSION));
            }
            for (var kind : StorageKind.values()) {
                if (kind.accepts(stack) && (id.getPath().contains("tool") || id.getPath().contains("wrench")
                        || id.getPath().contains("guide") || id.getPath().contains("book") || kind == StorageKind.ARMORY)) {
                    AppliedQuartermaster.LOGGER.info("SELFTEST addon {} accepts {}", kind.id(), id);
                }
            }
        }

        // An addon terminal in module slot 1, linked to the test access point and charged.
        var tablet = player.getInventory().getItem(tabletSlot);
        if (!terminals.isEmpty()) {
            var terminal = terminals.stream()
                    .filter(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("pattern_access"))
                    .findFirst().orElse(terminals.getFirst()).copy();
            terminal.set(AEComponents.WIRELESS_LINK_TARGET, GlobalPos.of(level.dimension(), wap));
            if (terminal.getItem() instanceof WirelessTerminalItem item) {
                item.injectAEPower(terminal, item.getAEMaxPower(terminal), Actionable.MODULATE);
            }
            TabletModules.set(tablet, MODULE_SLOT, terminal);
            AppliedQuartermaster.LOGGER.info("SELFTEST addon terminal in module slot: {}",
                    BuiltInRegistries.ITEM.getKey(terminal.getItem()));
        }
        // An addon range upgrade in the second upgrade slot, if there is one.
        for (var item : items()) {
            var stack = new ItemStack(item);
            if (stack.is(TabletModules.ANY_DIMENSION) || stack.is(TabletModules.INFINITE_RANGE)) {
                var upgrades = TabletModules.readUpgrades(tablet);
                upgrades.set(1, stack);
                TabletModules.writeUpgrades(tablet, upgrades);
                AppliedQuartermaster.LOGGER.info("SELFTEST addon upgrade installed: {} infinite={} anyDim={}",
                        BuiltInRegistries.ITEM.getKey(item), TabletModules.hasInfiniteRange(tablet),
                        TabletModules.worksAcrossDimensions(tablet));
                break;
            }
        }

        // Addon machines on a cable running east from the controller, one on top of each cable.
        var machines = new ArrayList<Block>();
        var seenMods = new java.util.HashSet<String>();
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (OWN.contains(id.getNamespace()) || !(block instanceof AEBaseEntityBlock<?>)) {
                continue;
            }
            // Multiblock parts (crafting units, quantum computer parts) only join a network once the multiblock
            // is complete, as in AE2 itself, so a lone one would never show.
            var path = id.getPath();
            if (path.contains("crafting") || path.contains("accelerator") || path.contains("quantum")
                    || path.endsWith("_unit") || path.endsWith("_core") || path.contains("structure")) {
                continue;
            }
            // Two machines per addon at most, so every addon gets a turn within the cable's channels.
            if (machines.stream().filter(b -> BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(id.getNamespace())).count() >= 2) {
                continue;
            }
            seenMods.add(id.getNamespace());
            machines.add(block);
        }
        int placed = 0;
        for (var block : machines) {
            if (placed >= 7) {
                break;
            }
            var cable = origin.east(1 + placed);
            try {
                PartHelper.setPart(level, cable, null, player, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
                level.setBlock(cable.above(), block.defaultBlockState(), 3);
                AppliedQuartermaster.LOGGER.info("SELFTEST addon machine placed: {} at {}",
                        BuiltInRegistries.BLOCK.getKey(block), cable.above());
                placed++;
            } catch (RuntimeException e) {
                AppliedQuartermaster.LOGGER.error("SELFTEST addon machine {} failed to place",
                        BuiltInRegistries.BLOCK.getKey(block), e);
                level.removeBlock(cable.above(), false);
            }
        }
        AppliedQuartermaster.LOGGER.info("SELFTEST addon machines: {} of {} candidates from {}", placed, machines.size(), seenMods);
    }
}
