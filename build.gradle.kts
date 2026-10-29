plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds the mod for every supported Minecraft version."
    dependsOn(subprojects.map { "${it.path}:build" })
}
