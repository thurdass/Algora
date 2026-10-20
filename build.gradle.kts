plugins { base }
allprojects {
    group = "io.algora"
    version = "0.2.0"
    repositories { mavenCentral() }
}
subprojects {
    apply(plugin = "java")
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
        testLogging { events("passed", "skipped", "failed") }
    }
    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:5.11.4"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
tasks.register("test") {
    group = "verification"
    dependsOn(subprojects.map { "${it.path}:test" })
}
tasks.named("check") { dependsOn(subprojects.map { "${it.path}:check" }) }
tasks.named("assemble") { dependsOn(subprojects.map { "${it.path}:assemble" }) }
