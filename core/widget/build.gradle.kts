plugins {
    id("catlytics.android.library")
    id("catlytics.android.compose")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.core.widget"
}

dependencies {
    implementation(project(path = ":core:designsystem"))
    implementation(project(path = ":core:domain"))
    implementation(project(path = ":core:model"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.coil)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
