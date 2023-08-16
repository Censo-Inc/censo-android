package co.censo.vault.di

import android.content.Context
import co.censo.vault.CryptographyManager
import co.censo.vault.CryptographyManagerImpl
import co.censo.vault.storage.SharedPrefsStorage
import co.censo.vault.storage.Storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideCryptographyManager(): CryptographyManager {
        return CryptographyManagerImpl()
    }

    @Singleton
    @Provides
    fun provideStorage(@ApplicationContext applicationContext: Context) : Storage {
        SharedPrefsStorage.setup(applicationContext)
        return SharedPrefsStorage
    }
}
