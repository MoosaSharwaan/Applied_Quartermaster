package io.github.moosasharwaan.appliedquartermaster.storage;

import io.github.moosasharwaan.appliedquartermaster.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ArmoryBlockEntity extends StorageBlockEntity {
    public ArmoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARMORY.get(), StorageKind.ARMORY, pos, state);
    }
}
