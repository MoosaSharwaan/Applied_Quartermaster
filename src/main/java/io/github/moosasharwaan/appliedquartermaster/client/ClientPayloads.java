package io.github.moosasharwaan.appliedquartermaster.client;

import io.github.moosasharwaan.appliedquartermaster.network.AutomationViewPayload;
import io.github.moosasharwaan.appliedquartermaster.network.DevicesViewPayload;
import io.github.moosasharwaan.appliedquartermaster.network.TabletViewPayload;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletMenu;
import net.minecraft.client.Minecraft;

/** Client-side payload handlers. Only loaded on the client. */
public final class ClientPayloads {

    private ClientPayloads() {
    }

    public static void onDevices(DevicesViewPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof TabletMenu menu && menu.containerId == payload.containerId()) {
            menu.receiveDevices(payload);
        }
    }

    public static void onAutomation(AutomationViewPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof TabletMenu menu && menu.containerId == payload.containerId()) {
            menu.receiveAutomation(payload);
        }
    }

    public static void onView(TabletViewPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof TabletMenu menu && menu.containerId == payload.containerId()) {
            menu.receiveView(payload);
        }
    }
}
