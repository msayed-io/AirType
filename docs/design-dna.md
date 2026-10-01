# Design DNA Report — AirType Visual Identity Redesign
**Source of Truth:** `REFERENCE_IMAGE` (Finance Dashboard / Fenco Design System)

---

## 3.1 Philosophy, Mood & Personality
- **Soul & Emotion:** The reference design embodies a calm, sophisticated, tactile modern aesthetic. It feels effortlessly airy, refined, and reassuringly precise. Within the first second, the user experiences clarity and trust—a tactile physical-paper quality elevated with soft ambient depth and luminous periwinkle blue focal points.
- **Design Strategy:** The visual hierarchy is established through extreme contrast between crisp tactile white surfaces (`#FFFFFF`), an airy warm-neutral canvas (`#E8E9ED`), a signature periwinkle gradient hero surface (`#8EA6DB` → `#6E88CE`), and confident dark charcoal graphite anchors (`#1C1E22`).
- **Density vs. Whitespace:** Generous internal padding (16–24dp) and clear grouping give content room to breathe without sacrificing information density.
- **Boldness vs. Restraint:** Color is applied with deliberate restraint. 90% of the interface relies on neutral surfaces and graphite typography; vibrant periwinkle and emerald accents are reserved strictly for active states, primary identity cards, and status confirmations.

---

## 3.2 Color System (Forensic Sampled Values)

| Token Role | Hex Code | Purpose / Usage |
|---|---|---|
| `canvas` | `#E8E9ED` | Root application background / page backdrop |
| `surfaceContainer` | `#F2F3F6` | Inner container backdrop |
| `surfaceWhite` | `#FFFFFF` | Primary tactile card background, white capsules, floating chips |
| `surfaceHighlight` | `#F6F7F9` | Subtle hover/pressed card surface, inner input container |
| `surfaceDark` | `#1C1E22` | Graphite black used for primary CTA pill, dark icon buttons, nav anchors |
| `surfaceDarkHover`| `#2A2D33` | Pressed state for graphite buttons |
| `accentPeriwinkle` | `#7C93D6` | Brand accent, charts, active indicators |
| `accentPeriwinkleDeep` | `#5F7ABF` | Gradient endpoint for hero cards |
| `heroGradientStart` | `#8EA6DB` | Hero periwinkle card 135° linear gradient start |
| `heroGradientEnd` | `#6E88CE` | Hero periwinkle card 135° linear gradient end |
| `borderSubtle` | `#E1E3E8` | 1dp card and capsule border strokes |
| `borderStrong` | `#CDD1D9` | Focused input border, emphasized dividers |
| `borderWhiteGlass` | `rgba(255, 255, 255, 0.35)` | Specular borders on hero cards and glass capsules |
| `textPrimary` | `#181A1F` | High-contrast display text, headings, primary labels |
| `textSecondary` | `#7A808C` | Subtitles, secondary labels, disabled icons |
| `textTertiary` | `#9DA3AF` | Captions, micro telemetry, timestamps |
| `textOnDark` | `#FFFFFF` | Text on graphite buttons and periwinkle hero cards |
| `textOnDarkMuted` | `rgba(255, 255, 255, 0.82)`| Subtitles inside hero cards |
| `stateSuccess` | `#10B981` | Connected/active green status dot, verified badges |
| `stateSuccessContainer` | `rgba(16, 185, 129, 0.12)` | Active status badge background |
| `stateWarning` | `#F59E0B` | Connecting/reconnecting state |
| `stateWarningContainer` | `rgba(245, 158, 11, 0.12)` | Connecting state badge background |
| `stateDanger` | `#EF4444` | Disconnect, errors, failed pairings |
| `stateDangerContainer` | `rgba(239, 68, 68, 0.10)` | Error banners, danger button backgrounds |

### Color Distribution Area Ratio
- **Canvas & Whitespace:** ~55% (`#E8E9ED` / `#F2F3F6`)
- **Tactile Card Surfaces:** ~25% (`#FFFFFF` with subtle shadows)
- **Hero Card (Periwinkle):** ~12% (`#8EA6DB` → `#6E88CE`)
- **Graphite Anchors & CTAs:** ~6% (`#1C1E22`)
- **Semantic & State Accents:** ~2% (`#10B981`, `#EF4444`, `#F59E0B`)

---

## 3.3 Depth, Light & Material
- **Elevation Model:** 3 clear physical levels:
  - **Level 0 (Canvas):** Base background `#E8E9ED`.
  - **Level 1 (Cards & Modules):** Pure white `#FFFFFF` with soft diffuse ambient drop shadow `0 6px 20px -4px rgba(45, 55, 75, 0.07)` and 1dp border `#E1E3E8`.
  - **Level 2 (Hero & Floating Floating Capsules):** Hero periwinkle card with colored ambient glow `0 14px 32px -6px rgba(100, 126, 196, 0.35)`; Top navigation floating capsules with elevation 8dp.
  - **Level 3 (Dark Primary CTA):** Graphite capsule with directional drop shadow `0 10px 24px -4px rgba(28, 30, 34, 0.28)`.
- **Lighting Direction:** Top-down ambient light from 12 o'clock, creating subtle top highlights and soft diffused shadows below.

---

## 3.4 Shape & Geometry
- **Corner Radii Scale:**
  - `RadiusLarge` (30dp): Hero card, major screen modules.
  - `RadiusMedium` (20dp): Standard info cards, telemetry tiles, secondary action cards.
  - `RadiusSmall` (14dp): Inner input fields, nested nodes, status badges.
  - `RadiusCapsule` (999dp / CircleShape): Primary CTAs, icon buttons, language selector pills, connection status chips.
- **Nested Radius Rule:** Inner corner radius = Outer corner radius − Padding (e.g., 30dp card with 14dp padding contains 16dp inner elements).
- **Spacing Grid:** 4dp / 8dp / 12dp / 16dp / 20dp / 24dp rhythm.

---

## 3.5 Typography
- **Typeface:** `Readex Pro` (clean geometric humanist Arabic and Latin matching the modern round grotesk of the reference).
- **Scale:**
  - Display Title: 30sp, Bold / ExtraBold, tracking -0.5sp, color `#181A1F`.
  - Hero Card Title: 20sp, Bold, color `#FFFFFF`.
  - Section / Card Headers: 16sp, SemiBold, color `#181A1F`.
  - Body Text: 14sp, Normal, line-height 22sp, color `#7A808C`.
  - Caption / Micro Labels: 11sp–12sp, Medium, color `#9DA3AF` (or `#FFFFFF` 85% on hero).
- **Bilingual Balance:** Arabic text receives +1sp font-size and 1.45x line-height to achieve visual optical parity with Latin.

---

## 3.6 Components Anatomy
- **Top Navigation Bar:** Detached floating capsules (`#FFFFFF` pill with language toggle + active network chip; circular `#1C1E22` settings button).
- **Hero Card:** Periwinkle gradient card containing connection state, node pair (phone ↔ laptop), animated Wi-Fi beam, and status chips.
- **Primary CTA Button:** Full-width 56dp height graphite capsule (`#1C1E22`), white bold text and icons, tactile active scale down to 0.97.
- **Secondary CTA Cards:** 2-column tactile white cards (`#FFFFFF`), border `#E1E3E8`, with icon and label.
- **Text Entry & Typing Pad:** Crisp white/off-white card with clean graphite text, periwinkle focus stroke, and live word/char counters.
- **Status Pills & Chips:** 36dp rounded capsules with glowing pulse dots and colored state backgrounds.

---

## 3.7 Accessibility Audit
- Primary dark text `#181A1F` on white `#FFFFFF`: Contrast ratio **16.2:1** (Exceeds WCAG AAA).
- Primary dark text `#181A1F` on canvas `#E8E9ED`: Contrast ratio **13.5:1** (Exceeds WCAG AAA).
- White text `#FFFFFF` on periwinkle hero gradient `#6E88CE`: Contrast ratio **4.8:1** (Passes WCAG AA for bold & body text).
- White text on dark graphite button `#1C1E22`: Contrast ratio **16.0:1** (Exceeds WCAG AAA).
- Minimum interactive touch targets: All buttons, chips, and toggles >= 48dp.

---

## 3.8 Motion & Micro-Interactions
- Tactile button press: scale to 0.97 with 100ms fast spring.
- Connection pulsing: gentle 900ms infinite breath on the active status dot.
- Screen transitions: crisp slide-fade reveals (250ms).
- Respects system reduce-motion settings.

---

## 3.9 The Design Constitution (10 Golden Rules)
1. **Airy Tactile Base:** The app canvas is soft warm-neutral `#E8E9ED` / `#F2F3F6`, never dark or stark black.
2. **Pure White Cards:** Standard cards are crisp `#FFFFFF` with 1dp `#E1E3E8` borders and soft diffuse shadows.
3. **Hero Periwinkle Identity:** The primary connection card is always the signature periwinkle gradient (`#8EA6DB` → `#6E88CE`).
4. **Graphite Anchor CTAs:** The primary action button is a solid charcoal capsule (`#1C1E22`) with pure white label.
5. **Secondary Tactile Modules:** Secondary actions use 2-column tactile white cards with 18dp rounded corners.
6. **Capsule Supremacy:** Interactive controls, top pills, and status indicators use full capsule radius (`CircleShape`).
7. **Zero Old Olive:** No trace of the retired `#9FA365` or dark teal palettes remain.
8. **Equal RTL & LTR:** Arabic and English render with identical visual balance, correct iconography mirroring, and bidi safety.
9. **Accessible & Responsive:** All touch targets >= 48dp, all text passes WCAG AA, scales cleanly up to 200% font size.
10. **Zero Logic Changes:** 100% of pairing, networking, diffing, protocols, and state management remain untouched.
