package co.censo.censo.test_helper

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import SeedPhraseId
import co.censo.shared.data.model.HashedValue
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.PhraseType
import co.censo.shared.data.model.Policy
import co.censo.shared.data.model.PolicySetup
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.model.SubscriptionStatus
import co.censo.shared.data.model.TimelockSetting
import co.censo.shared.data.model.Vault
import kotlinx.datetime.Clock

val mockSeedPhrase = SeedPhrase(
    guid = SeedPhraseId(value = "12345"),
    seedPhraseHash = HashedValue(value = ""),
    label = "Dummy Phrase",
    type = PhraseType.Binary,
    createdAt = Clock.System.now()
)

val mockReadyOwnerStateWithNullPolicySetup = OwnerState.Ready(
    subscriptionStatus = SubscriptionStatus.Active, policy = Policy(
        createdAt = Clock.System.now(),
        approvers = mockTrustedApprovers,
        threshold = 2U,
        encryptedMasterKey = Base64EncodedData(base64Encoded = "AA"),
        intermediateKey = Base58EncodedIntermediatePublicKey(value = "AA"),
        approverKeysSignatureByIntermediateKey = Base64EncodedData(base64Encoded = "AA"),
        masterKeySignature = null,
        ownerEntropy = generateEntropy(),
        owner = trustedOwnerApprover,
        downgradedAt = null,
        beneficiary = null,
    ), vault = Vault(
        seedPhrases = listOf(
            mockSeedPhrase
        ), publicMasterEncryptionKey = Base58EncodedMasterPublicKey(value = "AA")
    ), unlockedForSeconds = 600UL, access = null, policySetup = null,
    timelockSetting = TimelockSetting(defaultTimelockInSeconds = 7200L, currentTimelockInSeconds = null, disabledAt = null),
    onboarded = true,
    canRequestAuthenticationReset = false,
    authenticationReset = null,
    subscriptionRequired = true,
)

val mockReadyOwnerStateWithPolicySetup = mockReadyOwnerStateWithNullPolicySetup.copy(
    policySetup = PolicySetup(
        approvers = mockConfirmedProspectApprovers, threshold = 2U
    )
)

val mockInitialOwnerStateData = OwnerState.Initial(
    entropy = generateEntropy(),
    subscriptionStatus = SubscriptionStatus.Active,
    subscriptionRequired = false,
)
