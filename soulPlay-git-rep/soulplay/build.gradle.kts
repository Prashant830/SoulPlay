// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}

// Run `./gradlew updateDaemonJvm` to regenerate `gradle/gradle-daemon-jvm.properties`.
// That pins a full JDK (with jlink) for the Gradle daemon so AGP does not use a broken IDE JRE.
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}