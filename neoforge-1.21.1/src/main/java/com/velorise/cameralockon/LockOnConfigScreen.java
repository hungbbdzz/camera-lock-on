package com.velorise.cameralockon;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Compact tabbed configuration screen for Camera Lock-On 2.0. */
public final class LockOnConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 260;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_STEP = 20;

    private final Screen parent;
    private Page page = Page.MAIN;
    private final List<String> entityHistory = new ArrayList<>();
    private final List<String> entityBlacklist = new ArrayList<>();

    public LockOnConfigScreen(Screen parent) {
        super(Component.literal("Camera Lock-On"));
        this.parent = parent;
        this.entityHistory.addAll(CameraLockOnConfig.ENTITY_HISTORY.get());
        this.entityBlacklist.addAll(CameraLockOnConfig.ENTITY_BLACKLIST.get());
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        buildTabs(left, top + 25);

        int x = left + 12;
        int y = top + 53;
        switch (this.page) {
            case MAIN -> buildMain(x, y);
            case CAMERA -> buildCamera(x, y);
            case AUTO -> buildAuto(x, y);
            case FILTER -> buildFilter(x, y);
            case HUD -> buildHud(x, y);
            case THREAT -> buildThreat(x, y);
            case GROUP -> buildGroup(x, y);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.camera_lockon.button.reset_defaults"), button -> confirmReset())
                .bounds(left + 119, top + PANEL_HEIGHT - 25, 102, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.camera_lockon.button.done"), button -> closeAndSave())
                .bounds(left + 226, top + PANEL_HEIGHT - 25, 102, 18).build());
    }

    private void buildTabs(int left, int y) {
        Page[] pages = Page.values();
        int totalWidth = PANEL_WIDTH - 24;
        for (int i = 0; i < pages.length; i++) {
            Page candidate = pages[i];
            int xStart = left + 12 + (i * totalWidth) / pages.length;
            int xEnd = left + 12 + ((i + 1) * totalWidth) / pages.length;
            int width = (i == pages.length - 1) ? (xEnd - xStart) : (xEnd - xStart - 2);
            Button button = Button.builder(candidate.getLabel(), ignored -> {
                        this.page = candidate;
                        rebuildWidgets();
                    })
                    .bounds(xStart, y, width, 18)
                    .build();
            button.active = candidate != this.page;
            button.setTooltip(Tooltip.create(candidate.getDescription().copy().withStyle(ChatFormatting.WHITE)));
            this.addRenderableWidget(button);
        }
    }

    private void buildMain(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.core_targeting"); y += 14;
        addToggle(x, y, "gui.camera_lockon.main.smart_lock", CameraLockOnConfig.SMART_LOCK);
        addToggle(x + 161, y, "gui.camera_lockon.main.hostile_only", CameraLockOnConfig.HOSTILE_ONLY); y += ROW_STEP;
        addSlider(x, y, 316, "gui.camera_lockon.main.range", "gui.camera_lockon.unit.blocks", 5, 128,
                CameraLockOnConfig.LOCK_ON_RANGE::get, value -> CameraLockOnConfig.LOCK_ON_RANGE.set(value), 0);
        y += ROW_STEP;
        addCycle(x, y, 155, "gui.camera_lockon.main.aim", () -> CameraLockOnConfig.AimPreset.fromConfig(CameraLockOnConfig.AIM_PRESET.get()).getDisplayName(), () -> {
            CameraLockOnConfig.AimPreset next = CameraLockOnConfig.AimPreset.fromConfig(CameraLockOnConfig.AIM_PRESET.get()).next();
            CameraLockOnConfig.AIM_PRESET.set(next.name());
        });
        addAction(x + 161, y, 155, "gui.camera_lockon.main.edit_global_aim", () -> setScreen(new AimPointConfigScreen(this,
                CameraLockOnConfig.AIM_POINT_X.get(), CameraLockOnConfig.AIM_POINT_Y.get(), (px, py) -> {
                    CameraLockOnConfig.AIM_POINT_X.set(px);
                    CameraLockOnConfig.AIM_POINT_Y.set(py);
                    CameraLockOnConfig.AIM_PRESET.set(CameraLockOnConfig.AimPreset.CUSTOM.name());
                    saveConfig();
                })));
        y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.main.reticle", CameraLockOnConfig.SHOW_RETICLE);
        addCycle(x + 161, y, 155, "gui.camera_lockon.main.color", () -> Component.translatable("gui.camera_lockon.color." + CameraLockOnConfig.RETICLE_COLOR.get().toLowerCase(Locale.ROOT)), () -> {
            List<String> values = List.of("Cyan", "Red", "Green", "Yellow", "Purple");
            int index = values.indexOf(CameraLockOnConfig.RETICLE_COLOR.get());
            CameraLockOnConfig.RETICLE_COLOR.set(values.get((index + 1) % values.size()));
        });
        y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.main.lock_sounds", CameraLockOnConfig.LOCK_SOUNDS);
        addSlider(x + 161, y, 155, "gui.camera_lockon.main.volume", "gui.camera_lockon.unit.percent", 0, 100,
                () -> CameraLockOnConfig.SOUND_VOLUME.get() * 100.0D,
                value -> CameraLockOnConfig.SOUND_VOLUME.set(value / 100.0D), 0);
    }

    private void buildCamera(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.camera_steering"); y += 14;
        addToggle(x, y, 316, "gui.camera_lockon.camera.dead_zone", CameraLockOnConfig.DEAD_ZONE); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.camera.horizontal", "gui.camera_lockon.unit.degrees", 0, 30,
                CameraLockOnConfig.DEAD_ZONE_HORIZONTAL::get, value -> CameraLockOnConfig.DEAD_ZONE_HORIZONTAL.set(value), 1);
        addSlider(x + 161, y, 155, "gui.camera_lockon.camera.vertical", "gui.camera_lockon.unit.degrees", 0, 20,
                CameraLockOnConfig.DEAD_ZONE_VERTICAL::get, value -> CameraLockOnConfig.DEAD_ZONE_VERTICAL.set(value), 1); y += ROW_STEP;
        addHeader(x, y, "gui.camera_lockon.header.pause_steering"); y += 14;
        addToggle(x, y, "gui.camera_lockon.camera.mining", CameraLockOnConfig.SUSPEND_MINING);
        addToggle(x + 161, y, "gui.camera_lockon.camera.using_item", CameraLockOnConfig.SUSPEND_USING_ITEM); y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.camera.riding", CameraLockOnConfig.SUSPEND_RIDING);
        addToggle(x + 161, y, "gui.camera_lockon.camera.elytra", CameraLockOnConfig.SUSPEND_ELYTRA); y += ROW_STEP;
        addSlider(x, y, 316, "gui.camera_lockon.camera.grace", "gui.camera_lockon.unit.seconds", 0, 10,
                CameraLockOnConfig.LOST_TARGET_GRACE::get, value -> CameraLockOnConfig.LOST_TARGET_GRACE.set(value), 2);
    }

    private void buildAuto(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.auto_acquisition"); y += 14;
        addToggle(x, y, "gui.camera_lockon.auto.auto_lock", CameraLockOnConfig.AUTO_LOCK);
        addToggle(x + 161, y, "gui.camera_lockon.auto.auto_retarget", CameraLockOnConfig.AUTO_RETARGET); y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.auto.pixel_indicator", CameraLockOnConfig.AUTO_LOCK_INDICATOR);
        addCycle(x + 161, y, 155, "gui.camera_lockon.auto.switch", () -> ClientFeatureStore.getSwitchTargetMode().getDisplayName(), () -> {
            ClientFeatureStore.SwitchTargetMode next = ClientFeatureStore.getSwitchTargetMode().next();
            ClientFeatureStore.setSwitchTargetMode(next);
        }); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.auto.aim_delay", "gui.camera_lockon.unit.seconds", 0.05, 3,
                CameraLockOnConfig.AUTO_LOCK_DELAY::get, value -> CameraLockOnConfig.AUTO_LOCK_DELAY.set(value), 2);
        addSlider(x + 161, y, 155, "gui.camera_lockon.auto.unlock_cooldown", "gui.camera_lockon.unit.seconds", 0, 5,
                CameraLockOnConfig.AUTO_LOCK_COOLDOWN::get, value -> CameraLockOnConfig.AUTO_LOCK_COOLDOWN.set(value), 2); y += ROW_STEP;
        addCycle(x, y, 316, "gui.camera_lockon.auto.lock_on_hit", () -> CameraLockOnConfig.LockOnHitMode.fromConfig(CameraLockOnConfig.LOCK_ON_HIT_MODE.get()).getDisplayName(), () -> {
            CameraLockOnConfig.LockOnHitMode next = CameraLockOnConfig.LockOnHitMode.fromConfig(CameraLockOnConfig.LOCK_ON_HIT_MODE.get()).next();
            CameraLockOnConfig.LOCK_ON_HIT_MODE.set(next.name());
        }); y += ROW_STEP;
        addCycle(x, y, 316, "gui.camera_lockon.auto.temporary_pin", () -> CameraLockOnConfig.TemporaryPinMode.fromConfig(CameraLockOnConfig.TEMPORARY_PIN_MODE.get()).getDisplayName(), () -> {
            CameraLockOnConfig.TemporaryPinMode next = CameraLockOnConfig.TemporaryPinMode.fromConfig(CameraLockOnConfig.TEMPORARY_PIN_MODE.get()).next();
            CameraLockOnConfig.TEMPORARY_PIN_MODE.set(next.name());
        });
    }

    private void buildFilter(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.entity_rules"); y += 14;
        addCycle(x, y, 316, "gui.camera_lockon.filter.type_filter", () -> CameraLockOnConfig.TargetTypeMode.fromConfig(CameraLockOnConfig.TARGET_TYPE_MODE.get()).getDisplayName(), () -> {
            CameraLockOnConfig.TargetTypeMode next = CameraLockOnConfig.TargetTypeMode.fromConfig(CameraLockOnConfig.TARGET_TYPE_MODE.get()).next();
            CameraLockOnConfig.TARGET_TYPE_MODE.set(next.name());
        }); y += ROW_STEP;
        String selected = CameraLockOnConfig.SELECTED_ENTITY_TYPE.get();
        Component selectedEntityVal = selected.isBlank() ? Component.translatable("gui.camera_lockon.none") : Component.literal(TargetingRules.getEntityDisplayName(selected));
        Component selectedEntityText = Component.translatable("gui.camera_lockon.format.keyValue", Component.translatable("gui.camera_lockon.filter.selected_entity"), selectedEntityVal);
        addAction(x, y, 316, selectedEntityText, () -> setScreen(new EntitySelectorScreen(this,
                selected, this.entityHistory, id -> {
                    CameraLockOnConfig.SELECTED_ENTITY_TYPE.set(id);
                    saveLists();
                    rebuildWidgets();
                })), Component.translatable("gui.camera_lockon.filter.selected_entity.tooltip")); y += ROW_STEP;
        addCycle(x, y, 316, "gui.camera_lockon.filter.target_priority", () -> ClientFeatureStore.getTargetPriority().getDisplayName(), () -> {
            ClientFeatureStore.setTargetPriority(ClientFeatureStore.getTargetPriority().next());
        }); y += ROW_STEP;
        addCycle(x, y, 316, "gui.camera_lockon.filter.retarget_rule", () -> CameraLockOnConfig.RetargetMode.fromConfig(CameraLockOnConfig.RETARGET_MODE.get()).getDisplayName(), () -> {
            CameraLockOnConfig.RetargetMode next = CameraLockOnConfig.RetargetMode.fromConfig(CameraLockOnConfig.RETARGET_MODE.get()).next();
            CameraLockOnConfig.RETARGET_MODE.set(next.name());
        }); y += ROW_STEP;
        Component blacklistText = Component.translatable("gui.camera_lockon.filter.blacklist_count", this.entityBlacklist.size());
        addAction(x, y, 155, blacklistText, () -> setScreen(new BlacklistScreen(this, this.entityBlacklist, this.entityHistory)), Component.translatable("gui.camera_lockon.filter.blacklist.tooltip"));
        Component aimOverridesText = Component.translatable("gui.camera_lockon.filter.aim_overrides_count", EntityAimPointStore.snapshot().size());
        addAction(x + 161, y, 155, aimOverridesText, () -> setScreen(new EntityAimPointManagerScreen(this, this.entityHistory)), Component.translatable("gui.camera_lockon.filter.aim_overrides.tooltip")); y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.filter.prefer_bosses", CameraLockOnConfig.PREFER_BOSSES);
        addToggle(x + 161, y, "gui.camera_lockon.filter.show_registry_ids", CameraLockOnConfig.SHOW_REGISTRY_IDS);
    }

    private void buildHud(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.target_mini_hud"); y += 14;
        addToggle(x, y, "gui.camera_lockon.hud.hud_enabled", CameraLockOnConfig.TARGET_HUD);
        addToggle(x + 161, y, "gui.camera_lockon.hud.damage_flash", CameraLockOnConfig.HUD_ANIMATE_DAMAGE); y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.hud.name", CameraLockOnConfig.HUD_SHOW_NAME);
        addToggle(x + 161, y, "gui.camera_lockon.hud.health", CameraLockOnConfig.HUD_SHOW_HEALTH); y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.hud.distance", CameraLockOnConfig.HUD_SHOW_DISTANCE);
        addToggle(x + 161, y, "gui.camera_lockon.hud.armor_points", CameraLockOnConfig.HUD_SHOW_ARMOR); y += ROW_STEP;
        addToggle(x, y, "gui.camera_lockon.hud.registry_id", CameraLockOnConfig.SHOW_REGISTRY_IDS);
        addToggle(x + 161, y, "gui.camera_lockon.hud.source_mod", CameraLockOnConfig.HUD_SHOW_MOD_NAME); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.hud.scale", "gui.camera_lockon.unit.times", 0.6, 1.6,
                CameraLockOnConfig.HUD_SCALE::get, value -> CameraLockOnConfig.HUD_SCALE.set(value), 2);
        addSlider(x + 161, y, 155, "gui.camera_lockon.hud.background", "", 0.05, 1,
                CameraLockOnConfig.HUD_OPACITY::get, value -> CameraLockOnConfig.HUD_OPACITY.set(value), 2); y += ROW_STEP;
        addAction(x, y, 316, "gui.camera_lockon.hud.adjust_position", () -> setScreen(new HudPositionEditorScreen(this,
                CameraLockOnConfig.HUD_ANCHOR_X.get(), CameraLockOnConfig.HUD_ANCHOR_Y.get(), LockOnHudRenderer.optionsFromConfig(), (px, py) -> {
                    CameraLockOnConfig.HUD_ANCHOR_X.set(px);
                    CameraLockOnConfig.HUD_ANCHOR_Y.set(py);
                    saveConfig();
                })));
    }

    private void buildThreat(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.attacker_awareness"); y += 14;
        addToggle(x, y, "gui.camera_lockon.threat.indicators", CameraLockOnConfig.ATTACKER_INDICATOR);
        addToggle(x + 161, y, "gui.camera_lockon.threat.group_side", CameraLockOnConfig.GROUP_ATTACKER_DIRECTIONS); y += ROW_STEP;
        addCycle(x, y, 316, "gui.camera_lockon.threat.response", () -> CameraLockOnConfig.AttackerResponse.fromConfig(CameraLockOnConfig.ATTACKER_RESPONSE.get()).getDisplayName(), () -> {
            CameraLockOnConfig.AttackerResponse next = CameraLockOnConfig.AttackerResponse.fromConfig(CameraLockOnConfig.ATTACKER_RESPONSE.get()).next();
            CameraLockOnConfig.ATTACKER_RESPONSE.set(next.name());
        }); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.threat.required_hits", "", 2, 5,
                () -> CameraLockOnConfig.ATTACKER_REQUIRED_HITS.get().doubleValue(), value -> CameraLockOnConfig.ATTACKER_REQUIRED_HITS.set((int) Math.round(value)), 0);
        addSlider(x + 161, y, 155, "gui.camera_lockon.threat.hit_window", "gui.camera_lockon.unit.seconds", 1, 15,
                CameraLockOnConfig.ATTACKER_HIT_WINDOW::get, value -> CameraLockOnConfig.ATTACKER_HIT_WINDOW.set(value), 1); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.threat.lock_range", "gui.camera_lockon.unit.blocks", 4, 64,
                CameraLockOnConfig.ATTACKER_LOCK_RANGE::get, value -> CameraLockOnConfig.ATTACKER_LOCK_RANGE.set(value), 0);
        addSlider(x + 161, y, 155, "gui.camera_lockon.threat.indicators_count", "", 1, 6,
                () -> CameraLockOnConfig.MAX_ATTACKER_INDICATORS.get().doubleValue(), value -> CameraLockOnConfig.MAX_ATTACKER_INDICATORS.set((int) Math.round(value)), 0); y += ROW_STEP;
        addToggle(x, y, 316, "gui.camera_lockon.threat.replace_target", CameraLockOnConfig.ATTACKER_REPLACE_TARGET);
    }

    private void buildGroup(int x, int y) {
        addHeader(x, y, "gui.camera_lockon.header.experimental_sweep"); y += 14;
        addToggle(x, y, "gui.camera_lockon.group.group_aim", CameraLockOnConfig.GROUP_AIM);
        addToggle(x + 161, y, "gui.camera_lockon.group.same_type_only", CameraLockOnConfig.GROUP_AIM_SAME_TYPE_ONLY); y += ROW_STEP;
        addCycle(x, y, 316, "gui.camera_lockon.group.activation", () -> CameraLockOnConfig.GroupAimActivation.fromConfig(CameraLockOnConfig.GROUP_AIM_ACTIVATION.get()).getDisplayName(), () -> {
            CameraLockOnConfig.GroupAimActivation next = CameraLockOnConfig.GroupAimActivation.fromConfig(CameraLockOnConfig.GROUP_AIM_ACTIVATION.get()).next();
            CameraLockOnConfig.GROUP_AIM_ACTIVATION.set(next.name());
        }); y += ROW_STEP;
        Component aoeText = Component.translatable("gui.camera_lockon.group.manual_aoe_weapons_count", AoeWeaponStore.snapshot().size());
        addAction(x, y, 316, aoeText, () -> setScreen(new AoeWeaponManagerScreen(this)), Component.translatable("gui.camera_lockon.group.manual_aoe_weapons.tooltip")); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.group.radius", "gui.camera_lockon.unit.blocks", 0.5, 6,
                CameraLockOnConfig.GROUP_AIM_RADIUS::get, value -> CameraLockOnConfig.GROUP_AIM_RADIUS.set(value), 1);
        addSlider(x + 161, y, 155, "gui.camera_lockon.group.strength", "", 0, 1,
                CameraLockOnConfig.GROUP_AIM_STRENGTH::get, value -> CameraLockOnConfig.GROUP_AIM_STRENGTH.set(value), 2); y += ROW_STEP;
        addSlider(x, y, 155, "gui.camera_lockon.group.max_targets", "", 2, 8,
                () -> CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.get().doubleValue(), value -> CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.set((int) Math.round(value)), 0);
        addSlider(x + 161, y, 155, "gui.camera_lockon.group.max_offset", "gui.camera_lockon.unit.blocks", 0.1, 3,
                CameraLockOnConfig.GROUP_AIM_MAX_OFFSET::get, value -> CameraLockOnConfig.GROUP_AIM_MAX_OFFSET.set(value), 2);
    }

    private void addHeader(int x, int y, String key) {
        Button label = Button.builder(Component.translatable(key), ignored -> { }).bounds(x, y, 316, 12).build();
        label.active = false;
        this.addRenderableWidget(label);
    }

    private void addToggle(int x, int y, String key, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        addToggle(x, y, 155, key, value);
    }

    private void addToggle(int x, int y, int width, String key, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        Component label = Component.translatable(key);
        Component state = Component.translatable(value.get() ? "gui.camera_lockon.on" : "gui.camera_lockon.off");
        Component buttonText = Component.translatable("gui.camera_lockon.format.keyValue", label, state);
        Button button = Button.builder(buttonText, ignored -> {
                    value.set(!value.get());
                    saveConfig();
                    rebuildWidgets();
                }).bounds(x, y, width, ROW_HEIGHT).build();
        button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip").withStyle(ChatFormatting.WHITE)));
        this.addRenderableWidget(button);
    }

    private void addCycle(int x, int y, int width, String key, java.util.function.Supplier<Component> valueSupplier, Runnable action) {
        Component label = Component.translatable(key);
        Component buttonText = Component.translatable("gui.camera_lockon.format.keyValue", label, valueSupplier.get());
        Button button = Button.builder(buttonText, ignored -> {
                    action.run();
                    saveConfig();
                    rebuildWidgets();
                }).bounds(x, y, width, ROW_HEIGHT).build();
        button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip").withStyle(ChatFormatting.WHITE)));
        this.addRenderableWidget(button);
    }

    private void addAction(int x, int y, int width, String key, Runnable action) {
        Button button = Button.builder(Component.translatable(key), ignored -> action.run()).bounds(x, y, width, ROW_HEIGHT).build();
        button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip").withStyle(ChatFormatting.WHITE)));
        this.addRenderableWidget(button);
    }

    private void addAction(int x, int y, int width, Component text, Runnable action, Component tooltip) {
        Button button = Button.builder(text, ignored -> action.run()).bounds(x, y, width, ROW_HEIGHT).build();
        button.setTooltip(Tooltip.create(tooltip.copy().withStyle(ChatFormatting.WHITE)));
        this.addRenderableWidget(button);
    }

    private void addSlider(int x, int y, int width, String key, String suffixKey, double min, double max,
                           DoubleSupplier getter, DoubleConsumer setter, int decimals) {
        ValueSlider slider = new ValueSlider(x, y, width, key, suffixKey, min, max, getter.getAsDouble(), decimals, value -> {
            setter.accept(value);
            saveConfig();
        });
        this.addRenderableWidget(slider);
    }

    private void confirmReset() {
        if (this.minecraft == null) return;
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) resetDefaults();
            if (this.minecraft != null) this.minecraft.setScreen(this);
        }, Component.translatable("gui.camera_lockon.reset.title"),
                Component.translatable("gui.camera_lockon.reset.description"),
                Component.translatable("gui.camera_lockon.reset.confirm"),
                Component.translatable("gui.camera_lockon.reset.cancel")));
    }

    private void resetDefaults() {
        CameraLockOnConfig.LOCK_ON_RANGE.set(CameraLockOnConfig.DEFAULT_LOCK_ON_RANGE);
        CameraLockOnConfig.SMART_LOCK.set(CameraLockOnConfig.DEFAULT_SMART_LOCK);
        CameraLockOnConfig.SHOW_RETICLE.set(CameraLockOnConfig.DEFAULT_SHOW_RETICLE);
        CameraLockOnConfig.RETICLE_COLOR.set(CameraLockOnConfig.DEFAULT_RETICLE_COLOR);
        CameraLockOnConfig.RETICLE_OPACITY.set(CameraLockOnConfig.DEFAULT_RETICLE_OPACITY);
        CameraLockOnConfig.AUTO_RETARGET.set(CameraLockOnConfig.DEFAULT_AUTO_RETARGET);
        CameraLockOnConfig.HOSTILE_ONLY.set(CameraLockOnConfig.DEFAULT_HOSTILE_ONLY);
        CameraLockOnConfig.AIM_PRESET.set(CameraLockOnConfig.DEFAULT_AIM_PRESET.name());
        CameraLockOnConfig.AIM_POINT_X.set(CameraLockOnConfig.DEFAULT_AIM_POINT_X);
        CameraLockOnConfig.AIM_POINT_Y.set(CameraLockOnConfig.DEFAULT_AIM_POINT_Y);
        CameraLockOnConfig.LOST_TARGET_GRACE.set(CameraLockOnConfig.DEFAULT_LOST_TARGET_GRACE);
        CameraLockOnConfig.LOCK_SOUNDS.set(CameraLockOnConfig.DEFAULT_LOCK_SOUNDS);
        CameraLockOnConfig.SOUND_VOLUME.set(CameraLockOnConfig.DEFAULT_SOUND_VOLUME);

        CameraLockOnConfig.DEAD_ZONE.set(CameraLockOnConfig.DEFAULT_DEAD_ZONE);
        CameraLockOnConfig.DEAD_ZONE_HORIZONTAL.set(CameraLockOnConfig.DEFAULT_DEAD_ZONE_HORIZONTAL);
        CameraLockOnConfig.DEAD_ZONE_VERTICAL.set(CameraLockOnConfig.DEFAULT_DEAD_ZONE_VERTICAL);
        CameraLockOnConfig.SUSPEND_USING_ITEM.set(CameraLockOnConfig.DEFAULT_SUSPEND_USING_ITEM);
        CameraLockOnConfig.SUSPEND_MINING.set(CameraLockOnConfig.DEFAULT_SUSPEND_MINING);
        CameraLockOnConfig.SUSPEND_RIDING.set(CameraLockOnConfig.DEFAULT_SUSPEND_RIDING);
        CameraLockOnConfig.SUSPEND_ELYTRA.set(CameraLockOnConfig.DEFAULT_SUSPEND_ELYTRA);

        CameraLockOnConfig.AUTO_LOCK.set(CameraLockOnConfig.DEFAULT_AUTO_LOCK);
        CameraLockOnConfig.AUTO_LOCK_DELAY.set(CameraLockOnConfig.DEFAULT_AUTO_LOCK_DELAY);
        CameraLockOnConfig.AUTO_LOCK_COOLDOWN.set(CameraLockOnConfig.DEFAULT_AUTO_LOCK_COOLDOWN);
        CameraLockOnConfig.AUTO_LOCK_INDICATOR.set(CameraLockOnConfig.DEFAULT_AUTO_LOCK_INDICATOR);
        CameraLockOnConfig.LOCK_ON_HIT_MODE.set(CameraLockOnConfig.DEFAULT_LOCK_ON_HIT_MODE.name());
        CameraLockOnConfig.TEMPORARY_PIN_MODE.set(CameraLockOnConfig.DEFAULT_TEMPORARY_PIN_MODE.name());
        ClientFeatureStore.setSwitchTargetMode(ClientFeatureStore.SwitchTargetMode.SMART);

        CameraLockOnConfig.TARGET_TYPE_MODE.set(CameraLockOnConfig.DEFAULT_TARGET_TYPE_MODE.name());
        CameraLockOnConfig.SELECTED_ENTITY_TYPE.set(CameraLockOnConfig.DEFAULT_SELECTED_ENTITY_TYPE);
        CameraLockOnConfig.RETARGET_MODE.set(CameraLockOnConfig.DEFAULT_RETARGET_MODE.name());
        CameraLockOnConfig.PREFER_BOSSES.set(CameraLockOnConfig.DEFAULT_PREFER_BOSSES);
        ClientFeatureStore.setTargetPriority(ClientFeatureStore.TargetPriority.BALANCED);

        CameraLockOnConfig.TARGET_HUD.set(CameraLockOnConfig.DEFAULT_TARGET_HUD);
        CameraLockOnConfig.HUD_SHOW_NAME.set(CameraLockOnConfig.DEFAULT_HUD_SHOW_NAME);
        CameraLockOnConfig.HUD_SHOW_HEALTH.set(CameraLockOnConfig.DEFAULT_HUD_SHOW_HEALTH);
        CameraLockOnConfig.HUD_SHOW_DISTANCE.set(CameraLockOnConfig.DEFAULT_HUD_SHOW_DISTANCE);
        CameraLockOnConfig.HUD_SHOW_ARMOR.set(CameraLockOnConfig.DEFAULT_HUD_SHOW_ARMOR);
        CameraLockOnConfig.HUD_SHOW_MOD_NAME.set(CameraLockOnConfig.DEFAULT_HUD_SHOW_MOD_NAME);
        CameraLockOnConfig.HUD_ANIMATE_DAMAGE.set(CameraLockOnConfig.DEFAULT_HUD_ANIMATE_DAMAGE);
        CameraLockOnConfig.HUD_STYLE.set(CameraLockOnConfig.DEFAULT_HUD_STYLE.name());
        CameraLockOnConfig.HUD_POSITION.set(CameraLockOnConfig.DEFAULT_HUD_POSITION.name());
        CameraLockOnConfig.HUD_SCALE.set(CameraLockOnConfig.DEFAULT_HUD_SCALE);
        CameraLockOnConfig.HUD_OPACITY.set(CameraLockOnConfig.DEFAULT_HUD_OPACITY);
        CameraLockOnConfig.HUD_ANCHOR_X.set(CameraLockOnConfig.DEFAULT_HUD_ANCHOR_X);
        CameraLockOnConfig.HUD_ANCHOR_Y.set(CameraLockOnConfig.DEFAULT_HUD_ANCHOR_Y);
        CameraLockOnConfig.HUD_OFFSET_X.set(CameraLockOnConfig.DEFAULT_HUD_OFFSET_X);
        CameraLockOnConfig.HUD_OFFSET_Y.set(CameraLockOnConfig.DEFAULT_HUD_OFFSET_Y);
        CameraLockOnConfig.SHOW_REGISTRY_IDS.set(CameraLockOnConfig.DEFAULT_SHOW_REGISTRY_IDS);

        CameraLockOnConfig.ATTACKER_INDICATOR.set(CameraLockOnConfig.DEFAULT_ATTACKER_INDICATOR);
        CameraLockOnConfig.ATTACKER_RESPONSE.set(CameraLockOnConfig.DEFAULT_ATTACKER_RESPONSE.name());
        CameraLockOnConfig.ATTACKER_INDICATOR_LIFETIME.set(CameraLockOnConfig.DEFAULT_ATTACKER_INDICATOR_LIFETIME);
        CameraLockOnConfig.MAX_ATTACKER_INDICATORS.set(CameraLockOnConfig.DEFAULT_MAX_ATTACKER_INDICATORS);
        CameraLockOnConfig.GROUP_ATTACKER_DIRECTIONS.set(CameraLockOnConfig.DEFAULT_GROUP_ATTACKER_DIRECTIONS);
        CameraLockOnConfig.ATTACKER_REQUIRED_HITS.set(CameraLockOnConfig.DEFAULT_ATTACKER_REQUIRED_HITS);
        CameraLockOnConfig.ATTACKER_HIT_WINDOW.set(CameraLockOnConfig.DEFAULT_ATTACKER_HIT_WINDOW);
        CameraLockOnConfig.ATTACKER_REPLACE_TARGET.set(CameraLockOnConfig.DEFAULT_ATTACKER_REPLACE_TARGET);
        CameraLockOnConfig.ATTACKER_LOCK_RANGE.set(CameraLockOnConfig.DEFAULT_ATTACKER_LOCK_RANGE);
        CameraLockOnConfig.ATTACKER_LOCK_PROTECTION.set(CameraLockOnConfig.DEFAULT_ATTACKER_LOCK_PROTECTION);

        CameraLockOnConfig.GROUP_AIM.set(CameraLockOnConfig.DEFAULT_GROUP_AIM);
        CameraLockOnConfig.GROUP_AIM_ACTIVATION.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_ACTIVATION.name());
        CameraLockOnConfig.GROUP_AIM_RADIUS.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_RADIUS);
        CameraLockOnConfig.GROUP_AIM_MAX_DISTANCE.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_MAX_DISTANCE);
        CameraLockOnConfig.GROUP_AIM_MAX_TARGETS.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_MAX_TARGETS);
        CameraLockOnConfig.GROUP_AIM_STRENGTH.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_STRENGTH);
        CameraLockOnConfig.GROUP_AIM_MAX_OFFSET.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_MAX_OFFSET);
        CameraLockOnConfig.GROUP_AIM_SAME_TYPE_ONLY.set(CameraLockOnConfig.DEFAULT_GROUP_AIM_SAME_TYPE_ONLY);

        saveConfig();
        rebuildWidgets();
    }

    private void saveLists() {
        CameraLockOnConfig.ENTITY_HISTORY.set(List.copyOf(this.entityHistory));
        CameraLockOnConfig.ENTITY_BLACKLIST.set(List.copyOf(this.entityBlacklist));
        saveConfig();
    }

    private void saveConfig() {
        CameraLockOnConfig.CLIENT_SPEC.save();
    }

    private void closeAndSave() {
        saveLists();
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    private void setScreen(Screen screen) {
        saveLists();
        if (this.minecraft != null) this.minecraft.setScreen(screen);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        AimPointConfigScreen.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, "Camera Lock-On 2.0", this.width / 2, top + 9, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        closeAndSave();
    }

    private enum Page {
        MAIN("main"),
        CAMERA("camera"),
        AUTO("auto"),
        FILTER("filter"),
        HUD("hud"),
        THREAT("threat"),
        GROUP("group");

        private final String key;
        Page(String key) { this.key = key; }
        public Component getLabel() { return Component.translatable("gui.camera_lockon.tab." + this.key); }
        public Component getDescription() { return Component.translatable("gui.camera_lockon.tab." + this.key + ".tooltip"); }
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
            String number = this.decimals == 0
                    ? Integer.toString((int) Math.round(actual))
                    : String.format(Locale.ROOT, "%." + this.decimals + "f", actual);
            Component labelComponent = Component.translatable(this.key);
            Component suffixComponent = this.suffixKey.isEmpty() ? Component.empty() : Component.translatable(this.suffixKey);
            setMessage(Component.translatable("gui.camera_lockon.format.sliderValue", labelComponent, number, suffixComponent));
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
