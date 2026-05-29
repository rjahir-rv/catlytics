plugins {
    id("catlytics.android.feature")
}

android {
    namespace = "com.catlytics.feature.home.impl"
}

dependencies {
    api(project(":feature:home:api"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
}
