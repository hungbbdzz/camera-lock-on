package com.velorise.cameralockon;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

import java.util.Locale;

/**
 * Direct per-entity aim editor. The X is positioned over a visual hitbox around
 * the selected model. Left-drag moves the aim point; right-drag rotates the
 * preview freely. A saved per-entity override always has priority over the
 * global preset. The cross is rendered as a top-layer overlay.
 */
public final class EntityAimPointScreen extends Screen {
    private static final int PANEL_WIDTH = 326;
    private static final int PANEL_HEIGHT = 266;
    private static final int PREVIEW_WIDTH = 210;
    private static final int PREVIEW_HEIGHT = 142;

    private final Screen parentScreen;
    private final String entityId;
    private final Runnable onChanged;

    private double aimX;
    private double aimY;
    private boolean allowGroupAimOffset;
    private LivingEntity previewEntity;

    private int previewLeft;
    private int previewTop;
    private boolean draggingPoint;
    private boolean draggingModel;
    private double lastMouseX;
    private double lastMouseY;
    private float previewYaw;
    private float previewPitch;
    private long manualRotationUntilMillis;
    private long lastPreviewFrameMillis;

    public EntityAimPointScreen(Screen parentScreen, String entityId, Runnable onChanged) {
        super(Component.literal("Entity Aim Point"));
        this.parentScreen = parentScreen;
        this.entityId = entityId == null ? "" : entityId;
        this.onChanged = onChanged == null ? () -> { } : onChanged;

        EntityAimPointStore.Entry entry = EntityAimPointStore.get(this.entityId);
        if (entry != null) {
            this.aimX = entry.horizontal();
            this.aimY = entry.vertical();
            this.allowGroupAimOffset = entry.allowGroupAimOffset();
        } else {
            CameraLockOnConfig.AimPreset preset = CameraLockOnConfig.AimPreset.fromConfig(
                    CameraLockOnConfig.AIM_PRESET.get()
            );
            this.aimX = preset == CameraLockOnConfig.AimPreset.CUSTOM
                    ? CameraLockOnConfig.AIM_POINT_X.get()
                    : preset.getX();
            this.aimY = preset == CameraLockOnConfig.AimPreset.CUSTOM
                    ? CameraLockOnConfig.AIM_POINT_Y.get()
                    : preset.getY();
            this.allowGroupAimOffset = false;
        }
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.previewLeft = left + (PANEL_WIDTH - PREVIEW_WIDTH) / 2;
        this.previewTop = top + 58;
        this.previewEntity = createPreviewEntity();
        this.lastPreviewFrameMillis = Util.getMillis();

        Button groupOffset = Button.builder(
                        Component.literal("Allow Group Aim Offset: " + onOff(this.allowGroupAimOffset)),
                        button -> {
                            this.allowGroupAimOffset = !this.allowGroupAimOffset;
                            rebuildWidgets();
                        }
                )
                .bounds(left + 45, top + 207, 236, 18)
                .build();
        groupOffset.setTooltip(Tooltip.create(Component.literal(
                "OFF keeps this exact entity aim point. ON lets Sweep Assist shift it toward a nearby group."
        )));
        this.addRenderableWidget(groupOffset);

        this.addRenderableWidget(Button.builder(Component.literal("Save Override"), button -> {
                    EntityAimPointStore.put(
                            this.entityId,
                            this.aimX,
                            this.aimY,
                            this.allowGroupAimOffset
                    );
                    this.onChanged.run();
                    returnToParent();
                })
                .bounds(left + 8, top + 236, 100, 18)
                .build());

        Button global = Button.builder(Component.literal("Use Global"), button -> {
                    EntityAimPointStore.remove(this.entityId);
                    this.onChanged.run();
                    returnToParent();
                })
                .bounds(left + 113, top + 236, 100, 18)
                .build();
        global.setTooltip(Tooltip.create(Component.literal(
                "Remove this override so the entity uses the global preset or global custom point."
        )));
        this.addRenderableWidget(global);

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(left + 218, top + 236, 100, 18)
                .build());
    }

    private LivingEntity createPreviewEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(this.entityId);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == EntityType.PLAYER) {
            return null;
        }
        try {
            Entity entity = type.create(this.minecraft.level);
            return entity instanceof LivingEntity living ? living : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInsidePreview(mouseX, mouseY)) {
            if (button == 0) {
                this.draggingPoint = true;
                updateAimPoint(mouseX, mouseY);
                return true;
            }
            if (button == 1) {
                this.draggingModel = true;
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingPoint) {
            updateAimPoint(mouseX, mouseY);
            return true;
        }
        if (button == 1 && this.draggingModel) {
            this.previewYaw += (float) (mouseX - this.lastMouseX) * 0.85F;
            this.previewPitch = 0.0F;
            this.manualRotationUntilMillis = Util.getMillis() + 4500L;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingPoint) {
            updateAimPoint(mouseX, mouseY);
            this.draggingPoint = false;
            return true;
        }
        if (button == 1 && this.draggingModel) {
            this.draggingModel = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= this.previewLeft
                && mouseX <= this.previewLeft + PREVIEW_WIDTH
                && mouseY >= this.previewTop
                && mouseY <= this.previewTop + PREVIEW_HEIGHT;
    }

    private void updateAimPoint(double mouseX, double mouseY) {
        double nx = Mth.clamp((mouseX - this.previewLeft) / PREVIEW_WIDTH, 0.0D, 1.0D);
        double ny = Mth.clamp((mouseY - this.previewTop) / PREVIEW_HEIGHT, 0.0D, 1.0D);
        this.aimX = nx * 2.0D - 1.0D;
        this.aimY = 1.0D - ny;
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
        String name = TargetingRules.getEntityDisplayName(this.entityId);
        graphics.drawCenteredString(this.font, "Aim Point: " + name, this.width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawCenteredString(
                this.font,
                this.font.plainSubstrByWidth(this.entityId, PANEL_WIDTH - 28),
                this.width / 2,
                top + 23,
                0xFFB5B5B5
        );
        graphics.drawCenteredString(
                this.font,
                "Left-drag X   •   Right-drag model",
                this.width / 2,
                top + 34,
                0xFFD0D0D0
        );
        graphics.drawCenteredString(
                this.font,
                "This entity override takes priority over the global aim point",
                this.width / 2,
                top + 44,
                0xFF9FDCE6
        );

        drawPreview(graphics);
        renderPreviewEntity(graphics);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        drawAimCross(graphics);
        graphics.pose().popPose();

        String coordinates = String.format(
                Locale.ROOT,
                "Horizontal %.2f   •   Vertical %.2f",
                this.aimX,
                this.aimY
        );
        graphics.drawCenteredString(this.font, coordinates, this.width / 2, top + 183, 0xFFFFFFFF);
    }

    private void drawPreview(GuiGraphics graphics) {
        int right = this.previewLeft + PREVIEW_WIDTH;
        int bottom = this.previewTop + PREVIEW_HEIGHT;
        graphics.fill(this.previewLeft - 2, this.previewTop - 2, right + 2, bottom + 2, 0xFF777777);
        graphics.fill(this.previewLeft, this.previewTop, right, bottom, 0xFF202020);
        graphics.fill(this.previewLeft + 7, this.previewTop + 7, right - 7, bottom - 7, 0xFF2A2A2A);

        int centerX = this.previewLeft + PREVIEW_WIDTH / 2;
        int centerY = this.previewTop + PREVIEW_HEIGHT / 2;
        graphics.fill(centerX, this.previewTop + 7, centerX + 1, bottom - 7, 0xFF494949);
        graphics.fill(this.previewLeft + 7, centerY, right - 7, centerY + 1, 0xFF494949);
    }

    private void renderPreviewEntity(GuiGraphics graphics) {
        if (this.previewEntity == null) {
            graphics.drawCenteredString(
                    this.font,
                    "Preview unavailable",
                    this.previewLeft + PREVIEW_WIDTH / 2,
                    this.previewTop + PREVIEW_HEIGHT / 2 - 4,
                    0xFFFFA0A0
            );
            return;
        }

        long now = Util.getMillis();
        if (this.lastPreviewFrameMillis == 0L) {
            this.lastPreviewFrameMillis = now;
        }
        long elapsedMillis = Math.min(100L, Math.max(0L, now - this.lastPreviewFrameMillis));
        this.lastPreviewFrameMillis = now;
        if (!this.draggingModel && now >= this.manualRotationUntilMillis) {
            this.previewYaw += elapsedMillis * 0.030F;
        }
        this.previewPitch = 0.0F;

        try {
            float maxDimension = Math.max(this.previewEntity.getBbWidth(), this.previewEntity.getBbHeight());
            int scale = maxDimension <= 0.0F
                    ? 34
                    : Math.max(10, Math.min(42, (int) (58.0F / maxDimension)));

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
                    this.previewTop + PREVIEW_HEIGHT / 2 - 4,
                    0xFFFFA0A0
            );
        }
    }

    private void drawAimCross(GuiGraphics graphics) {
        int x = this.previewLeft
                + (int) Math.round((this.aimX + 1.0D) * 0.5D * PREVIEW_WIDTH);
        int y = this.previewTop
                + (int) Math.round((1.0D - this.aimY) * PREVIEW_HEIGHT);
        x = Mth.clamp(x, this.previewLeft, this.previewLeft + PREVIEW_WIDTH);
        y = Mth.clamp(y, this.previewTop, this.previewTop + PREVIEW_HEIGHT);
        AimPointConfigScreen.drawCross(graphics, x, y);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
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
