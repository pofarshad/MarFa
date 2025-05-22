# MarFaNet Changelog

All notable changes to the MarFaNet Android VPN application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-rc1] - 2025-05-22

### Added
- **Xray Core Integration**: Replaced Sing-box with latest Xray core for enhanced performance and protocol support
- **GFW Knocker Module**: Advanced anti-censorship capabilities with JNI integration
- **Iran Routing Rules**: Automatic daily updates with SHA-256 validation for optimal routing
- **Performance Optimization Suite**: Comprehensive macrobenchmark harness targeting aggressive KPIs
- **24h Stress Testing**: Automated connection reliability testing with detailed metrics
- **Memory Leak Detection**: LeakCanary integration for debug builds with 512KB threshold monitoring
- **Real-time Performance Profiling**: CPU/RAM/battery monitoring with 60-minute session tracking
- **CI/CD Pipeline**: Full GitHub Actions workflow with security scanning and automated releases
- **Multi-architecture Support**: Native libraries for arm64-v8a, armeabi-v7a, x86_64
- **Comprehensive Test Suite**: Unit tests with 90%+ coverage requirement and JaCoCo reporting

### Changed
- **Package Name**: Rebranded from Hiddify (`app.hiddify.com`) to MarFaNet (`net.marfanet.android`)
- **Application ID**: Complete rebrand with new signing certificates and metadata
- **Core Engine**: Migrated from Sing-box to Xray core for better performance and reliability
- **Build System**: Optimized Gradle configuration with R8 full mode and architecture splits
- **UI/UX**: Updated branding, icons, and user interface elements for MarFaNet identity

### Performance
- **Cold Start Time**: Target ≤1.2s (44% improvement from 1.8s baseline)
- **CPU Usage**: Target ≤11% (21% improvement from 14% baseline)
- **Memory Usage**: Target ≤180MB (18% improvement from 220MB baseline)
- **Battery Drain**: Target ≤2%/hour (33% improvement from 3% baseline)
- **Connection Stability**: Target ≤1 disconnect in 24h (86% improvement from 7 baseline)

### Security
- **OWASP Dependency Check**: Integrated security vulnerability scanning with zero tolerance for Critical/High CVEs
- **Code Coverage**: Minimum 80% test coverage enforced via CI/CD pipeline
- **Static Analysis**: Lint and Detekt integration with blocking gates on warnings
- **Release Signing**: Secure keystore management with environment-based configuration

### Fixed
- **Memory Leaks**: Comprehensive detection and resolution of memory management issues
- **Connection Drops**: Enhanced stability through stress testing and optimization
- **Startup Performance**: Optimized initialization sequence for faster app launch
- **Resource Management**: Improved cleanup and lifecycle management

### Development
- **Automated Testing**: Complete CI/CD pipeline with GitHub Actions
- **Documentation**: Technical reports, performance benchmarks, and API documentation
- **Monitoring**: Real-time performance metrics and flamegraph generation
- **Release Process**: Automated APK building, signing, and distribution

---

## Release Candidate Notes

This Release Candidate (RC-1) represents the culmination of Phase 1-4 development:

1. **Phase 1**: Requirements validation and architecture design
2. **Phase 2**: Core implementation with GFW Knocker and routing automation  
3. **Phase 3**: Performance optimization and stability engineering
4. **Phase 4**: CI/CD pipeline and release candidate preparation

### Testing Instructions

1. **Installation**: Install `MarFaNet-RC1.apk` on Android 5.0+ devices
2. **Performance**: Monitor cold start time, memory usage, and battery consumption
3. **Stability**: Test 24-hour continuous VPN sessions
4. **Connectivity**: Verify protocol support and routing rule effectiveness

### Known Issues

- ProGuard mapping may require fine-tuning for specific device configurations
- Battery optimization settings may affect background performance on some OEMs

### Next Steps

- **Phase 5**: Beta feedback collection and bug fixes
- **Phase 6**: Production release and distribution
- **Phase 7**: Post-launch monitoring and feature enhancements