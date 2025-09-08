import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    id("com.android.application")
}

fun getKey(key: String): String {
    val fl = rootProject.file("key.properties")
    if (fl.exists()) {
        val properties = Properties()
        properties.load(FileInputStream(fl))
        return properties.getProperty(key)
    } else {
        throw FileNotFoundException()
    }
}

android {
    namespace = "net.gitsaibot.af"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.gitsaibot.af"
        minSdk = 29
        targetSdk = 36
        versionCode = 19
        versionName = "2.8"
        buildConfigField("String", "USER_AGENT", "\"" + getKey("user_agent") + "\"")
        buildConfigField("String", "API_KEY", "\"" + getKey("apiKey") + "\"")
        buildConfigField("String", "USER_GEONAMES", "\"" + getKey("user_geonames") + "\"")
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.txt"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
}

dependencies {
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.legacy:legacy-preference-v14:1.0.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))
}
