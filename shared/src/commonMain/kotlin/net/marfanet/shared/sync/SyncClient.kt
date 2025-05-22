package net.marfanet.shared.sync

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.datetime.*

/**
 * MarFaNet Sync Client - Kotlin Multiplatform
 * Provides offline-first configuration synchronization across devices
 */
class SyncClient(
    private val deviceId: String,
    private val syncEndpoint: String = "https://sync.marfanet.com/v1",
    private val storage: LocalSyncStorage
) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var lastSyncTime: Instant? = null
    
    companion object {
        const val SYNC_VERSION = "1.0.0"
        const val ENCRYPTION_ALGORITHM = "XChaCha20-Poly1305"
    }
    
    /**
     * Authenticate device and obtain sync tokens
     */
    suspend fun authenticate(devicePublicKey: String, proofOfPossession: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = AuthRequest(
                    deviceId = deviceId,
                    publicKey = devicePublicKey,
                    proofOfPossession = proofOfPossession
                )
                
                // Make actual API call to sync service
                val response = performAuthentication(request)
                
                // Store tokens securely
                accessToken = response.accessToken
                refreshToken = response.refreshToken
                
                storage.storeAuthTokens(response.accessToken, response.refreshToken)
                
                AuthResult.Success(response)
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    /**
     * Upload local changes to cloud (offline-first)
     */
    suspend fun syncUp(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                // Get local changes since last sync
                val localManifest = storage.getLocalManifest()
                val pendingChanges = storage.getPendingChanges()
                
                if (pendingChanges.isEmpty()) {
                    return@withContext SyncResult.Success("No changes to sync")
                }
                
                // Encrypt manifest
                val encryptedEnvelope = encryptManifest(localManifest)
                
                // Upload to server
                val uploadResult = uploadManifest(encryptedEnvelope)
                
                if (uploadResult.success) {
                    // Mark changes as synced
                    storage.markChangesSynced(pendingChanges)
                    lastSyncTime = Clock.System.now()
                    storage.updateLastSyncTime(lastSyncTime!!)
                    
                    SyncResult.Success("Synced ${pendingChanges.size} changes")
                } else {
                    SyncResult.Error("Upload failed: ${uploadResult.message}")
                }
                
            } catch (e: Exception) {
                // Store for retry when online
                storage.queueForRetry(deviceId, Clock.System.now())
                SyncResult.Error(e.message ?: "Sync up failed")
            }
        }
    }
    
    /**
     * Download remote changes from cloud
     */
    suspend fun syncDown(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val since = lastSyncTime ?: Instant.DISTANT_PAST
                
                // Download manifests from server
                val manifests = downloadManifests(since)
                
                if (manifests.isEmpty()) {
                    return@withContext SyncResult.Success("No remote changes")
                }
                
                var appliedChanges = 0
                val conflicts = mutableListOf<ConflictInfo>()
                
                for (envelope in manifests) {
                    // Decrypt and apply manifest
                    val decryptedManifest = decryptManifest(envelope)
                    
                    val mergeResult = storage.mergeRemoteManifest(decryptedManifest)
                    when (mergeResult) {
                        is MergeResult.Success -> appliedChanges += mergeResult.changesApplied
                        is MergeResult.Conflict -> conflicts.addAll(mergeResult.conflicts)
                        is MergeResult.Error -> throw Exception(mergeResult.message)
                    }
                }
                
                lastSyncTime = Clock.System.now()
                storage.updateLastSyncTime(lastSyncTime!!)
                
                if (conflicts.isNotEmpty()) {
                    SyncResult.ConflictsDetected(conflicts, appliedChanges)
                } else {
                    SyncResult.Success("Applied $appliedChanges remote changes")
                }
                
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Sync down failed")
            }
        }
    }
    
    /**
     * Full bidirectional sync (up then down)
     */
    suspend fun fullSync(): SyncResult {
        val syncUpResult = syncUp()
        if (syncUpResult is SyncResult.Error) {
            return syncUpResult
        }
        
        val syncDownResult = syncDown()
        
        return when {
            syncDownResult is SyncResult.Error -> syncDownResult
            syncDownResult is SyncResult.ConflictsDetected -> syncDownResult
            else -> SyncResult.Success("Full sync completed successfully")
        }
    }
    
    /**
     * Get current sync status
     */
    suspend fun getSyncStatus(): SyncStatus {
        val pendingChanges = storage.getPendingChanges().size
        val lastSync = lastSyncTime
        val conflicts = storage.getUnresolvedConflicts()
        
        return SyncStatus(
            isAuthenticated = accessToken != null,
            lastSyncTime = lastSync,
            pendingChanges = pendingChanges,
            unresolvedConflicts = conflicts.size,
            syncEnabled = true,
            isOnline = checkConnectivity()
        )
    }
    
    // Platform-specific implementations will be provided via expect/actual
    private suspend fun performAuthentication(request: AuthRequest): AuthResponse {
        // This will connect to actual sync service
        throw NotImplementedError("Platform-specific authentication implementation required")
    }
    
    private suspend fun uploadManifest(envelope: SyncEnvelope): UploadResult {
        // This will make actual HTTP request
        throw NotImplementedError("Platform-specific upload implementation required")
    }
    
    private suspend fun downloadManifests(since: Instant): List<SyncEnvelope> {
        // This will make actual HTTP request
        throw NotImplementedError("Platform-specific download implementation required")
    }
    
    private suspend fun encryptManifest(manifest: DeviceManifest): SyncEnvelope {
        // This will use actual XChaCha20-Poly1305 encryption
        throw NotImplementedError("Platform-specific encryption implementation required")
    }
    
    private suspend fun decryptManifest(envelope: SyncEnvelope): DeviceManifest {
        // This will use actual XChaCha20-Poly1305 decryption
        throw NotImplementedError("Platform-specific decryption implementation required")
    }
    
    private suspend fun checkConnectivity(): Boolean {
        // This will check actual network connectivity
        throw NotImplementedError("Platform-specific connectivity check required")
    }
}

// Data classes for sync operations

@Serializable
data class AuthRequest(
    val deviceId: String,
    val publicKey: String,
    val proofOfPossession: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val syncEndpoint: String
)

@Serializable
data class SyncEnvelope(
    val deviceId: String,
    val encryptionAlgorithm: String,
    val encryptedPayload: ByteArray,
    val nonce: ByteArray,
    val authTag: ByteArray,
    val createdAt: Instant,
    val payloadHash: String
)

@Serializable
data class DeviceManifest(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val appVersion: String,
    val lastSync: Instant,
    val profiles: List<VPNProfile>,
    val preferences: UserPreferences,
    val usageStats: List<UsageStatistic>
)

@Serializable
data class VPNProfile(
    val profileId: String,
    val name: String,
    val protocol: String,
    val serverAddress: String,
    val serverPort: Int,
    val userId: String,
    val encryptedCredentials: ByteArray,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class UserPreferences(
    val autoConnect: Boolean = false,
    val preferredProtocol: String = "vmess",
    val killSwitch: Boolean = true,
    val bypassLan: Boolean = true,
    val theme: String = "auto",
    val language: String = "en"
)

@Serializable
data class UsageStatistic(
    val sessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val bytesSent: Long,
    val bytesReceived: Long,
    val serverUsed: String,
    val protocolUsed: String
)

data class UploadResult(
    val success: Boolean,
    val message: String,
    val serverTimestamp: Instant,
    val conflictCount: Int
)

data class ConflictInfo(
    val profileId: String,
    val localVersion: VPNProfile,
    val remoteVersion: VPNProfile,
    val conflictType: String
)

data class SyncStatus(
    val isAuthenticated: Boolean,
    val lastSyncTime: Instant?,
    val pendingChanges: Int,
    val unresolvedConflicts: Int,
    val syncEnabled: Boolean,
    val isOnline: Boolean
)

// Result types
sealed class AuthResult {
    data class Success(val response: AuthResponse) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class ConflictsDetected(val conflicts: List<ConflictInfo>, val appliedChanges: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

sealed class MergeResult {
    data class Success(val changesApplied: Int) : MergeResult()
    data class Conflict(val conflicts: List<ConflictInfo>) : MergeResult()
    data class Error(val message: String) : MergeResult()
}

// Platform-specific storage interface
interface LocalSyncStorage {
    suspend fun getLocalManifest(): DeviceManifest
    suspend fun getPendingChanges(): List<String>
    suspend fun markChangesSynced(changeIds: List<String>)
    suspend fun updateLastSyncTime(time: Instant)
    suspend fun storeAuthTokens(accessToken: String, refreshToken: String)
    suspend fun getDeviceEncryptionKey(): ByteArray
    suspend fun mergeRemoteManifest(manifest: DeviceManifest): MergeResult
    suspend fun getUnresolvedConflicts(): List<ConflictInfo>
    suspend fun queueForRetry(deviceId: String, timestamp: Instant)
}