# ARMv7 Native Libraries

## Required Libraries

Place the following native libraries in this directory before building the release APK:

### 1. libxray.so
- **Source**: Download from [Xray-core releases](https://github.com/XTLS/Xray-core/releases)
- **Architecture**: ARMv7 (32-bit ARM)
- **Purpose**: Xray core native implementation
- **Build Command**: `make android-arm32`

### 2. libgfwknocker.so
- **Source**: Build from GFW Knocker native implementation
- **Architecture**: ARMv7 (32-bit ARM)
- **Purpose**: Anti-censorship technology
- **Build Requirements**: NDK r23+, Go 1.19+

## Build Instructions

1. Download or build the native libraries for ARMv7 architecture
2. Verify library architecture: `file libxray.so` should show "ARM"
3. Place libraries in this directory
4. Build release APK: `./gradlew assembleRelease`

## Verification

Before building, ensure libraries are present:
```bash
ls -la app/src/main/jniLibs/armeabi-v7a/
# Should show:
# libxray.so
# libgfwknocker.so
```

## Security

- Verify SHA256 checksums of downloaded libraries
- Only use libraries from trusted sources
- Scan for malware before integration