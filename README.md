# Simple Phone

An accessible Android phone app designed for elderly users with vision and motor control challenges.

## Features

### Accessibility
* **Large touch targets** - All buttons are 48dp+ for easy tapping
* **High contrast colors** - Deep blue and bright green for visibility
* **Large text** - Option for huge text (same size as contact photos)
* **Haptic feedback** - Vibration on button presses (configurable)
* **Voice announcements** - Speaks contact names when calling (configurable)
* **Dark mode** - High-contrast dark theme option
* **Call confirmation** - Optional dialog to prevent accidental calls

### Phone Features
* **Real phone book integration** - Uses device contacts (only shows contacts with phone numbers)
* **Starred contacts** - Quick access to favorite contacts from phone book
* **Missed calls display** - Shows missed calls from the last 4 hours
* **Simple navigation** - Home screen shows favorites and all contacts
* **One-tap calling** - Tap contact photo or green call button

### Widget
* **Home screen widget** - Large touch targets for quick calling favorites
* **Permanent scrollbar** - Always visible for easy navigation

## Settings
* Text Size (Normal / Huge)
* Dark Mode (Light / Dark)
* Call Confirmation (On / Off)
* Vibration Feedback (On / Off)
* Voice Announcements (On / Off)
* Favorites Order - Reorder with up/down arrows

## Permissions Required
* `READ_CONTACTS` - To display phone book
* `READ_CALL_LOG` - To show missed calls
* `CALL_PHONE` - To make phone calls

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Build and run: `./gradlew installDebug`
4. Grant permissions when prompted

## Target Audience

Designed for elderly users with:
- Vision impairments
- Motor control challenges
- Limited technical experience
