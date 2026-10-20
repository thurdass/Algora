# API do interpretador

O pacote `@algora/core` não depende da IDE. O consumidor fornece o código como texto e callbacks de entrada, saída e observação.

```ts
import { createInterpreter, parse, tokenize } from '@algora/core';

const tokens = tokenize(source); // Tokens com linha e categoria.
const program = parse(source);   // AST; lança AlgoraError em erro sintático.
const interpreter = createInterpreter(source, {
  output: text => process.stdout.write(text),
  input: async () => '42',
  maxSteps: 100_000,
  maxCallDepth: 100,
  random: Math.random, // Gerador usado por aleatorio (valores em [0, 1)).
  onStep: snapshot => {
    if (snapshot.status === 'paused') {
      console.log(snapshot.line, snapshot.variables);
      // A ação posterior do usuário chama step() ou continue().
    }
  },
});
interpreter.setBreakpoints([10]);
await interpreter.run();
```

`execute(source, options)` cria uma instância e executa diretamente. `new Interpreter(source, options)` e `createInterpreter` permitem controlar a execução. Cada instância executa uma vez; reiniciar exige criar outra instância.

| Operação | Efeito |
| --- | --- |
| `run(): Promise<Snapshot>` | Inicia e aguarda conclusão; permanece pendente enquanto estiver pausado |
| `pause()` | Solicita pausa no próximo checkpoint; antes de `run`, começa pausado |
| `step()` | Libera a pausa atual e pausa no próximo checkpoint |
| `continue()` | Retoma e mantém os pontos de parada ativos |
| `stop()` | Cancela cooperativamente e libera uma pausa |
| `setBreakpoints(lines)` | Substitui o conjunto de linhas, numeradas a partir de 1 |
| `getSnapshot()` | Retorna linha, estado, passos e cópia dos valores visíveis |

`onStep` recebe snapshots com `status` igual a `idle`, `running`, `paused`, `completed`, `stopped` ou `error`. Também há notificações de execução e término: destaque a linha somente quando o estado apropriado exigir. A pausa ocorre antes da instrução. Cabeçalhos de laços voltam a gerar checkpoints entre iterações. O mapa `variables` combina escopos visíveis; nomes locais sobrepõem globais e não representam uma pilha de chamadas completa.

`input(prompt)` deve resolver uma string para cada destino de `leia`; `output(text)` recebe fragmentos, incluindo quebras de linha quando emitidas pelo programa. Um `AbortSignal` opcional também cancela a execução. O cancelamento também interrompe a espera por um callback de entrada que ainda não resolveu. O adaptador deve limpar sua interface de entrada pendente; uma resposta tardia não deve ser reutilizada em outra execução.

`AlgoraError` possui `line` e mensagem. Erros sintáticos podem surgir ao construir a instância; erros de execução rejeitam `run()`. Cancelamento rejeita com `AbortError` e produz estado `stopped`. Os consumidores devem tratar a rejeição para não gerar erros não capturados.

`random` injeta a fonte de aleatoriedade usada pelo comando `aleatorio`; deve retornar um número finito no intervalo `[0, 1)`. Isso permite entrada automática reproduzível nos testes. As funções `rand` e `randi` usam sua própria fonte padrão. Vetores estão sujeitos ao limite agregado de 1.000.000 de elementos nas declarações de cada escopo.
