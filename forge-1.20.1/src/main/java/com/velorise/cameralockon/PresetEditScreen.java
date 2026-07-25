package com.velorise.cameralockon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;

/** Selects which current setting groups are copied into a new or existing preset. */
public final class PresetEditScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int MAX_PANEL_HEIGHT = 286;

    private final Screen parent;
    private final String existingFileName;
    private final EnumSet<PresetStore.Category> categories;
    private boolean includeBlacklist;
    private boolean includeAimOverrides;
    private boolean includeAoeWeapons;
    private EditBox nameBox;
    private Button saveButton;

    public PresetEditScreen(Screen parent, PresetStore.PresetSummary summary) {
        super(Component.literal(summary == null ? "Save Current Preset" : "Update Preset"));
        this.parent = parent;
        this.existingFileName = summary == null ? null : summary.fileName();
        this.categories = summary == null || summary.categories().isEmpty()
                ? EnumSet.allOf(PresetStore.Category.class)
                : EnumSet.copyOf(summary.categories());
        if (summary != null) {
            PresetStore.Preset preset = PresetStore.load(summary.fileName());
            if (preset != null) {
                this.includeBlacklist = preset.includesBlacklist();
                this.includeAimOverrides = preset.includesAimOverrides();
                this.includeAoeWeapons = preset.includesAoeWeapons();
            }
        }
    }

    private int panelWidth() { return Math.max(280, Math.min(MAX_PANEL_WIDTH, this.width - 12)); }
    private int panelHeight() { return Math.max(224, Math.min(MAX_PANEL_HEIGHT, this.height - 12)); }
    private int panelLeft() { return (this.width - panelWidth()) / 2; }
    private int panelTop() { return Math.max(6, (this.height - panelHeight()) / 2); }

    @Override
    protected void init() {
        int panelW = panelWidth();
        int panelH = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        int margin = 16;
        int gap = 8;
        int columnWidth = (panelW - margin * 2 - gap) / 2;
        int x1 = left + margin;
        int x2 = x1 + columnWidth + gap;
        int rowStep = panelH < 260 ? 20 : 22;

        this.nameBox = new EditBox(this.font, x1, top + 39, panelW - margin * 2, 18, Component.literal("Preset Name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setHint(Component.literal("Preset name"));
        if (this.existingFileName != null) {
            PresetStore.Preset preset = PresetStore.load(this.existingFileName);
            if (preset != null) this.nameBox.setValue(preset.name());
        }
        this.nameBox.setResponder(value -> updateSaveButton());
        this.addRenderableWidget(this.nameBox);

        int categoryY = top + 72;
        PresetStore.Category[] values = PresetStore.Category.values();
        for (int i = 0; i < values.length; i++) {
            PresetStore.Category category = values[i];
            int x = i % 2 == 0 ? x1 : x2;
            int rowY = categoryY + (i / 2) * rowStep;
            this.addRenderableWidget(categoryButton(category, x, rowY, columnWidth));
        }

        int advancedY = categoryY + 4 * rowStep + 8;
        this.addRenderableWidget(toggleButton("Blacklist", x1, advancedY, columnWidth, () -> this.includeBlacklist, value -> this.includeBlacklist = value,
                "Include the target blacklist in this preset."));
        this.addRenderableWidget(toggleButton("Aim Overrides", x2, advancedY, columnWidth, () -> this.includeAimOverrides, value -> this.includeAimOverrides = value,
                "Include all per-entity aim points."));
        this.addRenderableWidget(toggleButton("AOE Weapons", x1, advancedY + rowStep, columnWidth, () -> this.includeAoeWeapons, value -> this.includeAoeWeapons = value,
                "Include the manual AOE weapon list."));

        int footerY = top + panelH - 25;
        this.saveButton = Button.builder(Component.literal(this.existingFileName == null ? "Save" : "Update"), button -> save())
                .bounds(left + panelW - 174, footerY, 74, 18).build();
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(left + panelW - 92, footerY, 74, 18).build());
        updateSaveButton();
    }

    private Button categoryButton(PresetStore.Category category, int x, int y, int width) {
        Button button = Button.builder(categoryLabel(category), clicked -> {
            if (this.categories.contains(category)) this.categories.remove(category); else this.categories.add(category);
            clicked.setMessage(categoryLabel(category));
            updateSaveButton();
        }).bounds(x, y, width, 18).build();
        button.setTooltip(Tooltip.create(Component.literal("Include this setting group.")));
        return button;
    }

    private Component categoryLabel(PresetStore.Category category) {
        return Component.literal((this.categories.contains(category) ? "[x] " : "[ ] ") + category.displayName());
    }

    private Button toggleButton(String label, int x, int y, int width, BooleanGetter getter, BooleanSetter setter, String tooltip) {
        Button button = Button.builder(Component.empty(), clicked -> {
            setter.set(!getter.get());
            clicked.setMessage(Component.literal((getter.get() ? "[x] " : "[ ] ") + label));
        }).bounds(x, y, width, 18).build();
        button.setMessage(Component.literal((getter.get() ? "[x] " : "[ ] ") + label));
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        return button;
    }

    private void updateSaveButton() {
        if (this.saveButton != null && this.nameBox != null)
            this.saveButton.active = !this.nameBox.getValue().trim().isEmpty() && !this.categories.isEmpty();
    }

    private void save() {
        String fileName = PresetStore.saveCurrent(this.nameBox.getValue(), this.categories, this.includeBlacklist,
                this.includeAimOverrides, this.includeAoeWeapons, this.existingFileName);
        if (fileName != null) returnToParent();
    }

    private void returnToParent() { if (this.minecraft != null) this.minecraft.setScreen(this.parent); }
    @Override public void onClose() { returnToParent(); }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        AimPointConfigScreen.drawPanel(graphics, panelLeft(), panelTop(), panelWidth(), panelHeight());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.setColor(1, 1, 1, 1);
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = panelLeft(); int top = panelTop();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 9, 0xFFFFFFFF);
        graphics.drawString(this.font, "Preset name", left + 16, top + 28, 0xFFBFC8D8, false);
        graphics.drawString(this.font, "Included groups", left + 16, top + 61, 0xFFBFC8D8, false);
        int rowStep = panelHeight() < 260 ? 20 : 22;
        graphics.drawString(this.font, "Optional data", left + 16, top + 72 + 4 * rowStep - 3, 0xFFBFC8D8, false);
    }

    @FunctionalInterface private interface BooleanGetter { boolean get(); }
    @FunctionalInterface private interface BooleanSetter { void set(boolean value); }
}
