plugins {
    `kotlin-dsl`
}

group = "com.catlytics.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.hilt.android.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}
