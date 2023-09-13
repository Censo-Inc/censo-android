package co.censo.guardian.di

import android.content.Context
import co.censo.guardian.BuildConfig
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.GuardianRepositoryImpl
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.OwnerRepositoryImpl
import co.censo.shared.data.storage.SharedPrefsStorage
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
        storage: Storage,
    ): OwnerRepository {
        return OwnerRepositoryImpl(apiService, storage)
    }

    @Singleton
    @Provides
    fun provideGuardianRepository(
        apiService: ApiService,
    ): GuardianRepository {
        return GuardianRepositoryImpl(apiService)
    }
}