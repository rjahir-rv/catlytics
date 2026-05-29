import com.android.build.api.dsl.ApplicationExtension

apply(plugin = "com.android.application")
apply(plugin = "catlytics.android.compose")

extensions.configure<ApplicationExtension>("android") {
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
