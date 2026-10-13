plugins {
    id("io.papermc.paperweight.userdev")
}

// Minecraft 26.2 requires Java 25; override the project-wide release=21 baseline.
tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

dependencies {
    api(project(":platforms:bukkit:common"))
    paperweight.paperDevBundle(Versions.Bukkit.paperDevBundle)
    implementation("xyz.jpenilla", "reflection-remapper", Versions.Bukkit.reflectionRemapper)
}