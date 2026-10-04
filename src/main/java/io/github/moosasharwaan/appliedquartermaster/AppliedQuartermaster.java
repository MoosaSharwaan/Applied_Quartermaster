package io.github.moosasharwaan.appliedquartermaster;

import com.mojang.logging.LogUtils;
import appeng.api.AECapabilities;
import appeng.menu.locator.MenuLocators;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlockEntities;
import java.util.List;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import io.github.moosasharwaan.appliedquartermaster.network.ModNetwork;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlocks;
import io.github.moosasharwaan.appliedquartermaster.registry.ModComponents;
import io.github.moosasharwaan.appliedquartermaster.registry.ModMenus;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModuleLocator;
import io.github.moosasharwaan.appliedquartermaster.registry.ModCreativeTabs;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Applied Quartermaster: an AE2 addon with the ME Tablet, ME Library, ME Armory,
 * ME Tool Rack, ME Farm Controller and ME Redstone Plate.
 * <p>
 * Stage 2: the tablet, its modules, and the Library, Armory and Tool Rack on the AE2 network.
 */
@Mod(AppliedQuartermaster.MOD_ID)
public final class AppliedQuartermaster {

    public static final String MOD_ID = "appliedquartermaster";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AppliedQuartermaster(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        modBus.addListener(AppliedQuartermaster::registerCapabilities);
        modBus.addListener(ModNetwork::register);
        MenuLocators.register(TabletModuleLocator.class, TabletModuleLocator::writeToPacket, TabletModuleLocator::readFromPacket);
        LOGGER.info("Applied Quartermaster loaded");
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Lets AE2 cables and other AE2 blocks connect to the storage blocks.
        for (var type : List.of(ModBlockEntities.LIBRARY.get(), ModBlockEntities.ARMORY.get(), ModBlockEntities.TOOL_RACK.get())) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type, (be, context) -> be);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
