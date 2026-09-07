package com.velorise.cameralockon;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Resolves Converged Lock center aim and Contextual free-look aim indicators. */
public final class ThirdPersonAimResolver {
    private static final double MAX_RAY_DISTANCE = 128.0D;
    private static final double BLOCK_EPSILON_SQR = 0.0025D;

    private ThirdPersonAimResolver() {}

    public static CursorPoint resolveCursor() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null
                || !ThirdPersonCameraController.isVisualCameraActive()) {
            return CursorPoint.hidden();
        }

        if (ThirdPersonCameraController.isConvergedCameraSteeringActive()) {
            Vec3 aimPoint = LockOnController.getLastEffectiveAimPoint();
            if (!isUsableAimPoint(aimPoint)) {
                return CursorPoint.hidden();
            }
            boolean blocked = isFiringPathBlocked(minecraft, player, aimPoint);
            return new CursorPoint(
                    minecraft.getWindow().getGuiScaledWidth() / 2,
                    minecraft.getWindow().getGuiScaledHeight() / 2,
                    blocked,
                    true,
                    false
            );
        }

        ClientFeatureStore.FreeCameraCursorMode mode =
                ClientFeatureStore.getFreeCameraCursorMode();
        if (mode == ClientFeatureStore.FreeCameraCursorMode.HIDDEN) {
            return CursorPoint.hidden();
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 actualAimPoint = raycastClosest(
                minecraft, player, eye, eye.add(look.scale(MAX_RAY_DISTANCE)));
        ProjectedPoint projected = project(
                minecraft.gameRenderer.getMainCamera(), actualAimPoint, minecraft);
        if (!projected.visible()) {
            return CursorPoint.hidden();
        }

        return new CursorPoint(
                (int) Math.round(projected.x()),
                (int) Math.round(projected.y()),
                false,
                true,
                mode == ClientFeatureStore.FreeCameraCursorMode.DUAL
        );
    }

    /**
     * In Follow Aim without lock-on, converge the player's real eye/projectile
     * direction on the world point under the centered camera crosshair.
     */
    public static void alignFreeAim(Camera camera, LocalPlayer player) {
        // Contextual intentionally keeps the camera free and does not force the
        // player's ray onto the center of the screen. Converged Lock is aligned
        // by LockOnController using the shared stabilized lock point.
    }


    public static Vec3 resolvePlayerRayImpact(LocalPlayer player, double range) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) {
            return null;
        }
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F).normalize();
        return raycastClosest(minecraft, player, eye, eye.add(direction.scale(range)));
    }

    /** Returns the first block/entity hit by the center-screen camera ray. */
    public static AimSolution resolveCameraAim(Camera camera, LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        Vec3 cameraStart = camera.position();
        Vec3 cameraDirection = Vec3.directionFromRotation(
                camera.xRot(), camera.yRot()).normalize();
        Vec3 cameraEnd = cameraStart.add(cameraDirection.scale(MAX_RAY_DISTANCE));
        Vec3 cameraAimPoint = raycastClosest(
                minecraft, player, cameraStart, cameraEnd);

        Vec3 eye = player.getEyePosition(1.0F);
        HitResult firingBlockHit = minecraft.level.clip(new ClipContext(
                eye,
                cameraAimPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        boolean blocked = false;
        Vec3 actualImpactPoint = cameraAimPoint;
        if (firingBlockHit.getType() != HitResult.Type.MISS) {
            double hitDistanceSqr = eye.distanceToSqr(firingBlockHit.getLocation());
            double aimDistanceSqr = eye.distanceToSqr(cameraAimPoint);
            if (hitDistanceSqr + BLOCK_EPSILON_SQR < aimDistanceSqr) {
                blocked = true;
                actualImpactPoint = firingBlockHit.getLocation();
            }
        }

        return new AimSolution(cameraAimPoint, actualImpactPoint, blocked);
    }

    /**
     * Dual cursor and Converged Lock acquire targets from the fixed center cursor.
     * Dynamic cursor acquires targets from the player's real eye/raycast direction.
     */
    public static Vec3 targetingOrigin(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (usesCameraCenterForTargeting(player)) {
            return minecraft.gameRenderer.getMainCamera().position();
        }
        return player.getEyePosition(1.0F);
    }

    public static Vec3 targetingDirection(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (usesCameraCenterForTargeting(player)) {
            Camera camera = minecraft.gameRenderer.getMainCamera();
            return Vec3.directionFromRotation(camera.xRot(), camera.yRot()).normalize();
        }
        return player.getViewVector(1.0F).normalize();
    }

    private static boolean usesCameraCenterForTargeting(LocalPlayer player) {
        if (!ThirdPersonCameraController.isVisualCameraActive()) {
            return false;
        }
        if (ThirdPersonCameraController.isConvergedCameraSteeringActive()) {
            return true;
        }
        // During Temporary Free Look, Converged Lock temporarily behaves like
        // the selected free-camera cursor mode as well: Dual uses the fixed
        // screen center, while Dynamic uses the player's real eye ray.
        return ClientFeatureStore.getFreeCameraCursorMode()
                == ClientFeatureStore.FreeCameraCursorMode.DUAL;
    }


    private static boolean isFiringPathBlocked(
            Minecraft minecraft, LocalPlayer player, Vec3 aimPoint
    ) {
        Vec3 eye = player.getEyePosition(1.0F);
        HitResult hit = minecraft.level.clip(new ClipContext(
                eye, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hit.getType() != HitResult.Type.MISS
                && eye.distanceToSqr(hit.getLocation()) + BLOCK_EPSILON_SQR
                < eye.distanceToSqr(aimPoint);
    }

    private static boolean isUsableAimPoint(Vec3 point) {
        return point != null
                && Double.isFinite(point.x)
                && Double.isFinite(point.y)
                && Double.isFinite(point.z)
                && point.lengthSqr() > 1.0E-8D;
    }

    private static Vec3 raycastClosest(
            Minecraft minecraft,
            LocalPlayer player,
            Vec3 start,
            Vec3 end
    ) {
        HitResult blockHit = minecraft.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 clippedEnd = blockHit.getType() == HitResult.Type.MISS
                ? end
                : blockHit.getLocation();
        double closestDistanceSq = start.distanceToSqr(clippedEnd);

        AABB searchBox = new AABB(start, clippedEnd).inflate(1.0D);
        Vec3 closestPoint = null;
        for (Entity entity : minecraft.level.getEntities(
                player,
                searchBox,
                candidate -> candidate != player
                        && !candidate.isSpectator()
                        && candidate.isPickable()
        )) {
            AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            java.util.Optional<Vec3> intersection = hitBox.clip(start, clippedEnd);
            if (intersection.isEmpty()) {
                continue;
            }

            double distanceSq = start.distanceToSqr(intersection.get());
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestPoint = intersection.get();
            }
        }

        return closestPoint != null ? closestPoint : clippedEnd;
    }

    private static ProjectedPoint project(Camera camera, Vec3 point, Minecraft minecraft) {
        Vec3 delta = point.subtract(camera.position());
        Vec3 forward = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        double depth = delta.dot(forward);
        if (depth <= 0.02D) {
            return ProjectedPoint.hidden();
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        double fov = Mth.clamp(minecraft.options.fov().get(), 30, 110);
        double focal = (height * 0.5D) / Math.tan(Math.toRadians(fov * 0.5D));
        double x = width * 0.5D + delta.dot(right) * focal / depth;
        double y = height * 0.5D - delta.dot(up) * focal / depth;

        int margin = 4;
        x = Mth.clamp(x, margin, width - margin);
        y = Mth.clamp(y, margin, height - margin);
        return new ProjectedPoint(x, y, true);
    }

    public record CursorPoint(
            int x,
            int y,
            boolean blocked,
            boolean visible,
            boolean showCameraCenter
    ) {
        static CursorPoint hidden() {
            return new CursorPoint(0, 0, false, false, false);
        }
    }

    public record AimSolution(
            Vec3 cameraAimPoint,
            Vec3 actualImpactPoint,
            boolean blocked
    ) {}

    private record ProjectedPoint(double x, double y, boolean visible) {
        static ProjectedPoint hidden() {
            return new ProjectedPoint(0.0D, 0.0D, false);
        }
    }
}
