package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Applies consistent dark tooltip styling to Camera Lock-On configuration screens. */
@EventBusSubscriber(modid = CameraLockOn.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ConfigScreenExtras {
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
}
