# Algora

![Status: Beta](https://img.shields.io/badge/status-Beta-orange)
![Versão: v0.2.0](https://img.shields.io/badge/vers%C3%A3o-v0.2.0-blue)
![Java 21](https://img.shields.io/badge/Java-21-007396)
![Plataformas: Linux e Windows](https://img.shields.io/badge/plataformas-Linux%20%7C%20Windows-555555)

> **Beta v0.2.0:** o Algora está em desenvolvimento ativo e pode apresentar bugs ou comportamentos inesperados. Esta versão já oferece edição, execução e depuração de algoritmos em uma interface moderna.

<p align="center">
  <img src="docs/assets/algora-logo.png" alt="Logo do Algora" width="320">
</p>

**Algora** é uma IDE moderna para escrever, executar e depurar algoritmos em Portugol, desenvolvida para **Linux e Windows**, com interpretador próprio para arquivos `.alg` do dialeto VisualG.

Idealizado e desenvolvido por **Arthur Almeida**, o Algora é um projeto pessoal criado para oferecer uma IDE moderna e acessível a estudantes de lógica de programação. Seu objetivo é unir simplicidade, aprendizado e uma experiência de desenvolvimento que acompanhe os primeiros passos na programação.

**Atualmente em fase beta (v0.2.0)**, o projeto continua recebendo melhorias, correções e novas funcionalidades.

O projeto usa **Java 21, JavaFX 21, ANTLR4 e RichTextFX**, com **Gradle Kotlin DSL**, testes **JUnit 5** e distribuição nativa via **jpackage**.

A interface reúne editor com abas e realce de sintaxe, entrada e saída do algoritmo, autocompletar e depurador com pontos de parada e inspeção de variáveis. O núcleo da linguagem permanece independente da interface gráfica. Para informações sobre os recursos do dialeto VisualG atendidos pelo Algora, consulte a [matriz de compatibilidade](docs/compatibilidade.md) e o [levantamento Java](interpreter/compatibility-survey.md).

![IDE JavaFX do Algora](docs/assets/algora.png)

A interface utiliza fontes IBM Plex locais, painéis recolhíveis e composição adaptada a janelas compactas. Os 23 testes e a execução dos aplicativos empacotados passaram em Linux e Windows. Veja a [direção visual](docs/design-interface.md), a [visualização compacta](docs/assets/algora-compacto.png) e o [registro de verificação](docs/verificacao.md).

## Download — Beta v0.2.0

Baixe os instaladores na [release v0.2.0](https://github.com/thurdass/Algora/releases/tag/v0.2.0):

- **Windows x64:** prefira `Algora-0.2.0.msi`, que abre diretamente no Windows Installer. O instalador `Algora-0.2.0.exe` também está disponível.
- **Linux x64 (Debian/Ubuntu):** baixe `algora_0.2.0_amd64.deb` e execute `sudo apt install ./algora_0.2.0_amd64.deb`.

Os instaladores incluem o runtime Java; não é necessário instalar um JDK para usar a IDE. O arquivo `SHA256SUMS.txt` permite conferir a integridade dos downloads. O pacote Linux é destinado a distribuições compatíveis com `.deb`.

## Executar a partir do código

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
| `Ctrl+B` | Mostrar ou ocultar arquivos |
| `Ctrl+Shift+D` | Mostrar ou ocultar depuração |
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
- `desktop`: aplicação JavaFX 21 e editor RichTextFX, organizados em documentos, editor, terminal, depuração, execução e componentes visuais. Veja o [relatório da refatoração estrutural](docs/refatoracao-estrutural.md).
- `cli`: execução de algoritmos no terminal usando o mesmo interpretador.
- `examples`: programas originais para experimentar.
- `docs`: arquitetura, API, compatibilidade e evidências da investigação.

Consulte a [arquitetura](docs/arquitetura.md), a [API Java](docs/api.md) e o [relatório REA](docs/engenharia-reversa.md). O Algora foi implementado de forma independente. O executável, recursos e código proprietário do VisualG não são distribuídos. A análise estática do original não comprova equivalência integral de comportamento.

Repositório: [thurdass/Algora](https://github.com/thurdass/Algora), privado. Os commits seguem Conventional Commits.

### Aparência

Escolha entre seis temas em **Exibir → Tema de cores**: Grafite, Dracula, Nord, Solarizado escuro, Claro e Solarizado claro. A preferência é salva automaticamente. O layout segue as referências do IntelliJ e VS Code, com barras compactas e atalhos laterais para os painéis. Consulte [o guia visual](docs/design-interface.md).
