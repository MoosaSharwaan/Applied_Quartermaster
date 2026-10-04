package io.github.moosasharwaan.appliedquartermaster.registry;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.storage.ArmoryBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.LibraryBlockEntity;
import io.github.moosasharwaan.appliedquartermaster.storage.ToolRackBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AppliedQuartermaster.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LibraryBlockEntity>> LIBRARY =
            BLOCK_ENTITIES.register("me_library", () -> new BlockEntityType<>(LibraryBlockEntity::new, ModBlocks.ME_LIBRARY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmoryBlockEntity>> ARMORY =
            BLOCK_ENTITIES.register("me_armory", () -> new BlockEntityType<>(ArmoryBlockEntity::new, ModBlocks.ME_ARMORY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolRackBlockEntity>> TOOL_RACK =
            BLOCK_ENTITIES.register("me_tool_rack", () -> new BlockEntityType<>(ToolRackBlockEntity::new, ModBlocks.ME_TOOL_RACK.get()));

    private ModBlockEntities() {
    }
}
