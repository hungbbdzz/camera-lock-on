package com.velorise.cameralockon;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Drag-and-drop editor for custom third-person camera framing. */
public final class CustomCameraPositionScreen extends Screen {
    private static final double MIN_X = 0.08D;
    private static final double MAX_X = 0.92D;
    private static final int TOOLBAR_Y = 7;
    private static final int TOOLBAR_HEIGHT = 18;

    private final Screen parent;
    private final ClientFeatureStore.CameraSlot slot;
    private boolean dragging;

    public CustomCameraPositionScreen(Screen parent, ClientFeatureStore.CameraSlot slot) {
        super(Component.translatable("gui.camera_lockon.camera.custom_editor"));
        this.parent = parent;
        this.slot = slot;
    }

    private int previewTop() {
        return TOOLBAR_Y + TOOLBAR_HEIGHT + 10;
    }

    private int previewBottom() {
        return Math.max(previewTop() + 70, this.height - 12);
    }

    @Override
    protected void init() {
        int usable = Math.min(330, Math.max(150, this.width - 20));
        int gap = 6;
        int buttonWidth = (usable - gap * 2) / 3;
        int left = (this.width - usable) / 2;

        Button mirrorButton = Button.builder(
                Component.translatable("gui.camera_lockon.camera.mirror"), button -> {
                    ClientFeatureStore.CameraPosition position =
                            ClientFeatureStore.getCameraPosition(slot);
                    ClientFeatureStore.setCameraPosition(slot,
                            new ClientFeatureStore.CameraPosition(
                                    -position.horizontalOffset(),
                                    position.verticalOffset(),
                                    position.distanceOffset()));
                }).bounds(left, TOOLBAR_Y, buttonWidth, TOOLBAR_HEIGHT).build();
        mirrorButton.setTooltip(tooltip("gui.camera_lockon.camera.mirror.tooltip"));
        addRenderableWidget(mirrorButton);

        Button resetButton = Button.builder(
                Component.translatable("gui.camera_lockon.button.reset_defaults"), button ->
                        ClientFeatureStore.setCameraPosition(slot, defaultFor(slot)))
                .bounds(left + buttonWidth + gap, TOOLBAR_Y,
                        buttonWidth, TOOLBAR_HEIGHT).build();
        resetButton.setTooltip(tooltip("gui.camera_lockon.button.reset_defaults.tooltip"));
        addRenderableWidget(resetButton);

        Button doneButton = Button.builder(
                Component.translatable("gui.camera_lockon.button.done"), button -> onClose())
                .bounds(left + (buttonWidth + gap) * 2, TOOLBAR_Y,
                        buttonWidth, TOOLBAR_HEIGHT).build();
        doneButton.setTooltip(tooltip("gui.camera_lockon.button.done.tooltip"));
        addRenderableWidget(doneButton);
    }

    private static Tooltip tooltip(String key) {
        return Tooltip.create(Component.translatable(key).withStyle(ChatFormatting.WHITE));
    }

    private static ClientFeatureStore.CameraPosition defaultFor(
            ClientFeatureStore.CameraSlot slot
    ) {
        return switch (slot) {
            case LEFT_SHOULDER ->
                    new ClientFeatureStore.CameraPosition(-0.95D, 0.34D, 0.10D);
            case RIGHT_SHOULDER ->
                    new ClientFeatureStore.CameraPosition(0.95D, 0.34D, 0.10D);
            case CENTERED ->
                    new ClientFeatureStore.CameraPosition(0.0D, 0.0D, 0.0D);
            case CUSTOM_1 ->
                    new ClientFeatureStore.CameraPosition(-1.35D, 0.48D, 0.20D);
            case CUSTOM_2 ->
                    new ClientFeatureStore.CameraPosition(1.35D, 0.48D, 0.20D);
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insideSafeArea(mouseX, mouseY)) {
            dragging = true;
            updateFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY
    ) {
        if (dragging && button == 0) {
            updateFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean insideSafeArea(double mouseX, double mouseY) {
        return mouseX >= this.width * MIN_X
                && mouseX <= this.width * MAX_X
                && mouseY >= previewTop()
                && mouseY <= previewBottom();
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        double normalizedX = Mth.clamp(mouseX / this.width, MIN_X, MAX_X);
        double normalizedY = Mth.clamp(
                (mouseY - previewTop())
                        / Math.max(1.0D, previewBottom() - previewTop()),
                0.0D,
                1.0D
        );
        double horizontal = (normalizedX - 0.5D) * 6.0D;
        double vertical = -0.5D + normalizedY * 2.0D;
        ClientFeatureStore.CameraPosition old =
                ClientFeatureStore.getCameraPosition(slot);
        ClientFeatureStore.setCameraPosition(slot,
                new ClientFeatureStore.CameraPosition(
                        horizontal, vertical, old.distanceOffset()));
    }

    @Override
    public void renderBackground(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick
    ) {
        // Keep the live world sharp while editing.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Outside a world, no scene frame exists to erase the previous preview.
        if (this.minecraft == null || this.minecraft.level == null) {
            graphics.fill(0, 0, this.width, this.height, 0xFF101218);
        }

        int left = (int) (this.width * MIN_X);
        int right = (int) (this.width * MAX_X);
        int top = previewTop();
        int bottom = previewBottom();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.fill(centerX - 7, centerY, centerX + 8, centerY + 1, 0xFFFFFFFF);
        graphics.fill(centerX, centerY - 7, centerX + 1, centerY + 8, 0xFFFFFFFF);

        ClientFeatureStore.CameraPosition position =
                ClientFeatureStore.getCameraPosition(slot);
        int playerX = (int) Math.round(
                (0.5D + position.horizontalOffset() / 6.0D) * this.width);
        double normalizedY = Mth.clamp(
                (position.verticalOffset() + 0.5D) / 2.0D, 0.0D, 1.0D);
        int playerY = (int) Math.round(top + normalizedY * (bottom - top));
        int boxWidth = 30;
        int boxHeight = 50;
        playerX = Mth.clamp(playerX, left + boxWidth / 2, right - boxWidth / 2);
        playerY = Mth.clamp(playerY, top + boxHeight / 2, bottom - boxHeight / 2);

        graphics.fill(
                playerX - boxWidth / 2,
                playerY - boxHeight / 2,
                playerX + boxWidth / 2,
                playerY + boxHeight / 2,
                0x6633AAFF
        );
        graphics.renderOutline(
                playerX - boxWidth / 2,
                playerY - boxHeight / 2,
                boxWidth,
                boxHeight,
                0xCCFFFFFF
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
