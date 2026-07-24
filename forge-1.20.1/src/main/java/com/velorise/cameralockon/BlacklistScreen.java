package com.velorise.cameralockon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Scrollable blacklist editor with a compact pixel-X remove control. */
public class BlacklistScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 248;
    private static final int LIST_WIDTH = 276;
    private static final int LIST_HEIGHT = 126;
    private static final int ROW_HEIGHT = 18;
    private static final int VISIBLE_ROWS = LIST_HEIGHT / ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Screen parentScreen;
    private final List<String> blacklist;
    private final List<String> history;
    private final List<String> filtered = new ArrayList<>();

    private EditBox searchBox;
    private String searchText = "";
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;
    private int listLeft;
    private int listTop;

    public BlacklistScreen(Screen parentScreen, List<String> blacklist, List<String> history) {
        super(Component.literal("Target Blacklist"));
        this.parentScreen = parentScreen;
        this.blacklist = blacklist;
        this.history = history;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.listLeft = left + 12;
        this.listTop = top + 54;

        this.searchBox = new EditBox(
                this.font,
                left + 12,
                top + 29,
                LIST_WIDTH,
                18,
                Component.literal("Search Blacklist")
        );
        this.searchBox.setHint(Component.literal("Search blacklisted entities"));
        this.searchBox.setMaxLength(80);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            this.scrollOffset = 0;
            rebuildFiltered();
        });
        this.addRenderableWidget(this.searchBox);

        Button add = Button.builder(Component.literal("Add Entities"), button -> openEntitySelector())
                .bounds(left + 12, top + 198, 86, 18)
                .build();
        add.setTooltip(Tooltip.create(Component.literal(
                "Open the entity list. Added entities stay selected in place so you can add several without scrolling back."
        )));
        this.addRenderableWidget(add);

        Button clear = Button.builder(Component.literal("Clear All"), button -> confirmClearAll())
                .bounds(left + 107, top + 198, 86, 18)
                .build();
        clear.active = !this.blacklist.isEmpty();
        this.addRenderableWidget(clear);

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> returnToParent())
                .bounds(left + 202, top + 198, 86, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> returnToParent())
                .bounds(left + 12, top + 222, 276, 18)
                .build());

        rebuildFiltered();
    }

    private void rebuildFiltered() {
        this.filtered.clear();
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        for (String id : this.blacklist) {
            String name = TargetingRules.getEntityDisplayName(id);
            if (query.isEmpty()
                    || id.toLowerCase(Locale.ROOT).contains(query)
                    || name.toLowerCase(Locale.ROOT).contains(query)) {
                this.filtered.add(id);
            }
        }
        this.filtered.sort(
                Comparator.comparing((String id) -> TargetingRules.getEntityDisplayName(id).toLowerCase(Locale.ROOT))
                        .thenComparing(id -> id)
        );
        clampScrollOffset();
    }

    private void openEntitySelector() {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.setScreen(new EntitySelectorScreen(
                this,
                "blacklist-add-selector",
                "",
                this.history,
                id -> {
                    if (id != null && !id.isBlank() && !this.blacklist.contains(id)) {
                        this.blacklist.add(id);
                        rebuildFiltered();
                    }
                },
                false,
                this.blacklist,
                "Add"
        ));
    }

    private void confirmClearAll() {
        if (this.minecraft == null || this.blacklist.isEmpty()) {
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        this.blacklist.clear();
                        this.scrollOffset = 0;
                        rebuildFiltered();
                    }
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this);
                    }
                },
                Component.literal("Clear target blacklist?"),
                Component.literal("Every blacklisted entity type will become targetable again."),
                Component.literal("Clear All"),
                Component.literal("Cancel")
        ));
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
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
            if (index >= 0 && index < this.filtered.size()) {
                int usableWidth = LIST_WIDTH - SCROLLBAR_WIDTH;
                int removeLeft = this.listLeft + usableWidth - 18;
                if (mouseX >= removeLeft) {
                    String id = this.filtered.get(index);
                    this.blacklist.remove(id);
                    rebuildWidgets();
                    return true;
                }
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
            clampScrollOffset();
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

    private void clampScrollOffset() {
        this.scrollOffset = Mth.clamp(
                this.scrollOffset,
                0,
                Math.max(0, this.filtered.size() - VISIBLE_ROWS)
        );
    }

    private int thumbHeight() {
        if (this.filtered.size() <= VISIBLE_ROWS) {
            return LIST_HEIGHT;
        }
        return Math.max(18, LIST_HEIGHT * VISIBLE_ROWS / this.filtered.size());
    }

    private int thumbTop() {
        int maxScroll = Math.max(1, this.filtered.size() - VISIBLE_ROWS);
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
                fraction * Math.max(0, this.filtered.size() - VISIBLE_ROWS)
        );
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
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        super.render(graphics, mouseX, mouseY, partialTick);

        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, "Target Blacklist", this.width / 2, top + 10, 0xFFFFFFFF);
        renderList(graphics, mouseX, mouseY);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
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

        int usableWidth = LIST_WIDTH - SCROLLBAR_WIDTH;
        if (this.filtered.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    this.blacklist.isEmpty() ? "Blacklist is empty" : "No matching entries",
                    this.listLeft + usableWidth / 2,
                    this.listTop + LIST_HEIGHT / 2 - 4,
                    0xFFC0C0C0
            );
        } else {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int index = this.scrollOffset + row;
                if (index >= this.filtered.size()) {
                    break;
                }
                String id = this.filtered.get(index);
                int rowY = this.listTop + row * ROW_HEIGHT;
                boolean hovered = mouseX >= this.listLeft
                        && mouseX < this.listLeft + usableWidth
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                graphics.fill(
                        this.listLeft + 1,
                        rowY + 1,
                        this.listLeft + usableWidth - 1,
                        rowY + ROW_HEIGHT - 1,
                        hovered ? 0xFF533838 : 0xFF292929
                );
                String name = TargetingRules.getEntityDisplayName(id);
                String text = this.font.plainSubstrByWidth(name, usableWidth - 34);
                graphics.drawString(this.font, text, this.listLeft + 6, rowY + 5, 0xFFFFFFFF, true);
                drawPixelX(
                        graphics,
                        this.listLeft + usableWidth - 11,
                        rowY + ROW_HEIGHT / 2,
                        hovered ? 0xFFFF7777 : 0xFFE0E0E0
                );
            }
        }

        int barLeft = scrollbarLeft();
        graphics.fill(barLeft, this.listTop, barLeft + SCROLLBAR_WIDTH, this.listTop + LIST_HEIGHT, 0xFF151515);
        graphics.fill(
                barLeft + 1,
                thumbTop(),
                barLeft + SCROLLBAR_WIDTH - 1,
                thumbTop() + thumbHeight(),
                0xFF9A9A9A
        );

        boolean filtering = !this.searchText.trim().isEmpty();
        String count = filtering
                ? this.filtered.size() + " / " + this.blacklist.size() + " entries"
                : this.blacklist.size() + " entries";
        graphics.drawString(
                this.font,
                count,
                this.listLeft,
                this.listTop + LIST_HEIGHT + 4,
                0xFFD0D0D0,
                true
        );
    }

    private static void drawPixelX(GuiGraphics graphics, int centerX, int centerY, int color) {
        graphics.fill(centerX - 3, centerY - 3, centerX - 1, centerY - 1, color);
        graphics.fill(centerX + 1, centerY - 3, centerX + 3, centerY - 1, color);
        graphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, color);
        graphics.fill(centerX - 3, centerY + 1, centerX - 1, centerY + 3, color);
        graphics.fill(centerX + 1, centerY + 1, centerX + 3, centerY + 3, color);
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
