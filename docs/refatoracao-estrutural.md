# Refatoração estrutural do desktop

Base: `9932f97`. A reorganização extrai responsabilidades de `AlgoraApplication.java` sem alterar funcionalidades, regras de Portugol, CSS, imagens, layout, atalhos, bibliotecas ou versões. Os commits desta refatoração permanecem locais, sem push ou atualização da release.

## Análise e escopo

A classe principal reunia o estado de documentos, configuração do editor, operações de arquivos, menus, terminal, sessão de execução, apresentação da depuração, layout e teste do aplicativo empacotado. A reorganização move essas implementações para proprietários concretos, mantendo condições, mensagens e sequência de operações dos eventos. As conexões usam callbacks da biblioteca padrão Java; não foram introduzidos frameworks ou interfaces genéricas próprias.

`AlgoraApplication.main`, `start` e `stop` permanecem nos mesmos nome e pacote. O método de aplicação de temas e `EditorTheme` também permanecem no pacote original, preservando a localização das preferências Java. `AlgoraLauncher`, `EditorTheme`, CLI e interpretador não foram modificados. A sessão de interpretação e seu estado de execução continuam no módulo `interpreter`; não se extraiu lógica desse runtime nesta etapa.

`Icons`, anteriormente interno ao pacote desktop, foi movido para `ui.components`; suas geometrias permanecem iguais. Sua visibilidade e a dos métodos extraídos foram ajustadas somente para conectar os pacotes. O caminho absoluto do recurso da logo no novo cabeçalho aponta para o mesmo arquivo existente.

## Classes e linhas

Contagem de linhas físicas, incluindo imports, comentários e linhas vazias, com `wc -l` nos fontes Java de produção do módulo desktop.

| Classe original | Antes | Depois | Destino |
| --- | ---: | ---: | --- |
| `AlgoraApplication` | 1.069 | 190 | Inicialização, composição, atalhos e coordenação |
| `Icons` | 45 | 45 | `ui.components.Icons` |
| `AlgoraLauncher` | 10 | 10 | Sem alteração |
| `EditorTheme` | 79 | 79 | Sem alteração |
| Total desktop | 1.203 | 1.595 | De 4 para 16 classes de topo |

O total de linhas aumenta por causa de declarações de pacotes, imports, construtores e conexões explícitas. A maior classe desktop após a extração tem 248 linhas.

| Nova classe | Linhas | Responsabilidade extraída |
| --- | ---: | --- |
| `documents.DocumentManager` | 248 | Documentos abertos, seleção de abas/explorador, abertura, salvamento e confirmação de alterações |
| `documents.EditorDocument` | 116 | Estado de um documento e seus eventos de editor, margem e aba; substitui a classe interna `Document` |
| `editor.PortugolEditorSupport` | 76 | Realce e autocompletar existentes |
| `terminal.TerminalPane` | 110 | Terminal, fila de entrada e entrega de saída acumulada |
| `debug.DebuggerPane` | 101 | Variáveis, lista de pontos de parada e atualização gráfica da margem |
| `execution.ExecutionToolbar` | 57 | Botões de execução e seus estados visuais |
| `execution.ExecutionController` | 219 | Sessão, execução em segundo plano, snapshots, pausa, passos e interrupção |
| `ui.ApplicationHeader` | 129 | Logo, menus, botões dos painéis e guia de atalhos |
| `ui.WorkspaceLayout` | 91 | Composição do editor/terminal e visibilidade dos painéis |
| `ui.StatusBar` | 39 | Estado e posição do cursor |
| `ui.components.IdeControls` | 30 | Fábricas de controles visuais existentes |
| `verification.PackagedApplicationProbe` | 55 | Teste de execução empacotada existente, acionado por `--smoke-test` |

## Pacotes finais

```text
io.algora.desktop
├── AlgoraApplication
├── AlgoraLauncher
├── EditorTheme
├── documents
│   ├── DocumentManager
│   └── EditorDocument
├── editor
│   └── PortugolEditorSupport
├── terminal
│   └── TerminalPane
├── debug
│   └── DebuggerPane
├── execution
│   ├── ExecutionController
│   └── ExecutionToolbar
├── ui
│   ├── ApplicationHeader
│   ├── WorkspaceLayout
│   ├── StatusBar
│   └── components
│       ├── Icons
│       └── IdeControls
└── verification
    └── PackagedApplicationProbe
```

A inspeção das dependências entre pacotes desktop não encontrou ciclos. `ui.components` e `editor` não dependem dos coordenadores. Documentos não importam execução ou depurador; os eventos necessários são conectados na inicialização por callbacks. O controlador de execução depende dos documentos e das apresentações do terminal e depurador.

## Validação incremental

Antes das extrações foi executada uma validação de referência. Em cada uma das onze etapas abaixo foram executados o build, os 23 testes existentes e o teste de execução do aplicativo empacotado:

```sh
xvfb-run -a ./gradlew build :desktop:jpackageSmokeTest --rerun-tasks
```

Após a primeira extração, o build também foi reexecutado após a formatação dos fontes. Erros de conexão e compilação introduzidos durante as extrações foram corrigidos antes de validar e commitar cada etapa.

| Módulo | Testes por etapa | Falhas | Ignorados |
| --- | ---: | ---: | ---: |
| Interpretador | 14 | 0 | 0 |
| CLI | 3 | 0 | 0 |
| Desktop JavaFX | 6 | 0 | 0 |
| Total | 23 | 0 | 0 |

Todos os onze testes de execução empacotada terminaram com `ALGORA_SMOKE_OK`. A execução exercita o aplicativo com runtime Java incluído, entrada de dados e saída esperada. Os testes JavaFX existentes exercitam execução, entrada, erros e recuperação, depuração, autocompletar, pontos de parada, temas, preferências e painéis compactos. Nenhum teste existente foi alterado.

As nove capturas dos testes foram comparadas à referência anterior. Na última etapa, oito foram idênticas byte a byte; `Algora-tema-SOLARIZED_LIGHT.png` diferiu em 22 pixels, limitados ao cursor de inserção piscando, no retângulo x=328–329, y=390–408. A captura compacta, a de execução e as demais paletas foram idênticas. CSS, fontes, logo e demais recursos permanecem sem alterações no Git.

A verificação desta refatoração foi realizada em Linux com Java 21 e Xvfb. Windows não foi reexecutado: nenhum push foi feito para acionar o workflow remoto. Os resultados Windows registrados para a release anterior continuam referindo-se àquela revisão.

## Commits incrementais

Cada linha corresponde a uma etapa validada antes do commit.

| Commit | Mensagem Conventional Commits |
| --- | --- |
| `3381eb6` | `refactor(editor): extract Portugol highlighting and completion` |
| `c0ef85b` | `refactor(terminal): isolate integrated input and buffered output` |
| `59867d8` | `refactor(ui): relocate existing icon and control factories` |
| `1da93a0` | `refactor(documents): extract document lifecycle and explorer management` |
| `141710f` | `refactor(debug): extract variables and breakpoint presentation` |
| `dec4b0f` | `refactor(execution): extract existing execution toolbar` |
| `8dd7ef7` | `refactor(execution): isolate session lifecycle and snapshot coordination` |
| `f9ecded` | `refactor(ui): extract application header menus and help dialog` |
| `ca6ef93` | `refactor(ui): isolate workspace composition and panel visibility` |
| `0bf62b5` | `refactor(ui): extract status and cursor-position bar` |
| `79ffb61` | `refactor(verification): extract packaged application smoke probe` |

Um commit final de documentação registra esta estrutura, métricas e resultados.
