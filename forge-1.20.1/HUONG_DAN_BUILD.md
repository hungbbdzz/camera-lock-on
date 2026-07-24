# Hướng dẫn build Forge 1.20.1

## Yêu cầu

- JDK 17
- Kết nối mạng trong lần build đầu tiên

## Build trên Windows

Mở Terminal tại thư mục `forge-1.20.1`, sau đó chạy:

```text
gradlew.bat clean build
```

File mod sau khi build nằm trong:

```text
build\libs\camera-lock-on-forge-1.20.1-2.0.0.jar
```

Không dùng file có hậu tố `-sources.jar` để chơi.

## Chạy thử trong môi trường phát triển

```text
gradlew.bat runClient
```

Bản Forge chỉ cần cài ở phía client. Máy chủ vanilla hoặc Forge không cần cài mod này.
