package com.velorise.cameralockon;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class PlatformPaths {
    private PlatformPaths() {
    }

    public static Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(CameraLockOn.MODID);
    }
}
