package com.velorise.cameralockon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

/** Client-only solid projectile arc with vanilla projectile profiles and impact preview. */
public final class ProjectileTrajectoryRenderer {
    private static final int MAX_STEPS = 480;
    private static final double SUBSTEP = 0.25D;
    private static final double ENTITY_HIT_PADDING = 0.18D;
    private static final double IMPACT_BOX_HALF_SIZE = 0.14D;

    private ProjectileTrajectoryRenderer() {}

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !CameraLockOnConfig.TRAJECTORY_PREVIEW.get()) return;

        ItemStack stack = findSupportedStack(player);
        ProjectileProfile profile = profileFor(stack);
        if (profile == null) return;

        LivingEntity lockedTarget = LockOnController.getLockedTarget();
        Vec3 aim = lockedTarget == null ? null : LockOnController.getLastEffectiveAimPoint();
        if (aim == null && !CameraLockOnConfig.TRAJECTORY_SHOW_WITHOUT_LOCK.get()) return;

        double maxLength = CameraLockOnConfig.TRAJECTORY_PREVIEW_LENGTH.get();
        Vec3 start = player.getEyePosition(partialTick).add(player.getViewVector(partialTick).scale(0.12D));
        if (aim == null) {
            aim = start.add(player.getViewVector(partialTick).scale(maxLength));
        }

        Vec3 launchDirection = applyPitchOffset(aim.subtract(start), profile.pitchOffsetDegrees);
        if (launchDirection.lengthSqr() < 1.0E-8D) return;

        double currentSpeed = profile.currentSpeed(player, stack);
        if (currentSpeed > 0.08D) {
            simulateAndDraw(mc, poseStack, camera, player, start, launchDirection, currentSpeed,
                    profile.gravity, profile.drag, 0.15F, 0.95F, 0.85F, 0.88F);
        }

        if (profile.bow
                && CameraLockOnConfig.TRAJECTORY_FULL_CHARGE.get()
                && currentSpeed < 2.92D) {
            simulateAndDraw(mc, poseStack, camera, player, start, launchDirection, 3.0D,
                    profile.gravity, profile.drag, 0.55F, 0.75F, 1.0F, 0.34F);
        }
    }

    private static ItemStack findSupportedStack(LocalPlayer player) {
        if (player.isUsingItem() && profileFor(player.getUseItem()) != null) {
            return player.getUseItem();
        }
        if (profileFor(player.getMainHandItem()) != null) {
            return player.getMainHandItem();
        }
        if (profileFor(player.getOffhandItem()) != null) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static ProjectileProfile profileFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.is(Items.BOW)) return ProjectileProfile.BOW;
        if (stack.is(Items.CROSSBOW)) return ProjectileProfile.CROSSBOW;
        if (stack.is(Items.TRIDENT)) return ProjectileProfile.TRIDENT;
        if (stack.is(Items.ENDER_PEARL)) return ProjectileProfile.ENDER_PEARL;
        if (stack.is(Items.SNOWBALL)) return ProjectileProfile.SNOWBALL;
        if (stack.is(Items.EGG)) return ProjectileProfile.EGG;
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return ProjectileProfile.POTION;
        if (stack.is(Items.EXPERIENCE_BOTTLE)) return ProjectileProfile.EXPERIENCE_BOTTLE;
        return null;
    }

    private static Vec3 applyPitchOffset(Vec3 direction, double pitchOffsetDegrees) {
        if (direction.lengthSqr() < 1.0E-8D) return Vec3.ZERO;
        Vec3 normalized = direction.normalize();
        if (Math.abs(pitchOffsetDegrees) < 1.0E-6D) return normalized;

        double horizontalLength = Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z);
        if (horizontalLength < 1.0E-6D) return normalized;

        Vec3 horizontal = new Vec3(normalized.x / horizontalLength, 0.0D, normalized.z / horizontalLength);
        double pitch = Math.atan2(-normalized.y, horizontalLength) + Math.toRadians(pitchOffsetDegrees);
        double horizontalScale = Math.cos(pitch);
        return horizontal.scale(horizontalScale).add(0.0D, -Math.sin(pitch), 0.0D).normalize();
    }

    private static void simulateAndDraw(Minecraft mc, PoseStack poseStack, Camera camera, LocalPlayer player,
                                        Vec3 start, Vec3 launchDirection, double speed,
                                        double gravity, double drag,
                                        float red, float green, float blue, float alpha) {
        Vec3 velocity = launchDirection.normalize().scale(speed);
        if (CameraLockOnConfig.PROJECTILE_COMPENSATE_PLAYER_MOVEMENT.get()) {
            velocity = velocity.add(player.getDeltaMovement());
        }

        double maxLength = CameraLockOnConfig.TRAJECTORY_PREVIEW_LENGTH.get();
        Vec3 position = start;
        double traveled = 0.0D;
        ImpactType impactType = ImpactType.RANGE_END;

        poseStack.pushPose();
        Vec3 cameraPosition = camera.getPosition();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        double substepDrag = Math.pow(drag, SUBSTEP);
        for (int step = 0; step < MAX_STEPS && traveled < maxLength; step++) {
            Vec3 proposed = position.add(velocity.scale(SUBSTEP));
            double remaining = maxLength - traveled;
            double proposedDistance = position.distanceTo(proposed);
            if (proposedDistance > remaining && proposedDistance > 1.0E-8D) {
                proposed = position.add(proposed.subtract(position).scale(remaining / proposedDistance));
            }

            SegmentImpact impact = findNearestImpact(mc, player, position, proposed);
            Vec3 next = impact == null ? proposed : impact.location;

            drawLine(consumer, matrix, position, next, red, green, blue, alpha);
            traveled += position.distanceTo(next);
            position = next;

            if (impact != null) {
                impactType = impact.type;
                break;
            }
            if (traveled >= maxLength - 1.0E-6D) break;

            velocity = velocity.scale(substepDrag).add(0.0D, -gravity * SUBSTEP, 0.0D);
        }

        float markerR;
        float markerG;
        float markerB;
        switch (impactType) {
            case ENTITY -> {
                markerR = 0.20F;
                markerG = 1.00F;
                markerB = 0.30F;
            }
            case BLOCK -> {
                markerR = 1.00F;
                markerG = 0.22F;
                markerB = 0.15F;
            }
            default -> {
                markerR = 0.65F;
                markerG = 0.75F;
                markerB = 0.85F;
            }
        }

        drawImpactBox(consumer, matrix, position, markerR, markerG, markerB, Math.max(alpha, 0.72F));
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static SegmentImpact findNearestImpact(Minecraft mc, LocalPlayer player, Vec3 from, Vec3 to) {
        HitResult blockHit = mc.level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 nearestLocation = null;
        ImpactType nearestType = null;
        double nearestDistanceSqr = Double.POSITIVE_INFINITY;

        if (blockHit.getType() != HitResult.Type.MISS) {
            nearestLocation = blockHit.getLocation();
            nearestType = ImpactType.BLOCK;
            nearestDistanceSqr = from.distanceToSqr(nearestLocation);
        }

        AABB searchBox = new AABB(
                Math.min(from.x, to.x),
                Math.min(from.y, to.y),
                Math.min(from.z, to.z),
                Math.max(from.x, to.x),
                Math.max(from.y, to.y),
                Math.max(from.z, to.z)
        ).inflate(ENTITY_HIT_PADDING + 0.25D);

        LivingEntity lockedTarget = LockOnController.getLockedTarget();
        if (lockedTarget != null && !lockedTarget.isRemoved()) {
            for (MultipartDamageBoxResolver.DamageBox damageBox : MultipartDamageBoxResolver.resolve(lockedTarget)) {
                Optional<Vec3> clipped = damageBox.box().inflate(ENTITY_HIT_PADDING).clip(from, to);
                if (clipped.isEmpty()) continue;

                Vec3 location = clipped.get();
                double distanceSqr = from.distanceToSqr(location);
                if (distanceSqr < nearestDistanceSqr) {
                    nearestLocation = location;
                    nearestType = ImpactType.ENTITY;
                    nearestDistanceSqr = distanceSqr;
                }
            }
        }

        LivingEntity finalLockedTarget = lockedTarget;
        List<Entity> entities = mc.level.getEntities(player, searchBox, entity ->
                entity != player
                        && entity != player.getVehicle()
                        && entity != finalLockedTarget
                        && !entity.isSpectator()
                        && entity.isPickable()
        );

        for (Entity entity : entities) {
            Optional<Vec3> clipped = entity.getBoundingBox().inflate(ENTITY_HIT_PADDING).clip(from, to);
            if (clipped.isEmpty()) continue;

            Vec3 location = clipped.get();
            double distanceSqr = from.distanceToSqr(location);
            if (distanceSqr < nearestDistanceSqr) {
                nearestLocation = location;
                nearestType = ImpactType.ENTITY;
                nearestDistanceSqr = distanceSqr;
            }
        }

        return nearestLocation == null ? null : new SegmentImpact(nearestLocation, nearestType);
    }

    private static void drawImpactBox(VertexConsumer consumer, Matrix4f matrix, Vec3 point,
                                      float r, float g, float b, float a) {
        double s = IMPACT_BOX_HALF_SIZE;
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
        if (normal.lengthSqr() < 1.0E-8D) normal = new Vec3(0, 1, 0); else normal = normal.normalize();
        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(r, g, b, a)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(r, g, b, a)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static final class ProjectileProfile {
        private static final ProjectileProfile BOW = new ProjectileProfile(true, 3.0D, 0.05D, 0.99D, 0.0D);
        private static final ProjectileProfile CROSSBOW = new ProjectileProfile(false, 3.15D, 0.05D, 0.99D, 0.0D);
        private static final ProjectileProfile TRIDENT = new ProjectileProfile(false, 2.5D, 0.05D, 0.99D, 0.0D);
        private static final ProjectileProfile ENDER_PEARL = new ProjectileProfile(false, 1.5D, 0.03D, 0.99D, 0.0D);
        private static final ProjectileProfile SNOWBALL = new ProjectileProfile(false, 1.5D, 0.03D, 0.99D, 0.0D);
        private static final ProjectileProfile EGG = new ProjectileProfile(false, 1.5D, 0.03D, 0.99D, 0.0D);
        private static final ProjectileProfile POTION = new ProjectileProfile(false, 0.5D, 0.05D, 0.99D, -20.0D);
        private static final ProjectileProfile EXPERIENCE_BOTTLE = new ProjectileProfile(false, 0.7D, 0.07D, 0.99D, -20.0D);

        private final boolean bow;
        private final double speed;
        private final double gravity;
        private final double drag;
        private final double pitchOffsetDegrees;

        private ProjectileProfile(boolean bow, double speed, double gravity, double drag, double pitchOffsetDegrees) {
            this.bow = bow;
            this.speed = speed;
            this.gravity = gravity;
            this.drag = drag;
            this.pitchOffsetDegrees = pitchOffsetDegrees;
        }

        private double currentSpeed(LocalPlayer player, ItemStack stack) {
            if (!this.bow) return this.speed;
            if (!player.isUsingItem() || !player.getUseItem().is(Items.BOW)) return 0.0D;
            float charge = player.getTicksUsingItem() / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            return Math.max(0.0D, Math.min(1.0D, charge)) * 3.0D;
        }
    }

    private enum ImpactType {
        BLOCK,
        ENTITY,
        RANGE_END
    }

    private static final class SegmentImpact {
        private final Vec3 location;
        private final ImpactType type;

        private SegmentImpact(Vec3 location, ImpactType type) {
            this.location = location;
            this.type = type;
        }
    }
}
