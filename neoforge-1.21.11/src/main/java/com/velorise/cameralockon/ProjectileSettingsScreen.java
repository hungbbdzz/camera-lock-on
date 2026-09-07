package com.velorise.cameralockon;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Dedicated projectile and ranged automation settings screen. */
public final class ProjectileSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 286;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_STEP = 18;
    private static final int CONTENT_WIDTH = 306;

    private final Screen parent;
    private boolean advancedExpanded;
    private int scrollOffset;
    private int maxScroll;
    private int contentTop;
    private int contentVisibleHeight;

    public ProjectileSettingsScreen(Screen parent) {
        super(Component.translatable("gui.camera_lockon.projectile_settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelHeight = currentPanelHeight();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - panelHeight) / 2;
        int x = left + 12;

        this.contentTop = top + 26;
        this.contentVisibleHeight = panelHeight - 70;

        int contentHeight = computeContentHeight();
        this.maxScroll = Math.max(0, contentHeight - this.contentVisibleHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

        buildContent(x);

        if (this.maxScroll > 0) {
            this.addRenderableWidget(new ScrollBar(left + PANEL_WIDTH - 10, this.contentTop, 6, this.contentVisibleHeight));
        }

        Button done = Button.builder(Component.translatable("gui.camera_lockon.button.done"), button -> closeAndSave())
                .bounds(left + 114, top + panelHeight - 24, 102, 18).build();
        Tooltip doneTooltip = tooltipIfPresent("gui.camera_lockon.button.done");
        if (doneTooltip != null) done.setTooltip(doneTooltip);
        this.addRenderableWidget(done);
    }

    private int currentPanelHeight() {
        return Math.max(200, Math.min(PANEL_HEIGHT, this.height - 12));
    }

    private int computeContentHeight() {
        int y = 0;
        y += 11 + ROW_STEP + ROW_STEP + ROW_STEP + ROW_STEP; // automation
        y += 11 + ROW_STEP + ROW_STEP + ROW_STEP + ROW_STEP; // prediction
        y += ROW_STEP; // advanced toggle
        if (this.advancedExpanded) {
            y += ROW_STEP + ROW_STEP + ROW_STEP + ROW_STEP + ROW_STEP;
        }
        return y;
    }

    private void buildContent(int x) {
        int y = 0;

        addHeaderRow(x, y, "gui.camera_lockon.projectile_settings.automation"); y += 11;
        addToggleRow(x, y, "gui.camera_lockon.camera.auto_release_bow", CameraLockOnConfig.AUTO_RELEASE_BOW);
        addToggleRow(x + 156, y, "gui.camera_lockon.camera.auto_recharge_bow", CameraLockOnConfig.AUTO_RECHARGE_BOW); y += ROW_STEP;
        addSliderRow(x, y, 150, "gui.camera_lockon.camera.auto_release_charge", "gui.camera_lockon.unit.percent", 25, 100,
                () -> CameraLockOnConfig.AUTO_RELEASE_BOW_CHARGE.get() * 100.0D,
                value -> CameraLockOnConfig.AUTO_RELEASE_BOW_CHARGE.set(value / 100.0D), 0);
        addSliderRow(x + 156, y, 150, "gui.camera_lockon.projectile_settings.aim_tolerance", "gui.camera_lockon.unit.degrees", 0.5, 10,
                CameraLockOnConfig.AUTO_RELEASE_AIM_TOLERANCE::get,
                value -> CameraLockOnConfig.AUTO_RELEASE_AIM_TOLERANCE.set(value), 1); y += ROW_STEP;
        addCycleRow(x, y, CONTENT_WIDTH, "gui.camera_lockon.projectile_settings.bow_aim_reference",
                () -> CameraLockOnConfig.BowAimReference.fromConfig(CameraLockOnConfig.BOW_AIM_REFERENCE.get()).getDisplayName(),
                () -> {
                    CameraLockOnConfig.BowAimReference next = CameraLockOnConfig.BowAimReference
                            .fromConfig(CameraLockOnConfig.BOW_AIM_REFERENCE.get()).next();
                    CameraLockOnConfig.BOW_AIM_REFERENCE.set(next.name());
                }); y += ROW_STEP;

        addActionRow(x, y, CONTENT_WIDTH, Component.literal("Manage Projectile Weapons"),
                "gui.camera_lockon.projectile_settings.projectile_weapons",
                () -> { if (this.minecraft != null) this.minecraft.setScreen(new ProjectileWeaponManagerScreen(this)); }); y += ROW_STEP;

        addHeaderRow(x, y, "gui.camera_lockon.projectile_settings.prediction"); y += 11;
        addSliderRow(x, y, 150, "gui.camera_lockon.camera.projectile_strength", "gui.camera_lockon.unit.percent", 0, 100,
                () -> CameraLockOnConfig.PROJECTILE_PREDICTION_STRENGTH.get() * 100.0D,
                value -> CameraLockOnConfig.PROJECTILE_PREDICTION_STRENGTH.set(value / 100.0D), 0);
        addToggleRow(x + 156, y, "gui.camera_lockon.camera.projectile_drop", CameraLockOnConfig.PROJECTILE_COMPENSATE_DROP); y += ROW_STEP;
        addSliderRow(x, y, CONTENT_WIDTH, "gui.camera_lockon.projectile_settings.early_prediction", "gui.camera_lockon.unit.percent", 0, 100,
                () -> CameraLockOnConfig.PROJECTILE_EARLY_PREDICTION.get() * 100.0D,
                value -> CameraLockOnConfig.PROJECTILE_EARLY_PREDICTION.set(value / 100.0D), 0); y += ROW_STEP;
        addToggleRow(x, y, "gui.camera_lockon.camera.smart_projectile_hitbox", CameraLockOnConfig.SMART_PROJECTILE_HITBOX);
        addToggleRow(x + 156, y, "gui.camera_lockon.projectile_settings.trajectory_preview", CameraLockOnConfig.TRAJECTORY_PREVIEW); y += ROW_STEP;
        addCycleRow(x, y, CONTENT_WIDTH, "gui.camera_lockon.projectile_settings.multipart_aim_mode",
                () -> CameraLockOnConfig.MultipartAimMode.fromConfig(CameraLockOnConfig.MULTIPART_AIM_MODE.get()).getDisplayName(),
                () -> {
                    CameraLockOnConfig.MultipartAimMode next = CameraLockOnConfig.MultipartAimMode
                            .fromConfig(CameraLockOnConfig.MULTIPART_AIM_MODE.get()).next();
                    CameraLockOnConfig.MULTIPART_AIM_MODE.set(next.name());
                }); y += ROW_STEP;

        addActionRow(x, y, CONTENT_WIDTH,
                this.advancedExpanded ? Component.translatable("gui.camera_lockon.projectile_settings.hide_advanced")
                        : Component.translatable("gui.camera_lockon.projectile_settings.advanced"),
                "gui.camera_lockon.projectile_settings.advanced_button",
                () -> {
                    this.advancedExpanded = !this.advancedExpanded;
                    rebuildWidgets();
                });
        y += ROW_STEP;

        if (this.advancedExpanded) {
            addSliderRow(x, y, 150, "gui.camera_lockon.projectile_settings.smoothing", "gui.camera_lockon.unit.percent", 0, 100,
                    () -> CameraLockOnConfig.PROJECTILE_MOTION_SMOOTHING.get() * 100.0D,
                    value -> CameraLockOnConfig.PROJECTILE_MOTION_SMOOTHING.set(value / 100.0D), 0);
            addSliderRow(x + 156, y, 150, "gui.camera_lockon.projectile_settings.max_flight", "gui.camera_lockon.unit.seconds", 0.5, 8.0,
                    CameraLockOnConfig.PROJECTILE_MAX_FLIGHT_TIME::get,
                    value -> CameraLockOnConfig.PROJECTILE_MAX_FLIGHT_TIME.set(value), 1); y += ROW_STEP;

            addToggleRow(x, y, "gui.camera_lockon.projectile_settings.player_movement", CameraLockOnConfig.PROJECTILE_COMPENSATE_PLAYER_MOVEMENT);
            addToggleRow(x + 156, y, "gui.camera_lockon.projectile_settings.full_charge_arc", CameraLockOnConfig.TRAJECTORY_FULL_CHARGE); y += ROW_STEP;

            addToggleRow(x, y, CONTENT_WIDTH, "gui.camera_lockon.projectile_settings.show_without_lock", CameraLockOnConfig.TRAJECTORY_SHOW_WITHOUT_LOCK); y += ROW_STEP;

            addSliderRow(x, y, CONTENT_WIDTH, "gui.camera_lockon.projectile_settings.preview_length", "gui.camera_lockon.unit.blocks", 16, 96,
                    CameraLockOnConfig.TRAJECTORY_PREVIEW_LENGTH::get,
                    value -> CameraLockOnConfig.TRAJECTORY_PREVIEW_LENGTH.set(value), 0); y += ROW_STEP;

            addToggleRow(x, y, "gui.camera_lockon.projectile_settings.adaptive_calibration", CameraLockOnConfig.ADAPTIVE_AIM_CALIBRATION);
            addActionRow(x + 156, y, 150,
                    Component.translatable("gui.camera_lockon.projectile_settings.reset_calibration"),
                    "gui.camera_lockon.projectile_settings.reset_calibration",
                    () -> {
                        AdaptiveAimCalibration.reset(this.minecraft);
                        rebuildWidgets();
                    });
        }
    }

    private int screenY(int contentY) {
        return this.contentTop + contentY - this.scrollOffset;
    }

    private boolean visible(int contentY, int height) {
        int y = screenY(contentY);
        return y >= this.contentTop && y + height <= this.contentTop + this.contentVisibleHeight;
    }

    private void addHeaderRow(int x, int contentY, String key) {
        if (!visible(contentY, 10)) return;
        addHeader(x, screenY(contentY), key);
    }

    private void addToggleRow(int x, int contentY, String key, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        addToggleRow(x, contentY, 150, key, value);
    }

    private void addToggleRow(int x, int contentY, int width, String key, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        if (!visible(contentY, ROW_HEIGHT)) return;
        addToggle(x, screenY(contentY), width, key, value);
    }

    private void addSliderRow(int x, int contentY, int width, String key, String suffixKey, double min, double max,
                              DoubleSupplier getter, DoubleConsumer setter, int decimals) {
        if (!visible(contentY, ROW_HEIGHT)) return;
        addSlider(x, screenY(contentY), width, key, suffixKey, min, max, getter, setter, decimals);
    }

    private void addCycleRow(int x, int contentY, int width, String key,
                             java.util.function.Supplier<Component> valueSupplier, Runnable cycleAction) {
        if (!visible(contentY, ROW_HEIGHT)) return;
        addCycle(x, screenY(contentY), width, key, valueSupplier, cycleAction);
    }

    private void addActionRow(int x, int contentY, int width, Component text, String tooltipKey, Runnable action) {
        if (!visible(contentY, ROW_HEIGHT)) return;
        addAction(x, screenY(contentY), width, text, tooltipKey, action);
    }

    private void addHeader(int x, int y, String key) {
        this.addRenderableWidget(Button.builder(Component.translatable(key), ignored -> {})
                .bounds(x, y, CONTENT_WIDTH, 10).build()).active = false;
    }

    private void addToggle(int x, int y, int width, String key, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        Button button = Button.builder(toggleText(key, value.get()), clicked -> {
            value.set(!value.get());
            clicked.setMessage(toggleText(key, value.get()));
        }).bounds(x, y, width, ROW_HEIGHT).build();
        Tooltip tooltip = tooltipIfPresent(key);
        if (tooltip != null) button.setTooltip(tooltip);
        this.addRenderableWidget(button);
    }

    private Component toggleText(String key, boolean enabled) {
        return Component.translatable("gui.camera_lockon.format.keyValue", Component.translatable(key),
                Component.translatable(enabled ? "gui.camera_lockon.on" : "gui.camera_lockon.off"));
    }

    private void addSlider(int x, int y, int width, String key, String suffixKey, double min, double max,
                           DoubleSupplier getter, DoubleConsumer setter, int decimals) {
        ValueSlider slider = new ValueSlider(x, y, width, key, suffixKey, min, max,
                getter.getAsDouble(), decimals, setter);
        Tooltip tooltip = tooltipIfPresent(key);
        if (tooltip != null) slider.setTooltip(tooltip);
        this.addRenderableWidget(slider);
    }

    private void addCycle(int x, int y, int width, String key,
                          java.util.function.Supplier<Component> valueSupplier, Runnable cycleAction) {
        Button button = Button.builder(cycleText(key, valueSupplier), clicked -> {
            cycleAction.run();
            clicked.setMessage(cycleText(key, valueSupplier));
        }).bounds(x, y, width, ROW_HEIGHT).build();
        Tooltip tooltip = tooltipIfPresent(key);
        if (tooltip != null) button.setTooltip(tooltip);
        this.addRenderableWidget(button);
    }

    private void addAction(int x, int y, int width, Component text, String tooltipKey, Runnable action) {
        Button button = Button.builder(text, clicked -> action.run()).bounds(x, y, width, ROW_HEIGHT).build();
        Tooltip tooltip = tooltipIfPresent(tooltipKey);
        if (tooltip != null) button.setTooltip(tooltip);
        this.addRenderableWidget(button);
    }

    private Component cycleText(String key, java.util.function.Supplier<Component> valueSupplier) {
        return Component.translatable("gui.camera_lockon.format.keyValue",
                Component.translatable(key), valueSupplier.get());
    }

    private Tooltip tooltipIfPresent(String key) {
        String tooltipKey = key.endsWith(".tooltip") ? key : key + ".tooltip";
        return Language.getInstance().has(tooltipKey)
                ? Tooltip.create(Component.translatable(tooltipKey).withStyle(ChatFormatting.WHITE))
                : null;
    }

    private void saveConfig() {
        try {
            CameraLockOnConfig.CLIENT_SPEC.save();
        } catch (RuntimeException exception) {
            System.err.println("[Camera Lock-On] Could not save projectile settings: " + exception.getMessage());
        }
    }

    private void closeAndSave() {
        saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        closeAndSave();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - currentPanelHeight()) / 2;
        AimPointConfigScreen.drawPanel(graphics, left, top, PANEL_WIDTH, currentPanelHeight());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.11, Screen.renderWithTooltipAndSubtitles renders background prior to render()
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = (this.height - currentPanelHeight()) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFFFFF);
    }

    private final class ScrollBar extends AbstractSliderButton {
        ScrollBar(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), maxScroll <= 0 ? 0.0D : (double) scrollOffset / (double) maxScroll);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty());
        }

        @Override
        protected void applyValue() {
            if (maxScroll <= 0) return;
            scrollOffset = Mth.clamp((int) Math.round(this.value * maxScroll), 0, maxScroll);
            rebuildWidgets();
        }
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final String suffixKey;
        private final double min;
        private final double max;
        private final int decimals;
        private final DoubleConsumer consumer;

        ValueSlider(int x, int y, int width, String key, String suffixKey, double min, double max,
                    double initial, int decimals, DoubleConsumer consumer) {
            super(x, y, width, ROW_HEIGHT, Component.empty(), normalize(initial, min, max));
            this.key = key;
            this.suffixKey = suffixKey;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.consumer = consumer;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double actual = this.min + (this.max - this.min) * this.value;
            String number = this.decimals == 0 ? Integer.toString((int) Math.round(actual))
                    : String.format(Locale.ROOT, "%." + this.decimals + "f", actual);
            Component suffix = this.suffixKey.isEmpty() ? Component.empty() : Component.translatable(this.suffixKey);
            setMessage(Component.translatable("gui.camera_lockon.format.sliderValue", Component.translatable(this.key), number, suffix));
        }

        @Override
        protected void applyValue() {
            double actual = this.min + (this.max - this.min) * this.value;
            double factor = Math.pow(10, this.decimals);
            this.consumer.accept(Math.round(actual * factor) / factor);
        }

        private static double normalize(double value, double min, double max) {
            return max <= min ? 0 : Mth.clamp((value - min) / (max - min), 0, 1);
        }
    }
}
