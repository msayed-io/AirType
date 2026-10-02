# LiveType Remote — تطبيق الأندرويد

تطبيق Android أصلي (Kotlin + Jetpack Compose + Material 3) يعمل كنظير لتطبيق الويندوز **LiveType Bridge**: تكتب أو تملي صوتيًا على الهاتف (بأي لوحة مفاتيح، وخاصة Gboard)، فيظهر النص لحظيًا في الخانة النشطة على اللابتوب — عبر الشبكة المحلية فقط.

> لا يوجد تحويل صوت→نص داخل التطبيق. الإملاء الصوتي مسؤولية Gboard/لوحة النظام، والتطبيق يرسل النص الناتج فقط.

## طريقة عمله في 3 خطوات
1. شغّل **LiveType Bridge** على اللابتوب واضغط «ربط هاتف» — يظهر QR و PIN.
2. افتح هذا التطبيق → «ربط اللابتوب» → امسح الرمز (أو أدخل البيانات يدويًا).
3. اكتب/املِ في الخانة — كل تغيير يُرسل فورًا كعمليات دقيقة (`deleteCount` + `insertText` + `revision`).

## البنية
```
app/src/main/java/com/livetype/remote/
  core/
    protocol/    # عقد الاتصال (نفس JSON camelCase المستخدم في تطبيق ويندوز)
    textdiff/    # حساب الفرق بين النصوص (backspaces + insert)
    session/     # منطق البث: revisions، reset_stream، إعادة المزامنة
    network/     # OkHttp WebSocket + heartbeat + إعادة اتصال تلقائية
    pairing/     # تحليل QR + تخزين مشفر (EncryptedSharedPreferences)
  ui/            # Compose + Material 3، عربي RTL + إنجليزي + وضع داكن
app/src/test/    # اختبارات JVM: التوافق مع البروتوكول، TextDiff، ترتيب revisions
```

## بناء الـ APK

### عبر GitHub Actions (الموصى به)
المستودع يحتوي `.github/workflows/android-build.yml` — عند أي push أو من تبويب Actions بضغطة "Run workflow" يتم:
1. تشغيل اختبارات الوحدة.
2. بناء `app-debug.apk` (قابل للتثبيت مباشرة على الهاتف).
3. رفعه كـ Artifact باسم `LiveTypeRemote-debug-apk` — نزّله من صفحة الـ Run.

### بناء محلي (اختياري)
يتطلب JDK 17 + Android SDK (compileSdk 35):
```
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
# الناتج: app/build/outputs/apk/debug/app-debug.apk
```

## الاقتران مع برنامج الويندوز
- تأكد أن الهاتف واللابتوب على **نفس شبكة Wi-Fi** (أو نقطة اتصال الهاتف نفسها).
- عند أول اقتران من اللابتوب سيطلب ويندوز السماح عبر جدار الحماية — اختر «السماح» للشبكات الخاصة.
- رمز الـ QR يحتوي: العنوان + المنفذ + معرّف الجلسة + الـ PIN المؤقت (بدون token).
- بعد نجاح أول PIN يحفظ الهاتف `clientId + token` الدائم. في التشغيلات التالية ينتظر Beacon الحالي ثم يعيد الاتصال دون PIN، حتى بعد إعادة تشغيل الديسكتوب.
- الانقطاع المؤقت يعيد فقط revisions غير المؤكدة؛ الديسكتوب يحتفظ بمرآة الجلسة ويرفض أي replay مطبق، لمنع تكرار النص.

## الخصوصية والصلاحيات
- `INTERNET` و `ACCESS_NETWORK_STATE` للاتصال المحلي فقط — لا خوادم ولا تتبع.
- `CAMERA` لمسح رمز QR فقط (ML Kit bundled — لا يُنزّل أي شيء وقت الاستخدام).
- بيانات الاقتران (إن اخترت «تذكّر») محفوظة مشفرة عبر EncryptedSharedPreferences.
- لا يُطلب الميكروفون ولا الموقع ولا الملفات.

## بروتوكول الرسائل (نفس عقد تطبيق ويندوز)
| رسالة | الاتجاه | حقول أساسية |
|---|---|---|
| `pair_request` | هاتف→كمبيوتر | أول مرة: sessionId, pin, deviceName, clientId — لاحقًا: sessionId, clientId, token |
| `session_ready` | كمبيوتر→هاتف | ok, token, heartbeatIntervalMs |
| `text_edit` | هاتف→كمبيوتر | token, revision, deleteCount, insertText, timestamp |
| `ack` | كمبيوتر→هاتف | revision, applied, reason |
| `reset_stream` | بالاتجاهين | baselineRevision, appliedLength |
| `heartbeat` / `heartbeat_ack` | بالاتجاهين | timestamp, lastRttMs |
| `disconnect` / `error` | بالاتجاهين | reason / errorCode |

كل الحقول camelCase مثلما ينتجها `System.Text.Json` في تطبيق الويندوز، واختبار `ProtocolCompatTest` يثبّت هذا الشكل.
