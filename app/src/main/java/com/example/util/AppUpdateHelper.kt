package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkName: String,
    val apkSizeBytes: Long,
    val htmlUrl: String
)

sealed class UpdateCheckResult {
    data class UpToDate(val currentVersion: String, val latestVersion: String) : UpdateCheckResult()
    data class UpdateAvailable(val currentVersion: String, val info: ReleaseUpdateInfo) : UpdateCheckResult()
}

enum class InstallLaunchStatus {
    LAUNCHED,
    NEEDS_PERMISSION,
    ERROR
}

object AppUpdateHelper {

    const val DEFAULT_GITHUB_REPO = "alovemamad/GYM2"
    private val VALID_SEGMENT_REGEX = Regex("^[A-Za-z0-9._-]+$")

    private val apiClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getEffectiveRepo(@Suppress("UNUSED_PARAMETER") savedRepo: String = ""): String {
        return normalizeRepoSlug(BuildConfig.GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
    }

    /**
     * Normalized repository slug parser supporting `owner/repo`, HTTPS/SSH URLs,
     * `gh repo clone owner/repo`, and `git clone https://github.com/owner/repo.git`.
     */
    fun normalizeRepoSlug(input: String): String? {
        var cleaned = input.trim()
        if (cleaned.isBlank()) return null

        // Strip CLI clone prefixes if pasted directly
        cleaned = cleaned
            .replace(Regex("^gh\\s+repo\\s+clone\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^git\\s+clone\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
            .substringBefore(" ")
            .substringBefore("?")
            .substringBefore("#")

        // Strip common GitHub URL prefixes (case-insensitive for host)
        cleaned = cleaned
            .replace(Regex("^https?://(www\\.)?github\\.com/", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^ssh://git@github\\.com/", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^git@github\\.com:", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^github\\.com/", RegexOption.IGNORE_CASE), "")
            .trim('/')

        val parts = cleaned.split("/").filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val owner = parts[0].trim()
        val repo = parts[1].trim().removeSuffix(".git").removeSuffix(".GIT")
        if (owner.isBlank() || repo.isBlank()) return null
        if (!VALID_SEGMENT_REGEX.matches(owner) || !VALID_SEGMENT_REGEX.matches(repo)) return null
        if (owner.equals("https:", ignoreCase = true) || owner.equals("http:", ignoreCase = true)) return null

        return "$owner/$repo"
    }

    fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
            .trim()
    }

    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = normalizeVersion(remoteVersion)
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
        val currentParts = normalizeVersion(currentVersion)
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun githubGetRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GYM-PRO-Android-Updater")
            .build()
    }

    private fun parseReleaseJson(
        json: JSONObject,
        currentVersion: String
    ): Result<UpdateCheckResult> {
        val tagName = json.optString("tag_name", "")
        val releaseTitle = json.optString("name", tagName).ifBlank { tagName }
        val changelog = json.optString("body", "").trim()
        val htmlUrl = json.optString("html_url", "")
        val remoteVersion = normalizeVersion(tagName)

        if (remoteVersion.isBlank()) {
            return Result.failure(IllegalStateException("شماره نسخه در ریلیز گیت‌هاب یافت نشد."))
        }

        val assets = json.optJSONArray("assets")
        var apkUrl = ""
        var apkName = "GYM-PRO-$tagName.apk"
        var apkSize = 0L

        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    apkName = name
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        return if (isNewerVersion(remoteVersion, currentVersion)) {
            if (apkUrl.isBlank()) {
                Result.failure(
                    IllegalStateException("نسخه جدید ($tagName) پیدا شد اما فایل APK در این ریلیز ضمیمه نشده است.")
                )
            } else {
                Result.success(
                    UpdateCheckResult.UpdateAvailable(
                        currentVersion = currentVersion,
                        info = ReleaseUpdateInfo(
                            tagName = tagName,
                            versionName = remoteVersion,
                            releaseTitle = releaseTitle,
                            changelog = changelog,
                            apkDownloadUrl = apkUrl,
                            apkName = apkName,
                            apkSizeBytes = apkSize,
                            htmlUrl = htmlUrl
                        )
                    )
                )
            }
        } else {
            Result.success(
                UpdateCheckResult.UpToDate(
                    currentVersion = currentVersion,
                    latestVersion = remoteVersion
                )
            )
        }
    }

    suspend fun checkForUpdate(
        repoInput: String = getEffectiveRepo(),
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<UpdateCheckResult> = withContext(Dispatchers.IO) {
        try {
            val slug = normalizeRepoSlug(repoInput) ?: DEFAULT_GITHUB_REPO

            val latestReq = githubGetRequest("https://api.github.com/repos/$slug/releases/latest")
            apiClient.newCall(latestReq).execute().use { response ->
                when (response.code) {
                    in 200..299 -> {
                        val bodyStr = response.body?.string()
                            ?: return@withContext Result.failure(IllegalStateException("پاسخ سرور خالی بود."))
                        return@withContext parseReleaseJson(JSONObject(bodyStr), currentVersion)
                    }
                    403, 429 -> {
                        return@withContext Result.failure(
                            IllegalStateException("محدودیت موقت درخواست به گیت‌هاب (کد ${response.code}). چند دقیقه دیگر دوباره تلاش کنید.")
                        )
                    }
                    404 -> {
                        // Continue to /releases fallback below
                    }
                    else -> {
                        return@withContext Result.failure(
                            IllegalStateException("خطا در ارتباط با سرور آپدیت (کد ${response.code})")
                        )
                    }
                }
            }

            // Fallback: /releases/latest returns 404 when no release exists yet or only prereleases exist
            val releasesReq = githubGetRequest("https://api.github.com/repos/$slug/releases?per_page=10")
            apiClient.newCall(releasesReq).execute().use { relResp ->
                if (relResp.isSuccessful) {
                    val arrStr = relResp.body?.string() ?: "[]"
                    val arr = JSONArray(arrStr)
                    for (i in 0 until arr.length()) {
                        val rel = arr.optJSONObject(i) ?: continue
                        if (!rel.optBoolean("draft", false)) {
                            return@withContext parseReleaseJson(rel, currentVersion)
                        }
                    }
                    return@withContext Result.success(
                        UpdateCheckResult.UpToDate(
                            currentVersion = currentVersion,
                            latestVersion = normalizeVersion(currentVersion)
                        )
                    )
                }
                if (relResp.code == 403 || relResp.code == 429) {
                    return@withContext Result.failure(
                        IllegalStateException("محدودیت موقت درخواست به گیت‌هاب (کد ${relResp.code}). چند دقیقه دیگر دوباره تلاش کنید.")
                    )
                }
            }

            // Check repository reachability
            val repoReq = githubGetRequest("https://api.github.com/repos/$slug")
            apiClient.newCall(repoReq).execute().use { repoResp ->
                return@withContext when {
                    repoResp.isSuccessful -> Result.success(
                        UpdateCheckResult.UpToDate(
                            currentVersion = currentVersion,
                            latestVersion = normalizeVersion(currentVersion)
                        )
                    )
                    repoResp.code == 404 -> Result.failure(
                        IllegalStateException("اطلاعات بروزرسانی در دسترس نیست (ممکن است مخزن در حالت Private باشد).")
                    )
                    repoResp.code == 403 || repoResp.code == 429 -> Result.failure(
                        IllegalStateException("محدودیت موقت درخواست (کد ${repoResp.code}).")
                    )
                    else -> Result.failure(
                        IllegalStateException("خطا در بررسی نسخه (کد ${repoResp.code})")
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("خطا در اتصال به اینترنت: ${e.localizedMessage ?: "نامشخص"}")
            )
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var targetFile: File? = null
        try {
            if (!downloadUrl.startsWith("https://", ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("آدرس دانلود نامعتبر است."))
            }
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()
            // Clean previous downloaded APKs to save cache space
            updatesDir.listFiles()?.forEach { oldFile ->
                if (oldFile.isFile) oldFile.delete()
            }

            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
            val resolvedFile = File(updatesDir, safeName)
            targetFile = resolvedFile

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "GYM-PRO-Android-Updater")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("خطا در دانلود فایل آپدیت (کد ${response.code})")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("فایل دانلودی خالی است."))
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(resolvedFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var lastReportedPercent = -1
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val percent = if (totalBytes > 0L) {
                                ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            if (percent != lastReportedPercent || percent == 100) {
                                lastReportedPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress(percent, downloaded, totalBytes)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }

            Result.success(resolvedFile)
        } catch (e: CancellationException) {
            targetFile?.delete()
            throw e
        } catch (e: Exception) {
            targetFile?.delete()
            Result.failure(e)
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File): InstallLaunchStatus {
        return try {
            if (!apkFile.exists()) return InstallLaunchStatus.ERROR

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                openInstallUnknownSourcesSettings(context)
                return InstallLaunchStatus.NEEDS_PERMISSION
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            InstallLaunchStatus.LAUNCHED
        } catch (_: Exception) {
            InstallLaunchStatus.ERROR
        }
    }
}
