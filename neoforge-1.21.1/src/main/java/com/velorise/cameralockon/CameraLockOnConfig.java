package com.velorise.cameralockon;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

public final class CameraLockOnConfig {
    public static final double DEFAULT_LOCK_ON_RANGE = 36.0D;
    public static final boolean DEFAULT_SMART_LOCK = true;
    public static final boolean DEFAULT_SHOW_RETICLE = true;
    public static final String DEFAULT_RETICLE_COLOR = "Cyan";
    public static final double DEFAULT_RETICLE_OPACITY = 0.90D;
    public static final boolean DEFAULT_AUTO_RETARGET = true;
    public static final boolean DEFAULT_HOSTILE_ONLY = false;
    public static final AimPreset DEFAULT_AIM_PRESET = AimPreset.CENTER;
    public static final double DEFAULT_AIM_POINT_X = 0.0D;
    public static final double DEFAULT_AIM_POINT_Y = 0.0D;
    public static final double DEFAULT_LOST_TARGET_GRACE = 3.0D;
    public static final boolean DEFAULT_LOCK_SOUNDS = true;
    public static final double DEFAULT_SOUND_VOLUME = 0.67D;

    public static final boolean DEFAULT_DEAD_ZONE = false;
    public static final double DEFAULT_DEAD_ZONE_HORIZONTAL = 7.0D;
    public static final double DEFAULT_DEAD_ZONE_VERTICAL = 6.0D;
    public static final boolean DEFAULT_SUSPEND_USING_ITEM = true;
    public static final boolean DEFAULT_SUSPEND_MINING = true;
    public static final boolean DEFAULT_SUSPEND_RIDING = true;
    public static final boolean DEFAULT_SUSPEND_ELYTRA = true;

    public static final boolean DEFAULT_AUTO_LOCK = true;
    public static final double DEFAULT_AUTO_LOCK_DELAY = 0.35D;
    public static final double DEFAULT_AUTO_LOCK_COOLDOWN = 0.75D;
    public static final boolean DEFAULT_AUTO_LOCK_INDICATOR = true;
    public static final LockOnHitMode DEFAULT_LOCK_ON_HIT_MODE = LockOnHitMode.WHEN_UNLOCKED;
    public static final TemporaryPinMode DEFAULT_TEMPORARY_PIN_MODE = TemporaryPinMode.SELECTED_ONLY;

    public static final TargetTypeMode DEFAULT_TARGET_TYPE_MODE = TargetTypeMode.ANY;
    public static final String DEFAULT_SELECTED_ENTITY_TYPE = "";
    public static final RetargetMode DEFAULT_RETARGET_MODE = RetargetMode.ANY;
    public static final boolean DEFAULT_PREFER_BOSSES = true;

    public static final boolean DEFAULT_TARGET_HUD = true;
    public static final boolean DEFAULT_HUD_SHOW_NAME = true;
    public static final boolean DEFAULT_HUD_SHOW_HEALTH = true;
    public static final boolean DEFAULT_HUD_SHOW_DISTANCE = true;
    public static final boolean DEFAULT_HUD_SHOW_ARMOR = true;
    public static final boolean DEFAULT_HUD_SHOW_MOD_NAME = false;
    public static final boolean DEFAULT_HUD_ANIMATE_DAMAGE = true;
    public static final HudStyle DEFAULT_HUD_STYLE = HudStyle.COMPACT;
    public static final HudPosition DEFAULT_HUD_POSITION = HudPosition.TOP_CENTER;
    public static final double DEFAULT_HUD_SCALE = 1.0D;
    public static final double DEFAULT_HUD_OPACITY = 0.50D;
    public static final double DEFAULT_HUD_ANCHOR_X = 0.50D;
    public static final double DEFAULT_HUD_ANCHOR_Y = 0.02D;
    public static final int DEFAULT_HUD_OFFSET_X = 0;
    public static final int DEFAULT_HUD_OFFSET_Y = 0;
    public static final boolean DEFAULT_SHOW_REGISTRY_IDS = false;

    public static final boolean DEFAULT_ATTACKER_INDICATOR = true;
    public static final AttackerResponse DEFAULT_ATTACKER_RESPONSE = AttackerResponse.INDICATOR_ONLY;
    public static final double DEFAULT_ATTACKER_INDICATOR_LIFETIME = 4.0D;
    public static final int DEFAULT_MAX_ATTACKER_INDICATORS = 3;
    public static final boolean DEFAULT_GROUP_ATTACKER_DIRECTIONS = false;
    public static final int DEFAULT_ATTACKER_REQUIRED_HITS = 2;
    public static final double DEFAULT_ATTACKER_HIT_WINDOW = 5.0D;
    public static final boolean DEFAULT_ATTACKER_REPLACE_TARGET = false;
    public static final double DEFAULT_ATTACKER_LOCK_RANGE = 32.0D;
    public static final double DEFAULT_ATTACKER_LOCK_PROTECTION = 1.5D;

    public static final boolean DEFAULT_GROUP_AIM = false;
    public static final GroupAimActivation DEFAULT_GROUP_AIM_ACTIVATION = GroupAimActivation.SWEEP_WEAPONS;
    public static final double DEFAULT_GROUP_AIM_RADIUS = 2.5D;
    public static final double DEFAULT_GROUP_AIM_MAX_DISTANCE = 5.0D;
    public static final int DEFAULT_GROUP_AIM_MAX_TARGETS = 4;
    public static final double DEFAULT_GROUP_AIM_STRENGTH = 0.65D;
    public static final double DEFAULT_GROUP_AIM_MAX_OFFSET = 1.25D;
    public static final boolean DEFAULT_GROUP_AIM_SAME_TYPE_ONLY = false;

    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.DoubleValue LOCK_ON_RANGE;
    public static final ModConfigSpec.BooleanValue SMART_LOCK;
    public static final ModConfigSpec.BooleanValue SHOW_RETICLE;
    public static final ModConfigSpec.ConfigValue<String> RETICLE_COLOR;
    public static final ModConfigSpec.DoubleValue RETICLE_OPACITY;
    public static final ModConfigSpec.BooleanValue AUTO_RETARGET;
    public static final ModConfigSpec.BooleanValue HOSTILE_ONLY;
    public static final ModConfigSpec.ConfigValue<String> AIM_PRESET;
    public static final ModConfigSpec.DoubleValue AIM_POINT_X;
    public static final ModConfigSpec.DoubleValue AIM_POINT_Y;
    public static final ModConfigSpec.DoubleValue LOST_TARGET_GRACE;
    public static final ModConfigSpec.BooleanValue LOCK_SOUNDS;
    public static final ModConfigSpec.DoubleValue SOUND_VOLUME;

    public static final ModConfigSpec.BooleanValue DEAD_ZONE;
    public static final ModConfigSpec.DoubleValue DEAD_ZONE_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue DEAD_ZONE_VERTICAL;
    public static final ModConfigSpec.BooleanValue SUSPEND_USING_ITEM;
    public static final ModConfigSpec.BooleanValue SUSPEND_MINING;
    public static final ModConfigSpec.BooleanValue SUSPEND_RIDING;
    public static final ModConfigSpec.BooleanValue SUSPEND_ELYTRA;

    public static final ModConfigSpec.BooleanValue AUTO_LOCK;
    public static final ModConfigSpec.DoubleValue AUTO_LOCK_DELAY;
    public static final ModConfigSpec.DoubleValue AUTO_LOCK_COOLDOWN;
    public static final ModConfigSpec.BooleanValue AUTO_LOCK_INDICATOR;
    public static final ModConfigSpec.ConfigValue<String> LOCK_ON_HIT_MODE;
    public static final ModConfigSpec.ConfigValue<String> TEMPORARY_PIN_MODE;

    public static final ModConfigSpec.ConfigValue<String> TARGET_TYPE_MODE;
    public static final ModConfigSpec.ConfigValue<String> SELECTED_ENTITY_TYPE;
    public static final ModConfigSpec.ConfigValue<String> RETARGET_MODE;
    public static final ModConfigSpec.BooleanValue PREFER_BOSSES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_HISTORY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOSS_ENTITY_TYPES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PER_ENTITY_AIM_POINTS;

    public static final ModConfigSpec.BooleanValue TARGET_HUD;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_NAME;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_HEALTH;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_DISTANCE;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_ARMOR;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_MOD_NAME;
    public static final ModConfigSpec.BooleanValue HUD_ANIMATE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<String> HUD_STYLE;
    public static final ModConfigSpec.ConfigValue<String> HUD_POSITION;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.DoubleValue HUD_OPACITY;
    public static final ModConfigSpec.DoubleValue HUD_ANCHOR_X;
    public static final ModConfigSpec.DoubleValue HUD_ANCHOR_Y;
    public static final ModConfigSpec.IntValue HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ModConfigSpec.BooleanValue SHOW_REGISTRY_IDS;

    public static final ModConfigSpec.BooleanValue ATTACKER_INDICATOR;
    public static final ModConfigSpec.ConfigValue<String> ATTACKER_RESPONSE;
    public static final ModConfigSpec.DoubleValue ATTACKER_INDICATOR_LIFETIME;
    public static final ModConfigSpec.IntValue MAX_ATTACKER_INDICATORS;
    public static final ModConfigSpec.BooleanValue GROUP_ATTACKER_DIRECTIONS;
    public static final ModConfigSpec.IntValue ATTACKER_REQUIRED_HITS;
    public static final ModConfigSpec.DoubleValue ATTACKER_HIT_WINDOW;
    public static final ModConfigSpec.BooleanValue ATTACKER_REPLACE_TARGET;
    public static final ModConfigSpec.DoubleValue ATTACKER_LOCK_RANGE;
    public static final ModConfigSpec.DoubleValue ATTACKER_LOCK_PROTECTION;

    public static final ModConfigSpec.BooleanValue GROUP_AIM;
    public static final ModConfigSpec.ConfigValue<String> GROUP_AIM_ACTIVATION;
    public static final ModConfigSpec.DoubleValue GROUP_AIM_RADIUS;
    public static final ModConfigSpec.DoubleValue GROUP_AIM_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue GROUP_AIM_MAX_TARGETS;
    public static final ModConfigSpec.DoubleValue GROUP_AIM_STRENGTH;
    public static final ModConfigSpec.DoubleValue GROUP_AIM_MAX_OFFSET;
    public static final ModConfigSpec.BooleanValue GROUP_AIM_SAME_TYPE_ONLY;

    private static final List<String> VALID_RETICLE_COLORS =
            List.of("Cyan", "Red", "Green", "Yellow", "Purple");

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Core Camera Lock-On Configuration").push("general");
        LOCK_ON_RANGE = builder.comment("Maximum lock-on range in blocks.")
                .defineInRange("lockOnRange", DEFAULT_LOCK_ON_RANGE, 5.0D, 128.0D);
        SMART_LOCK = builder.comment("Allows temporary free look before the camera returns to the target.")
                .define("smartLock", DEFAULT_SMART_LOCK);
        SHOW_RETICLE = builder.comment("Render the 3D lock-on reticle on the current target.")
                .define("showReticle", DEFAULT_SHOW_RETICLE);
        RETICLE_COLOR = builder.comment("Cyan, Red, Green, Yellow, or Purple.")
                .define("reticleColor", DEFAULT_RETICLE_COLOR, CameraLockOnConfig::isValidReticleColor);
        RETICLE_OPACITY = builder.comment("Opacity of the 3D lock-on reticle.")
                .defineInRange("reticleOpacity", DEFAULT_RETICLE_OPACITY, 0.10D, 1.0D);
        AUTO_RETARGET = builder.comment("Automatically finds another valid target when the current one is lost.")
                .define("autoRetarget", DEFAULT_AUTO_RETARGET);
        HOSTILE_ONLY = builder.comment("Only target hostile entities unless an entity type is explicitly selected or pinned.")
                .define("hostileOnly", DEFAULT_HOSTILE_ONLY);
        AIM_PRESET = builder.comment("FEET, LOWER_BODY, CENTER, CHEST, HEAD, or CUSTOM.")
                .define("aimPreset", DEFAULT_AIM_PRESET.name(), value -> isValidEnum(value, AimPreset.class));
        AIM_POINT_X = builder.comment("Custom horizontal point: -1 left, 0 center, 1 right.")
                .defineInRange("aimPointX", DEFAULT_AIM_POINT_X, -1.0D, 1.0D);
        AIM_POINT_Y = builder.comment("Custom vertical point: 0 bottom, 0.5 center, 1 top.")
                .defineInRange("aimPointY", DEFAULT_AIM_POINT_Y, 0.0D, 1.0D);
        LOST_TARGET_GRACE = builder.comment("Seconds to remember a target while line of sight is lost.")
                .defineInRange("lostTargetGraceSeconds", DEFAULT_LOST_TARGET_GRACE, 0.0D, 10.0D);
        LOCK_SOUNDS = builder.comment("Play feedback sounds when targets are locked, switched, pinned, or lost.")
                .define("lockSounds", DEFAULT_LOCK_SOUNDS);
        SOUND_VOLUME = builder.comment("Lock feedback sound volume.")
                .defineInRange("soundVolume", DEFAULT_SOUND_VOLUME, 0.0D, 1.0D);
        builder.pop();

        builder.comment("Camera Assistance and Suspension").push("camera");
        DEAD_ZONE = builder.comment("Do not move the camera while the target remains inside an elliptical center zone.")
                .define("deadZoneEnabled", DEFAULT_DEAD_ZONE);
        DEAD_ZONE_HORIZONTAL = builder.comment("Horizontal dead-zone radius in degrees.")
                .defineInRange("deadZoneHorizontalDegrees", DEFAULT_DEAD_ZONE_HORIZONTAL, 0.0D, 30.0D);
        DEAD_ZONE_VERTICAL = builder.comment("Vertical dead-zone radius in degrees.")
                .defineInRange("deadZoneVerticalDegrees", DEFAULT_DEAD_ZONE_VERTICAL, 0.0D, 20.0D);
        SUSPEND_USING_ITEM = builder.comment("Temporarily stop camera steering while using an item.")
                .define("suspendWhileUsingItem", DEFAULT_SUSPEND_USING_ITEM);
        SUSPEND_MINING = builder.comment("Temporarily stop camera steering while mining a block.")
                .define("suspendWhileMining", DEFAULT_SUSPEND_MINING);
        SUSPEND_RIDING = builder.comment("Temporarily stop camera steering while riding.")
                .define("suspendWhileRiding", DEFAULT_SUSPEND_RIDING);
        SUSPEND_ELYTRA = builder.comment("Temporarily stop camera steering while fall-flying.")
                .define("suspendWhileElytraFlying", DEFAULT_SUSPEND_ELYTRA);
        builder.pop();

        builder.comment("Automatic Acquisition").push("automatic_lock");
        AUTO_LOCK = builder.comment("Lock after the crosshair remains on the same living entity.")
                .define("enabled", DEFAULT_AUTO_LOCK);
        AUTO_LOCK_DELAY = builder.comment("Seconds the crosshair must remain on the same entity.")
                .defineInRange("delaySeconds", DEFAULT_AUTO_LOCK_DELAY, 0.05D, 3.0D);
        AUTO_LOCK_COOLDOWN = builder.comment("Seconds before Auto Lock can activate after a manual unlock.")
                .defineInRange("manualUnlockCooldownSeconds", DEFAULT_AUTO_LOCK_COOLDOWN, 0.0D, 5.0D);
        AUTO_LOCK_INDICATOR = builder.comment("Show Auto Lock acquisition progress near the crosshair.")
                .define("showProgressIndicator", DEFAULT_AUTO_LOCK_INDICATOR);
        LOCK_ON_HIT_MODE = builder.comment("OFF, WHEN_UNLOCKED, or ALWAYS_SWITCH.")
                .define("lockEntityYouHit", DEFAULT_LOCK_ON_HIT_MODE.name(), value -> isValidEnum(value, LockOnHitMode.class));
        TEMPORARY_PIN_MODE = builder.comment("How the temporary pinned type behaves: SELECTED_ONLY or PREFER_SELECTED.")
                .define("temporaryPinMode", DEFAULT_TEMPORARY_PIN_MODE.name(), value -> isValidEnum(value, TemporaryPinMode.class));
        builder.pop();

        builder.comment("Entity Type Filtering").push("entity_filter");
        TARGET_TYPE_MODE = builder.comment("ANY, SELECTED_ONLY, or PREFER_SELECTED.")
                .define("targetTypeMode", DEFAULT_TARGET_TYPE_MODE.name(), value -> isValidEnum(value, TargetTypeMode.class));
        SELECTED_ENTITY_TYPE = builder.comment("Selected living entity registry ID, for example minecraft:pig.")
                .define("selectedEntityType", DEFAULT_SELECTED_ENTITY_TYPE, CameraLockOnConfig::isValidEntityId);
        RETARGET_MODE = builder.comment("ANY, SAME_TYPE_FIRST, or SAME_TYPE_ONLY.")
                .define("retargetMode", DEFAULT_RETARGET_MODE.name(), value -> isValidEnum(value, RetargetMode.class));
        PREFER_BOSSES = builder.comment("Give configured boss entity types a targeting score bonus.")
                .define("preferBosses", DEFAULT_PREFER_BOSSES);
        ENTITY_HISTORY = builder.comment("Recently selected living entity IDs, newest first.")
                .defineListAllowEmpty("recentEntityTypes", List.of(), () -> "minecraft:pig", CameraLockOnConfig::isValidEntityId);
        ENTITY_BLACKLIST = builder.comment("Entity types that can never be locked.")
                .defineListAllowEmpty("blacklistedEntityTypes", List.of(), () -> "minecraft:bat", CameraLockOnConfig::isNonBlankEntityId);
        BOSS_ENTITY_TYPES = builder.comment("Entity types treated as bosses for priority scoring.")
                .defineListAllowEmpty(
                        "bossEntityTypes",
                        List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden"),
                        () -> "minecraft:ender_dragon",
                        CameraLockOnConfig::isNonBlankEntityId
                );
        PER_ENTITY_AIM_POINTS = builder.comment("Per-entity aim values formatted as registry_id|x|y.")
                .defineListAllowEmpty("perEntityAimPoints", List.of(), () -> "minecraft:zombie|0.0|0.85", CameraLockOnConfig::isValidAimPointEntry);
        builder.pop();

        builder.comment("Target HUD").push("hud");
        TARGET_HUD = builder.comment("Show a compact HUD for the currently locked target.")
                .define("enabled", DEFAULT_TARGET_HUD);
        HUD_SHOW_NAME = builder.define("showName", DEFAULT_HUD_SHOW_NAME);
        HUD_SHOW_HEALTH = builder.define("showHealth", DEFAULT_HUD_SHOW_HEALTH);
        HUD_SHOW_DISTANCE = builder.define("showDistance", DEFAULT_HUD_SHOW_DISTANCE);
        HUD_SHOW_ARMOR = builder.comment("Show target armor using vanilla armor icons. Values above 20 use one icon plus a number.")
                .define("showArmor", DEFAULT_HUD_SHOW_ARMOR);
        HUD_SHOW_MOD_NAME = builder.comment("Show the namespace/mod display name on the final HUD line.")
                .define("showModName", DEFAULT_HUD_SHOW_MOD_NAME);
        HUD_ANIMATE_DAMAGE = builder.comment("Flash recently lost hearts when the target takes damage.")
                .define("animateDamage", DEFAULT_HUD_ANIMATE_DAMAGE);
        HUD_STYLE = builder.comment("Legacy HUD style value retained for config compatibility; the HUD now uses per-row toggles.")
                .define("style", DEFAULT_HUD_STYLE.name(), value -> isValidEnum(value, HudStyle.class));
        HUD_POSITION = builder.comment("TOP_CENTER, TOP_LEFT, BELOW_CROSSHAIR, ABOVE_HOTBAR, or CUSTOM.")
                .define("position", DEFAULT_HUD_POSITION.name(), value -> isValidEnum(value, HudPosition.class));
        HUD_SCALE = builder.comment("Scale of the target HUD.")
                .defineInRange("scale", DEFAULT_HUD_SCALE, 0.60D, 1.60D);
        HUD_OPACITY = builder.comment("Target HUD panel opacity. Text and heart sprites remain fully opaque.")
                .defineInRange("opacity", DEFAULT_HUD_OPACITY, 0.05D, 1.0D);
        HUD_ANCHOR_X = builder.comment("Normalized horizontal Target HUD position from 0.0 to 1.0.")
                .defineInRange("anchorX", DEFAULT_HUD_ANCHOR_X, 0.0D, 1.0D);
        HUD_ANCHOR_Y = builder.comment("Normalized vertical Target HUD position from 0.0 to 1.0.")
                .defineInRange("anchorY", DEFAULT_HUD_ANCHOR_Y, 0.0D, 1.0D);
        HUD_OFFSET_X = builder.comment("Legacy horizontal HUD offset. Kept for old config compatibility.")
                .defineInRange("offsetX", DEFAULT_HUD_OFFSET_X, -1000, 1000);
        HUD_OFFSET_Y = builder.comment("Legacy vertical HUD offset. Kept for old config compatibility.")
                .defineInRange("offsetY", DEFAULT_HUD_OFFSET_Y, -1000, 1000);
        SHOW_REGISTRY_IDS = builder.comment("Show registry IDs in entity selectors and the target mini HUD.")
                .define("showRegistryIds", DEFAULT_SHOW_REGISTRY_IDS);
        builder.pop();

        builder.comment("Attacker Awareness").push("attacker_awareness");
        ATTACKER_INDICATOR = builder.comment("Show left/right indicators for recent off-screen attackers.")
                .define("indicatorEnabled", DEFAULT_ATTACKER_INDICATOR);
        ATTACKER_RESPONSE = builder.comment("OFF, INDICATOR_ONLY, SECOND_HIT, or IMMEDIATE.")
                .define("response", DEFAULT_ATTACKER_RESPONSE.name(), value -> isValidEnum(value, AttackerResponse.class));
        ATTACKER_INDICATOR_LIFETIME = builder.comment("Seconds attacker indicators remain visible after a hit.")
                .defineInRange("indicatorLifetimeSeconds", DEFAULT_ATTACKER_INDICATOR_LIFETIME, 0.5D, 10.0D);
        MAX_ATTACKER_INDICATORS = builder.comment("Maximum separate threat indicators.")
                .defineInRange("maximumIndicators", DEFAULT_MAX_ATTACKER_INDICATORS, 1, 6);
        GROUP_ATTACKER_DIRECTIONS = builder.comment("Group several attackers that are on the same side of the player.")
                .define("groupSimilarDirections", DEFAULT_GROUP_ATTACKER_DIRECTIONS);
        ATTACKER_REQUIRED_HITS = builder.comment("Hits required before SECOND_HIT response locks an attacker.")
                .defineInRange("requiredHits", DEFAULT_ATTACKER_REQUIRED_HITS, 2, 5);
        ATTACKER_HIT_WINDOW = builder.comment("Seconds in which repeated hits count toward automatic attacker lock.")
                .defineInRange("hitWindowSeconds", DEFAULT_ATTACKER_HIT_WINDOW, 1.0D, 15.0D);
        ATTACKER_REPLACE_TARGET = builder.comment("Allow an attacker to replace an existing valid lock target.")
                .define("replaceCurrentTarget", DEFAULT_ATTACKER_REPLACE_TARGET);
        ATTACKER_LOCK_RANGE = builder.comment("Maximum distance for automatic attacker locking.")
                .defineInRange("lockRange", DEFAULT_ATTACKER_LOCK_RANGE, 4.0D, 64.0D);
        ATTACKER_LOCK_PROTECTION = builder.comment("Seconds after an attacker lock during which another attacker cannot steal it.")
                .defineInRange("lockProtectionSeconds", DEFAULT_ATTACKER_LOCK_PROTECTION, 0.0D, 5.0D);
        builder.pop();

        builder.comment("Experimental Sweep and AOE Group Aim").push("group_aim");
        GROUP_AIM = builder.comment("Aim between a nearby cluster of valid targets.")
                .define("enabled", DEFAULT_GROUP_AIM);
        GROUP_AIM_ACTIVATION = builder.comment("SWEEP_WEAPONS or ALWAYS.")
                .define("activation", DEFAULT_GROUP_AIM_ACTIVATION.name(), value -> isValidEnum(value, GroupAimActivation.class));
        GROUP_AIM_RADIUS = builder.comment("Maximum distance from the primary target to include another entity.")
                .defineInRange("groupRadius", DEFAULT_GROUP_AIM_RADIUS, 0.5D, 6.0D);
        GROUP_AIM_MAX_DISTANCE = builder.comment("Maximum player distance for entities included in the group.")
                .defineInRange("maximumDistance", DEFAULT_GROUP_AIM_MAX_DISTANCE, 2.0D, 10.0D);
        GROUP_AIM_MAX_TARGETS = builder.comment("Maximum entities used to calculate the group center.")
                .defineInRange("maximumTargets", DEFAULT_GROUP_AIM_MAX_TARGETS, 2, 8);
        GROUP_AIM_STRENGTH = builder.comment("Blend from primary aim point toward the weighted group center.")
                .defineInRange("strength", DEFAULT_GROUP_AIM_STRENGTH, 0.0D, 1.0D);
        GROUP_AIM_MAX_OFFSET = builder.comment("Maximum blocks the group point may move away from the primary aim point.")
                .defineInRange("maximumOffset", DEFAULT_GROUP_AIM_MAX_OFFSET, 0.1D, 3.0D);
        GROUP_AIM_SAME_TYPE_ONLY = builder.comment("Only group entities of the same type as the primary target.")
                .define("sameTypeOnly", DEFAULT_GROUP_AIM_SAME_TYPE_ONLY);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    private CameraLockOnConfig() {
    }

    private static boolean isValidReticleColor(Object value) {
        if (!(value instanceof String color)) {
            return false;
        }
        return VALID_RETICLE_COLORS.stream().anyMatch(valid -> valid.equalsIgnoreCase(color));
    }

    private static boolean isValidEnum(Object value, Class<? extends Enum<?>> enumClass) {
        if (!(value instanceof String text)) {
            return false;
        }
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidEntityId(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        return text.isBlank() || ResourceLocation.tryParse(text) != null;
    }

    private static boolean isNonBlankEntityId(Object value) {
        return value instanceof String text && !text.isBlank() && ResourceLocation.tryParse(text) != null;
    }

    private static boolean isValidAimPointEntry(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        String[] parts = text.split("\\|", -1);
        if (parts.length != 3 || ResourceLocation.tryParse(parts[0]) == null) {
            return false;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            return x >= -1.0D && x <= 1.0D && y >= 0.0D && y <= 1.0D;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public interface DisplayEnum {
        Component getDisplayName();
    }

    private static <E extends Enum<E>> E parseEnum(String value, E fallback, Class<E> type) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E next(E value, Class<E> type) {
        E[] values = type.getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    public enum AimPreset implements DisplayEnum {
        FEET("Feet", 0.0D, 0.10D),
        LOWER_BODY("Lower Body", 0.0D, 0.30D),
        CENTER("Center", 0.0D, 0.50D),
        CHEST("Chest", 0.0D, 0.70D),
        HEAD("Head", 0.0D, 0.90D),
        CUSTOM("Custom", 0.0D, 0.50D);

        private final String displayName;
        private final double x;
        private final double y;

        AimPreset(String displayName, double x, double y) {
            this.displayName = displayName;
            this.x = x;
            this.y = y;
        }

        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.aim_preset." + name().toLowerCase(Locale.ROOT)); }
        public double getX() { return this.x; }
        public double getY() { return this.y; }
        public AimPreset next() { return CameraLockOnConfig.next(this, AimPreset.class); }
        public static AimPreset fromConfig(String value) { return parseEnum(value, DEFAULT_AIM_PRESET, AimPreset.class); }
    }

    public enum TargetTypeMode implements DisplayEnum {
        ANY("Any Entity"), SELECTED_ONLY("Selected Only"), PREFER_SELECTED("Prefer Selected");
        private final String displayName;
        TargetTypeMode(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.target_type_mode." + name().toLowerCase(Locale.ROOT)); }
        public TargetTypeMode next() { return CameraLockOnConfig.next(this, TargetTypeMode.class); }
        public static TargetTypeMode fromConfig(String value) { return parseEnum(value, DEFAULT_TARGET_TYPE_MODE, TargetTypeMode.class); }
    }

    public enum RetargetMode implements DisplayEnum {
        ANY("Any Entity"), SAME_TYPE_FIRST("Same Type First"), SAME_TYPE_ONLY("Same Type Only");
        private final String displayName;
        RetargetMode(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.retarget_mode." + name().toLowerCase(Locale.ROOT)); }
        public RetargetMode next() { return CameraLockOnConfig.next(this, RetargetMode.class); }
        public static RetargetMode fromConfig(String value) { return parseEnum(value, DEFAULT_RETARGET_MODE, RetargetMode.class); }
    }

    public enum LockOnHitMode implements DisplayEnum {
        OFF("Off"), WHEN_UNLOCKED("When Unlocked"), ALWAYS_SWITCH("Always Switch");
        private final String displayName;
        LockOnHitMode(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.lock_on_hit_mode." + name().toLowerCase(Locale.ROOT)); }
        public LockOnHitMode next() { return CameraLockOnConfig.next(this, LockOnHitMode.class); }
        public static LockOnHitMode fromConfig(String value) { return parseEnum(value, DEFAULT_LOCK_ON_HIT_MODE, LockOnHitMode.class); }
    }

    public enum TemporaryPinMode implements DisplayEnum {
        SELECTED_ONLY("Pinned Type Only"), PREFER_SELECTED("Prefer Pinned Type");
        private final String displayName;
        TemporaryPinMode(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.temporary_pin_mode." + name().toLowerCase(Locale.ROOT)); }
        public TemporaryPinMode next() { return CameraLockOnConfig.next(this, TemporaryPinMode.class); }
        public static TemporaryPinMode fromConfig(String value) { return parseEnum(value, DEFAULT_TEMPORARY_PIN_MODE, TemporaryPinMode.class); }
    }

    public enum AttackerResponse implements DisplayEnum {
        OFF("Off"), INDICATOR_ONLY("Indicator Only"), SECOND_HIT("Lock After Hits"), IMMEDIATE("Lock Immediately");
        private final String displayName;
        AttackerResponse(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.attacker_response." + name().toLowerCase(Locale.ROOT)); }
        public AttackerResponse next() { return CameraLockOnConfig.next(this, AttackerResponse.class); }
        public static AttackerResponse fromConfig(String value) { return parseEnum(value, DEFAULT_ATTACKER_RESPONSE, AttackerResponse.class); }
    }

    public enum GroupAimActivation implements DisplayEnum {
        SWEEP_WEAPONS("Sweep Weapons"), ALWAYS("Always");
        private final String displayName;
        GroupAimActivation(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.group_aim_activation." + name().toLowerCase(Locale.ROOT)); }
        public GroupAimActivation next() { return CameraLockOnConfig.next(this, GroupAimActivation.class); }
        public static GroupAimActivation fromConfig(String value) { return parseEnum(value, DEFAULT_GROUP_AIM_ACTIVATION, GroupAimActivation.class); }
    }

    public enum HudStyle implements DisplayEnum {
        COMPACT("Compact"), DETAILED("Detailed");
        private final String displayName;
        HudStyle(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.hud_style." + name().toLowerCase(Locale.ROOT)); }
        public HudStyle next() { return CameraLockOnConfig.next(this, HudStyle.class); }
        public static HudStyle fromConfig(String value) { return parseEnum(value, DEFAULT_HUD_STYLE, HudStyle.class); }
    }

    public enum HudPosition implements DisplayEnum {
        TOP_CENTER("Top Center"),
        TOP_LEFT("Top Left"),
        BELOW_CROSSHAIR("Below Crosshair"),
        ABOVE_HOTBAR("Above Hotbar"),
        CUSTOM("Custom");

        private final String displayName;
        HudPosition(String displayName) { this.displayName = displayName; }
        public Component getDisplayName() { return Component.translatable("gui.camera_lockon.enum.hud_position." + name().toLowerCase(Locale.ROOT)); }
        public HudPosition next() { return CameraLockOnConfig.next(this, HudPosition.class); }
        public static HudPosition fromConfig(String value) { return parseEnum(value, DEFAULT_HUD_POSITION, HudPosition.class); }
    }
}
