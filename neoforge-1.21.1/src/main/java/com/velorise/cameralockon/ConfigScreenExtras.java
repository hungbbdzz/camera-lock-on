package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Adds compact combat shortcuts without duplicating the existing tabbed config implementation. */
@EventBusSubscriber(modid = CameraLockOn.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ConfigScreenExtras {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 260;

    private ConfigScreenExtras() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof LockOnConfigScreen screen)) {
            return;
        }

        int left = (screen.width - PANEL_WIDTH) / 2;
        int top = (screen.height - PANEL_HEIGHT) / 2;
        int y = top + PANEL_HEIGHT - 47;

        Component presetText = ClientFeatureStore.getLastLoadedPreset().isBlank()
                ? Component.translatable("gui.camera_lockon.button.presets")
                : Component.translatable("gui.camera_lockon.format.keyValue", Component.translatable("gui.camera_lockon.button.preset_active"), Component.literal(compact(ClientFeatureStore.getLastLoadedPreset(), 12)));

        Button presets = Button.builder(presetText, button ->
                        Minecraft.getInstance().setScreen(new PresetManagerScreen(screen)))
                .bounds(left + 12, top + PANEL_HEIGHT - 25, 102, 18)
                .build();
        presets.setTooltip(Tooltip.create(Component.translatable("gui.camera_lockon.button.presets.tooltip")));
        event.addListener(presets);
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
