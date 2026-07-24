package com.velorise.cameralockon;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GroupAimCalculator {
    private GroupAimCalculator() {
    }

    public static GroupAimResult calculate(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity primaryTarget,
            Vec3 primaryAimPoint,
            EntityType<?> temporaryPinnedType
    ) {
        if (!CameraLockOnConfig.GROUP_AIM.get()
                || !TargetingRules.allowsGroupAimOffset(primaryTarget)) {
            return new GroupAimResult(primaryAimPoint, 1);
        }

        CameraLockOnConfig.GroupAimActivation activation =
                CameraLockOnConfig.GroupAimActivation.fromConfig(
                        CameraLockOnConfig.GROUP_AIM_ACTIVATION.get()
                );

        if (activation == CameraLockOnConfig.GroupAimActivation.SWEEP_WEAPONS
                && !supportsSweep(player.getMainHandItem())) {
            return new GroupAimResult(primaryAimPoint, 1);
        }

        double maximumDistance = CameraLockOnConfig.GROUP_AIM_MAX_DISTANCE.get();
        if (player.distanceTo(primaryTarget) > maximumDistance) {
            return new GroupAimResult(primaryAimPoint, 1);
        }

        double groupRadius = CameraLockOnConfig.GROUP_AIM_RADIUS.get();
        double groupRadiusSqr = groupRadius * groupRadius;
        int maximumTargets = CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.get();
        boolean sameTypeOnly = CameraLockOnConfig.GROUP_AIM_SAME_TYPE_ONLY.get();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        List<Candidate> candidates = new ArrayList<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living == primaryTarget) {
                continue;
            }

            if (sameTypeOnly && living.getType() != primaryTarget.getType()) {
                continue;
            }

            if (!TargetingRules.isEligible(
                    player,
                    living,
                    true,
                    temporaryPinnedType,
                    maximumDistance
            )) {
                continue;
            }

            if (living.distanceToSqr(primaryTarget) > groupRadiusSqr) {
                continue;
            }

            Vec3 toEntity = living.getBoundingBox().getCenter().subtract(eye);
            double length = toEntity.length();
            if (length <= 0.0001D || toEntity.scale(1.0D / length).dot(look) < 0.15D) {
                continue;
            }

            double playerDistance = player.distanceTo(living);
            double primaryDistance = living.distanceTo(primaryTarget);
            double sortingScore = playerDistance + primaryDistance * 0.35D;
            candidates.add(new Candidate(living, sortingScore));
        }

        candidates.sort(Comparator.comparingDouble(Candidate::sortingScore));
        int additionalCount = Math.min(Math.max(0, maximumTargets - 1), candidates.size());
        if (additionalCount <= 0) {
            return new GroupAimResult(primaryAimPoint, 1);
        }

        double weightedX = primaryAimPoint.x * 1.75D;
        double weightedY = primaryAimPoint.y * 1.75D;
        double weightedZ = primaryAimPoint.z * 1.75D;
        double totalWeight = 1.75D;

        for (int i = 0; i < additionalCount; i++) {
            LivingEntity target = candidates.get(i).entity();
            Vec3 point = TargetingRules.toWorldAimPoint(
                    target,
                    player.getEyePosition(),
                    target.getX(),
                    target.getY(),
                    target.getZ()
            );
            double weight = 1.0D / Math.max(0.75D, player.distanceTo(target));
            weightedX += point.x * weight;
            weightedY += point.y * weight;
            weightedZ += point.z * weight;
            totalWeight += weight;
        }

        Vec3 groupCenter = new Vec3(
                weightedX / totalWeight,
                weightedY / totalWeight,
                weightedZ / totalWeight
        );

        Vec3 offset = groupCenter.subtract(primaryAimPoint);
        double maximumOffset = CameraLockOnConfig.GROUP_AIM_MAX_OFFSET.get();
        if (offset.lengthSqr() > maximumOffset * maximumOffset) {
            offset = offset.normalize().scale(maximumOffset);
            groupCenter = primaryAimPoint.add(offset);
        }

        double strength = Mth.clamp(CameraLockOnConfig.GROUP_AIM_STRENGTH.get(), 0.0D, 1.0D);
        Vec3 effectivePoint = primaryAimPoint.lerp(groupCenter, strength);
        return new GroupAimResult(effectivePoint, additionalCount + 1);
    }

    private static boolean supportsSweep(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.canPerformAction(ToolActions.SWORD_SWEEP)) {
            return true;
        }
        return AoeWeaponStore.contains(stack.getItem());
    }

    public record GroupAimResult(Vec3 aimPoint, int targetCount) {
    }

    private record Candidate(LivingEntity entity, double sortingScore) {
    }
}
