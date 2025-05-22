# MarFaNet Android Build Guide

## Overview

This document provides comprehensive instructions for building the MarFaNet Android application from the refactored source code. MarFaNet is based on the Hiddify VPN app but has been completely transformed to use Xray core instead of Sing-box, along with comprehensive rebranding and feature enhancements.

## Prerequisites

### Development Environment

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: OpenJDK 11 or later
- **Kotlin**: 1.9.0 or later
- **Android Gradle Plugin**: 8.0 or later
- **Gradle**: 8.0 or later

### SDK Requirements

- **Compile SDK**: 34
- **Target SDK**: 34
- **Min SDK**: 21 (Android 5.0)

### NDK Requirements (for native libraries)

- **NDK Version**: 23.0.7599858 or later
- **CMake**: 3.22.1 or later

## Project Structure

