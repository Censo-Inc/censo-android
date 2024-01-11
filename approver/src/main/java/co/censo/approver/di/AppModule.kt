package co.censo.approver.di

import android.content.Context
import co.censo.approver.BuildConfig
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.TotpGeneratorImpl
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.data.repository.ApproverRepositoryImpl
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.KeyRepositoryImpl
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.OwnerRepositoryImpl
import co.censo.shared.data.repository.PlayIntegrityRepository
import co.censo.shared.data.repository.PlayIntegrityRepositoryImpl
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
        secureStorage: SecurePreferences,
        playIntegrityRepository: PlayIntegrityRepository
    ): ApiService {
        return ApiService.create(
            context = applicationContext,
            versionCode = BuildConfig.VERSION_CODE.toString(),
            packageName = BuildConfig.APPLICATION_ID,
            authUtil = authUtil,
            secureStorage = secureStorage,
            playIntegrityRepository = playIntegrityRepository
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
        keyRepository: KeyRepository,
        totpGenerator: TotpGenerator
    ): OwnerRepository {
        return OwnerRepositoryImpl(
            apiService = apiService,
            secureStorage = secureStorage,
            authUtil = authUtil,
            keyRepository = keyRepository,
            totpGenerator = totpGenerator
        )
    }

    @Singleton
    @Provides
    fun provideApproverRepository(
        apiService: ApiService,
        authUtil: AuthUtil,
        secureStorage: SecurePreferences,
        keyRepository: KeyRepository,
        totpGenerator: TotpGenerator
    ): ApproverRepository {
        return ApproverRepositoryImpl(
            apiService = apiService,
            authUtil = authUtil,
            secureStorage = secureStorage,
            keyRepository = keyRepository,
            totpGenerator = totpGenerator
        )
    }

    @Provides
    fun provideCountdownTimer() : VaultCountDownTimer {
        return CountDownTimerImpl()
    }

    @Singleton
    @Provides
    fun providesPlayIntegrityRepository(
        @ApplicationContext applicationContext: Context
    ): PlayIntegrityRepository {
        return PlayIntegrityRepositoryImpl(applicationContext)
    }

    @Provides
    fun provideIODispatcher() : CoroutineDispatcher = Dispatchers.IO

    @Singleton
    @Provides
    fun provideTotpGenerator() : TotpGenerator {
        return TotpGeneratorImpl
    }
}