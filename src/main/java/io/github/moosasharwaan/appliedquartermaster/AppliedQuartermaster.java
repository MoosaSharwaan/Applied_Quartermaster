package io.github.moosasharwaan.appliedquartermaster;

import com.mojang.logging.LogUtils;
import io.github.moosasharwaan.appliedquartermaster.registry.ModBlocks;
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
 * This is the starting skeleton: items and blocks are registered so the mod loads in game.
 * Network behaviour, screens and the tablet features are added in later stages.
 */
@Mod(AppliedQuartermaster.MOD_ID)
public final class AppliedQuartermaster {

    public static final String MOD_ID = "appliedquartermaster";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AppliedQuartermaster(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        LOGGER.info("Applied Quartermaster loaded");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
