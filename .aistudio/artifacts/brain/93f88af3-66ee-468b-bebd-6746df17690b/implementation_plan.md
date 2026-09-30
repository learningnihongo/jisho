# Live OCR Text Scanner & Flashcard Quiz Game

Enable seamless real-time Japanese text scanning with instant tap-to-copy without taking photos in the Lens section, and introduce an interactive 3D flip Flashcard Quiz Game with Spaced Repetition (SRS) self-rating in the Save section.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during the clarification phase:

- **Confirmed Decision 1 (Lens Scanning)**: Real-time detected text boxes with tap-to-copy directly on the camera preview without taking a photo.
- **Confirmed Decision 2 (Flashcard Quiz Format)**: Interactive flip card review game with SRS self-rating (Again, Hard, Good, Easy) and session queue management.
- **Confirmed Decision 3 (Quiz Prompt Direction)**: Japanese front (Kanji / Kana) testing recall, flipping to reveal reading (Furigana), Burmese meaning (🇲🇲), and English definition.
- **Enhanced Live Capabilities**: Added a "Freeze / Pause" toggle to lock the live frame and detected text for easy selection when moving, plus a 1-tap "Copy All Text" floating bar and quick Jisho search.

---

## 1. Overview & Core Concept

### What It Does
1. **Real-time Live OCR Lens**: Users point the camera at any Japanese book, label, sign, or screen. Text is detected in real-time on live camera frames via ML Kit. The app highlights recognized text with interactive overlay boxes. Users can tap any individual word or block to instantly copy it to the clipboard or tap "Copy All Text" without taking a snapshot.
2. **Flashcard Quiz Game (Save Section)**: Transforms saved vocabulary into a dedicated, gamified Flashcard review experience. Users flip cards with a smooth 3D rotation, listen to Japanese audio pronunciation, test their memory, rate their recall (Again, Hard, Good, Easy), and receive end-of-game celebration stats and streak tracking.

### Target Audience & Persona
Japanese language learners (JLPT N5–N1) and everyday readers in Myanmar and internationally who want immediate, frictionless text copying from the physical world and a high-yield memory retention game for their saved vocabulary.

### Key Value
- **Zero-shutter friction**: No saving photos to gallery or taking snapshots just to copy a sentence or kanji.
- **Immediate retention loop**: Words saved from camera lens or dictionary can be immediately reviewed and mastered through a fun, animated flashcard game.

---

## 2. User Experience & Visual Design

### Key User Flows

#### Flow A: Live Camera Text Scanning & Instant Copy (Lens Section)
1. **Camera Feed**: User enters the Lens section. Camera opens with real-time text recognition active.
2. **Real-time Highlights**: Bounding chips/boxes light up around detected Japanese text blocks in real time.
3. **Instant Copy Actions**:
   - **Tap any word/box**: Copies that word immediately with a haptic ripple and confirmation toast: *"Copied: [text]"*.
   - **Floating Live Control Bar**: Shows the detected live text stream, a prominent `[ 📋 Copy All Text ]` button, a `[ ❄️ Freeze Frame ]` toggle, and a `[ 🔍 Jisho ]` button.
   - **Freeze Mode**: Tapping "Freeze" holds the current frame and detected boxes so users can tap and copy comfortably without camera shake.
4. **Photo Mode Option**: Shutter capture and gallery upload remain available for users who still want in-depth full-image translation.

#### Flow B: Flashcard Quiz Game (Save Section)
1. **Launch Quiz**: From the Saved Words screen, user taps the prominent "Play Flashcard Quiz" (Flashcard ကစားမည်) banner or selects the "SRS Quiz" tab.
2. **Game Setup & Filters**: User can filter by JLPT level (All, N5–N1) or priority (Struggling Words First, Due for Review, Random).
3. **Interactive 3D Flip Flashcard**:
   - **Front**: Large Japanese word / Kanji, JLPT badge, speaker icon for native TTS audio, and clear visual cue *"Tap card to flip"*.
   - **Flip Interaction**: Tapping the card triggers a smooth 180° Y-axis card flip animation.
   - **Back**: Furigana reading `【よみ】`, Burmese translation `🇲🇲`, English definitions, and previous memory recall stats.
4. **SRS Rating Bar**:
   - 🔴 **Again (ခက်သည်)**: Re-queues the card 2 spots later in the current session so the user must get it right before finishing.
   - 🟠 **Hard (အတော်ခက်)**: Schedules review for 1 day.
   - 🟢 **Good (မှတ်မိ)**: Schedules review for 3 days.
   - 🔵 **Easy (လွယ်ကူ)**: Schedules review for 5+ days.
5. **Session Completion**: Displays animated trophy screen with score summary, mastery stats, and a "Play Again" button.

### Visual Identity & Theme
- **Theme**: Modern Japanese Indigo & Scarlet minimalist aesthetic (`#1A237E` primary, `#D32F2F` crimson accent, `#2E7D32` success green).
- **Cards**: Layered Material 3 elevated surfaces with rounded 20.dp corners, soft borders, and smooth shadows.
- **Laser Scan Effect**: Neon cyan scanner beam (`#00E5FF`) on live viewfinder indicating real-time analysis.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: CameraX ImageAnalysis vs. Static Snapshots**
  - *Chosen Approach*: Bind `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` alongside `Preview`. Throttle recognition to ~350–500ms intervals so ML Kit processes frames smoothly without UI lag or battery drain.
  - *Why*: Eliminates shutter latency and unwanted device storage clutter.
- **Decision 2: Live Tap-to-Copy Overlay & Floating Pill**
  - *Chosen Approach*: Provide both on-screen clickable text chips over the viewfinder and a bottom quick-action bar with "Copy All".
  - *Why*: Ensures usability whether scanning a single Kanji character or a multi-line paragraph.
- **Decision 3: In-Session Dynamic Queue for Flashcard Quiz**
  - *Chosen Approach*: When a user rates a word "Again", it is re-inserted into the active session queue instead of just updating the database date.
  - *Why*: True active recall practice ensures the user doesn't leave the quiz until every struggling word is successfully recalled.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Lens Screen                               │
│                                                                        │
│   ┌────────────────────────┐         ┌──────────────────────────────┐  │
│   │   CameraX Live Feed    │         │  Live OCR Analyzer (ML Kit)  │  │
│   │   (PreviewView)        │ ──────> │  ImageAnalysis (Japanese)    │  │
│   └───────────┬────────────┘         └──────────────┬───────────────┘  │
│               │                                     │                  │
│               ▼                                     ▼                  │
│   ┌──────────────────────────────────────────────────────────────┐     │
│   │        Live Detected Text Overlay & Tap-to-Copy Pill         │     │
│   │   [ Copy Single Word ]  [ Copy All Text ]  [ Freeze Frame ]   │     │
│   └──────────────────────────────────────────────────────────────┘     │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                              Save Screen                               │
│                                                                        │
│   ┌────────────────────────┐         ┌──────────────────────────────┐  │
│   │    Saved Words Tab     │         │    Flashcard Quiz Game       │  │
│   │  (Room WordDao Flow)   │         │    (Interactive 3D Flip)     │  │
│   └───────────┬────────────┘         └──────────────┬───────────────┘  │
│               │                                     │                  │
│               ▼                                     ▼                  │
│   ┌──────────────────────────────────────────────────────────────┐     │
│   │  SRS Session Queue & Review Engine (SrsAlgorithm)            │     │
│   │  [ Again (<1m) ]   [ Hard (1d) ]   [ Good (3d) ]   [ Easy ]  │     │
│   └──────────────────────────────────────────────────────────────┘     │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State Strategy

- **`LiveScanState`** (in `LensScreen` / ViewModel):
  - `isLiveScanEnabled: Boolean`
  - `isLiveScanFrozen: Boolean`
  - `detectedText: String`
  - `detectedWords: List<DetectedWord>`
  - `isAnalyzing: Boolean`
- **`FlashcardQuizState`** (in `SavedScreen` / ViewModel):
  - `activeQueue: List<SavedWord>`
  - `currentIndex: Int`
  - `isCardFlipped: Boolean`
  - `sessionReviewedCount: Int`
  - `againCount: Int`
  - `isCompleted: Boolean`
- **Room Persistence**: Updates `SavedWord` SRS interval, ease factor, repetition count, and next review timestamp via `SrsAlgorithm.calculateNextReview()`.

---

## 5. Implementation Steps (Execution Phase)

1. **Step 1: Enhance `JapaneseOcrManager`**:
   - Add `recognizeImageProxy(imageProxy: ImageProxy)` with proper rotation handling and resource cleanup.
2. **Step 2: Upgrade `LensScreen` Live Camera View**:
   - Integrate CameraX `ImageAnalysis` analyzer with frame throttle.
   - Add real-time text detection state, live clickable word pills, "Freeze Frame" toggle, and floating "Copy All" bar with clipboard feedback.
3. **Step 3: Elevate Flashcard Quiz in `SavedScreen`**:
   - Build a dedicated, full-featured interactive 3D Flip Flashcard Quiz Game with card rotation animation, progress bar, audio pronunciation, and 4-tier SRS rating buttons.
   - Implement dynamic re-queuing for "Again" cards and an engaging completion celebration screen.
4. **Step 4: Build Verification & Polish**:
   - Run `compile_applet` to verify compilation and test full functionality.
