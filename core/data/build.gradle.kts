plugins {
    id("catlytics.android.library")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.core.data"
}

dependencies {
    api(project(path = ":core:domain"))
    implementation(project(path = ":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
