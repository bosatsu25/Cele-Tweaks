pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        // Loom's own dependencies (gson, asm, ...) come from Maven Central; the Fabric Maven does not
        // mirror all of them and the plugin portal stopped resolving gson 2.14.0 (CI, 2026-11-19).
        mavenCentral()
        // Google's mirror of Maven Central, on a different CDN: Central's own edge answered 404 for
        // artifacts it holds to some CI runners (2026-11-19), and a retry on the same runner hits the
        // same edge. Gradle only gets here after Central said no.
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
    }
}

rootProject.name = "cele-tweaks"

// One subproject per supported Minecraft version. Each compiles src/main plus its own
// src/mc<version> overlay (see gradle/common.gradle). `-Pmc=26.3` limits the build to one version.
val supported = listOf("26.1.2", "26.2", "26.3")
val only = providers.gradleProperty("mc").orNull
for (mc in supported) {
    if (only != null && only != mc) continue
    include(":$mc")
    project(":$mc").projectDir = file("versions/$mc")
}
