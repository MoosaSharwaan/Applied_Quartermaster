package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AppliedQuartermaster.MOD_ID);
        registrar.playToServer(ReturnToTabletPayload.TYPE, ReturnToTabletPayload.STREAM_CODEC, ReturnToTabletPayload::handle);
        registrar.playToServer(OpenTabletPayload.TYPE, OpenTabletPayload.STREAM_CODEC, OpenTabletPayload::handle);
        registrar.playToServer(TabletActionPayload.TYPE, TabletActionPayload.STREAM_CODEC, TabletActionPayload::handle);
        registrar.playToClient(TabletViewPayload.TYPE, TabletViewPayload.STREAM_CODEC,
                (payload, context) -> io.github.moosasharwaan.appliedquartermaster.client.ClientPayloads.onView(payload));
        registrar.playToClient(AutomationViewPayload.TYPE, AutomationViewPayload.STREAM_CODEC,
                (payload, context) -> io.github.moosasharwaan.appliedquartermaster.client.ClientPayloads.onAutomation(payload));
        registrar.playToClient(DevicesViewPayload.TYPE, DevicesViewPayload.STREAM_CODEC,
                (payload, context) -> io.github.moosasharwaan.appliedquartermaster.client.ClientPayloads.onDevices(payload));
    }
}
