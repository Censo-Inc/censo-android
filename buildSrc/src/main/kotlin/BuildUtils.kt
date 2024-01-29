import java.io.File

object BuildUtils {

    const val SERVICE_FILE_NAME = "service-account.json"

    fun gitCommitCount(): Int {
        return try {
            Runtime.getRuntime()
                .exec("git rev-list --count --no-merges HEAD")
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
                .toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}