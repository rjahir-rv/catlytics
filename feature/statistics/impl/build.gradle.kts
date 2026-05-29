plugins {
    id("catlytics.android.feature")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.feature.statistics.impl"
}

dependencies {
    api(project(":feature:statistics:api"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
}
