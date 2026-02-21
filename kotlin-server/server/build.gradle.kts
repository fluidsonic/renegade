plugins {
    application
}

application {
    mainClass.set("ccr.server.MainKt")
    applicationDefaultJvmArgs = listOf("-Duser.dir=${rootDir.parentFile}")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir.parentFile  // project root (ccr/)
}

tasks.register<JavaExec>("computeExeKey") {
    group = "tools"
    description = "Compute Renegade ExeKey / ExeCRC / StringsCRC from game data files"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ccr.server.ComputeExeKeyKt")
    workingDir = rootDir.parentFile  // project root (ccr/)
}

tasks.register<JavaExec>("verifyDefIds") {
    group = "tools"
    description = "Read Objects.DDB from always.dat and verify soldier definition IDs"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ccr.server.VerifyDefIdsKt")
    workingDir = rootDir.parentFile  // project root (ccr/)
}

tasks.register<JavaExec>("miniClient") {
    group = "tools"
    description = "Minimal Renegade client for packet-capture diagnostics"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ccr.server.MiniClientKt")
    // Pass -PserverHost=<ip> -PserverPort=<port> to configure
    systemProperty("serverHost", findProperty("serverHost") ?: "127.0.0.1")
    systemProperty("serverPort", findProperty("serverPort") ?: "4848")
    systemProperty("exeKey", findProperty("exeKey") ?: "0")
}

tasks.register<JavaExec>("liveProxy") {
    group = "tools"
    description = "Live UDP proxy with real-time packet decoding between Renegade client and C++ server"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ccr.server.LiveProxyKt")
    workingDir = rootDir.parentFile
    systemProperty("localPort",  findProperty("localPort")  ?: "4848")
    systemProperty("remoteHost", findProperty("remoteHost") ?: "127.0.0.1")
    systemProperty("remotePort", findProperty("remotePort") ?: "4849")
    findProperty("logFile")?.let { systemProperty("logFile", it) }
}

dependencies {
    implementation(project(":math"))
    implementation(project(":net"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "ccr.server.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
