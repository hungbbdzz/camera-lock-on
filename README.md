# Camera Lock-On

<p align="center">
  <a href="https://modrinth.com/mod/velorise-camera-lock-on"><img src="https://img.shields.io/badge/Modrinth-Available_Now-00af5c?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth" /></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/camera-lock-on"><img src="https://img.shields.io/badge/CurseForge-Available_Now-f16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="CurseForge" /></a>
  <a href="https://github.com/hungbbdzz/camera-lock-on"><img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub" /></a>
  <a href="https://github.com/sponsors/hungbbdzz"><img src="https://img.shields.io/badge/Sponsor-GitHub_Sponsors-ea4aaa?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="GitHub Sponsors" /></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-v2.0.3-brightgreen.svg" alt="Release" />
  <img src="https://img.shields.io/badge/Minecraft-1.21.11%20%7C%201.21.1%20%7C%201.20.1-blue.svg" alt="Minecraft Versions" />
  <img src="https://img.shields.io/badge/Loaders-NeoForge%20%7C%20Fabric%20%7C%20Forge-orange.svg" alt="Mod Loaders" />
  <img src="https://img.shields.io/badge/Side-100%25%20Client-purple.svg" alt="Client Only" />
</p>

A modern, lightweight, and 100% client-side action-RPG target lock-on mod for Minecraft. Features smooth combat camera steering, intelligent projectile prediction and assist, configurable third-person aiming modes, and a compact tactical combat HUD without modifying vanilla reach, damage, or server-side rules.

---

## 📦 Multi-Loader Project Architecture

This repository contains all official editions of Camera Lock-On:

| Subfolder | Minecraft Version | Mod Loader | Java | Status | Release Artifact |
| :--- | :---: | :---: | :---: | :---: | :--- |
| [`neoforge-1.21.1/`](neoforge-1.21.1/) | **1.21.1** | NeoForge 21.1.65+ | Java 21 | Stable Release | `camera_lockon-1.21.1-neoforge-2.0.3.jar` |
| [`fabric-1.21.1/`](fabric-1.21.1/) | **1.21.1** | Fabric Loader 0.16.14+ | Java 21 | Stable Release | `camera_lockon-1.21.1-fabric-2.0.3.jar` |
| [`neoforge-1.21.11/`](neoforge-1.21.11/) | **1.21.11** | NeoForge 21.11.45+ | Java 21 | Stable Release | `camera_lockon-1.21.11-neoforge-2.0.3.jar` |
| [`fabric-1.21.11/`](fabric-1.21.11/) | **1.21.11** | Fabric Loader 0.17.3+ | Java 21 | Stable Release | `camera_lockon-1.21.11-fabric-2.0.3.jar` |
| [`forge-1.20.1/`](forge-1.20.1/) | **1.20.1** | Forge 47.4.10+ | Java 17 | Stable Release | `camera_lockon-1.20.1-forge-2.0.3.jar` |

---

## ⚡ Key Architectural Features (v2.0.3)

1. **Action-RPG Lock-On & Temporary Free Look**:
   - Smooth camera tracking with customizable first-person and third-person aim strength.
   - **Temporary Free Look**: Moving the mouse temporarily releases camera tracking to survey surroundings, smoothly returning to the target once mouse movement stops.
   - Elliptical Dead Zones and frame-rate independent rotation smoothing.

2. **Projectile Prediction & Ranged Assistance**:
   - Real-time parabolic ballistic trajectory calculation with lead prediction for moving targets.
   - Optional automatic bow/crossbow release at optimal charge and auto-recharge when ammunition is available.
   - Dynamic in-game registration for custom modded projectile weapons (`config/camera_lockon/projectile_weapons.json`).

3. **Configurable Third-Person Combat Modes**:
   - **Contextual Mode**: Keeps the camera free while aligning player attack vectors with the locked target.
   - **Converged Mode**: Camera and crosshair smoothly track the target with close-range anti-jitter clamping.
   - Visual 3D **Aim Ray** rendering to indicate true projectile vectors.

4. **Tactical Mini HUD & Attacker Awareness**:
   - Draggable in-game HUD displaying target name, health, armor, distance, and damage flash.
   - Directional off-screen indicator arrows warning of incoming damage or hostile aggro.
   - Strict line-of-sight occlusion verification with Safe Server presets compliant with multiplayer anti-cheat rules.

---

## 🎮 Controls & Quick Start

1. Look towards an entity and press **Middle Mouse Button** to toggle target lock.
2. Press **Tab** to cycle between adjacent living entities.
3. Open the configuration screen from the **Mods Menu** or assign the config keybind to fine-tune steering, HUD layout, and assist modes.

| Keybind | Default | Description |
| :--- | :---: | :--- |
| **Toggle Lock-On** | `Middle Mouse Button` | Lock/unlock the camera onto the targeted entity. |
| **Switch Target** | `Tab` | Cycle to the next valid target entity. |
| **Switch Target (Previous)** | *Unbound* | Cycle to the previous valid target entity. |
| **Open Config Screen** | *Unbound* | Open the interactive settings & preset manager. |
| **Toggle Auto Lock** | *Unbound* | Enable/disable automatic acquisition upon crosshair hover. |
| **Toggle Hostile Only** | *Unbound* | Restrict target acquisition exclusively to hostile mobs. |
| **Toggle Mini HUD** | *Unbound* | Show or hide the floating target status HUD. |

---

## 🛠️ Build & Dev Instructions

### 1. Build All Loaders in One Step
Run the master automation script from the repository root:
```cmd
build-all.bat
```
* Built `.jar` files are automatically assembled and copied into `mods/`.
* Requires JDK 21 (`JAVA21_HOME`) and JDK 17 (`JAVA17_HOME`).

### 2. Run Development Clients
Launch the interactive launcher at the root:
```cmd
run.bat
```
Or execute specific loader runner scripts from [`scripts/`](scripts/):
```cmd
# NeoForge 1.21.1 / 1.21.11
scripts\run-neoforge.bat 1
scripts\run-neoforge.bat 2

# Fabric 1.21.1 / 1.21.11
scripts\run-fabric.bat 1
scripts\run-fabric.bat 2

# Forge 1.20.1
scripts\run-forge-1.20.1.bat
```

---

## 📜 Changelog

Detailed release notes and historical changes are available in [`CHANGELOG.md`](CHANGELOG.md).

---

## 📄 License

Camera Lock-On is distributed under **All Rights Reserved**. See [LICENSE](LICENSE) for full licensing terms.
