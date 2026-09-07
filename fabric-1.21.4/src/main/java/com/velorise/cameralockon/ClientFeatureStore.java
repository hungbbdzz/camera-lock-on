package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Small JSON-backed preferences that are intentionally independent from the legacy TOML config. */
public final class ClientFeatureStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = PlatformPaths.configDirectory()
            .resolve("client_features.json");

    private static Data data;

    private ClientFeatureStore() {
    }

    public static synchronized SwitchTargetMode getSwitchTargetMode() {
        ensureLoaded();
        return SwitchTargetMode.fromName(data.switchTargetMode);
    }

    public static synchronized void setSwitchTargetMode(SwitchTargetMode mode) {
        ensureLoaded();
        data.switchTargetMode = (mode == null ? SwitchTargetMode.SMART : mode).name();
        save();
    }

    public static synchronized TargetPriority getTargetPriority() {
        ensureLoaded();
        return TargetPriority.fromName(data.targetPriority);
    }

    public static synchronized void setTargetPriority(TargetPriority priority) {
        ensureLoaded();
        data.targetPriority = (priority == null ? TargetPriority.BALANCED : priority).name();
        save();
    }

    public static synchronized String getLastLoadedPreset() {
        ensureLoaded();
        return data.lastLoadedPreset == null ? "" : data.lastLoadedPreset;
    }

    public static synchronized void setLastLoadedPreset(String name) {
        ensureLoaded();
        data.lastLoadedPreset = name == null ? "" : name.trim();
        save();
    }

    private static void ensureLoaded() {
        if (data != null) {
            return;
        }
        data = new Data();
        if (!Files.isRegularFile(FILE_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Data parsed = GSON.fromJson(reader, Data.class);
            if (parsed != null) {
                data = parsed;
            }
        } catch (IOException | JsonParseException exception) {
            System.err.println("[Camera Lock-On] Could not read " + FILE_PATH + ": " + exception.getMessage());
        }
        data.switchTargetMode = SwitchTargetMode.fromName(data.switchTargetMode).name();
        data.targetPriority = TargetPriority.fromName(data.targetPriority).name();
        if (data.lastLoadedPreset == null) {
            data.lastLoadedPreset = "";
        }
        data.thirdPersonCameraMode = ThirdPersonCameraMode.fromName(data.thirdPersonCameraMode).name();
        data.thirdPersonAimStyle = ThirdPersonAimStyle.fromName(data.thirdPersonAimStyle).name();
        data.freeCameraCursorMode = FreeCameraCursorMode.fromName(data.freeCameraCursorMode).name();
        data.activeCameraSlot = CameraSlot.fromName(data.activeCameraSlot).name();
        if (data.cycleEnabled == null || data.cycleEnabled.length != CameraSlot.values().length) data.cycleEnabled = new boolean[]{false,true,true,false,false};
        if (data.positions == null || data.positions.length != CameraSlot.values().length) data.positions = Data.defaultPositions();
    }

    private static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            Path temporary = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(temporary, FILE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Camera Lock-On] Could not write " + FILE_PATH + ": " + exception.getMessage());
        }
    }

    public static synchronized ThirdPersonCameraMode getThirdPersonCameraMode() { ensureLoaded(); return ThirdPersonCameraMode.fromName(data.thirdPersonCameraMode); }
    public static synchronized void setThirdPersonCameraMode(ThirdPersonCameraMode mode) { ensureLoaded(); data.thirdPersonCameraMode=(mode==null?ThirdPersonCameraMode.OFF:mode).name(); save(); }
    public static synchronized ThirdPersonAimStyle getThirdPersonAimStyle() { ensureLoaded(); return ThirdPersonAimStyle.fromName(data.thirdPersonAimStyle); }
    public static synchronized void setThirdPersonAimStyle(ThirdPersonAimStyle style) { ensureLoaded(); data.thirdPersonAimStyle=(style==null?ThirdPersonAimStyle.CONTEXTUAL:style).name(); save(); }
    public static synchronized FreeCameraCursorMode getFreeCameraCursorMode() { ensureLoaded(); return FreeCameraCursorMode.fromName(data.freeCameraCursorMode); }
    public static synchronized void setFreeCameraCursorMode(FreeCameraCursorMode mode) { ensureLoaded(); data.freeCameraCursorMode=(mode==null?FreeCameraCursorMode.HIDDEN:mode).name(); save(); }
    public static synchronized boolean isHardLockForcesFollowAim() { ensureLoaded(); return data.hardLockForcesFollowAim == null || data.hardLockForcesFollowAim; }
    public static synchronized void setHardLockForcesFollowAim(boolean enabled) { ensureLoaded(); data.hardLockForcesFollowAim=enabled; save(); }
    public static synchronized CameraSlot getActiveCameraSlot() { ensureLoaded(); return CameraSlot.fromName(data.activeCameraSlot); }
    public static synchronized void setActiveCameraSlot(CameraSlot slot) { ensureLoaded(); data.activeCameraSlot=(slot==null?CameraSlot.RIGHT_SHOULDER:slot).name(); save(); }
    public static synchronized double getCameraTransitionSpeed() { ensureLoaded(); return Math.max(0.05, Math.min(1.0, data.cameraTransitionSpeed)); }
    public static synchronized boolean isCameraPositionMessageEnabled() { ensureLoaded(); return data.showCameraPositionMessage; }
    public static synchronized boolean isCameraSlotEnabledInCycle(CameraSlot slot) { ensureLoaded(); return data.cycleEnabled[slot.ordinal()]; }
    public static synchronized void setCameraSlotEnabledInCycle(CameraSlot slot, boolean enabled) { ensureLoaded(); data.cycleEnabled[slot.ordinal()]=enabled; if (!anyCycleEnabled()) data.cycleEnabled[slot.ordinal()]=true; save(); }
    public static synchronized CameraPosition getCameraPosition(CameraSlot slot) { ensureLoaded(); double[] p=data.positions[slot.ordinal()]; return new CameraPosition(p[0],p[1],p[2]); }
    public static synchronized void setCameraPosition(CameraSlot slot, CameraPosition position) { ensureLoaded(); data.positions[slot.ordinal()]=new double[]{position.horizontalOffset(),position.verticalOffset(),position.distanceOffset()}; save(); }
    private static boolean anyCycleEnabled() { for(boolean b:data.cycleEnabled) if(b) return true; return false; }
    public static synchronized void resetCameraDefaults() { ensureLoaded(); data.thirdPersonCameraMode=ThirdPersonCameraMode.OFF.name(); data.thirdPersonAimStyle=ThirdPersonAimStyle.CONTEXTUAL.name(); data.freeCameraCursorMode=FreeCameraCursorMode.HIDDEN.name(); data.hardLockForcesFollowAim=false; data.activeCameraSlot=CameraSlot.RIGHT_SHOULDER.name(); data.cameraTransitionSpeed=0.22; data.showCameraPositionMessage=true; data.cycleEnabled=new boolean[]{false,true,true,false,false}; data.positions=Data.defaultPositions(); save(); }

    private static final class Data {
        private String switchTargetMode = SwitchTargetMode.SMART.name();
        private String targetPriority = TargetPriority.BALANCED.name();
        private String lastLoadedPreset = "";
        private String thirdPersonCameraMode = ThirdPersonCameraMode.OFF.name();
        private String thirdPersonAimStyle = ThirdPersonAimStyle.CONTEXTUAL.name();
        private String freeCameraCursorMode = FreeCameraCursorMode.HIDDEN.name();
        private Boolean hardLockForcesFollowAim = false;
        private String activeCameraSlot = CameraSlot.RIGHT_SHOULDER.name();
        private double cameraTransitionSpeed = 0.22D;
        private boolean showCameraPositionMessage = true;
        private boolean[] cycleEnabled = new boolean[]{false, true, true, false, false};
        private double[][] positions = defaultPositions();
        private static double[][] defaultPositions() { return new double[][]{
                {0.0,0.0,0.0}, {-0.95,0.34,0.10}, {0.95,0.34,0.10}, {-1.35,0.48,0.20}, {1.35,0.48,0.20}}; }
    }

    public enum TargetPriority {
        BALANCED("Balanced"),
        CROSSHAIR("Crosshair"),
        NEAREST("Nearest"),
        LOWEST_HEALTH("Lowest Health"),
        HIGHEST_HEALTH("Highest Health");

        private final String displayName;

        TargetPriority(String displayName) {
            this.displayName = displayName;
        }

        public Component getDisplayName() {
            return Component.translatable("gui.camera_lockon.enum.target_priority." + name().toLowerCase(Locale.ROOT));
        }

        public TargetPriority next() {
            TargetPriority[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static TargetPriority fromName(String value) {
            if (value == null) {
                return BALANCED;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return BALANCED;
            }
        }
    }

    public enum SwitchTargetMode {
        SMART("Smart"),
        CYCLE("Cycle");

        private final String displayName;

        SwitchTargetMode(String displayName) {
            this.displayName = displayName;
        }

        public Component getDisplayName() {
            return Component.translatable("gui.camera_lockon.enum.switch_target_mode." + name().toLowerCase(Locale.ROOT));
        }

        public SwitchTargetMode next() {
            return this == SMART ? CYCLE : SMART;
        }

        public static SwitchTargetMode fromName(String value) {
            if (value == null) {
                return SMART;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SMART;
            }
        }
    }
    public enum ThirdPersonCameraMode {
        OFF, ALWAYS, PROJECTILE_ONLY;
        public ThirdPersonCameraMode next(){ ThirdPersonCameraMode[] v=values(); return v[(ordinal()+1)%v.length]; }
        public Component getDisplayName(){ return Component.translatable("gui.camera_lockon.enum.third_person_camera_mode."+name().toLowerCase(Locale.ROOT)); }
        public static ThirdPersonCameraMode fromName(String value){
            String normalized = value == null ? "OFF" : value.toUpperCase(Locale.ROOT);
            try { return valueOf(normalized); } catch (Exception ignored) { return OFF; }
        }
    }
    public enum ThirdPersonAimStyle {
        CONTEXTUAL, CONVERGED_LOCK;
        public ThirdPersonAimStyle next(){ ThirdPersonAimStyle[] v=values(); return v[(ordinal()+1)%v.length]; }
        public Component getDisplayName(){ return Component.translatable("gui.camera_lockon.enum.third_person_aim_style."+name().toLowerCase(Locale.ROOT)); }
        public static ThirdPersonAimStyle fromName(String value){
            String normalized = value == null ? "CONTEXTUAL" : value.toUpperCase(Locale.ROOT);
            return "CONVERGED_LOCK".equals(normalized) ? CONVERGED_LOCK : CONTEXTUAL;
        }
    }
    public enum FreeCameraCursorMode {
        DYNAMIC, DUAL, HIDDEN;
        public FreeCameraCursorMode next(){ FreeCameraCursorMode[] v=values(); return v[(ordinal()+1)%v.length]; }
        public Component getDisplayName(){ return Component.translatable("gui.camera_lockon.enum.free_camera_cursor_mode."+name().toLowerCase(Locale.ROOT)); }
        public static FreeCameraCursorMode fromName(String value){ try{return valueOf(value==null?"DUAL":value.toUpperCase(Locale.ROOT));}catch(Exception e){return DUAL;} }
    }
    public enum CameraSlot {
        CENTERED, LEFT_SHOULDER, RIGHT_SHOULDER, CUSTOM_1, CUSTOM_2;
        public CameraSlot next(){ CameraSlot[] v=values(); return v[(ordinal()+1)%v.length]; }
        public Component getDisplayName(){ return Component.translatable("gui.camera_lockon.camera.position."+name().toLowerCase(Locale.ROOT)); }
        public static CameraSlot fromName(String value){ try{return valueOf(value==null?"RIGHT_SHOULDER":value.toUpperCase(Locale.ROOT));}catch(Exception e){return RIGHT_SHOULDER;} }
    }
    public record CameraPosition(double horizontalOffset, double verticalOffset, double distanceOffset) {}

}
