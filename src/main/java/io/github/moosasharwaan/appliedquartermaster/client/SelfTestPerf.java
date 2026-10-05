package io.github.moosasharwaan.appliedquartermaster.client;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlocks;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletItem;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModules;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Development benchmark ({@code -Dappliedquartermaster.perf=true}): builds small and huge networks out of different
 * AE2 and addon blocks, measures the server's tick time with each, then measures the open tablet on a huge network.
 * Results go to the log as "PERF" lines.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID)
public final class SelfTestPerf {

    static final boolean ENABLED = Boolean.getBoolean("appliedquartermaster.perf");

    private static final int MEASURE_TICKS = 300;

    private record Phase(String name, int ticks, Runnable start, boolean measure) {
    }

    private static final List<Phase> PHASES = new ArrayList<>();
    private static int phase = -1;
    private static int phaseTick;
    private static long tickStart;
    private static final long[] samples = new long[MEASURE_TICKS];
    private static int sampleCount;
    private static boolean running;
    static volatile boolean done;

    private static UUID playerId;
    private static ServerLevel level;
    private static BlockPos base;
    private static int tabletSlot;
    private static final List<BlockPos> placed = new ArrayList<>();
    private static final List<String> results = new ArrayList<>();
    private static double emptyMs;

    private SelfTestPerf() {
    }

    /** Called on the server thread once the test world is loaded. */
    static void start(ServerPlayer player, int slot) {
        playerId = player.getUUID();
        level = (ServerLevel) player.level();
        tabletSlot = slot;
        base = player.blockPosition().offset(-24, 0, -24);
        try {
            appeng.core.AEConfig.instance().setChannelModel(appeng.api.networking.pathing.ChannelMode.INFINITE);
            AppliedQuartermaster.LOGGER.info("PERF channel mode set to infinite");
        } catch (RuntimeException | LinkageError e) {
            AppliedQuartermaster.LOGGER.error("PERF could not set infinite channels", e);
        }
        plan();
        phase = 0;
        phaseTick = 0;
        running = true;
        PHASES.get(0).start().run();
    }

    private static void plan() {
        PHASES.clear();
        PHASES.add(new Phase("settle", 100, () -> { }, false));
        PHASES.add(new Phase("warm-up (ignore)", MEASURE_TICKS, () -> { }, true));
        PHASES.add(new Phase("empty world", MEASURE_TICKS, () -> { }, true));

        var kinds = new ArrayList<Block>();
        kinds.add(AEBlocks.INTERFACE.block());
        kinds.add(AEBlocks.PATTERN_PROVIDER.block());
        var aae = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("advanced_ae", "adv_pattern_provider"));
        aae.ifPresent(kinds::add);
        kinds.add(ModBlocks.ME_LIBRARY.get());
        kinds.add(ModBlocks.ME_ARMORY.get());
        kinds.add(ModBlocks.ME_TOOL_RACK.get());
        for (int size : new int[] {4, 16}) {
            for (var block : kinds) {
                String name = block.getName().getString() + " x" + (size * size * size);
                PHASES.add(new Phase("build " + name, 1, () -> cube(block, size), false));
                PHASES.add(new Phase("boot " + name, 200, () -> { }, false));
                PHASES.add(new Phase(name, MEASURE_TICKS, () -> { }, true));
                PHASES.add(new Phase("clear", 60, SelfTestPerf::clear, false));
            }
        }
        PHASES.add(new Phase("build 50 farms", 1, () -> farms(50), false));
        PHASES.add(new Phase("boot farms", 200, () -> { }, false));
        PHASES.add(new Phase("50 farms, 300 Redstone Plates", MEASURE_TICKS, () -> { }, true));
        PHASES.add(new Phase("clear", 60, SelfTestPerf::clear, false));

        // Tablet on a huge mixed network: 2048 interfaces + 2048 ME Libraries, linked to the tablet.
        PHASES.add(new Phase("build mixed", 1, () -> mixed(16), false));
        PHASES.add(new Phase("boot mixed", 300, () -> { }, false));
        PHASES.add(new Phase("Mixed x4096, tablet closed", MEASURE_TICKS, SelfTestPerf::closeTablet, true));
        PHASES.add(new Phase("Mixed x4096, tablet open: Modules", MEASURE_TICKS, () -> openTablet(TabletMenu.PAGE_MODULES), true));
        PHASES.add(new Phase("Mixed x4096, tablet open: Library tab", MEASURE_TICKS, () -> openTablet(0), true));
        PHASES.add(new Phase("Mixed x4096, tablet open: Devices tab", MEASURE_TICKS, () -> openTablet(TabletMenu.PAGE_DEVICES), true));
        PHASES.add(new Phase("Mixed x4096, tablet open: Devices list of 2048", MEASURE_TICKS, SelfTestPerf::openDeviceList, true));
        PHASES.add(new Phase("close", 20, SelfTestPerf::closeTablet, false));
        PHASES.add(new Phase("clear", 60, SelfTestPerf::clear, false));
        PHASES.add(new Phase("empty world again", MEASURE_TICKS, () -> { }, true));
    }

    // ------------------------------------------------------------------ building

    private static void set(BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
        placed.add(pos);
    }

    /** Controller + creative cell + access point, then a cube of the block on top of them. */
    private static BlockPos core() {
        set(base, AEBlocks.CONTROLLER.block().defaultBlockState());
        set(base.below(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        // The access point only connects from its back (below it), so it sits on an interface joined to the cell.
        set(base.west().below(), AEBlocks.INTERFACE.block().defaultBlockState());
        set(base.west(), AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState());
        return base.above();
    }

    private static void cube(Block block, int size) {
        var start = core();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    set(start.offset(x, y, z), block.defaultBlockState());
                }
            }
        }
    }

    private static void mixed(int size) {
        var start = core();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    var block = (x + y + z) % 2 == 0 ? AEBlocks.INTERFACE.block() : ModBlocks.ME_LIBRARY.get();
                    set(start.offset(x, y, z), block.defaultBlockState());
                }
            }
        }
        // An access point just north of the cube: placed facing north, it connects through its back (south) side.
        var wap = start.offset(0, 0, -1);
        set(wap, AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState());
        // Link the tablet's terminal to this network's access point.
        var player = level.getServer().getPlayerList().getPlayer(playerId);
        var tablet = player.getInventory().getItem(tabletSlot);
        var terminal = new ItemStack(AEItems.WIRELESS_TERMINAL);
        terminal.set(AEComponents.WIRELESS_LINK_TARGET, GlobalPos.of(level.dimension(), wap));
        if (terminal.getItem() instanceof WirelessTerminalItem item) {
            item.injectAEPower(terminal, item.getAEMaxPower(terminal), Actionable.MODULATE);
        }
        if (tablet.is(ModItems.ME_TABLET.get())) {
            TabletModules.set(tablet, 0, terminal);
        }
        player.connection.teleport(wap.getX() + 0.5, wap.getY(), wap.getZ() - 3 + 0.5, 180, 10);
    }

    /** Farms in a row: each a Farm Controller next to the main cable, its farm cable and 6 plates around it. */
    private static void farms(int count) {
        var start = core();
        var player = level.getServer().getPlayerList().getPlayer(playerId);
        for (int i = 0; i < count; i++) {
            var pos = start.offset(0, 0, i);
            // Main network: a line of interfaces west of the controllers (their network side) keeps them connected.
            set(pos, AEBlocks.INTERFACE.block().defaultBlockState());
            var controller = pos.east();
            set(controller, ModBlocks.ME_FARM_CONTROLLER.get().defaultBlockState()
                    .setValue(io.github.moosasharwaan.appliedquartermaster.block.FacingMachineBlock.FACING, Direction.NORTH));
            var cable = controller.east();
            PartHelper.setPart(level, cable, null, player, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
            placed.add(cable);
            for (var side : new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST}) {
                PartHelper.setPart(level, cable, side, player, ModItems.ME_REDSTONE_PLATE.get());
            }
        }
    }

    private static void clear() {
        for (int i = placed.size() - 1; i >= 0; i--) {
            level.setBlock(placed.get(i), Blocks.AIR.defaultBlockState(), 3);
        }
        placed.clear();
    }

    // ------------------------------------------------------------------ tablet

    private static ServerPlayer player() {
        return level.getServer().getPlayerList().getPlayer(playerId);
    }

    private static void openTablet(int page) {
        TabletMenu.resetPerf();
        var p = player();
        var network = io.github.moosasharwaan.appliedquartermaster.tablet.TabletNetwork.find(p, p.getInventory().getItem(tabletSlot));
        int devices = network.grid() == null ? -1
                : io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.devices(network.grid()).size();
        AppliedQuartermaster.LOGGER.info("PERF tablet reaches network: status={} devices={}", network.status(), devices);
        if (network.grid() != null && page == TabletMenu.PAGE_DEVICES) {
            var grid = network.grid();
            for (int round = 0; round < 3; round++) {
                long t0 = System.nanoTime();
                int n = 0;
                for (var node : grid.getNodes()) {
                    n++;
                }
                long t1 = System.nanoTime();
                for (var node : grid.getNodes()) {
                    node.getVisualRepresentation();
                }
                long t2 = System.nanoTime();
                for (var node : grid.getNodes()) {
                    io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.position(node);
                }
                long t3 = System.nanoTime();
                for (var node : grid.getNodes()) {
                    node.getUsedChannels();
                    node.getIdlePowerUsage();
                    node.isActive();
                }
                long t4 = System.nanoTime();
                io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.scan(grid);
                long t4b = System.nanoTime();
                var all = io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.devices(grid);
                long t5 = System.nanoTime();
                var kinds = io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.kinds(all, new ArrayList<>());
                long t6 = System.nanoTime();
                io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.summary(grid, all.size());
                long t7 = System.nanoTime();
                AppliedQuartermaster.LOGGER.info("PERF breakdown {} nodes: iterate {} us, visual {} us, position {} us, "
                        + "state {} us, full scan {} us, cached devices() {} us, kinds() {} us, summary {} us", n, (t1 - t0) / 1000,
                        (t2 - t1) / 1000, (t3 - t2) / 1000, (t4 - t3) / 1000, (t4b - t4) / 1000, (t5 - t4b) / 1000,
                        (t6 - t5) / 1000, (t7 - t6) / 1000);
            }
        }
        if (devices < 0) {
            for (var pos : placed) {
                if (level.getBlockEntity(pos) instanceof appeng.blockentity.networking.WirelessAccessPointBlockEntity ap) {
                    AppliedQuartermaster.LOGGER.info("PERF access point at {}: active={} range={} player at {} state={}",
                            pos, ap.isActive(), ap.getRange(), p.blockPosition(), level.getBlockState(pos));
                }
            }
        }
        TabletItem.openTablet(p, tabletSlot, page);
    }

    private static void openDeviceList() {
        TabletMenu.resetPerf();
        var p = player();
        if (p.containerMenu instanceof TabletMenu menu) {
            menu.broadcastChanges();
            // The first kind is the ME Interface (sorted by name, after "Creative Energy Cell"); open whichever has most.
            var grid = io.github.moosasharwaan.appliedquartermaster.tablet.TabletNetwork.find(p, menu.getTablet()).grid();
            if (grid != null) {
                var kinds = new ArrayList<appeng.api.stacks.AEItemKey>();
                var entries = io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.kinds(
                        io.github.moosasharwaan.appliedquartermaster.devices.DeviceScanner.devices(grid), kinds);
                int best = 0;
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).count() > entries.get(best).count()) {
                        best = i;
                    }
                }
                menu.handleAction(p, new io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload(
                        menu.containerId, io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload.DEVICE_OPEN,
                        best, ItemStack.EMPTY, 0));
            }
        }
    }

    private static void closeTablet() {
        TabletMenu.resetPerf();
        var p = player();
        if (p != null) {
            p.closeContainer();
        }
    }

    // ------------------------------------------------------------------ measuring

    @SubscribeEvent
    public static void onTickStart(ServerTickEvent.Pre event) {
        if (running) {
            tickStart = System.nanoTime();
        }
    }

    @SubscribeEvent
    public static void onTickEnd(ServerTickEvent.Post event) {
        if (!running) {
            return;
        }
        var current = PHASES.get(phase);
        if (current.measure() && sampleCount < samples.length) {
            samples[sampleCount++] = System.nanoTime() - tickStart;
        }
        if (++phaseTick < current.ticks()) {
            return;
        }
        if (current.measure()) {
            report(current.name());
        }
        sampleCount = 0;
        phaseTick = 0;
        if (++phase >= PHASES.size()) {
            running = false;
            AppliedQuartermaster.LOGGER.info("PERF ===== results (server tick time; Minecraft's budget is 50 ms) =====");
            for (var line : results) {
                AppliedQuartermaster.LOGGER.info("PERF {}", line);
            }
            done = true;
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
            return;
        }
        try {
            PHASES.get(phase).start().run();
        } catch (RuntimeException e) {
            AppliedQuartermaster.LOGGER.error("PERF phase {} failed", PHASES.get(phase).name(), e);
        }
    }

    /** The test network's grid, read from the energy cell under the controller. */
    private static appeng.api.networking.IGrid grid() {
        for (var pos : List.of(base.below(), base, base.above())) {
            var node = appeng.api.networking.GridHelper.getNodeHost(level, pos);
            if (node == null) {
                continue;
            }
            for (var dir : Direction.values()) {
                var n = node.getGridNode(dir);
                if (n != null && n.getGrid() != null) {
                    return n.getGrid();
                }
            }
        }
        return null;
    }

    private static void report(String name) {
        var sorted = Arrays.copyOf(samples, sampleCount);
        Arrays.sort(sorted);
        double mean = Arrays.stream(sorted).average().orElse(0) / 1e6;
        double median = sorted[sorted.length / 2] / 1e6;
        double p95 = sorted[(int) (sorted.length * 0.95)] / 1e6;
        if (name.equals("empty world")) {
            emptyMs = median;
        }
        int nodes = 0;
        var grid = grid();
        if (grid != null) {
            for (var ignored : grid.getNodes()) {
                nodes++;
            }
        }
        String tablet = "";
        if (TabletMenu.perfCalls() > 0) {
            tablet = String.format(" | tablet refresh: %d x, avg %.3f ms, max %.3f ms",
                    TabletMenu.perfCalls(), TabletMenu.perfNanos() / 1e6 / TabletMenu.perfCalls(), TabletMenu.perfMaxNanos() / 1e6);
        }
        results.add(String.format("%-48s median %6.3f ms (+%6.3f vs empty)  mean %6.3f  p95 %6.3f  nodes %5d%s",
                name, median, median - emptyMs, mean, p95, nodes, tablet));
        AppliedQuartermaster.LOGGER.info("PERF {}", results.get(results.size() - 1));
    }
}
