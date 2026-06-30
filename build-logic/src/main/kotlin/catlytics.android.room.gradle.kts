import org.gradle.api.artifacts.VersionCatalogsExtension

apply(plugin = "com.google.devtools.ksp")
apply(plugin = "androidx.room")

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<androidx.room.gradle.RoomExtension>("room") {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    "implementation"(libs.findLibrary("androidx-room-runtime").get())
    "implementation"(libs.findLibrary("androidx-room-ktx").get())
    "ksp"(libs.findLibrary("androidx-room-compiler").get())
}
