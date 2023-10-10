package co.censo.vault.di

import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.GuardianRepositoryImpl
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.KeyRepositoryImpl
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.OwnerRepositoryImpl
import co.censo.shared.data.storage.SharedPrefsStorage
import co.censo.shared.data.storage.Storage
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.vault.BuildConfig
import co.censo.vault.data.repository.FacetecRepository
import co.censo.vault.data.repository.FacetecRepositoryImpl
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun provideOwnerRepository(
        apiService: ApiService,
        storage: Storage
    ): OwnerRepository {
        return OwnerRepositoryImpl(
            apiService = apiService,
            storage = storage
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
    fun provideGuardianRepository(
        apiService: ApiService,
        storage: Storage
    ): GuardianRepository {
        return GuardianRepositoryImpl(
            storage = storage,
            apiService = apiService
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

    @Singleton
    @Provides
    fun providesFacetecRepository(
        api: ApiService,
    ): FacetecRepository {
        return FacetecRepositoryImpl(api)
    }

    @Provides
    fun provideCountdownTimer() : VaultCountDownTimer {
        return CountDownTimerImpl()
    }

    @Singleton
    @Provides
    fun providesOwnerStateFlow(): MutableStateFlow<Resource<OwnerState>> {
        return MutableStateFlow(Resource.Uninitialized)
    }
}
