plugins {
    id("catlytics.android.feature")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.feature.library.impl"
}

dependencies {
    api(project(path = ":feature:library:api"))
    implementation(project(path = ":core:domain"))
    implementation(project(path = ":core:model"))
    implementation(project(path = ":core:navigation"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
