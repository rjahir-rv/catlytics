plugins {
    id("catlytics.android.library")
    id("catlytics.android.compose")
}

android {
    namespace = "com.catlytics.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
}
