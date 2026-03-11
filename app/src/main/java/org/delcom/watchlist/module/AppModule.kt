package org.delcom.watchlist.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.delcom.watchlist.network.service.IWatchListAppContainer
import org.delcom.watchlist.network.service.IWatchListRepository
import org.delcom.watchlist.network.service.WatchListAppContainer
import org.delcom.watchlist.prefs.AuthTokenPref
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWatchListAppContainer(): IWatchListAppContainer = WatchListAppContainer()

    @Provides
    @Singleton
    fun provideWatchListRepository(container: IWatchListAppContainer): IWatchListRepository =
        container.repository

    @Provides
    fun provideAuthTokenPref(@ApplicationContext context: Context): AuthTokenPref =
        AuthTokenPref(context)
}
