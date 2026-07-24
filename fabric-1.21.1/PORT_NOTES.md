# Fabric 1.21.1 port notes

Implemented from the NeoForge 1.21.1 source:

- Replaced NeoForge lifecycle and client events with Fabric callbacks.
- Added Fabric key mapping registration.
- Added optional Mod Menu integration for the existing config screen.
- Added a minimal `GameRenderer` mixin for camera steering.
- Replaced NeoForge config paths with Fabric Loader's config directory.
- Replaced `ModConfigSpec` with a JSON-backed loader-independent value API.
- Preserved all targeting, HUD, awareness, presets, entity selectors, aim overrides, and Group Aim logic.
- Uses vanilla sword recognition plus the manual AOE weapon list where NeoForge item abilities are unavailable.
