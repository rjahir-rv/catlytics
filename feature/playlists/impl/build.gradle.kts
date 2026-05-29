plugins {
    id("catlytics.android.feature")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.feature.playlists.impl"
}

dependencies {
    api(project(":feature:playlists:api"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
}
