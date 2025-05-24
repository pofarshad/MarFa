package net.marfanet.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for app-specific routing rules
 */
@Entity(tableName = "app_rules")
@Serializable
data class AppRule(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val action: String, // "direct", "proxy", "block"
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
