package co.censo.shared.data.storage

import Base58EncodedPrivateKey
import ParticipantId
import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.util.GoogleAuth
import co.censo.shared.util.projectLog
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

interface CloudStorage {
    suspend fun uploadFile(fileContent: String, participantId: ParticipantId) : Resource<Unit>
    suspend fun retrieveFileContents(participantId: ParticipantId) : Resource<String?>
    suspend fun checkUserGrantedCloudStoragePermission() : Boolean
    suspend fun deleteFile(participantId: ParticipantId) : Resource<Unit>
}

class GoogleDriveStorage(private val context: Context) : CloudStorage {
    companion object {
        const val FILE_NAME = "DO-NOT-DELETE_Censo-approver-key"
        const val FILE_EXTENSION = ".txt"
        const val FILE_MIME_TYPE = "text/plain"
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
                    e.sendError("UploadFile")
                    return Resource.Error(exception = e)
                }

                //Upload file
                try {
                    val fileMetaData = com.google.api.services.drive.model.File()
                    fileMetaData.name = fileName

                    val mediaContent = FileContent(FILE_MIME_TYPE, localFile)

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
                    e.sendError("UploadFile")
                    projectLog(message = "File upload failed, with google json response exception: $e")
                    return Resource.Error(exception = e)
                } catch (e: Exception) {
                    e.sendError("UploadFile")
                    projectLog(message = "File upload failed, with exception: $e")
                    return Resource.Error(exception = e)
                }
        } else {
            projectLog(message = "Account was null")
            Resource.Error()
        }
    }

    override suspend fun retrieveFileContents(participantId: ParticipantId) : Resource<String?> {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            val driveService = getDriveService(account, context)
                ?: return Resource.Error(exception = Exception("Drive service was null"))

            //Assemble FileName and query params
            val fileName = "${FILE_NAME}_${participantId.value}${FILE_EXTENSION}"
            val queryForFileWithMatchingNameAndNotTrashed = "name='$fileName' and trashed=false"

            val file = try {
                //Get the list of files
                val fileList = driveService.files().list()
                    .setQ(queryForFileWithMatchingNameAndNotTrashed)
                    .execute()

                //Assign the matching file
                fileList.files.first {
                    it.name == fileName
                }
            } catch (e: NoSuchElementException) {
                e.sendError("RetrieveFileContent")
                return Resource.Error(exception = Exception("Retrieved files did not contain matching name"))
            } catch (e: Exception) {
                e.sendError("RetrieveFileContent")
                return Resource.Error(exception = Exception("Unable to find requested file in users Drive"))
            }

            try {
                val outputStream = ByteArrayOutputStream()

                driveService.files().get(file.id)
                    .executeMediaAndDownloadTo(outputStream)

                val fileContents = outputStream.toString()

                withContext(Dispatchers.IO) {
                    outputStream.close()
                }

                return if (fileContents.isNotEmpty()) {
                    projectLog(message = "Successfully exported file content to outputStream, file content: $fileContents")
                    Resource.Success(fileContents)
                } else {
                    Resource.Error(exception = Exception("File existed and contents were downloaded, however the contents were empty"))
                }
            } catch (e: GoogleJsonResponseException) {
                e.sendError("RetrieveFileContent")
                return Resource.Error(exception = e)
            } catch (e: Exception) {
                e.sendError("RetrieveFileContent")
                return Resource.Error(exception = e)
            }
        } else {
            Exception().sendError("RetrieveFileContent")
            return Resource.Error(exception = Exception("Users account was null"))
        }
    }

    override suspend fun checkUserGrantedCloudStoragePermission(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.grantedScopes?.contains(GoogleAuth.DRIVE_FILE_SCOPE) ?: false
    }

    override suspend fun deleteFile(participantId: ParticipantId): Resource<Unit> {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null) {
            val driveService = getDriveService(account, context)
            if (driveService == null) {
                projectLog(message = "Drive service was null")
                return Resource.Error()
            }

            //Assemble FileName and query params
            val fileName = "${FILE_NAME}_${participantId.value}${FILE_EXTENSION}"
            val queryForFileWithMatchingNameAndNotTrashed = "name='$fileName' and trashed=false"

            val file = try {
                //Get the list of files
                val fileList = driveService.files().list()
                    .setQ(queryForFileWithMatchingNameAndNotTrashed)
                    .execute()

                //Assign the matching file
                fileList.files.first {
                    it.name == fileName
                }
            } catch (e: NoSuchElementException) {
                e.sendError("DeleteFile")
                return Resource.Error(exception = Exception("Retrieved files did not contain matching name"))
            } catch (e: Exception) {
                e.sendError("DeleteFile")
                return Resource.Error(exception = Exception("Unable to find requested file in users Drive"))
            }

            try {
                //Will permanently delete the users file
                driveService.files().delete(file.id).execute()
                return Resource.Success(Unit)
            } catch (e: Exception) {
                e.sendError("DeleteFile")
                return Resource.Error(exception = e)
            }
        }
        else {
            Resource.Error(exception = Exception("Users account was null"))
        }
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
            e.sendError("RetrieveDriveService")
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

class CloudStoragePermissionNotGrantedException(override val message: String = "User did not grant cloud storage permissions") :
    Exception()