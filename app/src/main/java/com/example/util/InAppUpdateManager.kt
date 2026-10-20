package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * In-App Update Checker & Installation Manager for Zoha Trading.
 * Downloads APK directly from Google Drive and seamlessly triggers Android package installer.
 */
object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"

    // Direct Google Drive APK link and file ID
    const val FILE_ID = "1mdLgw1hLaNyofG5dxU67wiVhtYxdYS6t"
    const val REMOTE_APK_URL = "https://drive.google.com/uc?export=download&id=$FILE_ID"
    const val CURRENT_VERSION_NAME = "1.0.3"
    const val CURRENT_VERSION_CODE = 3

    data class RemoteUpdateInfo(
        val driveVersionName: String,
        val installedVersionName: String,
        val isDriveVersionHigher: Boolean,
        val isSameVersion: Boolean,
        val driveFileName: String
    )

    @Volatile
    var cachedUpdateInfo: RemoteUpdateInfo? = null

    /**
     * Gets installed version name from package manager with fallback.
     */
    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: CURRENT_VERSION_NAME
        } catch (e: Exception) {
            CURRENT_VERSION_NAME
        }
    }

    /**
     * Extracts version string (e.g. "1.0.4", "1.0.1") from filename like "Zoha Trading_1.0.1.apk".
     */
    fun extractVersionFromFilename(filename: String): String? {
        val pattern = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)")
        val matcher = pattern.matcher(filename)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.0.4" vs "1.0.3").
     * Returns:
     *   > 0 if v1 > v2 (v1 is higher)
     *   < 0 if v1 < v2 (v1 is lower)
     *   == 0 if v1 == v2 (same version)
     */
    fun compareVersionStrings(v1: String, v2: String): Int {
        val v1Parts = v1.replace(Regex("[^0-9.]"), "").split(".").mapNotNull { it.toIntOrNull() }
        val v2Parts = v2.replace(Regex("[^0-9.]"), "").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLen) {
            val p1 = if (i < v1Parts.size) v1Parts[i] else 0
            val p2 = if (i < v2Parts.size) v2Parts[i] else 0
            if (p1 > p2) return 1
            if (p1 < p2) return -1
        }
        return 0
    }

    /**
     * Checks Google Drive to discover the uploaded APK version.
     */
    suspend fun checkRemoteVersion(context: Context): Result<RemoteUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val installedVersion = getInstalledVersionName(context)
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            // 1. Fetch file view page from Google Drive to inspect filename/title
            val viewRequest = Request.Builder()
                .url("https://drive.google.com/file/d/$FILE_ID/view")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                .build()

            var remoteFileName: String? = null
            val response = client.newCall(viewRequest).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                // Check <meta itemprop="name" content="...">
                val metaMatcher = Pattern.compile("<meta\\s+itemprop=[\"']name[\"']\\s+content=[\"']([^\"']+)[\"']").matcher(html)
                if (metaMatcher.find()) {
                    remoteFileName = metaMatcher.group(1)
                }
                if (remoteFileName.isNullOrBlank()) {
                    // Check itemJson: [null,"filename.apk",...
                    val itemJsonMatcher = Pattern.compile("itemJson:\\s*\\[(?:null,)?[\"']([^\"']+\\.apk)[\"']").matcher(html)
                    if (itemJsonMatcher.find()) {
                        remoteFileName = itemJsonMatcher.group(1)
                    }
                }
                if (remoteFileName.isNullOrBlank()) {
                    // Check <title>... - Google Drive</title>
                    val titleMatcher = Pattern.compile("<title>([^<]+?)(?:\\s*-\\s*Google Drive)?</title>").matcher(html)
                    if (titleMatcher.find()) {
                        val rawTitle = titleMatcher.group(1) ?: ""
                        if (rawTitle.contains(".apk", ignoreCase = true)) {
                            remoteFileName = rawTitle.trim()
                        }
                    }
                }
            }

            // 2. Fallback: usercontent warning page
            if (remoteFileName.isNullOrBlank()) {
                val warnRequest = Request.Builder()
                    .url("https://drive.usercontent.google.com/download?id=$FILE_ID&export=download")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                    .build()
                val warnResp = client.newCall(warnRequest).execute()
                if (warnResp.isSuccessful) {
                    val warnHtml = warnResp.body?.string() ?: ""
                    val ucMatcher = Pattern.compile("class=[\"']uc-name-size[\"'][^>]*>\\s*<a[^>]*>([^<]+)</a>").matcher(warnHtml)
                    if (ucMatcher.find()) {
                        remoteFileName = ucMatcher.group(1)
                    }
                }
            }

            val finalFileName = remoteFileName ?: "Zoha Trading_1.0.1.apk"
            val driveVersion = extractVersionFromFilename(finalFileName) ?: "1.0.1"

            val comp = compareVersionStrings(driveVersion, installedVersion)
            val isHigher = comp > 0
            val isSame = comp == 0

            val info = RemoteUpdateInfo(
                driveVersionName = driveVersion,
                installedVersionName = installedVersion,
                isDriveVersionHigher = isHigher,
                isSameVersion = isSame,
                driveFileName = finalFileName
            )
            cachedUpdateInfo = info

            // If drive version is NOT higher, clean up any previous cached APKs so user isn't prompted to install old version
            if (!isHigher) {
                clearExistingApks(context)
            }

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking remote version", e)
            Result.failure(e)
        }
    }

    /**
     * Clears all downloaded APKs from the cache directory.
     */
    fun clearExistingApks(context: Context) {
        try {
            val dir = File(context.cacheDir, "updates")
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing updates cache", e)
        }
    }

    /**
     * Retrieves already downloaded APK file if present, valid, and strictly higher than installed version.
     */
    fun getExistingApk(context: Context, targetVersionName: String? = null): File? {
        val dir = File(context.cacheDir, "updates")
        if (!dir.exists()) return null
        val installedVersion = getInstalledVersionName(context)

        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.isFile && file.name.endsWith(".apk") && file.length() > 500_000 && isValidApkFile(file)) {
                val apkVer = extractVersionFromFilename(file.name)
                if (apkVer != null && compareVersionStrings(apkVer, installedVersion) > 0) {
                    return file
                } else if (apkVer != null && compareVersionStrings(apkVer, installedVersion) <= 0) {
                    // Stale or older APK, remove it
                    file.delete()
                }
            }
        }
        return null
    }

    /**
     * Downloads APK from remote Google Drive link with automatic redirection and confirmation bypass.
     * Reports downloaded bytes and total size via [onProgress].
     */
    suspend fun downloadApk(
        context: Context,
        targetVersionName: String = CURRENT_VERSION_NAME,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val tempFile = File(updatesDir, "Zoha_Trading_v$targetVersionName.apk.download")
            val targetFile = File(updatesDir, "Zoha_Trading_v$targetVersionName.apk")

            if (tempFile.exists()) tempFile.delete()

            // In-memory cookie jar to maintain Google Drive confirmation session
            val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
            val cookieJar = object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val list = cookieStore.getOrPut(url.host) { mutableListOf() }
                    cookies.forEach { cookie ->
                        list.removeAll { it.name == cookie.name }
                        list.add(cookie)
                    }
                }
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: emptyList()
                }
            }

            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build()

            val initialRequest = Request.Builder()
                .url(REMOTE_APK_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                .build()

            var response = client.newCall(initialRequest).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Response body is empty"))
            val contentType = response.header("Content-Type")?.lowercase() ?: ""

            // Handle Google Drive virus scan warning / confirmation page for large files
            if (contentType.contains("text/html")) {
                val htmlContent = body.string()
                var finalDownloadUrl: String? = null

                // Look for download form action and hidden inputs (confirm, uuid, id)
                val actionMatcher = Pattern.compile("action=[\"']([^\"']+)[\"']").matcher(htmlContent)
                val uuidMatcher = Pattern.compile("name=[\"']uuid[\"']\\s+value=[\"']([^\"']+)[\"']").matcher(htmlContent)
                val confirmMatcher = Pattern.compile("name=[\"']confirm[\"']\\s+value=[\"']([^\"']+)[\"']").matcher(htmlContent)

                val action = if (actionMatcher.find()) actionMatcher.group(1) else "https://drive.usercontent.google.com/download"
                val uuid = if (uuidMatcher.find()) uuidMatcher.group(1) else null
                val confirm = if (confirmMatcher.find()) confirmMatcher.group(1) else "t"

                if (action != null) {
                    val urlBuilder = StringBuilder(action)
                    urlBuilder.append("?id=").append(FILE_ID)
                    urlBuilder.append("&export=download")
                    urlBuilder.append("&confirm=").append(confirm)
                    if (!uuid.isNullOrBlank()) {
                        urlBuilder.append("&uuid=").append(uuid)
                    }
                    finalDownloadUrl = urlBuilder.toString()
                } else {
                    // Fallback direct usercontent format
                    finalDownloadUrl = "https://drive.usercontent.google.com/download?id=$FILE_ID&export=download&confirm=t"
                }

                val secondRequest = Request.Builder()
                    .url(finalDownloadUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                    .build()

                response = client.newCall(secondRequest).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code} downloading APK"))
                }
            }

            val downloadBody = response.body ?: return@withContext Result.failure(Exception("APK response body is empty"))
            val totalBytes = downloadBody.contentLength()

            downloadBody.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        onProgress(totalDownloaded, totalBytes)
                    }
                    outputStream.flush()
                }
            }

            if (!isValidApkFile(tempFile)) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Downloaded file failed integrity verification (invalid APK format)"))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                // If rename fails across volumes, copy then delete
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update APK", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a file has the standard ZIP / APK signature (starts with 'PK\x03\x04')
     * and is at least 500 KB.
     */
    private fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || file.length() < 500_000) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() && // 'P'
                    header[1] == 0x4B.toByte() && // 'K'
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Triggers direct in-app package installation using FileProvider.
     * If Android 8.0+ unknown app install permission is missing, opens settings prompt.
     */
    fun triggerInstall(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() < 500_000) {
                return Result.failure(IllegalStateException("APK file is missing or invalid."))
            }

            // On Android 8.0+ (Oreo, API 26+), verify if install unknown apps permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(
                        SecurityException("Please allow 'Install unknown apps' permission for Zoha Trading in Settings, then tap Install.")
                    )
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer", e)
            Result.failure(e)
        }
    }

    /**
     * Opens direct download link in external browser as a fallback.
     */
    fun openInBrowser(context: Context) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(REMOTE_APK_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
        }
    }
}
