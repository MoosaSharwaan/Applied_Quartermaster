package io.github.moosasharwaan.appliedquartermaster.tablet;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID)
public final class TabletEvents {

    private TabletEvents() {
    }

    /** Saves the terminal's last changes into the tablet when its screen closes. */
    @SubscribeEvent
    public static void onClose(PlayerContainerEvent.Close event) {
        TabletModuleLocator.flush(event.getEntity());
    }
}
