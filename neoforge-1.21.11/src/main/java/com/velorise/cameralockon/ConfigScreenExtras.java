package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Adds compact combat shortcuts without duplicating the existing tabbed config implementation. */
public final class ConfigScreenExtras {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 260;

    private ConfigScreenExtras() {
    }

    // Note: RenderTooltipEvent.Color was removed in 1.21.4 (tooltips now use textured sprites)

    private static String presetLabel() {
        String current = ClientFeatureStore.getLastLoadedPreset();
        return current.isBlank() ? "Presets..." : "Preset: " + compact(current, 17);
    }

    private static String switchLabel() {
        return "Switch: " + ClientFeatureStore.getSwitchTargetMode().getDisplayName();
    }

    private static String compact(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(1, maximum - 1)) + "…";
    }
}
