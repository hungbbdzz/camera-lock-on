package com.velorise.cameralockon;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Manager for manually declared projectile weapons. */
public final class ProjectileWeaponManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 244;
    private static final int LIST_WIDTH = 296;
    private static final int LIST_HEIGHT = 130;
    private static final int ROW_HEIGHT = 18;
    private static final int VISIBLE_ROWS = LIST_HEIGHT / ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int REMOVE_WIDTH = 16;

    private static String rememberedSearch = "";
    private static int rememberedScroll;

    private final Screen parentScreen;
    private final List<String> filtered = new ArrayList<>();

    private EditBox searchBox;
    private String searchText = rememberedSearch;
    private int scrollOffset = rememberedScroll;
    private int listLeft;
    private int listTop;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public ProjectileWeaponManagerScreen(Screen parentScreen) {
        super(Component.literal("Manual Projectile Weapons"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.listLeft = left + 12;
        this.listTop = top + 52;

        this.searchBox = new EditBox(
                this.font,
                left + 12,
                top + 27,
                LIST_WIDTH,
                18,
                Component.literal("Search Projectile Weapons")
        );
        this.searchBox.setHint(Component.literal("Search added items"));
        this.searchBox.setMaxLength(100);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            this.scrollOffset = 0;
            rebuildFiltered();
        });
        this.addRenderableWidget(this.searchBox);

        Button add = Button.builder(Component.literal("Add Weapons"), button -> openSelector())
                .bounds(left + 12, top + 201, 92, 18)
                .build();
        add.setTooltip(Tooltip.create(Component.literal(
                "Add vanilla or modded items that should be treated as projectile weapons."
        )));
        this.addRenderableWidget(add);

        Button clear = Button.builder(Component.literal("Clear All"), button -> confirmClear())
                .bounds(left + 114, top + 201, 92, 18)
                .build();
        clear.active = !ProjectileWeaponStore.snapshot().isEmpty();
        this.addRenderableWidget(clear);

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> returnToParent())
                .bounds(left + 216, top + 201, 92, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> returnToParent())
                .bounds(left + 12, top + 223, LIST_WIDTH, 18)
                .build());

        rebuildFiltered();
    }

    private void rebuildFiltered() {
        this.filtered.clear();
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        for (String id : ProjectileWeaponStore.snapshot()) {
            String name = getItemName(id);
            if (query.isEmpty()
                    || id.toLowerCase(Locale.ROOT).contains(query)
                    || name.toLowerCase(Locale.ROOT).contains(query)) {
                this.filtered.add(id);
            }
        }
        this.filtered.sort(
                Comparator.comparing((String id) -> getItemName(id).toLowerCase(Locale.ROOT))
                        .thenComparing(id -> id)
        );
        clampScroll();
    }

    private void openSelector() {
        if (this.minecraft == null) {
            return;
        }
        // Reuse the same registry-backed selector as the AOE manager so every
        // registered vanilla/modded item is shown consistently on all loaders.
        this.minecraft.setScreen(new ItemSelectorScreen(
                this,
                ProjectileWeaponStore.snapshot(),
                id -> {
                    ProjectileWeaponStore.add(id);
                    this.searchText = "";
                    this.scrollOffset = 0;
                    rebuildFiltered();
                },
                Component.literal("Select Projectile Weapon"),
                Component.literal("Treat the selected item as a projectile weapon.")
        ));
    }

    private void confirmClear() {
        if (this.minecraft == null || ProjectileWeaponStore.snapshot().isEmpty()) {
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        ProjectileWeaponStore.clear();
                        this.scrollOffset = 0;
                        rebuildFiltered();
                    }
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this);
                    }
                },
                Component.literal("Clear manual projectile weapons?"),
                Component.literal("Built-in projectile weapon recognition will remain active."),
                Component.literal("Clear All"),
                Component.literal("Cancel")
        ));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && isInsideList(mouseX, mouseY)) {
            if (mouseX >= scrollbarLeft()) {
                beginScrollbarDrag(mouseY);
                return true;
            }

            int row = (int) ((mouseY - this.listTop) / ROW_HEIGHT);
            int index = this.scrollOffset + row;
            if (index >= 0 && index < this.filtered.size()) {
                int removeLeft = this.listLeft + LIST_WIDTH - SCROLLBAR_WIDTH - REMOVE_WIDTH;
                if (mouseX >= removeLeft) {
                    ProjectileWeaponStore.remove(this.filtered.get(index));
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && this.draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideList(mouseX, mouseY) && scrollY != 0.0D) {
            this.scrollOffset -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        clampScroll();
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
        // In 1.21.11, Screen.renderWithTooltipAndSubtitles renders background prior to render()
        // setColor removed in 1.21.4
        super.render(graphics, mouseX, mouseY, partialTick);

        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawCenteredString(this.font, "Manual Projectile Weapons", this.width / 2, top + 9, 0xFFFFFFFF);
        renderList(graphics, mouseX, mouseY);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(
                this.listLeft - 2,
                this.listTop - 2,
                this.listLeft + LIST_WIDTH + 2,
                this.listTop + LIST_HEIGHT + 2,
                0xFF666666
        );
        graphics.fill(
                this.listLeft,
                this.listTop,
                this.listLeft + LIST_WIDTH,
                this.listTop + LIST_HEIGHT,
                0xEE171717
        );

        int usableWidth = LIST_WIDTH - SCROLLBAR_WIDTH;
        if (this.filtered.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    ProjectileWeaponStore.snapshot().isEmpty() ? "No manual projectile weapons" : "No matching items",
                    this.listLeft + usableWidth / 2,
                    this.listTop + LIST_HEIGHT / 2 - 4,
                    0xFFBBBBBB
            );
        }

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
                    hovered ? 0xFF383838 : 0xFF282828
            );

            String name = getItemName(id);
            graphics.drawString(
                    this.font,
                    this.font.plainSubstrByWidth(name, usableWidth - REMOVE_WIDTH - 12),
                    this.listLeft + 6,
                    rowY + 5,
                    0xFFFFFFFF,
                    true
            );
            drawPixelX(
                    graphics,
                    this.listLeft + usableWidth - 11,
                    rowY + ROW_HEIGHT / 2,
                    hovered ? 0xFFFF7777 : 0xFFE0E0E0
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

        String count = ProjectileWeaponStore.snapshot().size() + " items";
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

    private static String getItemName(String idString) {
        Identifier id = Identifier.tryParse(idString);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return idString;
        }
        Item item = BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null);
        return new ItemStack(item).getHoverName().getString();
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
}
