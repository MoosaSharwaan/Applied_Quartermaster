package io.github.moosasharwaan.appliedquartermaster.storage;

import io.github.moosasharwaan.appliedquartermaster.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class LibraryBlockEntity extends StorageBlockEntity {
    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY.get(), StorageKind.LIBRARY, pos, state);
    }
}
