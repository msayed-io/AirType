package com.livetype.remote.ui

/**
 * Lightweight in-code localization (no resource switching): Arabic is the default,
 * English is opt-in. All user-facing text lives here.
 */
object Strings {

    private val ar: Map<String, String> = mapOf(
        "app_title" to "AirType",
        "tagline" to "اكتب على هاتفك، يظهر على اللابتوب فورًا — نفس شبكة الواي فاي فقط، بدون إنترنت",
        "pair_laptop" to "ربط اللابتوب",
        "manual_entry" to "إدخال يدوي",
        "try_saved" to "إعادة الاتصال بالجهاز المحفوظ",
        "connect" to "اتصال",
        "cancel" to "إلغاء",
        "back" to "رجوع",

        "status_connected" to "متصل",
        "status_connecting" to "جارٍ الاقتران…",
        "status_reconnecting" to "يعيد الاتصال…",
        "status_disconnected" to "غير متصل",
        "status_paused" to "البث متوقف",
        "status_error" to "خطأ",

        "sub_connected" to "الإملاء والكتابة تُبث لحظيًا إلى الخانة النشطة",
        "sub_connecting" to "ننتظر تأكيد الخادم…",
        "sub_reconnecting" to "الجلسة ما زالت صالحة — نحاول الرجوع تلقائيًا",
        "sub_paused" to "تقدر تكتب محليًا، والبث يستأنف من الزر — لن يُرسل شيء الآن",

        "start_hint" to "ابدأ الإملاء أو الكتابة…",
        "clear_text" to "مسح النص",
        "stop_stream" to "إيقاف البث",
        "resume_stream" to "استئناف البث",
        "disconnect" to "قطع الاتصال",
        "diagnostics" to "التشخيص",

        "latency" to "التأخير المقاس",
        "no_measure" to "لا يوجد قياس بعد",
        "ops_sent" to "العمليات المُرسلة",
        "revision" to "آخر revision",
        "session_expires" to "تنتهي الجلسة",
        "last_error" to "آخر خطأ",
        "none" to "لا شيء",
        "laptop" to "اللابتوب",

        "scanner_title" to "امسح رمز الـ QR",
        "scanner_hint" to "افتح برنامج LiveType Bridge على اللابتوب واضغط «ربط هاتف»، ثم وجّه الكاميرا نحو الرمز",
        "manual_entry_title" to "إدخال يدوي",
        "host" to "عنوان الـ IP",
        "port" to "المنفذ",
        "session_id" to "معرّف الجلسة",
        "pin" to "رمز PIN (6 أرقام)",

        "perm_camera_title" to "إذن الكاميرا",
        "perm_camera_text" to "نستخدم الكاميرا لقراءة رمز الاقتران فقط. لا نصدّر أي صورة ولا نحتفظ بأي إطار — والمسح يتم داخل جهازك بالكامل.",
        "grant" to "منح الإذن",

        "paired_toast" to "تم الاقتران بنجاح ✓",

        "settings_title" to "الإعدادات",
        "language" to "اللغة",
        "arabic" to "العربية",
        "english" to "English",
        "vibrate_on_pair" to "اهتزاز خفيف عند نجاح الاقتران",
        "remember_pairing" to "تذكّر الاقتران (حفظ مشفّر على الهاتف)",
        "forget_pairing" to "نسيان الجهاز المحفوظ",

        "welcome_how" to "كيف يعمل؟",
        "welcome_step1" to "1. شغّل LiveType Bridge على اللابتوب واضغط «ربط هاتف»",
        "welcome_step2" to "2. امسح رمز QR من هنا",
        "welcome_step3" to "3. استخدم الإملاء الصوتي في Gboard أو اكتب — كل حرف يظهر على اللابتوب لحظيًا",
        "privacy_note" to "الاتصال محلي بين الهاتف واللابتوب فقط. لا يوجد إنترنت ولا خوادم ولا تسجيل.",

        // QR / protocol errors
        "err_generic" to "حدث خطأ غير متوقع",
        "err_network" to "تعذر الوصول إلى اللابتوب — تأكد أن الجهازين على نفس الشبكة وأن البرنامج يعمل",
        "err_session_expired" to "انتهت صلاحية الجلسة — أنشئ جلسة جديدة من برنامج اللابتوب",
        "err_bad_pin" to "رمز PIN غير صحيح — أعد المسح وتأكد من الرمز الظاهر على الشاشة",
        "err_unknown_session" to "الجلسة غير موجودة — أنشئ جلسة جديدة",
        "err_already_paired" to "هاتف آخر مرتبط بهذه الجلسة",
        "err_bad_token" to "رمز الجلسة غير صالح — أعد الاقتران",
        "err_unexpected_message" to "رد غير متوقع من الخادم — تأكد من تحديث البرنامجين",
        "err_malformed_message" to "رسالة تالفة — أعد الاقتران",
        "err_server_closing" to "أغلق الخادم الاتصال",
        "err_camera_denied" to "بدون إذن الكاميرا لا يمكن المسح — استخدم الإدخال اليدوي أو امنح الإذن",

        "err_NOT_JSON" to "الرمز المقروء ليس رمز LiveType Bridge صالحًا",
        "err_WRONG_APP" to "هذا الرمز من تطبيق آخر، ليس رمز LiveType Bridge",
        "err_EXPIRED" to "انتهت صلاحية هذا الرمز — أنشئ جلسة جديدة من اللابتوب",
        "err_BAD_PIN" to "رمز PIN داخل الرمز غير سليم",
        "err_BAD_ADDRESS" to "عنوان الشبكة داخل الرمز غير سليم",
        "err_MISSING_FIELDS" to "بيانات الرمز ناقصة — أكملها بالإدخال اليدوي",
    )

    private val en: Map<String, String> = mapOf(
        "app_title" to "AirType",
        "tagline" to "Type on your phone, it appears on your laptop instantly — same Wi-Fi only, no Internet",
        "pair_laptop" to "Pair laptop",
        "manual_entry" to "Manual entry",
        "try_saved" to "Reconnect to saved device",
        "connect" to "Connect",
        "cancel" to "Cancel",
        "back" to "Back",

        "status_connected" to "Connected",
        "status_connecting" to "Pairing…",
        "status_reconnecting" to "Reconnecting…",
        "status_disconnected" to "Not connected",
        "status_paused" to "Stream paused",
        "status_error" to "Error",

        "sub_connected" to "Dictation and typing stream live into the focused field",
        "sub_connecting" to "Waiting for the server to confirm…",
        "sub_reconnecting" to "Session is still valid — retrying automatically",
        "sub_paused" to "You can type locally; nothing is sent until you resume",

        "start_hint" to "Start dictating or typing…",
        "clear_text" to "Clear text",
        "stop_stream" to "Pause stream",
        "resume_stream" to "Resume stream",
        "disconnect" to "Disconnect",
        "diagnostics" to "Diagnostics",

        "latency" to "Measured latency",
        "no_measure" to "No measurement yet",
        "ops_sent" to "Ops sent",
        "revision" to "Last revision",
        "session_expires" to "Session expires",
        "last_error" to "Last error",
        "none" to "None",
        "laptop" to "Laptop",

        "scanner_title" to "Scan the QR code",
        "scanner_hint" to "Open LiveType Bridge on the laptop, press “Pair phone”, then point the camera at the code",
        "manual_entry_title" to "Manual entry",
        "host" to "IP address",
        "port" to "Port",
        "session_id" to "Session ID",
        "pin" to "PIN (6 digits)",

        "perm_camera_title" to "Camera permission",
        "perm_camera_text" to "The camera is used only to read the pairing code. No photo is taken or stored — scanning happens entirely on your device.",
        "grant" to "Grant permission",

        "paired_toast" to "Paired successfully ✓",

        "settings_title" to "Settings",
        "language" to "Language",
        "arabic" to "العربية",
        "english" to "English",
        "vibrate_on_pair" to "Light vibration when pairing succeeds",
        "remember_pairing" to "Remember pairing (encrypted on this phone)",
        "forget_pairing" to "Forget saved device",

        "welcome_how" to "How does it work?",
        "welcome_step1" to "1. Open LiveType Bridge on the laptop and press “Pair phone”",
        "welcome_step2" to "2. Scan the QR code from here",
        "welcome_step3" to "3. Use Gboard voice dictation or type — every character appears on the laptop instantly",
        "privacy_note" to "The connection is local between the phone and the laptop only. No Internet, no servers, no logging.",

        "err_generic" to "An unexpected error occurred",
        "err_network" to "Cannot reach the laptop — make sure both devices are on the same network and the app is running",
        "err_session_expired" to "Session expired — create a new one from the laptop app",
        "err_bad_pin" to "Wrong PIN — rescan and check the code shown on screen",
        "err_unknown_session" to "Unknown session — create a new one",
        "err_already_paired" to "Another phone holds this session",
        "err_bad_token" to "Invalid session token — pair again",
        "err_unexpected_message" to "Unexpected server reply — update both apps",
        "err_malformed_message" to "Corrupted message — pair again",
        "err_server_closing" to "The server closed the connection",
        "err_camera_denied" to "Without the camera permission, scanning is impossible — use manual entry or grant it",

        "err_NOT_JSON" to "The scanned code is not a valid LiveType Bridge code",
        "err_WRONG_APP" to "This code belongs to another app, not LiveType Bridge",
        "err_EXPIRED" to "This code has expired — create a new session on the laptop",
        "err_BAD_PIN" to "The PIN inside the code is malformed",
        "err_BAD_ADDRESS" to "The network address inside the code is malformed",
        "err_MISSING_FIELDS" to "The code is missing data — complete it with manual entry",
    )

    fun forLang(lang: String): Map<String, String> = if (lang == "en") en else ar
}

val LocalStrings = androidx.compose.runtime.staticCompositionLocalOf { Strings.forLang("ar") }

/** Read a localized string from anywhere in the Compose tree. */
@androidx.compose.runtime.Composable
fun s(key: String): String = LocalStrings.current[key] ?: key
