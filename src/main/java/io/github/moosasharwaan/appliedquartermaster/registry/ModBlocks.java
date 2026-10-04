package io.github.moosasharwaan.appliedquartermaster.registry;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.block.ConnectedMachineBlock;
import io.github.moosasharwaan.appliedquartermaster.block.FacingMachineBlock;
import io.github.moosasharwaan.appliedquartermaster.block.LibraryBlock;
import io.github.moosasharwaan.appliedquartermaster.storage.ArmoryBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.ToolRackBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

/** All Applied Quartermaster blocks. Library, Armory and Tool Rack join the AE2 network; the Farm Controller comes in stage 3. */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedQuartermaster.MOD_ID);

    /** Stores up to 8 guide books; unlocks the tablet's Library tab. */
    public static final DeferredBlock<LibraryBlock> ME_LIBRARY = machine("me_library", LibraryBlock::new);
    /** Stores up to 8 weapons and mining tools; unlocks the Armory tab. */
    public static final DeferredBlock<ConnectedMachineBlock> ME_ARMORY = machine("me_armory", p -> new ConnectedMachineBlock(p, ArmoryBlockEntity::new));
    /** Stores up to 8 utility tools (wrenches, memory cards...); unlocks the Tools tab. */
    public static final DeferredBlock<ConnectedMachineBlock> ME_TOOL_RACK = machine("me_tool_rack", p -> new ConnectedMachineBlock(p, ToolRackBlockEntity::new));
    /** Groups the Redstone Plates of one farm; unlocks the Automation tab. */
    public static final DeferredBlock<FacingMachineBlock> ME_FARM_CONTROLLER = machine("me_farm_controller", FacingMachineBlock::new);

    private ModBlocks() {
    }

    /** Same hardness and sound as AE2's machines. */
    private static <B extends Block> DeferredBlock<B> machine(String name, Function<BlockBehaviour.Properties, B> factory) {
        return BLOCKS.register(name, key -> factory.apply(BlockBehaviour.Properties.of()
                .strength(2.2f, 11f)
                .sound(SoundType.METAL)
                .setId(ResourceKey.create(Registries.BLOCK, key))));
    }
}
