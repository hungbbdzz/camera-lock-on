package com.velorise.cameralockon;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.BiConsumer;

/**
 * Direct manipulation editor for the normalized Target HUD position.
 * Drag the bright HUD card. Clicking the dimmed area applies and exits.
 */
public class HudPositionEditorScreen extends Screen {
    private final Screen parentScreen;
    private final LockOnHudRenderer.HudOptions hudOptions;
    private final BiConsumer<Double, Double> onApply;

    private double anchorX;
    private double anchorY;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    private int hudLeft;
    private int hudTop;
    private int hudWidth;
    private int hudHeight;

    public HudPositionEditorScreen(
            Screen parentScreen,
            double initialAnchorX,
            double initialAnchorY,
            LockOnHudRenderer.HudOptions hudOptions,
            BiConsumer<Double, Double> onApply
    ) {
        super(Component.literal("Adjust Target HUD"));
        this.parentScreen = parentScreen;
        this.anchorX = Mth.clamp(initialAnchorX, 0.0D, 1.0D);
        this.anchorY = Mth.clamp(initialAnchorY, 0.0D, 1.0D);
        this.hudOptions = hudOptions;
        this.onApply = onApply;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xA6000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.11, Screen.renderWithTooltipAndSubtitles renders background prior to render()
        updateHudBounds();

        graphics.drawCenteredString(
                this.font,
                "Drag the HUD to move it",
                this.width / 2,
                12,
                0xFFFFFFFF
        );
        graphics.drawCenteredString(
                this.font,
                "Click outside the HUD or press Esc to apply and exit",
                this.width / 2,
                this.height - 18,
                0xFFD0D0D0
        );

        boolean hovered = isInsideHud(mouseX, mouseY);
        int outline = this.dragging ? 0xFFFFFFFF : hovered ? 0xFFE8FFFF : 0xFF76DDE8;
        graphics.fill(
                this.hudLeft - 3,
                this.hudTop - 3,
                this.hudLeft + this.hudWidth + 3,
                this.hudTop - 1,
                outline
        );
        graphics.fill(
                this.hudLeft - 3,
                this.hudTop + this.hudHeight + 1,
                this.hudLeft + this.hudWidth + 3,
                this.hudTop + this.hudHeight + 3,
                outline
        );
        graphics.fill(
                this.hudLeft - 3,
                this.hudTop - 1,
                this.hudLeft - 1,
                this.hudTop + this.hudHeight + 1,
                outline
        );
        graphics.fill(
                this.hudLeft + this.hudWidth + 1,
                this.hudTop - 1,
                this.hudLeft + this.hudWidth + 3,
                this.hudTop + this.hudHeight + 1,
                outline
        );

                LockOnHudRenderer.renderPreviewCard(
                graphics,
                this.font,
                this.hudLeft,
                this.hudTop,
                this.hudOptions
        );
        
        if (hovered && !this.dragging) {
            graphics.drawCenteredString(
                    this.font,
                    "Drag to move",
                    this.hudLeft + this.hudWidth / 2,
                    Math.max(2, this.hudTop - 13),
                    0xFFFFFFFF
            );
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        updateHudBounds();
        if (isInsideHud(mouseX, mouseY)) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.hudLeft;
            this.dragOffsetY = mouseY - this.hudTop;
            return true;
        }

        applyAndClose();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && this.dragging) {
            updateAnchorFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && this.dragging) {
            updateAnchorFromMouse(mouseX, mouseY);
            this.dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateAnchorFromMouse(double mouseX, double mouseY) {
        int availableX = Math.max(0, this.width - this.hudWidth);
        int availableY = Math.max(0, this.height - this.hudHeight);

        double desiredLeft = Mth.clamp(mouseX - this.dragOffsetX, 0.0D, availableX);
        double desiredTop = Mth.clamp(mouseY - this.dragOffsetY, 0.0D, availableY);

        this.anchorX = availableX == 0 ? 0.0D : desiredLeft / availableX;
        this.anchorY = availableY == 0 ? 0.0D : desiredTop / availableY;
        updateHudBounds();
    }

    private void updateHudBounds() {
        LockOnHudRenderer.HudCardSize unscaled =
                LockOnHudRenderer.measurePreviewCard(this.font, this.hudOptions);
        this.hudWidth = Math.max(1, Math.round(unscaled.width() * this.hudOptions.scale()));
        this.hudHeight = Math.max(1, Math.round(unscaled.height() * this.hudOptions.scale()));

        int[] position = LockOnHudRenderer.calculateAnchoredPosition(
                this.width,
                this.height,
                this.hudWidth,
                this.hudHeight,
                this.anchorX,
                this.anchorY
        );
        this.hudLeft = position[0];
        this.hudTop = position[1];
    }

    private boolean isInsideHud(double mouseX, double mouseY) {
        return mouseX >= this.hudLeft
                && mouseX <= this.hudLeft + this.hudWidth
                && mouseY >= this.hudTop
                && mouseY <= this.hudTop + this.hudHeight;
    }

    private void applyAndClose() {
        this.onApply.accept(this.anchorX, this.anchorY);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void onClose() {
        applyAndClose();
    }
}
