# Changelog

All notable changes to **Camera Lock-On** are documented in this file.

---

## [2.0.3] — Target Outline, Occluded Steering & Safe Server Dialog

- Added client-side Target Outline via `EntityOutlineMixin` for glowing silhouettes matching the configured Reticle Color without network desync or damage-flicker.
- Added Ender Dragon multipart entity support for Target Outline.
- Added port for **Fabric 1.20.1** (Java 17).
- Dropped inactive **NeoForge 1.21.11** port to focus on the 5 core community editions (Forge 1.20.1, Fabric 1.20.1, NeoForge 1.21.1, Fabric 1.21.1, Fabric 1.21.11).
- Fixed target loss while `Occluded Steering` is active, maintaining lock and tracking continuously behind solid blocks and walls.
- Expanded First-Person Aim Strength slider and configuration limit up to 4.0x (400%).
- Added interactive Safe Server confirmation dialog (`SafeServerConfirmScreen`) displaying a scrollable list of proposed setting changes (`Setting: Old -> New`) with Apply and Cancel buttons.
- Changed default values for `autoReleaseBow` and `autoRechargeBow` to `false` (off by default).
- Set `multipartAimMode` (OFF), `smartProjectileHitbox` (false), and `adaptiveAimCalibration` (false) to off by default.
- Added translations for Safe Server dialog in all 6 supported languages (English, German, Spanish, Japanese, Russian, Chinese).

---

## [2.0.2] — Bug Fixes & Refinements

- Fixed `MultipartDamageBoxResolver` reflection recursion and vehicle/player hitbox misidentification.
- Fixed `ProjectileSettingsScreen` scrollbar and mouse scrolling across all supported versions.
- Added previous target switching keybind (`SWITCH_TARGET_PREVIOUS_KEY`) and directional target cycling.
- Fixed queued lock input when closing GUI screens on Fabric.
- Restored true third-person camera rotation on Forge 1.20.1.
- Enforced Line-of-Sight visibility checks for reticle rendering on NeoForge.
- Added Trident and offhand weapon support in projectile aim calculations.
- Added ammo checks before automatic bow and crossbow recharging.
- Excluded ridden vehicles, player pets, team allies, and invisible entities from targeting rules.
- Optimized projectile trajectory simulation, frame-rate independent aim smoothing, and adaptive aim calibration.
- Removed deprecated Minecraft 1.21.4 ports.

---

## [2.0.1] — Projectile & Third-Person Update

- Added support for Minecraft 1.21.11 ("Mounts of Mayhem") on both Fabric and NeoForge.
- Added projectile weapon and prediction support.
- Added configurable recognition for modded ranged weapons.
- Added bow auto-release and auto-recharge assistance.
- Added configurable third-person camera positions and aim modes.
- Added third-person aim ray support.
- Added separate first-person and third-person camera strength controls.
- Added Strict and Grace HUD line-of-sight modes.
- Added Safe Server enforcement for strict visibility behavior.
- Improved camera smoothing, free-look transitions, target switching, and close-range stability.
- Improved responsive configuration and preset screens.
- Added additional unbound utility keybinds.
- Fixed Forge camera steering and duplicate rotation updates.
- Fixed configuration blur, duplicate widgets, and stale-frame rendering issues.

---

## [2.0.0] — Major Feature Update

- Added Temporary Free Look and strict camera-lock behavior.
- Added Auto Lock with configurable delay.
- Added manual target switching and intelligent Auto Retarget.
- Added entity filters, blacklist, and per-entity aim points.
- Added draggable combat HUD with health and armor rendering.
- Added off-screen attacker awareness.
- Added Group Aim / Sweep Assist.
- Added manual AOE weapon recognition.
- Added Fabric 1.21.1, NeoForge 1.21.1, and Forge 1.20.1 support.
- Added built-in localization for six languages (English, Simplified Chinese, Russian, Spanish, German, Japanese).

