# Camera Lock-On

Camera Lock-On is a lightweight, highly configurable client-side target lock-on mod for Minecraft.

It adds action-game-style targeting without changing attack reach, damage, entity AI, or server-side combat rules. Version 2.0.1 expands the mod with projectile assistance, prediction, configurable third-person camera support, improved camera smoothing, and stricter line-of-sight controls.

## Supported Platforms

| Minecraft | Mod Loader | Java |
|---|---|---|
| 1.21.1 | NeoForge | Java 21 |
| 1.21.1 | Fabric | Java 21 |
| 1.20.1 | Forge | Java 17 |

Camera Lock-On is client-side only. The server normally does not need the mod.

Camera rotation, projectile assistance, persistent target information, or other automation may be restricted by competitive servers or anti-cheat systems. Check the server rules before using these features in multiplayer.

## Requirements

### NeoForge 1.21.1

- Minecraft 1.21.1
- NeoForge 21.1.65 or newer within the supported 21.1 range
- Java 21

### Fabric 1.21.1

- Minecraft 1.21.1
- Fabric Loader
- Fabric API
- Java 21
- Mod Menu is optional

### Forge 1.20.1

- Minecraft 1.20.1
- Forge for Minecraft 1.20.1
- Java 17

## Quick Start

1. Face a nearby living entity.
2. Press Middle Mouse Button to toggle lock-on.
3. Move the mouse while Temporary Free Look is enabled to look away temporarily.
4. Stop moving the mouse and the camera or player aim smoothly returns to the target.
5. Press the lock key again to unlock.
6. Open the mod configuration from the Mods screen or assign the dedicated configuration keybind.

Auto Lock is enabled by default. Keeping the active cursor or raycast on a valid entity briefly can acquire it automatically.

## Default Controls

| Action | Default |
|---|---|
| Toggle Camera Lock-On | Middle Mouse Button |
| Switch Target | Tab |
| Switch Target Previous | Unbound |
| Open Camera Lock-On Config | Unbound |
| Toggle Auto Lock | Unbound |
| Toggle Temporary Free Look | Unbound |
| Toggle Hostile Only | Unbound |
| Toggle Target Mini HUD | Unbound |
| Toggle Auto Bow Release | Unbound |
| Toggle Auto Bow Recharge | Unbound |
| Cycle Projectile Assist Mode | Unbound |
| Third-Person Camera X - / X + | Unbound |
| Third-Person Camera Y - / Y + | Unbound |
| Third-Person Camera Z - / Z + | Unbound |
| Cycle Target Priority | Unbound |
| Clear Temporary Pinned Type | Unbound |

All controls can be assigned or changed in Minecraft's standard Controls menu.

## Main Features

- Manual lock, Auto Lock, target switching, and Auto Retarget
- Temporary Free Look and configurable camera steering
- Searchable vanilla and modded living-entity selector
- Any Entity, Selected Type Only, and Prefer Selected Type filters
- Any Entity, Same Type First, and Same Type Only retarget rules
- Global and per-entity aim-point editors
- Target blacklist and temporary target-type pinning
- Draggable target HUD with health, armor, distance, and damage feedback
- Off-screen attacker indicators and optional attacker auto-lock
- Projectile weapon support and configurable modded weapon recognition
- Projectile prediction and trajectory assistance
- Bow auto-release and auto-recharge
- Configurable third-person camera positions and aiming modes
- Third-person aim-ray visualization
- Experimental Group Aim / Sweep Assist
- Manual AOE weapon list for modded weapons
- Strict and Grace HUD line-of-sight modes
- Responsive configuration screens, presets, tooltips, and unbound utility keybinds

## Camera Lock Behavior

### Temporary Free Look

Temporary Free Look gives direct mouse input priority.

While enabled:

- Moving the mouse temporarily releases camera steering.
- The player can inspect the surroundings without dropping the target.
- Camera and aim alignment return smoothly after mouse input stops.
- Third-person Converged mode also supports temporary free movement.

### Aim Strength

First-person and third-person steering use separate strength settings.

- First-Person Pull defaults to `1.20x`.
- Third-Person Pull defaults to the stable third-person value.
- Higher values return to the target faster.
- Excessively high values can look less smooth or shake when the target is very close.

### Elliptical Dead Zone

Dead Zone is optional and disabled by default. It softens small corrections around the center of the screen while preserving direct mouse input priority.

## Auto Lock

Auto Lock acquires an entity after the selected cursor or raycast remains on its hitbox for the configured delay.

Relevant settings include:

- Aim Delay
- Unlock Cooldown
- Pixel Indicator
- Lock Range
- Target Filter
- Line of Sight
- Cursor Mode
- Projectile recognition

Auto Lock respects visibility, blacklist, Hostile Only, selected entity type, temporary pin, range, and target validity.

## Line of Sight

### Strict

Strict is the default and safest mode.

When the target becomes occluded:

- Camera steering stops.
- Player aim steering stops.
- Target reticle and HUD information are hidden.
- The target may remain stored internally during Lost Target Grace for smooth reacquisition.
- Seeing the target again restores lock without exposing hidden information.

### Grace HUD

Grace HUD retains target information during Lost Target Grace.

When the target becomes occluded:

- Target name, health, distance, HUD, and reticle may remain visible.
- Camera and player steering stop by default.
- Optional Occluded Steering can continue following the hidden target.

Occluded Steering is disabled by default and may be restricted on multiplayer servers.

Safe Server forces:

```text
Line of Sight: Strict
Occluded Steering: Off
```

## Entity Filters

### Hostile Only

Default: OFF

Enable this to ignore passive entities during normal acquisition.

### Type Filter

- Any Entity
- Selected Type Only
- Prefer Selected Type

### Auto Retarget

- Any Entity
- Same Type First
- Same Type Only

### Target Blacklist

Blacklisted entity types are rejected by manual lock, Auto Lock, target switching, Auto Retarget, attacker auto-lock, and Group Aim.

## Aim Points

Global presets include:

- Feet
- Lower Body
- Center
- Chest
- Head
- Custom

Per-entity overrides take priority over the global fallback.

Saved at:

```text
config/camera_lockon/entity_aim_points.json
```

Multipart aiming is disabled by default. It can be enabled for entities with unusual multipart hitboxes.

## Projectile Assistance

Projectile assistance can recognize vanilla ranged weapons and manually registered modded weapons.

Supported features include:

- Projectile target prediction
- Target movement lead
- Configurable projectile assist modes
- Bow auto-release
- Bow auto-recharge
- Trajectory visualization
- Multipart target selection
- Adaptive aim calibration
- Manual projectile weapon recognition

### Modded Projectile Weapons

Open:

```text
Projectile Settings → Manage Projectile Weapons
```

Search by translated item name or registry ID, then add the item to the projectile weapon list.

Saved at:

```text
config/camera_lockon/projectile_weapons.json
```

Adding an item classifies it as a projectile weapon for compatible assistance features. It does not automatically guarantee correct gravity, velocity, charge timing, or custom firing behavior for every modded weapon.

### Bow Assistance

Auto Release and Auto Recharge are separate options and separate keybinds.

- Auto Release releases the bow at the configured charge point.
- Auto Recharge begins drawing the next shot when the required input and ammunition conditions are met.
- Both options can be enabled or disabled independently.

## Third-Person Camera

Third-person mode can be configured as:

- Off
- Always On
- Projectile

Camera positions use independent configurable slots and support horizontal, vertical, and distance offsets.

### Contextual

Contextual keeps the camera free.

- Lock-on controls the player's real aim direction.
- The camera is not forced toward the target.
- Temporary Free Look and realignment transitions are smoothed.
- Dual cursor can retain center-cursor Auto Lock.
- Dynamic cursor can follow the real player raycast.

### Converged

Converged is the only third-person aim mode that forces the camera toward the locked target.

- Camera, target aim point, and player aim attempt to converge.
- Camera movement is smoothed.
- Close-target correction is limited to reduce shaking.
- Temporary Free Look can temporarily release the camera.
- Occlusion and camera collision can prevent perfect convergence.

Because the camera, player eyes, and projectile origin are physically separated, perfect cursor and projectile alignment is not possible in every third-person situation.

### Aim Ray

Aim Ray can be configured as:

- Off
- Projectile Only
- Always

The ray displays the player's real aiming direction and is especially useful when the free camera direction differs from the projectile direction.

## Target Mini HUD

The target HUD can display:

- Target name
- Health
- Distance
- Armor
- Registry ID
- Source mod name
- Damage flash

Registry ID and source mod name are disabled by default.

The HUD position can be adjusted from:

```text
HUD → Adjust HUD Position
```

## Attacker Awareness

When an entity attacks from outside the current view, Camera Lock-On can display a directional warning or acquire the attacker.

Response modes include:

- Off
- Indicator Only
- Lock After Hits
- Lock Immediately

## Group Aim / Sweep Assist

Group Aim is experimental and disabled by default.

It keeps a primary target while shifting the effective aim point toward a nearby cluster of valid entities. This can help sword sweeps or other AOE attacks connect with several targets.

Modded AOE weapons can be registered manually.

Saved at:

```text
config/camera_lockon/aoe_weapons.json
```

## Presets

Presets can save and restore groups of settings including:

- Camera steering
- First-person and third-person aim strength
- Third-person camera mode and position
- Aim Ray
- Projectile assistance
- HUD
- Filters
- Line-of-sight mode
- Occluded Steering
- Group Aim

Reset Defaults restores the current release defaults.

Safe Server disables or restricts higher-risk automation and hidden-target behavior.

## Language Support

Camera Lock-On includes built-in UI localization for:

- English
- Simplified Chinese
- Russian
- Spanish
- German
- Japanese

## Configuration Files

```text
config/camera_lockon/entity_aim_points.json
config/camera_lockon/aoe_weapons.json
config/camera_lockon/projectile_weapons.json
```

The main configuration backend depends on the selected loader, but the in-game settings are intended to remain consistent across supported versions.

## Known Limitations

- Only loaded and client-tracked entities can be selected.
- Unloaded chunks cannot be searched.
- New target acquisition requires line of sight.
- Third-person camera, player-eye ray, and projectile origin cannot align perfectly in every situation.
- Camera collision and very close targets may reduce convergence accuracy.
- Some modded projectile weapons require manual registration.
- Custom weapon mechanics may require weapon-specific projectile profiles.
- Some modded entities may not support preview creation.
- The mod does not increase reach, damage, or server-side hit detection.
- Camera automation and hidden-target information may be restricted by multiplayer servers.

## Changelog

### 2.0.1 — Projectile & Third-Person Update

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

### 2.0.0 — Major Feature Update

- Added Temporary Free Look and strict camera-lock behavior.
- Added Auto Lock with configurable delay.
- Added manual target switching and intelligent Auto Retarget.
- Added entity filters, blacklist, and per-entity aim points.
- Added draggable combat HUD with health and armor rendering.
- Added off-screen attacker awareness.
- Added Group Aim / Sweep Assist.
- Added manual AOE weapon recognition.
- Added Fabric 1.21.1, NeoForge 1.21.1, and Forge 1.20.1 support.
- Added built-in localization for six languages.

## License

Camera Lock-On version 2.0.0 and later is distributed under All Rights Reserved.

See the [LICENSE](LICENSE) file for the complete terms.
