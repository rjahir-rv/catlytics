plugins {
    id("catlytics.android.feature")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.feature.settings.impl"
}

dependencies {
    api(project(":feature:settings:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
