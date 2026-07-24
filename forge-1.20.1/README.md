# Camera Lock-On — Forge 1.20.1 source port

Client-side Forge port of Camera Lock-On 2.0 for Minecraft 1.20.1.

## Requirements

- Minecraft 1.20.1
- Java 17
- Forge 47.4.10 or compatible Forge 47.x build

## Build on Windows

```text
gradlew.bat build
```

Generated JAR:

```text
build/libs/camera-lock-on-forge-1.20.1-2.0.0.jar
```

Development client:

```text
gradlew.bat runClient
```

## Forge-specific implementation

- Forge client entrypoint
- Forge key mapping event
- client tick event
- interaction input event
- camera angle event
- world render stage event
- GUI render event
- Forge config-screen factory
- 1.20.1 GUI, entity preview, HUD sprite, and vertex API backports

## Configuration

The port uses a JSON client config rather than loader-specific TOML:

```text
config/camera_lockon/camera_lockon-client.json
```

The remaining preset, entity aim point, client feature, and AOE weapon files keep their existing locations under `config/camera_lockon/`.

## Status

Source parsing and JSON validation passed. A full Gradle dependency build and in-game test must still be run on a network-enabled development machine before release.
