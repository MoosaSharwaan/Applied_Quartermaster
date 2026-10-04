package io.github.moosasharwaan.appliedquartermaster.storage;

import io.github.moosasharwaan.appliedquartermaster.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ToolRackBlockEntity extends StorageBlockEntity {
    public ToolRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOOL_RACK.get(), StorageKind.TOOLS, pos, state);
    }
}
