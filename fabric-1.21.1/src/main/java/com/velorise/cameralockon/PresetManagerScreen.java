package com.velorise.cameralockon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Lists, previews, loads, updates and deletes shareable preset files. */
public final class PresetManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 278;
    private static final int LIST_LEFT_OFFSET = 16;
    private static final int LIST_TOP_OFFSET = 63;
    private static final int LIST_WIDTH = 170;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 7;

    private final Screen parent;
    private final List<PresetStore.PresetSummary> presets = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset;
    private Button loadButton;
    private Button updateButton;
    private Button deleteButton;

    public PresetManagerScreen(Screen parent) {
        super(Component.literal("Camera Lock-On Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        reloadPresets();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Save Current..."), button -> openEditor(null))
                .bounds(left + 16, top + PANEL_HEIGHT - 27, 104, 18)
                .build());

        this.loadButton = Button.builder(Component.literal("Load"), button -> confirmLoad())
                .bounds(left + 204, top + 174, 72, 18)
                .build();
        this.loadButton.setTooltip(Tooltip.create(Component.literal("Replace only the setting groups included by the selected preset.")));
        this.addRenderableWidget(this.loadButton);

        this.updateButton = Button.builder(Component.literal("Update..."), button -> openEditor(selected()))
                .bounds(left + 282, top + 174, 82, 18)
                .build();
        this.updateButton.setTooltip(Tooltip.create(Component.literal("Copy the current configuration into this preset. Active settings are not changed.")));
        this.addRenderableWidget(this.updateButton);

        this.deleteButton = Button.builder(Component.literal("Delete"), button -> confirmDelete())
                .bounds(left + 204, top + 198, 72, 18)
                .build();
        this.addRenderableWidget(this.deleteButton);

        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> {
                    reloadPresets();
                    rebuildWidgets();
                })
                .bounds(left + 282, top + 198, 82, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> returnToParent())
                .bounds(left + PANEL_WIDTH - 92, top + PANEL_HEIGHT - 27, 76, 18)
                .build());
        updateActionButtons();
    }

    private void reloadPresets() {
        String selectedFile = selected() == null ? null : selected().fileName();
        this.presets.clear();
        this.presets.addAll(PresetStore.list());
        this.selectedIndex = -1;
        if (selectedFile != null) {
            for (int i = 0; i < this.presets.size(); i++) {
                if (this.presets.get(i).fileName().equals(selectedFile)) {
                    this.selectedIndex = i;
                    break;
                }
            }
        }
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, Math.max(0, this.presets.size() - VISIBLE_ROWS));
    }

    private PresetStore.PresetSummary selected() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.presets.size()
                ? this.presets.get(this.selectedIndex)
                : null;
    }

    private void openEditor(PresetStore.PresetSummary summary) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PresetEditScreen(this, summary));
        }
    }

    private void confirmLoad() {
        PresetStore.PresetSummary summary = selected();
        if (summary == null || this.minecraft == null) {
            return;
        }
        PresetStore.Preset preset = PresetStore.load(summary.fileName());
        if (preset == null) {
            reloadPresets();
            rebuildWidgets();
            return;
        }
        String groups = summary.categories().stream()
                .map(PresetStore.Category::displayName)
                .collect(Collectors.joining(", "));
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PresetStore.apply(preset);
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(this);
            }
        }, Component.literal("Load preset ‘" + preset.name() + "’?"),
                Component.literal("Replaces included current groups: " + (groups.isBlank() ? "None" : groups)),
                Component.literal("Load"), Component.literal("Cancel")));
    }

    private void confirmDelete() {
        PresetStore.PresetSummary summary = selected();
        if (summary == null || this.minecraft == null) {
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PresetStore.delete(summary.fileName());
                this.selectedIndex = -1;
                reloadPresets();
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(this);
            }
        }, Component.literal("Delete preset ‘" + summary.name() + "’?"),
                Component.literal("The JSON file will be removed from the presets folder."),
                Component.literal("Delete"), Component.literal("Cancel")));
    }

    private void updateActionButtons() {
        boolean active = selected() != null;
        if (this.loadButton != null) this.loadButton.active = active;
        if (this.updateButton != null) this.updateButton.active = active;
        if (this.deleteButton != null) this.deleteButton.active = active;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_WIDTH) / 2 + LIST_LEFT_OFFSET;
        int top = (this.height - PANEL_HEIGHT) / 2 + LIST_TOP_OFFSET;
        if (button == 0
                && mouseX >= left && mouseX < left + LIST_WIDTH
                && mouseY >= top && mouseY < top + ROW_HEIGHT * VISIBLE_ROWS) {
            int row = (int) ((mouseY - top) / ROW_HEIGHT);
            int index = this.scrollOffset + row;
            if (index >= 0 && index < this.presets.size()) {
                this.selectedIndex = index;
                updateActionButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (this.width - PANEL_WIDTH) / 2 + LIST_LEFT_OFFSET;
        int top = (this.height - PANEL_HEIGHT) / 2 + LIST_TOP_OFFSET;
        if (mouseX >= left && mouseX < left + LIST_WIDTH
                && mouseY >= top && mouseY < top + ROW_HEIGHT * VISIBLE_ROWS
                && scrollY != 0.0D) {
            this.scrollOffset = Mth.clamp(
                    this.scrollOffset - (int) Math.signum(scrollY),
                    0,
                    Math.max(0, this.presets.size() - VISIBLE_ROWS)
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int listLeft = left + LIST_LEFT_OFFSET;
        int listTop = top + LIST_TOP_OFFSET;

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font,
                "Presets are saved copies. Nothing changes until you press Load.",
                this.width / 2, top + 28, 0xFFB8C4D6);
        graphics.drawCenteredString(this.font,
                "Share or import them as JSON files in config/camera_lockon/presets.",
                this.width / 2, top + 40, 0xFF8592A8);
        graphics.drawString(this.font, "Saved presets", listLeft, top + 52, 0xFFD9E2F0, false);

        graphics.fill(listLeft - 3, listTop - 3, listLeft + LIST_WIDTH + 3,
                listTop + ROW_HEIGHT * VISIBLE_ROWS + 3, 0x66070A10);

        if (this.presets.isEmpty()) {
            graphics.drawCenteredString(this.font, "No presets yet", listLeft + LIST_WIDTH / 2, listTop + 57, 0xFF8994A6);
        } else {
            int end = Math.min(this.presets.size(), this.scrollOffset + VISIBLE_ROWS);
            for (int index = this.scrollOffset; index < end; index++) {
                int row = index - this.scrollOffset;
                int y = listTop + row * ROW_HEIGHT;
                if (index == this.selectedIndex) {
                    graphics.fill(listLeft, y, listLeft + LIST_WIDTH, y + ROW_HEIGHT - 2, 0x99505F78);
                } else if (mouseX >= listLeft && mouseX < listLeft + LIST_WIDTH
                        && mouseY >= y && mouseY < y + ROW_HEIGHT - 2) {
                    graphics.fill(listLeft, y, listLeft + LIST_WIDTH, y + ROW_HEIGHT - 2, 0x66404B5E);
                }
                PresetStore.PresetSummary summary = this.presets.get(index);
                graphics.drawString(this.font, summary.name(), listLeft + 5, y + 5, 0xFFFFFFFF, false);
            }
        }

        PresetStore.PresetSummary summary = selected();
        int detailX = left + 204;
        graphics.drawString(this.font, "Selected preset", detailX, top + 63, 0xFFD9E2F0, false);
        if (summary == null) {
            graphics.drawString(this.font, "Select a preset from the list.", detailX, top + 82, 0xFF8994A6, false);
        } else {
            graphics.drawString(this.font, summary.name(), detailX, top + 82, 0xFFFFFFFF, false);
            String groups = summary.categories().stream()
                    .map(PresetStore.Category::displayName)
                    .collect(Collectors.joining(", "));
            graphics.drawWordWrap(this.font, Component.literal("Includes: " + groups), detailX, top + 100, 160, 0xFFB8C4D6);
            PresetStore.Preset preset = PresetStore.load(summary.fileName());
            if (preset != null) {
                List<String> extras = new ArrayList<>();
                if (preset.includesBlacklist()) extras.add("Blacklist");
                if (preset.includesAimOverrides()) extras.add("Aim overrides");
                if (preset.includesAoeWeapons()) extras.add("AOE weapons");
                graphics.drawWordWrap(this.font,
                        Component.literal(extras.isEmpty() ? "Shared data: None" : "Shared data: " + String.join(", ", extras)),
                        detailX, top + 136, 160, 0xFF8592A8);
            }
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
}
