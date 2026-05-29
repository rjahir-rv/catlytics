plugins {
    id("catlytics.android.feature")
}

android {
    namespace = "com.catlytics.feature.statistics.impl"
}

dependencies {
    api(project(":feature:statistics:api"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
}
