package co.censo.shared.data.repository

import Base64EncodedData
import android.content.Context
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface PlayIntegrityRepository {
    suspend fun getIntegrityToken(requestHash: Base64EncodedData): String
}

class PlayIntegrityRepositoryImpl @Inject constructor(
    applicationContext: Context
) : PlayIntegrityRepository, BaseRepository() {
    private val cloudProjectNumber = 201461616619

    private val integrityManager =
        IntegrityManagerFactory.createStandard(applicationContext)

    private var integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    override suspend fun getIntegrityToken(requestHash: Base64EncodedData): String {
        return getIntegrityTokenWithRetry(requestHash)
    }

    private suspend fun getIntegrityTokenWithRetry(requestHash: Base64EncodedData, attemptNumber: Int = 0): String {
        try {
            return tokenProvider().request(
                StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                    .setRequestHash(requestHash.base64Encoded)
                    .build()
            ).await().token()
        } catch (e: StandardIntegrityException) {
            when (e.errorCode) {
                StandardIntegrityErrorCode.TOO_MANY_REQUESTS -> {
                    // limit the retries to 10 - delay increases between attempts - this is 5.5 seconds total
                    if (attemptNumber < 10) {
                        delay(100L * (attemptNumber + 1))
                        return getIntegrityTokenWithRetry(requestHash, attemptNumber + 1)
                    }
                }
                StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID -> {
                    // we have an invalid token provider, so get a new one
                    if (attemptNumber == 0) {
                        initiateTokenProvider()
                        return getIntegrityTokenWithRetry(requestHash, attemptNumber = 1)
                    }
                }
            }
            throw e
        }
    }

    private suspend fun tokenProvider(): StandardIntegrityManager.StandardIntegrityTokenProvider {
        return integrityTokenProvider ?: initiateTokenProvider()
    }

    private suspend fun initiateTokenProvider(): StandardIntegrityManager.StandardIntegrityTokenProvider {
        return runBlocking {
            try {
                integrityManager.prepareIntegrityToken(
                    StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(cloudProjectNumber)
                        .build()
                ).await().also {
                    integrityTokenProvider = it
                }
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.PlayIntegrity)
                throw e
            }
        }
    }

}
