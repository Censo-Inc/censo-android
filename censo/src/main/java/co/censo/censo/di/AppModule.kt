package co.censo.censo.di

import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.data.repository.ApproverRepositoryImpl
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
import co.censo.censo.billing.BillingClientWrapper
import co.censo.censo.billing.BillingClientWrapperImpl
import co.censo.censo.data.repository.FacetecRepository
import co.censo.censo.data.repository.FacetecRepositoryImpl
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.TotpGeneratorImpl
import co.censo.shared.data.repository.PlayIntegrityRepository
import co.censo.shared.data.repository.PlayIntegrityRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    fun providesKeyRepository(
        storage: SecurePreferences,
        cloudStorage: CloudStorage
    ): KeyRepository {
        return KeyRepositoryImpl(storage, cloudStorage)
    }

    @Singleton
    @Provides
    fun provideApproverRepository(
        apiService: ApiService,
        authUtil: AuthUtil,
        storage: SecurePreferences,
        keyRepository: KeyRepository,
        totpGenerator: TotpGenerator
    ): ApproverRepository {
        return ApproverRepositoryImpl(
            secureStorage = storage,
            apiService = apiService,
            keyRepository = keyRepository,
            authUtil = authUtil,
            totpGenerator = totpGenerator
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
    fun providesPlayIntegrityRepository(
        @ApplicationContext applicationContext: Context
    ): PlayIntegrityRepository {
        return PlayIntegrityRepositoryImpl(applicationContext)
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
    fun providesKeyValidationTrigger(): MutableSharedFlow<String> {
        return MutableSharedFlow<String>()
    }

    @Singleton
    @Provides
    fun provideBillingManager(@ApplicationContext context: Context): BillingClientWrapper {
        // debug/release implementation is provided based on sourcesSet configuration
        return BillingClientWrapperImpl(context)
    }

    @Provides
    fun provideIODispatcher() : CoroutineDispatcher = Dispatchers.IO

    @Singleton
    @Provides
    fun provideTotpGenerator() : TotpGenerator {
        return TotpGeneratorImpl
    }
}
