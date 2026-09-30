# Release Installable Android APK

Build, package, and release a standalone installable APK (`.apk`) of LensJisho for direct installation on Android devices.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed:

- **Confirmed Decision 1 (Package Format)**: Universal installable APK (`.apk`) for direct sideloading and testing on Android smartphones.
- **Confirmed Decision 2 (Signing)**: Standard self-signed signing config (`debugConfig` / self-signed certificate) ensuring any Android device can install the APK directly without requiring Play Store distribution credentials.
- **Confirmed Decision 3 (Distribution)**: Deliver build artifact via the standard Gradle output path (`app/build/outputs/apk/`) and provide user instructions for downloading the APK or full project via the AI Studio Settings menu.

---

## 1. Overview & Core Concept

### What It Does
Generates a complete, standalone, installable APK package of the **LensJisho** Japanese OCR & Flashcard Learning application. The APK includes all features:
- Real-time live camera Japanese OCR with instant tap-to-copy
- Jisho.org dictionary integration with furigana readings and Burmese translations
- Text translation engine
- Interactive 3D flip card SRS Flashcard Quiz game

### Key Value
Allows the user to install the compiled application directly on physical Android phones or tablets to test the live camera scanner and flashcard practice in real-world conditions.

---

## 2. Technical Architecture & Build Strategy

### Build Pipeline

```
┌────────────────────────────────────────────────────────┐
│                   Source Code & Assets                 │
│      (Kotlin, Jetpack Compose, CameraX, ML Kit)        │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│              Gradle Build System Assembly              │
│               `gradle :app:assembleDebug`              │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│               Signed Installable Package               │
│        `app/build/outputs/apk/debug/app-debug.apk`     │
│       (Directly installable on any Android device)     │
└────────────────────────────────────────────────────────┘
```

### Safety & Platform Adherence
- Strictly adheres to the requirement not to alter keystore files (`debug.keystore`) or build output directory locations.
- Keeps ProGuard / minification settings compatible to prevent bytecode stripping of ML Kit or Room components.

---

## 3. Implementation Steps (Execution Phase)

1. **Step 1: Execute Gradle Package Task**:
   - Run `gradle :app:assembleDebug` to assemble and sign the installable APK.
2. **Step 2: Validate Artifact Output**:
   - Confirm APK generation at `app/build/outputs/apk/debug/app-debug.apk`.
   - Verify size, architecture manifest, and packaging integrity.
3. **Step 3: Verification & Download Guidance**:
   - Run `compile_applet` to ensure platform state synchronization.
   - Present the user with clear instructions to download the APK or export via the AI Studio Settings panel.
