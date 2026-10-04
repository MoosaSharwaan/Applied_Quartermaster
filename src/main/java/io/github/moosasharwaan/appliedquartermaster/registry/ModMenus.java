package io.github.moosasharwaan.appliedquartermaster.registry;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.storage.StorageMenu;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AppliedQuartermaster.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TabletMenu>> TABLET = MENUS.register("tablet",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new TabletMenu(id, inventory, buf.readVarInt(), buf.readVarInt() - 1, buf.readVarInt())));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> STORAGE = MENUS.register("storage",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> StorageMenu.fromNetwork(id, inventory, buf.readBlockPos())));

    private ModMenus() {
    }
}
