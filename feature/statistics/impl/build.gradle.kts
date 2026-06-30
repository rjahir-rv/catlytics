plugins {
    id("catlytics.android.feature")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.feature.statistics.impl"
}

dependencies {
    api(project(path = ":feature:statistics:api"))
    implementation(project(path = ":core:navigation"))
    implementation(project(path = ":core:domain"))
    implementation(project(path = ":core:model"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
}
