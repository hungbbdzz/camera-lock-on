package com.velorise.cameralockon;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CameraLockOn.MODID)
public class CameraLockOn {
    public static final String MODID = "camera_lockon";

    public CameraLockOn(ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, CameraLockOnConfig.CLIENT_SPEC);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (mc, parent) -> new LockOnConfigScreen(parent));
        }
    }
}
