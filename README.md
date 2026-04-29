# Aether Manager

![](banner.png)

[![Release](https://img.shields.io/github/v/release/aetherdev01/aether-manager?color=1B6EBF&label=Release&style=for-the-badge)](https://github.com/aetherdev01/aether-manager/releases)
[![Downloads](https://img.shields.io/github/downloads/aetherdev01/aether-manager/total?color=2D7D46&label=Downloads&style=for-the-badge)](https://github.com/aetherdev01/aether-manager/releases)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue?style=for-the-badge)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-brightgreen?style=for-the-badge)](https://developer.android.com/about/versions/15)
[![License](https://img.shields.io/badge/License-Apache%202.0-6A3FA0?style=for-the-badge)](LICENSE)
[![Build Status](https://img.shields.io/github/actions/workflow/status/aetherdev01/aether-manager/android.yml?branch=main&label=Build&style=for-the-badge)](https://github.com/aetherdev01/aether-manager/actions)
[![Code Size](https://img.shields.io/github/languages/code-size/aetherdev01/aether-manager?style=for-the-badge)](https://github.com/aetherdev01/aether-manager)
[![Last Commit](https://img.shields.io/github/last-commit/aetherdev01/aether-manager?style=for-the-badge)](https://github.com/aetherdev01/aether-manager/commits/main)

## Table of Contents

- [Introduction](#introduction)
- [Key Features](#key-features)
- [Jetpack Integration](#jetpack-integration)
- [Downloads](#downloads)
- [Requirements](#requirements)
- [Useful Links](#useful-links)
- [Bug Reports](#bug-reports)
- [Contributing](#contributing)
- [Contributors](#contributors)
- [License](#license)

## Introduction

Aether Manager is a robust and highly optimized native Android application meticulously engineered for **rooted devices**, specifically targeting Android 8.0 (API 26) and newer versions. This project aims to provide unparalleled control and optimization capabilities for your Android device's performance, battery life, and overall system behavior. Built with modern Android development practices and leveraging the power of Jetpack components, Aether Manager offers a seamless and intuitive user experience while delivering powerful system-level tweaks.

## Key Features

Aether Manager empowers users with a comprehensive suite of features designed to fine-tune their device:

-   **Performance Profiles**: Effortlessly switch between predefined profiles such as **Balance**, **Performance**, **Gaming**, and **Battery Saver** to instantly adapt your device's behavior to your current needs. Each profile intelligently adjusts various system parameters for optimal results.
-   **Advanced Kernel Tweaks**: Gain granular control over 17+ individual system tweaks spanning critical areas including CPU governance, GPU frequency, memory management, I/O scheduling, network optimization, and battery efficiency. These tweaks are designed to unlock your device's full potential.
-   **Real-time System Monitor**: Keep a close eye on your device's vital statistics with a comprehensive real-time system monitor. Track SoC (System on Chip) performance, RAM utilization, CPU/GPU frequencies, temperature, and device uptime, providing valuable insights into your device's operational state.
-   **Per-App Optimization (App Profile)**: Customize the behavior of individual applications with dedicated profiles. Automatically apply specific CPU governors, refresh rates, and system tweaks on a per-application basis, ensuring that your favorite apps always run with optimal settings.

## Jetpack Integration

Aether Manager is built upon the foundation of modern Android development, extensively utilizing **Android Jetpack** components to ensure a stable, maintainable, and high-performance application. Jetpack libraries help in adhering to best practices, reducing boilerplate code, and providing backward compatibility. Key Jetpack components integrated into Aether Manager include:

-   **Architecture Components**: Such as `ViewModel` for UI-related data management, `LiveData` for observable data holders, and potentially `Room` for persistent local storage (if applicable for future features), ensuring a robust and testable application architecture.
-   **UI Components**: Leveraging modern UI development practices, potentially including `Jetpack Compose` for declarative UI (if the project transitions to it, or for specific UI elements) or `AppCompat` for consistent UI across different Android versions.
-   **Foundation Components**: Utilizing `Android KTX` extensions to write more concise and idiomatic Kotlin code, enhancing developer productivity and code readability.

## Downloads

The only official and secure source for downloading Aether Manager is through the [**GitHub Releases**](https://github.com/aetherdev01/aether-manager/releases) page. We strongly advise against downloading from unofficial sources to ensure the integrity and security of your device.

## Requirements

To ensure Aether Manager functions correctly on your device, please meet the following requirements:

| Item         | Requirement                               |
| :----------- | :---------------------------------------- |
| **Root Access** | Magisk or KernelSU (required for system-level modifications) |
| **Android Version** | Android 8.0 (Oreo, API 26) or higher      |
| **Architecture** | ARM / ARM64                               |

## Useful Links

Stay connected and get support through our official channels:

-   [**GitHub Releases**](https://github.com/aetherdev01/aether-manager/releases): Download the latest stable versions.
-   [**Changelog**](changelog.md): Review all changes and updates across different versions.
-   [**Telegram Channel**](https://t.me/get01projects): Join our community for announcements and discussions.
-   [**Support / Donate**](https://saweria.co/AetherDev): Support the development of Aether Manager.

## Bug Reports

We appreciate your efforts in reporting bugs to help us improve Aether Manager. To ensure efficient resolution, **only bug reports accompanied by comprehensive logs will be accepted.** Please provide the following information based on the issue type:

| Issue Type          | Required Information                                                              |
| :------------------ | :-------------------------------------------------------------------------------- |
| **Root / Permission Issues** | Specify your root manager version (e.g., Magisk v26.4, KernelSU v0.7.6) and Android version (e.g., Android 14) |
| **Application Crash** | A `logcat` recording captured precisely during the application crash event.       |
| **Tweak Functionality Issue** | Device model, System on Chip (SoC) details, and a screenshot of the 'Log' tab within Aether Manager. |

## Contributing

We welcome contributions from the community! If you're interested in contributing to Aether Manager, please refer to our `CONTRIBUTING.md` guide (to be created) for detailed instructions on how to submit pull requests, report issues, and suggest new features.

## Contributors

A heartfelt thank you to all individuals who have contributed to the development and success of Aether Manager:

-   [@AetherDev22](https://github.com/aetherdev01) - Initial Developer & Maintainer

## License

Aether Manager is distributed under the Apache License, Version 2.0. See the `LICENSE` file for more details.

```
Copyright 2026 AetherDev (@AetherDev22)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
