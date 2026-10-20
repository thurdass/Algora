# @algora/core

Interpretador independente, escrito em TypeScript e sem dependências da interface. O código fonte não contém código do VisualG. Funciona em ambientes JavaScript modernos no Linux e Windows.

```ts
import { createInterpreter } from '@algora/core';
const interpreter = createInterpreter(source, {
  output: text => process.stdout.write(text),
  input: async () => '42',
  onStep: snapshot => console.log(snapshot.line, snapshot.variables),
  maxSteps: 100_000,
});
interpreter.setBreakpoints([10]);
// Para começar pausado, chame interpreter.pause() antes de run().
const completion = interpreter.run();
// No callback onStep, quando status === 'paused', use step() ou continue().
await completion;
```

`parse(source)` retorna a árvore sintática; `tokenize(source)` retorna tokens com linhas. `execute(source, options)` executa sem precisar conservar uma instância. Cada instância aceita uma execução; crie outra para reiniciar. `stop()` ou `AbortSignal` cancelam a execução. O limite padrão é de 100.000 instruções e 100 chamadas aninhadas. Laços cedem periodicamente o controle ao ambiente, permitindo cancelar sem bloquear indefinidamente a interface.

O depurador pausa **antes** de executar a instrução indicada. `getSnapshot()` contém linha, estado, número de passos e uma cópia dos valores das variáveis. `onStep` também recebe notificações durante execução e no término; a interface deve considerar `status` antes de destacar a linha. `step()` avança uma instrução, incluindo instruções dentro de funções. `continue()` retoma respeitando os breakpoints. Breakpoints em cabeçalhos de laços também são atingidos entre iterações.

## Compatibilidade implementada

- Palavras chave sem distinção entre maiúsculas, minúsculas e acentos; comentários `//`.
- Tipos inteiro, real, lógico e caractere; vetores multidimensionais com limites declarados e checagem de índices.
- Constantes com expressões aritméticas/textuais, usadas inclusive em limites vetoriais; atribuir a uma constante produz erro.
- Atribuição `<-` e `:=`; aritmética, comparações, operadores lógicos com curto circuito, potência, divisão inteira e módulo.
- `escreva`, `escreval` (inclusive sem parênteses para linha vazia), formatação por largura/casas decimais e `leia` assíncrono com conversão de tipos.
- `se/senao`, `para/passo`, `enquanto`, `repita/ate`, `escolha/caso/outrocaso` (incluindo intervalos `caso 5 ate 7`) e `interrompa`; `repita` também admite `fimrepita` e `ate_que`.
- Funções com `retorne`, procedimentos, recursão, parâmetros escalares e parâmetros escalares por referência.
- Funções matemáticas e de texto comuns: `abs`, `raizq`, `quad`, `int`, `sen`, `cos`, `tan`, `arctan`, `arccos`, `arcsen`, `cotan`, `exp(base,expoente)`, `grauprad`, `radpgrau`, `log`, `logn`, `pi`/`pi()`, `compr`, `maiusc`, `minusc`, `copia`, `pos`, `asc`, `carac`, `numpcarac`, `caracpnum`, `rand`/`rand()` e `randi`.

A compatibilidade é parcial e explicitamente testada, não uma reprodução integral do VisualG. Registros, arquivos, parâmetros vetoriais, comandos gráficos e opções específicas da aplicação original ainda não são suportados. `limpatela` emite sequência ANSI; `pausa` é aceita sem bloquear. Os argumentos são separados por vírgulas; grupos de parâmetros por ponto e vírgula. Texto deve permanecer em uma linha. Conteúdo depois de `fimalgoritmo` é rejeitado.

As sintaxes das declarações e dos comandos foram conferidas nos exemplos distribuídos na pasta `Exemplos` da instalação fornecida, sem incorporar esses arquivos ao projeto. Os testes usam algoritmos novos.

Execute os testes a partir da raiz com `npx tsx --test packages/core/test/*.test.ts`.

### Diretivas e geração de entrada

`dos` é aceito como diretiva de apresentação, sem emular DOS ou mudar o terminal. `aleatorio on` ativa geração numérica de entrada de 0 a 100; `aleatorio min,max[,decimais]` configura limites inclusivos e casas decimais (0 a 10). `leia` usa esses valores no lugar do callback de entrada até `aleatorio off`. Para inteiros, a geração usa valores inteiros dentro do intervalo; para reais, uma grade decimal. Intervalos inválidos e geração para variáveis de texto/lógicas produzem erro explícito. Não se reivindica equivalência do gerador com o VisualG. `InterpreterOptions.random` permite injetar um gerador que retorne valores no intervalo [0,1), facilitando testes reproduzíveis.

`eco on/off` controla a exibição dos valores gerados automaticamente (padrão ligado). A exibição da entrada humana pertence à interface hospedeira. Comandos desconhecidos continuam produzindo erro; não são descartados silenciosamente. Veja o [levantamento dos exemplos](../../docs/compatibility-survey.md) para a cobertura medida e suas limitações.
