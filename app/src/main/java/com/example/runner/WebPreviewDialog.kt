package com.example.runner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreviewDialog(
    appName: String,
    sourceType: SourceType,
    url: String,
    htmlCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember { mutableStateOf(appName) }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Preview Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("preview_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = pageTitle.ifEmpty { appName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (sourceType == SourceType.URL) url else "Local HTML Mode (كود محلي)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Print action
                    IconButton(
                        onClick = {
                            webViewInstance?.let { wv ->
                                try {
                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                    val adapter = wv.createPrintDocumentAdapter(appName)
                                    printManager.print("$appName Document", adapter, PrintAttributes.Builder().build())
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("preview_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print document")
                    }

                    // Reload action
                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.testTag("preview_reload_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload preview")
                    }
                }

                if (progress in 0.01f..0.99f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // WebView container
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                isVerticalScrollBarEnabled = true
                                isHorizontalScrollBarEnabled = false

                                try {
                                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                } catch (e: Exception) {
                                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                }

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                        mediaPlaybackRequiresUserGesture = false
                                    }
                                }

                                // Injected bridges
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onBlobDownloaded(base64Data: String, filename: String, mimeType: String) {
                                        Handler(Looper.getMainLooper()).post {
                                            try {
                                                val clean = if (base64Data.contains(",")) {
                                                    base64Data.substring(base64Data.indexOf(",") + 1)
                                                } else base64Data
                                                val bytes = Base64.decode(clean, Base64.DEFAULT)
                                                val outDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: ctx.cacheDir
                                                val f = File(outDir, filename.ifEmpty { "download_${System.currentTimeMillis()}" })
                                                FileOutputStream(f).use { it.write(bytes) }
                                                Toast.makeText(ctx, "📥 تم حفظ ملف الـ Blob بنجاح: ${f.name}", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(ctx, "فشل تحميل Blob: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }, "AndroidBlobDownloader")

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun shareText(title: String, text: String, url: String) {
                                        Handler(Looper.getMainLooper()).post {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                                                putExtra(Intent.EXTRA_TEXT, "$text $url".trim())
                                            }
                                            ctx.startActivity(Intent.createChooser(intent, "Share"))
                                        }
                                    }

                                    @JavascriptInterface
                                    fun shareFiles(title: String, text: String, url: String, filesJson: String) {
                                        Handler(Looper.getMainLooper()).post {
                                            try {
                                                val arr = JSONArray(filesJson)
                                                val uris = ArrayList<Uri>()
                                                val shareDir = File(ctx.cacheDir, "preview_shared").apply { mkdirs() }
                                                for (i in 0 until arr.length()) {
                                                    val obj = arr.getJSONObject(i)
                                                    val name = obj.optString("name", "file_$i")
                                                    var data = obj.optString("data", "")
                                                    if (data.contains(",")) data = data.substring(data.indexOf(",") + 1)
                                                    val bytes = Base64.decode(data, Base64.DEFAULT)
                                                    val f = File(shareDir, name)
                                                    FileOutputStream(f).use { it.write(bytes) }
                                                    val u = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                                                    uris.add(u)
                                                }
                                                val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                                                    type = "*/*"
                                                    if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0])
                                                    else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                                                    val body = "$text $url".trim()
                                                    if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                                                }
                                                ctx.startActivity(Intent.createChooser(intent, "Share Files"))
                                            } catch (e: Exception) {
                                                Toast.makeText(ctx, "فشل مشاركة الملفات: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }, "AndroidShareBridge")

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun print() {
                                        Handler(Looper.getMainLooper()).post {
                                            try {
                                                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                                val ad = createPrintDocumentAdapter(appName)
                                                pm.print("$appName Print", ad, PrintAttributes.Builder().build())
                                            } catch (e: Exception) {
                                                Toast.makeText(ctx, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }, "AndroidPrintBridge")

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress / 100f
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) {
                                            pageTitle = title
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val u = request?.url ?: return false
                                        val scheme = u.scheme
                                        if (scheme != "http" && scheme != "https" && scheme != "file") {
                                            try {
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, u))
                                                return true
                                            } catch (ignored: Exception) {}
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        // Inject hooks for Blob, Share shim, and Print
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                if (!window._previewInjected) {
                                                    window._previewInjected = true;
                                                    window.saveBlob = function(bUrl, name) {
                                                        fetch(bUrl).then(r => r.blob()).then(b => {
                                                            let reader = new FileReader();
                                                            reader.onloadend = () => {
                                                                if (window.AndroidBlobDownloader) {
                                                                    window.AndroidBlobDownloader.onBlobDownloaded(reader.result, name || 'download', b.type);
                                                                }
                                                            };
                                                            reader.readAsDataURL(b);
                                                        });
                                                    };
                                                    document.addEventListener('click', function(e) {
                                                        let t = e.target;
                                                        while (t && t.tagName !== 'A') t = t.parentElement;
                                                        if (t && t.href && t.href.indexOf('blob:') === 0) {
                                                            e.preventDefault();
                                                            window.saveBlob(t.href, t.getAttribute('download') || 'download');
                                                        }
                                                    }, true);
                                                    if (!navigator.share || navigator.share._isShim) {
                                                        navigator.share = function(data) {
                                                            return new Promise((resolve) => {
                                                                if (window.AndroidShareBridge) {
                                                                    window.AndroidShareBridge.shareText(data.title||'', data.text||'', data.url||'');
                                                                }
                                                                resolve();
                                                            });
                                                        };
                                                        navigator.canShare = () => true;
                                                    }
                                                    window.print = function() {
                                                        if (window.AndroidPrintBridge) window.AndroidPrintBridge.print();
                                                    };
                                                }
                                            })();
                                            """.trimIndent(),
                                            null
                                        )
                                    }
                                }

                                setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                                    if (downloadUrl.startsWith("blob:")) {
                                        evaluateJavascript("window.saveBlob('$downloadUrl', '${URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)}');", null)
                                    } else {
                                        Toast.makeText(ctx, "HTTP Download: $downloadUrl", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                if (sourceType == SourceType.URL) {
                                    loadUrl(url)
                                } else {
                                    loadDataWithBaseURL("https://local.webapp/", htmlCode, "text/html", "UTF-8", null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
