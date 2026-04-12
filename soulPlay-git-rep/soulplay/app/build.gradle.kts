import com.android.build.api.dsl.ApplicationExtension
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val agoraAppId: String = localProperties.getProperty("AGORA_APP_ID", "")

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore: Boolean = keystorePropertiesFile.exists()

android {
    namespace = "com.souljoy.soulmasti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.souljoy.soulmasti"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AGORA_APP_ID", "\"$agoraAppId\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                val storePwd = keystoreProperties.getProperty("storePassword")
                    ?: error("keystore.properties: storePassword is required")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: error("keystore.properties: keyAlias is required")
                keyPassword = keystoreProperties.getProperty("keyPassword", storePwd)
                storePassword = storePwd
                storeFile = rootProject.file(
                    keystoreProperties.getProperty("storeFile")
                        ?: error("keystore.properties: storeFile is required"),
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Versioned output names (AGP 9 new DSL has no stable applicationVariants.outputs API here):
//   APK: build/outputs/apk/<debug|release>/SoulMasti-<type>-v<versionName>-<versionCode>.apk
//   AAB: build/outputs/bundle/release/SoulMasti-release-v<versionName>-<versionCode>.aab
afterEvaluate {
    fun versionPair(): Pair<String, Int> {
        val app = extensions.getByType(ApplicationExtension::class.java)
        val vn = app.defaultConfig.versionName ?: "0"
        val vc = app.defaultConfig.versionCode ?: 0
        return vn to vc
    }

    fun renameApkIn(dir: File, buildType: String) {
        if (!dir.isDirectory) return
        val (vn, vc) = versionPair()
        val destName = "SoulMasti-${buildType}-v${vn}-${vc}.apk"
        val dest = File(dir, destName)
        val src = dir.listFiles()
            ?.filter { it.isFile && it.extension == "apk" && it.name.startsWith("app-") }
            ?.maxByOrNull { it.lastModified() }
            ?: return
        if (src.name == destName) return
        if (dest.exists()) dest.delete()
        if (!src.renameTo(dest)) {
            src.copyTo(dest, overwrite = true)
            src.delete()
        }
    }

    listOf("debug" to "assembleDebug", "release" to "assembleRelease").forEach { (type, taskName) ->
        tasks.findByName(taskName)?.doLast {
            renameApkIn(
                layout.buildDirectory.dir("outputs/apk/$type").get().asFile,
                type,
            )
        }
    }

    tasks.findByName("bundleRelease")?.doLast {
        val (vn, vc) = versionPair()
        val dir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        val src = dir.listFiles()?.firstOrNull { it.isFile && it.extension == "aab" } ?: return@doLast
        val dest = File(dir, "SoulMasti-release-v${vn}-${vc}.aab")
        if (src.absolutePath == dest.absolutePath) return@doLast
        if (dest.exists()) dest.delete()
        if (!src.renameTo(dest)) {
            src.copyTo(dest, overwrite = true)
            src.delete()
        }
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.android.gms" && requested.name == "play-services-base") {
            // Prevent Gradle from trying to download missing sources for older base versions.
            useVersion("18.1.0")
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation(libs.kotlinx.coroutines.play.services)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.agora.rtc:voice-rtc-basic:4.6.3")
    implementation("com.android.billingclient:billing:7.0.0")
    implementation("com.airbnb.android:lottie-compose:6.6.2")
}