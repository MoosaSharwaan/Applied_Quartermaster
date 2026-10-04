package io.github.moosasharwaan.appliedquartermaster.client;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.block.FacingMachineBlock;
import io.github.moosasharwaan.appliedquartermaster.network.ReturnToTabletPayload;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlocks;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageKind;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletItem;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Development self-test, only active with {@code -Dappliedquartermaster.selftest=true} (the {@code runSelftest}
 * Gradle task). It builds a small AE2 network in a singleplayer world, opens every tablet page and saves
 * screenshots, then quits. It does nothing in normal play.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID, value = Dist.CLIENT)
public final class SelfTest {

    private static final boolean ENABLED = Boolean.getBoolean("appliedquartermaster.selftest");

    private record Step(int delay, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int ticks;
    private static int step = -1;
    private static int wait;
    private static int tabletSlot;
    private static BlockPos origin;

    private SelfTest() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        if (step == -1) {
            if (ticks < 60) {
                return;
            }
            plan();
            step = 0;
            wait = 0;
        }
        if (step >= STEPS.size()) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        var current = STEPS.get(step++);
        try {
            current.action().run();
        } catch (RuntimeException e) {
            AppliedQuartermaster.LOGGER.error("SELFTEST step {} failed", step - 1, e);
        }
        wait = step < STEPS.size() ? STEPS.get(step).delay() : 0;
    }

    private static void plan() {
        STEPS.clear();
        STEPS.add(new Step(0, () -> server(SelfTest::build)));
        STEPS.add(new Step(80, () -> {
            var player = Minecraft.getInstance().player;
            player.setYRot(-90f);
            player.setXRot(15f);
        }));
        STEPS.add(new Step(40, () -> shot("01_world")));
        STEPS.add(new Step(5, () -> server(p -> TabletItem.openTablet(p, tabletSlot, TabletMenu.PAGE_MODULES))));
        STEPS.add(new Step(30, () -> shot("02_modules")));
        for (var kind : StorageKind.values()) {
            STEPS.add(new Step(5, () -> server(p -> TabletItem.openTablet(p, tabletSlot, kind.ordinal()))));
            STEPS.add(new Step(30, () -> shot("03_" + kind.id() + "_large")));
        }
        STEPS.add(new Step(5, () -> server(p -> {
            TabletModules.setViewSize(p.getInventory().getItem(tabletSlot), StorageKind.ARMORY, 1);
            TabletItem.openTablet(p, tabletSlot, StorageKind.ARMORY.ordinal());
        })));
        STEPS.add(new Step(30, () -> shot("04_armory_medium")));
        STEPS.add(new Step(5, () -> server(p -> {
            TabletModules.setViewSize(p.getInventory().getItem(tabletSlot), StorageKind.ARMORY, 2);
            TabletItem.openTablet(p, tabletSlot, StorageKind.ARMORY.ordinal());
        })));
        STEPS.add(new Step(30, () -> shot("05_armory_small")));
        STEPS.add(new Step(5, () -> server(p -> {
            TabletModules.setViewSize(p.getInventory().getItem(tabletSlot), StorageKind.ARMORY, 0);
            TabletItem.openTablet(p, tabletSlot, StorageKind.ARMORY.ordinal());
        })));
        STEPS.add(new Step(20, () -> server(SelfTest::interact)));
        STEPS.add(new Step(30, () -> shot("05b_armory_after_take_and_store")));
        STEPS.add(new Step(5, () -> server(p -> TabletItem.openModule(p, tabletSlot, 0))));
        STEPS.add(new Step(40, () -> shot("06_terminal_from_tablet")));
        STEPS.add(new Step(5, () -> ClientPacketDistributor.sendToServer(new ReturnToTabletPayload())));
        STEPS.add(new Step(30, () -> shot("07_back_to_tablet")));
        STEPS.add(new Step(5, () -> server(p -> {
            var be = p.level().getBlockEntity(origin.south());
            if (be instanceof StorageBlockEntity) {
                p.closeContainer();
                io.github.moosasharwaan.appliedquartermaster.block.StorageMachine.open(p.level(), origin.south(), p);
            }
        })));
        STEPS.add(new Step(30, () -> shot("08_library_block_screen")));
        STEPS.add(new Step(5, () -> server(ServerPlayer::closeContainer)));
        STEPS.add(new Step(10, () -> server(SelfTest::report)));
        STEPS.add(new Step(20, () -> Minecraft.getInstance().stop()));
    }

    private static void server(java.util.function.Consumer<ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                try {
                    action.accept(player);
                } catch (RuntimeException e) {
                    AppliedQuartermaster.LOGGER.error("SELFTEST server action failed", e);
                }
            }
        });
    }

    private static void shot(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "aq_" + name + ".png", mc.getMainRenderTarget(), 1,
                message -> AppliedQuartermaster.LOGGER.info("SELFTEST screenshot {}: {}", name, message.getString()));
    }

    private static BlockState facingWest(BlockState state) {
        return state.setValue(FacingMachineBlock.FACING, Direction.WEST);
    }

    /** Controller + creative cell + access point, two libraries, two stacked armories and a tool rack. */
    private static void build(ServerPlayer player) {
        var level = player.level();
        player.setGameMode(GameType.CREATIVE);
        origin = player.blockPosition().east(3);
        level.setBlock(origin, AEBlocks.CONTROLLER.block().defaultBlockState(), 3);
        level.setBlock(origin.north(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(), 3);
        var wap = origin.above();
        level.setBlock(wap, AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState(), 3);
        level.setBlock(origin.south(), facingWest(ModBlocks.ME_LIBRARY.get().defaultBlockState()), 3);
        level.setBlock(origin.south(2), facingWest(ModBlocks.ME_LIBRARY.get().defaultBlockState()), 3);
        level.setBlock(origin.south().above(), facingWest(ModBlocks.ME_ARMORY.get().defaultBlockState()), 3);
        level.setBlock(origin.south(2).above(), facingWest(ModBlocks.ME_ARMORY.get().defaultBlockState()), 3);
        level.setBlock(origin.south(3), facingWest(ModBlocks.ME_TOOL_RACK.get().defaultBlockState()), 3);

        var book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Base notes"),
                "Dev", 0, List.of(Filterable.passThrough(Component.literal("Hello from the ME Library."))), true));
        fill(level.getBlockEntity(origin.south()), book, new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.KNOWLEDGE_BOOK));
        fill(level.getBlockEntity(origin.south().above()), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.NETHERITE_PICKAXE),
                new ItemStack(Items.BOW), new ItemStack(Items.TRIDENT), new ItemStack(Items.MACE), new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.SHIELD));
        fill(level.getBlockEntity(origin.south(3)), new ItemStack(AEItems.NETWORK_TOOL), new ItemStack(AEItems.MEMORY_CARD));

        var terminal = new ItemStack(AEItems.WIRELESS_TERMINAL);
        terminal.set(AEComponents.WIRELESS_LINK_TARGET, GlobalPos.of(level.dimension(), wap));
        if (terminal.getItem() instanceof WirelessTerminalItem item) {
            item.injectAEPower(terminal, item.getAEMaxPower(terminal), Actionable.MODULATE);
        }
        var tablet = new ItemStack(ModItems.ME_TABLET.get());
        TabletModules.set(tablet, 0, terminal);
        tabletSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setItem(tabletSlot, tablet);
        player.getInventory().setItem(9, new ItemStack(Items.ENCHANTED_BOOK));
        player.getInventory().setItem(10, new ItemStack(Items.GOLDEN_SWORD));
        player.getInventory().setItem(11, new ItemStack(Items.WRITTEN_BOOK));
    }

    private static void fill(Object be, ItemStack... stacks) {
        if (be instanceof StorageBlockEntity storage) {
            for (int i = 0; i < stacks.length && i < StorageKind.SLOTS; i++) {
                storage.setItem(i, stacks[i]);
            }
        } else {
            AppliedQuartermaster.LOGGER.error("SELFTEST expected a storage block entity, got {}", be);
        }
    }

    /** Shift-click (take) the first armory item, then store a carried golden sword, as the tablet buttons do. */
    private static void interact(ServerPlayer player) {
        if (!(player.containerMenu instanceof TabletMenu menu)) {
            AppliedQuartermaster.LOGGER.error("SELFTEST tablet menu not open: {}", player.containerMenu);
            return;
        }
        menu.broadcastChanges();
        var first = new ItemStack(Items.DIAMOND_SWORD);
        menu.handleAction(player, new io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload(
                menu.containerId, io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload.TAKE, 0, first, 0));
        AppliedQuartermaster.LOGGER.info("SELFTEST take: inventory has diamond sword={}",
                player.getInventory().contains(new ItemStack(Items.DIAMOND_SWORD)));
        menu.setCarried(new ItemStack(Items.GOLDEN_SWORD));
        menu.handleAction(player, new io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload(
                menu.containerId, io.github.moosasharwaan.appliedquartermaster.network.TabletActionPayload.STORE, -1, ItemStack.EMPTY, 0));
        AppliedQuartermaster.LOGGER.info("SELFTEST store: carried now={}", menu.getCarried());
    }

    private static void report(ServerPlayer player) {
        var level = player.level();
        for (int i = 1; i <= 3; i++) {
            for (var pos : List.of(origin.south(i), origin.south(i).above())) {
                var be = level.getBlockEntity(pos);
                if (be instanceof StorageBlockEntity storage) {
                    AppliedQuartermaster.LOGGER.info("SELFTEST {} at {}: active={} items={} state={}",
                            storage.getKind(), pos, storage.isActive(), storage.count(), level.getBlockState(pos));
                }
            }
        }
        // Store and take through the network directly, the same way the tablet does.
        AppliedQuartermaster.LOGGER.info("SELFTEST library accepts written book={} enchanted book={} sword={}",
                StorageKind.LIBRARY.accepts(new ItemStack(Items.WRITTEN_BOOK)),
                StorageKind.LIBRARY.accepts(new ItemStack(Items.ENCHANTED_BOOK)),
                StorageKind.LIBRARY.accepts(new ItemStack(Items.DIAMOND_SWORD)));
    }
}
