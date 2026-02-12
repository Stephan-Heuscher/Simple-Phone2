# ProGuard / R8 rules for Simple Phone / gentle phone
#
# User benefit: R8 shrinks the APK, removes unused code, and obfuscates
# class names — resulting in a smaller download and faster install for users.

# Compose — keep runtime metadata
-dontwarn androidx.compose.**

# Keep Firebase Firestore model classes (Gentle Phone remote settings)
-keep class ch.heuscher.simplephone.model.** { *; }

# Keep CallService and InCallService (system-invoked via telecom framework)
-keep class ch.heuscher.simplephone.call.CallService { *; }
-keep class ch.heuscher.simplephone.call.NotificationReceiver { *; }

# Keep Coil (image loading) internals
-dontwarn coil.**

# Keep UCrop activity
-keep class com.yalantis.ucrop.** { *; }

# Keep libphonenumber metadata
-keep class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**

# Firebase Firestore
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# Standard Android keep rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
