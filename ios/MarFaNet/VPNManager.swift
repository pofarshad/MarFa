import Foundation
import NetworkExtension
import shared

/**
 * MarFaNet iOS VPN Manager
 * Integrates with Kotlin Multiplatform shared core
 */
@available(iOS 9.0, *)
public class iOSVPNManager: NSObject, ObservableObject {
    
    private let sharedCore = SharedVPNCore()
    private var vpnManager: NEVPNManager?
    
    @Published var connectionState: NEVPNStatus = .invalid
    @Published var connectionStats: ConnectionStats?
    
    private let notificationCenter = NotificationCenter.default
    
    override init() {
        super.init()
        setupVPNManager()
        observeVPNStatusChanges()
    }
    
    // MARK: - VPN Setup
    
    private func setupVPNManager() {
        NEVPNManager.shared().loadFromPreferences { [weak self] error in
            if let error = error {
                print("Failed to load VPN preferences: \(error.localizedDescription)")
                return
            }
            
            self?.vpnManager = NEVPNManager.shared()
            self?.updateConnectionState()
        }
    }
    
    // MARK: - Connection Management
    
    func connect(with config: VPNConfig) async throws {
        guard let vpnManager = vpnManager else {
            throw VPNError.managerNotReady
        }
        
        // Update shared core state
        sharedCore.updateConnectionState(state: .connecting)
        
        // Generate Xray configuration using shared logic
        let xrayConfig = try await sharedCore.generateXrayConfig(vpnConfig: config)
        
        // Configure NEPacketTunnelProvider
        let providerProtocol = NEPacketTunnelProviderProtocol()
        providerProtocol.providerBundleIdentifier = "net.marfanet.ios.tunnel"
        providerProtocol.serverAddress = config.serverAddress
        
        // Pass configuration to tunnel provider
        providerProtocol.providerConfiguration = [
            "xray_config": xrayConfig,
            "server_address": config.serverAddress,
            "server_port": config.serverPort,
            "protocol": config.protocol,
            "user_id": config.userId
        ]
        
        // Configure VPN settings
        vpnManager.protocolConfiguration = providerProtocol
        vpnManager.localizedDescription = "MarFaNet VPN"
        vpnManager.isEnabled = true
        
        // Save configuration
        try await vpnManager.saveToPreferences()
        
        // Start VPN connection
        try vpnManager.connection.startVPNTunnel()
        
        // Update shared state
        sharedCore.updateConnectionState(state: .connected)
    }
    
    func disconnect() {
        vpnManager?.connection.stopVPNTunnel()
        sharedCore.updateConnectionState(state: .disconnected)
    }
    
    // MARK: - Status Monitoring
    
    private func observeVPNStatusChanges() {
        notificationCenter.addObserver(
            forName: .NEVPNStatusDidChange,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.updateConnectionState()
        }
    }
    
    private func updateConnectionState() {
        guard let vpnManager = vpnManager else { return }
        
        connectionState = vpnManager.connection.status
        
        // Sync with shared core
        let sharedState: shared.ConnectionState
        switch connectionState {
        case .connected:
            sharedState = .connected
        case .connecting, .reasserting:
            sharedState = .connecting
        case .disconnecting:
            sharedState = .disconnecting
        case .disconnected, .invalid:
            sharedState = .disconnected
        @unknown default:
            sharedState = .error
        }
        
        sharedCore.updateConnectionState(state: sharedState)
        
        // Update statistics
        Task {
            connectionStats = try? await sharedCore.generateConnectionStats()
        }
    }
    
    // MARK: - Statistics
    
    func getConnectionStats() async -> ConnectionStats? {
        return try? await sharedCore.generateConnectionStats()
    }
    
    // MARK: - Configuration Validation
    
    func validateConfig(_ config: VPNConfig) throws {
        do {
            _ = sharedCore.validateConfig(config: config)
        } catch {
            throw VPNError.invalidConfiguration(error.localizedDescription)
        }
    }
}

// MARK: - Error Types

enum VPNError: LocalizedError {
    case managerNotReady
    case invalidConfiguration(String)
    case connectionFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .managerNotReady:
            return "VPN manager is not ready"
        case .invalidConfiguration(let message):
            return "Invalid configuration: \(message)"
        case .connectionFailed(let message):
            return "Connection failed: \(message)"
        }
    }
}