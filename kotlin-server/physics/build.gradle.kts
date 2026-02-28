plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":math"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
