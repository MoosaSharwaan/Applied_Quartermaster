package io.github.moosasharwaan.appliedquartermaster.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.network.OpenTabletPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The "Open ME Tablet" key: opens the tablet from the hand, the inventory or a Curios slot. Unbound by default,
 * like AE2's own terminal keys; set it under Options, Controls, Key Binds.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID, value = Dist.CLIENT)
public final class TabletKeys {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(AppliedQuartermaster.id("main"));

    public static final KeyMapping OPEN_TABLET = new KeyMapping("key.appliedquartermaster.open_tablet",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private TabletKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_TABLET);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        while (OPEN_TABLET.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                ClientPacketDistributor.sendToServer(new OpenTabletPayload(mc.player.isShiftKeyDown()));
            }
        }
    }
}
