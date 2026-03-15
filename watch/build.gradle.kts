import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "ch.heuscher.simplephone.watch"
    compileSdk = 35

    defaultConfig {
        applicationId = "ch.heuscher.simplephone"
        minSdk = 26
        targetSdk = 35
        
        // Version Logic
        val versionProps = Properties()
        val versionPropsFile = rootProject.file("app/version.properties")
        if (versionPropsFile.exists()) {
            versionProps.load(FileInputStream(versionPropsFile))
        }
        
        val code = versionProps.getProperty("VERSION_CODE", "2").toInt()
        val major = versionProps.getProperty("VERSION_MAJOR", "1")
        val minor = versionProps.getProperty("VERSION_MINOR", "0")
        val patch = versionProps.getProperty("VERSION_PATCH", "0")
        
        versionCode = code
        versionName = "$major.$minor.$patch"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("app/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "key0"
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Product Flavors for Simple Phone and Gentle Phone
    flavorDimensions += "version"
    productFlavors {
        create("simplephone") {
            dimension = "version"
            applicationId = "ch.heuscher.simplephone"
            resValue("string", "app_name", "simple phone watch")
        }
        create("gentlephone") {
            dimension = "version"
            applicationId = "ch.heuscher.gentlephone"
            resValue("string", "app_name", "gentle phone watch")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    
    // Compose for Wear OS
    val wearComposeVersion = "1.4.0"
    implementation("androidx.wear.compose:compose-material:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-foundation:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-navigation:$wearComposeVersion")

    // Compose Core
    implementation(platform("androidx.compose:compose-bom:2026.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.0")

    // Wear OS support
    implementation("androidx.wear:wear:1.3.0")
}
