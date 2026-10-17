plugins {
    id("catlytics.android.application")
    id("catlytics.android.hilt")
    alias(libs.plugins.aboutlibraries.android)
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

aboutLibraries {
    // Sin red: la lista se genera solo con los POM ya resueltos, así el build es reproducible.
    offlineMode = true
    collect {
        // Entradas manuales para lo que no es una dependencia Maven (el `libtaglib.so` nativo).
        configPath = file("config/aboutlibraries")
    }
    license {
        // Los textos viven en config/aboutlibraries/licenses/ (offline). Una licencia nueva
        // rompe el build hasta que se agregue su texto ahí y aquí.
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "BSD-3-Clause", "LGPL-2.1-only", "MPL-1.1")
    }
}

dependencies {
    implementation(project(path = ":core:data"))
    implementation(project(path = ":core:designsystem"))
    implementation(project(path = ":core:domain"))
    implementation(project(path = ":core:navigation"))
    implementation(project(path = ":core:playback"))
    implementation(project(path = ":core:widget"))
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
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coil.compose)
    implementation(libs.haze.blur)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
