package co.censo.approver.di

import android.content.Context
import co.censo.approver.BuildConfig
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
    fun provideSecureStorage(@ApplicationContext  applicationContext: Context) : SecurePreferences {
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
    fun providesKeyRepository(
        secureStorage: SecurePreferences,
        cloudStorage: CloudStorage
    ): KeyRepository {
        return KeyRepositoryImpl(secureStorage, cloudStorage)
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
    fun provideGuardianRepository(
        apiService: ApiService,
        secureStorage: SecurePreferences,
        keyRepository: KeyRepository
    ): GuardianRepository {
        return GuardianRepositoryImpl(
            apiService = apiService,
            secureStorage = secureStorage,
            keyRepository = keyRepository
        )
    }

    @Singleton
    @Provides
    fun providesPushRepository(
        api: ApiService,
        @ApplicationContext applicationContext: Context,
        secureStorage: SecurePreferences
    ): PushRepository {
        return PushRepositoryImpl(
            api = api,
            applicationContext = applicationContext,
            secureStorage = secureStorage
        )
    }

    @Provides
    fun provideCountdownTimer() : VaultCountDownTimer {
        return CountDownTimerImpl()
    }
}