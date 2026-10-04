package io.github.moosasharwaan.appliedquartermaster.registry;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All Applied Quartermaster items, including the block items. */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AppliedQuartermaster.MOD_ID);

    /** The ME Tablet. */
    public static final DeferredItem<TabletItem> ME_TABLET = ITEMS.register("me_tablet",
            key -> new TabletItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, key))));

    /** The ME Redstone Plate. Becomes an AE2 cable part (like a P2P tunnel) in a later stage. */
    public static final DeferredItem<Item> ME_REDSTONE_PLATE = ITEMS.register("me_redstone_plate",
            key -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, key))));

    public static final DeferredItem<BlockItem> ME_LIBRARY = blockItem("me_library", ModBlocks.ME_LIBRARY);
    public static final DeferredItem<BlockItem> ME_ARMORY = blockItem("me_armory", ModBlocks.ME_ARMORY);
    public static final DeferredItem<BlockItem> ME_TOOL_RACK = blockItem("me_tool_rack", ModBlocks.ME_TOOL_RACK);
    public static final DeferredItem<BlockItem> ME_FARM_CONTROLLER = blockItem("me_farm_controller", ModBlocks.ME_FARM_CONTROLLER);

    private ModItems() {
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(ResourceKey.create(Registries.ITEM, key))));
    }
}
