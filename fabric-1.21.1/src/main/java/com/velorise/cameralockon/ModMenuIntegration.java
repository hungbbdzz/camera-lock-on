package com.velorise.cameralockon;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu integration. The mod still works when Mod Menu is absent. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LockOnConfigScreen::new;
    }
}
