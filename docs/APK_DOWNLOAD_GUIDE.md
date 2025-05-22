# 📱 MarFaNet APK Download Guide

**Get your MarFaNet APK right now - no waiting for releases!**

## 🚀 Quick Download (Recommended)

### Method 1: GitHub Actions Artifacts
1. **Go to Actions**: Visit the [Actions tab](../../actions) in this repository
2. **Find Latest Build**: Click on the most recent "Build MarFaNet APK" workflow run
3. **Download APK**: Scroll to "Artifacts" section and download `marfanet-release-apk`
4. **Extract & Install**: Unzip the file and install the APK on your Android device

### Method 2: Manual Build Trigger
1. **Go to Actions**: Visit the [Actions tab](../../actions)
2. **Select Workflow**: Click "Build MarFaNet APK" from the left sidebar
3. **Run Workflow**: Click "Run workflow" button, select "release", then click "Run workflow"
4. **Wait & Download**: Wait ~5-10 minutes for build completion, then download the artifact

## 📱 Installation Instructions

### Enable Unknown Apps
1. Open **Settings** on your Android device
2. Go to **Security** (or **Privacy & Security**)
3. Enable **Install unknown apps** for your file manager or browser

### Install MarFaNet
1. Transfer the APK file to your device
2. Open your file manager and locate the APK
3. Tap the APK file to begin installation
4. Follow the on-screen prompts
5. Launch MarFaNet and enjoy! 🎉

## 🔧 Build It Yourself (Advanced)

If you prefer to build from source:

```bash
# Clone the repository
git clone https://github.com/your-org/marfanet-android.git
cd marfanet-android

# Build release APK
./gradlew assembleRelease

# Find your APK at:
# app/build/outputs/apk/release/app-release.apk
```

**Requirements**: Java 17, Android SDK (API 34), Git

## 🛡️ Security & Verification

### APK Verification
- All APKs include SHA256 checksums in the build info
- Compare checksums to verify file integrity
- Only download from official GitHub Actions or releases

### Permissions
MarFaNet requires these permissions:
- **Network Access**: For VPN functionality
- **VPN Service**: To create secure tunnels
- **Storage**: For configuration and logs
- **Notification**: For connection status updates

## 🎯 What's New in Latest Build

**Version 1.0.1** includes:
- 🔒 **Enhanced Security**: Automated vulnerability monitoring
- 🌐 **Better Connectivity**: IPv6 network support improvements
- 📊 **Improved Diagnostics**: Detailed connection logging
- 🎨 **AMOLED Theme**: Pure black theme for battery savings
- ♿ **Accessibility**: Enhanced screen reader support

## 📞 Need Help?

- **Issues**: Open a [GitHub issue](../../issues) for bug reports
- **Questions**: Start a [discussion](../../discussions) for general questions
- **Updates**: Watch this repository for release notifications

## 🔮 Coming Soon

**Official Release (v1.0.1)** will be available soon with:
- Direct download from GitHub Releases
- Google Play Store distribution
- Automatic update notifications
- Enhanced stability after beta testing

---

**Happy browsing with MarFaNet!** 🌟

*Last updated: May 22, 2025*