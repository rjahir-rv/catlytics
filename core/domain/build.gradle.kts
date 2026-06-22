plugins {
    id("catlytics.android.library")
}

android {
    namespace = "com.catlytics.core.domain"
}

dependencies {
    api(project(path = ":core:model"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
