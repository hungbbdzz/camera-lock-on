package com.velorise.cameralockon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.BiConsumer;

/** Visual editor for the global normalized camera aim point. */
public class AimPointConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 224;
    private static final int PREVIEW_WIDTH = 210;
    private static final int PREVIEW_HEIGHT = 116;

    private final Screen parentScreen;
    private final BiConsumer<Double, Double> onApply;
    private final double originalX;
    private final double originalY;

    private double aimPointX;
    private double aimPointY;
    private boolean dragging;
    private int previewLeft;
    private int previewTop;

    public AimPointConfigScreen(
            Screen parentScreen,
            double initialX,
            double initialY,
            BiConsumer<Double, Double> onApply
    ) {
        this(parentScreen, "Global Camera Aim Point", initialX, initialY, onApply);
    }

    public AimPointConfigScreen(
            Screen parentScreen,
            String title,
            double initialX,
            double initialY,
            BiConsumer<Double, Double> onApply
    ) {
        super(Component.literal(title));
        this.parentScreen = parentScreen;
        this.onApply = onApply;
        this.originalX = Mth.clamp(initialX, -1.0D, 1.0D);
        this.originalY = Mth.clamp(initialY, 0.0D, 1.0D);
        this.aimPointX = this.originalX;
        this.aimPointY = this.originalY;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.previewLeft = left + (PANEL_WIDTH - PREVIEW_WIDTH) / 2;
        this.previewTop = top + 48;

        Button center = Button.builder(Component.literal("Reset to Center"), button -> {
                    this.aimPointX = 0.0D;
                    this.aimPointY = 0.5D;
                })
                .bounds(left + 45, top + 171, 210, 18)
                .build();
        center.setTooltip(Tooltip.create(Component.literal("Restore the global aim point to the middle of the target hitbox.")));
        this.addRenderableWidget(center);

        this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                    this.onApply.accept(this.aimPointX, this.aimPointY);
                    returnToParent();
                })
                .bounds(left + 12, top + 196, 132, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
                    this.aimPointX = this.originalX;
                    this.aimPointY = this.originalY;
                    returnToParent();
                })
                .bounds(left + 156, top + 196, 132, 18)
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsidePreview(mouseX, mouseY)) {
            this.dragging = true;
            updatePointFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.dragging) {
            updatePointFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragging) {
            updatePointFromMouse(mouseX, mouseY);
            this.dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= this.previewLeft
                && mouseX <= this.previewLeft + PREVIEW_WIDTH
                && mouseY >= this.previewTop
                && mouseY <= this.previewTop + PREVIEW_HEIGHT;
    }

    private void updatePointFromMouse(double mouseX, double mouseY) {
        double normalizedX = Mth.clamp((mouseX - this.previewLeft) / PREVIEW_WIDTH, 0.0D, 1.0D);
        double normalizedY = Mth.clamp((mouseY - this.previewTop) / PREVIEW_HEIGHT, 0.0D, 1.0D);
        this.aimPointX = normalizedX * 2.0D - 1.0D;
        this.aimPointY = 1.0D - normalizedY;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        super.render(graphics, mouseX, mouseY, partialTick);

        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 12, 0xFFFFFFFF);
        graphics.drawCenteredString(
                this.font,
                "Drag the X to choose where the camera aims",
                this.width / 2,
                top + 29,
                0xFFD0D0D0
        );

        drawPreviewRectangle(graphics);
        drawAimCross(graphics);

        String coordinateText = String.format(
                Locale.ROOT,
                "Horizontal %.2f   •   Vertical %.2f",
                this.aimPointX,
                this.aimPointY
        );
        graphics.drawCenteredString(this.font, coordinateText, this.width / 2, top + 150, 0xFFFFFFFF);
        graphics.drawCenteredString(
                this.font,
                "Bottom 0.00   •   Center 0.50   •   Top 1.00",
                this.width / 2,
                top + 160,
                0xFFB0B0B0
        );
    }

    private void drawPreviewRectangle(GuiGraphics graphics) {
        int right = this.previewLeft + PREVIEW_WIDTH;
        int bottom = this.previewTop + PREVIEW_HEIGHT;
        graphics.fill(this.previewLeft - 2, this.previewTop - 2, right + 2, bottom + 2, 0xFF707070);
        graphics.fill(this.previewLeft, this.previewTop, right, bottom, 0xFF242424);

        int centerX = this.previewLeft + PREVIEW_WIDTH / 2;
        int centerY = this.previewTop + PREVIEW_HEIGHT / 2;
        graphics.fill(centerX, this.previewTop, centerX + 1, bottom, 0xFF555555);
        graphics.fill(this.previewLeft, centerY, right, centerY + 1, 0xFF555555);
    }

    private void drawAimCross(GuiGraphics graphics) {
        int crossX = this.previewLeft
                + (int) Math.round((this.aimPointX + 1.0D) * 0.5D * PREVIEW_WIDTH);
        int crossY = this.previewTop
                + (int) Math.round((1.0D - this.aimPointY) * PREVIEW_HEIGHT);
        crossX = Mth.clamp(crossX, this.previewLeft, this.previewLeft + PREVIEW_WIDTH);
        crossY = Mth.clamp(crossY, this.previewTop, this.previewTop + PREVIEW_HEIGHT);
        drawCross(graphics, crossX, crossY);
    }

    static void drawCross(GuiGraphics graphics, int x, int y) {
        int radius = 6;
        graphics.fill(x - radius, y - 1, x + radius + 1, y + 2, 0xFF000000);
        graphics.fill(x - 1, y - radius, x + 2, y + radius + 1, 0xFF000000);
        graphics.fill(x - radius + 1, y, x + radius, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y - radius + 1, x + 1, y + radius, 0xFFFFFFFF);
    }

    static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left - 3, top - 3, left + width + 3, top + height + 3, 0xC0777777);
        graphics.fill(left, top, left + width, top + height, 0xC0181818);
        graphics.fill(left, top, left + width, top + 1, 0xD0B8B8B8);
    }

    private void returnToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }
}
