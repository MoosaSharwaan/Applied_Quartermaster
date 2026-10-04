package io.github.moosasharwaan.appliedquartermaster.integration.curios;

import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Uses Curios classes; only reached through {@link CuriosCompat} when Curios is installed. */
final class CuriosTablets {

    private CuriosTablets() {
    }

    private static List<Map.Entry<String, ICurioStacksHandler>> handlers(Player player) {
        var inventory = CuriosApi.getCuriosInventoryOrNull(player);
        if (inventory == null) {
            return List.of();
        }
        var list = new ArrayList<>(inventory.getCurios().entrySet());
        list.sort(Map.Entry.comparingByKey());
        return list;
    }

    static ItemStack get(Player player, int slot) {
        int flat = -slot - 1;
        for (var entry : handlers(player)) {
            var stacks = entry.getValue().getStacks();
            if (flat < stacks.getSlots()) {
                return stacks.getStackInSlot(flat);
            }
            flat -= stacks.getSlots();
        }
        return ItemStack.EMPTY;
    }

    static int find(Player player) {
        int flat = 0;
        for (var entry : handlers(player)) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (stacks.getStackInSlot(i).is(ModItems.ME_TABLET.get())) {
                    return -(flat + i) - 1;
                }
            }
            flat += stacks.getSlots();
        }
        return Integer.MIN_VALUE;
    }

    static int equip(Player player, String slotType, ItemStack stack) {
        int flat = 0;
        for (var entry : handlers(player)) {
            var stacks = entry.getValue().getStacks();
            if (entry.getKey().equals(slotType) && stacks.getSlots() > 0) {
                stacks.setStackInSlot(0, stack);
                return -flat - 1;
            }
            flat += stacks.getSlots();
        }
        return Integer.MIN_VALUE;
    }

    static String describe(Player player) {
        var sb = new StringBuilder();
        for (var entry : handlers(player)) {
            sb.append(entry.getKey()).append('x').append(entry.getValue().getStacks().getSlots()).append(' ');
        }
        return sb.toString().trim();
    }
}
