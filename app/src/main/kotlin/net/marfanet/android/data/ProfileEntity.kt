package net.marfanet.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for VPN profiles
 * Stores complete proxy configuration data
 */
@Entity(tableName = "profiles")
@Serializable
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val protocol: String, // vmess, vless, trojan, shadowsocks
    val serverAddress: String,
    val serverPort: Int,
    val userId: String,
    val password: String = "",
    val alterId: Int? = null,
    val security: String? = null,
    val network: String? = null, // tcp, ws, grpc
    val path: String? = null,
    val host: String? = null,
    val tls: Boolean? = null,
    val sni: String? = null,
    val allowInsecure: Boolean? = null,
    val subscriptionUrl: String? = null,
    val subscriptionGroup: String? = null,
    val latency: Long = -1, // -1 means not tested
    val lastConnected: Long = 0,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Subscription group entity
 */
@Entity(tableName = "subscription_groups")
@Serializable
data class SubscriptionGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val lastUpdated: Long = 0,
    val profileCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Profile statistics entity
 */
@Entity(tableName = "profile_stats")
@Serializable
data class ProfileStatsEntity(
    @PrimaryKey
    val profileId: String,
    val totalConnections: Int = 0,
    val totalUpload: Long = 0,
    val totalDownload: Long = 0,
    val averageLatency: Long = 0,
    val lastConnectionTime: Long = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0
)
