# Algora

IDE para escrever, executar e depurar algoritmos em Portugol, com interpretador próprio para arquivos `.alg` do dialeto VisualG. O projeto usa **Java 21, JavaFX 21, ANTLR4 e RichTextFX**, com **Gradle Kotlin DSL**, testes **JUnit 5** e distribuição nativa via **jpackage**.

A interface reúne editor, entrada e saída do algoritmo, autocompletar e depurador. O núcleo da linguagem permanece independente da interface gráfica. A compatibilidade é parcial; consulte a [matriz de compatibilidade](docs/compatibilidade.md) antes de importar programas existentes.

## Executar

Instale um JDK 21 completo. O Gradle Wrapper baixa as dependências na primeira execução.

```sh
./gradlew test
./gradlew :desktop:run
./gradlew :cli:run --args='examples/ola.alg'
```

No Windows, substitua `./gradlew` por `.\gradlew.bat`. Os exemplos originais em `examples/` incluem entrada de dados, fatorial e vetores. O terminal integrado recebe valores de `leia` e apresenta a saída; não é um shell do sistema operacional.

## Gerar distribuição

```sh
./gradlew :desktop:jpackageAppImage
./gradlew :desktop:jpackageInstaller
```

`jpackageAppImage` gera uma pasta de aplicação com runtime Java incluído, correspondente ao tipo `app-image` do JDK. Não gera um arquivo Linux `.AppImage`. `jpackageInstaller` gera o instalador nativo do sistema onde o build é executado. Linux e Windows exigem builds e validação próprios. Veja [instalação](docs/instalacao.md) para requisitos e limitações.

## Organização

- `interpreter`: gramática ANTLR4, AST, execução e API de depuração, sem dependência gráfica.
- `desktop`: aplicação JavaFX 21 e editor RichTextFX.
- `cli`: execução de algoritmos no terminal usando o mesmo interpretador.
- `examples`: programas originais para experimentar.
- `docs`: arquitetura, API, compatibilidade e evidências da investigação.

Consulte a [arquitetura](docs/arquitetura.md), a [API Java](docs/api.md) e o [relatório REA](docs/engenharia-reversa.md). O Algora foi implementado de forma independente. O executável, recursos e código proprietário do VisualG não são distribuídos. A análise estática do original não comprova equivalência integral de comportamento.

Repositório: [thurdass/Algora](https://github.com/thurdass/Algora), privado. Os commits seguem Conventional Commits.
