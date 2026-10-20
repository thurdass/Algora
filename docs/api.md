# API Java do interpretador

O módulo `interpreter` usa Java 21 e não depende da interface. A fachada pública é `io.algora.interpreter.AlgoraEngine`. O consumidor fornece código como texto, um provedor de entrada e callbacks de saída e observação.

| Operação | Finalidade |
| --- | --- |
| `AlgoraEngine.parse(String)` | Analisa o texto e devolve um `Program` |
| `AlgoraEngine.run(String, InputProvider, Consumer<String>)` | Executa diretamente e devolve um `Snapshot` final |
| `AlgoraEngine.createSession(String, InputProvider, Consumer<String>, Consumer<Snapshot>)` | Cria uma `DebugSession` controlável |
| `InputProvider.read()` | Fornece uma entrada textual para `leia`; pode lançar exceção |

`Program` representa a AST do algoritmo. Erros da linguagem são apresentados por `AlgoraException`; os consumidores devem tratar erros de análise e de execução, incluindo os que chegam pela conclusão assíncrona da sessão.

## Sessões e snapshots

| Método de `DebugSession` | Efeito |
| --- | --- |
| `start(boolean debug)` | Inicia a execução e retorna `CompletableFuture<Snapshot>` |
| `setBreakpoints(Set<Integer>)` | Define o conjunto de linhas de parada |
| `pause()` | Solicita pausa em um checkpoint |
| `step()` | Avança até o próximo checkpoint |
| `resume()` | Retoma a execução |
| `stop()` | Solicita cancelamento |
| `snapshot()` | Consulta o estado observado |

`Snapshot` possui `line`, `variables`, `status` e `steps`. O mapa `variables` expõe nomes e valores; `status` usa `READY`, `RUNNING`, `PAUSED`, `COMPLETED`, `STOPPED` ou `ERROR`. A interface deve observar o estado ao destacar a linha ou habilitar ações de depuração. Um passo corresponde a um checkpoint de instrução; não representa uma instrução de máquina.

O callback de observação pertence à execução do interpretador: encaminhe atualizações gráficas à thread JavaFX. Não bloqueie a thread gráfica esperando `CompletableFuture.get()`. O provedor de entrada deve coordenar a espera por uma resposta com o host e limpar controles pendentes ao encerrar uma execução.

As assinaturas e o comportamento detalhado devem ser conferidos no código público e nos testes JUnit do módulo. Resultados do protótipo TypeScript não estabelecem comportamento desta API Java.
