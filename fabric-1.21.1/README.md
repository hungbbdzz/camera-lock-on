# Camera Lock-On — Fabric 1.21.1 source port

Client-side Fabric port of Camera Lock-On 2.0 for Minecraft 1.21.1.

## Requirements

- Minecraft 1.21.1
- Java 21
- Fabric Loader 0.16.14 or compatible newer 0.16.x
- Fabric API 0.116.14+1.21.1
- Mod Menu 11.0.3 is optional

## Build on Windows

```text
gradlew.bat build
```

Generated JAR:

```text
build/libs/camera-lock-on-fabric-1.21.1-2.0.0-fabric.1.jar
```

Development client:

```text
gradlew.bat runClient
```

## Fabric-specific implementation

- Fabric client entrypoint
- Fabric keybinding registration
- end-client-tick bridge
- HUD and world render callbacks
- attack entity callback
- optional Mod Menu config screen integration
- GameRenderer mixin for camera steering

## Configuration

The port uses a JSON client config instead of NeoForge `ModConfigSpec`:

```text
config/camera_lockon/camera_lockon-client.json
```

The remaining preset, entity aim point, client feature, and AOE weapon files keep their existing locations under `config/camera_lockon/`.

## Status

Source parsing and JSON validation passed. A full Gradle dependency build and in-game test must still be run on a network-enabled development machine before release.
