plugins {
    id("catlytics.android.library")
    id("catlytics.android.compose")
}

android {
    namespace = "com.catlytics.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.compose.runtime)
}
