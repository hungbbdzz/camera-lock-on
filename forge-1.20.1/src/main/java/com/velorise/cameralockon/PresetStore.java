package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JSON preset files that can be copied between players. Only known keys are ever applied. */
public final class PresetStore {
    public static final int FORMAT_VERSION = 1;
    private static final int MAX_FILE_BYTES = 128 * 1024;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get()
            .resolve(CameraLockOn.MODID)
            .resolve("presets");

    private PresetStore() {
    }

    public static Path getDirectory() {
        return DIRECTORY;
    }

    public static List<PresetSummary> list() {
        List<PresetSummary> result = new ArrayList<>();
        try {
            Files.createDirectories(DIRECTORY);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORY, "*.json")) {
                for (Path path : stream) {
                    Preset preset = read(path);
                    if (preset != null) {
                        result.add(new PresetSummary(preset.name, path.getFileName().toString(), preset.categories()));
                    }
                }
            }
        } catch (IOException exception) {
            System.err.println("[Camera Lock-On] Could not list presets: " + exception.getMessage());
        }
        result.sort(Comparator.comparing(PresetSummary::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public static Preset load(String fileName) {
        Path path = resolveFile(fileName);
        return path == null ? null : read(path);
    }

    public static String saveCurrent(
            String requestedName,
            EnumSet<Category> categories,
            boolean includeBlacklist,
            boolean includeAimOverrides,
            boolean includeAoeWeapons,
            String existingFileName
    ) {
        String name = sanitizeDisplayName(requestedName);
        if (name.isBlank()) {
            return null;
        }

        String fileName = existingFileName == null || existingFileName.isBlank()
                ? uniqueFileName(slug(name) + ".json", null)
                : existingFileName;
        Path path = resolveFile(fileName);
        if (path == null) {
            return null;
        }

        Preset preset = capture(name, categories, includeBlacklist, includeAimOverrides, includeAoeWeapons);
        return write(path, preset) ? path.getFileName().toString() : null;
    }

    public static boolean rename(String fileName, String requestedName) {
        Preset preset = load(fileName);
        if (preset == null) {
            return false;
        }
        preset.name = sanitizeDisplayName(requestedName);
        if (preset.name.isBlank()) {
            return false;
        }
        Path path = resolveFile(fileName);
        return path != null && write(path, preset);
    }

    public static boolean delete(String fileName) {
        Path path = resolveFile(fileName);
        if (path == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(path);
        } catch (IOException exception) {
            System.err.println("[Camera Lock-On] Could not delete preset " + path + ": " + exception.getMessage());
            return false;
        }
    }

    public static boolean apply(Preset preset) {
        if (preset == null || preset.settings == null) {
            return false;
        }
        EnumSet<Category> categories = preset.categories();
        JsonObject s = preset.settings;

        try {
            if (categories.contains(Category.MAIN)) {
                CameraLockOnConfig.SMART_LOCK.set(bool(s, "smartLock", CameraLockOnConfig.SMART_LOCK.get()));
                CameraLockOnConfig.AUTO_RETARGET.set(bool(s, "autoRetarget", CameraLockOnConfig.AUTO_RETARGET.get()));
                CameraLockOnConfig.HOSTILE_ONLY.set(bool(s, "hostileOnly", CameraLockOnConfig.HOSTILE_ONLY.get()));
                CameraLockOnConfig.LOCK_ON_RANGE.set(clamp(number(s, "lockOnRange", CameraLockOnConfig.LOCK_ON_RANGE.get()), 5, 128));
                CameraLockOnConfig.AIM_PRESET.set(CameraLockOnConfig.AimPreset.fromConfig(text(s, "aimPreset", CameraLockOnConfig.AIM_PRESET.get())).name());
                CameraLockOnConfig.AIM_POINT_X.set(clamp(number(s, "aimPointX", CameraLockOnConfig.AIM_POINT_X.get()), -1, 1));
                CameraLockOnConfig.AIM_POINT_Y.set(clamp(number(s, "aimPointY", CameraLockOnConfig.AIM_POINT_Y.get()), 0, 1));
                CameraLockOnConfig.SHOW_RETICLE.set(bool(s, "showReticle", CameraLockOnConfig.SHOW_RETICLE.get()));
                String color = text(s, "reticleColor", CameraLockOnConfig.RETICLE_COLOR.get());
                if (List.of("Cyan", "Red", "Green", "Yellow", "Purple").contains(color)) {
                    CameraLockOnConfig.RETICLE_COLOR.set(color);
                }
                CameraLockOnConfig.RETICLE_OPACITY.set(clamp(number(s, "reticleOpacity", CameraLockOnConfig.RETICLE_OPACITY.get()), 0.1, 1));
                CameraLockOnConfig.LOCK_SOUNDS.set(bool(s, "lockSounds", CameraLockOnConfig.LOCK_SOUNDS.get()));
                CameraLockOnConfig.SOUND_VOLUME.set(clamp(number(s, "soundVolume", CameraLockOnConfig.SOUND_VOLUME.get()), 0, 1));
                ClientFeatureStore.setSwitchTargetMode(ClientFeatureStore.SwitchTargetMode.fromName(
                        text(s, "switchTargetMode", ClientFeatureStore.getSwitchTargetMode().name())));
            }

            if (categories.contains(Category.CAMERA)) {
                CameraLockOnConfig.DEAD_ZONE.set(bool(s, "deadZone", CameraLockOnConfig.DEAD_ZONE.get()));
                CameraLockOnConfig.DEAD_ZONE_HORIZONTAL.set(clamp(number(s, "deadZoneHorizontal", CameraLockOnConfig.DEAD_ZONE_HORIZONTAL.get()), 0, 30));
                CameraLockOnConfig.DEAD_ZONE_VERTICAL.set(clamp(number(s, "deadZoneVertical", CameraLockOnConfig.DEAD_ZONE_VERTICAL.get()), 0, 20));
                CameraLockOnConfig.SUSPEND_MINING.set(bool(s, "suspendMining", CameraLockOnConfig.SUSPEND_MINING.get()));
                CameraLockOnConfig.SUSPEND_USING_ITEM.set(bool(s, "suspendUsing", CameraLockOnConfig.SUSPEND_USING_ITEM.get()));
                CameraLockOnConfig.SUSPEND_RIDING.set(bool(s, "suspendRiding", CameraLockOnConfig.SUSPEND_RIDING.get()));
                CameraLockOnConfig.SUSPEND_ELYTRA.set(bool(s, "suspendElytra", CameraLockOnConfig.SUSPEND_ELYTRA.get()));
                CameraLockOnConfig.LOST_TARGET_GRACE.set(clamp(number(s, "lostTargetGrace", CameraLockOnConfig.LOST_TARGET_GRACE.get()), 0, 10));
            }

            if (categories.contains(Category.AUTO)) {
                CameraLockOnConfig.AUTO_LOCK.set(bool(s, "autoLock", CameraLockOnConfig.AUTO_LOCK.get()));
                CameraLockOnConfig.AUTO_LOCK_INDICATOR.set(bool(s, "autoLockIndicator", CameraLockOnConfig.AUTO_LOCK_INDICATOR.get()));
                CameraLockOnConfig.AUTO_LOCK_DELAY.set(clamp(number(s, "autoLockDelay", CameraLockOnConfig.AUTO_LOCK_DELAY.get()), 0.05, 3));
                CameraLockOnConfig.AUTO_LOCK_COOLDOWN.set(clamp(number(s, "autoLockCooldown", CameraLockOnConfig.AUTO_LOCK_COOLDOWN.get()), 0, 5));
                CameraLockOnConfig.LOCK_ON_HIT_MODE.set(CameraLockOnConfig.LockOnHitMode.fromConfig(text(s, "lockOnHitMode", CameraLockOnConfig.LOCK_ON_HIT_MODE.get())).name());
                CameraLockOnConfig.TEMPORARY_PIN_MODE.set(CameraLockOnConfig.TemporaryPinMode.fromConfig(text(s, "temporaryPinMode", CameraLockOnConfig.TEMPORARY_PIN_MODE.get())).name());
            }

            if (categories.contains(Category.FILTER)) {
                CameraLockOnConfig.TARGET_TYPE_MODE.set(CameraLockOnConfig.TargetTypeMode.fromConfig(text(s, "targetTypeMode", CameraLockOnConfig.TARGET_TYPE_MODE.get())).name());
                CameraLockOnConfig.SELECTED_ENTITY_TYPE.set(text(s, "selectedEntityType", CameraLockOnConfig.SELECTED_ENTITY_TYPE.get()));
                CameraLockOnConfig.RETARGET_MODE.set(CameraLockOnConfig.RetargetMode.fromConfig(text(s, "retargetMode", CameraLockOnConfig.RETARGET_MODE.get())).name());
                ClientFeatureStore.setTargetPriority(ClientFeatureStore.TargetPriority.fromName(
                        text(s, "targetPriority", ClientFeatureStore.getTargetPriority().name())));
                CameraLockOnConfig.PREFER_BOSSES.set(bool(s, "preferBosses", CameraLockOnConfig.PREFER_BOSSES.get()));
            }

            if (categories.contains(Category.HUD)) {
                CameraLockOnConfig.TARGET_HUD.set(bool(s, "hudEnabled", CameraLockOnConfig.TARGET_HUD.get()));
                CameraLockOnConfig.HUD_SHOW_NAME.set(bool(s, "hudName", CameraLockOnConfig.HUD_SHOW_NAME.get()));
                CameraLockOnConfig.HUD_SHOW_HEALTH.set(bool(s, "hudHealth", CameraLockOnConfig.HUD_SHOW_HEALTH.get()));
                CameraLockOnConfig.HUD_SHOW_DISTANCE.set(bool(s, "hudDistance", CameraLockOnConfig.HUD_SHOW_DISTANCE.get()));
                CameraLockOnConfig.HUD_SHOW_ARMOR.set(bool(s, "hudArmor", CameraLockOnConfig.HUD_SHOW_ARMOR.get()));
                CameraLockOnConfig.SHOW_REGISTRY_IDS.set(bool(s, "hudRegistryId", CameraLockOnConfig.SHOW_REGISTRY_IDS.get()));
                CameraLockOnConfig.HUD_SHOW_MOD_NAME.set(bool(s, "hudModName", CameraLockOnConfig.HUD_SHOW_MOD_NAME.get()));
                CameraLockOnConfig.HUD_ANIMATE_DAMAGE.set(bool(s, "hudDamageFlash", CameraLockOnConfig.HUD_ANIMATE_DAMAGE.get()));
                CameraLockOnConfig.HUD_SCALE.set(clamp(number(s, "hudScale", CameraLockOnConfig.HUD_SCALE.get()), 0.6, 1.6));
                CameraLockOnConfig.HUD_OPACITY.set(clamp(number(s, "hudOpacity", CameraLockOnConfig.HUD_OPACITY.get()), 0.05, 1));
                if (preset.includeHudPosition) {
                    CameraLockOnConfig.HUD_ANCHOR_X.set(clamp(number(s, "hudAnchorX", CameraLockOnConfig.HUD_ANCHOR_X.get()), 0, 1));
                    CameraLockOnConfig.HUD_ANCHOR_Y.set(clamp(number(s, "hudAnchorY", CameraLockOnConfig.HUD_ANCHOR_Y.get()), 0, 1));
                }
            }

            if (categories.contains(Category.THREAT)) {
                CameraLockOnConfig.ATTACKER_INDICATOR.set(bool(s, "attackerIndicator", CameraLockOnConfig.ATTACKER_INDICATOR.get()));
                CameraLockOnConfig.GROUP_ATTACKER_DIRECTIONS.set(bool(s, "groupAttackerDirections", CameraLockOnConfig.GROUP_ATTACKER_DIRECTIONS.get()));
                CameraLockOnConfig.ATTACKER_RESPONSE.set(CameraLockOnConfig.AttackerResponse.fromConfig(text(s, "attackerResponse", CameraLockOnConfig.ATTACKER_RESPONSE.get())).name());
                CameraLockOnConfig.ATTACKER_REQUIRED_HITS.set((int) clamp(number(s, "attackerRequiredHits", CameraLockOnConfig.ATTACKER_REQUIRED_HITS.get()), 2, 5));
                CameraLockOnConfig.ATTACKER_HIT_WINDOW.set(clamp(number(s, "attackerHitWindow", CameraLockOnConfig.ATTACKER_HIT_WINDOW.get()), 1, 15));
                CameraLockOnConfig.ATTACKER_LOCK_RANGE.set(clamp(number(s, "attackerLockRange", CameraLockOnConfig.ATTACKER_LOCK_RANGE.get()), 4, 64));
                CameraLockOnConfig.MAX_ATTACKER_INDICATORS.set((int) clamp(number(s, "maxAttackerIndicators", CameraLockOnConfig.MAX_ATTACKER_INDICATORS.get()), 1, 6));
                CameraLockOnConfig.ATTACKER_REPLACE_TARGET.set(bool(s, "attackerReplaceTarget", CameraLockOnConfig.ATTACKER_REPLACE_TARGET.get()));
            }

            if (categories.contains(Category.GROUP)) {
                CameraLockOnConfig.GROUP_AIM.set(bool(s, "groupAim", CameraLockOnConfig.GROUP_AIM.get()));
                CameraLockOnConfig.GROUP_AIM_SAME_TYPE_ONLY.set(bool(s, "groupAimSameType", CameraLockOnConfig.GROUP_AIM_SAME_TYPE_ONLY.get()));
                CameraLockOnConfig.GROUP_AIM_ACTIVATION.set(CameraLockOnConfig.GroupAimActivation.fromConfig(text(s, "groupAimActivation", CameraLockOnConfig.GROUP_AIM_ACTIVATION.get())).name());
                CameraLockOnConfig.GROUP_AIM_RADIUS.set(clamp(number(s, "groupAimRadius", CameraLockOnConfig.GROUP_AIM_RADIUS.get()), 0.5, 6));
                CameraLockOnConfig.GROUP_AIM_STRENGTH.set(clamp(number(s, "groupAimStrength", CameraLockOnConfig.GROUP_AIM_STRENGTH.get()), 0, 1));
                CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.set((int) clamp(number(s, "groupAimMaxTargets", CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.get()), 2, 8));
                CameraLockOnConfig.GROUP_AIM_MAX_OFFSET.set(clamp(number(s, "groupAimMaxOffset", CameraLockOnConfig.GROUP_AIM_MAX_OFFSET.get()), 0.1, 3));
            }

            if (preset.includeBlacklist && preset.blacklist != null) {
                CameraLockOnConfig.ENTITY_BLACKLIST.set(List.copyOf(preset.blacklist));
            }
            if (preset.includeAimOverrides && preset.aimOverrides != null) {
                EntityAimPointStore.clear();
                for (Map.Entry<String, AimOverride> entry : preset.aimOverrides.entrySet()) {
                    AimOverride value = entry.getValue();
                    if (value != null) {
                        EntityAimPointStore.put(entry.getKey(), value.horizontal, value.vertical, value.allowGroupAimOffset);
                    }
                }
            }
            if (preset.includeAoeWeapons && preset.aoeWeapons != null) {
                AoeWeaponStore.clear();
                for (String id : preset.aoeWeapons) {
                    AoeWeaponStore.add(id);
                }
            }

            CameraLockOnConfig.CLIENT_SPEC.save();
            ClientFeatureStore.setLastLoadedPreset(preset.name);
            return true;
        } catch (RuntimeException exception) {
            System.err.println("[Camera Lock-On] Could not apply preset " + preset.name + ": " + exception.getMessage());
            return false;
        }
    }

    private static Preset capture(
            String name,
            EnumSet<Category> categories,
            boolean includeBlacklist,
            boolean includeAimOverrides,
            boolean includeAoeWeapons
    ) {
        Preset preset = new Preset();
        preset.formatVersion = FORMAT_VERSION;
        preset.name = name;
        preset.modVersion = "2.0.0";
        preset.categoryNames = categories.stream().map(Enum::name).toList();
        preset.settings = new JsonObject();
        preset.includeHudPosition = categories.contains(Category.HUD);
        preset.includeBlacklist = includeBlacklist;
        preset.includeAimOverrides = includeAimOverrides;
        preset.includeAoeWeapons = includeAoeWeapons;

        JsonObject s = preset.settings;
        if (categories.contains(Category.MAIN)) {
            put(s, "smartLock", CameraLockOnConfig.SMART_LOCK.get());
            put(s, "autoRetarget", CameraLockOnConfig.AUTO_RETARGET.get());
            put(s, "hostileOnly", CameraLockOnConfig.HOSTILE_ONLY.get());
            put(s, "lockOnRange", CameraLockOnConfig.LOCK_ON_RANGE.get());
            put(s, "aimPreset", CameraLockOnConfig.AIM_PRESET.get());
            put(s, "aimPointX", CameraLockOnConfig.AIM_POINT_X.get());
            put(s, "aimPointY", CameraLockOnConfig.AIM_POINT_Y.get());
            put(s, "showReticle", CameraLockOnConfig.SHOW_RETICLE.get());
            put(s, "reticleColor", CameraLockOnConfig.RETICLE_COLOR.get());
            put(s, "reticleOpacity", CameraLockOnConfig.RETICLE_OPACITY.get());
            put(s, "lockSounds", CameraLockOnConfig.LOCK_SOUNDS.get());
            put(s, "soundVolume", CameraLockOnConfig.SOUND_VOLUME.get());
            put(s, "switchTargetMode", ClientFeatureStore.getSwitchTargetMode().name());
        }
        if (categories.contains(Category.CAMERA)) {
            put(s, "deadZone", CameraLockOnConfig.DEAD_ZONE.get());
            put(s, "deadZoneHorizontal", CameraLockOnConfig.DEAD_ZONE_HORIZONTAL.get());
            put(s, "deadZoneVertical", CameraLockOnConfig.DEAD_ZONE_VERTICAL.get());
            put(s, "suspendMining", CameraLockOnConfig.SUSPEND_MINING.get());
            put(s, "suspendUsing", CameraLockOnConfig.SUSPEND_USING_ITEM.get());
            put(s, "suspendRiding", CameraLockOnConfig.SUSPEND_RIDING.get());
            put(s, "suspendElytra", CameraLockOnConfig.SUSPEND_ELYTRA.get());
            put(s, "lostTargetGrace", CameraLockOnConfig.LOST_TARGET_GRACE.get());
        }
        if (categories.contains(Category.AUTO)) {
            put(s, "autoLock", CameraLockOnConfig.AUTO_LOCK.get());
            put(s, "autoLockIndicator", CameraLockOnConfig.AUTO_LOCK_INDICATOR.get());
            put(s, "autoLockDelay", CameraLockOnConfig.AUTO_LOCK_DELAY.get());
            put(s, "autoLockCooldown", CameraLockOnConfig.AUTO_LOCK_COOLDOWN.get());
            put(s, "lockOnHitMode", CameraLockOnConfig.LOCK_ON_HIT_MODE.get());
            put(s, "temporaryPinMode", CameraLockOnConfig.TEMPORARY_PIN_MODE.get());
        }
        if (categories.contains(Category.FILTER)) {
            put(s, "targetTypeMode", CameraLockOnConfig.TARGET_TYPE_MODE.get());
            put(s, "selectedEntityType", CameraLockOnConfig.SELECTED_ENTITY_TYPE.get());
            put(s, "retargetMode", CameraLockOnConfig.RETARGET_MODE.get());
            put(s, "targetPriority", ClientFeatureStore.getTargetPriority().name());
            put(s, "preferBosses", CameraLockOnConfig.PREFER_BOSSES.get());
        }
        if (categories.contains(Category.HUD)) {
            put(s, "hudEnabled", CameraLockOnConfig.TARGET_HUD.get());
            put(s, "hudName", CameraLockOnConfig.HUD_SHOW_NAME.get());
            put(s, "hudHealth", CameraLockOnConfig.HUD_SHOW_HEALTH.get());
            put(s, "hudDistance", CameraLockOnConfig.HUD_SHOW_DISTANCE.get());
            put(s, "hudArmor", CameraLockOnConfig.HUD_SHOW_ARMOR.get());
            put(s, "hudRegistryId", CameraLockOnConfig.SHOW_REGISTRY_IDS.get());
            put(s, "hudModName", CameraLockOnConfig.HUD_SHOW_MOD_NAME.get());
            put(s, "hudDamageFlash", CameraLockOnConfig.HUD_ANIMATE_DAMAGE.get());
            put(s, "hudScale", CameraLockOnConfig.HUD_SCALE.get());
            put(s, "hudOpacity", CameraLockOnConfig.HUD_OPACITY.get());
            put(s, "hudAnchorX", CameraLockOnConfig.HUD_ANCHOR_X.get());
            put(s, "hudAnchorY", CameraLockOnConfig.HUD_ANCHOR_Y.get());
        }
        if (categories.contains(Category.THREAT)) {
            put(s, "attackerIndicator", CameraLockOnConfig.ATTACKER_INDICATOR.get());
            put(s, "groupAttackerDirections", CameraLockOnConfig.GROUP_ATTACKER_DIRECTIONS.get());
            put(s, "attackerResponse", CameraLockOnConfig.ATTACKER_RESPONSE.get());
            put(s, "attackerRequiredHits", CameraLockOnConfig.ATTACKER_REQUIRED_HITS.get());
            put(s, "attackerHitWindow", CameraLockOnConfig.ATTACKER_HIT_WINDOW.get());
            put(s, "attackerLockRange", CameraLockOnConfig.ATTACKER_LOCK_RANGE.get());
            put(s, "maxAttackerIndicators", CameraLockOnConfig.MAX_ATTACKER_INDICATORS.get());
            put(s, "attackerReplaceTarget", CameraLockOnConfig.ATTACKER_REPLACE_TARGET.get());
        }
        if (categories.contains(Category.GROUP)) {
            put(s, "groupAim", CameraLockOnConfig.GROUP_AIM.get());
            put(s, "groupAimSameType", CameraLockOnConfig.GROUP_AIM_SAME_TYPE_ONLY.get());
            put(s, "groupAimActivation", CameraLockOnConfig.GROUP_AIM_ACTIVATION.get());
            put(s, "groupAimRadius", CameraLockOnConfig.GROUP_AIM_RADIUS.get());
            put(s, "groupAimStrength", CameraLockOnConfig.GROUP_AIM_STRENGTH.get());
            put(s, "groupAimMaxTargets", CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.get());
            put(s, "groupAimMaxOffset", CameraLockOnConfig.GROUP_AIM_MAX_OFFSET.get());
        }

        if (includeBlacklist) {
            preset.blacklist = new ArrayList<>(CameraLockOnConfig.ENTITY_BLACKLIST.get());
        }
        if (includeAimOverrides) {
            preset.aimOverrides = new LinkedHashMap<>();
            EntityAimPointStore.snapshot().forEach((id, entry) -> preset.aimOverrides.put(
                    id,
                    new AimOverride(entry.horizontal(), entry.vertical(), entry.allowGroupAimOffset())
            ));
        }
        if (includeAoeWeapons) {
            preset.aoeWeapons = new ArrayList<>(AoeWeaponStore.snapshot());
        }
        return preset;
    }

    private static Preset read(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_FILE_BYTES) {
                return null;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                Preset preset = GSON.fromJson(reader, Preset.class);
                if (preset == null || preset.formatVersion != FORMAT_VERSION || preset.settings == null) {
                    return null;
                }
                preset.name = sanitizeDisplayName(preset.name);
                return preset.name.isBlank() ? null : preset;
            }
        } catch (IOException | JsonParseException exception) {
            System.err.println("[Camera Lock-On] Could not read preset " + path + ": " + exception.getMessage());
            return null;
        }
    }

    private static boolean write(Path path, Preset preset) {
        try {
            Files.createDirectories(DIRECTORY);
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(preset, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            System.err.println("[Camera Lock-On] Could not write preset " + path + ": " + exception.getMessage());
            return false;
        }
    }

    private static Path resolveFile(String fileName) {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9._-]+\\.json")) {
            return null;
        }
        Path normalized = DIRECTORY.resolve(fileName).normalize();
        return normalized.getParent() != null && normalized.getParent().equals(DIRECTORY.normalize()) ? normalized : null;
    }

    private static String uniqueFileName(String requested, String ignored) {
        String base = requested.endsWith(".json") ? requested.substring(0, requested.length() - 5) : requested;
        String candidate = base + ".json";
        int suffix = 2;
        while (!candidate.equals(ignored) && Files.exists(DIRECTORY.resolve(candidate))) {
            candidate = base + "-" + suffix++ + ".json";
        }
        return candidate;
    }

    private static String sanitizeDisplayName(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}]", "").trim();
        return cleaned.length() > 48 ? cleaned.substring(0, 48).trim() : cleaned;
    }

    private static String slug(String value) {
        String result = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return result.isBlank() ? "preset" : result;
    }

    private static void put(JsonObject object, String key, boolean value) { object.addProperty(key, value); }
    private static void put(JsonObject object, String key, Number value) { object.addProperty(key, value); }
    private static void put(JsonObject object, String key, String value) { object.addProperty(key, value == null ? "" : value); }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        try { return element == null ? fallback : element.getAsBoolean(); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static double number(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        try { return element == null ? fallback : element.getAsDouble(); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String text(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        try { return element == null ? fallback : element.getAsString(); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static double clamp(double value, double min, double max) {
        return Mth.clamp(Double.isFinite(value) ? value : min, min, max);
    }

    public enum Category {
        MAIN("Main"), CAMERA("Camera"), AUTO("Auto"), FILTER("Filter"), HUD("HUD"), THREAT("Threat"), GROUP("Group");
        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String displayName() { return this.displayName; }
    }

    public record PresetSummary(String name, String fileName, EnumSet<Category> categories) {
    }

    public static final class Preset {
        private int formatVersion;
        private String name;
        private String modVersion;
        private List<String> categoryNames = List.of();
        private JsonObject settings = new JsonObject();
        private boolean includeHudPosition;
        private boolean includeBlacklist;
        private boolean includeAimOverrides;
        private boolean includeAoeWeapons;
        private List<String> blacklist;
        private Map<String, AimOverride> aimOverrides;
        private List<String> aoeWeapons;

        public String name() { return this.name; }
        public boolean includesBlacklist() { return this.includeBlacklist; }
        public boolean includesAimOverrides() { return this.includeAimOverrides; }
        public boolean includesAoeWeapons() { return this.includeAoeWeapons; }

        public EnumSet<Category> categories() {
            EnumSet<Category> result = EnumSet.noneOf(Category.class);
            if (this.categoryNames != null) {
                for (String value : this.categoryNames) {
                    try { result.add(Category.valueOf(value)); }
                    catch (RuntimeException ignored) { }
                }
            }
            return result;
        }
    }

    private static final class AimOverride {
        private double horizontal;
        private double vertical;
        private boolean allowGroupAimOffset;

        private AimOverride() {
        }

        private AimOverride(double horizontal, double vertical, boolean allowGroupAimOffset) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.allowGroupAimOffset = allowGroupAimOffset;
        }
    }
}
