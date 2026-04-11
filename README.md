<div align="center">

![Aether Manager Banner](banner.jpg)

# Aether Manager

**A Magisk module to optimize your device performance based on your needs:**  
memory control, kernel tweaks, and performance profiles — with WebUI support.

[![Release](https://img.shields.io/github/v/release/aetherdev01/aether-manager?style=flat-square&color=1B6EBF&label=Latest)](https://github.com/aetherdev01/aether-manager/releases)
[![Downloads](https://img.shields.io/github/downloads/aetherdev01/aether-manager/total?style=flat-square&color=2D7D46)](https://github.com/aetherdev01/aether-manager/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-6A3FA0?style=flat-square)](LICENSE)

</div>

---

## Overview

Aether Manager is a root module that brings a full-featured performance manager to your Android device. It provides four performance profiles and 17+ individual tweaks across CPU, GPU, memory, I/O, network, and daily use — all configurable through a clean Material Design 3 WebUI.

---

## Chipset Support

| Chipset | Support |
|---|---|
| **Snapdragon** | Full (KGSL · CPU Boost · SchedBoost · LMK · ZRAM · TCP) |
| **MediaTek** | Full (GPU GED · HPS · CCI Mode · ZRAM · VM) |
| **Exynos** | Partial (Mali · CPU · VM · ZRAM · TCP — KGSL/MTK skipped) |
| **Kirin** | Partial (Mali · CPU · VM · ZRAM — KGSL/MTK skipped) |

> SOC is auto-detected on install from `ro.board.platform`, `ro.hardware`, and `ro.soc.model`.

---

## Requirements

- **Root Manager:** Magisk · KernelSU · APatch
- **Android:** 11.0+ (API 30+)
- **Architecture:** ARM/ARM64

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
- **GPU Throttle Off** — Disable GPU thermal throttling on all SOCs
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

## WebUI

Aether Manager ships with a standalone WebUI accessible via **KernelSU**, **APatch**, or **Magisk** WebUI.

**WebUI features:**
- Real-time device info — model, SoC, Android version, kernel, root type
- Service PID status with bootloop guard indicator
- Profile switcher with active state indicator
- Individual toggle for all 17 tweaks with realtime apply
- ZRAM size and algorithm selector
- I/O scheduler dropdown
- System log viewer with OK / FAIL filter
- Reboot / Reboot Recovery shortcut
- Safe mode toggle for bootloop recovery

> All changes apply instantly without requiring a reboot.

---

## Bootloop Protection

Aether Manager includes a built-in **Bootloop Guard**:

- Tracks boot attempts via a `boot_count` file
- If the device fails to boot successfully **3 or more times**, the guard activates `safe_mode`
- In safe mode, all tweaks are skipped on boot
- Safe mode can be toggled manually from the WebUI Home tab

---

## Installation

1. Download the latest `.zip` from [Releases](https://github.com/aetherdev01/aether-manager/releases)
2. Flash via **Magisk** / **KernelSU** / **APatch** module manager
3. Reboot
4. Open **WebUI** from your root manager and navigate to Aether Manager

---

## Changelog

See [changelog.md](changelog.md) for the full version history.

---

## Credits

| Project | Role |
|---|---|
| [Magisk](https://github.com/topjohnwu/Magisk) | Module Installer |
| [KernelSU WebUI](https://github.com/tiann/KernelSU) | WebUI |
| Open Source Community | Guidance and support |

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
Support the project → [saweria.co/AetherDev](https://saweria.co/AetherDev)

</div>
