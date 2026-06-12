import org.gradle.api.artifacts.VersionCatalogsExtension

apply(plugin = "com.google.dagger.hilt.android")
apply(plugin = "com.google.devtools.ksp")

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val kotlinVersion = libs.findVersion("kotlin").get().requiredVersion

configurations.configureEach {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinVersion")
}

dependencies {
    "implementation"(libs.findLibrary("hilt-android").get())
    "ksp"(libs.findLibrary("hilt-android-compiler").get())
}
