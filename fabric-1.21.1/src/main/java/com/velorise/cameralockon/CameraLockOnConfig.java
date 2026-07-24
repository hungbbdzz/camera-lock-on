package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Loader-independent client configuration used by the Fabric and Forge ports.
 * The public value API deliberately mirrors ModConfigSpec's get/set pattern so
 * all existing screens and gameplay logic can be reused without loader code.
 */
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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static final List<String> VALID_RETICLE_COLORS =
            List.of("Cyan", "Red", "Green", "Yellow", "Purple");

    public static final ClientSpec CLIENT_SPEC = new ClientSpec();

    public static final DoubleValue LOCK_ON_RANGE = doubleValue("general.lockOnRange", DEFAULT_LOCK_ON_RANGE, 5.0D, 128.0D);
    public static final BooleanValue SMART_LOCK = booleanValue("general.smartLock", DEFAULT_SMART_LOCK);
    public static final BooleanValue SHOW_RETICLE = booleanValue("general.showReticle", DEFAULT_SHOW_RETICLE);
    public static final ConfigValue<String> RETICLE_COLOR = stringValue("general.reticleColor", DEFAULT_RETICLE_COLOR, CameraLockOnConfig::isValidReticleColor);
    public static final DoubleValue RETICLE_OPACITY = doubleValue("general.reticleOpacity", DEFAULT_RETICLE_OPACITY, 0.10D, 1.0D);
    public static final BooleanValue AUTO_RETARGET = booleanValue("general.autoRetarget", DEFAULT_AUTO_RETARGET);
    public static final BooleanValue HOSTILE_ONLY = booleanValue("general.hostileOnly", DEFAULT_HOSTILE_ONLY);
    public static final ConfigValue<String> AIM_PRESET = stringValue("general.aimPreset", DEFAULT_AIM_PRESET.name(), value -> isValidEnum(value, AimPreset.class));
    public static final DoubleValue AIM_POINT_X = doubleValue("general.aimPointX", DEFAULT_AIM_POINT_X, -1.0D, 1.0D);
    public static final DoubleValue AIM_POINT_Y = doubleValue("general.aimPointY", DEFAULT_AIM_POINT_Y, 0.0D, 1.0D);
    public static final DoubleValue LOST_TARGET_GRACE = doubleValue("general.lostTargetGraceSeconds", DEFAULT_LOST_TARGET_GRACE, 0.0D, 10.0D);
    public static final BooleanValue LOCK_SOUNDS = booleanValue("general.lockSounds", DEFAULT_LOCK_SOUNDS);
    public static final DoubleValue SOUND_VOLUME = doubleValue("general.soundVolume", DEFAULT_SOUND_VOLUME, 0.0D, 1.0D);

    public static final BooleanValue DEAD_ZONE = booleanValue("camera.deadZoneEnabled", DEFAULT_DEAD_ZONE);
    public static final DoubleValue DEAD_ZONE_HORIZONTAL = doubleValue("camera.deadZoneHorizontalDegrees", DEFAULT_DEAD_ZONE_HORIZONTAL, 0.0D, 30.0D);
    public static final DoubleValue DEAD_ZONE_VERTICAL = doubleValue("camera.deadZoneVerticalDegrees", DEFAULT_DEAD_ZONE_VERTICAL, 0.0D, 20.0D);
    public static final BooleanValue SUSPEND_USING_ITEM = booleanValue("camera.suspendWhileUsingItem", DEFAULT_SUSPEND_USING_ITEM);
    public static final BooleanValue SUSPEND_MINING = booleanValue("camera.suspendWhileMining", DEFAULT_SUSPEND_MINING);
    public static final BooleanValue SUSPEND_RIDING = booleanValue("camera.suspendWhileRiding", DEFAULT_SUSPEND_RIDING);
    public static final BooleanValue SUSPEND_ELYTRA = booleanValue("camera.suspendWhileElytraFlying", DEFAULT_SUSPEND_ELYTRA);

    public static final BooleanValue AUTO_LOCK = booleanValue("automatic_lock.enabled", DEFAULT_AUTO_LOCK);
    public static final DoubleValue AUTO_LOCK_DELAY = doubleValue("automatic_lock.delaySeconds", DEFAULT_AUTO_LOCK_DELAY, 0.05D, 3.0D);
    public static final DoubleValue AUTO_LOCK_COOLDOWN = doubleValue("automatic_lock.manualUnlockCooldownSeconds", DEFAULT_AUTO_LOCK_COOLDOWN, 0.0D, 5.0D);
    public static final BooleanValue AUTO_LOCK_INDICATOR = booleanValue("automatic_lock.showProgressIndicator", DEFAULT_AUTO_LOCK_INDICATOR);
    public static final ConfigValue<String> LOCK_ON_HIT_MODE = stringValue("automatic_lock.lockEntityYouHit", DEFAULT_LOCK_ON_HIT_MODE.name(), value -> isValidEnum(value, LockOnHitMode.class));
    public static final ConfigValue<String> TEMPORARY_PIN_MODE = stringValue("automatic_lock.temporaryPinMode", DEFAULT_TEMPORARY_PIN_MODE.name(), value -> isValidEnum(value, TemporaryPinMode.class));

    public static final ConfigValue<String> TARGET_TYPE_MODE = stringValue("entity_filter.targetTypeMode", DEFAULT_TARGET_TYPE_MODE.name(), value -> isValidEnum(value, TargetTypeMode.class));
    public static final ConfigValue<String> SELECTED_ENTITY_TYPE = stringValue("entity_filter.selectedEntityType", DEFAULT_SELECTED_ENTITY_TYPE, CameraLockOnConfig::isValidEntityId);
    public static final ConfigValue<String> RETARGET_MODE = stringValue("entity_filter.retargetMode", DEFAULT_RETARGET_MODE.name(), value -> isValidEnum(value, RetargetMode.class));
    public static final BooleanValue PREFER_BOSSES = booleanValue("entity_filter.preferBosses", DEFAULT_PREFER_BOSSES);
    public static final ConfigValue<List<? extends String>> ENTITY_HISTORY = listValue("entity_filter.recentEntityTypes", List.of(), CameraLockOnConfig::isValidEntityId);
    public static final ConfigValue<List<? extends String>> ENTITY_BLACKLIST = listValue("entity_filter.blacklistedEntityTypes", List.of(), CameraLockOnConfig::isNonBlankEntityId);
    public static final ConfigValue<List<? extends String>> BOSS_ENTITY_TYPES = listValue("entity_filter.bossEntityTypes", List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden"), CameraLockOnConfig::isNonBlankEntityId);
    public static final ConfigValue<List<? extends String>> PER_ENTITY_AIM_POINTS = listValue("entity_filter.perEntityAimPoints", List.of(), CameraLockOnConfig::isValidAimPointEntry);

    public static final BooleanValue TARGET_HUD = booleanValue("hud.enabled", DEFAULT_TARGET_HUD);
    public static final BooleanValue HUD_SHOW_NAME = booleanValue("hud.showName", DEFAULT_HUD_SHOW_NAME);
    public static final BooleanValue HUD_SHOW_HEALTH = booleanValue("hud.showHealth", DEFAULT_HUD_SHOW_HEALTH);
    public static final BooleanValue HUD_SHOW_DISTANCE = booleanValue("hud.showDistance", DEFAULT_HUD_SHOW_DISTANCE);
    public static final BooleanValue HUD_SHOW_ARMOR = booleanValue("hud.showArmor", DEFAULT_HUD_SHOW_ARMOR);
    public static final BooleanValue HUD_SHOW_MOD_NAME = booleanValue("hud.showModName", DEFAULT_HUD_SHOW_MOD_NAME);
    public static final BooleanValue HUD_ANIMATE_DAMAGE = booleanValue("hud.animateDamage", DEFAULT_HUD_ANIMATE_DAMAGE);
    public static final ConfigValue<String> HUD_STYLE = stringValue("hud.style", DEFAULT_HUD_STYLE.name(), value -> isValidEnum(value, HudStyle.class));
    public static final ConfigValue<String> HUD_POSITION = stringValue("hud.position", DEFAULT_HUD_POSITION.name(), value -> isValidEnum(value, HudPosition.class));
    public static final DoubleValue HUD_SCALE = doubleValue("hud.scale", DEFAULT_HUD_SCALE, 0.60D, 1.60D);
    public static final DoubleValue HUD_OPACITY = doubleValue("hud.opacity", DEFAULT_HUD_OPACITY, 0.05D, 1.0D);
    public static final DoubleValue HUD_ANCHOR_X = doubleValue("hud.anchorX", DEFAULT_HUD_ANCHOR_X, 0.0D, 1.0D);
    public static final DoubleValue HUD_ANCHOR_Y = doubleValue("hud.anchorY", DEFAULT_HUD_ANCHOR_Y, 0.0D, 1.0D);
    public static final IntValue HUD_OFFSET_X = intValue("hud.offsetX", DEFAULT_HUD_OFFSET_X, -1000, 1000);
    public static final IntValue HUD_OFFSET_Y = intValue("hud.offsetY", DEFAULT_HUD_OFFSET_Y, -1000, 1000);
    public static final BooleanValue SHOW_REGISTRY_IDS = booleanValue("hud.showRegistryIds", DEFAULT_SHOW_REGISTRY_IDS);

    public static final BooleanValue ATTACKER_INDICATOR = booleanValue("attacker_awareness.indicatorEnabled", DEFAULT_ATTACKER_INDICATOR);
    public static final ConfigValue<String> ATTACKER_RESPONSE = stringValue("attacker_awareness.response", DEFAULT_ATTACKER_RESPONSE.name(), value -> isValidEnum(value, AttackerResponse.class));
    public static final DoubleValue ATTACKER_INDICATOR_LIFETIME = doubleValue("attacker_awareness.indicatorLifetimeSeconds", DEFAULT_ATTACKER_INDICATOR_LIFETIME, 0.5D, 10.0D);
    public static final IntValue MAX_ATTACKER_INDICATORS = intValue("attacker_awareness.maximumIndicators", DEFAULT_MAX_ATTACKER_INDICATORS, 1, 6);
    public static final BooleanValue GROUP_ATTACKER_DIRECTIONS = booleanValue("attacker_awareness.groupSimilarDirections", DEFAULT_GROUP_ATTACKER_DIRECTIONS);
    public static final IntValue ATTACKER_REQUIRED_HITS = intValue("attacker_awareness.requiredHits", DEFAULT_ATTACKER_REQUIRED_HITS, 2, 5);
    public static final DoubleValue ATTACKER_HIT_WINDOW = doubleValue("attacker_awareness.hitWindowSeconds", DEFAULT_ATTACKER_HIT_WINDOW, 1.0D, 15.0D);
    public static final BooleanValue ATTACKER_REPLACE_TARGET = booleanValue("attacker_awareness.replaceCurrentTarget", DEFAULT_ATTACKER_REPLACE_TARGET);
    public static final DoubleValue ATTACKER_LOCK_RANGE = doubleValue("attacker_awareness.lockRange", DEFAULT_ATTACKER_LOCK_RANGE, 4.0D, 64.0D);
    public static final DoubleValue ATTACKER_LOCK_PROTECTION = doubleValue("attacker_awareness.lockProtectionSeconds", DEFAULT_ATTACKER_LOCK_PROTECTION, 0.0D, 5.0D);

    public static final BooleanValue GROUP_AIM = booleanValue("group_aim.enabled", DEFAULT_GROUP_AIM);
    public static final ConfigValue<String> GROUP_AIM_ACTIVATION = stringValue("group_aim.activation", DEFAULT_GROUP_AIM_ACTIVATION.name(), value -> isValidEnum(value, GroupAimActivation.class));
    public static final DoubleValue GROUP_AIM_RADIUS = doubleValue("group_aim.groupRadius", DEFAULT_GROUP_AIM_RADIUS, 0.5D, 6.0D);
    public static final DoubleValue GROUP_AIM_MAX_DISTANCE = doubleValue("group_aim.maximumDistance", DEFAULT_GROUP_AIM_MAX_DISTANCE, 2.0D, 10.0D);
    public static final IntValue GROUP_AIM_MAX_TARGETS = intValue("group_aim.maximumTargets", DEFAULT_GROUP_AIM_MAX_TARGETS, 2, 8);
    public static final DoubleValue GROUP_AIM_STRENGTH = doubleValue("group_aim.strength", DEFAULT_GROUP_AIM_STRENGTH, 0.0D, 1.0D);
    public static final DoubleValue GROUP_AIM_MAX_OFFSET = doubleValue("group_aim.maximumOffset", DEFAULT_GROUP_AIM_MAX_OFFSET, 0.1D, 3.0D);
    public static final BooleanValue GROUP_AIM_SAME_TYPE_ONLY = booleanValue("group_aim.sameTypeOnly", DEFAULT_GROUP_AIM_SAME_TYPE_ONLY);

    private CameraLockOnConfig() {
    }

    public static void initialize(Path configFile) {
        CLIENT_SPEC.initialize(configFile);
    }

    private static BooleanValue booleanValue(String key, boolean defaultValue) {
        BooleanValue value = new BooleanValue(key, defaultValue);
        VALUES.put(key, value);
        return value;
    }

    private static DoubleValue doubleValue(String key, double defaultValue, double minimum, double maximum) {
        DoubleValue value = new DoubleValue(key, defaultValue, minimum, maximum);
        VALUES.put(key, value);
        return value;
    }

    private static IntValue intValue(String key, int defaultValue, int minimum, int maximum) {
        IntValue value = new IntValue(key, defaultValue, minimum, maximum);
        VALUES.put(key, value);
        return value;
    }

    private static ConfigValue<String> stringValue(String key, String defaultValue, Predicate<Object> validator) {
        ConfigValue<String> value = new ConfigValue<>(
                key,
                defaultValue,
                element -> element.isJsonPrimitive() ? element.getAsString() : defaultValue,
                text -> GSON.toJsonTree(text),
                candidate -> validator.test(candidate) ? candidate : defaultValue
        );
        VALUES.put(key, value);
        return value;
    }

    private static ConfigValue<List<? extends String>> listValue(
            String key,
            List<String> defaultValue,
            Predicate<Object> elementValidator
    ) {
        List<? extends String> immutableDefault = List.copyOf(defaultValue);
        ConfigValue<List<? extends String>> value = new ConfigValue<>(
                key,
                immutableDefault,
                element -> {
                    if (!element.isJsonArray()) {
                        return immutableDefault;
                    }
                    List<String> result = new ArrayList<>();
                    for (JsonElement child : element.getAsJsonArray()) {
                        if (child.isJsonPrimitive()) {
                            String text = child.getAsString();
                            if (elementValidator.test(text)) {
                                result.add(text);
                            }
                        }
                    }
                    return List.copyOf(result);
                },
                list -> {
                    JsonArray array = new JsonArray();
                    for (String text : list) {
                        if (elementValidator.test(text)) {
                            array.add(text);
                        }
                    }
                    return array;
                },
                list -> {
                    List<String> result = new ArrayList<>();
                    for (String text : list) {
                        if (elementValidator.test(text)) {
                            result.add(text);
                        }
                    }
                    return List.copyOf(result);
                }
        );
        VALUES.put(key, value);
        return value;
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

    public static class ConfigValue<T> {
        private final String key;
        private final T defaultValue;
        private final Function<JsonElement, T> decoder;
        private final Function<T, JsonElement> encoder;
        private final UnaryOperator<T> normalizer;
        private T value;

        protected ConfigValue(
                String key,
                T defaultValue,
                Function<JsonElement, T> decoder,
                Function<T, JsonElement> encoder,
                UnaryOperator<T> normalizer
        ) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.decoder = decoder;
            this.encoder = encoder;
            this.normalizer = normalizer;
            this.value = defaultValue;
        }

        public T get() {
            return this.value;
        }

        public void set(T value) {
            this.value = this.normalizer.apply(value);
        }

        public T getDefault() {
            return this.defaultValue;
        }

        private void reset() {
            this.value = this.defaultValue;
        }

        private void read(JsonElement element) {
            try {
                this.value = this.normalizer.apply(this.decoder.apply(element));
            } catch (RuntimeException ignored) {
                reset();
            }
        }

        private JsonElement write() {
            return this.encoder.apply(this.value);
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        private BooleanValue(String key, boolean defaultValue) {
            super(
                    key,
                    defaultValue,
                    element -> element.isJsonPrimitive() ? element.getAsBoolean() : defaultValue,
                    GSON::toJsonTree,
                    value -> value != null ? value : defaultValue
            );
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private DoubleValue(String key, double defaultValue, double minimum, double maximum) {
            super(
                    key,
                    defaultValue,
                    element -> element.isJsonPrimitive() ? element.getAsDouble() : defaultValue,
                    GSON::toJsonTree,
                    value -> Math.max(minimum, Math.min(maximum, value == null ? defaultValue : value))
            );
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private IntValue(String key, int defaultValue, int minimum, int maximum) {
            super(
                    key,
                    defaultValue,
                    element -> element.isJsonPrimitive() ? element.getAsInt() : defaultValue,
                    GSON::toJsonTree,
                    value -> Math.max(minimum, Math.min(maximum, value == null ? defaultValue : value))
            );
        }
    }

    public static final class ClientSpec {
        private Path configFile;

        private void initialize(Path file) {
            this.configFile = file;
            load();
        }

        public synchronized void load() {
            for (ConfigValue<?> value : VALUES.values()) {
                value.reset();
            }
            if (this.configFile == null || !Files.isRegularFile(this.configFile)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(this.configFile, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    return;
                }
                JsonObject object = root.getAsJsonObject();
                for (Map.Entry<String, ConfigValue<?>> entry : VALUES.entrySet()) {
                    JsonElement element = object.get(entry.getKey());
                    if (element != null) {
                        entry.getValue().read(element);
                    }
                }
            } catch (IOException | RuntimeException exception) {
                System.err.println("[Camera Lock-On] Failed to load config: " + exception.getMessage());
            }
        }

        public synchronized void save() {
            if (this.configFile == null) {
                return;
            }
            try {
                Files.createDirectories(this.configFile.getParent());
                JsonObject root = new JsonObject();
                for (Map.Entry<String, ConfigValue<?>> entry : VALUES.entrySet()) {
                    root.add(entry.getKey(), entry.getValue().write());
                }
                Path temporary = this.configFile.resolveSibling(this.configFile.getFileName() + ".tmp");
                try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    GSON.toJson(root, writer);
                }
                try {
                    Files.move(temporary, this.configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException unsupportedAtomicMove) {
                    Files.move(temporary, this.configFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                System.err.println("[Camera Lock-On] Failed to save config: " + exception.getMessage());
            }
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
