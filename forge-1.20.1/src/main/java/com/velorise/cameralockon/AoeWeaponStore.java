package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * User-maintained AOE weapon overrides. Group Aim still recognizes NeoForge's
 * SWORD_SWEEP ability automatically; this file only covers items that do not
 * expose that ability.
 */
public final class AoeWeaponStore {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Type SET_TYPE = new TypeToken<LinkedHashSet<String>>() { }.getType();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(CameraLockOn.MODID)
            .resolve("aoe_weapons.json");

    private static final LinkedHashSet<String> IDS = new LinkedHashSet<>();
    private static boolean loaded;

    private AoeWeaponStore() {
    }

    public static synchronized boolean contains(Item item) {
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && IDS.contains(id.toString());
    }

    public static synchronized boolean contains(String itemId) {
        ensureLoaded();
        return itemId != null && IDS.contains(itemId);
    }

    public static synchronized void add(String itemId) {
        if (itemId == null || ResourceLocation.tryParse(itemId) == null) {
            return;
        }
        ensureLoaded();
        if (IDS.add(itemId)) {
            save();
        }
    }

    public static synchronized void remove(String itemId) {
        ensureLoaded();
        if (itemId != null && IDS.remove(itemId)) {
            save();
        }
    }

    public static synchronized void clear() {
        ensureLoaded();
        if (!IDS.isEmpty()) {
            IDS.clear();
            save();
        }
    }

    public static synchronized List<String> snapshot() {
        ensureLoaded();
        return List.copyOf(IDS);
    }

    public static synchronized Path getFilePath() {
        return FILE_PATH;
    }

    public static synchronized void reload() {
        loaded = false;
        IDS.clear();
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        IDS.clear();

        if (!Files.isRegularFile(FILE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Set<String> parsed = GSON.fromJson(reader, SET_TYPE);
            if (parsed == null) {
                return;
            }
            for (String value : parsed) {
                if (value != null && ResourceLocation.tryParse(value) != null) {
                    IDS.add(value);
                }
            }
        } catch (IOException | JsonParseException exception) {
            System.err.println("[Camera Lock-On] Could not read " + FILE_PATH + ": " + exception.getMessage());
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            Path temporary = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(IDS, SET_TYPE, writer);
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
}
