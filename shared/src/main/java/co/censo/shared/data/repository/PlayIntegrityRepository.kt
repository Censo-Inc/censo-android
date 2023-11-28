package co.censo.shared.data.repository

import Base64EncodedData
import android.content.Context
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
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

    private val integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider by lazy {
        runBlocking {
            try {
                integrityManager.prepareIntegrityToken(
                    StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(cloudProjectNumber)
                        .build()
                ).await()
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.PlayIntegrity)
                throw e
            }
        }
    }

    override suspend fun getIntegrityToken(requestHash: Base64EncodedData): String {
        return integrityTokenProvider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash.base64Encoded)
                .build()).await().token()
    }

}
