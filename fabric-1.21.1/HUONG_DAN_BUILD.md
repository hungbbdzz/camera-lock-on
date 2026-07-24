# Hướng dẫn build Fabric 1.21.1

## Yêu cầu

- JDK 21
- Kết nối mạng trong lần build đầu tiên

## Build trên Windows

Mở Terminal tại thư mục `fabric-1.21.1`, sau đó chạy:

```text
gradlew.bat clean build
```

File mod sau khi build nằm trong:

```text
build\libs\camera-lock-on-fabric-1.21.1-2.0.0-fabric.1.jar
```

Không dùng file có hậu tố `-sources.jar` để chơi.

## Chạy thử trong môi trường phát triển

```text
gradlew.bat runClient
```

Bản Fabric yêu cầu Fabric API. Mod Menu là tùy chọn; không có Mod Menu thì vẫn có thể mở config bằng keybind riêng.
