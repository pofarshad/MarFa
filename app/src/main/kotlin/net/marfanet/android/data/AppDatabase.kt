package net.marfanet.android.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import net.marfanet.android.data.ProfileDao
import net.marfanet.android.data.SubscriptionDao
import net.marfanet.android.data.ProfileStatsDao
import net.marfanet.android.data.AppRuleDao

/**
 * Room database for MarFa VPN
 * Stores profiles, subscription groups, and statistics
 */
@Database(
    entities = [
        ProfileEntity::class,
        SubscriptionGroupEntity::class,
        ProfileStatsEntity::class,
        AppRule::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun profileDao(): ProfileDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun statsDao(): ProfileStatsDao
    abstract fun appRuleDao(): AppRuleDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marfa_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
