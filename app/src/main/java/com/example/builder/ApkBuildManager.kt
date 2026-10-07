package com.example.builder

import android.content.Context
import android.os.Environment
import com.example.model.AppBuildConfig
import com.example.model.BuildState
import com.example.model.BuildStep
import com.example.model.BuiltAppRecord
import com.example.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ApkBuildManager(private val context: Context) {

    private val packer = ZipAlignAndPacker()
    private val signer = ApkSigningEngine(context)

    fun buildApk(config: AppBuildConfig): Flow<BuildState> = flow {
        try {
            emit(BuildState.Building(BuildStep.INITIALIZING, 0.05f, "تهيئة مسارات العمل والمكتبات الثنائية..."))

            val buildDir = File(context.cacheDir, "build_${System.currentTimeMillis()}").apply { mkdirs() }
            val unsignedAlignedFile = File(buildDir, "unsigned_aligned.apk")
            val signedApkFile = File(buildDir, "signed_output.apk")

            // 1. Generate unique package name
            val uniquePackageName = if (config.customPackageName.isNotBlank()) {
                config.customPackageName.trim()
            } else {
                "com.webtoapk.gen" + System.currentTimeMillis().toString(36) + (100..999).random()
            }

            emit(BuildState.Building(BuildStep.PREPARING_ASSETS, 0.20f, "إعداد بيانات التكوين وملفات HTML/JS..."))

            // Prepare app_config.json
            val configJson = JSONObject().apply {
                put("type", if (config.sourceType == SourceType.URL) "url" else "local")
                put("url", config.url.trim())
                put("appName", config.appName.trim())
                put("enableDownloads", config.enableDownloads)
                put("enableShare", config.enableShare)
                put("enablePrint", config.enablePrint)
                put("enableOfflineCache", config.enableOfflineCache)
            }.toString(2)

            // Prepare local web files
            val webFiles = mutableMapOf<String, ByteArray>()
            if (config.sourceType == SourceType.HTML_CODE) {
                var fullHtml = config.htmlCode
                if (config.cssCode.isNotBlank() && !fullHtml.contains("<style>")) {
                    fullHtml = fullHtml.replace("</head>", "<style>\n${config.cssCode}\n</style>\n</head>")
                    if (!fullHtml.contains("</head>")) {
                        fullHtml = "<style>\n${config.cssCode}\n</style>\n$fullHtml"
                    }
                }
                if (config.jsCode.isNotBlank() && !fullHtml.contains("<script>")) {
                    fullHtml = fullHtml.replace("</body>", "<script>\n${config.jsCode}\n</script>\n</body>")
                    if (!fullHtml.contains("</body>")) {
                        fullHtml = "$fullHtml\n<script>\n${config.jsCode}\n</script>"
                    }
                }
                webFiles["index.html"] = fullHtml.toByteArray(Charsets.UTF_8)
                if (config.cssCode.isNotBlank()) {
                    webFiles["style.css"] = config.cssCode.toByteArray(Charsets.UTF_8)
                }
                if (config.jsCode.isNotBlank()) {
                    webFiles["script.js"] = config.jsCode.toByteArray(Charsets.UTF_8)
                }
            }

            // 2. Generate Mipmaps
            emit(BuildState.Building(BuildStep.GENERATING_ICONS, 0.35f, "توليد أيقونات الهاتف بدقة عالية لكل كثافات Mipmap..."))
            val generatedIcons = IconGenerator.generateAllMipmaps(config.iconBitmap, config.appName)

            // 3. Extract manifest & resources.arsc from template.apk
            val (originalManifestBytes, originalArscBytes) = extractTemplateCoreFiles()

            // 4. Binary Patch AndroidManifest.xml
            emit(BuildState.Building(BuildStep.PATCHING_MANIFEST, 0.50f, "تعديل حزمة التطبيق الثنائية والصلاحيات (AXML)..."))
            val patchedManifest = ManifestPatcher.patchManifest(originalManifestBytes, uniquePackageName)

            // 5. Binary Patch resources.arsc
            emit(BuildState.Building(BuildStep.PATCHING_RESOURCES, 0.65f, "تعديل جدول الموارد واسم التطبيق في resources.arsc..."))
            val patchedArsc = ArscPatcher.patchResourcesArsc(originalArscBytes, config.appName)

            // 6. Zip Align and Pack with STORED assets and resources.arsc
            emit(BuildState.Building(BuildStep.ALIGNING_AND_PACKING, 0.80f, "إعادة التجميع بمحاذاة 4-Byte ZipAlign وحفظ ملفات Assets غير مضغوطة..."))
            context.assets.open("template.apk").use { stream ->
                packer.repackAndAlign(
                    templateApkStream = stream,
                    patchedManifest = patchedManifest,
                    patchedArsc = patchedArsc,
                    icons = generatedIcons,
                    appConfigJson = configJson,
                    webFiles = webFiles,
                    outputFile = unsignedAlignedFile
                )
            }

            // 7. Cryptographic Signing (v1/v2/v3 scheme)
            emit(BuildState.Building(BuildStep.SIGNING_APK, 0.90f, "توقيع APK رقمياً (APK Signature Scheme v2/v1)..."))
            signer.signApk(unsignedAlignedFile, signedApkFile)

            // 8. Copy to final output directory
            emit(BuildState.Building(BuildStep.FINALIZING, 0.95f, "حفظ ملف APK النهائي وتجهيز خيارات التثبيت..."))
            val sanitizedName = config.appName.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_").take(30).ifEmpty { "WebApp" }
            val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "built_apks").apply { mkdirs() }
            val finalOutputFile = File(outputDir, "${sanitizedName}_${System.currentTimeMillis().toString(36)}.apk")

            signedApkFile.copyTo(finalOutputFile, overwrite = true)

            // Save icon image for history
            val savedIconFile = File(outputDir, "${finalOutputFile.nameWithoutExtension}_icon.png")
            generatedIcons.squareIcons[IconGenerator.Density.HDPI]?.let {
                FileOutputStream(savedIconFile).use { fos -> fos.write(it) }
            }

            val sizeFormatted = formatFileSize(finalOutputFile.length())

            // Save to history
            saveToHistory(
                BuiltAppRecord(
                    id = finalOutputFile.name,
                    appName = config.appName,
                    packageName = uniquePackageName,
                    apkPath = finalOutputFile.absolutePath,
                    iconPath = savedIconFile.absolutePath,
                    fileSizeFormatted = sizeFormatted,
                    createdAt = System.currentTimeMillis(),
                    sourceType = config.sourceType,
                    targetUrl = if (config.sourceType == SourceType.URL) config.url else null
                )
            )

            // Clean up temporary build dir
            try {
                buildDir.deleteRecursively()
            } catch (ignored: Exception) {}

            emit(BuildState.Building(BuildStep.COMPLETED, 1.0f, "اكتمل البناء بنجاح! جاهز للتثبيت."))
            emit(
                BuildState.Success(
                    apkFile = finalOutputFile,
                    appName = config.appName,
                    packageName = uniquePackageName,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = finalOutputFile.length()
                )
            )

        } catch (e: Exception) {
            emit(BuildState.Error("فشل بناء الحزمة: ${e.message ?: "خطأ غير متوقع"}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun extractTemplateCoreFiles(): Pair<ByteArray, ByteArray> {
        var manifestBytes: ByteArray? = null
        var arscBytes: ByteArray? = null

        context.assets.open("template.apk").use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "AndroidManifest.xml" -> {
                            manifestBytes = readStream(zis)
                        }
                        "resources.arsc" -> {
                            arscBytes = readStream(zis)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (manifestBytes == null || arscBytes == null) {
            throw IllegalStateException("Failed to extract AndroidManifest.xml or resources.arsc from template.apk")
        }

        return Pair(manifestBytes!!, arscBytes!!)
    }

    private fun readStream(stream: java.io.InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var read: Int
        while (stream.read(buf).also { read = it } != -1) {
            baos.write(buf, 0, read)
        }
        return baos.toByteArray()
    }

    fun getBuildHistory(): List<BuiltAppRecord> {
        val prefs = context.getSharedPreferences("built_apps_history", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("records", "[]") ?: "[]"
        val list = mutableListOf<BuiltAppRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BuiltAppRecord(
                        id = obj.getString("id"),
                        appName = obj.getString("appName"),
                        packageName = obj.getString("packageName"),
                        apkPath = obj.getString("apkPath"),
                        iconPath = obj.optString("iconPath", null),
                        fileSizeFormatted = obj.getString("fileSizeFormatted"),
                        createdAt = obj.getLong("createdAt"),
                        sourceType = SourceType.valueOf(obj.getString("sourceType")),
                        targetUrl = obj.optString("targetUrl", null)
                    )
                )
            }
        } catch (ignored: Exception) {}
        return list.sortedByDescending { it.createdAt }
    }

    private fun saveToHistory(record: BuiltAppRecord) {
        val current = getBuildHistory().toMutableList()
        current.removeAll { it.apkPath == record.apkPath }
        current.add(0, record)

        val arr = JSONArray()
        for (item in current.take(30)) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("appName", item.appName)
                put("packageName", item.packageName)
                put("apkPath", item.apkPath)
                put("iconPath", item.iconPath)
                put("fileSizeFormatted", item.fileSizeFormatted)
                put("createdAt", item.createdAt)
                put("sourceType", item.sourceType.name)
                put("targetUrl", item.targetUrl)
            }
            arr.put(obj)
        }

        context.getSharedPreferences("built_apps_history", Context.MODE_PRIVATE)
            .edit()
            .putString("records", arr.toString())
            .apply()
    }

    fun deleteHistoryRecord(record: BuiltAppRecord) {
        try {
            File(record.apkPath).delete()
            record.iconPath?.let { File(it).delete() }
        } catch (ignored: Exception) {}

        val current = getBuildHistory().toMutableList()
        current.removeAll { it.id == record.id }

        val arr = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("appName", item.appName)
                put("packageName", item.packageName)
                put("apkPath", item.apkPath)
                put("iconPath", item.iconPath)
                put("fileSizeFormatted", item.fileSizeFormatted)
                put("createdAt", item.createdAt)
                put("sourceType", item.sourceType.name)
                put("targetUrl", item.targetUrl)
            }
            arr.put(obj)
        }

        context.getSharedPreferences("built_apps_history", Context.MODE_PRIVATE)
            .edit()
            .putString("records", arr.toString())
            .apply()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
