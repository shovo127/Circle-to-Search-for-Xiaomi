# User Interface Specification (UI_Spec)

## 1. Visual Design Principles
- **Design System**: Material Design 3 (Material You) with dynamic contrast.
- **Color Palette**:
  - Primary Glow Accent: `#00E5FF` (Electric Cyan)
  - Selection Fill: `#2600E5FF` (15% Translucent Cyan)
  - Bounding Box Dash: `#FFFFFF` (White dashed 2dp stroke)
  - Dark Surface Dimming: `#66000000` (40% Black Scrim)
- **Typography**: Roboto / MiSans System font, legible weights.

## 2. Screen Specifications

### A. Overlay Search View (`CircleSearchActivity`)
- **Root Layout**: Full-screen translucent frame (`Theme.CircleSearchTranslucent`).
- **Edge-to-Edge**: `WindowCompat.setDecorFitsSystemWindows(false)` with cutoutShortEdges mode to cover notch/camera hole seamlessly.
- **Background View**: `ImageView` displaying the exact captured screenshot bitmap at 1:1 hardware pixel ratio.
- **Interaction Layer**: `CircleCanvasView` capturing continuous freeform touch points with antialiasing and blur-mask filters.
- **Top HUD**: Centered translucent chip pill with hint text ("Circle or tap anywhere to search") and close button.

### B. Settings & Setup Dashboard (`MainActivity`)
- **Header**: Xiaomi 13 Device Card showing Model, Codename (`fuxi`), HyperOS Build, and Service Status badge.
- **Service Toggle Card**: Direct intent to Android Accessibility Settings with clear visual state indicator (Green = Active, Red = Service Inactive).
- **Activation Mode Selector**: Radio group with:
  1. Bottom-Center Hold (Configurable slider: 300ms - 700ms, default 450ms).
  2. Back Tap Shortcut (Status indicator and HyperOS setup guide).
  3. Power Button Hold (Status indicator and Assistant setup guide).
- **Search Provider Selector**: Preferred search engine selection (Google Lens, Bing Visual Search, Yandex Images).
- **Privacy & Storage Management**: Clear cache button and 5-minute auto-purge toggle.

