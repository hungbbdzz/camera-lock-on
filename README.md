# Camera Lock-On

Camera Lock-On is a lightweight, highly configurable client-side target lock-on mod for Minecraft.

It adds action-game-style targeting without changing attack reach, damage, entity AI, or server-side combat rules. Version 2.0 introduces automatic acquisition, intelligent retargeting, entity-specific aim points, a compact combat HUD, attacker awareness, and experimental sweep/AOE assistance.

## Supported Platforms

| Minecraft | Mod Loader | Java |
|---|---|---|
| 1.21.1 | NeoForge | Java 21 |
| 1.21.1 | Fabric | Java 21 |
| 1.20.1 | Forge | Java 17 |

Camera Lock-On is client-side only. The server normally does not need the mod.

Camera rotation automation may be restricted by competitive servers or anti-cheat systems. Check the server rules before using it in multiplayer.

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
3. In Smart Lock, move the mouse to look around temporarily.
4. Stop moving the mouse and the camera smoothly returns to the target.
5. Press the lock key again to unlock.
6. Open the mod configuration from the Mods screen or assign the dedicated configuration keybind.

Auto Lock is enabled by default. Keeping the crosshair on a valid entity hitbox briefly can acquire it automatically.

## Default Controls

| Action | Default |
|---|---|
| Toggle Camera Lock-On | Middle Mouse Button |
| Switch Target (Next) | Tab |
| Switch Target (Previous) | Unbound |
| Pin Current Target Type | Unbound |
| Open Camera Lock-On Config | Unbound |
| Toggle Auto Lock | Unbound |
| Toggle Smart Lock | Unbound |
| Toggle Hostile Only | Unbound |
| Toggle Target Mini HUD | Unbound |
| Cycle Target Priority | Unbound |
| Cycle Switch Target Mode | Unbound |
| Clear Temporary Pinned Type | Unbound |

All controls can be assigned or changed in Minecraft's standard Controls menu.

## Language Support

Camera Lock-On includes complete built-in UI localization for:

- English (`en_us`)
- Simplified Chinese (`zh_cn`)
- Russian (`ru_ru`)
- Spanish (`es_es`)
- German (`de_de`)
- Japanese (`ja_jp`)

All UI screens, tabs, buttons, tooltips, dialogs, and options adapt automatically to Minecraft's language setting.

## Main Features

- Smart Lock and Hard Lock camera behavior
- Manual lock, Auto Lock, target switching, and Auto Retarget
- Searchable vanilla and modded living-entity selector
- 3D entity preview with automatic and manual rotation
- Any Entity, Selected Type Only, and Prefer Selected Type filters
- Any Entity, Same Type First, and Same Type Only retarget rules
- Global and per-entity aim-point editors
- Searchable per-entity aim override manager
- Target blacklist with compact removal controls
- Draggable mini combat HUD with vanilla hearts and armor icons
- Damage-flash animation and configurable HUD transparency
- Off-screen attacker indicators and optional attacker auto-lock
- Experimental Group Aim / Sweep Assist
- Manual AOE weapon list for modded weapons
- Configurable reticle, sounds, dead zone, suspension rules, and keybinds

## Smart Lock and Hard Lock

### Smart Lock

Smart Lock gives direct mouse input priority. Move the mouse to look away temporarily; after input stops, the camera gently returns to the target.

This is the recommended mode for normal gameplay.

### Hard Lock

Hard Lock continually steers toward the target and allows less free look. It is useful when a stricter action-RPG camera is preferred.

### Elliptical Dead Zone

Dead Zone is optional and disabled by default. It softens small camera corrections around the center of the screen.

In Smart Lock, active mouse input remains the highest-priority control layer.

## Auto Lock

Auto Lock acquires an entity after the crosshair remains on its hitbox for the configured delay.

Relevant settings:

- Aim Delay — how long the crosshair must remain on the same hitbox
- Unlock Cooldown — prevents immediate relocking after manual unlock
- Pixel Indicator — displays one shrinking pixel box around the crosshair

The shrinking box uses the selected reticle color and flashes white when acquisition completes.

Suggested delays:

```text
Fast combat:      0.10–0.25 seconds
Fewer accidents:  0.40–0.75 seconds
```

Auto Lock respects range, visibility, blacklist, Hostile Only, selected entity type, temporary pin, and target validity.

## Entity Filters

### Hostile Only

Default: OFF

Enable this to ignore passive entities during normal acquisition. Keep it disabled for farming, locating animals, testing, or targeting passive modded mobs.

### Type Filter

- Any Entity — any valid nearby living entity may be selected
- Selected Type Only — only the selected registered entity type may be acquired
- Prefer Selected Type — prioritizes the selected type but falls back to other valid targets

### Selecting an Entity

1. Open the Filter tab.
2. Click Selected Entity.
3. Search by translated name or registry ID, such as `Pig` or `minecraft:pig`.
4. Click an entry to preview it.
5. Confirm the selection.
6. Choose Selected Type Only or Prefer Selected Type.

The selector shows A–Z results when the search box is empty, supports scrolling, remembers search and scroll position, and includes recent-selection history.

## Auto Retarget

When a target dies or becomes invalid, Auto Retarget can find a replacement.

- Any Entity — choose any valid nearby target
- Same Type First — prefer the previous target type, then fall back to normal targeting
- Same Type Only — only choose another entity of the same type; otherwise unlock

### Recommended Farming Setup

For farming one mob type in a crowded area:

```text
Hostile Only: OFF
Auto Retarget: ON
Retarget Rule: Same Type Only
```

For the strictest behavior:

```text
Selected Entity: Pig
Type Filter: Selected Type Only
Retarget Rule: Same Type Only
```

This prevents a pig farm lock from jumping to nearby cows, sheep, or other mobs.

## Target Blacklist

Blacklisted entity types are rejected by:

- Manual lock
- Auto Lock
- Target switching
- Auto Retarget
- Attacker auto-lock
- Group Aim

Use the searchable blacklist manager to add or remove registered vanilla and modded living entities.

## Aim Points

### Global Presets

Available global fallback presets:

- Feet
- Lower Body
- Center
- Chest
- Head
- Custom

### Per-Entity Aim Overrides

A saved entity-specific point always has higher priority:

```text
Per-Entity Override
→ Global Preset / Global Custom Point
→ Center fallback
```

Example uses:

- Zombie — head or upper chest
- Spider — low body center
- Enderman — upper torso
- Small modded mob — a stable visible hitbox area

Open `Filter → Aim Overrides` to search, add, edit, or remove saved points.

Saved at:

```text
config/camera_lockon/entity_aim_points.json
```

Per-entity points remain exact unless Allow Group Aim Offset is enabled for that entity.

## Target Mini HUD

Open:

```text
HUD → Adjust HUD Position
```

Drag the highlighted card, then click outside it to apply.

Available information:

- Target name
- Health
- Distance
- Armor points
- Registry ID
- Source mod name
- Damage flash

Registry ID and source mod name are disabled by default to keep the card compact.

### Health Display

For targets with up to 40 maximum health:

- Vanilla heart sprites are used
- Up to ten hearts appear per row
- The second row overlaps tightly
- Heart containers blink when damage is received

For targets above 40 maximum health:

```text
♥ current / maximum
```

### Armor Display

For armor values up to 20, vanilla full and half armor sprites are shown.

Above 20, the HUD uses one armor icon plus the numeric armor value.

### Transparency

HUD background opacity affects only the dark panel. Text, hearts, armor icons, and combat information remain fully visible.

## Attacker Awareness

When an entity attacks from outside the current view, Camera Lock-On can display a directional warning instead of immediately forcing the camera around.

Response modes:

- Off
- Indicator Only
- Lock After Hits
- Lock Immediately

Repeated-hit locking supports configurable hit count, hit window, maximum range, target replacement, protection time, indicator count, and same-side grouping.

When several attackers are active, the system ranks them using recency, repeated hits, estimated damage, distance, and visibility.

## Group Aim / Sweep Assist

Group Aim is experimental and disabled by default.

It keeps a primary target but shifts the effective aim point toward a nearby cluster of valid entities. This can help sword sweeps or other AOE attacks connect with several targets.

Settings include:

- Activation mode
- Same-type grouping
- Group radius
- Maximum group distance
- Maximum targets
- Aim strength
- Maximum offset

### AOE Weapon Recognition

Depending on the loader and Minecraft version, Sweep Weapons mode recognizes supported sword-sweep weapons through the available loader API.

Modded weapons can also be added manually:

```text
Group → Manual AOE Weapons
```

Saved at:

```text
config/camera_lockon/aoe_weapons.json
```

## Recommended Presets

### General Exploration

```text
Lock Mode: Smart
Auto Lock: ON
Hostile Only: OFF
Auto Retarget: ON
Retarget Rule: Same Type First
Dead Zone: OFF
```

### Hostile Combat

```text
Hostile Only: ON
Auto Lock: ON
Auto Retarget: ON
Attacker Response: Indicator Only
```

### Searching for One Entity Type

```text
Selected Entity: desired entity
Type Filter: Prefer Selected Type
```

### Strict Action Camera

```text
Lock Mode: Hard
Dead Zone: OFF
Auto Lock: ON
```

## Configuration Files

Additional structured data:

```text
config/camera_lockon/entity_aim_points.json
config/camera_lockon/aoe_weapons.json
```

The main configuration backend depends on the selected mod loader, but the in-game options and behavior are intended to remain consistent across supported versions.

## Known Limitations

- Only loaded and client-tracked entities can be selected.
- Unloaded chunks cannot be searched.
- New acquisition requires line of sight.
- Entity preview requires an active world.
- Some modded entities may not support temporary preview creation.
- Some modded weapons require manual AOE registration.
- Not every mod-specific animation system runs in preview screens.
- The mod does not increase reach, damage, or server-side hit detection.
- Group Aim may aim between entities because it optimizes a cluster rather than one exact hitbox.
- Camera automation may be restricted by some multiplayer anti-cheat systems.

## Changelog

### 2.0.0 — Major Feature Update

#### Added

- Smart Lock and Hard Lock behavior
- Auto Lock with configurable delay and shrinking pixel acquisition box
- Manual target switching and intelligent Auto Retarget
- Same Type First and Same Type Only retarget rules
- Selected Type Only and Prefer Selected Type filters
- Searchable vanilla and modded entity selector with 3D preview
- Global and per-entity aim-point editors
- Searchable per-entity aim override manager
- Target blacklist manager
- Temporary target-type pinning
- Lock-on-hit modes
- Draggable combat HUD
- Vanilla heart and armor rendering
- Damage-flash animation
- Off-screen attacker awareness and attacker auto-lock
- Group Aim / Sweep Assist
- Manual AOE weapon manager
- Dedicated config keybind and additional unbound combat keybinds
- Full UI localization for English, Chinese, Russian, Spanish, German, and Japanese
- Tooltips, confirmation screens, and translucent configuration UI
- Fabric 1.21.1 support
- Forge 1.20.1 support
- NeoForge 1.21.1 support

#### Changed

- Default lock range increased to 36 blocks
- Auto Lock now defaults ON
- Hostile Only now defaults OFF
- Dead Zone now defaults OFF
- Registry ID and source mod name default OFF in the HUD
- Smart Lock now has priority over Dead Zone during direct mouse input
- Low-health HUD cards resize dynamically
- Second heart rows overlap more tightly
- Common targeting settings are easier to access
- Secondary keybinds default to unbound
- Mod metadata no longer references Zenless Zone Zero
- License for version 2.0.0 and later changed to All Rights Reserved

#### Fixed

- Empty entity-list rendering
- Dynamic entity-search refresh
- Auto Lock hitbox detection edge cases
- UI layers being hidden behind model previews
- HUD distance leaving the card
- Duplicate target panels
- Blacklist and selector footer overlap
- Aim marker being hidden behind the preview entity
- Preview head rotation appearing detached from the body
- Dead Zone overpowering Smart Lock input
- Configuration panel exceeding the visible screen at larger GUI scales
- Fabric Middle Mouse Button lock/unlock input

## License

Camera Lock-On version 2.0.0 and later is distributed under All Rights Reserved.

See the [LICENSE](LICENSE) file for the complete terms.
