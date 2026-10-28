import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

apply(plugin = "dev.detekt")

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<DetektExtension>("detekt") {
    buildUponDefaultConfig.set(true)
    parallel.set(true)
    config.setFrom(layout.settingsDirectory.file("config/detekt/detekt.yml"))
    baseline.set(layout.projectDirectory.file("detekt-baseline.xml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
}

dependencies {
    "detektPlugins"(libs.findLibrary("detekt-rules-ktlint-wrapper").get())
    "detektPlugins"(libs.findLibrary("compose-rules-detekt").get())
}

fun Lint.configureCatlytics() {
    baseline = file("lint-baseline.xml")
    // Las versiones nuevas las propone Dependabot; aquí solo harían caducar la baseline.
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}

plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension>("android") {
        lint { configureCatlytics() }
    }
}

plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension>("android") {
        lint { configureCatlytics() }
    }
}
