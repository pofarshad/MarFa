# Contributing to MarFaNet

Welcome to the MarFaNet community! 🚀 We're excited to have you contribute to building the most advanced Android VPN application.

## 🎯 **Project Vision**

MarFaNet aims to provide a **high-performance, secure, and user-friendly VPN experience** with cutting-edge technology including Xray core integration, advanced anti-censorship capabilities, and enterprise-grade optimization.

## 🛠️ **Development Setup**

### Prerequisites
- **Android Studio**: Latest stable version
- **JDK**: 17 or higher
- **Android SDK**: API 21+ (Android 5.0+)
- **Git**: For version control
- **ADB**: For device testing

### Quick Start
```bash
# Clone repository
git clone https://github.com/marfanet/android.git
cd android

# Run tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## 📋 **Contribution Guidelines**

### Code Standards
- **Language**: Kotlin preferred, Java accepted for legacy components
- **Style**: Follow [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- **Coverage**: Maintain 80%+ test coverage for new code
- **Documentation**: All public APIs must be documented

### Commit Convention
```
type(scope): description

feat(vpn): add split-tunneling support for per-app routing
fix(core): resolve memory leak in connection manager
docs(readme): update installation instructions
perf(startup): optimize cold start by 200ms
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`

## 🔄 **Development Workflow**

### 1. Issue First
- Check existing issues before creating new ones
- Use issue templates for bug reports and feature requests
- Label appropriately: `bug`, `enhancement`, `security`, `performance`

### 2. Branch Strategy
```bash
# Feature development
git checkout -b feature/split-tunneling-ui

# Bug fixes
git checkout -b fix/connection-timeout-issue

# Hotfixes (from release branch)
git checkout -b hotfix/security-patch-cve-2025-1234
```

### 3. Pull Request Process
1. **Create PR** with descriptive title and detailed description
2. **Link Issues** using "Fixes #123" or "Addresses #456"
3. **Add Tests** for new functionality or bug fixes
4. **Update Documentation** if needed
5. **Request Review** from code owners

### 4. Review Criteria
- ✅ All CI checks pass (lint, tests, security scan)
- ✅ Code coverage maintained or improved
- ✅ No new warnings or security vulnerabilities
- ✅ Performance impact assessed
- ✅ UI/UX changes include screenshots

## 🏗️ **Architecture Overview**

### Core Components
```
app/
├── src/main/kotlin/net/marfanet/android/
│   ├── core/          # Core VPN functionality
│   ├── ui/            # User interface components
│   ├── service/       # Background services
│   ├── data/          # Data layer (Room, preferences)
│   └── telemetry/     # Analytics and crash reporting
├── gfwknocker/        # Anti-censorship module
└── macrobenchmark/    # Performance testing
```

### Technology Stack
- **Core**: Kotlin, Coroutines, Flow
- **UI**: Android Views (migrating to Compose in v2.0)
- **VPN**: Xray core with JNI bindings
- **Database**: Room for local storage
- **Networking**: OkHttp for API calls
- **Testing**: JUnit, Robolectric, Espresso

## 🎨 **UI/UX Guidelines**

### Design Principles
1. **Performance First**: UI should never block VPN functionality
2. **Simplicity**: Complex features should have simple interfaces
3. **Accessibility**: Support screen readers and high contrast
4. **Material Design**: Follow Android design guidelines

### Color Scheme
- **Primary**: Deep Purple (#673AB7)
- **Secondary**: Purple Accent (#9C27B0)
- **Surface**: Dynamic based on system theme
- **Error**: Material Design error colors

## 🧪 **Testing Strategy**

### Test Types
- **Unit Tests**: Business logic, data processing
- **Integration Tests**: Component interactions
- **UI Tests**: User interface flows
- **Performance Tests**: Macrobenchmark suite

### Test Coverage Requirements
- **New Features**: 90%+ coverage required
- **Bug Fixes**: Must include regression test
- **Performance**: Benchmark tests for critical paths

### Running Tests
```bash
# Unit tests
./gradlew testDebugUnitTest

# UI tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Performance benchmarks
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest

# Coverage report
./gradlew jacocoTestReport
```

## 🔒 **Security Considerations**

### Security Requirements
- **No Hardcoded Secrets**: Use BuildConfig or environment variables
- **Input Validation**: Sanitize all user inputs
- **Secure Storage**: Use Android Keystore for sensitive data
- **Network Security**: Certificate pinning for API calls

### Vulnerability Reporting
- **Security Issues**: Email security@marfanet.com (not GitHub issues)
- **Bounty Program**: Available through HackerOne
- **Response SLA**: 48 hours for security reports

## 📊 **Performance Guidelines**

### Performance Targets
- **Cold Start**: ≤1.2s from tap to ready
- **CPU Usage**: ≤11% during active VPN session
- **Memory**: ≤180MB RSS during operation
- **Battery**: ≤2%/hour drain rate

### Optimization Practices
- **Lazy Loading**: Initialize components only when needed
- **Memory Management**: Avoid memory leaks, use weak references
- **Background Processing**: Use WorkManager for non-critical tasks
- **Network Efficiency**: Batch API calls, implement caching

## 🚀 **Release Process**

### Version Numbering
- **Major (2.0.0)**: Breaking changes, major features
- **Minor (1.1.0)**: New features, backwards compatible
- **Patch (1.0.1)**: Bug fixes, security patches

### Release Criteria
- ✅ All tests pass on CI
- ✅ Performance benchmarks meet targets
- ✅ Security scan shows 0 Critical/High vulnerabilities
- ✅ Crash-free sessions ≥95%

## 🤝 **Community**

### Communication Channels
- **GitHub Discussions**: Feature proposals, Q&A
- **GitHub Issues**: Bug reports, specific improvements
- **Monthly RFC Calls**: Community feature discussions
- **Email**: dev-team@marfanet.com for private matters

### Code Owners
- **Core VPN**: @vpn-team
- **UI/UX**: @ui-team
- **Security**: @security-team
- **Performance**: @perf-team
- **GFW Knocker**: @anti-censorship-team

### Recognition
- **Contributors**: Listed in CONTRIBUTORS.md
- **Major Features**: Credited in release notes
- **Security Findings**: Acknowledged in security advisories

## 📈 **Roadmap Participation**

### Current Focus (Q3-Q4 2025)
- **Split-Tunneling UI**: Per-app route selection
- **Real-Time Stats**: Performance overlay
- **WireGuard Integration**: Alternative core option
- **Auto-QoS**: Dynamic congestion control

### How to Contribute to Roadmap
1. **RFC Process**: Submit detailed proposals via GitHub Discussions
2. **Community Feedback**: Participate in monthly calls
3. **Prototype Development**: Build proof-of-concept implementations
4. **User Research**: Conduct usability studies

## 🎓 **Learning Resources**

### Android Development
- [Android Developer Guides](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Android Testing](https://developer.android.com/training/testing)

### VPN Technology
- [Xray Documentation](https://xtls.github.io/en/)
- [Android VPN Service](https://developer.android.com/reference/android/net/VpnService)
- [Network Security](https://developer.android.com/training/articles/security-networking)

## ❓ **FAQ**

### Q: How do I report a bug?
A: Use our [bug report template](.github/ISSUE_TEMPLATE/bug-report.yml) on GitHub Issues.

### Q: Can I add a new VPN protocol?
A: Yes! Submit an RFC first to discuss the implementation approach.

### Q: How do I test my changes?
A: Run the full test suite and manually test on multiple devices/Android versions.

### Q: What if my PR is rejected?
A: Don't worry! We'll provide feedback to help improve your contribution.

---

**Thank you for contributing to MarFaNet! Together, we're building the future of secure, private internet access.** 🔒🌐