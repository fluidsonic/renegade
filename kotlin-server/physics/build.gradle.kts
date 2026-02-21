plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":math"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
