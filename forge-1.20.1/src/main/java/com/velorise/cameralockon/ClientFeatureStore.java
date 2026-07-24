package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Small JSON-backed preferences that are intentionally independent from the legacy TOML config. */
public final class ClientFeatureStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(CameraLockOn.MODID)
            .resolve("client_features.json");

    private static Data data;

    private ClientFeatureStore() {
    }

    public static synchronized SwitchTargetMode getSwitchTargetMode() {
        ensureLoaded();
        return SwitchTargetMode.fromName(data.switchTargetMode);
    }

    public static synchronized void setSwitchTargetMode(SwitchTargetMode mode) {
        ensureLoaded();
        data.switchTargetMode = (mode == null ? SwitchTargetMode.SMART : mode).name();
        save();
    }

    public static synchronized TargetPriority getTargetPriority() {
        ensureLoaded();
        return TargetPriority.fromName(data.targetPriority);
    }

    public static synchronized void setTargetPriority(TargetPriority priority) {
        ensureLoaded();
        data.targetPriority = (priority == null ? TargetPriority.BALANCED : priority).name();
        save();
    }

    public static synchronized String getLastLoadedPreset() {
        ensureLoaded();
        return data.lastLoadedPreset == null ? "" : data.lastLoadedPreset;
    }

    public static synchronized void setLastLoadedPreset(String name) {
        ensureLoaded();
        data.lastLoadedPreset = name == null ? "" : name.trim();
        save();
    }

    private static void ensureLoaded() {
        if (data != null) {
            return;
        }
        data = new Data();
        if (!Files.isRegularFile(FILE_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Data parsed = GSON.fromJson(reader, Data.class);
            if (parsed != null) {
                data = parsed;
            }
        } catch (IOException | JsonParseException exception) {
            System.err.println("[Camera Lock-On] Could not read " + FILE_PATH + ": " + exception.getMessage());
        }
        data.switchTargetMode = SwitchTargetMode.fromName(data.switchTargetMode).name();
        data.targetPriority = TargetPriority.fromName(data.targetPriority).name();
        if (data.lastLoadedPreset == null) {
            data.lastLoadedPreset = "";
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            Path temporary = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(temporary, FILE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Camera Lock-On] Could not write " + FILE_PATH + ": " + exception.getMessage());
        }
    }

    private static final class Data {
        private String switchTargetMode = SwitchTargetMode.SMART.name();
        private String targetPriority = TargetPriority.BALANCED.name();
        private String lastLoadedPreset = "";
    }

    public enum TargetPriority {
        BALANCED("Balanced"),
        CROSSHAIR("Crosshair"),
        NEAREST("Nearest"),
        LOWEST_HEALTH("Lowest Health"),
        HIGHEST_HEALTH("Highest Health");

        private final String displayName;

        TargetPriority(String displayName) {
            this.displayName = displayName;
        }

        public Component getDisplayName() {
            return Component.translatable("gui.camera_lockon.enum.target_priority." + name().toLowerCase(Locale.ROOT));
        }

        public TargetPriority next() {
            TargetPriority[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static TargetPriority fromName(String value) {
            if (value == null) {
                return BALANCED;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return BALANCED;
            }
        }
    }

    public enum SwitchTargetMode {
        SMART("Smart"),
        CYCLE("Cycle");

        private final String displayName;

        SwitchTargetMode(String displayName) {
            this.displayName = displayName;
        }

        public Component getDisplayName() {
            return Component.translatable("gui.camera_lockon.enum.switch_target_mode." + name().toLowerCase(Locale.ROOT));
        }

        public SwitchTargetMode next() {
            return this == SMART ? CYCLE : SMART;
        }

        public static SwitchTargetMode fromName(String value) {
            if (value == null) {
                return SMART;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SMART;
            }
        }
    }
}
