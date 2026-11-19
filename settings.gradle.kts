pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        // Loom's own dependencies (gson, asm, ...) come from Maven Central; the Fabric Maven does not
        // mirror all of them and the plugin portal stopped resolving gson 2.14.0 (CI, 2026-11-19).
        mavenCentral()
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
