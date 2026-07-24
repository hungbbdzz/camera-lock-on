package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LockOnHudRenderer {
    /** Heart-card HUD implementation. This renderer never draws a long health bar. */
    public static final String HUD_RENDERER_REVISION = "auto-lock-box-v7";

    private static final ResourceLocation VANILLA_GUI_ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");

    private enum HudIcon {
        EMPTY_HEART,
        FULL_HEART,
        HALF_HEART,
        BLINKING_EMPTY_HEART,
        BLINKING_FULL_HEART,
        BLINKING_HALF_HEART,
        HALF_ARMOR,
        FULL_ARMOR
    }

    private static final HudIcon EMPTY_HEART = HudIcon.EMPTY_HEART;
    private static final HudIcon FULL_HEART = HudIcon.FULL_HEART;
    private static final HudIcon HALF_HEART = HudIcon.HALF_HEART;
    private static final HudIcon BLINKING_EMPTY_HEART = HudIcon.BLINKING_EMPTY_HEART;
    private static final HudIcon BLINKING_FULL_HEART = HudIcon.BLINKING_FULL_HEART;
    private static final HudIcon BLINKING_HALF_HEART = HudIcon.BLINKING_HALF_HEART;
    private static final HudIcon HALF_ARMOR = HudIcon.HALF_ARMOR;
    private static final HudIcon FULL_ARMOR = HudIcon.FULL_ARMOR;

    private static final int HEART_SIZE = 9;
    private static final int HEART_STEP = 8;
    private static final int HEARTS_PER_ROW = 10;
    /** The lower heart row is almost completely hidden behind the upper row. */
    private static final int HEART_ROW_STEP = 3;
    private static final float INLINE_HEALTH_LIMIT = 40.0F;

    private static final int ARMOR_SIZE = 9;
    private static final int ARMOR_STEP = 8;
    private static final int MAX_INLINE_ARMOR_ICONS = 10;
    private static final int INLINE_ARMOR_LIMIT = MAX_INLINE_ARMOR_ICONS * 2;

    private static int trackedTargetId = Integer.MIN_VALUE;
    private static float previousHealth = -1.0F;
    private static float trailingHealth = -1.0F;
    private static long lastFrameNanos;
    private static long damageHoldUntilNanos;
    private static long damageFlashUntilNanos;

    private LockOnHudRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        if (CameraLockOnConfig.TARGET_HUD.get() && LockOnController.isActive()) {
            renderTargetHud(graphics, minecraft, player);
        }
        if (CameraLockOnConfig.AUTO_LOCK_INDICATOR.get()) {
            renderAutoLockProgress(graphics, minecraft);
        }
        if (CameraLockOnConfig.ATTACKER_INDICATOR.get()) {
            renderAttackerIndicators(graphics, minecraft, player);
        }
        renderTemporaryPin(graphics, minecraft);
    }

    private static void renderTargetHud(
            GuiGraphics graphics,
            Minecraft minecraft,
            LocalPlayer player
    ) {
        LivingEntity target = LockOnController.getLockedTarget();
        if (target == null || target.isRemoved()) {
            return;
        }

        HudOptions options = optionsFromConfig();
        HudData data = createLiveData(player, target);
        HudCardSize unscaledSize = measureCard(minecraft.font, data, options);
        int scaledWidth = Math.max(1, Math.round(unscaledSize.width() * options.scale()));
        int scaledHeight = Math.max(1, Math.round(unscaledSize.height() * options.scale()));

        int[] position = calculateAnchoredPosition(
                graphics.guiWidth(),
                graphics.guiHeight(),
                scaledWidth,
                scaledHeight,
                CameraLockOnConfig.HUD_ANCHOR_X.get(),
                CameraLockOnConfig.HUD_ANCHOR_Y.get()
        );

        updateDamageAnimation(target, data.currentHealth(), data.maximumHealth());

        graphics.pose().pushPose();
        graphics.pose().translate(position[0], position[1], 0.0F);
        graphics.pose().scale(options.scale(), options.scale(), 1.0F);
        drawTargetCard(
                graphics,
                minecraft.font,
                0,
                0,
                data,
                options,
                unscaledSize,
                trailingHealth,
                isDamageFlashVisible()
        );
        graphics.pose().popPose();
    }

    public static HudOptions optionsFromConfig() {
        return new HudOptions(
                CameraLockOnConfig.HUD_SHOW_NAME.get(),
                CameraLockOnConfig.HUD_SHOW_HEALTH.get(),
                CameraLockOnConfig.HUD_SHOW_DISTANCE.get(),
                CameraLockOnConfig.HUD_SHOW_ARMOR.get(),
                CameraLockOnConfig.SHOW_REGISTRY_IDS.get(),
                CameraLockOnConfig.HUD_SHOW_MOD_NAME.get(),
                CameraLockOnConfig.HUD_ANIMATE_DAMAGE.get(),
                (float) Mth.clamp(CameraLockOnConfig.HUD_SCALE.get(), 0.60D, 1.60D),
                (float) Mth.clamp(CameraLockOnConfig.HUD_OPACITY.get(), 0.05D, 1.0D)
        );
    }

    public static HudData previewData() {
        return new HudData(
                "Armored Zombie",
                "minecraft:zombie",
                "Minecraft",
                20.0F,
                20.0F,
                3.1F,
                10,
                false,
                1
        );
    }

    public static HudCardSize measurePreviewCard(Font font, HudOptions options) {
        return measureCard(font, previewData(), options);
    }

    public static void renderPreviewCard(
            GuiGraphics graphics,
            Font font,
            int left,
            int top,
            HudOptions options
    ) {
        HudData data = previewData();
        HudCardSize size = measureCard(font, data, options);

        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        graphics.pose().scale(options.scale(), options.scale(), 1.0F);
        drawTargetCard(
                graphics,
                font,
                0,
                0,
                data,
                options,
                size,
                data.currentHealth(),
                false
        );
        graphics.pose().popPose();
    }

    public static int[] calculateAnchoredPosition(
            int guiWidth,
            int guiHeight,
            int scaledWidth,
            int scaledHeight,
            double anchorX,
            double anchorY
    ) {
        int availableX = Math.max(0, guiWidth - scaledWidth);
        int availableY = Math.max(0, guiHeight - scaledHeight);
        int left = (int) Math.round(Mth.clamp(anchorX, 0.0D, 1.0D) * availableX);
        int top = (int) Math.round(Mth.clamp(anchorY, 0.0D, 1.0D) * availableY);
        return new int[]{left, top};
    }

    private static HudData createLiveData(LocalPlayer player, LivingEntity target) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String registryId = id == null ? "unknown:entity" : id.toString();
        String modName = id == null ? "Unknown" : displayModName(id.getNamespace());
        float current = Math.max(0.0F, target.getHealth() + target.getAbsorptionAmount());
        float maximum = Math.max(1.0F, target.getMaxHealth() + target.getAbsorptionAmount());

        return new HudData(
                target.getName().getString(),
                registryId,
                modName,
                current,
                maximum,
                player.distanceTo(target),
                target.getArmorValue(),
                TargetingRules.isBoss(target.getType()),
                Math.max(1, LockOnController.getGroupTargetCount())
        );
    }

    private static HudCardSize measureCard(Font font, HudData data, HudOptions options) {
        int contentWidth = 0;
        int contentHeight = 0;

        if (options.showName()) {
            contentWidth = Math.max(contentWidth, font.width(decoratedName(data)));
            contentHeight += 10;
        }

        if (options.showRegistryId()) {
            contentWidth = Math.max(contentWidth, font.width(data.registryId()));
            contentHeight += 10;
        }

        if (options.showHealth()) {
            if (data.maximumHealth() <= INLINE_HEALTH_LIMIT) {
                int heartCount = Math.max(1, (int) Math.ceil(data.maximumHealth() / 2.0F));
                int longestRow = Math.min(HEARTS_PER_ROW, heartCount);
                int rows = (heartCount + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
                int heartWidth = iconRowWidth(longestRow, HEART_STEP);

                if (rows == 1 && options.showDistance()) {
                    contentWidth = Math.max(
                            contentWidth,
                            heartWidth + 8 + font.width(formatDistance(data.distance()))
                    );
                    contentHeight += HEART_SIZE;
                } else {
                    contentWidth = Math.max(contentWidth, heartWidth);
                    contentHeight += HEART_SIZE + Math.max(0, rows - 1) * HEART_ROW_STEP;
                    if (options.showDistance()) {
                        contentWidth = Math.max(contentWidth, font.width(formatDistance(data.distance())));
                        contentHeight += 9;
                    }
                }
            } else {
                String healthText = formatHealth(data.currentHealth()) + " / " + formatHealth(data.maximumHealth());
                int rowWidth = HEART_SIZE + 3 + font.width(healthText);
                if (options.showDistance()) {
                    rowWidth += 10 + font.width(formatDistance(data.distance()));
                }
                contentWidth = Math.max(contentWidth, rowWidth);
                contentHeight += HEART_SIZE + 1;
            }
        } else if (options.showDistance()) {
            contentWidth = Math.max(contentWidth, font.width(formatDistance(data.distance())));
            contentHeight += 10;
        }

        if (options.showArmor() && data.armor() > 0) {
            if (data.armor() <= INLINE_ARMOR_LIMIT) {
                int armorIcons = Math.max(1, (data.armor() + 1) / 2);
                contentWidth = Math.max(contentWidth, iconRowWidth(armorIcons, ARMOR_STEP));
            } else {
                contentWidth = Math.max(contentWidth, ARMOR_SIZE + 3 + font.width(Integer.toString(data.armor())));
            }
            contentHeight += ARMOR_SIZE + 1;
        }

        if (options.showModName()) {
            contentWidth = Math.max(contentWidth, font.width(data.modName()));
            contentHeight += 10;
        }

        int width = Mth.clamp(contentWidth + 10, 50, 236);
        int height = Math.max(18, contentHeight + 6);
        return new HudCardSize(width, height);
    }

    private static int iconRowWidth(int iconCount, int step) {
        return iconCount <= 0 ? 0 : (iconCount - 1) * step + 9;
    }

    private static void drawTargetCard(
            GuiGraphics graphics,
            Font font,
            int left,
            int top,
            HudData data,
            HudOptions options,
            HudCardSize size,
            float damageHealth,
            boolean damageFlash
    ) {
        int backgroundAlpha = Mth.clamp(Math.round(options.opacity() * 255.0F), 0, 255);
        int border = 0xB0192530;
        int background = backgroundAlpha << 24;

        graphics.fill(left, top, left + size.width(), top + size.height(), border);
        graphics.fill(
                left + 1,
                top + 1,
                left + size.width() - 1,
                top + size.height() - 1,
                background
        );

        int x = left + 5;
        int y = top + 3;
        int right = left + size.width() - 5;
        int maxTextWidth = Math.max(1, right - x);

        if (options.showName()) {
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(decoratedName(data), maxTextWidth),
                    x,
                    y,
                    0xFFFFFFFF,
                    true
            );
            y += 10;
        }

        if (options.showRegistryId()) {
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(data.registryId(), maxTextWidth),
                    x,
                    y,
                    0xFFAFAFAF,
                    true
            );
            y += 10;
        }

        if (options.showHealth()) {
            if (data.maximumHealth() <= INLINE_HEALTH_LIMIT) {
                drawHeartRows(
                        graphics,
                        x,
                        y,
                        data.currentHealth(),
                        data.maximumHealth(),
                        options.animateDamage() ? damageHealth : data.currentHealth(),
                        options.animateDamage() && damageFlash
                );
                int heartCount = Math.max(1, (int) Math.ceil(data.maximumHealth() / 2.0F));
                int rows = (heartCount + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
                int heartWidth = iconRowWidth(Math.min(HEARTS_PER_ROW, heartCount), HEART_STEP);

                if (rows == 1 && options.showDistance()) {
                    String distanceText = formatDistance(data.distance());
                    graphics.drawString(
                            font,
                            distanceText,
                            Math.max(x + heartWidth + 8, right - font.width(distanceText)),
                            y + 1,
                            0xFFFFFFFF,
                            true
                    );
                    y += HEART_SIZE;
                } else {
                    y += HEART_SIZE + Math.max(0, rows - 1) * HEART_ROW_STEP;
                    if (options.showDistance()) {
                        graphics.drawString(font, formatDistance(data.distance()), x, y, 0xFFFFFFFF, true);
                        y += 9;
                    }
                }
            } else {
                HudIcon container = damageFlash ? BLINKING_EMPTY_HEART : EMPTY_HEART;
                HudIcon full = damageFlash ? BLINKING_FULL_HEART : FULL_HEART;
                drawHeart(graphics, x, y, container);
                drawHeart(graphics, x, y, full);

                String healthText = formatHealth(data.currentHealth()) + " / " + formatHealth(data.maximumHealth());
                graphics.drawString(font, healthText, x + HEART_SIZE + 3, y + 1, 0xFFFFFFFF, true);

                if (options.showDistance()) {
                    String distanceText = formatDistance(data.distance());
                    graphics.drawString(font, distanceText, right - font.width(distanceText), y + 1, 0xFFFFFFFF, true);
                }
                y += HEART_SIZE + 1;
            }
        } else if (options.showDistance()) {
            graphics.drawString(font, formatDistance(data.distance()), x, y, 0xFFFFFFFF, true);
            y += 10;
        }

        if (options.showArmor() && data.armor() > 0) {
            drawArmor(graphics, font, x, y, data.armor());
            y += ARMOR_SIZE + 1;
        }

        if (options.showModName()) {
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(data.modName(), maxTextWidth),
                    x,
                    y,
                    0xFF5753FF,
                    true
            );
        }
    }

    private static void drawHeartRows(
            GuiGraphics graphics,
            int x,
            int y,
            float currentHealth,
            float maximumHealth,
            float damageHealth,
            boolean damageFlash
    ) {
        int heartCount = Math.max(1, (int) Math.ceil(maximumHealth / 2.0F));
        int rowCount = (heartCount + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;

        /* Draw lower rows first so the upper row hides most of them. */
        for (int row = rowCount - 1; row >= 0; row--) {
            int rowStart = row * HEARTS_PER_ROW;
            int rowEnd = Math.min(heartCount, rowStart + HEARTS_PER_ROW);
            for (int heart = rowStart; heart < rowEnd; heart++) {
                int column = heart - rowStart;
                int heartX = x + column * HEART_STEP;
                int heartY = y + row * HEART_ROW_STEP;

                drawHeart(graphics, heartX, heartY, damageFlash ? BLINKING_EMPTY_HEART : EMPTY_HEART);
                drawHealthHeart(graphics, heartX, heartY, currentHealth - heart * 2.0F, damageFlash);

                if (damageFlash && damageHealth > currentHealth) {
                    float oldRemaining = damageHealth - heart * 2.0F;
                    float currentRemaining = currentHealth - heart * 2.0F;
                    if (oldRemaining > currentRemaining) {
                        drawHealthHeart(graphics, heartX, heartY, oldRemaining, false);
                    }
                }
            }
        }
    }

    private static void drawHealthHeart(
            GuiGraphics graphics,
            int x,
            int y,
            float remainingHealth,
            boolean blinking
    ) {
        if (remainingHealth >= 1.5F) {
            drawHeart(graphics, x, y, blinking ? BLINKING_FULL_HEART : FULL_HEART);
        } else if (remainingHealth > 0.0F) {
            drawHeart(graphics, x, y, blinking ? BLINKING_HALF_HEART : HALF_HEART);
        }
    }

    private static void drawArmor(GuiGraphics graphics, Font font, int x, int y, int armorPoints) {
        if (armorPoints <= INLINE_ARMOR_LIMIT) {
            int iconCount = Math.max(1, (armorPoints + 1) / 2);
            for (int icon = 0; icon < iconCount; icon++) {
                int remaining = armorPoints - icon * 2;
                HudIcon sprite = remaining >= 2 ? FULL_ARMOR : HALF_ARMOR;
                drawArmorIcon(graphics, x + icon * ARMOR_STEP, y, sprite);
            }
            return;
        }

        drawArmorIcon(graphics, x, y, FULL_ARMOR);
        graphics.drawString(font, Integer.toString(armorPoints), x + ARMOR_SIZE + 3, y + 1, 0xFFFFFFFF, true);
    }

    private static void drawArmorIcon(GuiGraphics graphics, int x, int y, HudIcon sprite) {
        int u = sprite == HudIcon.FULL_ARMOR ? 34 : 25;
        graphics.blit(VANILLA_GUI_ICONS, x, y, u, 9, ARMOR_SIZE, ARMOR_SIZE);
    }

    private static void drawHeart(GuiGraphics graphics, int x, int y, HudIcon sprite) {
        int u = switch (sprite) {
            case EMPTY_HEART -> 16;
            case BLINKING_EMPTY_HEART -> 25;
            case FULL_HEART -> 52;
            case BLINKING_FULL_HEART -> 70;
            case HALF_HEART -> 61;
            case BLINKING_HALF_HEART -> 79;
            default -> 16;
        };
        graphics.blit(VANILLA_GUI_ICONS, x, y, u, 0, HEART_SIZE, HEART_SIZE);
    }

    private static void updateDamageAnimation(
            LivingEntity target,
            float currentHealth,
            float maximumHealth
    ) {
        long now = System.nanoTime();
        if (trackedTargetId != target.getId()) {
            trackedTargetId = target.getId();
            previousHealth = currentHealth;
            trailingHealth = currentHealth;
            damageHoldUntilNanos = 0L;
            damageFlashUntilNanos = 0L;
            lastFrameNanos = now;
            return;
        }

        if (previousHealth >= 0.0F && currentHealth < previousHealth - 0.001F) {
            trailingHealth = Math.max(trailingHealth, previousHealth);
            damageHoldUntilNanos = now + 260_000_000L;
            damageFlashUntilNanos = now + 180_000_000L;
        } else if (currentHealth > trailingHealth) {
            trailingHealth = currentHealth;
        }
        previousHealth = currentHealth;

        double deltaSeconds = lastFrameNanos == 0L
                ? 0.0D
                : Math.min(0.10D, Math.max(0.0D, (now - lastFrameNanos) / 1_000_000_000.0D));
        lastFrameNanos = now;

        if (now > damageHoldUntilNanos && trailingHealth > currentHealth) {
            float fallSpeed = Math.max(5.0F, maximumHealth * 0.85F);
            trailingHealth = Math.max(currentHealth, trailingHealth - fallSpeed * (float) deltaSeconds);
        }
    }

    private static boolean isDamageFlashVisible() {
        return System.nanoTime() < damageFlashUntilNanos;
    }

    private static String decoratedName(HudData data) {
        String name = data.name();
        if (data.groupCount() > 1) {
            name += "  ×" + data.groupCount();
        }
        if (data.boss()) {
            name += "  ★";
        }
        return name;
    }

    private static String formatDistance(float distance) {
        return String.format(Locale.ROOT, "%.1f m", distance);
    }

    private static String formatHealth(float health) {
        if (Math.abs(health - Math.round(health)) < 0.05F) {
            return Integer.toString(Math.round(health));
        }
        return String.format(Locale.ROOT, "%.1f", health);
    }

    private static String displayModName(String namespace) {
        if ("minecraft".equals(namespace)) {
            return "Minecraft";
        }

        String[] words = namespace.split("[_\\-.]");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.isEmpty() ? namespace : result.toString();
    }

    private static void renderAutoLockProgress(GuiGraphics graphics, Minecraft minecraft) {
        LivingEntity candidate = LockOnController.getAutoLockCandidate();
        float progress = LockOnController.getAutoLockProgress();
        if (candidate == null || progress <= 0.0F || LockOnController.isActive()) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        /*
         * One visual is enough: four pixel corners contract toward the
         * crosshair. The old pip/progress row and candidate-name label were
         * intentionally removed to keep acquisition quiet and unobtrusive.
         */
        int radius = 15 - Math.round(Mth.clamp(progress, 0.0F, 1.0F) * 9.0F);
        int arm = progress >= 0.999F ? 5 : 4;
        int color = autoLockIndicatorColor(progress);

        drawBracket(graphics, centerX - radius, centerY - radius, arm, 1, 1, color);
        drawBracket(graphics, centerX + radius, centerY - radius, arm, -1, 1, color);
        drawBracket(graphics, centerX - radius, centerY + radius, arm, 1, -1, color);
        drawBracket(graphics, centerX + radius, centerY + radius, arm, -1, -1, color);
    }

    /**
     * Auto Lock uses the existing reticle color choice. This avoids adding a
     * second redundant color setting while keeping the HUD visually coherent.
     */
    private static int autoLockIndicatorColor(float progress) {
        String configured = CameraLockOnConfig.RETICLE_COLOR.get();
        int rgb;
        if ("Red".equalsIgnoreCase(configured)) {
            rgb = 0xFF493F;
        } else if ("Green".equalsIgnoreCase(configured)) {
            rgb = 0x59E66A;
        } else if ("Yellow".equalsIgnoreCase(configured)) {
            rgb = 0xFFE15A;
        } else if ("Purple".equalsIgnoreCase(configured)) {
            rgb = 0xD76BFF;
        } else {
            rgb = 0x63E8F2;
        }

        /*
         * A completed acquisition flashes white for its final frame, making
         * the lock confirmation readable against every background.
         */
        if (progress >= 0.999F) {
            rgb = 0xFFFFFF;
        }

        return 0xFF000000 | rgb;
    }

    private static void drawBracket(
            GuiGraphics graphics,
            int x,
            int y,
            int arm,
            int horizontalDirection,
            int verticalDirection,
            int color
    ) {
        int horizontalEnd = x + arm * horizontalDirection;
        int verticalEnd = y + arm * verticalDirection;
        graphics.fill(
                Math.min(x, horizontalEnd),
                y,
                Math.max(x, horizontalEnd) + 1,
                y + 1,
                color
        );
        graphics.fill(
                x,
                Math.min(y, verticalEnd),
                x + 1,
                Math.max(y, verticalEnd) + 1,
                color
        );
    }

    private static void renderAttackerIndicators(
            GuiGraphics graphics,
            Minecraft minecraft,
            LocalPlayer player
    ) {
        List<CombatAwareness.ThreatSnapshot> snapshots = CombatAwareness.getThreatSnapshots(player);
        if (snapshots.isEmpty()) {
            return;
        }

        int maximum = Math.min(CameraLockOnConfig.MAX_ATTACKER_INDICATORS.get(), snapshots.size());
        List<SideThreat> sideThreats = new ArrayList<>();
        for (int i = 0; i < maximum; i++) {
            CombatAwareness.ThreatSnapshot snapshot = snapshots.get(i);
            LivingEntity attacker = snapshot.entity();
            if (attacker == null || attacker == LockOnController.getLockedTarget()) {
                continue;
            }

            double deltaX = attacker.getX() - player.getX();
            double deltaZ = attacker.getZ() - player.getZ();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
            float relativeYaw = Mth.wrapDegrees(targetYaw - player.getYRot());
            if (Math.abs(relativeYaw) <= 52.0F) {
                continue;
            }
            sideThreats.add(new SideThreat(relativeYaw < 0.0F ? Side.LEFT : Side.RIGHT, snapshot));
        }

        if (sideThreats.isEmpty()) {
            return;
        }
        if (CameraLockOnConfig.GROUP_ATTACKER_DIRECTIONS.get()) {
            renderGroupedSide(graphics, minecraft, player, sideThreats, Side.LEFT);
            renderGroupedSide(graphics, minecraft, player, sideThreats, Side.RIGHT);
        } else {
            renderSeparateSides(graphics, minecraft, player, sideThreats);
        }
    }

    private static void renderGroupedSide(
            GuiGraphics graphics,
            Minecraft minecraft,
            LocalPlayer player,
            List<SideThreat> threats,
            Side side
    ) {
        SideThreat strongest = null;
        int count = 0;
        for (SideThreat threat : threats) {
            if (threat.side() != side) {
                continue;
            }
            count++;
            if (strongest == null || threat.snapshot().score() > strongest.snapshot().score()) {
                strongest = threat;
            }
        }
        if (strongest != null) {
            drawSideIndicator(
                    graphics,
                    minecraft.font,
                    buildThreatText(player, strongest.snapshot(), count),
                    side,
                    0
            );
        }
    }

    private static void renderSeparateSides(
            GuiGraphics graphics,
            Minecraft minecraft,
            LocalPlayer player,
            List<SideThreat> threats
    ) {
        int leftIndex = 0;
        int rightIndex = 0;
        for (SideThreat threat : threats) {
            int index = threat.side() == Side.LEFT ? leftIndex++ : rightIndex++;
            drawSideIndicator(
                    graphics,
                    minecraft.font,
                    buildThreatText(player, threat.snapshot(), 1),
                    threat.side(),
                    index
            );
        }
    }

    private static String buildThreatText(
            LocalPlayer player,
            CombatAwareness.ThreatSnapshot snapshot,
            int groupedCount
    ) {
        String text = snapshot.entity().getName().getString()
                + " "
                + String.format(Locale.ROOT, "%.0fm", player.distanceTo(snapshot.entity()));
        if (groupedCount > 1) {
            text += " ×" + groupedCount;
        }
        if (snapshot.hitCount() > 1) {
            text += " [" + snapshot.hitCount() + "]";
        }
        return text;
    }

    private static void drawSideIndicator(
            GuiGraphics graphics,
            Font font,
            String text,
            Side side,
            int row
    ) {
        int y = graphics.guiHeight() / 2 - 9 + row * 16;
        int width = Math.min(130, font.width(text) + 20);
        if (side == Side.LEFT) {
            int left = 6;
            graphics.fill(left, y, left + width, y + 14, 0xC014191D);
            graphics.fill(left, y, left + 2, y + 14, 0xFFFFC857);
            graphics.drawString(font, "<", left + 5, y + 3, 0xFFFFD66B, true);
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(text, width - 20),
                    left + 14,
                    y + 3,
                    0xFFFFFFFF,
                    true
            );
        } else {
            int left = graphics.guiWidth() - width - 6;
            graphics.fill(left, y, left + width, y + 14, 0xC014191D);
            graphics.fill(left + width - 2, y, left + width, y + 14, 0xFFFFC857);
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(text, width - 20),
                    left + 5,
                    y + 3,
                    0xFFFFFFFF,
                    true
            );
            graphics.drawString(font, ">", left + width - 11, y + 3, 0xFFFFD66B, true);
        }
    }

    private static void renderTemporaryPin(GuiGraphics graphics, Minecraft minecraft) {
        if (LockOnController.getTemporaryPinnedType() == null) {
            return;
        }
        String text = "Pinned: " + LockOnController.getTemporaryPinnedType().getDescription().getString();
        int width = minecraft.font.width(text) + 12;
        graphics.fill(6, 6, 6 + width, 21, 0xC014191D);
        graphics.fill(6, 6, 8, 21, 0xFF69D9E8);
        graphics.drawString(minecraft.font, text, 12, 10, 0xFFE8FAFF, true);
    }

    public record HudOptions(
            boolean showName,
            boolean showHealth,
            boolean showDistance,
            boolean showArmor,
            boolean showRegistryId,
            boolean showModName,
            boolean animateDamage,
            float scale,
            float opacity
    ) {
    }

    public record HudData(
            String name,
            String registryId,
            String modName,
            float currentHealth,
            float maximumHealth,
            float distance,
            int armor,
            boolean boss,
            int groupCount
    ) {
    }

    public record HudCardSize(int width, int height) {
    }

    private enum Side {
        LEFT,
        RIGHT
    }

    private record SideThreat(Side side, CombatAwareness.ThreatSnapshot snapshot) {
    }
}
