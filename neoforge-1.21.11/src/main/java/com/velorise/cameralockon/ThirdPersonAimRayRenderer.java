package com.velorise.cameralockon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Draws the player's real eye/projectile ray in third person. */
public final class ThirdPersonAimRayRenderer {
    private static final double MAX_DISTANCE = 128.0D;
    private static final double IMPACT_HALF_SIZE = 0.10D;

    private ThirdPersonAimRayRenderer() {}

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        CameraLockOnConfig.AimRayMode rayMode = CameraLockOnConfig.AimRayMode
                .fromConfig(CameraLockOnConfig.AIM_RAY_MODE.get());
        if (player == null || minecraft.level == null
                || !ThirdPersonCameraController.isVisualCameraActive()
                || rayMode == CameraLockOnConfig.AimRayMode.OFF
                || (rayMode == CameraLockOnConfig.AimRayMode.PROJECTILE_ONLY
                && !ProjectileAimCalculator.isSupportedWeapon(player))) {
            return;
        }

        Vec3 start = player.getEyePosition(partialTick)
                .add(player.getViewVector(partialTick).normalize().scale(0.14D));
        Vec3 impact = ThirdPersonAimResolver.resolvePlayerRayImpact(player, MAX_DISTANCE);
        if (impact == null || start.distanceToSqr(impact) < 0.01D) {
            return;
        }

        poseStack.pushPose();
        Vec3 cameraPosition = camera.position();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderTypes.lines());

        drawLine(consumer, matrix, start, impact, 0.35F, 0.90F, 1.0F, 0.82F);
        drawImpactBox(consumer, matrix, impact, 0.75F, 0.95F, 1.0F, 0.90F);

        minecraft.renderBuffers().bufferSource().endBatch(RenderTypes.lines());
        poseStack.popPose();
    }

    private static void drawImpactBox(VertexConsumer consumer, Matrix4f matrix, Vec3 point,
                                      float r, float g, float b, float a) {
        double s = IMPACT_HALF_SIZE;
        Vec3 p000 = point.add(-s, -s, -s);
        Vec3 p001 = point.add(-s, -s, s);
        Vec3 p010 = point.add(-s, s, -s);
        Vec3 p011 = point.add(-s, s, s);
        Vec3 p100 = point.add(s, -s, -s);
        Vec3 p101 = point.add(s, -s, s);
        Vec3 p110 = point.add(s, s, -s);
        Vec3 p111 = point.add(s, s, s);
        drawLine(consumer, matrix, p000, p001, r, g, b, a);
        drawLine(consumer, matrix, p000, p010, r, g, b, a);
        drawLine(consumer, matrix, p000, p100, r, g, b, a);
        drawLine(consumer, matrix, p111, p110, r, g, b, a);
        drawLine(consumer, matrix, p111, p101, r, g, b, a);
        drawLine(consumer, matrix, p111, p011, r, g, b, a);
        drawLine(consumer, matrix, p001, p011, r, g, b, a);
        drawLine(consumer, matrix, p001, p101, r, g, b, a);
        drawLine(consumer, matrix, p010, p011, r, g, b, a);
        drawLine(consumer, matrix, p010, p110, r, g, b, a);
        drawLine(consumer, matrix, p100, p101, r, g, b, a);
        drawLine(consumer, matrix, p100, p110, r, g, b, a);
    }

    private static void drawLine(VertexConsumer consumer, Matrix4f matrix, Vec3 from, Vec3 to,
                                 float r, float g, float b, float a) {
        Vec3 normal = to.subtract(from);
        if (normal.lengthSqr() < 1.0E-8D) {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }
        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(r, g, b, a)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(2.0F);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(r, g, b, a)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(2.0F);
    }
}
