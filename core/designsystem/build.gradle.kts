plugins {
    id("catlytics.android.library")
    id("catlytics.android.compose")
}

android {
    namespace = "com.catlytics.core.designsystem"
}

dependencies {
    implementation(project(path = ":core:model"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.coil.compose)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
}
