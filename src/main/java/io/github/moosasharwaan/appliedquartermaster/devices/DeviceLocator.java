package io.github.moosasharwaan.appliedquartermaster.devices;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * "Locate" on the Devices page: a light beam rises above the device for 10 seconds, and the action bar shows where it
 * is and which way to go.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID)
public final class DeviceLocator {

    private static final int BEAM_TICKS = 200;

    private record Beam(ServerLevel level, BlockPos pos, int[] ticksLeft) {
    }

    private static final List<Beam> BEAMS = new ArrayList<>();

    private DeviceLocator() {
    }

    public static void locate(ServerPlayer player, DeviceScanner.Device device) {
        var node = device.node();
        var level = node.getLevel();
        var pos = device.pos();
        var name = DeviceScanner.name(device.kind());
        if (level == null || level != player.level()) {
            player.sendOverlayMessage(Component.translatable("gui.appliedquartermaster.devices.locate_other_dimension",
                    name, pos.getX(), pos.getY(), pos.getZ(), device.dimension()));
            return;
        }
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + (pos.getY() - player.getY()) * (pos.getY() - player.getY()) + dz * dz));
        player.sendOverlayMessage(Component.translatable("gui.appliedquartermaster.devices.locate", name,
                pos.getX(), pos.getY(), pos.getZ(), distance,
                Component.translatable("gui.appliedquartermaster.direction." + direction(dx, dz))));
        synchronized (BEAMS) {
            BEAMS.removeIf(b -> b.level == level && b.pos.equals(pos));
            BEAMS.add(new Beam(level, pos.immutable(), new int[]{BEAM_TICKS}));
        }
    }

    /** One of eight compass directions from the player to the device. */
    static String direction(double dx, double dz) {
        if (Math.abs(dx) < 1.5 && Math.abs(dz) < 1.5) {
            return "here";
        }
        // Minecraft: +X is east, +Z is south.
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) {
            angle += 360;
        }
        String[] names = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
        return names[(int) Math.round(angle / 45) % 8];
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (BEAMS) {
            if (BEAMS.isEmpty()) {
                return;
            }
            var it = BEAMS.iterator();
            while (it.hasNext()) {
                var beam = it.next();
                int left = --beam.ticksLeft[0];
                if (left <= 0) {
                    it.remove();
                    continue;
                }
                if (left % 4 == 0) {
                    for (int i = 0; i < 24; i++) {
                        beam.level.sendParticles(ParticleTypes.END_ROD, true, true,
                                beam.pos.getX() + 0.5, beam.pos.getY() + 1.2 + i * 0.5, beam.pos.getZ() + 0.5,
                                1, 0.02, 0.1, 0.02, 0.0);
                    }
                }
            }
        }
    }
}
