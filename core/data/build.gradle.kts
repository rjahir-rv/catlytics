plugins {
    id("catlytics.android.library")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
