# 📱 Get MarFaNet APK Now - No Release Required!

**Want to try MarFaNet before the official release? Here's how to get it right now!**

## 🚀 Current Status

- **Repository**: Ready with all Patch 1.0.1 improvements
- **CI/CD**: Fully configured and tested
- **Official Release**: Coming soon after 48-hour beta validation
- **APK Available**: **YES! Get it from GitHub Actions**

## 📥 Download APK Immediately (3 Steps)

### Step 1: Go to Actions
Visit the **Actions** tab in this repository: [Actions →](../../actions)

### Step 2: Find Latest Build  
Look for the most recent workflow run named:
- **"Build MarFaNet APK"** 
- **"Android Build • Patch 1.0.1"**
- Or any recent successful build

### Step 3: Download APK
1. Click on the workflow run
2. Scroll down to **Artifacts** section  
3. Download `marfanet-release-apk.zip`
4. Extract the ZIP to get your APK file

## 📱 Installation Guide

### Enable Installation
1. **Android Settings** → **Security** → **Install unknown apps**
2. Enable for your **File Manager** or **Browser**

### Install MarFaNet
1. Transfer APK to your Android device (API 21+)
2. Open file manager and tap the APK
3. Follow installation prompts
4. Launch and enjoy! 🎉

## 🔧 Alternative: Build Yourself

```bash
# Clone repository
git clone https://github.com/your-org/marfanet-android.git
cd marfanet-android

# Build release APK
./gradlew assembleRelease

# Your APK will be at:
# app/build/outputs/apk/release/app-release.apk
```

**Requirements**: Java 17, Android SDK (API 34)

## ⏰ When Will Official Release Be Available?

### Current Timeline:
- **Now**: APK available via GitHub Actions artifacts
- **Day 0-2**: 48-hour closed beta monitoring period
- **Day 2**: If metrics pass (crash-free ≥98%, ANR ≤0.47%), tag `v1.0.1` will be pushed
- **Day 2+**: Official GitHub Release with downloadable APK
- **Day 5**: Full production rollout (100% users)

### Quality Gates:
✅ **Security Scan**: No critical vulnerabilities  
✅ **Performance**: All benchmarks passed  
✅ **Accessibility**: 95%+ WCAG compliance  
✅ **Beta Testing**: Crash-free rate monitoring active  

## 🎯 What's in This Version?

### 🔒 Security Enhancements
- Automated CVE monitoring with 24h response
- Enhanced vulnerability scanning
- Security issue auto-creation system

### 🌐 Network Improvements  
- IPv6-only network connectivity fixes
- DNS-over-HTTPS fallback system
- Multi-endpoint routing reliability

### 🎨 User Experience
- Pure black AMOLED theme for battery savings
- Comprehensive accessibility improvements
- Enhanced connection diagnostics

### ⚡ Performance
- Faster connection establishment
- Improved memory management
- Battery optimization features

## 🛡️ Security Verification

Each APK includes:
- **SHA256 checksum** in build logs
- **Digital signature** for authenticity
- **Build information** with commit details

## 📞 Need Help?

- **Issues**: [Open an issue](../../issues) for problems
- **Questions**: [Start a discussion](../../discussions) for help
- **Updates**: Watch this repo for notifications

## 🎉 Stay Updated

- **GitHub Releases**: Coming very soon!
- **Telegram**: Check README for community channel
- **Repository**: Watch for release notifications

---

**Get MarFaNet now and experience the enhanced VPN app!** 🚀

*This guide is valid until the official v1.0.1 release is published*