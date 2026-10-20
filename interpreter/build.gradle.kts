plugins { `java-library`; antlr }
dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    api("org.antlr:antlr4-runtime:4.13.2")
}
tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-long-messages", "-package", "io.algora.interpreter.grammar")
}
// The generator belongs to build time; only the ANTLR 4 runtime ships to consumers.
configurations.named("api") { exclude(group = "org.antlr", module = "antlr4") }
