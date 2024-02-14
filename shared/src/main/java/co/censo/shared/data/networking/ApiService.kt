package co.censo.shared.data.networking

import InitBiometryVerificationApiResponse
import SeedPhraseId
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import co.censo.shared.BuildConfig
import co.censo.shared.data.Header
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.sha256digest
import co.censo.shared.data.maintenance.GlobalMaintenanceState
import co.censo.shared.data.model.AcceptApprovershipApiResponse
import co.censo.shared.data.model.AcceptAuthenticationResetRequestApiResponse
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiRequest
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiResponse
import co.censo.shared.data.model.ActivateBeneficiaryApiRequest
import co.censo.shared.data.model.ActivateBeneficiaryApiResponse
import co.censo.shared.data.model.ApproveAccessApiRequest
import co.censo.shared.data.model.ApproveAccessApiResponse
import co.censo.shared.data.model.ApproveTakeoverInitiationApiRequest
import co.censo.shared.data.model.ApproveTakeoverInitiationApiResponse
import co.censo.shared.data.model.AttestationChallengeResponse
import co.censo.shared.data.model.AuthenticationResetApprovalId
import co.censo.shared.data.model.CancelAuthenticationResetApiResponse
import co.censo.shared.data.model.CancelTakeoverApiResponse
import co.censo.shared.data.model.CompleteOwnerApprovershipApiRequest
import co.censo.shared.data.model.CompleteOwnerApprovershipApiResponse
import co.censo.shared.data.model.ConfirmApprovershipApiRequest
import co.censo.shared.data.model.ConfirmApprovershipApiResponse
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiRequest
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.DeletePolicySetupApiResponse
import co.censo.shared.data.model.DeleteSeedPhraseApiResponse
import co.censo.shared.data.model.FinalizeTakeoverApiRequest
import co.censo.shared.data.model.FinalizeTakeoverApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.GetImportEncryptedDataApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.GetSeedPhraseApiResponse
import co.censo.shared.data.model.InitiateAccessApiRequest
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.InitiateAuthenticationResetApiResponse
import co.censo.shared.data.model.InitiateTakeoverApiResponse
import co.censo.shared.data.model.InviteBeneficiaryApiRequest
import co.censo.shared.data.model.InviteBeneficiaryApiResponse
import co.censo.shared.data.model.LabelOwnerByApproverApiRequest
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerProof
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.RejectAccessApiResponse
import co.censo.shared.data.model.RejectApproverVerificationApiResponse
import co.censo.shared.data.model.RejectAuthenticationResetRequestApiResponse
import co.censo.shared.data.model.RejectBeneficiaryVerificationApiResponse
import co.censo.shared.data.model.RejectTakeoverInitiationApiResponse
import co.censo.shared.data.model.RemoveBeneficiaryApiResponse
import co.censo.shared.data.model.ReplaceAuthenticationApiRequest
import co.censo.shared.data.model.ReplaceAuthenticationApiResponse
import co.censo.shared.data.model.ReplacePolicyApiRequest
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.ReplacePolicyShardsApiRequest
import co.censo.shared.data.model.ReplacePolicyShardsApiResponse
import co.censo.shared.data.model.ResetLoginIdApiRequest
import co.censo.shared.data.model.ResetLoginIdApiResponse
import co.censo.shared.data.model.RetrieveAuthTypeApiRequest
import co.censo.shared.data.model.RetrieveAuthTypeApiResponse
import co.censo.shared.data.model.RetrieveAccessShardsApiRequest
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.model.RetrieveTakeoverKeyApiRequest
import co.censo.shared.data.model.RetrieveTakeoverKeyApiResponse
import co.censo.shared.data.model.SetPromoCodeApiRequest
import co.censo.shared.data.model.SignInApiRequest
import co.censo.shared.data.model.StoreAccessTotpSecretApiRequest
import co.censo.shared.data.model.StoreAccessTotpSecretApiResponse
import co.censo.shared.data.model.StoreSeedPhraseApiRequest
import co.censo.shared.data.model.StoreSeedPhraseApiResponse
import co.censo.shared.data.model.StoreTakeoverTotpSecretApiRequest
import co.censo.shared.data.model.StoreTakeoverTotpSecretApiResponse
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiResponse
import co.censo.shared.data.model.SubmitApproverVerificationApiRequest
import co.censo.shared.data.model.SubmitApproverVerificationApiResponse
import co.censo.shared.data.model.SubmitAuthenticationResetTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitAuthenticationResetTotpVerificationApiResponse
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiRequest
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiResponse
import co.censo.shared.data.model.SubmitPurchaseApiRequest
import co.censo.shared.data.model.SubmitPurchaseApiResponse
import co.censo.shared.data.model.SubmitTakeoverTotpVerificationApiRequest
import co.censo.shared.data.model.SubmitTakeoverTotpVerificationApiResponse
import co.censo.shared.data.model.TimelockApiResponse
import co.censo.shared.data.model.UnlockApiRequest
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.model.UpdateBeneficiaryApproverContactInfoApiRequest
import co.censo.shared.data.model.UpdateBeneficiaryApproverContactInfoApiResponse
import co.censo.shared.data.model.UpdateSeedPhraseMetaInfoApiRequest
import co.censo.shared.data.model.UpdateSeedPhraseMetaInfoApiResponse
import co.censo.shared.data.networking.ApiService.Companion.APPLICATION_IDENTIFIER
import co.censo.shared.data.networking.ApiService.Companion.APP_PLATFORM_HEADER
import co.censo.shared.data.networking.ApiService.Companion.APP_VERSION_HEADER
import co.censo.shared.data.networking.ApiService.Companion.DEVICE_TYPE_HEADER
import co.censo.shared.data.networking.ApiService.Companion.IS_API
import co.censo.shared.data.networking.ApiService.Companion.OS_VERSION_HEADER
import co.censo.shared.data.networking.IgnoreKeysJson.baseKotlinXJson
import co.censo.shared.data.repository.PlayIntegrityRepository
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.projectLog
import co.censo.shared.util.sendError
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.IOException
import java.time.Duration
import java.util.Base64
import retrofit2.Response as RetrofitResponse

object IgnoreKeysJson {
    val baseKotlinXJson = Json {
        ignoreUnknownKeys = true
    }
}

interface ApiService {

    companion object {

        const val INVITATION_ID = "invitationId"
        const val TAKEOVER_ID = "takeoverId"
        const val PARTICIPANT_ID = "participantId"
        const val CHANNEL = "channel"
        const val APPROVAL_ID = "approvalId"
        const val AUTH_RESET_APPROVAL_ID = "resetApprovalId"
        const val SEED_PHRASE_ID = "seedPhraseId"

        const val IS_API = "X-IsApi"
        const val APPLICATION_IDENTIFIER = "X-Censo-App-Identifier"
        const val DEVICE_TYPE_HEADER = "X-Censo-Device-Type"
        const val APP_VERSION_HEADER = "X-Censo-App-Version"
        const val APP_PLATFORM_HEADER = "X-Censo-App-Platform"
        const val OS_VERSION_HEADER = "X-Censo-OS-Version"
        const val PLAY_INTEGRITY_HEADER = "X-Censo-Play-Integrity"
        const val PLAY_INTEGRITY_TOKEN_HEADER = "X-Censo-Play-Integrity-Token"
        const val ATTESTATION_CHALLENGE_HEADER = "X-Censo-Challenge"

        fun create(
            secureStorage: SecurePreferences,
            context: Context,
            versionCode: String,
            packageName: String,
            authUtil: AuthUtil,
            playIntegrityRepository: PlayIntegrityRepository
        ): ApiService {

            val playIntegrityInterceptor = PlayIntegrityInterceptor(playIntegrityRepository)
            val client = OkHttpClient.Builder()
                .addInterceptor(ConnectivityInterceptor(context))
                .addInterceptor(AnalyticsInterceptor(versionCode, packageName))
                .addInterceptor(AuthInterceptor(authUtil, secureStorage))
                .addInterceptor(playIntegrityInterceptor)
                .addInterceptor(MaintenanceModeInterceptor())
                .addInterceptor(FeatureFlagInterceptor(secureStorage))
                .connectTimeout(Duration.ofSeconds(180))
                .readTimeout(Duration.ofSeconds(180))
                .callTimeout(Duration.ofSeconds(180))
                .writeTimeout(Duration.ofSeconds(180))


            if (BuildConfig.BUILD_TYPE == "debug") {
                val logger =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                client.addInterceptor(logger)
            }

            val contentType = "application/json".toMediaType()
            val apiService = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client.build())
                .addConverterFactory(
                    baseKotlinXJson.asConverterFactory(
                        contentType
                    )
                )
                .build()
                .create(ApiService::class.java)
            playIntegrityInterceptor.apiService = apiService
            return apiService
        }

        val enablePlayIntegrity = mapOf(PLAY_INTEGRITY_HEADER to "true")
    }


    @POST("/v1/sign-in")
    suspend fun signIn(
        @Body signInApiRequest: SignInApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<Unit>

    @POST("/v1/device")
    suspend fun createDevice(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<Unit>

    @POST("/v1/auth-type")
    suspend fun retrieveAuthType(
        @Body apiRequest: RetrieveAuthTypeApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<RetrieveAuthTypeApiResponse>

    @PUT("/v1/login-id")
    suspend fun resetLoginId(
        @Body apiRequest: ResetLoginIdApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ResetLoginIdApiResponse>

    @POST("v1/login-id-reset-token/{$PARTICIPANT_ID}")
    suspend fun createLoginIdResetToken(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<GetApproverUserApiResponse>

    @GET("/v1/user")
    suspend fun ownerUser(): RetrofitResponse<GetOwnerUserApiResponse>

    @GET("/v1/user")
    suspend fun approverUser(): RetrofitResponse<GetApproverUserApiResponse>

    @DELETE("/v1/user")
    suspend fun deleteUser(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<Unit>

    @POST("/v1/biometry-verifications")
    suspend fun initBiometryVerification(): RetrofitResponse<InitBiometryVerificationApiResponse>

    @POST("/v1/policy-setup")
    suspend fun createOrUpdatePolicySetup(
        @Body apiRequest: CreatePolicySetupApiRequest
    ): RetrofitResponse<CreatePolicySetupApiResponse>

    @DELETE("/v1/policy-setup")
    suspend fun deletePolicySetup(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<DeletePolicySetupApiResponse>

    @POST("/v1/policy")
    suspend fun createPolicy(
        @Body createPolicyApiRequest: CreatePolicyApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<CreatePolicyApiResponse>

    @PUT("/v1/policy")
    suspend fun replacePolicy(
        @Body createPolicyApiRequest: ReplacePolicyApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ReplacePolicyApiResponse>

    @PUT("/v1/policy/shards")
    suspend fun replacePolicyShards(
        @Body replacePolicyApiRequest: ReplacePolicyShardsApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ReplacePolicyShardsApiResponse>

    @POST("v1/policy/beneficiary")
    suspend fun inviteBeneficiary(
        @Body apiRequest: InviteBeneficiaryApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<InviteBeneficiaryApiResponse>

    @DELETE("v1/policy/beneficiary")
    suspend fun removeBeneficiary(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<RemoveBeneficiaryApiResponse>

    @POST("/v1/approvership-invitations/{$INVITATION_ID}/accept")
    suspend fun acceptApprovership(
        @Path(value = INVITATION_ID) invitationId: String
    ): RetrofitResponse<AcceptApprovershipApiResponse>

    @POST("/v1/approvership-invitations/{$INVITATION_ID}/decline")
    suspend fun declineApprovership(
        @Path(value = INVITATION_ID) invitationId: String,
    ): RetrofitResponse<Unit>

    @POST("v1/approvership-invitations/{$INVITATION_ID}/verification")
    suspend fun submitApproverVerification(
        @Path(value = INVITATION_ID) invitationId: String,
        @Body submitApproverVerificationApiRequest: SubmitApproverVerificationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitApproverVerificationApiResponse>

    @POST("/v1/approvers/{$PARTICIPANT_ID}/confirmation")
    suspend fun confirmApprovership(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body confirmApprovershipApiRequest: ConfirmApprovershipApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ConfirmApprovershipApiResponse>

    @POST("/v1/approvers/{$PARTICIPANT_ID}/verification/reject")
    suspend fun rejectVerification(
        @Path(value = PARTICIPANT_ID) participantId: String,
    ): RetrofitResponse<RejectApproverVerificationApiResponse>

    @POST("v1/notification-tokens")
    suspend fun addPushNotificationToken(@Body pushData: PushBody): RetrofitResponse<Unit>

    @DELETE("v1/notification-tokens/{deviceType}")
    suspend fun removePushNotificationToken(
        @Path("deviceType") deviceType: String
    ): RetrofitResponse<Unit>

    @POST("/v1/unlock")
    suspend fun unlock(
        @Body apiRequest: UnlockApiRequest
    ): RetrofitResponse<UnlockApiResponse>

    @POST("/v1/unlock-prolongation")
    suspend fun prolongUnlock(): RetrofitResponse<ProlongUnlockApiResponse>

    @POST("/v1/lock")
    suspend fun lock(): RetrofitResponse<LockApiResponse>

    @POST("/v1/vault/seed-phrases")
    suspend fun storeSeedPhrase(
        @Body apiRequest: StoreSeedPhraseApiRequest
    ): RetrofitResponse<StoreSeedPhraseApiResponse>

    @DELETE("/v1/vault/seed-phrases/{$SEED_PHRASE_ID}")
    suspend fun deleteSeedPhrase(
        @Path(value = SEED_PHRASE_ID) seedPhraseId: SeedPhraseId,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<DeleteSeedPhraseApiResponse>

    @GET("/v1/vault/seed-phrases/{$SEED_PHRASE_ID}")
    suspend fun getSeedPhrase(
        @Path(value = SEED_PHRASE_ID) seedPhraseId: SeedPhraseId,
    ): RetrofitResponse<GetSeedPhraseApiResponse>

    @PATCH("/v1/vault/seed-phrases/{$SEED_PHRASE_ID}/meta-info")
    suspend fun updateSeedPhraseMetaInfo(
        @Path(value = SEED_PHRASE_ID) seedPhraseId: SeedPhraseId,
        @Body apiRequest: UpdateSeedPhraseMetaInfoApiRequest
    ): RetrofitResponse<UpdateSeedPhraseMetaInfoApiResponse>

    @POST("/v1/access")
    suspend fun requestAccess(
        @Body apiRequest: InitiateAccessApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<InitiateAccessApiResponse>

    @DELETE("/v1/access")
    suspend fun deleteAccess(): RetrofitResponse<DeleteAccessApiResponse>

    @POST("/v1/access/{$APPROVAL_ID}/totp")
    suspend fun storeAccessTotpSecret(
        @Path(value = APPROVAL_ID) approvalId: String,
        @Body apiRequest: StoreAccessTotpSecretApiRequest
    ): RetrofitResponse<StoreAccessTotpSecretApiResponse>

    @POST("/v1/access/{$PARTICIPANT_ID}/totp-verification")
    suspend fun submitAccessTotpVerification(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: SubmitAccessTotpVerificationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitAccessTotpVerificationApiResponse>

    @POST("/v1/access/{$APPROVAL_ID}/approval")
    suspend fun approveAccess(
        @Path(value = APPROVAL_ID) approvalId: String,
        @Body apiRequest: ApproveAccessApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ApproveAccessApiResponse>

    @POST("/v1/access/{$APPROVAL_ID}/rejection")
    suspend fun rejectAccess(
        @Path(value = APPROVAL_ID) approvalId: String
    ): RetrofitResponse<RejectAccessApiResponse>

    @POST("/v1/access/retrieval")
    suspend fun retrieveAccessShards(
        @Body apiRequest: RetrieveAccessShardsApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<RetrieveAccessShardsApiResponse>

    @POST("/v1/purchases")
    suspend fun submitPurchase(
        @Body apiRequest: SubmitPurchaseApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitPurchaseApiResponse>

    @POST("v1/approvers/{$PARTICIPANT_ID}/owner-completion")
    suspend fun completeOwnerApprovership(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: CompleteOwnerApprovershipApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<CompleteOwnerApprovershipApiResponse>

    @POST("/v1/attestation-challenge")
    suspend fun createAttestationChallenge(): RetrofitResponse<AttestationChallengeResponse>

    @POST("/v1/timelock/enable")
    suspend fun enableTimelock(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<TimelockApiResponse>

    @POST("/v1/timelock/disable")
    suspend fun disableTimelock(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<TimelockApiResponse>

    @DELETE("/v1/timelock/disable")
    suspend fun cancelDisableTimelock(): RetrofitResponse<Unit>

    @PUT("v1/approvers/{$PARTICIPANT_ID}/owner-label")
    suspend fun labelOwnerByApprover(
        @Path(value = PARTICIPANT_ID) participantId: String,
        @Body apiRequest: LabelOwnerByApproverApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<GetApproverUserApiResponse>

    @POST("v1/import/{$CHANNEL}/accept")
    suspend fun acceptImport(
        @Path(value = CHANNEL) channel: String,
        @Body ownerProof: OwnerProof,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<Unit>

    @GET("v1/import/{$CHANNEL}/encrypted")
    suspend fun importedEncryptedData(
        @Path(value = CHANNEL) channel: String,
    ): RetrofitResponse<GetImportEncryptedDataApiResponse>

    @POST("/v1/authentication-reset")
    suspend fun requestAuthenticationReset(
         @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<InitiateAuthenticationResetApiResponse>

    @DELETE("/v1/authentication-reset")
    suspend fun cancelAuthenticationReset(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<CancelAuthenticationResetApiResponse>

    @POST("/v1/authentication-reset/{$AUTH_RESET_APPROVAL_ID}/acceptance")
    suspend fun acceptAuthenticationResetRequest(
        @Path(value = AUTH_RESET_APPROVAL_ID) authResetApprovalId: AuthenticationResetApprovalId,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<AcceptAuthenticationResetRequestApiResponse>

    @POST("/v1/authentication-reset/{$AUTH_RESET_APPROVAL_ID}/rejection")
    suspend fun rejectAuthenticationResetRequest(
        @Path(value = AUTH_RESET_APPROVAL_ID) authResetApprovalId: AuthenticationResetApprovalId,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<RejectAuthenticationResetRequestApiResponse>

    @POST("/v1/authentication-reset/{$AUTH_RESET_APPROVAL_ID}/totp-verification")
    suspend fun submitAuthenticationResetTotpVerification(
        @Path(value = AUTH_RESET_APPROVAL_ID) authResetApprovalId: AuthenticationResetApprovalId,
        @Body apiRequest: SubmitAuthenticationResetTotpVerificationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<SubmitAuthenticationResetTotpVerificationApiResponse>

    @PUT("/v1/authentication")
    suspend fun replaceAuthentication(
        @Body apiRequest: ReplaceAuthenticationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<ReplaceAuthenticationApiResponse>

    @GET("/health")
    suspend fun health(): RetrofitResponse<Unit>
    @POST("v1/beneficiary-invitations/{$INVITATION_ID}/accept")
    suspend fun acceptBeneficiaryInvitation(
        @Path(value = INVITATION_ID) invitationId: String,
        @Body requestBody: AcceptBeneficiaryInvitationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ): RetrofitResponse<AcceptBeneficiaryInvitationApiResponse>

    @POST("v1/beneficiary-invitations/{$INVITATION_ID}/verification")
    suspend fun submitBeneficiaryVerification(
        @Path(value = INVITATION_ID) invitationId: String,
        @Body requestBody: SubmitBeneficiaryVerificationApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<SubmitBeneficiaryVerificationApiResponse>

    @POST("v1/policy/beneficiary/activate")
    suspend fun activateBeneficiary(
        @Body requestBody: ActivateBeneficiaryApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<ActivateBeneficiaryApiResponse>

    @POST("/v1/promo-code")
    suspend fun setPromoCode(
        @Body requestBody: SetPromoCodeApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<Unit>
    @POST("v1/policy/beneficiary/reject")
    suspend fun rejectBeneficiaryVerification(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<RejectBeneficiaryVerificationApiResponse>

    @PUT("v1/policy/beneficiary/approver-contact-info")
    suspend fun updateBeneficiaryApproverContactInfo(
        @Body requestBody: UpdateBeneficiaryApproverContactInfoApiRequest,
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<UpdateBeneficiaryApproverContactInfoApiResponse>

    @POST("v1/takeover")
    suspend fun initiateTakeover(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<InitiateTakeoverApiResponse>

    @DELETE("v1/takeover")
    suspend fun cancelTakeover(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity
    ) : RetrofitResponse<CancelTakeoverApiResponse>

    @POST("v1/takeover/{$TAKEOVER_ID}/approval")
    suspend fun approveTakeoverInitiation(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity,
        @Path(value = TAKEOVER_ID) takeoverId: String,
        @Body requestBody: ApproveTakeoverInitiationApiRequest
    ) : RetrofitResponse<ApproveTakeoverInitiationApiResponse>

    @POST("v1/takeover/{$TAKEOVER_ID}/rejection")
    suspend fun rejectTakeoverInitiation(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity,
        @Path(value = TAKEOVER_ID) takeoverId: String,
    ) : RetrofitResponse<RejectTakeoverInitiationApiResponse>

    @POST("v1/takeover/{$TAKEOVER_ID}/totp")
    suspend fun storeTakeoverTotpSecret(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity,
        @Path(value = TAKEOVER_ID) takeoverId: String,
        @Body requestBody: StoreTakeoverTotpSecretApiRequest
    ) : RetrofitResponse<StoreTakeoverTotpSecretApiResponse>

    @POST("v1/takeover/totp-verification")
    suspend fun submitTakeoverTotpVerification(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity,
        @Body requestBody: SubmitTakeoverTotpVerificationApiRequest
    ) : RetrofitResponse<SubmitTakeoverTotpVerificationApiResponse>

    @POST("v1/takeover/{$TAKEOVER_ID}/totp-verification/approval")
    suspend fun approveTakeoverTotpVerification(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity,
        @Body requestBody: ApproveTakeoverInitiationApiRequest,
        @Path(value = TAKEOVER_ID) takeoverId: String,
        ) : RetrofitResponse<ApproveTakeoverInitiationApiResponse>

    @POST("v1/takeover/{$TAKEOVER_ID}/totp-verification/rejection")
    suspend fun rejectTakeoverTotpVerification(
        @HeaderMap headers: Map<String, String> = enablePlayIntegrity,
        @Path(value = TAKEOVER_ID) takeoverId: String,
    ) : RetrofitResponse<RejectTakeoverInitiationApiResponse>

    @POST("v1/takeover/retrieval")
    suspend fun retrieveTakeoverKey(
        @Body requestBody: RetrieveTakeoverKeyApiRequest,
    ) : RetrofitResponse<RetrieveTakeoverKeyApiResponse>

    @POST("v1/takeover/finalize")
    suspend fun finalizeTakeover(
        @Body requestBody: FinalizeTakeoverApiRequest,
    ) : RetrofitResponse<FinalizeTakeoverApiResponse>
}

class AnalyticsInterceptor(
    private val versionCode: String,
    private val packageName: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) =
        chain.proceed(
            chain.request().newBuilder()
                .apply {
                    addHeader(
                        IS_API,
                        "true"
                    )
                    addHeader(
                        APPLICATION_IDENTIFIER,
                        packageName
                    )
                    addHeader(
                        DEVICE_TYPE_HEADER,
                        "Android ${Build.MANUFACTURER} - ${Build.DEVICE} (${Build.MODEL})"
                    )
                    addHeader(
                        APP_VERSION_HEADER,
                        "${BuildConfig.VERSION_NAME}+$versionCode"
                    )
                    addHeader(
                        APP_PLATFORM_HEADER,
                        "android"
                    )
                    addHeader(
                        OS_VERSION_HEADER,
                        "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"
                    )
                }
                .build()
        )
}

class AuthInterceptor(
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences
) : Interceptor {
    private val AUTHORIZATION_HEADER = "Authorization"
    private val DEVICE_PUBLIC_KEY_HEADER = "X-Censo-Device-Public-Key"
    private val TIMESTAMP_HEADER = "X-Censo-Timestamp"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val now = Clock.System.now()
        var headers : List<Header>

        runBlocking {
            headers = getAuthHeaders(now, request)
        }

        return if (headers.isEmpty()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder().apply {
                    for (header in headers) {
                        addHeader(header.name, header.value)
                    }
                }.build()
            )
        }
    }

    private suspend fun getAuthHeaders(now: Instant, request: Request): List<Header> {
        val deviceKeyId = secureStorage.retrieveDeviceKeyId()
        val jwt = secureStorage.retrieveJWT()

        if (jwt.isEmpty() || deviceKeyId.isEmpty()) {
            return emptyList()
        }

        try {
            authUtil.silentlyRefreshTokenIfInvalid(jwt, deviceKeyId)
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.AuthHeaders)
            return emptyList()
        }

        val deviceKey = InternalDeviceKey(deviceKeyId)
        val signature = Base64.getEncoder().encodeToString(deviceKey.sign(request.dataToSign(now.toString())))

        return listOf(
            Header(AUTHORIZATION_HEADER, "signature $signature"),
            Header(DEVICE_PUBLIC_KEY_HEADER, deviceKey.publicExternalRepresentation().value),
            Header(TIMESTAMP_HEADER, now.toString())
        )
    }
}

class FeatureFlagInterceptor(
    private val secureStorage: SecurePreferences
) : Interceptor {
    private val featureFlag = "x-censo-feature-flags"
    private val legacyFeature = "legacy"

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        response.headers[featureFlag]?.let { featuresEnabled ->
            val legacyEnabled = featuresEnabled
                .split(",")
                .contains(legacyFeature)

            secureStorage.setLegacyEnabled(legacyEnabled)
        } ?: secureStorage.setLegacyEnabled(false)

        return response
    }
}

class PlayIntegrityInterceptor(private val playIntegrityRepository: PlayIntegrityRepository) : Interceptor {

    lateinit var apiService: ApiService

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var headers : List<Header>

        return when {
            !BuildConfig.PLAY_INTEGRITY_ENABLED -> {
                chain.proceed(request.newBuilder().apply {
                    removeHeader(ApiService.PLAY_INTEGRITY_HEADER)
                }.build())
            }

            request.header(ApiService.PLAY_INTEGRITY_HEADER) == null ->
                chain.proceed(request)

            else -> {
                runBlocking {
                    headers = getPlayIntegrityHeaders(request)
                }
                chain.proceed(
                    request.newBuilder().apply {
                        for (header in headers) {
                            addHeader(header.name, header.value)
                        }
                        removeHeader(ApiService.PLAY_INTEGRITY_HEADER)
                    }.build()
                )
            }

        }
    }

    private suspend fun getPlayIntegrityHeaders(request: Request): List<Header> {
        return try {
            apiService.createAttestationChallenge().body()?.challenge?.base64Encoded?.let { challenge ->
                listOf(
                    Header(
                        ApiService.ATTESTATION_CHALLENGE_HEADER,
                        challenge
                    ),
                    Header(
                        ApiService.PLAY_INTEGRITY_TOKEN_HEADER,
                        playIntegrityRepository.getIntegrityToken(
                            request.dataToSign(challenge).sha256digest().base64Encoded()
                        )
                    )
                )
            } ?: listOf()
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.PlayIntegrity)
            listOf()
        }
    }
}

class ConnectivityInterceptor(private val context: Context) : Interceptor {

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkInfo != null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isOnline(context = context)) {
            throw NoConnectivityException()
        } else {
            return chain.proceed(chain.request())
        }
    }
}

class NoConnectivityException : IOException() {
    override val message: String
        get() = "No network connection established"
}

class MaintenanceModeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        GlobalMaintenanceState.isMaintenanceMode.value = response.code == 418
        return response
    }
}

@Serializable
data class PushBody(
    val deviceType: String, val token: String
)

fun Request.dataToSign(timestampOrChallenge: String): ByteArray {
    val requestPathAndQueryParams =
        this.url.encodedPath + (this.url.encodedQuery?.let { "?$it" } ?: "")
    val requestBody = this.body?.let {
        val buffer = Buffer()
        it.writeTo(buffer)
        buffer.readByteArray()
    } ?: byteArrayOf()

    return (this.method + requestPathAndQueryParams + Base64.getEncoder()
        .encodeToString(requestBody) + timestampOrChallenge).toByteArray()
}
