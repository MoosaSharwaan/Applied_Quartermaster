package io.github.moosasharwaan.appliedquartermaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: with the off hand empty, the ME Tablet is held in both hands in front of you, like a map
 * (and tilts up as you look down). With something in the off hand it is held in one hand as usual.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID, value = Dist.CLIENT)
public final class TabletHandRenderer {

    private TabletHandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.ME_TABLET.get()) || !player.getOffhandItem().isEmpty()
                || player.isScoping()) {
            return;
        }
        event.setCanceled(true);
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        renderTwoHanded(mc, poseStack, event.getSubmitNodeCollector(), event.getPackedLight(),
                event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(), event.getItemStack());
        poseStack.popPose();
    }

    /** Same motion as the vanilla two-handed map, with the 3D tablet in place of the map. */
    private static void renderTwoHanded(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, int light,
                                        float xRot, float inverseArmHeight, float attack, ItemStack tablet) {
        float sqrtAttack = Mth.sqrt(attack);
        float ySwing = -0.2F * Mth.sin(attack * (float) Math.PI);
        float zSwing = -0.4F * Mth.sin(sqrtAttack * (float) Math.PI);
        poseStack.translate(0.0F, -ySwing / 2.0F, zSwing);
        float tilt = mapTilt(xRot);
        poseStack.translate(0.0F, 0.04F + inverseArmHeight * -1.2F + tilt * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));
        if (!mc.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            renderHand(mc, poseStack, collector, light, HumanoidArm.RIGHT);
            renderHand(mc, poseStack, collector, light, HumanoidArm.LEFT);
            poseStack.popPose();
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(sqrtAttack * (float) Math.PI) * 20.0F));
        // Place the tablet where the map would be: centred between the hands, screen towards you.
        poseStack.scale(0.8F, 0.8F, 0.8F);
        var state = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(state, tablet, ItemDisplayContext.NONE, mc.level, mc.player, 0);
        state.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
    }

    private static float mapTilt(float xRot) {
        float tilt = 1.0F - xRot / 45.0F + 0.1F;
        tilt = Mth.clamp(tilt, 0.0F, 1.0F);
        return -Mth.cos(tilt * (float) Math.PI) * 0.5F + 0.5F;
    }

    private static void renderHand(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidArm arm) {
        var player = mc.player;
        var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
        poseStack.pushPose();
        float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -41.0F));
        poseStack.translate(invert * 0.3F, -1.1F, 0.45F);
        var skin = player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, collector, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
        } else {
            renderer.renderLeftHand(poseStack, collector, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
        }
        poseStack.popPose();
    }
}
