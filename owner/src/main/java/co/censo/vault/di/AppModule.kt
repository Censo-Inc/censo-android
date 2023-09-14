package co.censo.vault.di

import android.content.Context
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.storage.SharedPrefsStorage
import co.censo.vault.BuildConfig
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.OwnerRepositoryImpl
import co.censo.vault.data.repository.FacetecRepository
import co.censo.vault.data.repository.FacetecRepositoryImpl
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.GuardianRepositoryImpl
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl
import co.censo.shared.data.storage.Storage
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
    fun provideInternalDeviceKey(): InternalDeviceKey {
        return InternalDeviceKey()
    }

    @Singleton
    @Provides
    fun provideStorage(@ApplicationContext applicationContext: Context): Storage {
        SharedPrefsStorage.setup(applicationContext)
        return SharedPrefsStorage
    }

    @Singleton
    @Provides
    fun provideApiService(
        storage: Storage,
        @ApplicationContext applicationContext: Context
    ): ApiService {
        return ApiService.create(storage, applicationContext, BuildConfig.VERSION_CODE.toString())
    }

    @Singleton
    @Provides
    fun provideOwnerRepository(
        apiService: ApiService,
    ): OwnerRepository {
        return OwnerRepositoryImpl(apiService)
    }

    @Singleton
    @Provides
    fun provideGuardianRepository(
        apiService: ApiService,
    ): GuardianRepository {
        return GuardianRepositoryImpl(apiService)
    }

    @Singleton
    @Provides
    fun providesPushRepository(
        api: ApiService,
        @ApplicationContext applicationContext: Context
    ): PushRepository {
        return PushRepositoryImpl(api, applicationContext)
    }

    @Singleton
    @Provides
    fun providesFacetecRepository(
        api: ApiService,
    ): FacetecRepository {
        return FacetecRepositoryImpl(api)
    }
}
