# Fabric 1.21.11 ("Mounts of Mayhem") Port Notes

Ported for Minecraft 1.21.11 with Fabric Loader `>=0.17.3`, Fabric API `0.141.6+1.21.11`, and Fabric Loom `1.14.10` (Gradle `9.2.1`).

## 1. Mojang Mappings & API Updates
- **Identifier**: `ResourceLocation` renamed to `net.minecraft.resources.Identifier`.
- **Util**: `net.minecraft.Util` moved to `net.minecraft.util.Util`.
- **Camera API**:
  - `getPosition()` -> `position()`.
  - `getXRot()` / `getYRot()` -> `xRot()` / `yRot()`.
  - `getEntity()` -> `entity()`.
  - `Camera.setup(Level, Entity, boolean, boolean, float)`: First parameter updated from `BlockGetter` to `Level`.
  - Mixin shadow updated to `position()`.
- **Entities & Tags**:
  - `AbstractArrow` moved to `net.minecraft.world.entity.projectile.arrow.AbstractArrow`.
  - `EnderDragonPart` moved to `net.minecraft.world.entity.boss.enderdragon.EnderDragonPart`.
  - Sword checks use vanilla tag `stack.is(ItemTags.SWORDS)`.
  - Window handle accessed via `minecraft.getWindow().handle()`.

## 2. Rendering & Line Pipeline Fixes
- **LineWidth Requirement**:
  - In 1.21.11, `RenderPipelines.LINES` uses `DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH`.
  - All line draw methods (`LockOnController`, `ThirdPersonAimRayRenderer`, `ProjectileTrajectoryRenderer`) provide `.setLineWidth(2.0F)` on each vertex to prevent `IllegalStateException: Missing elements in vertex: LineWidth`.
- **World Rendering**:
  - `WorldRenderEvents.AFTER_ENTITIES` retrieves `PoseStack` via `context.matrices()`.
  - Camera obtained via `Minecraft.getInstance().gameRenderer.getMainCamera()`.
  - Partial tick obtained via `Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)`.

## 3. GUI & Screen Layer Architecture
- **Strata-Based Rendering**:
  - In 1.21.11, `Screen.renderWithTooltipAndSubtitles` renders the background stratum (`renderBackground`) before `render()`.
  - Removed redundant `this.renderBackground(...)` calls inside `render()` across all 13 screens to prevent `IllegalStateException: Can only blur once per frame`.
- **2D Matrix Stack**:
  - `graphics.pose()` returns a 2D `Matrix3x2fStack` (JOML).
  - Used `pushMatrix()`, `popMatrix()`, `translate(float, float)`, and `scale(float, float)`.
- **Event Callbacks**:
  - Mouse handlers updated to `(MouseButtonEvent event, ...)`.
  - Modifiers checked via `event.hasShiftDown()`.
- **Textures**:
  - `LockOnHudRenderer` uses `RenderPipelines.GUI_TEXTURED`.
- **Key Categories**:
  - Key mapping category registered using `KeyMapping.Category.register(Identifier)`.
  - Middle mouse capture uses `MouseHandler.onButton(long, MouseButtonInfo, int, CallbackInfo)`.

## 4. Preserved Core Systems
- Complete feature parity with 1.21.1/1.21.4: target locking, target cycle (Tab), auto-lock, smart switch, third-person camera offsets, trajectory prediction, and local JSON config persistence.

