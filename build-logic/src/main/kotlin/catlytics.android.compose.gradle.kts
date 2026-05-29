import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

apply(plugin = "org.jetbrains.kotlin.plugin.compose")

plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension>("android") {
        buildFeatures {
            compose = true
        }
    }
}

plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension>("android") {
        buildFeatures {
            compose = true
        }
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(platform(libs.findLibrary("androidx-compose-bom").get()))
    "implementation"(libs.findLibrary("androidx-compose-ui").get())
    "implementation"(libs.findLibrary("androidx-compose-ui-graphics").get())
    "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    "implementation"(libs.findLibrary("androidx-navigation3-runtime").get())
    "androidTestImplementation"(platform(libs.findLibrary("androidx-compose-bom").get()))
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
}
