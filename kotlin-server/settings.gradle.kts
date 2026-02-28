plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kotlin-server"

include(":core", ":math", ":net", ":physics", ":server")
