package co.censo.censo.di

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
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl
import co.censo.shared.data.storage.CloudStorage
import co.censo.shared.data.storage.GoogleDriveStorage
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.data.storage.SecurePreferencesImpl
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.GoogleAuth
import co.censo.shared.util.VaultCountDownTimer
import co.censo.censo.BuildConfig
import co.censo.censo.data.repository.FacetecRepository
import co.censo.censo.data.repository.FacetecRepositoryImpl
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
    fun provideSecureStorage(@ApplicationContext applicationContext: Context): SecurePreferences {
        return SecurePreferencesImpl(applicationContext)
    }

    @Singleton
    @Provides
    fun provideCloudStorage(@ApplicationContext applicationContext: Context) : CloudStorage {
        return GoogleDriveStorage(applicationContext)
    }

    @Singleton
    @Provides
    fun provideAuthUtil(
        @ApplicationContext applicationContext: Context,
        secureStorage: SecurePreferences
    ): AuthUtil {
        return GoogleAuth(applicationContext, secureStorage)
    }

    @Singleton
    @Provides
    fun provideApiService(
        @ApplicationContext applicationContext: Context,
        authUtil: AuthUtil,
        secureStorage: SecurePreferences
    ): ApiService {
        return ApiService.create(
            context = applicationContext,
            versionCode = BuildConfig.VERSION_CODE.toString(),
            packageName = BuildConfig.APPLICATION_ID,
            authUtil = authUtil,
            secureStorage = secureStorage
        )
    }

    @Singleton
    @Provides
    fun provideOwnerRepository(
        apiService: ApiService,
        secureStorage: SecurePreferences,
        authUtil: AuthUtil,
        keyRepository: KeyRepository
    ): OwnerRepository {
        return OwnerRepositoryImpl(
            apiService = apiService,
            secureStorage = secureStorage,
            authUtil = authUtil,
            keyRepository = keyRepository
        )
    }

    @Singleton
    @Provides
    fun providesKeyRepository(
        storage: SecurePreferences,
        cloudStorage: CloudStorage
    ): KeyRepository {
        return KeyRepositoryImpl(storage, cloudStorage)
    }

    @Singleton
    @Provides
    fun provideGuardianRepository(
        apiService: ApiService,
        storage: SecurePreferences,
        keyRepository: KeyRepository
    ): GuardianRepository {
        return GuardianRepositoryImpl(
            secureStorage = storage,
            apiService = apiService,
            keyRepository = keyRepository
        )
    }

    @Singleton
    @Provides
    fun providesPushRepository(
        api: ApiService,
        secureStorage: SecurePreferences,
        @ApplicationContext applicationContext: Context
    ): PushRepository {
        return PushRepositoryImpl(api, secureStorage, applicationContext)
    }

    @Singleton
    @Provides
    fun providesFacetecRepository(
        api: ApiService,
    ): FacetecRepository {
        return FacetecRepositoryImpl(api)
    }

    @Provides
    fun provideCountdownTimer(): VaultCountDownTimer {
        return CountDownTimerImpl()
    }

    @Singleton
    @Provides
    fun providesOwnerStateFlow(): MutableStateFlow<Resource<OwnerState>> {
        return MutableStateFlow(Resource.Uninitialized)
    }
}
