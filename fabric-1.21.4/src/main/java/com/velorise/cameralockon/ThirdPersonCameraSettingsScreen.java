package com.velorise.cameralockon;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Responsive third-person camera editor that also fits small windowed GUI sizes. */
public final class ThirdPersonCameraSettingsScreen extends Screen {
    private static final int MAX_WIDTH = 360;
    private final Screen parent;
    private ClientFeatureStore.CameraSlot editingSlot;

    public ThirdPersonCameraSettingsScreen(Screen parent) {
        super(Component.translatable("gui.camera_lockon.camera.third_person_settings"));
        this.parent = parent;
        this.editingSlot = ClientFeatureStore.getActiveCameraSlot();
    }

    private int contentWidth() {
        return Math.min(MAX_WIDTH, Math.max(220, this.width - 8));
    }

    private int contentLeft() {
        return (this.width - contentWidth()) / 2;
    }

    @Override
    protected void init() {
        boolean compact = this.height < 300;
        int rowHeight = compact ? 17 : 20;
        int gap = compact ? 2 : 4;
        int width = contentWidth();
        int left = contentLeft();
        int columnGap = 6;
        int half = (width - columnGap) / 2;
        int right = left + half + columnGap;
        int y = compact ? 2 : 6;

        Button modeButton = Button.builder(modeLabel(), b -> {
            ClientFeatureStore.setThirdPersonCameraMode(
                    ClientFeatureStore.getThirdPersonCameraMode().next());
            b.setMessage(modeLabel());
        }).bounds(left, y, half, rowHeight).build();
        modeButton.setTooltip(tooltip("gui.camera_lockon.camera.mode.tooltip"));
        addRenderableWidget(modeButton);

        Button slotButton = Button.builder(slotLabel(), b -> {
            editingSlot = editingSlot.next();
            ClientFeatureStore.setActiveCameraSlot(editingSlot);
            rebuildWidgets();
        }).bounds(right, y, half, rowHeight).build();
        slotButton.setTooltip(tooltip("gui.camera_lockon.camera.position_slot.tooltip"));
        addRenderableWidget(slotButton);
        y += rowHeight + gap;

        Button aimStyleButton = Button.builder(aimStyleLabel(), b -> {
            ClientFeatureStore.setThirdPersonAimStyle(
                    ClientFeatureStore.getThirdPersonAimStyle().next());
            b.setMessage(aimStyleLabel());
        }).bounds(left, y, half, rowHeight).build();
        aimStyleButton.setTooltip(tooltip("gui.camera_lockon.camera.aim_style.tooltip"));
        addRenderableWidget(aimStyleButton);

        Button cursorModeButton = Button.builder(cursorModeLabel(), b -> {
            ClientFeatureStore.setFreeCameraCursorMode(
                    ClientFeatureStore.getFreeCameraCursorMode().next());
            b.setMessage(cursorModeLabel());
        }).bounds(right, y, half, rowHeight).build();
        cursorModeButton.setTooltip(tooltip("gui.camera_lockon.camera.cursor_mode.tooltip"));
        addRenderableWidget(cursorModeButton);
        y += rowHeight + gap;

        ValueSlider thirdPersonStrength = new ValueSlider(left, y, half, rowHeight,
                "gui.camera_lockon.camera.third_person_aim_strength", 0.25, 2.0,
                CameraLockOnConfig.THIRD_PERSON_AIM_STRENGTH.get(),
                value -> CameraLockOnConfig.THIRD_PERSON_AIM_STRENGTH.set(value));
        thirdPersonStrength.setTooltip(tooltip(
                "gui.camera_lockon.camera.third_person_aim_strength.tooltip"));
        addRenderableWidget(thirdPersonStrength);

        Button aimRayButton = Button.builder(aimRayLabel(), b -> {
            CameraLockOnConfig.AimRayMode next = CameraLockOnConfig.AimRayMode
                    .fromConfig(CameraLockOnConfig.AIM_RAY_MODE.get()).next();
            CameraLockOnConfig.AIM_RAY_MODE.set(next.name());
            b.setMessage(aimRayLabel());
        }).bounds(right, y, half, rowHeight).build();
        aimRayButton.setTooltip(tooltip("gui.camera_lockon.camera.aim_ray.tooltip"));
        addRenderableWidget(aimRayButton);
        y += rowHeight + gap * 2;

        for (ClientFeatureStore.CameraSlot slot : ClientFeatureStore.CameraSlot.values()) {
            final ClientFeatureStore.CameraSlot captured = slot;
            int col = slot.ordinal() % 2;
            int row = slot.ordinal() / 2;
            Button cycleButton = Button.builder(cycleLabel(captured), b -> {
                ClientFeatureStore.setCameraSlotEnabledInCycle(captured,
                        !ClientFeatureStore.isCameraSlotEnabledInCycle(captured));
                b.setMessage(cycleLabel(captured));
            }).bounds(col == 0 ? left : right,
                    y + row * (rowHeight + gap), half, rowHeight).build();
            cycleButton.setTooltip(tooltip("gui.camera_lockon.camera.cycle_slot.tooltip"));
            addRenderableWidget(cycleButton);
        }
        y += 3 * (rowHeight + gap) + gap;

        ClientFeatureStore.CameraPosition pos =
                ClientFeatureStore.getCameraPosition(editingSlot);
        ValueSlider horizontal = new ValueSlider(left, y, width, rowHeight,
                "gui.camera_lockon.camera.horizontal_offset", -3.0, 3.0,
                pos.horizontalOffset(), v -> update(v, null, null));
        horizontal.setTooltip(tooltip("gui.camera_lockon.camera.horizontal_offset.tooltip"));
        addRenderableWidget(horizontal);
        y += rowHeight + gap;

        ValueSlider vertical = new ValueSlider(left, y, width, rowHeight,
                "gui.camera_lockon.camera.vertical_offset", -0.5, 1.5,
                pos.verticalOffset(), v -> update(null, v, null));
        vertical.setTooltip(tooltip("gui.camera_lockon.camera.vertical_offset.tooltip"));
        addRenderableWidget(vertical);
        y += rowHeight + gap;

        ValueSlider distance = new ValueSlider(left, y, width, rowHeight,
                "gui.camera_lockon.camera.distance_offset", -2.0, 2.0,
                pos.distanceOffset(), v -> update(null, null, v));
        distance.setTooltip(tooltip("gui.camera_lockon.camera.distance_offset.tooltip"));
        addRenderableWidget(distance);

        int footerY = this.height - rowHeight - 3;
        int footerGap = 5;
        int footerW = (width - footerGap * 3) / 4;

        Button editButton = Button.builder(
                Component.translatable("gui.camera_lockon.camera.custom_editor"), b ->
                        this.minecraft.setScreen(new CustomCameraPositionScreen(this, editingSlot)))
                .bounds(left, footerY, footerW, rowHeight).build();
        editButton.setTooltip(tooltip("gui.camera_lockon.camera.custom_editor.tooltip"));
        addRenderableWidget(editButton);

        Button mirrorButton = Button.builder(
                Component.translatable("gui.camera_lockon.camera.mirror"), b -> {
            ClientFeatureStore.CameraPosition p =
                    ClientFeatureStore.getCameraPosition(editingSlot);
            ClientFeatureStore.setCameraPosition(editingSlot,
                    new ClientFeatureStore.CameraPosition(
                            -p.horizontalOffset(), p.verticalOffset(), p.distanceOffset()));
            rebuildWidgets();
        }).bounds(left + footerW + footerGap, footerY, footerW, rowHeight).build();
        mirrorButton.setTooltip(tooltip("gui.camera_lockon.camera.mirror.tooltip"));
        addRenderableWidget(mirrorButton);

        Button resetButton = Button.builder(
                Component.translatable("gui.camera_lockon.button.reset_defaults"), b -> {
            ClientFeatureStore.resetCameraDefaults();
            editingSlot = ClientFeatureStore.getActiveCameraSlot();
            rebuildWidgets();
        }).bounds(left + (footerW + footerGap) * 2,
                footerY, footerW, rowHeight).build();
        resetButton.setTooltip(tooltip("gui.camera_lockon.button.reset_defaults.tooltip"));
        addRenderableWidget(resetButton);

        Button doneButton = Button.builder(
                Component.translatable("gui.camera_lockon.button.done"), b -> onClose())
                .bounds(left + (footerW + footerGap) * 3,
                        footerY, footerW, rowHeight).build();
        doneButton.setTooltip(tooltip("gui.camera_lockon.button.done.tooltip"));
        addRenderableWidget(doneButton);
    }

    private static Tooltip tooltip(String key) {
        return Tooltip.create(Component.translatable(key).withStyle(ChatFormatting.WHITE));
    }

    private void update(Double horizontal, Double vertical, Double distance) {
        ClientFeatureStore.CameraPosition p =
                ClientFeatureStore.getCameraPosition(editingSlot);
        ClientFeatureStore.setCameraPosition(editingSlot,
                new ClientFeatureStore.CameraPosition(
                        horizontal == null ? p.horizontalOffset() : horizontal,
                        vertical == null ? p.verticalOffset() : vertical,
                        distance == null ? p.distanceOffset() : distance));
    }

    private Component modeLabel() {
        return Component.literal("Mode: ")
                .append(ClientFeatureStore.getThirdPersonCameraMode().getDisplayName());
    }

    private Component slotLabel() {
        return Component.literal("Position: ").append(editingSlot.getDisplayName());
    }

    private Component aimStyleLabel() {
        return Component.literal("Aim: ")
                .append(ClientFeatureStore.getThirdPersonAimStyle().getDisplayName());
    }

    private Component cursorModeLabel() {
        return Component.literal("Cursor: ")
                .append(ClientFeatureStore.getFreeCameraCursorMode().getDisplayName());
    }

    private Component aimRayLabel() {
        return Component.literal("Aim Ray: ").append(
                CameraLockOnConfig.AimRayMode
                        .fromConfig(CameraLockOnConfig.AIM_RAY_MODE.get())
                        .getDisplayName());
    }

    private Component cycleLabel(ClientFeatureStore.CameraSlot slot) {
        return Component.literal(
                ClientFeatureStore.isCameraSlotEnabledInCycle(slot) ? "[x] " : "[ ] ")
                .append(slot.getDisplayName());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // With no loaded world there is no scene render to clear the framebuffer.
        // Use an opaque fallback so dragged preview frames and parent widgets cannot persist.
        if (this.minecraft == null || this.minecraft.level == null) {
            graphics.fill(0, 0, this.width, this.height, 0xFF101218);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final java.util.function.DoubleConsumer consumer;

        ValueSlider(
                int x, int y, int width, int height,
                String key, double min, double max, double current,
                java.util.function.DoubleConsumer consumer
        ) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.consumer = consumer;
            updateMessage();
        }

        private double actual() {
            return min + value * (max - min);
        }

        @Override
        protected void updateMessage() {
            double actual = actual();
            Component message = Component.translatable(key).append(": "
                    + String.format(java.util.Locale.ROOT, "%.2f", actual));
            if (key.endsWith("aim_strength") && actual > 1.25D) {
                message = message.copy().append(
                        Component.literal(" !").withStyle(ChatFormatting.GOLD));
            }
            setMessage(message);
        }

        @Override
        protected void applyValue() {
            consumer.accept(actual());
        }
    }
}
