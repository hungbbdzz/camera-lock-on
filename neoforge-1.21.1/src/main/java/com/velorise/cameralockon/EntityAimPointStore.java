package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores per-entity camera aim overrides separately from the main NeoForge
 * client TOML. The JSON file is intentionally registry-ID based so entries for
 * temporarily missing modded entities remain safe and reusable.
 */
public final class EntityAimPointStore {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Entry>>() { }.getType();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(CameraLockOn.MODID)
            .resolve("entity_aim_points.json");

    private static final LinkedHashMap<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean loaded;

    private EntityAimPointStore() {
    }

    public static synchronized Path getFilePath() {
        return FILE_PATH;
    }

    public static synchronized Entry get(String entityId) {
        ensureLoaded();
        Entry entry = ENTRIES.get(entityId);
        return entry == null ? null : entry.copy();
    }

    public static synchronized boolean has(String entityId) {
        ensureLoaded();
        return entityId != null && ENTRIES.containsKey(entityId);
    }

    public static synchronized void put(
            String entityId,
            double horizontal,
            double vertical,
            boolean allowGroupAimOffset
    ) {
        if (entityId == null || ResourceLocation.tryParse(entityId) == null) {
            return;
        }

        ensureLoaded();
        ENTRIES.put(
                entityId,
                new Entry(
                        Mth.clamp(horizontal, -1.0D, 1.0D),
                        Mth.clamp(vertical, 0.0D, 1.0D),
                        allowGroupAimOffset
                )
        );
        save();
    }

    public static synchronized void remove(String entityId) {
        ensureLoaded();
        if (entityId != null && ENTRIES.remove(entityId) != null) {
            save();
        }
    }

    public static synchronized void clear() {
        ensureLoaded();
        if (!ENTRIES.isEmpty()) {
            ENTRIES.clear();
            save();
        }
    }

    public static synchronized Map<String, Entry> snapshot() {
        ensureLoaded();
        LinkedHashMap<String, Entry> copy = new LinkedHashMap<>();
        ENTRIES.forEach((id, entry) -> copy.put(id, entry.copy()));
        return copy;
    }

    public static synchronized void reload() {
        loaded = false;
        ENTRIES.clear();
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        ENTRIES.clear();

        if (Files.isRegularFile(FILE_PATH)) {
            try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                Map<String, Entry> parsed = GSON.fromJson(reader, MAP_TYPE);
                if (parsed != null) {
                    for (Map.Entry<String, Entry> mapEntry : parsed.entrySet()) {
                        String id = mapEntry.getKey();
                        Entry value = mapEntry.getValue();
                        if (id == null
                                || ResourceLocation.tryParse(id) == null
                                || value == null) {
                            continue;
                        }
                        ENTRIES.put(id, value.sanitized());
                    }
                }
            } catch (IOException | JsonParseException exception) {
                System.err.println("[Camera Lock-On] Could not read " + FILE_PATH + ": " + exception.getMessage());
            }
        }

        if (ENTRIES.isEmpty()) {
            migrateLegacyTomlEntries();
        }
    }

    private static void migrateLegacyTomlEntries() {
        try {
            List<? extends String> legacy = CameraLockOnConfig.PER_ENTITY_AIM_POINTS.get();
            for (Map.Entry<String, TargetingRules.AimPoint> entry
                    : TargetingRules.parseAimPointMap(legacy).entrySet()) {
                ENTRIES.put(
                        entry.getKey(),
                        new Entry(entry.getValue().x(), entry.getValue().y(), false)
                );
            }
            if (!ENTRIES.isEmpty()) {
                save();
                CameraLockOnConfig.PER_ENTITY_AIM_POINTS.set(List.of());
                CameraLockOnConfig.CLIENT_SPEC.save();
            }
        } catch (RuntimeException ignored) {
            // The client config may not be available yet. A later access retries
            // normal JSON loading without making startup fail.
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            Path temporary = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(ENTRIES, MAP_TYPE, writer);
            }
            try {
                Files.move(
                        temporary,
                        FILE_PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException ignored) {
                Files.move(temporary, FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Camera Lock-On] Could not write " + FILE_PATH + ": " + exception.getMessage());
        }
    }

    public static final class Entry {
        private double horizontal;
        private double vertical;
        private boolean allowGroupAimOffset;

        @SuppressWarnings("unused")
        private Entry() {
        }

        public Entry(double horizontal, double vertical, boolean allowGroupAimOffset) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.allowGroupAimOffset = allowGroupAimOffset;
        }

        public double horizontal() {
            return this.horizontal;
        }

        public double vertical() {
            return this.vertical;
        }

        public boolean allowGroupAimOffset() {
            return this.allowGroupAimOffset;
        }

        private Entry sanitized() {
            return new Entry(
                    Mth.clamp(this.horizontal, -1.0D, 1.0D),
                    Mth.clamp(this.vertical, 0.0D, 1.0D),
                    this.allowGroupAimOffset
            );
        }

        private Entry copy() {
            return new Entry(this.horizontal, this.vertical, this.allowGroupAimOffset);
        }
    }
}
