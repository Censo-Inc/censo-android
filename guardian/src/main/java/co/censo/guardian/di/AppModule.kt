package co.censo.guardian.di

import android.content.Context
import co.censo.guardian.BuildConfig
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.GuardianRepositoryImpl
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.KeyRepositoryImpl
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.OwnerRepositoryImpl
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl
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
        return ApiService.create(
            storage = storage,
            context = applicationContext,
            versionCode = BuildConfig.VERSION_CODE.toString(),
            packageName = BuildConfig.APPLICATION_ID
        )
    }

    @Singleton
    @Provides
    fun providesKeyRepository(
        storage: Storage
    ): KeyRepository {
        return KeyRepositoryImpl(storage)
    }

    @Singleton
    @Provides
    fun provideOwnerRepository(
        apiService: ApiService,
        storage: Storage
    ): OwnerRepository {
        return OwnerRepositoryImpl(
            storage = storage,
            apiService = apiService
        )
    }

    @Singleton
    @Provides
    fun provideGuardianRepository(
        apiService: ApiService,
        storage: Storage
    ): GuardianRepository {
        return GuardianRepositoryImpl(
            apiService = apiService,
            storage = storage
        )
    }

    @Singleton
    @Provides
    fun providesPushRepository(
        api: ApiService,
        @ApplicationContext applicationContext: Context
    ): PushRepository {
        return PushRepositoryImpl(api, applicationContext)
    }
}