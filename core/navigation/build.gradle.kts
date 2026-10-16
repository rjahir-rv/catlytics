plugins {
    id("catlytics.android.library")
    id("catlytics.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.catlytics.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    api(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
}
