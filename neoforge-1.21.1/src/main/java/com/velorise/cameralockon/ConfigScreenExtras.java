package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Adds compact combat shortcuts without duplicating the existing tabbed config implementation. */
@EventBusSubscriber(modid = CameraLockOn.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ConfigScreenExtras {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 260;

    private ConfigScreenExtras() {
    }

    @SubscribeEvent
    public static void onTooltipColor(net.neoforged.neoforge.client.event.RenderTooltipEvent.Color event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass().getPackageName().startsWith("com.velorise.cameralockon")) {
            event.setBackgroundStart(0xF0121212);
            event.setBackgroundEnd(0xF0121212);
            event.setBorderStart(0xFF3A3A3A);
            event.setBorderEnd(0xFF242424);
        }
    }

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
