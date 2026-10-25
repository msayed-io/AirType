# AirType Screen & State Inventory

This inventory catalogs 100% of screens, states, dialogs, and components in AirType to ensure complete visual redesign coverage.

---

## 1. WelcomeScreen (Home / Dashboard)
- **Top Navigation Bar:**
  - Settings button (graphite circular capsule)
  - Language toggle pill ("English" / "العربية")
  - Local direct network status badge ("شبكة محلية مباشرة" with pulsing emerald dot)
- **Hero Brand Header:**
  - Keyboard icon glyph card badge
  - App wordmark "AirType"
  - Bilingual subtitle tagline
- **Connection Hero Card (Periwinkle Gradient):**
  - Card meta header (Connection status + "AIRTYPE LOCAL")
  - Status title ("غير موصول" / "Not connected")
  - Node pair (Phone ↔ Laptop)
  - Animated Wi-Fi bridge indicator with ping dots
  - Security note strip with verified checkmark
- **Action Controls:**
  - Primary CTA: "ربط هاتف (مسح الكود)" / "Pair laptop (Scan QR)" (Full width graphite capsule)
  - Saved pairing button: "إعادة الاتصال بالجهاز المحفوظ" / "Reconnect to saved device" (when available)
  - Secondary CTA grid (2 columns):
    - "إدخال يدوي" / "Manual entry"
    - "التشخيص" / "Diagnostics"
- **Security Footer Strip:**
  - Local direct-connection badge (no encryption claim: the transport is plain local WebSocket)

---

## 2. QrScannerScreen (Camera Pairing Flow)
- **Top Navigation Bar:**
  - Back button (circular capsule)
  - Title pill ("امسح رمز الـ QR" / "Scan the QR code")
- **States:**
  - **Permission Granted State:**
    - Camera preview viewfinder with rounded corners (26dp) and border
    - Scanning target square (240dp) with moving laser beam sweep animation
  - **Permission Request State:**
    - Tactile card with camera icon, title, explanatory text, and "منح الإذن" / "Grant permission" primary button
  - **Error State:**
    - Liquid error banner for bad QR / expired session / network errors
  - **Help / Instructions Card:**
    - Info icon + instructions on opening PC app
  - **Fallback CTA:**
    - "إدخال يدوي" / "Manual entry" button

---

## 3. ManualEntryScreen (Manual PIN / IP Connection)
- **Top Navigation Bar:**
  - Back button + Title pill ("إدخال يدوي" / "Manual entry")
- **Input Card (Tactile White Card):**
  - IP Address field ("عنوان الـ IP" / "Host") with Router icon
  - Port field ("المنفذ" / "Port") with Numbers icon
  - Session ID field ("معرّف الجلسة" / "Session ID") with Key icon
  - PIN field ("رمز PIN (6 أرقام)" / "PIN") with Dialpad icon
- **Error Banner:**
  - Displays validation and connection error messages
- **Primary CTA:**
  - "اتصال" / "Connect" button (disabled when required fields are blank, active graphite when valid)

---

## 4. ConnectedScreen (Active Remote Typing Session)
- **Top Navigation Bar:**
  - Live Connection Status Pill (Connected / Paused / Reconnecting / Error)
  - Language toggle pill
  - Diagnostics shortcut button
- **Session Subtitle / Status Explanation Banner**
- **Error Banner** (if any transient error occurs)
- **Target Laptop Info Card:**
  - Laptop icon + IP address + live status dot
- **Dictation & Live Typing Pad Card:**
  - Large multiline text input (200dp height) with clean typography and custom cursor
  - Live statistics row: Character count, word count, Revision badge ("Rev: X")
- **Stream Controls:**
  - Stream Toggle Button ("إيقاف البث" / "Pause stream" vs "استئناف البث" / "Resume stream")
- **Secondary Actions:**
  - "مسح النص" / "Clear text" button
  - "قطع الاتصال" / "Disconnect" button (Danger red styling)
- **Telemetry & Metrics Dashboard Card:**
  - Measured latency ("التأخير المقاس" with dynamic color coding: green <30ms, yellow <100ms, red >100ms)
  - Ops sent ("العمليات المُرسلة")
  - Last revision ("آخر revision")
  - Session expiration time ("تنتهي الجلسة")

---

## 5. DiagnosticsScreen & Settings
- **Top Navigation Bar:**
  - Back button + Title pill ("التشخيص" / "Diagnostics")
- **Session & Network Telemetry Card:**
  - Detailed connection state, laptop IP, latency, ops, revision, last error
- **Settings Card:**
  - Language Selector Tabs (Segmented pill: العربية | English)
  - "اهتزاز خفيف عند نجاح الاقتران" / "Light vibration on pair" toggle switch
  - "تذكّر الاقتران" / "Remember pairing" toggle switch
  - "نسيان الجهاز المحفوظ" / "Forget saved device" button
- **Error Banner** (if applicable)

---

## 6. Feedback & Micro-Interactions
- Tactile haptic feedback on button presses and successful QR pairing
- Pulsing animated dots for active network and streaming
- Smooth color and alpha transitions across states
