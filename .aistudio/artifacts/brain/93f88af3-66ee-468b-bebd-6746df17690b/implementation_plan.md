# Bounding Box Visibility Toggle on Lens Screen

Add a toggle button in the top control bar of the Lens screen allowing users to quickly show or hide all bounding boxes, laser scan lines, and viewfinder highlights for an unobstructed view of the camera feed.

## User Review & Critical Decisions

> [!IMPORTANT]
> - **Placement**: Top control bar alongside the flash toggle and camera switch buttons in Portrait mode, and in the top-status row / side rail in Landscape mode.
> - **Visual Style**: Circular frosted glass button with Eye icon toggle (`Icons.Default.Visibility` / `Icons.Default.VisibilityOff`) and subtle state glow (cyan when active/visible, semi-transparent white with slash when hidden).
> - **Scope**: Hides/shows the viewfinder scan border, laser scanner, and text highlight bounding boxes in live camera mode, and extends to the interactive text selection view so users can also view captured photos cleanly without box overlays.

---

## 1. Overview & Core Concept

- **Problem**: When pointing the camera at documents, books, or signs with dense Japanese characters, viewfinder brackets, laser scanning animations, and text highlights can occasionally obscure surrounding details or visual context.
- **Solution**: A quick 1-tap Eye icon toggle in the top control bar that instantly switches bounding box/overlay visibility ON or OFF without interrupting live OCR scanning or text detection in the background.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Live Camera (Portrait & Landscape)**:
   - Users see a new circular icon button in the top bar: `👁️` (Visible, active cyan/white) or `👁️‍🗨️` / `VisibilityOff` (Hidden).
   - Tapping it toggles `areBoundingBoxesVisible` state with a subtle haptic feedback and brief toast confirmation ("ဘောင်များ ဖျောက်ထားပါသည် (Bounding boxes hidden)" / "ဘောင်များ ပြန်ဖွင့်ပါသည် (Bounding boxes visible)").
   - Even when boxes are hidden, live text detection continues smoothly in the background, keeping the `[ 📋 Copy text ]` pill ready if Japanese text is recognized.

2. **Captured & Interactive Select Text Screen**:
   - In `InteractiveSelectTextView`, the top action bar also features the Eye toggle button.
   - When toggled off, word highlight rectangles and corner handles fade out cleanly so users can examine the raw, untouched photo clearly before toggling them back on to tap and select words.

---

## 3. Technical Architecture & Data Strategy

### State Flow

```
┌────────────────────────────────────────────────────────┐
│                      LensScreen                        │
│         var areBoxesVisible by remember { true }       │
└───────────────────────────┬────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
   ┌──────────────────────┐    ┌──────────────────────┐
   │    LiveCameraView    │    │InteractiveSelectText │
   │                      │    │                      │
   │ - Top bar Eye toggle │    │ - Top bar Eye toggle │
   │ - ViewfinderOverlay  │    │ - Word bounding box  │
   │   (visible if true)  │    │   rectangles (alpha) │
   └──────────────────────┘    └──────────────────────┘
```

### Key Changes
1. **Live Camera View (`LensScreen.kt`)**:
   - Add state: `var areBoundingBoxesVisible by remember { mutableStateOf(true) }`.
   - Add toggle IconButton in top bar (Portrait) and top row (Landscape) with `Icons.Default.Visibility` / `Icons.Default.VisibilityOff`.
   - Pass visibility state to `ViewfinderOverlay(isTextDetected = hasLiveText, isLandscape = isLandscape, isVisible = areBoundingBoxesVisible)`.
   - In `ViewfinderOverlay`, animate alpha smoothly between `1f` and `0f` when visibility changes.

2. **Interactive Select Text View (`InteractiveSelectTextView.kt`)**:
   - Add `var areBoxesVisible by remember { mutableStateOf(true) }`.
   - Add toggle in the top bar header.
   - Draw word highlight bounding boxes only when `areBoxesVisible` is true (or fade to `0f` alpha), keeping selected text highlight active if a specific word is currently selected.

---

## 4. Verification Plan

1. Verify build with `compile_applet`.
2. Run unit tests with `gradle :app:testDebugUnitTest`.
3. Test Eye toggle button in both Portrait and Landscape orientations.
4. Verify that live OCR and 1-tap copy pill function continuously regardless of box visibility.
