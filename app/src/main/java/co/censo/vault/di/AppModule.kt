package co.censo.vault.di

import android.content.Context
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.CryptographyManagerImpl
import co.censo.vault.data.repository.OwnerRepository
import co.censo.vault.data.repository.OwnerRepositoryImpl
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl
import co.censo.vault.data.storage.SharedPrefsStorage
import co.censo.vault.data.storage.Storage
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
    fun provideStorage(@ApplicationContext applicationContext: Context): Storage {
        SharedPrefsStorage.setup(applicationContext)
        return SharedPrefsStorage
    }

    @Singleton
    @Provides
    fun provideApiService(cryptographyManager: CryptographyManager, storage: Storage, @ApplicationContext applicationContext: Context): ApiService {
        return ApiService.create(cryptographyManager, storage, applicationContext)
    }

    @Singleton
    @Provides
    fun provideOwnerRepository(apiService: ApiService): OwnerRepository {
        return OwnerRepositoryImpl(apiService)
    }

    @Singleton
    @Provides
    fun providesPushRepository(api: ApiService, @ApplicationContext applicationContext: Context) : PushRepository {
        return PushRepositoryImpl(api, applicationContext)
    }
}
