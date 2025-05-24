package net.marfanet.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.marfanet.android.MainActivity
import net.marfanet.android.R
import net.marfanet.android.data.ProfileEntity
import net.marfanet.android.logging.ConnectionLogger
import net.marfanet.android.stats.VpnStatsCollector
import net.marfanet.android.xray.XrayConfigBuilder
import net.marfanet.android.xray.XrayCore
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Connection information data class
 */
data class ConnectionInfo(
    val state: VpnConnectionState,
    val profileName: String? = null,
    val serverAddress: String? = null,
    val protocol: String? = null,
    val latency: Long? = null,
    val connectedAt: Long? = null,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0
)

/**
 * MarFa VPN Service
 * Main VPN service that manages Xray core and network tunnel
 */
