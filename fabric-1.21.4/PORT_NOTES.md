# Fabric 1.21.4 Port Notes

Ported for Minecraft 1.21.4 with Fabric Loader `>=0.16.10`, Fabric API `0.115.6+1.21.4`, Fabric Loom `1.9.2`, and Gradle `8.11.1`:

- Replaced NeoForge lifecycle and client events with Fabric callbacks.
- Added Fabric key mapping registration.
- Added optional Mod Menu integration for the existing config screen.
- Added a minimal `GameRenderer` mixin for camera steering.
- Replaced NeoForge config paths with Fabric Loader's config directory.
- Replaced `ModConfigSpec` with a JSON-backed loader-independent value API.
- Preserved all targeting, HUD, awareness, presets, entity selectors, aim overrides, and Group Aim logic.
- Uses vanilla sword recognition plus the manual AOE weapon list where NeoForge item abilities are unavailable.
