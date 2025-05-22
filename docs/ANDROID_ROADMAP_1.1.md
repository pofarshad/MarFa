# MarFaNet Android v1.1.0 Roadmap

## 🎯 **Strategic Focus: Android Excellence First**

**Decision**: Temporarily freeze cross-platform work to perfect the Android application before expanding to iOS/Desktop. This ensures a rock-solid foundation and maximizes user satisfaction on our primary platform.

## 🚨 **Immediate Patch 1.0.1 Tasks**

### **🔴 P0 - Critical Security**
- **CVE Patch Pipeline**: Continuous monitoring of Xray & BoringSSL vulnerabilities
- **48-hour Response SLA**: Automated hotfix deployment for Critical/High CVEs
- **Security Dashboard**: Real-time vulnerability scanning and alerting

### **🟠 P1 - High Priority Fixes**
- **IPv6 Routing Bug**: Add IPv4 fallback for rule updates in IPv6-only networks
- **Connection Watchdog**: Structured JSON logging for disconnect root-cause analysis
- **Stability Improvements**: Enhanced connection reliability monitoring

### **🟡 P2 - User Experience Polish**
- **Pure Black Theme**: AMOLED-optimized dark mode for battery savings
- **Android 14 Compatibility**: Contrast adjustments and API 34 optimizations
- **Accessibility Compliance**: Critical issues resolution from Accessibility Scanner

## 🚀 **Major Release v1.1.0 Features**

### **1. Split-Tunneling UI** 
**Goal**: Per-app VPN bypass with intuitive interface

**Implementation**:
- Visual app selector with search and filtering
- Smart defaults (bypass banking apps, include browsers)
- Rule persistence in Xray routing configuration
- Real-time rule testing and validation

**Acceptance**: Users can configure 3+ apps, settings persist across restarts

### **2. WireGuard Core Option**
**Goal**: Alternative high-performance protocol option

**Implementation**:
- Kernel-less WireGuard userspace integration
- Settings toggle: Xray (default) vs WireGuard mode
- Automatic protocol selection based on network conditions
- Performance benchmarking and user feedback

**Acceptance**: ≥30% throughput improvement on 5G networks

### **3. Real-Time Stats Overlay**
**Goal**: Live performance monitoring without leaving current app

**Implementation**:
- Floating overlay with permission management
- Real-time RTT, throughput, and data usage
- Customizable position and transparency
- Battery-optimized refresh rates

**Acceptance**: Overlay respects system permissions, minimal battery impact

### **4. Material You & Compose Migration**
**Goal**: Modern UI foundation for future features

**Implementation**:
- Migrate Home and Settings screens to Jetpack Compose
- Dynamic theming with Material You color system
- RTL language support and accessibility improvements
- Performance optimization and smooth animations

**Acceptance**: No performance regression, full RTL support

## 🔧 **Technical Infrastructure Upgrades**

### **Build System Modernization**
- **Android Gradle Plugin 9.x**: Android 15 compatibility
- **Kotlin 2.0**: K2 compiler adoption with performance benefits
- **Lint Baselining**: Only new violations fail builds

### **Development Workflow**
- **Feature Flags**: `experimental_*` preferences for safe rollouts
- **Automated Testing**: Enhanced coverage for new features
- **Performance Monitoring**: Real-time metrics and alerting

## 📅 **8-Week Development Timeline**

### **Weeks 1-2: Foundation & P0 Fixes**
- Implement CVE monitoring pipeline
- Fix IPv6 routing and connection logging
- Setup v1.1.0 development infrastructure

### **Weeks 3-4: Core Features Development**
- Split-tunneling UI implementation
- WireGuard integration foundation
- Real-time overlay development

### **Weeks 5-6: Polish & Integration**
- Material You/Compose migration
- Feature integration testing
- Performance optimization

### **Weeks 7-8: Beta Testing & Release**
- Closed beta rollout (2-week cycles)
- Metrics collection and analysis
- Production release preparation

## 📊 **Success Metrics & KPIs**

### **Quality Targets**
- **Crash-Free Sessions**: ≥98% (vs current 96.8%)
- **User Rating**: 4.6+ stars on Google Play Store
- **Performance**: Maintain existing KPI targets
- **Battery Life**: No regression from new features

### **Feature Adoption Goals**
- **Split-Tunneling**: ≥40% of active users within 30 days
- **WireGuard Mode**: ≥25% adoption on 5G devices
- **Real-Time Overlay**: ≥60% enable rate among power users

### **Technical Health**
- **Build Time**: ≤3 minutes for clean builds
- **APK Size**: ≤50MB for architecture-specific builds
- **Cold Start**: Maintain ≤1.2s target

## 🛡️ **Risk Mitigation**

### **Technical Risks**
- **Compose Migration**: Gradual rollout with fallback to Views
- **WireGuard Integration**: Extensive device compatibility testing
- **Feature Complexity**: Progressive disclosure and user education

### **Timeline Risks**
- **Scope Creep**: Strict feature freeze after Week 4
- **Quality Issues**: 2-week buffer built into timeline
- **External Dependencies**: Vendor coordination for critical libraries

## 🚪 **Cross-Platform Resume Criteria**

Resume iOS/Desktop development when:
1. **v1.1.0 Success**: All metrics targets achieved
2. **Issue Backlog**: ≤2 Critical/Major Android issues open
3. **User Satisfaction**: 4.6+ star rating maintained for 30 days
4. **Technical Debt**: Core infrastructure modernization complete

## 📋 **Resource Allocation**

| Role | Allocation | Focus Areas |
|------|------------|-------------|
| **Android Core Dev (×2)** | 100% | Feature development, technical debt |
| **UX Designer** | 40% | Material You migration, split-tunneling UI |
| **QA/Automation** | 60% | Feature testing, regression prevention |
| **DevOps** | 30% | CI/CD optimization, monitoring setup |
| **Product Manager** | 30% | Metrics tracking, user feedback analysis |

## 🎯 **Next Immediate Actions**

1. **Create GitHub Epic**: Split this roadmap into trackable issues
2. **Setup Feature Flags**: Implement experimental preferences system  
3. **CVE Monitoring**: Deploy security scanning automation
4. **Beta Track**: Configure closed testing with 500+ users
5. **Metrics Dashboard**: Real-time KPI tracking and alerting

---

**This Android-focused approach ensures MarFaNet becomes the gold standard VPN application before expanding to other platforms. Quality first, then scale!** 🏆