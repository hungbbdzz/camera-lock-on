package com.velorise.cameralockon;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TargetingRules {
    private static final Map<EntityType<?>, String> TYPE_ID_CACHE = new ConcurrentHashMap<>();
    private static List<? extends String> lastBlacklistSource;
    private static Set<String> cachedBlacklist = Collections.emptySet();
    private static List<? extends String> lastBossListSource;
    private static Set<String> cachedBossTypes = Collections.emptySet();

    private TargetingRules() {
    }

    private static String getTypeIdString(EntityType<?> type) {
        if (type == null) {
            return "";
        }
        return TYPE_ID_CACHE.computeIfAbsent(type, t -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            return id == null ? "" : id.toString();
        });
    }

    public static boolean isEligible(
            LocalPlayer player,
            LivingEntity target,
            boolean requireLineOfSight,
            EntityType<?> temporaryPinnedType,
            double maximumRange
    ) {
        if (target == player || target == player.getVehicle() || !target.isAlive() || target.isRemoved()) {
            return false;
        }

        if (target instanceof Player targetPlayer && targetPlayer.isSpectator()) {
            return false;
        }

        if (target.isInvisibleTo(player) && !target.isCurrentlyGlowing()) {
            return false;
        }

        if (player.distanceToSqr(target) > maximumRange * maximumRange) {
            return false;
        }

        if (isBlacklisted(target.getType())) {
            return false;
        }

        boolean pinnedMatch = temporaryPinnedType != null && target.getType() == temporaryPinnedType;
        CameraLockOnConfig.TemporaryPinMode pinMode = CameraLockOnConfig.TemporaryPinMode.fromConfig(
                CameraLockOnConfig.TEMPORARY_PIN_MODE.get()
        );

        if (temporaryPinnedType != null
                && pinMode == CameraLockOnConfig.TemporaryPinMode.SELECTED_ONLY
                && !pinnedMatch) {
            return false;
        }

        CameraLockOnConfig.TargetTypeMode targetTypeMode = CameraLockOnConfig.TargetTypeMode.fromConfig(
                CameraLockOnConfig.TARGET_TYPE_MODE.get()
        );
        boolean selectedMatch = matchesConfiguredSelectedType(target);

        if (temporaryPinnedType == null
                && targetTypeMode == CameraLockOnConfig.TargetTypeMode.SELECTED_ONLY
                && !selectedMatch) {
            return false;
        }

        boolean explicitTypeOverride = pinnedMatch
                || (targetTypeMode != CameraLockOnConfig.TargetTypeMode.ANY && selectedMatch);

        if (!explicitTypeOverride
                && target instanceof OwnableEntity ownable
                && ownable.getOwner() == player) {
            return false;
        }

        if (CameraLockOnConfig.HOSTILE_ONLY.get()
                && !explicitTypeOverride
                && !isHostile(target, player)) {
            return false;
        }

        return !requireLineOfSight || player.hasLineOfSight(target);
    }

    public static boolean isHostile(LivingEntity target, LocalPlayer player) {
        if (target instanceof Player targetPlayer) {
            return !targetPlayer.isCreative()
                    && !targetPlayer.isSpectator()
                    && !player.isAlliedTo(targetPlayer);
        }
        if (target instanceof Enemy) {
            return true;
        }
        if (target.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }
        if (target instanceof NeutralMob neutral && neutral.isAngry()) {
            return true;
        }
        return target instanceof Mob mob && mob.getTarget() == player;
    }

    public static boolean matchesConfiguredSelectedType(LivingEntity target) {
        String configuredId = CameraLockOnConfig.SELECTED_ENTITY_TYPE.get();
        if (configuredId == null || configuredId.isBlank()) {
            return false;
        }
        String actualId = getTypeIdString(target.getType());
        return !actualId.isEmpty() && configuredId.equals(actualId);
    }

    public static boolean isBlacklisted(EntityType<?> type) {
        String value = getTypeIdString(type);
        if (value.isEmpty()) {
            return false;
        }
        List<? extends String> current = CameraLockOnConfig.ENTITY_BLACKLIST.get();
        if (current != lastBlacklistSource) {
            lastBlacklistSource = current;
            cachedBlacklist = current == null || current.isEmpty()
                    ? Collections.emptySet()
                    : new HashSet<>(current);
        }
        return cachedBlacklist.contains(value);
    }

    public static boolean isBoss(EntityType<?> type) {
        String value = getTypeIdString(type);
        if (value.isEmpty()) {
            return false;
        }
        List<? extends String> current = CameraLockOnConfig.BOSS_ENTITY_TYPES.get();
        if (current != lastBossListSource) {
            lastBossListSource = current;
            cachedBossTypes = current == null || current.isEmpty()
                    ? Collections.emptySet()
                    : new HashSet<>(current);
        }
        return cachedBossTypes.contains(value);
    }

    public static Vec3 toWorldAimPoint(
            LivingEntity target,
            Vec3 observerPosition,
            double entityX,
            double entityY,
            double entityZ
    ) {
        AimPoint point = resolveAimPoint(target);
        double normalizedX = Mth.clamp(point.x(), -1.0D, 1.0D);
        double normalizedY = Mth.clamp(point.y(), 0.0D, 1.0D);
        double worldY = entityY + target.getBbHeight() * normalizedY;

        double directionX = entityX - observerPosition.x;
        double directionZ = entityZ - observerPosition.z;
        double horizontalLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (horizontalLength < 0.0001D) {
            return new Vec3(entityX, worldY, entityZ);
        }

        double rightX = -directionZ / horizontalLength;
        double rightZ = directionX / horizontalLength;
        double horizontalOffset = normalizedX * target.getBbWidth() * 0.5D;
        return new Vec3(
                entityX + rightX * horizontalOffset,
                worldY,
                entityZ + rightZ * horizontalOffset
        );
    }

    public static AimPoint resolveAimPoint(LivingEntity target) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (id != null) {
            EntityAimPointStore.Entry custom = EntityAimPointStore.get(id.toString());
            if (custom != null) {
                return new AimPoint(custom.horizontal(), custom.vertical());
            }
        }

        CameraLockOnConfig.AimPreset preset = CameraLockOnConfig.AimPreset.fromConfig(
                CameraLockOnConfig.AIM_PRESET.get()
        );
        if (preset != CameraLockOnConfig.AimPreset.CUSTOM) {
            return new AimPoint(preset.getX(), preset.getY());
        }

        return new AimPoint(
                Mth.clamp(CameraLockOnConfig.AIM_POINT_X.get(), -1.0D, 1.0D),
                Mth.clamp(CameraLockOnConfig.AIM_POINT_Y.get(), 0.0D, 1.0D)
        );
    }

    public static AimPoint findPerEntityAimPoint(String entityId) {
        EntityAimPointStore.Entry entry = EntityAimPointStore.get(entityId);
        return entry == null ? null : new AimPoint(entry.horizontal(), entry.vertical());
    }

    public static boolean allowsGroupAimOffset(LivingEntity target) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (id == null) {
            return true;
        }
        EntityAimPointStore.Entry entry = EntityAimPointStore.get(id.toString());
        return entry == null || entry.allowGroupAimOffset();
    }

    public static LinkedHashMap<String, AimPoint> parseAimPointMap(List<? extends String> entries) {
        LinkedHashMap<String, AimPoint> result = new LinkedHashMap<>();
        for (String entry : entries) {
            ParsedAimPoint parsed = parseAimPointEntry(entry);
            if (parsed != null) {
                result.put(parsed.entityId(), parsed.point());
            }
        }
        return result;
    }

    public static List<String> formatAimPointMap(Map<String, AimPoint> points) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, AimPoint> entry : points.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || entry.getValue() == null) {
                continue;
            }
            AimPoint point = entry.getValue();
            result.add(String.format(
                    Locale.ROOT,
                    "%s|%.4f|%.4f",
                    id,
                    Mth.clamp(point.x(), -1.0D, 1.0D),
                    Mth.clamp(point.y(), 0.0D, 1.0D)
            ));
        }
        return result;
    }

    public static String getEntityDisplayName(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return entityId == null || entityId.isBlank() ? "None" : entityId;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription().getString();
    }

    private static ParsedAimPoint parseAimPointEntry(String entry) {
        if (entry == null) {
            return null;
        }
        String[] parts = entry.split("\\|", -1);
        if (parts.length != 3 || ResourceLocation.tryParse(parts[0]) == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            return new ParsedAimPoint(
                    parts[0],
                    new AimPoint(Mth.clamp(x, -1.0D, 1.0D), Mth.clamp(y, 0.0D, 1.0D))
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record AimPoint(double x, double y) {
    }

    private record ParsedAimPoint(String entityId, AimPoint point) {
    }
}
