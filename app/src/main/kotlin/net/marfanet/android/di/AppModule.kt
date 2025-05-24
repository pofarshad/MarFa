package net.marfanet.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.marfanet.android.data.AppDatabase
import net.marfanet.android.data.ProfileDao
import net.marfanet.android.data.SubscriptionDao
import net.marfanet.android.data.ProfileStatsDao
import net.marfanet.android.data.AppRuleDao
import net.marfanet.android.logging.ConnectionLogger
import net.marfanet.android.stats.OptimizedStatsCollector
import net.marfanet.android.stats.VpnStatsCollector
import net.marfanet.android.vpn.ConnectionManager
import net.marfanet.android.xray.XrayCore
import net.marfanet.android.xray.XrayConfigBuilder
import javax.inject.Qualifier
import javax.inject.Singleton

// Define Qualifier
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideConnectionLogger(
        @ApplicationContext context: Context
    ): ConnectionLogger {
        return ConnectionLogger(context)
    }
    
    @Provides
    @Singleton
    fun provideOptimizedStatsCollector(
        @ApplicationContext context: Context
    ): OptimizedStatsCollector {
        return OptimizedStatsCollector(context)
    }
    
    @Provides
    @Singleton
    fun provideVpnStatsCollector(
        @ApplicationContext context: Context
    ): VpnStatsCollector {
        return VpnStatsCollector(context)
    }
    
    @Provides
    @Singleton
    fun provideXrayCore(
        @ApplicationContext context: Context
    ): XrayCore {
        return XrayCore(context)
    }
    
    @Provides
    @Singleton
    fun provideXrayConfigBuilder(): XrayConfigBuilder {
        return XrayConfigBuilder()
    }
    
    @Provides
    @Singleton
    fun provideConnectionManager(
        @ApplicationContext context: Context,
        logger: ConnectionLogger
    ): ConnectionManager {
        return ConnectionManager(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }
    
    @Provides
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }
    
    @Provides
    fun provideProfileStatsDao(database: AppDatabase): ProfileStatsDao {
        return database.statsDao()
    }
    
    @Provides
    fun provideAppRuleDao(database: AppDatabase): AppRuleDao {
        return database.appRuleDao()
    }

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
