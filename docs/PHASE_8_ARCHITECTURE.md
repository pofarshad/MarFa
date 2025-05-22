# Phase 8 Architecture: Cross-Platform Expansion

## 🎯 **Strategic Vision**

Transform MarFaNet from Android-only to a **unified multi-platform ecosystem** with:
- **Cross-platform clients** (iOS, macOS, Windows, Linux)
- **Secure cloud synchronization** across all devices
- **Public APIs** for automation and integration
- **Browser extensions** for quick access

## 🏗️ **Technology Stack Decision**

### **Approach: Kotlin Multiplatform Mobile (KMM) + Native UI**

#### **Rationale**
- **Shared Business Logic**: 70% code reuse across platforms
- **Native Performance**: Platform-specific UI for optimal UX
- **Existing Codebase**: Leverage current Android Kotlin investment
- **Team Expertise**: Minimal learning curve for Android team

#### **Architecture Stack**
```
┌─────────────────────────────────────────────────────────────┐
│                     Platform UI Layer                       │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│  Android    │    iOS      │   Desktop   │   Browser Ext   │
│  (Compose)  │ (SwiftUI)   │ (Compose)   │   (React)       │
├─────────────┴─────────────┴─────────────┴─────────────────┤
│              Kotlin Multiplatform Shared                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  • VPN Core Logic (Xray integration)               │   │
│  │  • Network Layer (HTTP client, WebSocket)          │   │
│  │  │  • Configuration Management                     │   │
│  │  • Encryption/Decryption                           │   │
│  │  • Cloud Sync Protocol                             │   │
│  │  • Analytics & Telemetry                           │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                   Platform Services                         │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│  Android    │    iOS      │   Desktop   │   Cloud APIs    │
│  Services   │ Network Ext │  TUN/TAP    │   (Go/Rust)     │
└─────────────┴─────────────┴─────────────┴─────────────────┘
```

## 🔒 **Cloud Sync Security Architecture**

### **End-to-End Encryption Protocol**
```
User Device A                Cloud Storage              User Device B
     │                            │                           │
     │ 1. Generate E2E Key        │                           │
     │    (XChaCha20-Poly1305)    │                           │
     │                            │                           │
     │ 2. Encrypt Config Data     │                           │
     │    + HMAC Authentication   │                           │
     │                            │                           │
     │ 3. Upload Encrypted Blob ──┼──► Store Encrypted Data   │
     │                            │                           │
     │                            │ ◄── Download Encrypted ───│ 4. Fetch & Decrypt
     │                            │         Data               │    on Device B
```

### **Key Management**
- **Master Key**: Derived from user passphrase (Argon2id)
- **Device Keys**: Unique per device, rotated monthly
- **Zero-Knowledge**: Server cannot decrypt user data
- **Forward Secrecy**: Past data secure if current keys compromised

## 📱 **Platform-Specific Implementation**

### **iOS Implementation**
```swift
// iOS-specific VPN integration
class iOSVPNManager: VPNManagerProtocol {
    private let tunnelProvider = NEPacketTunnelProvider()
    private let sharedCore = SharedVPNCore() // KMM
    
    func connect(config: VPNConfig) async throws {
        let nativeConfig = try await sharedCore.processConfig(config)
        try await tunnelProvider.startTunnel(options: nativeConfig)
    }
}
```

### **Desktop Implementation (Compose Multiplatform)**
```kotlin
// Desktop UI with Compose Multiplatform
@Composable
fun DesktopMainScreen() {
    MaterialTheme {
        SharedVPNScreen( // Shared UI component
            onConnect = { config -> 
                nativeVPNManager.connect(config) // Platform-specific
            }
        )
    }
}
```

### **Browser Extension (Manifest V3)**
```javascript
// Chrome extension integration
chrome.action.onClicked.addListener(async (tab) => {
    const response = await fetch('https://api.marfanet.com/v1/quick-connect', {
        headers: { 'Authorization': `Bearer ${apiKey}` }
    });
    // Toggle VPN via native messaging
});
```

## 🌐 **Public API Architecture**

### **REST API Endpoints**
```yaml
openapi: 3.0.3
info:
  title: MarFaNet Config API
  version: 1.0.0

paths:
  /v1/configs:
    get:
      summary: List user configurations
      responses:
        200:
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/VPNConfig'
                  
  /v1/nodes/health:
    get:
      summary: Get server health status
      responses:
        200:
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/NodeHealth'
```

### **gRPC Service Definition**
```protobuf
service MarFaNetAPI {
    rpc GetConfigs(GetConfigsRequest) returns (GetConfigsResponse);
    rpc SyncDevices(SyncDevicesRequest) returns (SyncDevicesResponse);
    rpc StreamTelemetry(stream TelemetryEvent) returns (TelemetryResponse);
}

message VPNConfig {
    string id = 1;
    string name = 2;
    string protocol = 3; // vmess, vless, trojan, etc.
    bytes encrypted_config = 4;
    int64 created_at = 5;
}
```

## 🔧 **Development Workflow**

### **Multi-Platform CI/CD**
```yaml
# .github/workflows/multiplatform.yml
strategy:
  matrix:
    platform: [android, ios, desktop-windows, desktop-macos, desktop-linux]
    
steps:
- name: Build Platform
  run: |
    case ${{ matrix.platform }} in
      android) ./gradlew assembleRelease ;;
      ios) xcodebuild -scheme MarFaNet -destination generic/platform=iOS ;;
      desktop-*) ./gradlew packageDistributionForCurrentOS ;;
    esac
```

### **Shared Testing Strategy**
```kotlin
// Shared test suite
class SharedVPNCoreTest {
    @Test
    fun testConfigurationParsing() {
        val config = SharedVPNCore.parseConfig(testConfigJson)
        assertEquals("vmess", config.protocol)
        assertTrue(config.isValid())
    }
}
```

## 📊 **Performance Targets**

### **Platform-Specific KPIs**
| Platform | Cold Start | Memory | Battery | Network |
|----------|------------|--------|---------|---------|
| **Android** | ≤1.2s | ≤180MB | ≤2%/h | Baseline |
| **iOS** | ≤1.5s | ≤150MB | ≤1.5%/h | +5% efficiency |
| **Desktop** | ≤2.0s | ≤200MB | N/A | +10% throughput |
| **Browser** | ≤0.5s | ≤50MB | N/A | Proxy only |

### **Sync Performance**
- **Sync Latency**: ≤2 seconds for config updates
- **Offline Support**: 7 days cached operation
- **Bandwidth**: ≤1MB monthly sync traffic
- **Encryption**: ≤100ms encrypt/decrypt time

## 🛡️ **Security Considerations**

### **App Store Compliance**
- **iOS**: Use NEPacketTunnelProvider with business justification
- **macOS**: Notarization + System Extension approval
- **Windows**: Code signing + SmartScreen whitelist
- **Chrome**: Manifest V3 compliance + privacy policy

### **API Security**
- **OAuth 2.1**: PKCE for mobile clients
- **Rate Limiting**: 1000 requests/hour/user
- **Audit Logging**: All API calls logged with retention
- **CORS**: Strict origin policies for browser requests

## 🚀 **Rollout Strategy**

### **Phase 8 Milestones**
1. **M1**: iOS MVP (2 weeks) - Connect/disconnect only
2. **M2**: Desktop MVP (2 weeks) - Windows/macOS support
3. **M3**: Cloud Sync Alpha (3 weeks) - E2E encryption
4. **M4**: Public API Beta (2 weeks) - REST + gRPC
5. **M5**: Browser Extension (1 week) - Chrome/Firefox
6. **M6**: Multi-platform Beta (1 week) - Public testing

### **Success Metrics**
- **User Adoption**: 25% of Android users try other platforms
- **Sync Usage**: 60% of multi-device users enable sync
- **API Usage**: 100+ third-party integrations
- **Performance**: All platform KPIs met

---

**This architecture provides a solid foundation for MarFaNet's evolution into a comprehensive multi-platform VPN ecosystem while maintaining security, performance, and user experience standards.**