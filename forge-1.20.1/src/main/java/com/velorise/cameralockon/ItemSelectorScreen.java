package com.velorise.cameralockon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Searchable item selector used by the manual AOE weapon manager. */
public final class ItemSelectorScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 230;
    private static final int LIST_WIDTH = 176;
    private static final int LIST_HEIGHT = 136;
    private static final int ROW_HEIGHT = 17;
    private static final int VISIBLE_ROWS = LIST_HEIGHT / ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;

    private static String rememberedSearch = "";
    private static int rememberedScroll;

    private final Screen parentScreen;
    private final Consumer<String> selectionCallback;
    private final List<String> unavailableIds;

    private final List<ItemOption> allOptions = new ArrayList<>();
    private final List<ItemOption> filteredOptions = new ArrayList<>();

    private EditBox searchBox;
    private Button addButton;
    private ItemOption selected;
    private String searchText = rememberedSearch;
    private int scrollOffset = rememberedScroll;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    private int listLeft;
    private int listTop;
    private int previewLeft;
    private int previewTop;

    public ItemSelectorScreen(
            Screen parentScreen,
            List<String> unavailableIds,
            Consumer<String> selectionCallback
    ) {
        super(Component.literal("Select AOE Weapon"));
        this.parentScreen = parentScreen;
        this.unavailableIds = unavailableIds == null
                ? new ArrayList<>()
                : new ArrayList<>(unavailableIds);
        this.selectionCallback = selectionCallback;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.listLeft = left + 10;
        this.listTop = top + 48;
        this.previewLeft = left + 198;
        this.previewTop = top + 48;

        if (this.allOptions.isEmpty()) {
            loadItems();
        }

        this.searchBox = new EditBox(
                this.font,
                this.listLeft,
                top + 25,
                LIST_WIDTH,
                18,
                Component.literal("Search Item")
        );
        this.searchBox.setHint(Component.literal("Search name or registry ID"));
        this.searchBox.setValue(this.searchText);
        this.searchBox.setMaxLength(100);
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            this.scrollOffset = 0;
            rebuildFiltered();
        });
        this.addRenderableWidget(this.searchBox);

        this.addButton = Button.builder(Component.literal("Add"), button -> addSelected())
                .bounds(left + 198, top + 184, 72, 18)
                .build();
        this.addButton.setTooltip(Tooltip.create(Component.literal(
                "Add the selected item as a manual AOE weapon override."
        )));
        this.addRenderableWidget(this.addButton);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> returnToParent())
                .bounds(left + 278, top + 184, 72, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> returnToParent())
                .bounds(left + 198, top + 207, 152, 18)
                .build());

        rebuildFiltered();
        updateAddButton();
    }

    private void loadItems() {
        this.allOptions.clear();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            String name = stack.getHoverName().getString();
            if (name == null || name.isBlank()) {
                name = id.toString();
            }
            this.allOptions.add(new ItemOption(id, item, name));
        }
        this.allOptions.sort(
                Comparator.comparing((ItemOption option) -> option.name().toLowerCase(Locale.ROOT))
                        .thenComparing(option -> option.id().toString())
        );
    }

    private void rebuildFiltered() {
        this.filteredOptions.clear();
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        for (ItemOption option : this.allOptions) {
            if (query.isEmpty()
                    || option.name().toLowerCase(Locale.ROOT).contains(query)
                    || option.id().toString().toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredOptions.add(option);
            }
        }
        clampScroll();
    }

    private void addSelected() {
        if (this.selected == null || this.unavailableIds.contains(this.selected.id().toString())) {
            return;
        }
        String id = this.selected.id().toString();
        this.selectionCallback.accept(id);
        if (!this.unavailableIds.contains(id)) {
            this.unavailableIds.add(id);
        }
        updateAddButton();
    }

    private void updateAddButton() {
        if (this.addButton == null) {
            return;
        }
        boolean unavailable = this.selected != null
                && this.unavailableIds.contains(this.selected.id().toString());
        this.addButton.active = this.selected != null && !unavailable;
        this.addButton.setMessage(Component.literal(unavailable ? "Already Added" : "Add"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideList(mouseX, mouseY)) {
            if (mouseX >= scrollbarLeft()) {
                beginScrollbarDrag(mouseY);
                return true;
            }
            int row = (int) ((mouseY - this.listTop) / ROW_HEIGHT);
            int index = this.scrollOffset + row;
            if (index >= 0 && index < this.filteredOptions.size()) {
                this.selected = this.filteredOptions.get(index);
                updateAddButton();
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
        if (isInsideList(mouseX, mouseY) && scrollDelta != 0.0D) {
            this.scrollOffset -= (int) Math.signum(scrollDelta);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    private boolean isInsideList(double mouseX, double mouseY) {
        return mouseX >= this.listLeft
                && mouseX < this.listLeft + LIST_WIDTH
                && mouseY >= this.listTop
                && mouseY < this.listTop + LIST_HEIGHT;
    }

    private int scrollbarLeft() {
        return this.listLeft + LIST_WIDTH - SCROLLBAR_WIDTH;
    }

    private void clampScroll() {
        this.scrollOffset = Mth.clamp(
                this.scrollOffset,
                0,
                Math.max(0, this.filteredOptions.size() - VISIBLE_ROWS)
        );
    }

    private int thumbHeight() {
        if (this.filteredOptions.size() <= VISIBLE_ROWS) {
            return LIST_HEIGHT;
        }
        return Math.max(18, LIST_HEIGHT * VISIBLE_ROWS / this.filteredOptions.size());
    }

    private int thumbTop() {
        int maxScroll = Math.max(1, this.filteredOptions.size() - VISIBLE_ROWS);
        int track = LIST_HEIGHT - thumbHeight();
        return this.listTop + (int) Math.round(track * (this.scrollOffset / (double) maxScroll));
    }

    private void beginScrollbarDrag(double mouseY) {
        int top = thumbTop();
        int height = thumbHeight();
        if (mouseY >= top && mouseY <= top + height) {
            this.scrollbarGrabOffset = (int) mouseY - top;
        } else {
            this.scrollbarGrabOffset = height / 2;
            updateScrollFromMouse(mouseY);
        }
        this.draggingScrollbar = true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int height = thumbHeight();
        int track = Math.max(1, LIST_HEIGHT - height);
        double fraction = Mth.clamp(
                (mouseY - this.listTop - this.scrollbarGrabOffset) / track,
                0.0D,
                1.0D
        );
        this.scrollOffset = (int) Math.round(
                fraction * Math.max(0, this.filteredOptions.size() - VISIBLE_ROWS)
        );
        clampScroll();
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

        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, "Select AOE Weapon", this.width / 2, top + 8, 0xFFFFFFFF);
        renderList(graphics, mouseX, mouseY);
        renderPreview(graphics);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(
                this.listLeft - 2,
                this.listTop - 2,
                this.listLeft + LIST_WIDTH + 2,
                this.listTop + LIST_HEIGHT + 2,
                0xFF6A6A6A
        );
        graphics.fill(
                this.listLeft,
                this.listTop,
                this.listLeft + LIST_WIDTH,
                this.listTop + LIST_HEIGHT,
                0xEE171717
        );

        int usableWidth = LIST_WIDTH - SCROLLBAR_WIDTH;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.scrollOffset + row;
            if (index >= this.filteredOptions.size()) {
                break;
            }
            ItemOption option = this.filteredOptions.get(index);
            int rowY = this.listTop + row * ROW_HEIGHT;
            boolean hovered = mouseX >= this.listLeft
                    && mouseX < this.listLeft + usableWidth
                    && mouseY >= rowY
                    && mouseY < rowY + ROW_HEIGHT;
            boolean selectedRow = option == this.selected;
            boolean unavailable = this.unavailableIds.contains(option.id().toString());

            int background = selectedRow
                    ? 0xFF55646C
                    : hovered ? 0xFF3A3A3A : 0xFF272727;
            graphics.fill(
                    this.listLeft + 1,
                    rowY + 1,
                    this.listLeft + usableWidth - 1,
                    rowY + ROW_HEIGHT - 1,
                    background
            );
            if (selectedRow) {
                graphics.fill(
                        this.listLeft + 1,
                        rowY + 1,
                        this.listLeft + 3,
                        rowY + ROW_HEIGHT - 1,
                        0xFF62DCE7
                );
            }

            graphics.drawString(
                    this.font,
                    this.font.plainSubstrByWidth(
                            (unavailable ? "✓ " : "") + option.name(),
                            usableWidth - 10
                    ),
                    this.listLeft + 6,
                    rowY + 4,
                    unavailable ? 0xFF999999 : 0xFFFFFFFF,
                    true
            );
        }

        int barLeft = scrollbarLeft();
        graphics.fill(barLeft, this.listTop, barLeft + SCROLLBAR_WIDTH, this.listTop + LIST_HEIGHT, 0xFF111111);
        graphics.fill(
                barLeft + 1,
                thumbTop(),
                barLeft + SCROLLBAR_WIDTH - 1,
                thumbTop() + thumbHeight(),
                0xFF999999
        );
    }

    private void renderPreview(GuiGraphics graphics) {
        graphics.fill(
                this.previewLeft - 2,
                this.previewTop - 2,
                this.previewLeft + 152,
                this.previewTop + 132,
                0xFF666666
        );
        graphics.fill(
                this.previewLeft,
                this.previewTop,
                this.previewLeft + 150,
                this.previewTop + 130,
                0xEE151515
        );

        if (this.selected == null) {
            graphics.drawCenteredString(
                    this.font,
                    "Choose an item",
                    this.previewLeft + 75,
                    this.previewTop + 60,
                    0xFFBBBBBB
            );
            return;
        }

        graphics.drawCenteredString(
                this.font,
                this.font.plainSubstrByWidth(this.selected.name(), 138),
                this.previewLeft + 75,
                this.previewTop + 8,
                0xFFFFFFFF
        );
        graphics.drawCenteredString(
                this.font,
                this.font.plainSubstrByWidth(this.selected.id().toString(), 138),
                this.previewLeft + 75,
                this.previewTop + 20,
                0xFFAFAFAF
        );

        ItemStack stack = new ItemStack(this.selected.item());
        graphics.pose().pushPose();
        graphics.pose().translate(this.previewLeft + 43, this.previewTop + 42, 200.0F);
        graphics.pose().scale(4.0F, 4.0F, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();

        graphics.drawCenteredString(
                this.font,
                "Manual AOE override",
                this.previewLeft + 75,
                this.previewTop + 111,
                0xFFD6D6D6
        );
    }

    private void rememberState() {
        rememberedSearch = this.searchText;
        rememberedScroll = this.scrollOffset;
    }

    private void returnToParent() {
        rememberState();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private record ItemOption(ResourceLocation id, Item item, String name) {
    }
}
