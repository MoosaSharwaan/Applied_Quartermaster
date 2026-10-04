package io.github.moosasharwaan.appliedquartermaster.network;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AppliedQuartermaster.MOD_ID);
        registrar.playToServer(ReturnToTabletPayload.TYPE, ReturnToTabletPayload.STREAM_CODEC, ReturnToTabletPayload::handle);
    }
}
