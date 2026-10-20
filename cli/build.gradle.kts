plugins { application }
dependencies { implementation(project(":interpreter")) }
application { mainClass.set("io.algora.cli.Main") }
tasks.named<JavaExec>("run") { standardInput = System.`in` }
