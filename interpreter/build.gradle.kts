plugins { `java-library`; antlr }
dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    api("org.antlr:antlr4-runtime:4.13.2")
}
tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-long-messages", "-package", "io.algora.interpreter.grammar")
}
