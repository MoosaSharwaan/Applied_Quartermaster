package io.github.moosasharwaan.appliedquartermaster.registry;

import com.mojang.serialization.Codec;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Data stored on the ME Tablet item. */
public final class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AppliedQuartermaster.MOD_ID);

    /** The terminals installed in the tablet's module slots. */
    public static final DataComponentType<ItemContainerContents> TABLET_MODULES = register("tablet_modules",
            builder -> builder.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** The pinned app that opens first (module slot 0..3, or 10 + storage kind); absent when nothing is pinned. */
    public static final DataComponentType<Integer> TABLET_DEFAULT_MODULE = register("tablet_default_module",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Remembered view size (Large, Medium, Small) for each storage app, 2 bits per app. */
    public static final DataComponentType<Integer> TABLET_VIEW_SIZES = register("tablet_view_sizes",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The tablet's upgrade cards (wireless boosters). */
    public static final DataComponentType<ItemContainerContents> TABLET_UPGRADES = register("tablet_upgrades",
            builder -> builder.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    private ModComponents() {
    }

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var type = builder.build();
        COMPONENTS.register(name, () -> type);
        return type;
    }
}
