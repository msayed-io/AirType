# خط أساس: استمرار الجلسة في الخلفية والوضع العائم (مرحلة صفر)

تاريخ الفحص: 2026-10-26 · الالتزام المرجعي قبل التعديل: `c95d74b` (v1.0.7)

كل نتيجة مصنّفة: **مؤكد** (بالكود أو بمرجع رسمي) · **مرجّح** (استنتاج قوي بلا تشغيل) · **غير مؤكد** (يحتاج جهازًا).

## 1. التقنيات والإصدارات والبناء
- Kotlin + Jetpack Compose · AGP 8.7.3 · Gradle 8.9 · JDK 17.
- `compileSdk = 35` · `targetSdk = 35` · `minSdk = 26` — **مؤكد** (`app/build.gradle.kts`).
- التوقيع من أسرار بيئة `android-release`، و`versionCode/versionName` تُمرَّر من الـworkflow. **مؤكد**

## 2. الملفات المسؤولة
| المسؤولية | الملف |
|---|---|
| شاشة الكتابة | `ui/ConnectedScreen.kt` (`ConnectedScreen`, `SettingsScreen`) |
| حالة الواجهة والتنسيق | `ui/AppViewModel.kt` |
| الاتصال بسلك WebSocket | `core/network/BridgeConnection.kt` |
| نقل النص والـdiff والـrevision | `core/session/StreamSession.kt`, `core/textdiff/TextDiff.kt` |
| البروتوكول | `core/protocol/BridgeProtocol.kt` |
| الاقتران والتخزين | `core/pairing/QrPayloadParser.kt`, `core/pairing/PairingStore.kt` |
| الاكتشاف | `core/discovery/*` (UDP فقط) |
| دورة حياة التطبيق | `MainActivity.kt`, `AndroidManifest.xml` |

## 3. مسار بدء الاتصال وما يغلقه — السبب الجذري
- **مؤكد:** `AppViewModel.beginPairing()` كان ينشئ `BridgeConnection(client, **viewModelScope**, …)` أي أن عمر المقبس مربوط بـ`ViewModel` الخاص بالـActivity.
- **مؤكد:** `AppViewModel.onCleared()` كان ينادي `connection?.closeByUser(Envelope.Reasons.PHONE_CLOSED)` — إغلاق صريح عند تدمير الـViewModel.
- **مؤكد:** لا يوجد في `AndroidManifest.xml` أي `service` ولا خدمة أمامية؛ فالتطبيق في الخلفية مجرد عملية مخزّنة يمكن للنظام قتلها.
- **مؤكد:** `MainActivity` لا يغلق الاتصال في `onStop`، فلا يوجد إغلاق مباشر في دورة حياة الشاشة.
- **الخلاصة:** الانقطاع عند مغادرة التطبيق سببه ملكية الاتصال للـViewModel (`onCleared` + `viewModelScope`) وغياب مكوّن خلفي يملك الجلسة.
- **غير مؤكد:** توقيت قتل النظام للعملية على جهاز حقيقي (يعتمد على الشركة المصنّعة وضغط الذاكرة) — يحتاج جهازًا.

## 4. البروتوكول (لم يُمسّ)
- `ws://<host>:53017/livetype` · رسائل: `pair_request`, `session_ready`, `text_edit`, `ack`, `reset_stream`, `heartbeat`, `heartbeat_ack`, `disconnect`, `error`.
- المصادقة: PIN مرة واحدة ثم token دائم في `pair_request`. `protocolVersion` اختياري وغيابه = 1. **مؤكد** (`BridgeProtocol.kt` و`README.md`).

## 5. الصلاحيات والخدمات قبل التعديل
`INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `VIBRATE`, `CAMERA`, `RECORD_AUDIO`. لا خدمة، لا إشعارات. **مؤكد**

## 6. قرار نوع الخدمة والإذن (توثيق أندرويد الرسمي، فُحص 2026-10-26)
- **`connectedDevice`** هو النوع المطابق: «Interactions with external devices that require a … network connection»، ولا يوجد له سقف زمني يومي. متطلباته السابقة تتحقق بامتلاك `CHANGE_WIFI_MULTICAST_STATE` المعلن أصلًا للاكتشاف. ← `developer.android.com/develop/background-work/services/fgs/service-types`
- رُفض **`dataSync`**: له سقف 6 ساعات/24 ساعة ثم `onTimeout` وإيقاف إجباري — لا يصلح لاتصال دائم. ← `developer.android.com/about/versions/15/behavior-changes-15`
- **`shortService`** مرفوض (دقائق). **`specialUse`** مرفوض لأن النوع المخصص متاح ودقيق.
- بدء الخدمة من واجهة ظاهرة فقط، ثم `startForeground(..., FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)`. ← `.../fgs/launch` و`.../fgs/restrictions-bg-start`
- النافذة العائمة: `TYPE_APPLICATION_OVERLAY` (المسموح منذ API 26) مع `SYSTEM_ALERT_WINDOW` وفحص `Settings.canDrawOverlays()` في كل استخدام. ← `developer.android.com/reference/android/view/WindowManager.LayoutParams` و`Manifest.permission#SYSTEM_ALERT_WINDOW`
- إشعار دائم + زر قطع الاتصال، وإذن `POST_NOTIFICATIONS` وقت التشغيل على Android 13+. **مؤكد بالمراجع**

## 7. خط البناء والنشر
`.github/workflows/android-build.yml`: يبني على `ubuntu-24.04`، يحسب الإصدار تلقائيًا من الوسوم عند الدفع إلى `main`، يوقّع، وينشر `AirType-v<version>.apk/.aab` + `SHA256SUMS` + `BUILD-INFO.txt` في GitHub Releases. **مؤكد** (تم التحقق من v1.0.5 وv1.0.6 سابقًا).

## 8. ملفات حساسة لا تُمسّ بلا ضرورة
`app/build.gradle.kts` (التوقيع/الإصدار)، `AndroidManifest.xml` (إلا الصلاحيات والخدمة المطلوبة)، `BridgeProtocol.kt` (أسماء الحقول والرسائل)، `StreamSession.kt`/`TextDiff.kt` (منطق النقل)، `core/pairing/*` (الاقتران).

## 9. المخاطر
- **مؤكد:** النوافذ العائمة قد تُغطى بقائمة الإشعارات أو لوحة المفاتيح؛ عولج ذلك بحدّ أعلى للموضع (55% من الشاشة) وبإخفاء الكبسولة عند إغلاقها.
- **مؤكد:** بعض الشركات تقتل الخدمات الأمامية بقوة؛ لذلك الحالة المعروضة تُقرأ دائمًا من `AirTypeSession.snapshot` وليست مخزّنة في الواجهة.
- **مرجّح:** إدخال النص في نافذة عائمة يتطلب نافذة قابلة للتركيز؛ ضبطها خطأً يمنع لوحة المفاتيح، لذا الكبسولة وحدها (لا الفقاعة) هي القابلة للتركيز.
- **غير مؤكد:** سلوك لوحة المفاتيح على شاشات صغيرة/قابلة للطي — يحتاج جهازًا.

## 10. خلاصة القرار
نقل ملكية الاتصال إلى `AirTypeSession` (عملية واحدة، مقبس واحد، `StreamSession` واحدة) + خدمة أمامية `connectedDevice` تملك الإشعار والنوافذ العائمة، وواجهة تقرأ الحالة فقط. الكبسولة تستخدم `AirTypeSession.onTextChanged` نفسها، فلا يوجد مسار نص ثانٍ.
