plugins {
    id("catlytics.android.feature")
}

android {
    namespace = "com.catlytics.feature.library.impl"
}

dependencies {
    api(project(":feature:library:api"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
}
