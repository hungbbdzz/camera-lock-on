package com.velorise.cameralockon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class SafeServerConfirmScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 220;
    private static final int LIST_WIDTH = 292;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 6;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Screen parent;
    private final Runnable onConfirm;
    private final List<SettingDiff> diffs = new ArrayList<>();

    private int listLeft;
    private int listTop;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public SafeServerConfirmScreen(Screen parent, Runnable onConfirm) {
        super(Component.translatable("gui.camera_lockon.safe_server.title"));
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    public static final class SettingDiff {
        public final Component name;
        public final Component fromValue;
        public final Component toValue;

        public SettingDiff(Component name, Component fromValue, Component toValue) {
            this.name = name;
            this.fromValue = fromValue;
            this.toValue = toValue;
        }
    }

    private void populateDiffs() {
        this.diffs.clear();
        Component onText = Component.translatable("gui.camera_lockon.on");
        Component offText = Component.translatable("gui.camera_lockon.off");

        if (CameraLockOnConfig.AUTO_LOCK.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.auto.auto_lock"), onText, offText));
        }
        if (CameraLockOnConfig.AUTO_RETARGET.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.auto.auto_retarget"), onText, offText));
        }
        if (CameraLockOnConfig.AUTO_LOCK_INDICATOR.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.auto.pixel_indicator"), onText, offText));
        }
        if (!CameraLockOnConfig.PROJECTILE_ASSIST_MODE.get().equals(CameraLockOnConfig.ProjectileAssistMode.OFF.name())) {
            diffs.add(new SettingDiff(
                    Component.translatable("gui.camera_lockon.main.projectile_assist"),
                    CameraLockOnConfig.ProjectileAssistMode.fromConfig(CameraLockOnConfig.PROJECTILE_ASSIST_MODE.get()).getDisplayName(),
                    CameraLockOnConfig.ProjectileAssistMode.OFF.getDisplayName()
            ));
        }
        if (!CameraLockOnConfig.MULTIPART_AIM_MODE.get().equals(CameraLockOnConfig.MultipartAimMode.OFF.name())) {
            diffs.add(new SettingDiff(
                    Component.translatable("gui.camera_lockon.projectile_settings.multipart_aim_mode"),
                    CameraLockOnConfig.MultipartAimMode.fromConfig(CameraLockOnConfig.MULTIPART_AIM_MODE.get()).getDisplayName(),
                    CameraLockOnConfig.MultipartAimMode.OFF.getDisplayName()
            ));
        }
        if (CameraLockOnConfig.SMART_PROJECTILE_HITBOX.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.camera.smart_projectile_hitbox"), onText, offText));
        }
        if (CameraLockOnConfig.AUTO_RELEASE_BOW.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.camera.auto_release_bow"), onText, offText));
        }
        if (CameraLockOnConfig.AUTO_RECHARGE_BOW.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.camera.auto_recharge_bow"), onText, offText));
        }
        if (CameraLockOnConfig.TRAJECTORY_PREVIEW.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.projectile_settings.trajectory_preview"), onText, offText));
        }
        if (CameraLockOnConfig.TRAJECTORY_SHOW_WITHOUT_LOCK.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.projectile_settings.show_without_lock"), onText, offText));
        }
        if (CameraLockOnConfig.TARGET_OUTLINE.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.hud.target_outline"), onText, offText));
        }
        if (CameraLockOnConfig.ADAPTIVE_AIM_CALIBRATION.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.projectile_settings.adaptive_calibration"), onText, offText));
        }
        if (!CameraLockOnConfig.LINE_OF_SIGHT_MODE.get().equals(CameraLockOnConfig.LineOfSightMode.STRICT.name())) {
            diffs.add(new SettingDiff(
                    Component.translatable("gui.camera_lockon.camera.line_of_sight"),
                    CameraLockOnConfig.LineOfSightMode.fromConfig(CameraLockOnConfig.LINE_OF_SIGHT_MODE.get()).getDisplayName(),
                    CameraLockOnConfig.LineOfSightMode.STRICT.getDisplayName()
            ));
        }
        if (CameraLockOnConfig.OCCLUDED_STEERING.get()) {
            diffs.add(new SettingDiff(Component.translatable("gui.camera_lockon.camera.occluded_steering"), onText, offText));
        }
        if (ClientFeatureStore.getSwitchTargetMode() != ClientFeatureStore.SwitchTargetMode.SMART) {
            diffs.add(new SettingDiff(
                    Component.translatable("gui.camera_lockon.auto.switch"),
                    ClientFeatureStore.getSwitchTargetMode().getDisplayName(),
                    ClientFeatureStore.SwitchTargetMode.SMART.getDisplayName()
            ));
        }
        if (ClientFeatureStore.getTargetPriority() != ClientFeatureStore.TargetPriority.BALANCED) {
            diffs.add(new SettingDiff(
                    Component.translatable("gui.camera_lockon.filter.target_priority"),
                    ClientFeatureStore.getTargetPriority().getDisplayName(),
                    ClientFeatureStore.TargetPriority.BALANCED.getDisplayName()
            ));
        }
    }

    @Override
    protected void init() {
        populateDiffs();
        clampScrollOffset();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.listLeft = left + 14;
        this.listTop = top + 42;

        int buttonY = top + PANEL_HEIGHT - 28;
        int buttonWidth = 138;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.camera_lockon.safe_server.cancel"),
                b -> this.onClose()
        ).bounds(left + 14, buttonY, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.camera_lockon.safe_server.apply"),
                b -> {
                    this.onConfirm.run();
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                    }
                }
        ).bounds(left + PANEL_WIDTH - 14 - buttonWidth, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private int maxScroll() {
        return Math.max(0, this.diffs.size() - VISIBLE_ROWS);
    }

    private void clampScrollOffset() {
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());
    }

    private int scrollbarLeft() {
        return this.listLeft + LIST_WIDTH - SCROLLBAR_WIDTH;
    }

    private int thumbHeight() {
        if (this.diffs.size() <= VISIBLE_ROWS) {
            return LIST_HEIGHT;
        }
        return Math.max(18, LIST_HEIGHT * VISIBLE_ROWS / this.diffs.size());
    }

    private int thumbTop() {
        int max = maxScroll();
        if (max <= 0) return this.listTop;
        int track = LIST_HEIGHT - thumbHeight();
        return this.listTop + (int) Math.round(track * (this.scrollOffset / (double) max));
    }

    private void updateScrollFromMouse(double mouseY) {
        int height = thumbHeight();
        int track = Math.max(1, LIST_HEIGHT - height);
        double fraction = Mth.clamp((mouseY - this.listTop - this.scrollbarGrabOffset) / track, 0.0D, 1.0D);
        this.scrollOffset = (int) Math.round(fraction * maxScroll());
        clampScrollOffset();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        AimPointConfigScreen.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.camera_lockon.safe_server.description"), this.width / 2, top + 26, 0xFFAAAAAA);

        renderList(graphics, mouseX, mouseY);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(this.listLeft - 1, this.listTop - 1, this.listLeft + LIST_WIDTH + 1, this.listTop + LIST_HEIGHT + 1, 0xFF505050);
        graphics.fill(this.listLeft, this.listTop, this.listLeft + LIST_WIDTH, this.listTop + LIST_HEIGHT, 0xFF1E1E1E);

        int usableWidth = (maxScroll() > 0) ? (LIST_WIDTH - SCROLLBAR_WIDTH - 2) : LIST_WIDTH;

        if (this.diffs.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.camera_lockon.safe_server.no_changes"),
                    this.listLeft + usableWidth / 2,
                    this.listTop + LIST_HEIGHT / 2 - 4,
                    0xFF88FF88
            );
        } else {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int index = this.scrollOffset + row;
                if (index >= this.diffs.size()) break;

                SettingDiff diff = this.diffs.get(index);
                int rowY = this.listTop + row * ROW_HEIGHT;
                boolean hovered = mouseX >= this.listLeft && mouseX < this.listLeft + usableWidth
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

                graphics.fill(
                        this.listLeft + 1,
                        rowY + 1,
                        this.listLeft + usableWidth - 1,
                        rowY + ROW_HEIGHT - 1,
                        hovered ? 0xFF2E3440 : (row % 2 == 0 ? 0xFF242424 : 0xFF1C1C1C)
                );

                String nameStr = diff.name.getString();
                String clippedName = this.font.plainSubstrByWidth(nameStr, usableWidth - 110);
                graphics.drawString(this.font, clippedName, this.listLeft + 6, rowY + 6, 0xFFE0E0E0, false);

                String fromStr = diff.fromValue.getString();
                String arrowStr = " -> ";
                String toStr = diff.toValue.getString();

                int toWidth = this.font.width(toStr);
                int arrowWidth = this.font.width(arrowStr);
                int fromWidth = this.font.width(fromStr);

                int rightX = this.listLeft + usableWidth - 6;
                int toX = rightX - toWidth;
                int arrowX = toX - arrowWidth;
                int fromX = arrowX - fromWidth;

                graphics.drawString(this.font, fromStr, fromX, rowY + 6, 0xFFFF7777, false);
                graphics.drawString(this.font, arrowStr, arrowX, rowY + 6, 0xFF888888, false);
                graphics.drawString(this.font, toStr, toX, rowY + 6, 0xFF55FF55, false);
            }
        }

        if (maxScroll() > 0) {
            int barLeft = scrollbarLeft();
            graphics.fill(barLeft, this.listTop, barLeft + SCROLLBAR_WIDTH, this.listTop + LIST_HEIGHT, 0xFF141414);
            int thumbH = thumbHeight();
            int thumbY = thumbTop();
            graphics.fill(barLeft + 1, thumbY, barLeft + SCROLLBAR_WIDTH - 1, thumbY + thumbH, 0xFF888888);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0) {
            int barLeft = scrollbarLeft();
            if (mouseX >= barLeft && mouseX <= barLeft + SCROLLBAR_WIDTH && mouseY >= this.listTop && mouseY <= this.listTop + LIST_HEIGHT) {
                int top = thumbTop();
                int height = thumbHeight();
                if (mouseY >= top && mouseY <= top + height) {
                    this.scrollbarGrabOffset = (int) mouseY - top;
                } else {
                    this.scrollbarGrabOffset = height / 2;
                    updateScrollFromMouse(mouseY);
                }
                this.draggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (maxScroll() > 0 && scrollDelta != 0.0D) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.signum(scrollDelta), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }
}

