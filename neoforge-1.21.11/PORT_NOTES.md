# NeoForge 1.21.11 ("Mounts of Mayhem") Port Notes

Ported for Minecraft 1.21.11 with NeoForge `21.11.45`, Parchment mappings `2025.12.20`, and `net.neoforged.moddev` `2.0.146` (Gradle `8.10.2`).

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

## 2. NeoForge 21.11 & FancyModLoader 10 Architecture
- **Unified EventBusSubscriber**:
  - In FML 10, `@EventBusSubscriber` no longer accepts `bus = Bus.GAME` or `bus = Bus.MOD`.
  - FML automatically checks if the event parameter implements `IModBusEvent` to route to the mod event bus (e.g. `RegisterKeyMappingsEvent`) or to the game event bus (all other events).
- **FMLEnvironment**:
  - `FMLEnvironment.dist` replaced by `FMLEnvironment.getDist()`.
- **RenderLevelStageEvent**:
  - Event hierarchy separated into stage-specific classes: `RenderLevelStageEvent.AfterEntities`.
  - Partial tick obtained via `minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)`.

## 3. Rendering & Line Pipeline Fixes
- **LineWidth Requirement**:
  - In 1.21.11, `RenderPipelines.LINES` uses `DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH`.
  - All line draw methods (`LockOnController`, `ThirdPersonAimRayRenderer`, `ProjectileTrajectoryRenderer`) provide `.setLineWidth(2.0F)` on each vertex to prevent `IllegalStateException: Missing elements in vertex: LineWidth`.

## 4. GUI & Screen Layer Architecture
- **Strata-Based Rendering**:
  - In 1.21.11, `Screen.renderWithTooltipAndSubtitles` renders the background stratum (`renderBackground`) before `render()`.
  - Removed redundant `this.renderBackground(...)` calls inside `render()` across all 13 screens to prevent `IllegalStateException: Can only blur once per frame`.
- **2D Matrix Stack**:
  - `graphics.pose()` returns a 2D `Matrix3x2fStack` (JOML).
  - Used `pushMatrix()`, `popMatrix()`, `translate(float, float)`, and `scale(float, float)`.
- **Event Callbacks**:
  - Mouse handlers updated to `(MouseButtonEvent event, ...)`.
  - Modifiers checked via `event.hasShiftDown()`.
- **Key Categories**:
  - Key mapping category registered using `KeyMapping.Category.register(Identifier)`.
  - Middle mouse capture uses `MouseHandler.onButton(long, MouseButtonInfo, int, CallbackInfo)`.
