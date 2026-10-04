package io.github.moosasharwaan.appliedquartermaster.automation;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.util.AECableType;
import appeng.parts.AEBasePart;
import appeng.parts.automation.PartModelData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The ME Redstone Plate: a cable part (like a P2P tunnel) on a farm's cables. When switched on it gives a
 * redstone signal (strength 1 to 15) to the block it faces. Its light ring is green when on, red when off,
 * and dark when the farm's controller is offline. Up to 6 fit around one cable.
 */
public class RedstonePlatePart extends AEBasePart implements IGridTickable {

    private boolean on;
    private int strength = 15;
    private @Nullable String label;
    private ItemStack icon = ItemStack.EMPTY;

    /** Server: whether an online Farm Controller is on this plate's farm grid. Client: last synced value. */
    private boolean online;
    /** Signal currently given out, so neighbours are only updated on change. */
    private int emitted;

    public RedstonePlatePart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setFlags().setIdlePowerUsage(0).addService(IGridTickable.class, this);
    }

    // ---------------------------------------------------------------- state

    public boolean isOn() {
        return on;
    }

    public boolean isOnline() {
        return online;
    }

    public int getStrength() {
        return strength;
    }

    public void setOn(boolean on) {
        if (this.on != on) {
            this.on = on;
            changed();
        }
    }

    public void setStrength(int strength) {
        int clamped = Math.max(1, Math.min(15, strength));
        if (this.strength != clamped) {
            this.strength = clamped;
            changed();
        }
    }

    public Component getLabel() {
        if (label != null && !label.isBlank()) {
            return Component.literal(label);
        }
        if (getSide() == null) {
            return getName();
        }
        return Component.translatable("gui.appliedquartermaster.plate.default_name",
                Component.translatable("gui.appliedquartermaster.side." + getSide().getSerializedName()));
    }

    public void setLabel(@Nullable String label) {
        this.label = label == null || label.isBlank() ? null : label.substring(0, Math.min(40, label.length()));
        getHost().markForSave();
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon.isEmpty() ? ItemStack.EMPTY : icon.copyWithCount(1);
        getHost().markForSave();
    }

    /** The online Farm Controller of this plate's farm, if any. */
    public @Nullable FarmControllerBlockEntity findController() {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return null;
        }
        for (var port : grid.getMachines(FarmControllerBlockEntity.FarmPort.class)) {
            if (!port.controller().isRemoved() && port.controller().isOnline()) {
                return port.controller();
            }
        }
        return null;
    }

    private void updateOnline() {
        if (isClientSide()) {
            return;
        }
        boolean nowOnline = findController() != null;
        if (nowOnline != online) {
            online = nowOnline;
            changed();
        }
    }

    private void changed() {
        if (getHost() == null || isClientSide()) {
            return;
        }
        getHost().markForSave();
        getHost().markForUpdate();
        int signal = on && online ? strength : 0;
        if (signal != emitted) {
            emitted = signal;
            var be = getBlockEntity();
            var level = be.getLevel();
            if (level != null && getSide() != null) {
                var block = be.getBlockState().getBlock();
                level.updateNeighborsAt(be.getBlockPos(), block, null);
                level.updateNeighborsAt(be.getBlockPos().relative(getSide()), block, null);
            }
        }
    }

    // ---------------------------------------------------------------- AE2

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 10, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        updateOnline();
        return TickRateModulation.SAME;
    }

    @Override
    protected void onMainNodeStateChanged(appeng.api.networking.IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        updateOnline();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        emitted = -1;
        updateOnline();
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public int isProvidingStrongPower() {
        return on && online ? strength : 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return on && online ? strength : 0;
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            if (player.isShiftKeyDown()) {
                setStrength(strength >= 15 ? 1 : strength + 1);
                player.sendOverlayMessage(Component.translatable("gui.appliedquartermaster.plate.strength", strength));
            } else {
                setOn(!on);
            }
        }
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 12, 11, 11, 13);
        bch.addBox(3, 3, 13, 13, 13, 14);
        bch.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public void collectModelData(ModelData.Builder builder) {
        // Reuse AE2's status-indicator model: active = on (green), powered = off (red), unpowered = offline (dark).
        PartModelData.StatusIndicatorState state;
        if (online && on) {
            state = PartModelData.StatusIndicatorState.ACTIVE;
        } else if (online) {
            state = PartModelData.StatusIndicatorState.POWERED;
        } else {
            state = PartModelData.StatusIndicatorState.UNPOWERED;
        }
        builder.with(PartModelData.STATUS_INDICATOR, state);
    }

    // ---------------------------------------------------------------- sync and save

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(on);
        data.writeBoolean(online);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean wasOn = on;
        boolean wasOnline = online;
        on = data.readBoolean();
        online = data.readBoolean();
        return changed || wasOn != on || wasOnline != online;
    }

    @Override
    public void writeVisualStateToNBT(ValueOutput output) {
        super.writeVisualStateToNBT(output);
        output.putBoolean("on", on);
        output.putBoolean("online", online);
    }

    @Override
    public void readVisualStateFromNBT(ValueInput input) {
        super.readVisualStateFromNBT(input);
        on = input.getBooleanOr("on", false);
        online = input.getBooleanOr("online", false);
    }

    @Override
    public void writeToNBT(ValueOutput data) {
        super.writeToNBT(data);
        data.putBoolean("on", on);
        data.putInt("strength", strength);
        if (label != null) {
            data.putString("label", label);
        }
        if (!icon.isEmpty()) {
            data.store("icon", ItemStack.OPTIONAL_CODEC, icon);
        }
    }

    @Override
    public void readFromNBT(ValueInput input) {
        super.readFromNBT(input);
        on = input.getBooleanOr("on", false);
        strength = Math.max(1, Math.min(15, input.getIntOr("strength", 15)));
        label = input.getString("label").orElse(null);
        icon = input.read("icon", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
