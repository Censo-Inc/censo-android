package co.censo.shared.data.storage

import ParticipantId
import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.util.projectLog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

interface CloudStorage {
    suspend fun uploadFile(fileContent: String, participantId: ParticipantId) : Resource<Unit>
    suspend fun retrieveFile()
}

class GoogleDriveStorage(private val context: Context) : CloudStorage {
    companion object {
        const val FILE_NAME = "DO-NOT-DELETE_Censo-approver-key"
        const val FILE_EXTENSION = ".txt"
        const val FILE_TYPE = "text/plain"
        const val ID_FIELD = "id"

        const val APP_NAME = "Censo"
    }

    override suspend fun uploadFile(fileContent: String, participantId: ParticipantId) : Resource<Unit> {
        projectLog(message = "Starting file upload")
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null) {
            val driveService = getDriveService(account, context)
            if (driveService == null) {
                projectLog(message = "Drive service was null")
                return Resource.Error()
            }

                //Save local file
                val fileName = "${FILE_NAME}_${participantId.value}$FILE_EXTENSION"
                val fileDir = context.getExternalFilesDir(null)
                val localFile = File(fileDir, fileName)

                try {
                    val outputStream = FileOutputStream(localFile)
                    outputStream.write(fileContent.toByteArray())
                    outputStream.close()
                    projectLog(message = "Saved local file")
                } catch (e: IOException) {
                    projectLog(message = "Failed to save local file")
                    return Resource.Error(exception = e)
                }

                //Upload file
                try {
                    val fileMetaData = com.google.api.services.drive.model.File()
                    fileMetaData.name = fileName

                    val mediaContent = FileContent(FILE_TYPE, localFile)

                    val uploadedFile = driveService.files().create(fileMetaData, mediaContent)
                        .setFields(ID_FIELD)
                        .execute()

                    //Delete the temp local file
                    localFile.delete()
                    projectLog(message = "File upload process finished")
                    return if (uploadedFile != null && uploadedFile.id != null ) {
                        projectLog(message = "File uploaded with id: ${uploadedFile.id}")
                        Resource.Success(Unit)
                    } else {
                        projectLog(message = "File upload failed")
                        Resource.Error()
                    }
                } catch (e: GoogleJsonResponseException) {
                    projectLog(message = "File upload failed, with google json response exception: $e")
                    return Resource.Error(exception = e)
                } catch (e: Exception) {
                    projectLog(message = "File upload failed, with exception: $e")
                    return Resource.Error(exception = e)
                }
        } else {
            projectLog(message = "Account was null")
            Resource.Error()
        }
    }

    override suspend fun retrieveFile() {
        TODO("Not yet implemented")
    }

    private fun getDriveService(
        account: GoogleSignInAccount,
        context: Context,
    ): Drive? {
        val credential: GoogleAccountCredential?
        try {
            credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            ).setSelectedAccount(account.account)
        } catch (e: IOException) {
            return null
        }

        return if (credential != null) {
            Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName(APP_NAME)
                .build()
        } else {
            null
        }
    }

}