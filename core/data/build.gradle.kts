plugins {
    id("catlytics.android.library")
    id("catlytics.android.hilt")
    id("catlytics.android.room")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.catlytics.core.data"
}

dependencies {
    api(project(path = ":core:domain"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.taglib)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
