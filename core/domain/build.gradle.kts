plugins {
    id("catlytics.android.library")
}

android {
    namespace = "com.catlytics.core.domain"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
