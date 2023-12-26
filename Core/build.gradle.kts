plugins {
    id("dev.mrshawn.deathmessages.wrapper")
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply true
}

dependencies {
    compileOnly("com.github.sirblobman.combatlogx:CombatLogX:11.4.0.2.Beta-1212")
    api(project(":WorldGuard6"))
    api(project(":WorldGuard7"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName = "${rootProject.name}-${project.version}.${archiveExtension.get()}"
    exclude("META-INF/**") // Dreeam - Avoid to include META-INF/maven in Jar
    minimize {
        exclude(dependency("com.tcoded.folialib:.*:.*"))
    }
    relocate("kotlin", "dev.mrshawn.deathmessages.libs.kotlin")
    relocate("net.kyori", "dev.mrshawn.deathmessages.libs.kyori")
    relocate("com.cryptomorin.xseries", "dev.mrshawn.deathmessages.libs.xseries")
    relocate("org.bstats", "dev.mrshawn.deathmessages.libs.bstats")
    relocate("com.tcoded.folialib", "dev.mrshawn.deathmessages.libs.folialib")
    relocate("de.tr7zw.changeme.nbtapi", "dev.mrshawn.deathmessages.libs.nbtapi")
    relocate("net.dv8tion.jda", "dev.mrshawn.deathmessages.libs.jda")
}
