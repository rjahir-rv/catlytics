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
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
