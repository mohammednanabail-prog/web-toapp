package com.example.model

import android.graphics.Bitmap
import java.io.File

enum class SourceType {
    URL,
    HTML_CODE
}

data class AppBuildConfig(
    val appName: String,
    val sourceType: SourceType,
    val url: String = "https://",
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    val customPackageName: String = "",
    val iconBitmap: Bitmap? = null,
    val enableDownloads: Boolean = true,
    val enableShare: Boolean = true,
    val enablePrint: Boolean = true,
    val enableOfflineCache: Boolean = true
)

enum class BuildStep(val arabicTitle: String, val englishTitle: String) {
    INITIALIZING("تهيئة بيئة البناء", "Initializing build environment"),
    PREPARING_ASSETS("تجهيز ملفات الويب والتهيئة", "Preparing web assets & configuration"),
    GENERATING_ICONS("توليد أيقونات التطبيق بكل المقاسات", "Generating multi-density icons"),
    PATCHING_MANIFEST("تعديل AndroidManifest وتخصيص الحزمة", "Patching AndroidManifest & Package ID"),
    PATCHING_RESOURCES("تعديل resources.arsc واسم التطبيق", "Patching resources.arsc & App Label"),
    ALIGNING_AND_PACKING("ضغط ملفات APK بمحاذاة ZipAlign الدقيقة", "Assembling APK with Zip alignment"),
    SIGNING_APK("توقيع الحزمة بشهادة V1/V2 Cryptographic", "Cryptographically signing APK (v1/v2)"),
    FINALIZING("حفظ الحزمة النهائية", "Finalizing installable APK"),
    COMPLETED("اكتمل البناء بنجاح!", "Build completed successfully!")
}

sealed class BuildState {
    object Idle : BuildState()
    data class Building(
        val step: BuildStep,
        val progressPercent: Float,
        val message: String
    ) : BuildState()
    data class Success(
        val apkFile: File,
        val appName: String,
        val packageName: String,
        val fileSizeFormatted: String,
        val fileSizeBytes: Long
    ) : BuildState()
    data class Error(val message: String) : BuildState()
}

data class BuiltAppRecord(
    val id: String,
    val appName: String,
    val packageName: String,
    val apkPath: String,
    val iconPath: String? = null,
    val fileSizeFormatted: String,
    val createdAt: Long,
    val sourceType: SourceType,
    val targetUrl: String? = null
)

object SampleTemplates {
    val MINI_GAME = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>Coin Clicker Game</title>
<style>
  body {
    margin: 0;
    background: linear-gradient(135deg, #0F172A, #1E1B4B);
    color: white;
    font-family: system-ui, -apple-system, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    overflow: hidden;
    text-align: center;
    touch-action: manipulation;
  }
  .coin {
    width: 140px;
    height: 140px;
    border-radius: 50%;
    background: radial-gradient(circle at 35% 35%, #FDE047, #CA8A04);
    box-shadow: 0 0 35px rgba(234, 179, 8, 0.6), inset 0 0 15px rgba(0,0,0,0.3);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 55px;
    cursor: pointer;
    user-select: none;
    transition: transform 0.1s ease;
    margin: 30px auto;
  }
  .coin:active {
    transform: scale(0.92);
  }
  .score {
    font-size: 38px;
    font-weight: 800;
    color: #FACC15;
    text-shadow: 0 2px 10px rgba(0,0,0,0.5);
  }
  .btn-row {
    display: flex;
    gap: 12px;
    margin-top: 25px;
  }
  button {
    background: #3B82F6;
    color: white;
    border: none;
    padding: 12px 24px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
  }
</style>
</head>
<body>
  <h2>🪙 Mini Coin Clicker</h2>
  <div class="score" id="score">0</div>
  <p>اضغط على العملة لجمع النقاط!</p>
  <div class="coin" onclick="clickCoin()">⭐</div>
  <div class="btn-row">
    <button onclick="shareScore()">📤 مشاركة النتيجة</button>
    <button onclick="downloadScoreReport()" style="background:#10B981">📥 تحميل التقرير (Blob)</button>
  </div>
  <script>
    let score = 0;
    function clickCoin() {
      score++;
      document.getElementById('score').innerText = score;
    }
    function shareScore() {
      if (navigator.share) {
        navigator.share({
          title: 'Coin Clicker Record',
          text: 'لقد حققت ' + score + ' نقطة في لعبة Coin Clicker!',
          url: window.location.href
        }).catch(console.error);
      } else {
        alert('مجموع نقاطك: ' + score);
      }
    }
    function downloadScoreReport() {
      const report = 'تقرير لعبة النقر\nالتاريخ: ' + new Date().toLocaleString() + '\nالنقاط: ' + score;
      const blob = new Blob([report], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'score_report_' + score + '.txt';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
  </script>
</body>
</html>
""".trimIndent()

    val BUSINESS_PORTFOLIO = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Business Portfolio</title>
<style>
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    background: #F8FAFC;
    color: #1E293B;
  }
  header {
    background: linear-gradient(135deg, #1E40AF, #3B82F6);
    color: white;
    padding: 40px 20px;
    text-align: center;
    border-bottom-left-radius: 25px;
    border-bottom-right-radius: 25px;
  }
  header h1 { margin: 0 0 10px 0; font-size: 26px; }
  .container { padding: 20px; max-width: 600px; margin: auto; }
  .card {
    background: white;
    border-radius: 16px;
    padding: 20px;
    margin-bottom: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    border: 1px solid #E2E8F0;
  }
  .card h3 { margin-top: 0; color: #1E40AF; }
  .btn {
    display: inline-block;
    width: 100%;
    text-align: center;
    background: #2563EB;
    color: white;
    padding: 14px;
    border-radius: 12px;
    text-decoration: none;
    font-weight: 600;
    margin-top: 10px;
    border: none;
    cursor: pointer;
  }
  .btn.secondary { background: #059669; }
</style>
</head>
<body>
  <header>
    <h1>شركة الحلول الذكية</h1>
    <p>تطبيق الويب الرسمي للهاتف المحمول</p>
  </header>
  <div class="container">
    <div class="card">
      <h3>🚀 خدماتنا</h3>
      <p>نقدم حلول البرمجيات المتقدمة، وتصميم المواقع وتطبيقات الهواتف الذكية بأعلى كفاءة.</p>
      <button class="btn" onclick="printBrochure()">🖨️ طباعة كتيب الخدمات (Print)</button>
    </div>
    <div class="card">
      <h3>📞 تواصل معنا</h3>
      <p>فريقنا متاح على مدار الساعة للإجابة على جميع استفساراتكم.</p>
      <button class="btn secondary" onclick="shareApp()">📤 مشاركة التطبيق مع صديق</button>
    </div>
  </div>
  <script>
    function printBrochure() {
      window.print();
    }
    function shareApp() {
      if (navigator.share) {
        navigator.share({
          title: 'شركة الحلول الذكية',
          text: 'تعرف على خدمات شركة الحلول الذكية عبر تطبيقنا المخصص!',
          url: 'https://smartsolutions.example'
        });
      } else {
        alert('شكراً لاهتمامكم!');
      }
    }
  </script>
</body>
</html>
""".trimIndent()
}
