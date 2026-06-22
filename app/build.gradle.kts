plugins {
    id("catlytics.android.application")
    id("catlytics.android.hilt")
}

android {
    namespace = "com.catlytics.app"

    defaultConfig {
        applicationId = "com.catlytics.app"
        versionCode = 1
        versionName = "0.0.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(path = ":core:data"))
    implementation(project(path = ":core:designsystem"))
    implementation(project(path = ":core:domain"))
    implementation(project(path = ":core:navigation"))
    implementation(project(path = ":core:playback"))
    implementation(project(path = ":feature:home:api"))
    implementation(project(path = ":feature:home:impl"))
    implementation(project(path = ":feature:library:api"))
    implementation(project(path = ":feature:library:impl"))
    implementation(project(path = ":feature:playlists:api"))
    implementation(project(path = ":feature:playlists:impl"))
    implementation(project(path = ":feature:settings:api"))
    implementation(project(path = ":feature:settings:impl"))
    implementation(project(path = ":feature:statistics:api"))
    implementation(project(path = ":feature:statistics:impl"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
