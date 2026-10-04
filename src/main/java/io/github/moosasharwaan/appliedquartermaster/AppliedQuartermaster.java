package io.github.moosasharwaan.appliedquartermaster;

import com.mojang.logging.LogUtils;
import appeng.menu.locator.MenuLocators;
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
 * Stage 1: the tablet screen with module tabs and the Inventory module (a real AE2 terminal).
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
        modBus.addListener(ModNetwork::register);
        MenuLocators.register(TabletModuleLocator.class, TabletModuleLocator::writeToPacket, TabletModuleLocator::readFromPacket);
        LOGGER.info("Applied Quartermaster loaded");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
