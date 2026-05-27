# Android Theme & Color System Rules

## Theme System Overview

This application uses:
- Light Theme
- Dark Theme
- Dynamic system theme support
- Material 3 design principles
- Poppins font family throughout the app

---

# 1. PRIMARY BRAND COLOR

## Active / Primary Brand Color
Use the following primary accent color in BOTH themes:

```text
#45B1E8
```

This color is used for:
- Primary buttons
- Active states
- Selected tabs
- Highlights
- Progress indicators
- Links
- Focus states
- Active icons
- Accent UI elements

---

# 2. FONT FAMILY RULES

## Global Font Family
The entire application MUST use:

```text
Poppins
```

## Typography Rules
Use:
- Poppins Regular
- Poppins Medium
- Poppins SemiBold
- Poppins Bold

## Forbidden
NEVER:
- Mix random fonts
- Use system default fonts inconsistently
- Use more than one font family

---

# 3. LIGHT THEME COLORS

| Token | Hex |
|---|---|
| Background | `#FFFFFF` |
| Surface | `#F8F8F8` |
| Card Surface | `#F0F0F0` |
| Primary Text (H1) | `#1D1D1D` |
| Secondary Text (H2) | `#2D2D2D` |
| Inline Text | `#2D2D2D` |
| Field Background | `#F0F0F0` |
| Field Text | `#1D1D1D` |
| Border Color | `#E0E0E0` |
| Active Button | `#45B1E8` |
| Inactive Button | `#656565` |
| Error Color | `#D32F2F` |
| Success Color | `#4CAF50` |
| Warning Color | `#FFA000` |

---

# 4. DARK THEME COLORS

| Token | Hex |
|---|---|
| Background | `#121212` |
| Surface | `#1A1A1A` |
| Card Surface | `#1D1D1D` |
| Primary Text (H1) | `#FFFFFF` |
| Secondary Text (H2) | `#F5F5F5` |
| Inline Text | `#949494` |
| Field Background | `#1D1D1D` |
| Field Text | `#A7A7A7` |
| Border Color | `#2A2A2A` |
| Active Button | `#45B1E8` |
| Inactive Button | `#656565` |
| Error Color | `#EF5350` |
| Success Color | `#66BB6A` |
| Warning Color | `#FFB300` |

---

# 5. TYPOGRAPHY SYSTEM

## Typography Hierarchy

| Style | Usage | Weight |
|---|---|---|
| Display Large | Splash / Hero | Bold |
| H1 | Main headings | SemiBold |
| H2 | Section titles | Medium |
| Body Large | Main content | Regular |
| Body Medium | Secondary content | Regular |
| Label | Buttons / Inputs | Medium |
| Caption | Metadata | Regular |

---

# 6. MATERIAL THEME IMPLEMENTATION

## Mandatory
ALL UI components MUST use:
```kotlin
MaterialTheme.colorScheme
MaterialTheme.typography
```

## Example
```kotlin
Text(
    text = "Explore Art",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground
)
```

---

# 7. BUTTON RULES

## Active Buttons
Use:
```text
#45B1E8
```

## Inactive Buttons
Use:
```text
#656565
```

## Button Requirements
Buttons MUST:
- Have proper contrast
- Support disabled state
- Support loading state
- Maintain consistent radius
- Maintain consistent height

---

# 8. TEXT COLOR RULES

## Heading Text
Use:
- H1 for major titles
- H2 for section headings

## Inline Text
Use muted text colors for:
- Metadata
- Captions
- Secondary descriptions

## Forbidden
NEVER:
- Use pure black text on dark backgrounds
- Use low-contrast gray text
- Use random text colors

---

# 9. SURFACE & CARD RULES

## Cards
Cards MUST:
- Use surface colors from theme
- Maintain subtle elevation
- Maintain consistent corner radius
- Respect dark mode contrast

---

# 10. INPUT FIELD RULES

## Input Fields MUST
- Use themed background colors
- Use proper placeholder colors
- Use accessible text contrast
- Support focus states
- Support error states

## Field Colors

### Light Theme
```text
Background: #F0F0F0
Text: #1D1D1D
```

### Dark Theme
```text
Background: #1D1D1D
Text: #A7A7A7
```

---

# 11. ICON RULES

Icons MUST:
- Use theme colors
- Maintain visual consistency
- Match typography weight visually

## Active Icons
```text
#45B1E8
```

---

# 12. ACCESSIBILITY RULES

## Mandatory
Maintain proper contrast ratios in BOTH themes.

The UI MUST:
- Remain readable in sunlight
- Remain readable in dark environments
- Support accessibility font scaling
- Avoid eye strain

---

# 13. AI GENERATION RULES

Whenever generating ANY new screen:

ALWAYS:
- Use Poppins font family
- Use theme color tokens
- Use #45B1E8 as active/accent color
- Support both themes
- Maintain visual consistency
- Use Material 3 principles

NEVER:
- Hardcode random colors
- Use inconsistent typography
- Ignore dark mode
- Mix font families

---

# 14. GOLDEN RULE

The UI should feel:
- Minimal
- Modern
- Premium
- Consistent
- Readable
- Elegant
- Art-focused

Every screen must visually feel like part of the SAME application.