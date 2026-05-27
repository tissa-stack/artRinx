# Android AI UI Generation Master Rules

## Purpose
This document defines mandatory UI/UX, responsiveness, accessibility, theme, architecture, and consistency rules for ALL AI-generated Android screens in this application.

These rules MUST be followed for every screen, component, modal, dialog, bottom sheet, navigation flow, loading state, and reusable UI element.

---

# 1. GLOBAL UI PRINCIPLES

## Mandatory
Every screen MUST:
- Be fully responsive
- Work across all Android screen sizes
- Adapt to tablets and foldables
- Support portrait and landscape
- Respect accessibility font scaling
- Respect safe areas and system bars
- Use reusable design systems
- Use adaptive layouts instead of fixed dimensions

## Forbidden
NEVER:
- Hardcode dimensions unnecessarily
- Hardcode colors directly inside UI
- Use random spacing values
- Use inconsistent typography
- Allow clipping or overlapping
- Ignore keyboard visibility
- Create layouts optimized for only one device

---

# 2. THEME SYSTEM RULES

## Themes
The app MUST support:
- Light Theme
- Dark Theme
- System Theme switching

## Mandatory Theme Usage
ALL UI colors MUST come from:
- `MaterialTheme.colorScheme`
- App theme tokens
- Semantic design tokens

Example:
```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.background
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onSurface
```

## Forbidden
NEVER:
- Use hardcoded hex colors inside composables
- Use pure black backgrounds
- Use pure white backgrounds
- Use low-contrast text

---

# 3. TYPOGRAPHY RULES

## Mandatory
ALL text MUST use:
```kotlin
MaterialTheme.typography
```

## Typography Consistency
Use ONLY app-defined:
- Font families
- Font weights
- Typography hierarchy

## Accessibility Rules
Typography MUST:
- Scale properly
- Respect system font scaling
- Never clip
- Never overlap
- Never become unreadable

## Required Text Handling
Use:
```kotlin
overflow = TextOverflow.Ellipsis
maxLines
lineHeight
```

---

# 4. RESPONSIVE LAYOUT RULES

## Mandatory
UI MUST adapt to:
- Small phones
- Medium phones
- Large phones
- Foldables
- Tablets
- Multi-window mode
- Landscape mode

## Always Use
```kotlin
Modifier.fillMaxWidth()
Modifier.wrapContentHeight()
Modifier.weight()
BoxWithConstraints
WindowSizeClass
```

## Forbidden
NEVER:
- Use fixed widths for main layouts
- Assume screen size
- Use absolute positioning unnecessarily
- Let content overflow outside screen

---

# 5. SPACING SYSTEM RULES

## Approved Spacing Scale
Use ONLY:
- 4dp
- 8dp
- 12dp
- 16dp
- 20dp
- 24dp
- 32dp
- 40dp
- 48dp

## Mandatory
Spacing MUST remain:
- Consistent
- Predictable
- Symmetrical where appropriate

---

# 6. SAFE AREA & SYSTEM BAR RULES

## Mandatory
ALL screens MUST respect:
- Status bar
- Navigation bar
- Gesture navigation
- Camera cutouts
- Notches
- Waterfall displays

## Always Use
```kotlin
statusBarsPadding()
navigationBarsPadding()
safeDrawingPadding()
WindowInsets
```

## Keyboard Rules
Keyboard MUST NEVER:
- Cover input fields
- Hide important actions
- Break scrolling behavior

Use:
```kotlin
imePadding()
verticalScroll()
```

---

# 7. SCROLLING RULES

## Mandatory
If content may overflow:
- Make it scrollable

## Preferred Components
Use:
```kotlin
LazyColumn
LazyRow
LazyVerticalGrid
verticalScroll()
```

## Forbidden
NEVER:
- Allow hidden inaccessible content
- Clip important actions
- Nest excessive scroll containers

---

# 8. COMPONENT DESIGN RULES

## Buttons
Buttons MUST:
- Have consistent height
- Support loading states
- Support disabled states
- Support pressed states
- Respect touch target sizes

Minimum touch target:
```text
48dp x 48dp
```

## Cards
Cards MUST:
- Use consistent corner radius
- Use consistent elevation
- Maintain internal padding
- Respect theme surfaces

## Inputs
Inputs MUST:
- Handle focus properly
- Support validation
- Support error states
- Support accessibility
- Support proper keyboard types

---

# 9. IMAGE & ARTWORK RULES

## Artwork Display
Artwork MUST:
- Preserve aspect ratio
- Avoid unnecessary cropping
- Maintain image quality
- Support zoom when needed

## Image Scaling
Use:
```kotlin
ContentScale.Crop
ContentScale.Fit
```

## Loading States
Images MUST support:
- Placeholder states
- Loading shimmer/skeleton
- Error states
- Retry handling

---

# 10. ACCESSIBILITY RULES

## Mandatory
ALL interactive elements MUST:
- Have content descriptions
- Be screen-reader friendly
- Have proper touch targets

## Use
```kotlin
contentDescription
```

## Accessibility Support
The app MUST support:
- Large fonts
- Screen readers
- Keyboard navigation where applicable
- Proper color contrast

---

# 11. PERFORMANCE RULES

## Avoid
- Deep nested composables
- Unnecessary recompositions
- Heavy UI calculations on main thread
- Rendering unnecessary UI

## Prefer
```kotlin
remember()
derivedStateOf()
LazyColumn
stable state holders
```

---

# 12. ANIMATION RULES

## Animations MUST
- Be smooth
- Be subtle
- Improve UX
- Not block interactions

## Forbidden
NEVER:
- Use excessive motion
- Use laggy animations
- Use distracting transitions

---

# 13. STATE MANAGEMENT RULES

## UI MUST handle
- Rotation
- Process recreation
- Theme switching
- Screen resizing
- Multi-window mode

## State Rules
Separate:
- UI state
- Business logic
- Navigation state
- API state

---

# 14. LOADING / EMPTY / ERROR STATES

Every screen MUST implement:
- Loading state
- Empty state
- Error state
- Offline state
- Retry state

## Forbidden
NEVER:
- Leave blank screens
- Freeze UI during loading
- Hide errors silently

---

# 15. NAVIGATION RULES

## Mandatory
Navigation MUST:
- Handle back press correctly
- Preserve screen state
- Prevent duplicate navigation
- Handle deep links safely

---

# 16. DESIGN SYSTEM RULES

## Mandatory
ALL screens MUST use:
- Shared theme system
- Shared typography system
- Shared spacing system
- Shared component system
- Shared iconography system

## Consistency Rules
Maintain:
- Same corner radii
- Same shadows/elevation
- Same button styles
- Same animations
- Same spacing logic

---

# 17. GRID & GALLERY RULES

## Art Gallery Screens
Must:
- Use adaptive grids
- Dynamically calculate columns
- Maintain artwork ratios
- Load efficiently

## Responsive Grid Rules
Columns MUST adapt based on:
- Screen width
- Device orientation
- Tablet mode

---

# 18. MARKETPLACE RULES

## Purchase Screens
Must:
- Clearly show price
- Clearly show CTA buttons
- Maintain accessibility
- Handle loading safely

## Cart & Checkout
Must:
- Prevent accidental actions
- Clearly show totals
- Maintain responsiveness

---

# 19. FORM RULES

## Forms MUST
- Scroll properly
- Handle keyboard safely
- Show validation clearly
- Preserve entered state

## Validation Rules
Errors MUST:
- Be readable
- Be accessible
- Be visually clear

---

# 20. API & NETWORK STATE UI RULES

## Mandatory
UI MUST handle:
- Slow internet
- No internet
- API failures
- Timeout states
- Retry states

## Forbidden
NEVER:
- Freeze during API calls
- Leave user without feedback

---

# 21. CODE QUALITY RULES

## Mandatory
Code MUST:
- Be modular
- Be reusable
- Be scalable
- Be maintainable
- Follow clean architecture principles

## Required
- Reusable composables
- Proper naming
- Preview composables
- Separation of concerns

---

# 22. TESTING CHECKLIST

Before finalizing ANY screen verify:

## Theme Testing
- Light mode works
- Dark mode works
- Dynamic theme switching works

## Responsiveness Testing
- Small phones work
- Large phones work
- Tablets work
- Foldables work
- Landscape works
- Split screen works

## Accessibility Testing
- Large fonts work
- Screen readers work
- Buttons remain clickable
- Text remains readable

## Safe Area Testing
- Status bar overlap does not happen
- Navigation bar overlap does not happen
- Keyboard overlap does not happen

## UI Integrity Testing
- No clipping
- No overlapping
- No hidden content
- No broken scrolling

---

# 23. MODERN ANDROID STANDARDS

## Preferred Stack
- Jetpack Compose
- Material 3
- Kotlin
- Edge-to-edge UI
- Adaptive layouts

## Follow
- Material Design guidelines
- Android accessibility standards
- Modern Android architecture practices

---

# 24. AI GENERATION RULES

Whenever generating ANY new UI screen:

ALWAYS:
- Reuse theme system
- Reuse typography system
- Reuse spacing system
- Support dark mode
- Support accessibility
- Respect safe areas
- Use adaptive layouts
- Use reusable components
- Maintain visual consistency

NEVER:
- Generate one-off inconsistent UI
- Ignore responsiveness
- Ignore accessibility
- Ignore keyboard handling
- Ignore edge-to-edge handling

---

# 25. GOLDEN RULE

If a UI works properly only on ONE device,
then the UI is WRONG.

A screen is considered COMPLETE only when:
- It works on all screen sizes
- It supports all themes
- It supports accessibility
- It respects safe areas
- It maintains consistency
- It handles all states properly
- It feels polished and production-ready