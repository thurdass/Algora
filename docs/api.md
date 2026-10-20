# API Java do interpretador

O módulo `interpreter` usa Java 21 e não depende da interface. A fachada pública é `io.algora.interpreter.AlgoraEngine`; `DebugSession`, `Snapshot`, `Status`, `InputProvider`, `Program` e `AlgoraException` são tipos públicos do mesmo pacote.

```java
import io.algora.interpreter.AlgoraEngine;
import io.algora.interpreter.DebugSession;
import io.algora.interpreter.Snapshot;
import io.algora.interpreter.Status;
import java.util.Set;

DebugSession session = AlgoraEngine.createSession(
    source,
    () -> "42",                      // Uma string por leitura.
    System.out::print,
    snapshot -> {
        if (snapshot.status() == Status.PAUSED) {
            System.out.println("Linha: " + snapshot.line());
            System.out.println(snapshot.variables());
            // Uma ação posterior do usuário chama step() ou resume().
        }
    });
session.limits(100_000, 100);
session.setBreakpoints(Set.of(10));
var completion = session.start(false);
// Não aguarde na thread JavaFX enquanto a execução está pausada.
Snapshot finalState = completion.join();
```

| Operação | Finalidade |
| --- | --- |
| `AlgoraEngine.parse(String)` | Analisa o texto e devolve um `Program`, sem executar instruções |
| `AlgoraEngine.run(String, InputProvider, Consumer<String>)` | Executa diretamente e devolve um `Snapshot` final |
| `AlgoraEngine.createSession(String, InputProvider, Consumer<String>, Consumer<Snapshot>)` | Cria uma `DebugSession` controlável e prepara as declarações globais |
| `InputProvider.read()` | Fornece uma entrada textual para cada destino de `leia`; pode lançar exceção |

O provedor pode ser nulo quando o programa não precisa de entrada humana. `null` retornado por `read()` sinaliza entrada encerrada e produz erro. Os callbacks de saída e observação podem ser nulos. `AlgoraException.line()` informa a linha da falha; a mensagem também inclui essa linha. A criação da sessão pode falhar ao avaliar declarações globais, além dos erros sintáticos.

## Sessões e snapshots

| Método de `DebugSession` | Efeito |
| --- | --- |
| `start(boolean debug)` | Executa em thread virtual e retorna `CompletableFuture<Snapshot>`; `true` pausa no primeiro checkpoint |
| `setBreakpoints(Set<Integer>)` | Substitui o conjunto de linhas, numeradas a partir de 1 |
| `pause()` | Solicita pausa no próximo checkpoint |
| `step()` | Libera a pausa atual e pausa no próximo checkpoint |
| `resume()` | Retoma respeitando os pontos de parada |
| `stop()` | Solicita cancelamento, acorda uma pausa e interrompe a thread de execução |
| `snapshot()` | Retorna o último snapshot publicado |
| `limits(long instructions, int calls)` | Configura limites positivos de instruções e profundidade de chamadas antes de iniciar |
| `random(DoubleSupplier)` | Injeta o gerador antes de iniciar; cada resultado deve ser finito e estar em `[0, 1)` |

Cada sessão executa uma vez. Para reiniciar, crie outra sessão. Os limites padrão são 100.000 checkpoints de instruções e 100 chamadas aninhadas. A alocação de variáveis e estruturas vetoriais tem um orçamento interno de 1.000.000 de posições; não é um limite de memória em bytes. O gerador injetado alimenta `aleatorio`, `rand` e `randi`.

`Snapshot` é um record com `line`, `variables`, `status` e `steps`. O mapa de variáveis é somente leitura e copia listas de vetores; reúne globais e locais da rotina atual, com locais sobrepondo nomes globais. Não representa a pilha completa de chamadas. `Status` contém `READY`, `RUNNING`, `PAUSED`, `COMPLETED`, `STOPPED` e `ERROR`.

A pausa acontece antes da instrução indicada. Cabeçalhos de laços voltam a gerar checkpoints entre iterações. O passo entra nas instruções das rotinas; não há comandos de passo sobre ou saída de função. `steps` conta checkpoints, incluindo os de cabeçalhos repetidos, e não instruções de máquina.

O observador recebe o estado inicial durante a criação e atualizações durante a execução. Encaminhe mudanças gráficas à thread JavaFX, por exemplo com `Platform.runLater`, e mantenha callbacks rápidos. `stop()` depende de o provedor bloqueante de entrada respeitar a interrupção da thread; a IDE usa uma fila interrompível. Provedores que ignoram interrupção podem atrasar o encerramento. O adaptador deve limpar controles de entrada pendentes.

Cancelamento conclui a future normalmente com estado `STOPPED`. Erros de execução a concluem excepcionalmente e publicam `ERROR`. `AlgoraEngine.run` usa `join()` internamente; esses erros podem chegar envolvidos em `CompletionException`. O consumidor deve tratar ambos os caminhos.
