plugins {
    id("catlytics.android.library")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.core.playback"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
