import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

apply(plugin = "org.jetbrains.kotlin.plugin.compose")

extensions.configure<ComposeCompilerGradlePluginExtension>("composeCompiler") {
    stabilityConfigurationFiles.add(layout.settingsDirectory.file("compose_stability.conf"))
    // Informes de estabilidad/recomposición: ./gradlew assembleDebug -Pcatlytics.composeReports=true
    if (providers.gradleProperty("catlytics.composeReports").orNull == "true") {
        reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
        metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
    }
}

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
    // ui-test trae espresso-core 3.5.0, que falla en API 37 (InputManager.getInstance).
    "androidTestImplementation"(libs.findLibrary("androidx-espresso-core").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
}
