package com.example.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.builder.IconGenerator
import com.example.model.BuildState
import com.example.model.SampleTemplates
import com.example.model.SourceType
import com.example.runner.WebPreviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val sourceType by viewModel.sourceType.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val htmlCode by viewModel.htmlCode.collectAsState()
    val cssCode by viewModel.cssCode.collectAsState()
    val jsCode by viewModel.jsCode.collectAsState()
    val appName by viewModel.appName.collectAsState()
    val customPackageName by viewModel.customPackageName.collectAsState()
    val iconBitmap by viewModel.iconBitmap.collectAsState()
    val enableDownloads by viewModel.enableDownloads.collectAsState()
    val enableShare by viewModel.enableShare.collectAsState()
    val enablePrint by viewModel.enablePrint.collectAsState()
    val enableOfflineCache by viewModel.enableOfflineCache.collectAsState()
    val buildState by viewModel.buildState.collectAsState()
    val history by viewModel.history.collectAsState()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var codeTabSelection by remember { mutableIntStateOf(0) } // 0: HTML, 1: CSS, 2: JS

    // Image Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.setIconBitmap(bitmap)
            } catch (e: Exception) {
                // Ignore decoding error
            }
        }
    }

    val htmlFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader(Charsets.UTF_8).use { r -> r.readText() }
                    viewModel.setHtmlCode(content)
                    codeTabSelection = 0
                    Toast.makeText(context, "تم استيراد ملف HTML بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WebToAPK",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "محوّل الويب إلى تطبيقات أندرويد",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("open_history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (history.isNotEmpty()) {
                                    Badge { Text("${history.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startBuild() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("build_apk_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "بناء تطبيق APK الآن",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // 1. Source Type Segmented Switcher
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "١. اختيار مصدر المحتوى",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = sourceType == SourceType.URL,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.setSourceType(SourceType.URL)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = sourceType == SourceType.URL) {
                                        Icon(
                                            Icons.Default.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("source_url_tab")
                            ) {
                                Text("رابط ويب (URL)", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            SegmentedButton(
                                selected = sourceType == SourceType.HTML_CODE,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.setSourceType(SourceType.HTML_CODE)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = sourceType == SourceType.HTML_CODE) {
                                        Icon(
                                            Icons.Default.Code,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("source_html_tab")
                            ) {
                                Text("كود محلي (Code)", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 2. Input Card (URL or Local Code)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (sourceType == SourceType.URL) {
                            Text(
                                text = "رابط الموقع الإلكتروني",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سيتم فتح هذا الرابط تلقائياً داخل WebView كامل الميزات والتحميلات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { viewModel.setUrl(it) },
                                label = { Text("https://example.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Http, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("url_input_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = {
                                    if (urlInput.isNotBlank() && urlInput != "https://") {
                                        IconButton(onClick = { viewModel.setUrl("https://") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                viewModel.setUrl(clip.trim())
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Quick preview button
                            FilledTonalButton(
                                onClick = { showPreviewDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preview_url_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("معاينة الرابط في مشغل الويب المباشر")
                            }

                        } else {
                            // HTML Code Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "محرر الكود المصدري المحلي",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "يتم تضمين الكود داخل Assets ليعمل بدون إنترنت 100%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { htmlFilePickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_html_file_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📂 رفع ملف HTML من الهاتف (.html)", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            when (codeTabSelection) {
                                                0 -> viewModel.setHtmlCode(clip)
                                                1 -> viewModel.setCssCode(clip)
                                                else -> viewModel.setJsCode(clip)
                                            }
                                            Toast.makeText(context, "تم لصق الكود من الحافظة!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("paste_code_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("لصق الكود من الحافظة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        when (codeTabSelection) {
                                            0 -> viewModel.setHtmlCode("")
                                            1 -> viewModel.setCssCode("")
                                            else -> viewModel.setJsCode("")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .testTag("clear_code_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Templates chips
                            Text(
                                text = "قوالب جاهزة للتجربة السريعة:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = htmlCode == SampleTemplates.MINI_GAME,
                                        onClick = { viewModel.loadTemplate(SampleTemplates.MINI_GAME) },
                                        label = { Text("🪙 لعبة نقر تفاعلية") },
                                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = htmlCode == SampleTemplates.BUSINESS_PORTFOLIO,
                                        onClick = { viewModel.loadTemplate(SampleTemplates.BUSINESS_PORTFOLIO) },
                                        label = { Text("💼 تطبيق أعمال وخدمات") },
                                        leadingIcon = { Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tabs: HTML / CSS / JS
                            TabRow(
                                selectedTabIndex = codeTabSelection,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Tab(
                                    selected = codeTabSelection == 0,
                                    onClick = {
                                        focusManager.clearFocus()
                                        codeTabSelection = 0
                                    },
                                    text = { Text("كود مدمج / HTML") },
                                    icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = codeTabSelection == 1,
                                    onClick = {
                                        focusManager.clearFocus()
                                        codeTabSelection = 1
                                    },
                                    text = { Text("CSS (التنسيق)") },
                                    icon = { Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = codeTabSelection == 2,
                                    onClick = {
                                        focusManager.clearFocus()
                                        codeTabSelection = 2
                                    },
                                    text = { Text("JavaScript") },
                                    icon = { Icon(Icons.Default.Javascript, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val currentCodeValue = when (codeTabSelection) {
                                0 -> htmlCode
                                1 -> cssCode
                                else -> jsCode
                            }
                            val currentPlaceholder = when (codeTabSelection) {
                                0 -> "<!DOCTYPE html>\n<html>..."
                                1 -> "/* CSS Styles */\nbody { background: #000; color: #fff; }"
                                else -> "// JavaScript Code\nconsole.log('App started');"
                            }

                            OutlinedTextField(
                                value = currentCodeValue,
                                onValueChange = { newValue ->
                                    when (codeTabSelection) {
                                        0 -> viewModel.setHtmlCode(newValue)
                                        1 -> viewModel.setCssCode(newValue)
                                        else -> viewModel.setJsCode(newValue)
                                    }
                                },
                                placeholder = { Text(currentPlaceholder) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .testTag("code_editor_input"),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FilledTonalButton(
                                onClick = { showPreviewDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preview_code_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("معاينة الكود المكتوب مباشرة")
                            }
                        }
                    }
                }
            }

            // 3. App Identity Card (Name & Package)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "٢. معلومات وهوية التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = appName,
                            onValueChange = { viewModel.setAppName(it) },
                            label = { Text("اسم التطبيق الظاهر على الهاتف (App Label)") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_name_input_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customPackageName,
                            onValueChange = { viewModel.setCustomPackageName(it) },
                            label = { Text("معرّف الحزمة المخصص (Package Name - اختياري)") },
                            placeholder = { Text("يتم توليده تلقائياً وفريداً في حال تركه فارغاً") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_package_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }

            // 4. App Icon Generator & Customizer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "٣. أيقونة التطبيق (App Launcher Icon)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يتم توليد جميع كثافات Mipmap المطلوبة تلقائياً (hdpi إلى xxxhdpi)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Mockup Preview
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        )
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap!!.asImageBitmap(),
                                            contentDescription = "Icon preview",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = appName.trim().take(1).uppercase().ifEmpty { "W" },
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = appName.take(10).ifEmpty { "تطبيقي" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("upload_icon_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("رفع صورة من الهاتف")
                                }

                                if (iconBitmap != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.setIconBitmap(null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reset_icon_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("استعادة الأيقونة الافتراضية")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Advanced Settings Card (Expandable)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedSettings = !showAdvancedSettings },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "٤. الميزات التفاعلية المتقدمة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showAdvancedSettings = !showAdvancedSettings }) {
                                Icon(
                                    if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showAdvancedSettings,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SettingSwitchRow(
                                    title = "اعتراض تحميلات Blob عبر JS",
                                    description = "التقاط أزرار تصدير PDF والملفات المولدة بجافاسكريبت بدون أخطاء",
                                    icon = Icons.Default.Download,
                                    checked = enableDownloads,
                                    onCheckedChange = { viewModel.toggleDownloads(it) }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                SettingSwitchRow(
                                    title = "محاكاة Web Share API (navigator.share)",
                                    description = "دعم كامل لمشاركة النصوص والملفات والصور مع التطبيقات الخارجية",
                                    icon = Icons.Default.Share,
                                    checked = enableShare,
                                    onCheckedChange = { viewModel.toggleShare(it) }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                SettingSwitchRow(
                                    title = "دعم الطباعة الرسمية (PrintManager)",
                                    description = "تمكين window.print() وحفظ الصفحة بصيغة PDF",
                                    icon = Icons.Default.Print,
                                    checked = enablePrint,
                                    onCheckedChange = { viewModel.togglePrint(it) }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                SettingSwitchRow(
                                    title = "تفعيل التخزين المؤقت الذكي (Cache)",
                                    description = "تفادي الشاشة البيضاء عند انقطاع الشبكة",
                                    icon = Icons.Default.Security,
                                    checked = enableOfflineCache,
                                    onCheckedChange = { viewModel.toggleOfflineCache(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    when (val state = buildState) {
        is BuildState.Building -> {
            BuildProgressDialog(
                state = state,
                onCancel = { viewModel.resetBuildState() }
            )
        }
        is BuildState.Success -> {
            BuildSuccessDialog(
                success = state,
                onDismiss = { viewModel.resetBuildState() },
                onTestInPreview = {
                    showPreviewDialog = true
                    viewModel.resetBuildState()
                }
            )
        }
        is BuildState.Error -> {
            BuildErrorDialog(
                message = state.message,
                onDismiss = { viewModel.resetBuildState() }
            )
        }
        is BuildState.Idle -> { /* do nothing */ }
    }

    if (showHistorySheet) {
        HistorySheet(
            records = history,
            onDismiss = { showHistorySheet = false },
            onDelete = { viewModel.deleteHistoryItem(it) }
        )
    }

    if (showPreviewDialog) {
        var finalUrl = urlInput.trim()
        if (sourceType == SourceType.URL && !finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://$finalUrl"
        }

        var fullHtml = htmlCode
        if (cssCode.isNotBlank() && !fullHtml.contains("<style>")) {
            fullHtml = "<style>\n$cssCode\n</style>\n$fullHtml"
        }
        if (jsCode.isNotBlank() && !fullHtml.contains("<script>")) {
            fullHtml = "$fullHtml\n<script>\n$jsCode\n</script>"
        }

        WebPreviewDialog(
            appName = appName,
            sourceType = sourceType,
            url = finalUrl,
            htmlCode = fullHtml,
            onDismiss = { showPreviewDialog = false }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
