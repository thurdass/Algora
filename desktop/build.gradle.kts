import org.gradle.internal.os.OperatingSystem
plugins { application }
val hostOs = OperatingSystem.current()
val javafxPlatform = if (hostOs.isWindows) "win" else if (hostOs.isMacOsX) "mac" else "linux"
val javafxVersion = "21.0.13"
dependencies {
    implementation(project(":interpreter"))
    implementation("org.fxmisc.richtext:richtextfx:0.11.7")
    for (module in listOf("base", "graphics", "controls")) {
        implementation("org.openjfx:javafx-$module:$javafxVersion:$javafxPlatform")
    }
}
application { mainClass.set("io.algora.desktop.AlgoraLauncher") }
tasks.named<JavaExec>("run") { jvmArgs("-Dfile.encoding=UTF-8"); workingDir(rootProject.projectDir) }
val packageInput = tasks.register<Sync>("preparePackageInput") {
    dependsOn(tasks.jar)
    into(layout.buildDirectory.dir("jpackage/input"))
    from(tasks.jar)
    from(configurations.runtimeClasspath)
}
val jpackageExecutable = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}.map { it.metadata.installationPath.file("bin/jpackage${if (hostOs.isWindows) ".exe" else ""}").asFile.absolutePath }
val applicationIcon = layout.projectDirectory.file(
    "src/main/resources/io/algora/desktop/icons/algora.${if (hostOs.isWindows) "ico" else "png"}"
)
val appImageDirectory = layout.buildDirectory.dir("jpackage/image")
val appImage = tasks.register<Exec>("jpackageAppImage") {
    group = "distribution"
    description = "Create a self-contained Java 21 desktop application with jpackage"
    dependsOn(packageInput)
    doFirst {
        delete(appImageDirectory)
        commandLine(jpackageExecutable.get(), "--type", "app-image", "--dest", appImageDirectory.get().asFile.absolutePath,
            "--input", layout.buildDirectory.dir("jpackage/input").get().asFile.absolutePath,
            "--name", "Algora", "--app-version", project.version.toString(), "--vendor", "Algora",
            "--description", "IDE de Portugol com interpretador independente",
            "--main-jar", tasks.jar.get().archiveFileName.get(), "--main-class", application.mainClass.get(),
            "--icon", applicationIcon.asFile.absolutePath,
            "--java-options", "-Dfile.encoding=UTF-8",
            "--add-modules", "java.base,java.desktop,java.logging,java.prefs,java.xml,jdk.unsupported,java.net.http")
    }
}
tasks.register<Exec>("jpackageInstaller") {
    group = "distribution"
    description = "Create the native installer on the current host (deb on Linux, exe on Windows)"
    dependsOn(appImage)
    doFirst {
        val output = layout.buildDirectory.dir("jpackage/installer").get().asFile
        delete(output)
        val args = mutableListOf(jpackageExecutable.get(), "--type", if(hostOs.isWindows) "exe" else if(hostOs.isMacOsX) "dmg" else "deb",
            "--dest", output.absolutePath, "--name", "Algora", "--app-version", project.version.toString(),
            "--vendor", "Algora", "--icon", applicationIcon.asFile.absolutePath, "--app-image", appImageDirectory.get().dir("Algora").asFile.absolutePath)
        if(hostOs.isWindows) args += listOf("--win-menu", "--win-shortcut", "--win-dir-chooser", "--win-per-user-install")
        if(hostOs.isLinux) args += listOf("--resource-dir", layout.projectDirectory.dir("packaging/linux").asFile.absolutePath, "--linux-menu-group", "Development", "--linux-shortcut", "--linux-package-name", "algora", "--linux-deb-maintainer", "thurdass@users.noreply.github.com")
        commandLine(args)
    }
}

tasks.register<Exec>("jpackageSmokeTest") {
    group = "verification"
    description = "Launch and exercise the self-contained native application image"
    dependsOn(appImage)
    doFirst {
        val image = appImageDirectory.get().dir("Algora").asFile
        val executable = if(hostOs.isWindows) image.resolve("Algora.exe") else if(hostOs.isMacOsX) image.resolve("Contents/MacOS/Algora") else image.resolve("bin/Algora")
        commandLine(executable.absolutePath, "--smoke-test")
    }
}
