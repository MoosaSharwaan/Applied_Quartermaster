package io.github.moosasharwaan.appliedquartermaster.client;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.me.common.MEStorageMenu;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.network.ReturnToTabletPayload;
import io.github.moosasharwaan.appliedquartermaster.registry.ModMenus;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModuleLocator;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TABLET.get(), TabletScreen::new);
    }

    /** Esc in an AE2 terminal that was opened from the tablet goes back to the tablet instead of closing. */
    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!event.getKeyEvent().isEscape()) {
            return;
        }
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
                && screen.getMenu() instanceof MEStorageMenu menu
                && menu.getTarget() instanceof ItemMenuHost<?> host
                && host.getLocator() instanceof TabletModuleLocator) {
            ClientPacketDistributor.sendToServer(new ReturnToTabletPayload());
            event.setCanceled(true);
        }
    }
}
