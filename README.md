<div align="center">

![Aether Manager Banner](banner.jpg)

# Aether Manager

**A native Android app to manage and optimize your rooted device —**  
performance profiles, kernel tweaks, memory control, and system monitoring — all in one place.

[![Release](https://img.shields.io/github/v/release/aetherdev01/aether-manager?style=flat-square&color=1B6EBF&label=Latest)](https://github.com/aetherdev01/aether-manager/releases)
[![Downloads](https://img.shields.io/github/downloads/aetherdev01/aether-manager/total?style=flat-square&color=2D7D46)](https://github.com/aetherdev01/aether-manager/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-6A3FA0?style=flat-square)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue?style=flat-square)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-brightgreen?style=flat-square)](https://developer.android.com/about/versions/15)

</div>

---

## Overview

Aether Manager is a native Android application for rooted devices. It provides full control over performance profiles and 17+ individual tweaks across CPU, GPU, memory, I/O, network, and battery — with a clean Material Design 3 interface and real-time system monitoring.

Root is managed through **Magisk** or **KernelSU** — no WebUI required.

---

## Requirements

- **Root Manager:** Magisk · KernelSU
- **Android:** 8.0+ (API 26+)
- **Target SDK:** 36 (Android 16)
- **Architecture:** ARM / ARM64

---

## Chipset Support

| Chipset | Support |
|---|---|
| **Snapdragon** | Full (KGSL · CPU Boost · SchedBoost · LMK · ZRAM · TCP) |
| **MediaTek** | Full (GPU GED · HPS · CCI Mode · ZRAM · VM) |
| **Exynos** | Partial (Mali · CPU · VM · ZRAM · TCP — KGSL/MTK skipped) |
| **Kirin** | Partial (Mali · CPU · VM · ZRAM — KGSL/MTK skipped) |

> SoC is auto-detected at runtime from `ro.board.platform`, `ro.hardware`, and `ro.soc.model`.

---

## Features

### Performance Profiles

| Profile | Governor | Description |
|---|---|---|
| Balance | `schedutil` | Smart balance between power and performance |
| Performance | `performance` | Max CPU/GPU frequency, no throttling |
| Gaming | `schedutil + boost` | Sched boost, TCP fastopen, GPU max, MTK CCI |
| Battery Saver | `powersave` | Caps CPU freq to ~60%, reduces GPU OPP |

### CPU & Kernel
- **Sched Boost** — CPU scheduler priority boost (Snapdragon)
- **CPU Boost** — Input boost for MTK / Snapdragon / Exynos
- **GPU Throttle Off** — Disable GPU thermal throttling on all SoCs
- **CPUset Optimizer** — Redistribute cores across top-app / foreground / background

### Memory
- **LMK Aggressive** — Kill background apps faster to free RAM
- **ZRAM** — Compressed swap in RAM (configurable size & algorithm: lz4 / lzo / zstd / lz4hc)
- **VM Dirty Optimization** — Tune dirty page write-back ratios for smoother I/O

### I/O
- **Block Scheduler** — Set per-device I/O scheduler (none · noop · cfq · deadline · bfq · kyber)
- **I/O Latency Opt** — Optimize `read_ahead`, `add_random`, `rotational`

### Network
- **TCP BBR** — BBR congestion control + `fq` qdisc for better throughput
- **DNS over HTTPS** — Private DNS via Cloudflare (`one.one.one.one`)
- **Net Buffer Boost** — Enlarge TCP `rmem_max` / `wmem_max`

### Battery & Daily
- **Aggressive Doze** — Faster deep sleep entry
- **Fast Animation** — Scale all animations to 0.5×
- **Clear Boot Cache** — Clear dalvik/art cache every boot
- **Entropy Boost** — Raise random generator thresholds

---

## App Structure

Aether Manager is built with **Jetpack Compose** and organized into four main tabs:

| Tab | Description |
|---|---|
| **Home** | Real-time system info — SoC, RAM, CPU/GPU freq, temps, uptime |
| **Tweak** | Toggle individual tweaks, set ZRAM size/algo, I/O scheduler |
| **Log** | Live shell log viewer with OK / FAIL filter + reboot options |
| **About** | App version, root info, developer links |

---

## Bootloop Protection

Aether Manager includes a built-in **Bootloop Guard**:

- Tracks boot attempts via a `boot_count` file
- If the device fails to boot **3 or more times**, safe mode activates automatically
- In safe mode, all tweaks are skipped on boot
- Safe mode can be toggled manually from the Home tab

---

## Installation

1. Ensure **Magisk** or **KernelSU** is installed and active
2. Download the latest `.apk` from [Releases](https://github.com/aetherdev01/aether-manager/releases)
3. Install and grant root access when prompted
4. Open Aether Manager and complete the setup wizard

---

## Build

```bash
# Clone
git clone https://github.com/aetherdev01/aether-manager.git
cd aether-manager

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

**Requirements:**
- Android Studio Hedgehog or newer
- JDK 17+
- AGP 8.3.2 · Gradle 8.9

---

## Changelog

See [changelog.md](changelog.md) for the full version history.

---

## Contributors

| Contributor | Role |
|---|---|
| [@AetherDev22](https://github.com/aetherdev22) | Lead Developer — App & Module |
| [Magisk](https://github.com/topjohnwu/Magisk) | Root framework & module system |
| [KernelSU](https://github.com/tiann/KernelSU) | Root framework & kernel-level support |
| [topjohnwu](https://github.com/topjohnwu) | Magisk creator & Android root developer |
| [tiann](https://github.com/tiann) | KernelSU creator & kernel developer |
| [Android Open Source Project](https://source.android.com) | Platform foundation |
| Open Source Community | Libraries, guidance, and support |

---

## License

```
Copyright 2026 AetherDev (@AetherDev22)

Licensed under the Apache License, Version 2.0.
You may not use this file except in compliance with the License.
See LICENSE for the full license text.
```

---

<div align="center">

Support the Project : [saweria.co/AetherDev](https://saweria.co/AetherDev)

</div>
