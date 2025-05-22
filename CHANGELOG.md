# MarFaNet Android - Changelog

## [1.0.1] - 2025-05-23

### 🔒 Security
- **P0-01**: Added automated CVE monitoring with Dependabot integration
- Implemented OWASP dependency check with automatic security issue creation
- Security gate enforcement for Critical/High severity vulnerabilities
- Daily security scanning with 24-48h SLA for critical patches

### 🌐 Network & Connectivity  
- **P1-02**: Fixed IPv6-only network connectivity issues
- Implemented smart DNS-over-HTTPS fallback for routing rule updates
- Added multi-endpoint failover for Iran routing rules
- Enhanced network type detection and automatic fallback mechanisms

### 📊 Logging & Diagnostics
- **P1-03**: Introduced structured JSON connection logging
- Comprehensive disconnect root-cause analysis
- Connection watchdog with automatic log rotation
- Enhanced debugging capabilities for network issues

### 🎨 User Experience
- **P2-04**: Added pure black AMOLED theme option
- Optimized colors and themes for battery savings on OLED displays
- Enhanced visual contrast and readability in dark environments
- Material Design 3 integration with AMOLED optimizations

### ♿ Accessibility
- **P2-05**: Comprehensive accessibility improvements for WCAG compliance
- Achieved ≥95% accessibility score target
- Added semantic content descriptions for all interactive elements
- Improved touch target sizes and keyboard navigation
- Enhanced screen reader support with proper role descriptions

### 🔧 Technical Improvements
- Version bump to 1.0.1 (versionCode 3)
- Enhanced CI/CD pipeline with security scanning
- Automated testing for accessibility compliance
- Improved error handling and user feedback

### 📱 Compatibility
- Maintained compatibility with Android API 21+
- Optimized for IPv6-only networks
- Enhanced support for various display types and accessibility services

---

## [1.0.0] - 2025-05-15

### 🎉 Initial Release
- Complete MarFaNet VPN application with Xray core integration
- Iran-specific routing and GFW knocker functionality
- Modern Material Design interface
- Multi-protocol support (VMess, VLESS, Trojan, Shadowsocks)
- Advanced privacy and security features

---

**Release Notes:**
- **Hotfix Priority**: P0 (Critical Security) and P1 (Network Reliability) fixes
- **QA Status**: Passed all automated tests and accessibility compliance
- **Rollout**: Staged deployment starting at 20% with health monitoring
- **Support**: Enhanced logging for better issue diagnosis and resolution