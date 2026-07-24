package com.velorise.cameralockon;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 1.20.1 entry point. The mod contains no server-side gameplay logic. */
@Mod(CameraLockOn.MODID)
public final class CameraLockOn {
    public static final String MODID = "camera_lockon";

    public CameraLockOn() {
        CameraLockOnConfig.initialize(
                FMLPaths.CONFIGDIR.get()
                        .resolve(MODID)
                        .resolve("camera_lockon-client.json")
        );

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientOnly::registerConfigScreen);
    }

    private static final class ClientOnly {
        private ClientOnly() {
        }

        private static void registerConfigScreen() {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(LockOnConfigScreen::new)
            );
        }
    }
}
