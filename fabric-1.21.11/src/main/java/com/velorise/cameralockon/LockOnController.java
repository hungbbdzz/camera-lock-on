package com.velorise.cameralockon;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public final class LockOnController {
    private static final double VIEW_CONE_DOT_THRESHOLD = 0.7D;

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("camera_lockon", "general")
    );

    public static final KeyMapping LOCK_ON_KEY = new KeyMapping(
            "key.camera_lockon.lock_on",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            CATEGORY
    );

    public static final KeyMapping SWITCH_TARGET_KEY = new KeyMapping(
            "key.camera_lockon.switch_target",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            CATEGORY
    );

    public static final KeyMapping SWITCH_TARGET_PREVIOUS_KEY = new KeyMapping(
            "key.camera_lockon.switch_target_previous",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping PIN_TARGET_TYPE_KEY = new KeyMapping(
            "key.camera_lockon.pin_target_type",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.camera_lockon.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_AUTO_LOCK_KEY = new KeyMapping(
            "key.camera_lockon.toggle_auto_lock",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_SMART_LOCK_KEY = new KeyMapping(
            "key.camera_lockon.toggle_smart_lock",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_HOSTILE_ONLY_KEY = new KeyMapping(
            "key.camera_lockon.toggle_hostile_only",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.camera_lockon.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CYCLE_TARGET_PRIORITY_KEY = new KeyMapping(
            "key.camera_lockon.cycle_priority",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CYCLE_SWITCH_MODE_KEY = new KeyMapping(
            "key.camera_lockon.cycle_switch_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CLEAR_PIN_KEY = new KeyMapping(
            "key.camera_lockon.clear_pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CYCLE_CAMERA_POSITION_KEY = new KeyMapping(
            "key.camera_lockon.cycle_camera_position",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );


    public static final KeyMapping TOGGLE_AUTO_RELEASE_BOW_KEY = new KeyMapping(
            "key.camera_lockon.toggle_auto_release_bow",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_AUTO_RECHARGE_BOW_KEY = new KeyMapping(
            "key.camera_lockon.toggle_auto_recharge_bow",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CYCLE_PROJECTILE_ASSIST_KEY = new KeyMapping(
            "key.camera_lockon.cycle_projectile_assist",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CAMERA_X_DECREASE_KEY = new KeyMapping(
            "key.camera_lockon.camera_x_decrease",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CAMERA_X_INCREASE_KEY = new KeyMapping(
            "key.camera_lockon.camera_x_increase",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CAMERA_Y_DECREASE_KEY = new KeyMapping(
            "key.camera_lockon.camera_y_decrease",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CAMERA_Y_INCREASE_KEY = new KeyMapping(
            "key.camera_lockon.camera_y_increase",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CAMERA_Z_DECREASE_KEY = new KeyMapping(
            "key.camera_lockon.camera_z_decrease",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CAMERA_Z_INCREASE_KEY = new KeyMapping(
            "key.camera_lockon.camera_z_increase",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    private static LivingEntity lockedTarget;
    private static boolean active;
    private static EntityType<?> temporaryPinnedType;

    private static float lastLockedYaw;
    private static float lastLockedPitch;
    private static boolean wasLockedLastFrame;
    private static long lastMouseInputTime;
    private static long lastSteeringNanos;
    /* Smoothly restores contextual player aim after manual free-look or a new target. */
    private static double contextualReturnProgress = 1.0D;
    private static boolean contextualReturnPending;
    private static int lostLineOfSightTicks;

    private static LivingEntity autoLockCandidate;
    private static long autoLockCandidateStartTime;
    private static long autoLockBlockedUntil;

    private static Vec3 lastEffectiveAimPoint = Vec3.ZERO;
    private static int groupTargetCount = 1;

    private static int pendingRawLockKeyPresses;
    private static boolean lockMouseWasDown;
    private static boolean lockMousePressedThisTick;

    private static float deadZoneSteeringBlend;
    private static boolean deadZoneOutside;
    private static boolean deadZoneWasEnabled;

    private LockOnController() {
    }

    /** Called by the Fabric mouse hook when the configured lock key is a mouse button. */
    public static void queueRawLockKeyPress() {
        pendingRawLockKeyPresses++;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        if (player == null || level == null) {
            clearAllState();
            CombatAwareness.clear();
            return;
        }

        CombatAwareness.tick(player, level);
        pollConfiguredLockMouseButton(minecraft);

        while (OPEN_CONFIG_KEY.consumeClick()) {
            minecraft.setScreen(new LockOnConfigScreen(minecraft.screen));
            resetAutoLockCandidate();
            return;
        }

        if (minecraft.screen != null) {
            resetAutoLockCandidate();
            return;
        }

        ThirdPersonCameraController.tick();
        handleLockKey(player, level);
        handleSwitchTargetKey(player, level);
        handleTemporaryPinKey();
        handleQuickToggleKeys();
        updateCurrentTarget(player, level);
        AutoBowReleaseController.tick(minecraft, player);
        AdaptiveAimCalibration.tick(minecraft, player);
        updateAutoLock(player, level);
    }

    public static void onEntityAttacked(LivingEntity target) {
        CameraLockOnConfig.LockOnHitMode mode =
                CameraLockOnConfig.LockOnHitMode.fromConfig(
                        CameraLockOnConfig.LOCK_ON_HIT_MODE.get()
                );
        if (mode == CameraLockOnConfig.LockOnHitMode.OFF) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null
                || target == null) {
            return;
        }

        if (mode == CameraLockOnConfig.LockOnHitMode.WHEN_UNLOCKED && active) {
            return;
        }

        requestLock(
                target,
                LockReason.HIT_TARGET,
                mode == CameraLockOnConfig.LockOnHitMode.ALWAYS_SWITCH
        );
    }

    private static void handleLockKey(LocalPlayer player, ClientLevel level) {
        int mappedClicks = 0;
        while (LOCK_ON_KEY.consumeClick()) {
            mappedClicks++;
        }

        int rawMouseClicks = pendingRawLockKeyPresses;
        pendingRawLockKeyPresses = 0;

        int polledMouseClicks = lockMousePressedThisTick ? 1 : 0;
        lockMousePressedThisTick = false;

        // Fabric may not dispatch a normal KeyMapping click when the same mouse
        // button is also used by vanilla Pick Block. Accept any of the three
        // input paths, but count one physical press only once.
        int clicks = Math.max(mappedClicks, Math.max(rawMouseClicks, polledMouseClicks));
        for (int i = 0; i < clicks; i++) {
            if (active) {
                unlockTarget(true, LockReason.MANUAL);
                continue;
            }

            LivingEntity target = findTarget(player, level, null);
            if (target != null) {
                lockTarget(target, LockReason.MANUAL);
            }
        }
    }

    /**
     * Polls the mouse button currently assigned to Lock-On.
     *
     * This is a fallback independent of KeyMapping's click counter and of the
     * MouseHandler mixin, so Middle Mouse Button still works when it conflicts
     * with Minecraft's Pick Block binding.
     */
        private static void pollConfiguredLockMouseButton(Minecraft minecraft) {
        boolean down = false;
        long window = minecraft.getWindow().handle();

        for (int button = GLFW.GLFW_MOUSE_BUTTON_1;
             button <= GLFW.GLFW_MOUSE_BUTTON_LAST;
             button++) {
            net.minecraft.client.input.MouseButtonEvent event = new net.minecraft.client.input.MouseButtonEvent(
                    0, 0, new net.minecraft.client.input.MouseButtonInfo(button, 0)
            );
            if (!LOCK_ON_KEY.matchesMouse(event)) {
                continue;
            }

            down = GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
            break;
        }

        lockMousePressedThisTick = down && !lockMouseWasDown;
        lockMouseWasDown = down;
    }

    private static void handleSwitchTargetKey(LocalPlayer player, ClientLevel level) {
        while (SWITCH_TARGET_KEY.consumeClick()) {
            if (!active || lockedTarget == null) {
                continue;
            }

            LivingEntity target = findSwitchTarget(player, level, lockedTarget);
            if (target != null) {
                lockTarget(target, LockReason.SWITCH);
            }
        }
    }

    /** Applies the Retarget Rule (Any / Same Type First / Same Type Only) when manually switching via Tab. */
    private static LivingEntity findSwitchTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity current
    ) {
        CameraLockOnConfig.RetargetMode mode =
                CameraLockOnConfig.RetargetMode.fromConfig(CameraLockOnConfig.RETARGET_MODE.get());
        if (mode == CameraLockOnConfig.RetargetMode.ANY) {
            return findTarget(player, level, current);
        }

        LivingEntity sameType = findTarget(player, level, current, current.getType());
        if (sameType != null) {
            return sameType;
        }

        return mode == CameraLockOnConfig.RetargetMode.SAME_TYPE_FIRST
                ? findTarget(player, level, current)
                : null;
    }

    private static void handleTemporaryPinKey() {
        while (PIN_TARGET_TYPE_KEY.consumeClick()) {
            if (lockedTarget == null) {
                if (temporaryPinnedType != null) {
                    temporaryPinnedType = null;
                    playFeedbackSound(0.65F);
                }
                continue;
            }

            EntityType<?> targetType = lockedTarget.getType();
            if (temporaryPinnedType == targetType) {
                temporaryPinnedType = null;
            } else {
                temporaryPinnedType = targetType;
            }
            playFeedbackSound(1.35F);
        }
    }

    private static void handleQuickToggleKeys() {
        while (TOGGLE_AUTO_LOCK_KEY.consumeClick()) {
            boolean value = !CameraLockOnConfig.AUTO_LOCK.get();
            CameraLockOnConfig.AUTO_LOCK.set(value);
            CameraLockOnConfig.CLIENT_SPEC.save();
            playFeedbackSound(value ? 1.2F : 0.6F);
        }
        while (TOGGLE_SMART_LOCK_KEY.consumeClick()) {
            boolean value = !CameraLockOnConfig.SMART_LOCK.get();
            CameraLockOnConfig.SMART_LOCK.set(value);
            CameraLockOnConfig.CLIENT_SPEC.save();
            playFeedbackSound(value ? 1.2F : 0.6F);
        }
        while (TOGGLE_HOSTILE_ONLY_KEY.consumeClick()) {
            boolean value = !CameraLockOnConfig.HOSTILE_ONLY.get();
            CameraLockOnConfig.HOSTILE_ONLY.set(value);
            CameraLockOnConfig.CLIENT_SPEC.save();
            playFeedbackSound(value ? 1.2F : 0.6F);
        }
        while (TOGGLE_HUD_KEY.consumeClick()) {
            boolean value = !CameraLockOnConfig.TARGET_HUD.get();
            CameraLockOnConfig.TARGET_HUD.set(value);
            CameraLockOnConfig.CLIENT_SPEC.save();
            playFeedbackSound(value ? 1.2F : 0.6F);
        }
        while (CYCLE_TARGET_PRIORITY_KEY.consumeClick()) {
            ClientFeatureStore.TargetPriority next = ClientFeatureStore.getTargetPriority().next();
            ClientFeatureStore.setTargetPriority(next);
            playFeedbackSound(0.9F);
        }
        while (CYCLE_SWITCH_MODE_KEY.consumeClick()) {
            ClientFeatureStore.SwitchTargetMode next = ClientFeatureStore.getSwitchTargetMode().next();
            ClientFeatureStore.setSwitchTargetMode(next);
            playFeedbackSound(0.9F);
        }

        while (TOGGLE_AUTO_RELEASE_BOW_KEY.consumeClick()) {
            boolean value = !CameraLockOnConfig.AUTO_RELEASE_BOW.get();
            CameraLockOnConfig.AUTO_RELEASE_BOW.set(value);
            CameraLockOnConfig.CLIENT_SPEC.save();
            showQuickSetting("message.camera_lockon.auto_release_bow", value ? "ON" : "OFF");
            playFeedbackSound(value ? 1.2F : 0.6F);
        }
        while (TOGGLE_AUTO_RECHARGE_BOW_KEY.consumeClick()) {
            boolean value = !CameraLockOnConfig.AUTO_RECHARGE_BOW.get();
            CameraLockOnConfig.AUTO_RECHARGE_BOW.set(value);
            CameraLockOnConfig.CLIENT_SPEC.save();
            showQuickSetting("message.camera_lockon.auto_recharge_bow", value ? "ON" : "OFF");
            playFeedbackSound(value ? 1.2F : 0.6F);
        }
        while (CYCLE_PROJECTILE_ASSIST_KEY.consumeClick()) {
            CameraLockOnConfig.ProjectileAssistMode next = CameraLockOnConfig.ProjectileAssistMode
                    .fromConfig(CameraLockOnConfig.PROJECTILE_ASSIST_MODE.get()).next();
            CameraLockOnConfig.PROJECTILE_ASSIST_MODE.set(next.name());
            CameraLockOnConfig.CLIENT_SPEC.save();
            showQuickSetting("message.camera_lockon.projectile_assist", next.getDisplayName().getString());
            playFeedbackSound(0.9F);
        }
        adjustCameraAxis(CAMERA_X_DECREASE_KEY, -0.10D, 0.0D, 0.0D);
        adjustCameraAxis(CAMERA_X_INCREASE_KEY, 0.10D, 0.0D, 0.0D);
        adjustCameraAxis(CAMERA_Y_DECREASE_KEY, 0.0D, -0.10D, 0.0D);
        adjustCameraAxis(CAMERA_Y_INCREASE_KEY, 0.0D, 0.10D, 0.0D);
        adjustCameraAxis(CAMERA_Z_DECREASE_KEY, 0.0D, 0.0D, -0.10D);
        adjustCameraAxis(CAMERA_Z_INCREASE_KEY, 0.0D, 0.0D, 0.10D);

        while (CLEAR_PIN_KEY.consumeClick()) {
            if (temporaryPinnedType != null) {
                temporaryPinnedType = null;
                playFeedbackSound(0.65F);
            }
        }
    }



    private static void adjustCameraAxis(KeyMapping key, double dx, double dy, double dz) {
        while (key.consumeClick()) {
            ClientFeatureStore.CameraSlot slot = ClientFeatureStore.getActiveCameraSlot();
            ClientFeatureStore.CameraPosition current = ClientFeatureStore.getCameraPosition(slot);
            double x = Mth.clamp(current.horizontalOffset() + dx, -3.0D, 3.0D);
            double y = Mth.clamp(current.verticalOffset() + dy, -0.5D, 1.5D);
            double z = Mth.clamp(current.distanceOffset() + dz, -2.0D, 2.0D);
            ClientFeatureStore.setCameraPosition(slot, new ClientFeatureStore.CameraPosition(x, y, z));
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(
                        "message.camera_lockon.camera_xyz", x, y, z), true);
            }
            playFeedbackSound(1.0F);
        }
    }

    private static void showQuickSetting(String translationKey, String value) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey, value), true);
        }
    }

    private static void updateCurrentTarget(LocalPlayer player, ClientLevel level) {
        if (!active || lockedTarget == null) {
            return;
        }

        if (!lockedTarget.isAlive()) {
            retargetOrUnlock(player, level, lockedTarget, TargetLossReason.DEAD);
            return;
        }
        if (lockedTarget.isRemoved()) {
            retargetOrUnlock(player, level, lockedTarget, TargetLossReason.REMOVED);
            return;
        }
        if (lockedTarget.level() != level) {
            retargetOrUnlock(player, level, lockedTarget, TargetLossReason.LEVEL_CHANGED);
            return;
        }
        if (player.distanceTo(lockedTarget) > CameraLockOnConfig.LOCK_ON_RANGE.get()) {
            retargetOrUnlock(player, level, lockedTarget, TargetLossReason.OUT_OF_RANGE);
            return;
        }

        if (!player.hasLineOfSight(lockedTarget)) {
            lostLineOfSightTicks++;
            int graceTicks = Math.max(
                    0,
                    (int) Math.round(CameraLockOnConfig.LOST_TARGET_GRACE.get() * 20.0D)
            );
            if (lostLineOfSightTicks > graceTicks) {
                retargetOrUnlock(player, level, lockedTarget, TargetLossReason.LOST_LINE_OF_SIGHT);
            }
        } else {
            lostLineOfSightTicks = 0;
        }
    }

    private static void retargetOrUnlock(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity previousTarget,
            TargetLossReason reason
    ) {
        LivingEntity replacement = null;
        if (CameraLockOnConfig.AUTO_RETARGET.get()) {
            replacement = findRetargetTarget(player, level, previousTarget, reason);
        }

        if (replacement != null) {
            lockTarget(replacement, LockReason.RETARGET);
        } else {
            unlockTarget(false, LockReason.LOST);
        }
    }

    private static LivingEntity findRetargetTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity previousTarget,
            TargetLossReason reason
    ) {
        if (reason != TargetLossReason.DEAD) {
            return findTarget(player, level, previousTarget);
        }

        CameraLockOnConfig.RetargetMode mode =
                CameraLockOnConfig.RetargetMode.fromConfig(
                        CameraLockOnConfig.RETARGET_MODE.get()
                );

        if (mode == CameraLockOnConfig.RetargetMode.ANY) {
            return findTarget(player, level, previousTarget);
        }

        LivingEntity sameType = findNearestTargetOfType(
                player,
                level,
                previousTarget.getType(),
                previousTarget
        );
        if (sameType != null) {
            return sameType;
        }

        return mode == CameraLockOnConfig.RetargetMode.SAME_TYPE_FIRST
                ? findTarget(player, level, previousTarget)
                : null;
    }

    private static void updateAutoLock(LocalPlayer player, ClientLevel level) {
        if (active || !CameraLockOnConfig.AUTO_LOCK.get()) {
            resetAutoLockCandidate();
            return;
        }

        long now = System.currentTimeMillis();
        if (now < autoLockBlockedUntil) {
            resetAutoLockCandidate();
            return;
        }

        LivingEntity candidate = raycastTarget(player, level);
        if (candidate == null) {
            resetAutoLockCandidate();
            return;
        }

        if (candidate != autoLockCandidate) {
            autoLockCandidate = candidate;
            autoLockCandidateStartTime = now;
            return;
        }

        long requiredDuration = Math.max(
                1L,
                Math.round(CameraLockOnConfig.AUTO_LOCK_DELAY.get() * 1000.0D)
        );
        if (now - autoLockCandidateStartTime >= requiredDuration) {
            lockTarget(candidate, LockReason.AUTO_CROSSHAIR);
        }
    }

    private static LivingEntity raycastTarget(LocalPlayer player, ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        double range = CameraLockOnConfig.LOCK_ON_RANGE.get();

        if (minecraft.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living
                && TargetingRules.isEligible(
                        player,
                        living,
                        true,
                        temporaryPinnedType,
                        range
                )) {
            return living;
        }

        Vec3 start = ThirdPersonAimResolver.targetingOrigin(player);
        Vec3 direction = ThirdPersonAimResolver.targetingDirection(player);
        Vec3 end = start.add(direction.scale(range));

        HitResult blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        double maximumHitDistanceSqr = blockHit.getType() == HitResult.Type.MISS
                ? range * range
                : start.distanceToSqr(blockHit.getLocation());

        LivingEntity nearestGeneral = null;
        LivingEntity nearestPreferred = null;
        double nearestGeneralDistanceSqr = maximumHitDistanceSqr;
        double nearestPreferredDistanceSqr = maximumHitDistanceSqr;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == player) {
                continue;
            }
            if (!TargetingRules.isEligible(
                    player,
                    living,
                    false,
                    temporaryPinnedType,
                    range
            )) {
                continue;
            }

            AABB hitBox = living.getBoundingBox().inflate(living.getPickRadius());
            Optional<Vec3> intersection = hitBox.clip(start, end);
            if (intersection.isEmpty()) {
                continue;
            }

            double hitDistanceSqr = start.distanceToSqr(intersection.get());
            if (hitDistanceSqr > maximumHitDistanceSqr + 1.0E-7D) {
                continue;
            }

            if (isPreferredType(living) && hitDistanceSqr <= nearestPreferredDistanceSqr) {
                nearestPreferred = living;
                nearestPreferredDistanceSqr = hitDistanceSqr;
            }
            if (hitDistanceSqr <= nearestGeneralDistanceSqr) {
                nearestGeneral = living;
                nearestGeneralDistanceSqr = hitDistanceSqr;
            }
        }

        return nearestPreferred != null ? nearestPreferred : nearestGeneral;
    }

    public static void updateCameraAngles() {
        if (!active || lockedTarget == null) {
            wasLockedLastFrame = false;
            lastSteeringNanos = 0L;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || lockedTarget.level() != level) {
            wasLockedLastFrame = false;
            lastSteeringNanos = 0L;
            return;
        }

        double steeringDt = nextSteeringDeltaSeconds();
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        boolean thirdPersonView = !minecraft.options.getCameraType().isFirstPerson();
        double aimStrength = Mth.clamp(
                thirdPersonView
                        ? CameraLockOnConfig.THIRD_PERSON_AIM_STRENGTH.get()
                        : CameraLockOnConfig.FIRST_PERSON_AIM_STRENGTH.get(),
                0.25D, 2.0D);

        if (shouldSuspendCamera(minecraft, player)) {
            rememberCurrentAngles(currentYaw, currentPitch);
            return;
        }

        /*
         * Lost Target Grace only preserves the lock/HUD. It must never keep
         * steering the player or camera through solid blocks, foliage, or a
         * corner. Steering resumes naturally when line of sight returns.
         */
        if (!player.hasLineOfSight(lockedTarget) && !allowsOccludedSteering()) {
            rememberCurrentAngles(currentYaw, currentPitch);
            return;
        }

        boolean smartLock = CameraLockOnConfig.SMART_LOCK.get();
        boolean convergedLock = ThirdPersonCameraController.isConvergedLockActive();
        if (smartLock) {
            if (wasLockedLastFrame) {
                float yawDifference = Math.abs(Mth.wrapDegrees(currentYaw - lastLockedYaw));
                float pitchDifference = Math.abs(currentPitch - lastLockedPitch);
                if (yawDifference > 0.05F || pitchDifference > 0.05F) {
                    lastMouseInputTime = System.currentTimeMillis();
                }
            }

            if (System.currentTimeMillis() - lastMouseInputTime < 400L) {
                if (!convergedLock) {
                    contextualReturnProgress = 0.0D;
                    contextualReturnPending = true;
                }
                rememberCurrentAngles(currentYaw, currentPitch);
                return;
            }
        }

        Vec3 primaryAimPoint = TargetingRules.toWorldAimPoint(
                lockedTarget,
                player.getEyePosition(),
                lockedTarget.getX(),
                lockedTarget.getY(),
                lockedTarget.getZ()
        );
        GroupAimCalculator.GroupAimResult groupResult = GroupAimCalculator.calculate(
                player,
                level,
                lockedTarget,
                primaryAimPoint,
                temporaryPinnedType
        );
        Vec3 aimPoint = ProjectileAimCalculator.resolveForCamera(player, lockedTarget, groupResult.aimPoint());
        if (ThirdPersonCameraController.isConvergedLockActive()) {
            aimPoint = ThirdPersonCameraController.stabilizeConvergedAimPoint(lockedTarget, aimPoint);
        }
        lastEffectiveAimPoint = aimPoint;
        groupTargetCount = groupResult.targetCount();

        double deltaX = aimPoint.x - player.getX();
        double deltaY = aimPoint.y - player.getEyeY();
        double deltaZ = aimPoint.z - player.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));
        float yawDifference = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDifference = targetPitch - currentPitch;

        if (convergedLock) {
            // Never snap the player/camera to a close target. A target crossing the
            // player's origin can otherwise flip yaw by nearly 180 degrees per frame.
            double targetDistance = player.getEyePosition().distanceTo(aimPoint);
            if (horizontalDistance < 0.35D) {
                targetYaw = currentYaw;
                yawDifference = 0.0F;
            }

            double transitionSpeed = Mth.clamp(
                    ClientFeatureStore.getCameraTransitionSpeed(), 0.05D, 1.0D);
            double tau = (targetDistance < 4.0D ? 0.30D
                    : targetDistance < 8.0D ? 0.20D : 0.13D)
                    * (1.20D - transitionSpeed * 0.45D);
            float response = (float) (1.0D - Math.exp(
                    -steeringDt / Math.max(0.05D, tau / aimStrength)));
            float maxStep = (float) ((targetDistance < 4.0D ? 85.0D : 165.0D)
                    * steeringDt * aimStrength);
            float newYaw = currentYaw + Mth.clamp(yawDifference * response, -maxStep, maxStep);
            float newPitch = Mth.clamp(
                    currentPitch + Mth.clamp(pitchDifference * response, -maxStep, maxStep),
                    -89.5F,
                    89.5F
            );

            player.setYRot(newYaw);
            player.setXRot(newPitch);
            player.yRotO = newYaw;
            player.xRotO = newPitch;
            player.yHeadRot = newYaw;
            player.yHeadRotO = newYaw;
            ThirdPersonCameraController.markPlayerRotationDriven(player);
            lastLockedYaw = newYaw;
            lastLockedPitch = newPitch;
            wasLockedLastFrame = smartLock;
            return;
        }

        float deadZoneStrength = getDeadZoneSteeringStrength(yawDifference, pitchDifference);
        /*
         * Smart Lock owns manual-input priority. After the free-look timeout,
         * Dead Zone may soften the return pull but may not permanently suppress it.
         */
        float steeringStrength = smartLock
                ? Math.max(0.28F, deadZoneStrength)
                : deadZoneStrength;
        if (steeringStrength <= 0.0F) {
            rememberCurrentAngles(currentYaw, currentPitch);
            return;
        }

        /*
         * Contextual keeps the visible camera free, while lock-on steers the
         * player's real ray. When those directions are far apart, resuming at
         * full strength produces a visible jerk in the player model and aim ray.
         * Ramp the correction back in, with a longer blend for large angular
         * errors and very close targets.
         */
        double targetDistance = player.getEyePosition().distanceTo(aimPoint);
        float angularError = Math.max(
                Math.abs(yawDifference),
                Math.abs(pitchDifference) * 0.85F
        );
        double errorRatio = Mth.clamp(angularError / 180.0D, 0.0D, 1.0D);
        float contextualReturnBlend = 1.0F;
        if (contextualReturnPending) {
            double duration = (0.24D + errorRatio * 0.42D
                    + (targetDistance < 4.0D ? 0.14D : 0.0D))
                    / Math.sqrt(aimStrength);
            contextualReturnProgress = Math.min(1.0D,
                    contextualReturnProgress
                            + steeringDt / Math.max(0.16D, duration));
            contextualReturnBlend = smoothStep((float) contextualReturnProgress);
            if (contextualReturnProgress >= 1.0D) {
                contextualReturnPending = false;
            }
        }

        double contextualTau = (0.11D + errorRatio * 0.10D
                + (targetDistance < 4.0D ? 0.08D : 0.0D))
                / Math.sqrt(aimStrength);
        float interpolationFactor = Mth.clamp((float) (
                (1.0D - Math.exp(-steeringDt / Math.max(0.06D, contextualTau)))
                        * steeringStrength * contextualReturnBlend), 0.0F, 1.0F);
        double turnRate = (targetDistance < 4.0D ? 105.0D : 175.0D)
                * Math.sqrt(aimStrength);
        float maxStep = (float) (turnRate * steeringDt
                * Math.max(0.20F, contextualReturnBlend));
        float newYaw = currentYaw + Mth.clamp(
                yawDifference * interpolationFactor, -maxStep, maxStep);
        float newPitch = Mth.clamp(
                currentPitch + Mth.clamp(
                        pitchDifference * interpolationFactor, -maxStep, maxStep),
                -90.0F,
                90.0F
        );

        player.setYRot(newYaw);
        player.setXRot(newPitch);
        player.yRotO = newYaw;
        player.xRotO = newPitch;
        player.yHeadRot = newYaw;
        player.yHeadRotO = newYaw;
        ThirdPersonCameraController.markPlayerRotationDriven(player);

        if (smartLock) {
            lastLockedYaw = newYaw;
            lastLockedPitch = newPitch;
            wasLockedLastFrame = true;
        } else {
            wasLockedLastFrame = false;
        }
    }

    private static boolean shouldSuspendCamera(Minecraft minecraft, LocalPlayer player) {
        if (CameraLockOnConfig.SUSPEND_USING_ITEM.get() && player.isUsingItem()
                && !ProjectileAimCalculator.isCameraAssistActive(player)) {
            return true;
        }
        if (CameraLockOnConfig.SUSPEND_RIDING.get() && player.isPassenger()) {
            return true;
        }
        if (CameraLockOnConfig.SUSPEND_ELYTRA.get() && player.isFallFlying()) {
            return true;
        }
        return CameraLockOnConfig.SUSPEND_MINING.get()
                && minecraft.options.keyAttack.isDown()
                && minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
    }

    private static float getDeadZoneSteeringStrength(
            float yawDifference,
            float pitchDifference
    ) {
        if (!CameraLockOnConfig.DEAD_ZONE.get()) {
            deadZoneWasEnabled = false;
            deadZoneOutside = false;
            deadZoneSteeringBlend = 1.0F;
            return 1.0F;
        }

        double horizontal = CameraLockOnConfig.DEAD_ZONE_HORIZONTAL.get();
        double vertical = CameraLockOnConfig.DEAD_ZONE_VERTICAL.get();
        if (horizontal <= 0.0001D || vertical <= 0.0001D) {
            return 1.0F;
        }

        double normalized = Math.sqrt(
                (yawDifference * yawDifference) / (horizontal * horizontal)
                        + (pitchDifference * pitchDifference) / (vertical * vertical)
        );

        if (!deadZoneWasEnabled) {
            deadZoneWasEnabled = true;
            deadZoneOutside = normalized > 1.08D;
            deadZoneSteeringBlend = deadZoneOutside ? 0.15F : 0.0F;
        }

        // Hysteresis prevents the state from rapidly toggling while the target
        // hovers around the ellipse boundary during circular movement.
        if (deadZoneOutside) {
            if (normalized < 0.90D) {
                deadZoneOutside = false;
            }
        } else if (normalized > 1.08D) {
            deadZoneOutside = true;
        }

        float desiredStrength = 0.0F;
        if (deadZoneOutside) {
            double t = Mth.clamp((normalized - 0.90D) / 0.85D, 0.0D, 1.0D);
            // Smoothstep gives the dead-zone edge a gradual pull instead of an
            // abrupt zero-to-full transition.
            desiredStrength = (float) (t * t * (3.0D - 2.0D * t));
        }

        float response = desiredStrength > deadZoneSteeringBlend ? 0.20F : 0.12F;
        deadZoneSteeringBlend += (desiredStrength - deadZoneSteeringBlend) * response;
        if (!deadZoneOutside && deadZoneSteeringBlend < 0.01F) {
            deadZoneSteeringBlend = 0.0F;
        }
        return Mth.clamp(deadZoneSteeringBlend, 0.0F, 1.0F);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static double nextSteeringDeltaSeconds() {
        long now = System.nanoTime();
        double dt = lastSteeringNanos == 0L
                ? 1.0D / 60.0D
                : Math.min(0.10D, Math.max(0.0D,
                (now - lastSteeringNanos) / 1_000_000_000.0D));
        lastSteeringNanos = now;
        return Math.max(1.0D / 1000.0D, dt);
    }

    private static void rememberCurrentAngles(float yaw, float pitch) {
        lastLockedYaw = yaw;
        lastLockedPitch = pitch;
        wasLockedLastFrame = true;
    }

    public static void renderReticle(PoseStack poseStack, Camera camera, float partialTick) {
        if (!CameraLockOnConfig.SHOW_RETICLE.get()
                || !canRenderLockedTargetInfo()
                || !active
                || lockedTarget == null
                || lockedTarget.isRemoved()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || lockedTarget.level() != level) {
            return;
        }

        double interpolatedX = Mth.lerp(partialTick, lockedTarget.xo, lockedTarget.getX());
        double interpolatedY = Mth.lerp(partialTick, lockedTarget.yo, lockedTarget.getY());
        double interpolatedZ = Mth.lerp(partialTick, lockedTarget.zo, lockedTarget.getZ());

        Vec3 primaryAimPoint = TargetingRules.toWorldAimPoint(
                lockedTarget,
                camera.position(),
                interpolatedX,
                interpolatedY,
                interpolatedZ
        );
        GroupAimCalculator.GroupAimResult groupResult = GroupAimCalculator.calculate(
                player,
                level,
                lockedTarget,
                primaryAimPoint,
                temporaryPinnedType
        );
        Vec3 resolvedReticlePoint = ProjectileAimCalculator.resolveForReticle(
                player, lockedTarget, groupResult.aimPoint());
        Vec3 aimPoint;
        if (ThirdPersonCameraController.isConvergedLockActive()
                && lastEffectiveAimPoint != null
                && lastEffectiveAimPoint.lengthSqr() > 1.0E-8D) {
            // Render from the same stabilized point used by player/camera steering.
            // The renderer must not overwrite it with a second unsmoothed solution.
            aimPoint = lastEffectiveAimPoint;
        } else {
            aimPoint = resolvedReticlePoint;
            lastEffectiveAimPoint = aimPoint;
        }
        groupTargetCount = groupResult.targetCount();

        Vec3 relative = aimPoint.subtract(camera.position());
        double distance = relative.length();
        if (distance > CameraLockOnConfig.LOCK_ON_RANGE.get()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        poseStack.mulPose(camera.rotation());

        float scale = Mth.clamp((float) (distance * 0.05D), 0.15F, 1.5F);
        poseStack.scale(scale, scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = minecraft.renderBuffers()
                .bufferSource()
                .getBuffer(RenderTypes.lines());
        double time = (level.getGameTime() + partialTick) * 0.05D;
        drawReticle(consumer, matrix, (float) time);
        minecraft.renderBuffers().bufferSource().endBatch(RenderTypes.lines());
        poseStack.popPose();
    }

    private static LivingEntity findTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity excludedTarget
    ) {
        return findTarget(player, level, excludedTarget, null);
    }

    private static LivingEntity findTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity excludedTarget,
            EntityType<?> requiredType
    ) {
        Vec3 eye = ThirdPersonAimResolver.targetingOrigin(player);
        Vec3 look = ThirdPersonAimResolver.targetingDirection(player);
        double maximumRange = CameraLockOnConfig.LOCK_ON_RANGE.get();

        LivingEntity bestGeneral = null;
        LivingEntity bestPreferred = null;
        double bestGeneralScore = -Double.MAX_VALUE;
        double bestPreferredScore = -Double.MAX_VALUE;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living == excludedTarget
                    || (requiredType != null && living.getType() != requiredType)
                    || !TargetingRules.isEligible(
                            player,
                            living,
                            true,
                            temporaryPinnedType,
                            maximumRange
                    )) {
                continue;
            }

            Vec3 toTarget = living.getBoundingBox().getCenter().subtract(eye);
            double length = toTarget.length();
            if (length <= 0.0001D) {
                continue;
            }

            double alignment = toTarget.scale(1.0D / length).dot(look);
            if (alignment <= VIEW_CONE_DOT_THRESHOLD) {
                continue;
            }

            double distance = player.distanceTo(living);
            double score = scoreTarget(living, alignment, distance, maximumRange);
            if (CameraLockOnConfig.PREFER_BOSSES.get()
                    && TargetingRules.isBoss(living.getType())) {
                score += 0.35D;
            }

            if (score > bestGeneralScore) {
                bestGeneralScore = score;
                bestGeneral = living;
            }

            if (isPreferredType(living) && score > bestPreferredScore) {
                bestPreferredScore = score;
                bestPreferred = living;
            }
        }

        return bestPreferred != null ? bestPreferred : bestGeneral;
    }

    private static double scoreTarget(
            LivingEntity target,
            double alignment,
            double distance,
            double maximumRange
    ) {
        double normalizedDistance = Mth.clamp(
                distance / Math.max(0.0001D, maximumRange),
                0.0D,
                1.0D
        );
        double healthRatio = target.getMaxHealth() <= 0.0F
                ? 1.0D
                : Mth.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F);

        return switch (ClientFeatureStore.getTargetPriority()) {
            case CROSSHAIR -> alignment - 0.15D * normalizedDistance;
            case NEAREST -> (1.0D - normalizedDistance) + 0.20D * alignment;
            case LOWEST_HEALTH -> alignment
                    - 0.35D * normalizedDistance
                    + 0.55D * (1.0D - healthRatio);
            case HIGHEST_HEALTH -> alignment
                    - 0.35D * normalizedDistance
                    + 0.55D * healthRatio;
            case BALANCED -> alignment - 0.60D * normalizedDistance;
        };
    }

    private static boolean isPreferredType(LivingEntity target) {
        if (temporaryPinnedType != null) {
            CameraLockOnConfig.TemporaryPinMode mode =
                    CameraLockOnConfig.TemporaryPinMode.fromConfig(
                            CameraLockOnConfig.TEMPORARY_PIN_MODE.get()
                    );
            return mode == CameraLockOnConfig.TemporaryPinMode.PREFER_SELECTED
                    && target.getType() == temporaryPinnedType;
        }

        return CameraLockOnConfig.TargetTypeMode.fromConfig(
                CameraLockOnConfig.TARGET_TYPE_MODE.get()
        ) == CameraLockOnConfig.TargetTypeMode.PREFER_SELECTED
                && TargetingRules.matchesConfiguredSelectedType(target);
    }

    private static LivingEntity findNearestTargetOfType(
            LocalPlayer player,
            ClientLevel level,
            EntityType<?> requiredType,
            LivingEntity excludedTarget
    ) {
        LivingEntity nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        double maximumRange = CameraLockOnConfig.LOCK_ON_RANGE.get();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living == excludedTarget
                    || living.getType() != requiredType
                    || !TargetingRules.isEligible(
                            player,
                            living,
                            true,
                            temporaryPinnedType,
                            maximumRange
                    )) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(living);
            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = living;
            }
        }
        return nearest;
    }

    public static boolean requestLock(
            LivingEntity target,
            LockReason reason,
            boolean allowReplace
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || target == null) {
            return false;
        }

        if (active && lockedTarget == target) {
            return true;
        }
        if (active && !allowReplace) {
            return false;
        }
        if (!TargetingRules.isEligible(
                player,
                target,
                true,
                temporaryPinnedType,
                Math.max(
                        CameraLockOnConfig.LOCK_ON_RANGE.get(),
                        CameraLockOnConfig.ATTACKER_LOCK_RANGE.get()
                )
        )) {
            return false;
        }

        lockTarget(target, reason);
        return true;
    }

    private static void lockTarget(LivingEntity target, LockReason reason) {
        boolean switched = active && lockedTarget != null && lockedTarget != target;
        lockedTarget = target;
        active = true;
        wasLockedLastFrame = false;
        lastMouseInputTime = 0L;
        lastSteeringNanos = 0L;
        contextualReturnProgress = 0.0D;
        contextualReturnPending = true;
        lostLineOfSightTicks = 0;
        groupTargetCount = 1;
        resetAutoLockCandidate();

        float pitch = switched || reason == LockReason.SWITCH || reason == LockReason.RETARGET
                ? 1.25F
                : 1.0F;
        playFeedbackSound(pitch);
    }

    private static void unlockTarget(boolean manualUnlock, LockReason reason) {
        boolean wasActive = active;
        lockedTarget = null;
        active = false;
        wasLockedLastFrame = false;
        contextualReturnProgress = 1.0D;
        contextualReturnPending = false;
        lostLineOfSightTicks = 0;
        groupTargetCount = 1;
        lastEffectiveAimPoint = Vec3.ZERO;
        deadZoneSteeringBlend = 0.0F;
        deadZoneOutside = false;
        deadZoneWasEnabled = false;
        resetAutoLockCandidate();

        if (manualUnlock) {
            long cooldownMillis = Math.max(
                    0L,
                    Math.round(CameraLockOnConfig.AUTO_LOCK_COOLDOWN.get() * 1000.0D)
            );
            autoLockBlockedUntil = System.currentTimeMillis() + cooldownMillis;
        }
        if (wasActive) {
            playFeedbackSound(reason == LockReason.LOST ? 0.55F : 0.75F);
        }
    }

    private static void playFeedbackSound(float pitch) {
        if (!CameraLockOnConfig.LOCK_SOUNDS.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        float volume = (float) Mth.clamp(CameraLockOnConfig.SOUND_VOLUME.get(), 0.0D, 1.0D);
        if (volume > 0.0F) {
            minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, volume, pitch);
        }
    }

    private static void resetAutoLockCandidate() {
        autoLockCandidate = null;
        autoLockCandidateStartTime = 0L;
    }

    private static void clearAllState() {
        pendingRawLockKeyPresses = 0;
        lockMouseWasDown = false;
        lockMousePressedThisTick = false;
        lockedTarget = null;
        active = false;
        temporaryPinnedType = null;
        wasLockedLastFrame = false;
        lastMouseInputTime = 0L;
        lastSteeringNanos = 0L;
        contextualReturnProgress = 1.0D;
        contextualReturnPending = false;
        lostLineOfSightTicks = 0;
        resetAutoLockCandidate();
        autoLockBlockedUntil = 0L;
        lastEffectiveAimPoint = Vec3.ZERO;
        groupTargetCount = 1;
        deadZoneSteeringBlend = 0.0F;
        deadZoneOutside = false;
        deadZoneWasEnabled = false;
    }

    public static boolean isActive() {
        return active && lockedTarget != null;
    }

    /** True during the short manual-camera override provided by Temporary Free Look. */
    public static boolean isTemporaryFreeLookActive() {
        return isActive()
                && CameraLockOnConfig.SMART_LOCK.get()
                && lastMouseInputTime > 0L
                && System.currentTimeMillis() - lastMouseInputTime < 400L;
    }

    public static LivingEntity getLockedTarget() {
        return lockedTarget;
    }

    public static boolean canRenderLockedTargetInfo() {
        if (!isActive()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return false;
        if (player.hasLineOfSight(lockedTarget)) return true;
        return CameraLockOnConfig.LineOfSightMode.fromConfig(CameraLockOnConfig.LINE_OF_SIGHT_MODE.get())
                == CameraLockOnConfig.LineOfSightMode.GRACE_HUD;
    }

    private static boolean allowsOccludedSteering() {
        return CameraLockOnConfig.LineOfSightMode.fromConfig(CameraLockOnConfig.LINE_OF_SIGHT_MODE.get())
                == CameraLockOnConfig.LineOfSightMode.GRACE_HUD
                && CameraLockOnConfig.OCCLUDED_STEERING.get();
    }

    /** True only while the current target can legally drive player/camera aim. */
    public static boolean canSteerToLockedTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return active
                && lockedTarget != null
                && player != null
                && lockedTarget.isAlive()
                && !lockedTarget.isRemoved()
                && (player.hasLineOfSight(lockedTarget) || allowsOccludedSteering());
    }

    public static LivingEntity getAutoLockCandidate() {
        return autoLockCandidate;
    }

    public static float getAutoLockProgress() {
        if (autoLockCandidate == null || autoLockCandidateStartTime <= 0L) {
            return 0.0F;
        }
        long required = Math.max(
                1L,
                Math.round(CameraLockOnConfig.AUTO_LOCK_DELAY.get() * 1000.0D)
        );
        return Mth.clamp(
                (System.currentTimeMillis() - autoLockCandidateStartTime) / (float) required,
                0.0F,
                1.0F
        );
    }

    public static EntityType<?> getTemporaryPinnedType() {
        return temporaryPinnedType;
    }

    public static String getTemporaryPinnedTypeName() {
        if (temporaryPinnedType == null) {
            return "";
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(temporaryPinnedType);
        return id == null ? temporaryPinnedType.getDescription().getString() : id.toString();
    }

    public static Vec3 getLastEffectiveAimPoint() {
        return lastEffectiveAimPoint;
    }

    public static int getGroupTargetCount() {
        return groupTargetCount;
    }

    private static void drawReticle(VertexConsumer consumer, Matrix4f matrix, float theta) {
        float red = 0.1F;
        float green = 0.9F;
        float blue = 0.8F;
        float alpha = (float) Mth.clamp(
                CameraLockOnConfig.RETICLE_OPACITY.get(),
                0.10D,
                1.0D
        );

        String color = CameraLockOnConfig.RETICLE_COLOR.get();
        if ("Red".equalsIgnoreCase(color)) {
            red = 1.0F; green = 0.2F; blue = 0.1F;
        } else if ("Green".equalsIgnoreCase(color)) {
            red = 0.2F; green = 1.0F; blue = 0.2F;
        } else if ("Yellow".equalsIgnoreCase(color)) {
            red = 1.0F; green = 0.9F; blue = 0.1F;
        } else if ("Purple".equalsIgnoreCase(color)) {
            red = 0.8F; green = 0.2F; blue = 1.0F;
        }

        float distance = 0.25F;
        float arm = 0.08F;
        float cosine = Mth.cos(theta);
        float sine = Mth.sin(theta);
        float[][] corners = {
                {distance, distance},
                {-distance, distance},
                {-distance, -distance},
                {distance, -distance}
        };

        for (float[] corner : corners) {
            float cornerX = corner[0];
            float cornerY = corner[1];
            float rotatedX = cornerX * cosine - cornerY * sine;
            float rotatedY = cornerX * sine + cornerY * cosine;
            float horizontalDirection = cornerX > 0.0F ? -arm : arm;
            float verticalDirection = cornerY > 0.0F ? -arm : arm;
            drawLine(consumer, matrix, rotatedX, rotatedY,
                    rotatedX + horizontalDirection * cosine,
                    rotatedY + horizontalDirection * sine,
                    red, green, blue, alpha);
            drawLine(consumer, matrix, rotatedX, rotatedY,
                    rotatedX - verticalDirection * sine,
                    rotatedY + verticalDirection * cosine,
                    red, green, blue, alpha);
        }

        float diamond = 0.04F;
        drawLine(consumer, matrix, 0.0F, diamond, diamond, 0.0F, red, green, blue, alpha);
        drawLine(consumer, matrix, diamond, 0.0F, 0.0F, -diamond, red, green, blue, alpha);
        drawLine(consumer, matrix, 0.0F, -diamond, -diamond, 0.0F, red, green, blue, alpha);
        drawLine(consumer, matrix, -diamond, 0.0F, 0.0F, diamond, red, green, blue, alpha);
    }

    private static void drawLine(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x1,
            float y1,
            float x2,
            float y2,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.addVertex(matrix, x1, y1, 0.0F)
                .setColor(red, green, blue, alpha)
                .setNormal(0.0F, 0.0F, 1.0F)
                .setLineWidth(2.0F);
        consumer.addVertex(matrix, x2, y2, 0.0F)
                .setColor(red, green, blue, alpha)
                .setNormal(0.0F, 0.0F, 1.0F)
                .setLineWidth(2.0F);
    }

    private enum TargetLossReason {
        DEAD,
        REMOVED,
        LEVEL_CHANGED,
        OUT_OF_RANGE,
        LOST_LINE_OF_SIGHT
    }

    public enum LockReason {
        MANUAL,
        AUTO_CROSSHAIR,
        SWITCH,
        RETARGET,
        HIT_TARGET,
        ATTACKER,
        LOST
    }
}
