package co.censo.vault.presentation.plan_setup

import Base58EncodedIntermediatePublicKey
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


data class PlanSetupState(
    val ownerStateResource: Resource<OwnerState> = Resource.Uninitialized,
    // Screen in Plan Setup Flow
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.InviteApprovers,

    //Plan Data
    val primaryApprover: ApproverActivation = ApproverActivation(),
    val backupApprover: ApproverActivation = ApproverActivation(),

    // API Calls
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,

    val createPolicyResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized
) {

    val backArrowType = when (planSetupUIState) {
        PlanSetupUIState.InviteApprovers,
        PlanSetupUIState.PrimaryApproverActivation,
        PlanSetupUIState.BackupApproverActivation -> BackIconType.Back

        PlanSetupUIState.PrimaryApproverNickname,
        PlanSetupUIState.PrimaryApproverGettingLive,
        PlanSetupUIState.AddBackupApprover,
        PlanSetupUIState.BackupApproverNickname,
        PlanSetupUIState.BackupApproverGettingLive,
        PlanSetupUIState.Completed -> BackIconType.Exit
    }

    val loading =
        userResponse is Resource.Loading || createPolicySetupResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error || createPolicySetupResponse is Resource.Error

    data class ApproverActivation(
        val nickname: String = "",
        val participantId: ParticipantId = ParticipantId(""),
        val totpSecret: String = "",

        val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
        val secondsLeft: Int = 0,
        val totpCode: String = "",

        val prospectGuardian: Guardian.ProspectGuardian? = null
    ) {
        fun totpTick(): ApproverActivation {
            val newCounter = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
            val totpCode = if (counter != newCounter) {
                TotpGenerator.generateCode(this.totpSecret, counter)
            } else {
                totpCode
            }

            val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second
            val secondsLeft = (newCounter + TotpGenerator.CODE_EXPIRATION - currentSecond).toInt()

            return this.copy(
                counter = newCounter,
                totpCode = totpCode,
                secondsLeft = secondsLeft
            )
        }
    }

    enum class BackIconType {
        Back, Exit
    }
}

enum class PlanSetupUIState {
    InviteApprovers,
    PrimaryApproverNickname,
    PrimaryApproverGettingLive,
    PrimaryApproverActivation,
    AddBackupApprover,
    BackupApproverNickname,
    BackupApproverGettingLive,
    BackupApproverActivation,
    Completed
}