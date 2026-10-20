# Algora

IDE para escrever, executar e depurar algoritmos em Portugol, com interpretador próprio para arquivos `.alg` do dialeto VisualG. O projeto usa **Java 21, JavaFX 21, ANTLR4 e RichTextFX**, com **Gradle Kotlin DSL**, testes **JUnit 5** e distribuição nativa via **jpackage**.

A interface reúne editor com abas e realce de sintaxe, entrada e saída do algoritmo, autocompletar e depurador com pontos de parada e inspeção de variáveis. O núcleo da linguagem permanece independente da interface gráfica. A compatibilidade é parcial: o [levantamento Java](interpreter/compatibility-survey.md) aceitou 58 de 73 arquivos do conjunto local pelo parser, sem afirmar equivalência integral de execução. Consulte a [matriz de compatibilidade](docs/compatibilidade.md) antes de importar programas existentes.

## Executar

Instale um JDK 21 completo. O Gradle Wrapper baixa as dependências na primeira execução.

```sh
./gradlew test
./gradlew :desktop:run
./gradlew :cli:run --args='examples/ola.alg'
```

No Windows, substitua `./gradlew` por `.\gradlew.bat`. Os exemplos originais em `examples/` incluem entrada de dados, fatorial e vetores. O terminal integrado recebe valores de `leia` e apresenta a saída; não é um shell do sistema operacional.

## Usar a IDE

| Atalho | Ação |
| --- | --- |
| `Ctrl+N`, `Ctrl+O`, `Ctrl+S` | Novo arquivo, abrir e salvar |
| `Ctrl+Espaço` | Sugestões de palavras e modelos de estruturas |
| `F5` | Executar ou continuar |
| `F9` | Alternar ponto de parada na linha atual |
| `F10` | Avançar uma instrução |

Também é possível marcar pontos de parada na margem esquerda do editor. Os controles permitem iniciar depuração, pausar e interromper. A abertura tenta UTF-8 e usa Windows-1252 para arquivos legados; o salvamento escreve UTF-8.

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
