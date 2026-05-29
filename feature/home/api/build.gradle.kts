plugins {
    id("catlytics.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.catlytics.feature.home.api"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)
}
