package com.velorise.cameralockon;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Searchable, scrollable living-entity selector with a stable 3D preview. */
public class EntitySelectorScreen extends Screen {
    private static final int PANEL_WIDTH = 350;
    private static final int PANEL_HEIGHT = 244;
    private static final int LIST_WIDTH = 158;
    private static final int LIST_HEIGHT = 104;
    private static final int ROW_HEIGHT = 17;
    private static final int VISIBLE_ROWS = LIST_HEIGHT / ROW_HEIGHT;
    private static final int PREVIEW_WIDTH = 158;
    private static final int PREVIEW_HEIGHT = 132;
    private static final int MAX_HISTORY = 8;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Screen parentScreen;
    private final Consumer<String> selectionCallback;
    private final List<String> history;
    private final List<String> unavailableIds;
    private final String initiallySelectedId;
    private final String stateKey;
    private final boolean closeAfterSelection;
    private final String selectLabel;

    private final List<EntityOption> allOptions = new ArrayList<>();
    private final List<EntityOption> filteredOptions = new ArrayList<>();
    private final List<HistoryChip> historyChips = new ArrayList<>();

    private EditBox searchBox;
    private Button selectButton;
    private EntityOption highlightedOption;
    private LivingEntity previewEntity;

    private String searchText = "";
    private int scrollOffset;
    private boolean optionsLoaded;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    private float previewYaw = 18.0F;
    private float previewPitch;
    private boolean draggingPreview;
    private double lastDragX;
    private double lastDragY;
    private long manualRotationUntilMillis;
    private long lastPreviewFrameMillis;

    private EntityOption lastClickedOption;
    private long lastEntityClickMillis;

    private int listLeft;
    private int listTop;
    private int previewLeft;
    private int previewTop;

    public EntitySelectorScreen(
            Screen parentScreen,
            String initiallySelectedId,
            List<String> history,
            Consumer<String> selectionCallback
    ) {
        this(
                parentScreen,
                "target-selector",
                initiallySelectedId,
                history,
                selectionCallback,
                true,
                List.of(),
                "Select"
        );
    }

    public EntitySelectorScreen(
            Screen parentScreen,
            String stateKey,
            String initiallySelectedId,
            List<String> history,
            Consumer<String> selectionCallback,
            boolean closeAfterSelection,
            List<String> unavailableIds,
            String selectLabel
    ) {
        super(Component.translatable("gui.camera_lockon.selector.title"));
        this.parentScreen = parentScreen;
        this.stateKey = stateKey == null ? "entity-selector" : stateKey;
        this.initiallySelectedId = initiallySelectedId == null ? "" : initiallySelectedId;
        this.history = history;
        this.selectionCallback = selectionCallback;
        this.closeAfterSelection = closeAfterSelection;
        this.unavailableIds = unavailableIds == null
                ? new ArrayList<>()
                : new ArrayList<>(unavailableIds);
        this.selectLabel = selectLabel == null ? "gui.camera_lockon.selector.select" : selectLabel;
        normalizeHistory();

        EntitySelectorState.State state = EntitySelectorState.get(this.stateKey);
        this.searchText = state.searchText();
        this.scrollOffset = state.scrollOffset();
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.listLeft = left + 10;
        this.listTop = top + 49;
        this.previewLeft = left + 182;
        this.previewTop = top + 27;

        if (!this.optionsLoaded) {
            loadEntityOptions();
            this.optionsLoaded = true;
            restoreSelection();
        }

        this.searchBox = new EditBox(
                this.font,
                this.listLeft,
                top + 27,
                LIST_WIDTH,
                18,
                Component.translatable("gui.camera_lockon.selector.search")
        );
        this.searchBox.setMaxLength(80);
        this.searchBox.setHint(Component.translatable("gui.camera_lockon.selector.search_hint"));
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            this.scrollOffset = 0;
            rebuildFilteredOptions();
        });
        this.addRenderableWidget(this.searchBox);

        int footerY = top + PANEL_HEIGHT - 22;
        Component labelComp = this.selectLabel.startsWith("gui.") ? Component.translatable(this.selectLabel) : Component.literal(this.selectLabel);
        this.selectButton = Button.builder(labelComp, button -> confirmSelection())
                .bounds(left + 94, footerY, 76, 18)
                .build();
        this.selectButton.setTooltip(Tooltip.create(Component.translatable(
                this.closeAfterSelection
                        ? "gui.camera_lockon.selector.select.tooltip"
                        : "gui.camera_lockon.selector.add.tooltip"
        )));
        this.addRenderableWidget(this.selectButton);

        if (this.closeAfterSelection) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.camera_lockon.button.clear"), button -> {
                        saveSelectorState();
                        this.selectionCallback.accept("");
                        returnToParent();
                    })
                    .bounds(left + 177, footerY, 76, 18)
                    .build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.camera_lockon.button.done"), button -> {
                        saveSelectorState();
                        returnToParent();
                    })
                    .bounds(left + 177, footerY, 76, 18)
                    .build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.camera_lockon.button.back"), button -> {
                    saveSelectorState();
                    returnToParent();
                })
                .bounds(left + 260, footerY, 76, 18)
                .build());

        rebuildFilteredOptions();
        layoutHistoryChips(left, top);
        updateSelectButton();
    }

    private void loadEntityOptions() {
        this.allOptions.clear();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type == null || !isLivingEntityType(type)) {
                continue;
            }
            String displayName = type.getDescription().getString();
            if (displayName == null || displayName.isBlank()) {
                displayName = id.toString();
            }
            this.allOptions.add(new EntityOption(id, type, displayName));
        }
        this.allOptions.sort(
                Comparator.comparing((EntityOption option) -> option.displayName.toLowerCase(Locale.ROOT))
                        .thenComparing(option -> option.id.toString())
        );
    }

    private boolean isLivingEntityType(EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.level != null) {
            try {
                return type.create(this.minecraft.level) instanceof LivingEntity;
            } catch (Throwable ignored) {
            }
        }
        try {
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return type.getCategory() != MobCategory.MISC;
    }

    private LivingEntity createLivingEntity(EntityType<?> type) {
        if (type == EntityType.PLAYER || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        try {
            Entity entity = type.create(this.minecraft.level);
            return entity instanceof LivingEntity living ? living : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void rebuildFilteredOptions() {
        this.filteredOptions.clear();
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        for (EntityOption option : this.allOptions) {
            if (query.isEmpty()
                    || option.displayName.toLowerCase(Locale.ROOT).contains(query)
                    || option.id.toString().toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredOptions.add(option);
            }
        }
        clampScrollOffset();
    }

    private void restoreSelection() {
        EntitySelectorState.State state = EntitySelectorState.get(this.stateKey);
        String preferred = !state.highlightedEntityId().isBlank()
                ? state.highlightedEntityId()
                : this.initiallySelectedId;
        if (!preferred.isBlank()) {
            highlightOptionById(preferred, false);
        }
    }

    private void setHighlightedOption(EntityOption option) {
        this.highlightedOption = option;
        this.previewEntity = option == null ? null : createLivingEntity(option.type);
        this.previewYaw = 18.0F;
        this.previewPitch = 0.0F;
        this.lastPreviewFrameMillis = Util.getMillis();
        updateSelectButton();
    }

    private boolean highlightOptionById(String id, boolean reveal) {
        if (id == null || id.isBlank()) {
            return false;
        }
        for (EntityOption option : this.allOptions) {
            if (!option.id.toString().equals(id)) {
                continue;
            }
            setHighlightedOption(option);
            if (reveal) {
                this.searchText = "";
                if (this.searchBox != null) {
                    this.searchBox.setValue("");
                }
                rebuildFilteredOptions();
                int index = this.filteredOptions.indexOf(option);
                if (index >= 0) {
                    this.scrollOffset = Math.max(0, index - VISIBLE_ROWS / 2);
                    clampScrollOffset();
                }
            }
            return true;
        }
        return false;
    }

    private void confirmSelection() {
        if (this.highlightedOption == null || isUnavailable(this.highlightedOption)) {
            return;
        }
        String selectedId = this.highlightedOption.id.toString();
        addToHistory(selectedId);
        this.selectionCallback.accept(selectedId);
        if (!this.closeAfterSelection && !this.unavailableIds.contains(selectedId)) {
            this.unavailableIds.add(selectedId);
        }
        saveSelectorState();

        if (this.closeAfterSelection) {
            returnToParent();
            return;
        }

        int currentIndex = this.filteredOptions.indexOf(this.highlightedOption);
        if (currentIndex >= 0 && currentIndex + 1 < this.filteredOptions.size()) {
            setHighlightedOption(this.filteredOptions.get(currentIndex + 1));
        }
        updateSelectButton();
    }

    private boolean isUnavailable(EntityOption option) {
        return option != null && this.unavailableIds.contains(option.id.toString());
    }

    private void addToHistory(String id) {
        this.history.remove(id);
        this.history.add(0, id);
        normalizeHistory();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        layoutHistoryChips(left, top);
    }

    private void normalizeHistory() {
        List<String> normalized = new ArrayList<>();
        for (String id : this.history) {
            if (id == null
                    || id.isBlank()
                    || ResourceLocation.tryParse(id) == null
                    || normalized.contains(id)) {
                continue;
            }
            normalized.add(id);
            if (normalized.size() >= MAX_HISTORY) {
                break;
            }
        }
        this.history.clear();
        this.history.addAll(normalized);
    }

    private void layoutHistoryChips(int left, int top) {
        this.historyChips.clear();
        int x = left + 10;
        int y = top + 190;
        int right = left + PANEL_WIDTH - 10;
        for (String id : this.history) {
            String label = getEntityDisplayName(id);
            int chipWidth = Math.min(88, Math.max(42, this.font.width(label) + 12));
            if (x + chipWidth > right) {
                break;
            }
            this.historyChips.add(new HistoryChip(id, label, x, y, chipWidth, 15));
            x += chipWidth + 4;
        }
    }

    private String getEntityDisplayName(String idString) {
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return idString;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription().getString();
    }

    private void removeHistoryEntry(String id) {
        this.history.remove(id);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        layoutHistoryChips(left, top);
    }

    private void updateSelectButton() {
        if (this.selectButton != null) {
            this.selectButton.active = this.highlightedOption != null && !isUnavailable(this.highlightedOption);
            if (this.highlightedOption != null && isUnavailable(this.highlightedOption)) {
                this.selectButton.setMessage(Component.translatable("gui.camera_lockon.selector.already_added"));
            } else {
                Component labelComp = this.selectLabel.startsWith("gui.") ? Component.translatable(this.selectLabel) : Component.literal(this.selectLabel);
                this.selectButton.setMessage(labelComp);
            }
        }
    }

    private void clampScrollOffset() {
        this.scrollOffset = Mth.clamp(
                this.scrollOffset,
                0,
                Math.max(0, this.filteredOptions.size() - VISIBLE_ROWS)
        );
    }

    private boolean isInsideList(double mouseX, double mouseY) {
        return mouseX >= this.listLeft
                && mouseX < this.listLeft + LIST_WIDTH
                && mouseY >= this.listTop
                && mouseY < this.listTop + LIST_HEIGHT;
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= this.previewLeft
                && mouseX < this.previewLeft + PREVIEW_WIDTH
                && mouseY >= this.previewTop
                && mouseY < this.previewTop + PREVIEW_HEIGHT;
    }

    private int scrollbarLeft() {
        return this.listLeft + LIST_WIDTH - SCROLLBAR_WIDTH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (HistoryChip chip : this.historyChips) {
            if (!chip.contains(mouseX, mouseY)) {
                continue;
            }
            if (Screen.hasShiftDown()) {
                removeHistoryEntry(chip.id);
            } else {
                highlightOptionById(chip.id, true);
            }
            return true;
        }

        if (button == 0 && isInsideList(mouseX, mouseY)) {
            if (mouseX >= scrollbarLeft()) {
                beginScrollbarDrag(mouseY);
                return true;
            }

            int row = (int) ((mouseY - this.listTop) / ROW_HEIGHT);
            int index = this.scrollOffset + row;
            if (index >= 0 && index < this.filteredOptions.size()) {
                EntityOption option = this.filteredOptions.get(index);
                setHighlightedOption(option);
                long now = Util.getMillis();
                if (option == this.lastClickedOption && now - this.lastEntityClickMillis <= 350L) {
                    confirmSelection();
                }
                this.lastClickedOption = option;
                this.lastEntityClickMillis = now;
                return true;
            }
        }

        if (button == 1 && isInsidePreview(mouseX, mouseY) && this.previewEntity != null) {
            this.draggingPreview = true;
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void beginScrollbarDrag(double mouseY) {
        int thumbTop = getScrollbarThumbTop();
        int thumbHeight = getScrollbarThumbHeight();
        if (mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight) {
            this.scrollbarGrabOffset = (int) mouseY - thumbTop;
        } else {
            this.scrollbarGrabOffset = thumbHeight / 2;
            updateScrollFromMouse(mouseY);
        }
        this.draggingScrollbar = true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        if (button == 1 && this.draggingPreview) {
            this.previewYaw += (float) (mouseX - this.lastDragX) * 0.85F;
            this.previewPitch = 0.0F;
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            this.manualRotationUntilMillis = Util.getMillis() + 4500L;
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
        if (button == 1 && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideList(mouseX, mouseY) && scrollY != 0.0D) {
            this.scrollOffset -= (int) Math.signum(scrollY);
            clampScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getScrollbarThumbHeight() {
        if (this.filteredOptions.size() <= VISIBLE_ROWS) {
            return LIST_HEIGHT;
        }
        return Math.max(18, LIST_HEIGHT * VISIBLE_ROWS / this.filteredOptions.size());
    }

    private int getScrollbarThumbTop() {
        int maxScroll = Math.max(1, this.filteredOptions.size() - VISIBLE_ROWS);
        int track = LIST_HEIGHT - getScrollbarThumbHeight();
        return this.listTop + (int) Math.round(track * (this.scrollOffset / (double) maxScroll));
    }

    private void updateScrollFromMouse(double mouseY) {
        int thumbHeight = getScrollbarThumbHeight();
        int track = Math.max(1, LIST_HEIGHT - thumbHeight);
        double relative = mouseY - this.listTop - this.scrollbarGrabOffset;
        double fraction = Mth.clamp(relative / track, 0.0D, 1.0D);
        int maxScroll = Math.max(0, this.filteredOptions.size() - VISIBLE_ROWS);
        this.scrollOffset = (int) Math.round(fraction * maxScroll);
        clampScrollOffset();
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
        graphics.drawCenteredString(this.font, Component.translatable("gui.camera_lockon.selector.title"), this.width / 2, top + 9, 0xFFFFFFFF);
        renderEntityList(graphics, mouseX, mouseY);
        renderPreviewBackground(graphics);
        renderPreviewEntity(graphics);
        renderPreviewText(graphics);
        renderHistory(graphics, mouseX, mouseY, left, top);
    }

    private void renderEntityList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(
                this.listLeft - 2,
                this.listTop - 2,
                this.listLeft + LIST_WIDTH + 2,
                this.listTop + LIST_HEIGHT + 2,
                0xFF707070
        );
        graphics.fill(
                this.listLeft,
                this.listTop,
                this.listLeft + LIST_WIDTH,
                this.listTop + LIST_HEIGHT,
                0xFF202020
        );

        if (this.filteredOptions.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.camera_lockon.selector.no_matching"),
                    this.listLeft + LIST_WIDTH / 2,
                    this.listTop + LIST_HEIGHT / 2 - 4,
                    0xFFC0C0C0
            );
        } else {
            int usableWidth = LIST_WIDTH - SCROLLBAR_WIDTH;
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int optionIndex = this.scrollOffset + row;
                if (optionIndex >= this.filteredOptions.size()) {
                    break;
                }
                EntityOption option = this.filteredOptions.get(optionIndex);
                int rowY = this.listTop + row * ROW_HEIGHT;
                boolean hovered = mouseX >= this.listLeft
                        && mouseX < this.listLeft + usableWidth
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                boolean selected = option == this.highlightedOption;
                boolean unavailable = isUnavailable(option);

                int background = selected
                        ? 0xFF56636A
                        : hovered ? 0xFF3C3C3C : 0xFF292929;
                if (unavailable) {
                    background = selected ? 0xFF4A4A4A : 0xFF252525;
                }
                graphics.fill(
                        this.listLeft + 1,
                        rowY + 1,
                        this.listLeft + usableWidth - 1,
                        rowY + ROW_HEIGHT - 1,
                        background
                );
                if (selected) {
                    graphics.fill(
                            this.listLeft + 1,
                            rowY + 1,
                            this.listLeft + 3,
                            rowY + ROW_HEIGHT - 1,
                            0xFF66D9EF
                    );
                }

                String prefix = unavailable ? "✓ " : "";
                String label = this.font.plainSubstrByWidth(
                        prefix + option.displayName,
                        usableWidth - 11
                );
                graphics.drawString(
                        this.font,
                        label,
                        this.listLeft + 6,
                        rowY + 4,
                        unavailable ? 0xFF9A9A9A : 0xFFFFFFFF,
                        true
                );
            }
        }

        renderScrollbar(graphics);
        String count = this.filteredOptions.isEmpty()
                ? "0 entities"
                : (this.scrollOffset + 1)
                        + "-"
                        + Math.min(this.filteredOptions.size(), this.scrollOffset + VISIBLE_ROWS)
                        + " of "
                        + this.filteredOptions.size();
        graphics.drawString(
                this.font,
                count,
                this.listLeft + LIST_WIDTH - this.font.width(count),
                this.listTop + LIST_HEIGHT + 3,
                0xFFD0D0D0,
                true
        );
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int left = scrollbarLeft();
        graphics.fill(left, this.listTop, left + SCROLLBAR_WIDTH, this.listTop + LIST_HEIGHT, 0xFF151515);
        int thumbTop = getScrollbarThumbTop();
        int thumbHeight = getScrollbarThumbHeight();
        graphics.fill(left + 1, thumbTop, left + SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight, 0xFF9A9A9A);
    }

    private void renderPreviewBackground(GuiGraphics graphics) {
        graphics.fill(
                this.previewLeft - 2,
                this.previewTop - 2,
                this.previewLeft + PREVIEW_WIDTH + 2,
                this.previewTop + PREVIEW_HEIGHT + 2,
                0xFF707070
        );
        graphics.fill(
                this.previewLeft,
                this.previewTop,
                this.previewLeft + PREVIEW_WIDTH,
                this.previewTop + PREVIEW_HEIGHT,
                0xFF202020
        );
    }

    private void renderPreviewText(GuiGraphics graphics) {
        if (this.highlightedOption == null) {
            graphics.drawCenteredString(
                    this.font,
                    "Choose an entity",
                    this.previewLeft + PREVIEW_WIDTH / 2,
                    this.previewTop + PREVIEW_HEIGHT / 2 - 4,
                    0xFFC0C0C0
            );
            return;
        }

        graphics.fill(
                this.previewLeft + 2,
                this.previewTop + 2,
                this.previewLeft + PREVIEW_WIDTH - 2,
                this.previewTop + (CameraLockOnConfig.SHOW_REGISTRY_IDS.get() ? 29 : 18),
                0xCC181818
        );
        graphics.drawCenteredString(
                this.font,
                this.font.plainSubstrByWidth(this.highlightedOption.displayName, PREVIEW_WIDTH - 12),
                this.previewLeft + PREVIEW_WIDTH / 2,
                this.previewTop + 6,
                0xFFFFFFFF
        );
        if (CameraLockOnConfig.SHOW_REGISTRY_IDS.get()) {
            graphics.drawCenteredString(
                    this.font,
                    this.font.plainSubstrByWidth(this.highlightedOption.id.toString(), PREVIEW_WIDTH - 12),
                    this.previewLeft + PREVIEW_WIDTH / 2,
                    this.previewTop + 18,
                    0xFFB5B5B5
            );
        }

        if (this.previewEntity == null) {
            graphics.drawCenteredString(
                    this.font,
                    this.minecraft == null || this.minecraft.level == null
                            ? "Preview requires a world"
                            : "Preview unavailable",
                    this.previewLeft + PREVIEW_WIDTH / 2,
                    this.previewTop + 70,
                    0xFFFFA0A0
            );
        } else {
            graphics.drawCenteredString(
                    this.font,
                    "Right-drag to rotate",
                    this.previewLeft + PREVIEW_WIDTH / 2,
                    this.previewTop + PREVIEW_HEIGHT - 13,
                    0xFFAFAFAF
            );
        }
    }

    private void renderPreviewEntity(GuiGraphics graphics) {
        if (this.previewEntity == null) {
            return;
        }

        long now = Util.getMillis();
        if (this.lastPreviewFrameMillis == 0L) {
            this.lastPreviewFrameMillis = now;
        }
        long elapsedMillis = Math.min(100L, Math.max(0L, now - this.lastPreviewFrameMillis));
        this.lastPreviewFrameMillis = now;
        if (!this.draggingPreview && now >= this.manualRotationUntilMillis) {
            this.previewYaw += elapsedMillis * 0.030F;
        }
        this.previewPitch = 0.0F;

        try {
            float maxDimension = Math.max(this.previewEntity.getBbWidth(), this.previewEntity.getBbHeight());
            int scale = maxDimension <= 0.0F
                    ? 34
                    : Math.max(10, Math.min(40, (int) (54.0F / maxDimension)));

            float bodyYaw = Mth.wrapDegrees(180.0F + this.previewYaw);
            this.previewEntity.setYRot(bodyYaw);
            this.previewEntity.yRotO = bodyYaw;
            this.previewEntity.yBodyRot = bodyYaw;
            this.previewEntity.yBodyRotO = bodyYaw;
            this.previewEntity.yHeadRot = bodyYaw;
            this.previewEntity.yHeadRotO = bodyYaw;
            this.previewEntity.setXRot(0.0F);
            this.previewEntity.xRotO = 0.0F;

            float entityScale = Math.max(0.001F, this.previewEntity.getScale());
            Vector3f translation = new Vector3f(
                    0.0F,
                    this.previewEntity.getBbHeight() / 2.0F,
                    0.0F
            );
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            InventoryScreen.renderEntityInInventory(
                    graphics,
                    this.previewLeft + PREVIEW_WIDTH / 2.0F,
                    this.previewTop + 62.0F,
                    scale / entityScale,
                    translation,
                    pose,
                    null,
                    this.previewEntity
            );
        } catch (Throwable ignored) {
            graphics.drawCenteredString(
                    this.font,
                    "Renderer unavailable",
                    this.previewLeft + PREVIEW_WIDTH / 2,
                    this.previewTop + 70,
                    0xFFFFA0A0
            );
        }
    }

    private void renderHistory(GuiGraphics graphics, int mouseX, int mouseY, int left, int top) {
        graphics.drawString(
                this.font,
                "Recent — click to reuse, Shift-click to remove",
                left + 10,
                top + 176,
                0xFFFFFFFF,
                true
        );
        if (this.historyChips.isEmpty()) {
            graphics.drawString(this.font, "None", left + 10, top + 190, 0xFFAAAAAA, true);
            return;
        }

        for (HistoryChip chip : this.historyChips) {
            boolean hovered = chip.contains(mouseX, mouseY);
            graphics.fill(chip.x, chip.y, chip.x + chip.width, chip.y + chip.height, 0xFF202020);
            graphics.fill(
                    chip.x + 1,
                    chip.y + 1,
                    chip.x + chip.width - 1,
                    chip.y + chip.height - 1,
                    hovered ? 0xFF6A6A6A : 0xFF4F4F4F
            );
            String label = this.font.plainSubstrByWidth(chip.label, chip.width - 8);
            graphics.drawCenteredString(this.font, label, chip.x + chip.width / 2, chip.y + 4, 0xFFFFFFFF);
        }
    }

    private void saveSelectorState() {
        EntitySelectorState.save(
                this.stateKey,
                this.searchText,
                this.scrollOffset,
                this.highlightedOption == null ? "" : this.highlightedOption.id.toString()
        );
    }

    private void returnToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void onClose() {
        saveSelectorState();
        returnToParent();
    }

    private static final class EntityOption {
        private final ResourceLocation id;
        private final EntityType<?> type;
        private final String displayName;

        private EntityOption(ResourceLocation id, EntityType<?> type, String displayName) {
            this.id = id;
            this.type = type;
            this.displayName = displayName;
        }
    }

    private static final class HistoryChip {
        private final String id;
        private final String label;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private HistoryChip(String id, String label, int x, int y, int width, int height) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x
                    && mouseX < this.x + this.width
                    && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }
}
