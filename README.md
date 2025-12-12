# Simple Phone

An accessible Android phone app designed for elderly users with vision and motor control challenges.

## 🎯 Key Features

### Accessibility First
* **Extra Large Touch Targets** - All buttons are 48dp+ for easy tapping
* **High Contrast Colors** - Deep blue and bright green for maximum visibility
* **Huge Text Option** - Make contact names as large as photos
* **Haptic Feedback** - Feel vibration on every button press
* **Voice Announcements** - Hear contact names spoken when calling
* **Dark Mode** - High-contrast dark theme for low-light conditions
* **Call Confirmation** - Optional dialog to prevent accidental calls

### Phone Functionality
* **Real Phone Book Integration** - Uses your device contacts (shows only contacts with phone numbers)
* **Starred Contacts** - Quick access to favorites at the top of the screen
* **One-Tap Calling** - Tap the contact photo or green call button
* **Missed Calls Display** - Shows recent missed calls
* **Smart Search** - Quickly find any contact by name

### Call Handling
* **Incoming Call Screen** - Large, easy-to-tap Answer and Reject buttons
* **Audio Output Selection** - Switch between Speaker, Bluetooth, and Earpiece during calls
* **Do Not Disturb Aware** - Respects system DND settings
* **Repeat Caller Exception** - Rings for repeat callers within 15 minutes (when enabled in DND)

### Home Screen Widget
* **Large Touch Targets** - Quick access to favorite contacts from home screen
* **Always Visible Scrollbar** - Easy navigation through favorites

## 📱 Screenshots

The app features:
- Clean, uncluttered interface
- Large contact photos with names
- Green call buttons that are easy to spot
- Simple bottom navigation bar

## ⚙️ Settings

| Setting | Description |
|---------|-------------|
| Text Size | Normal or Huge text for contact names |
| Dark Mode | System default, Light, or Dark theme |
| Call Confirmation | Ask before making each call |
| Vibration Feedback | Vibrate on button presses |
| Voice Announcements | Speak contact names when calling |
| Favorites Order | Reorder favorites with up/down arrows |

## 🔐 Permissions Required

| Permission | Purpose |
|------------|---------|
| `READ_CONTACTS` | Display your phone book |
| `READ_CALL_LOG` | Show missed calls |
| `CALL_PHONE` | Make phone calls |
| `READ_PHONE_STATE` | Handle incoming calls |
| `ANSWER_PHONE_CALLS` | Answer calls from the app |
| `POST_NOTIFICATIONS` | Show missed call notifications |

## 🚀 Getting Started

1. Clone the repository
2. Open in Android Studio
3. Build and run: `./gradlew installDebug`
4. Grant permissions when prompted
5. **Set as Default Dialer** - For best experience, set Simple Phone as your default phone app

## 🎓 First-Time Setup

When you first open the app, you'll see an onboarding guide that explains:
- How to access your favorite contacts
- How to search for contacts
- How to see missed calls
- Audio options during calls
- Available settings

## 👴 Target Audience

Designed specifically for users with:
- Vision impairments
- Motor control challenges  
- Limited technical experience
- Need for simplified phone interface

## 🛠️ Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## 📄 License

This project is open source. Feel free to use and modify for your needs.
