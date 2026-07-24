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
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 286;

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

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        this.nameBox = new EditBox(this.font, left + 20, top + 50, PANEL_WIDTH - 40, 18, Component.literal("Preset Name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setHint(Component.literal("Preset name"));
        if (this.existingFileName != null) {
            PresetStore.Preset preset = PresetStore.load(this.existingFileName);
            if (preset != null) {
                this.nameBox.setValue(preset.name());
            }
        }
        this.nameBox.setResponder(value -> updateSaveButton());
        this.addRenderableWidget(this.nameBox);

        int x1 = left + 20;
        int x2 = left + 184;
        int y = top + 87;
        PresetStore.Category[] values = PresetStore.Category.values();
        for (int i = 0; i < values.length; i++) {
            PresetStore.Category category = values[i];
            int x = i % 2 == 0 ? x1 : x2;
            int rowY = y + (i / 2) * 22;
            this.addRenderableWidget(categoryButton(category, x, rowY));
        }

        int advancedY = top + 183;
        this.addRenderableWidget(toggleButton("Blacklist", x1, advancedY, () -> this.includeBlacklist, value -> this.includeBlacklist = value,
                "Include the target blacklist in this preset. Disabled by default for safer sharing."));
        this.addRenderableWidget(toggleButton("Aim Overrides", x2, advancedY, () -> this.includeAimOverrides, value -> this.includeAimOverrides = value,
                "Include all per-entity aim points. Disabled by default."));
        this.addRenderableWidget(toggleButton("AOE Weapons", x1, advancedY + 22, () -> this.includeAoeWeapons, value -> this.includeAoeWeapons = value,
                "Include the manual AOE weapon list. Disabled by default."));

        this.saveButton = Button.builder(Component.literal(this.existingFileName == null ? "Save" : "Update"), button -> save())
                .bounds(left + PANEL_WIDTH - 178, top + PANEL_HEIGHT - 28, 74, 18)
                .build();
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(left + PANEL_WIDTH - 96, top + PANEL_HEIGHT - 28, 74, 18)
                .build());
        updateSaveButton();
    }

    private Button categoryButton(PresetStore.Category category, int x, int y) {
        Button button = Button.builder(categoryLabel(category), clicked -> {
                    if (this.categories.contains(category)) {
                        this.categories.remove(category);
                    } else {
                        this.categories.add(category);
                    }
                    clicked.setMessage(categoryLabel(category));
                    updateSaveButton();
                })
                .bounds(x, y, 156, 18)
                .build();
        button.setTooltip(Tooltip.create(Component.literal("Copy this group from the current configuration into the preset.")));
        return button;
    }

    private Component categoryLabel(PresetStore.Category category) {
        return Component.literal((this.categories.contains(category) ? "[x] " : "[ ] ") + category.displayName());
    }

    private Button toggleButton(
            String label,
            int x,
            int y,
            BooleanGetter getter,
            BooleanSetter setter,
            String tooltip
    ) {
        Button button = Button.builder(Component.empty(), clicked -> {
                    setter.set(!getter.get());
                    clicked.setMessage(Component.literal((getter.get() ? "[x] " : "[ ] ") + label));
                })
                .bounds(x, y, 156, 18)
                .build();
        button.setMessage(Component.literal((getter.get() ? "[x] " : "[ ] ") + label));
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        return button;
    }

    private void updateSaveButton() {
        if (this.saveButton != null && this.nameBox != null) {
            this.saveButton.active = !this.nameBox.getValue().trim().isEmpty() && !this.categories.isEmpty();
        }
    }

    private void save() {
        String fileName = PresetStore.saveCurrent(
                this.nameBox.getValue(),
                this.categories,
                this.includeBlacklist,
                this.includeAimOverrides,
                this.includeAoeWeapons,
                this.existingFileName
        );
        if (fileName != null) {
            returnToParent();
        }
    }

    private void returnToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
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
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 11, 0xFFFFFFFF);
        graphics.drawString(this.font, "Preset name", left + 20, top + 38, 0xFFBFC8D8, false);
        graphics.drawString(this.font, "Included setting groups", left + 20, top + 75, 0xFFBFC8D8, false);
        graphics.drawString(this.font, "Optional shared data", left + 20, top + 169, 0xFFBFC8D8, false);
        graphics.drawCenteredString(this.font,
                "This copies current settings into the preset; it does not change the active configuration.",
                this.width / 2, top + 231, 0xFF9FAABD);
    }

    @FunctionalInterface
    private interface BooleanGetter { boolean get(); }

    @FunctionalInterface
    private interface BooleanSetter { void set(boolean value); }
}
