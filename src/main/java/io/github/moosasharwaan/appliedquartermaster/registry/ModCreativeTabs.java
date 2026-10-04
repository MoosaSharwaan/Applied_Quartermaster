package io.github.moosasharwaan.appliedquartermaster.registry;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Applied Quartermaster creative tab, listing every item of the mod. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedQuartermaster.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.appliedquartermaster"))
                    .icon(() -> new ItemStack(ModItems.ME_TABLET.get()))
                    .displayItems((params, output) -> ModItems.ITEMS.getEntries()
                            .forEach(entry -> output.accept(entry.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
