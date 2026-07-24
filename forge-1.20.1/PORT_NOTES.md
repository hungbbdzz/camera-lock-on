# Forge 1.20.1 port notes

Implemented from the NeoForge 1.21.1 source:

- Replaced NeoForge imports and metadata with Forge 1.20.1 equivalents.
- Backported client tick, key mapping, input, camera, HUD, and world rendering hooks.
- Backported GUI background and mouse-scroll signatures.
- Backported entity-preview rendering to the 1.20.1 `InventoryScreen` API.
- Backported HUD heart and armor rendering to the 1.20.1 GUI sprite sheet.
- Backported vertex construction to the 1.20.1 `VertexConsumer` API.
- Replaced `ModConfigSpec` with a JSON-backed loader-independent value API.
- Preserved Forge `ToolActions.SWORD_SWEEP` support and the manual AOE weapon list.
