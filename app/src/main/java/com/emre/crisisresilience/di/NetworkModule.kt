package com.emre.crisisresilience.di

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import com.emre.crisisresilience.data.local.dao.MessageDao
import com.emre.crisisresilience.data.network.wifi.P2pSocketManager
import com.emre.crisisresilience.data.network.wifi.WifiDirectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideWifiP2pManager(@ApplicationContext context: Context): WifiP2pManager {
        return context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }

    @Provides
    @Singleton
    fun provideWifiP2pChannel(
        @ApplicationContext context: Context,
        manager: WifiP2pManager
    ): WifiP2pManager.Channel {
        return manager.initialize(context, context.mainLooper, null)
    }

    @Provides
    @Singleton
    fun provideP2pSocketManager(messageDao: MessageDao): P2pSocketManager {
        return P2pSocketManager(messageDao)
    }

    @Provides
    @Singleton
    fun provideWifiDirectManager(
        @ApplicationContext context: Context,
        manager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        p2pSocketManager: P2pSocketManager
    ): WifiDirectManager {
        val wifiDirectManager = WifiDirectManager(context, manager, channel)
        wifiDirectManager.p2pSocketManager = p2pSocketManager
        return wifiDirectManager
    }

    @Provides
    @Singleton
    fun provideBleManager(@ApplicationContext context: Context): com.emre.crisisresilience.data.network.bluetooth.BleManager {
        return com.emre.crisisresilience.data.network.bluetooth.BleManager(context)
    }
}
