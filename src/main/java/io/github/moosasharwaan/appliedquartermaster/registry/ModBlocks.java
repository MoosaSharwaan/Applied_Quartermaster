package io.github.moosasharwaan.appliedquartermaster.registry;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All Applied Quartermaster blocks. Plain blocks for now; network logic comes in later stages. */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedQuartermaster.MOD_ID);

    /** Stores up to 8 guide books; unlocks the tablet's Library tab. */
    public static final DeferredBlock<Block> ME_LIBRARY = machine("me_library");
    /** Stores up to 8 weapons and mining tools; unlocks the Armory tab. */
    public static final DeferredBlock<Block> ME_ARMORY = machine("me_armory");
    /** Stores up to 8 utility tools (wrenches, memory cards...); unlocks the Tools tab. */
    public static final DeferredBlock<Block> ME_TOOL_RACK = machine("me_tool_rack");
    /** Groups the Redstone Plates of one farm; unlocks the Automation tab. */
    public static final DeferredBlock<Block> ME_FARM_CONTROLLER = machine("me_farm_controller");

    private ModBlocks() {
    }

    /** Same hardness and sound as AE2's machines. */
    private static DeferredBlock<Block> machine(String name) {
        return BLOCKS.register(name, key -> new Block(BlockBehaviour.Properties.of()
                .strength(2.2f, 11f)
                .sound(SoundType.METAL)
                .setId(ResourceKey.create(Registries.BLOCK, key))));
    }
}
