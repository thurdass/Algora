# Compatibilidade com arquivos .alg

O Algora implementa um subconjunto do dialeto Portugol do VisualG com interpretador próprio em Java 21 e parser ANTLR4. Arquivos `.alg` de outros ambientes podem usar sintaxes diferentes. A extensão não garante compatibilidade.

## Recursos implementados

A tabela descreve a gramática e o runtime Java atuais. Aceitação sintática e equivalência com o original são avaliações distintas.

| Recurso | Cobertura |
| --- | --- |
| Estrutura | `algoritmo "Nome"`, declarações, rotinas, `inicio` e `fimalgoritmo`; conteúdo extra no final é rejeitado |
| Identificadores | Sem distinção de caixa; normalização de acentos fora de textos e comentários |
| Comentários e texto | Comentários `//`, textos de uma linha entre aspas simples ou duplas; aspas duplas duplicadas dentro do texto |
| Tipos | `inteiro`, `real`, `logico`, `caractere` e aliases `caracter`/`literal` |
| Constantes | Declarações `const`, expressões e uso em limites vetoriais; atribuição posterior é rejeitada |
| Vetores | Múltiplas dimensões, limites inteiros declarados e validação de índices |
| Atribuição | `<-` e `:=`, com conversão para o tipo declarado |
| Expressões | Aritmética, potência, comparações, divisão inteira `div`/`\`, módulo `mod`/`%`, `e`, `ou`, `xou`, `nao`; curto circuito em `e` e `ou` |
| Condicionais | `se/entao/senao/fimse`, `escolha/caso/outrocaso/fimescolha`, inclusive intervalos `caso 5 ate 7` |
| Laços | `para/passo`, `enquanto`, `repita/ate`; formas `ate_que` e `fimrepita`; `interrompa` em laços |
| Rotinas | Funções, procedimentos, recursão, parâmetros escalares por valor ou referência e `retorne` |
| Entrada e saída | `leia`, `escreva`, `escreval`, largura e casas decimais; `escreval` sem parênteses produz linha vazia |
| Matemática | `abs`, `raizq`, `quad`, `int`, `sen`, `cos`, `tan`, `arctan`, `arccos`, `arcsen`, `cotan`, `grauprad`, `radpgrau`, `exp(base, expoente)`, `log`, `logn`, `pi` e `pi()` |
| Texto e conversão | `compr`, `maiusc`, `minusc`, `copia`, `pos`, `asc`, `carac`, `numpcarac`, `caracpnum` |
| Aleatoriedade | `rand`, `rand()`, `randi(n)`, `aleatorio on/off` e intervalos; fonte injetável para testes |
| Apresentação | `eco on/off` para entrada automática; `dos` e `pausa` aceitos sem bloquear; `limpatela` emite ANSI |
| Depuração | Pausa antes da instrução, pontos por linha, continuar, passo, parar e cópias das variáveis visíveis |

`inteiro` trunca valores em direção a zero; números são representados internamente por `double`. Condições exigem valores lógicos. Isso não oferece inteiros arbitrariamente grandes, nem comprova equivalência de coerções e arredondamento com o VisualG. Funções trigonométricas usam radianos; as conversões têm funções próprias. `exp` recebe base e expoente. `copia` e `pos` usam posições a partir de 1.

`aleatorio on` gera entradas numéricas de 0 a 100. `aleatorio min,max[,decimais]` configura limites inclusivos e até 10 casas decimais; `off` restaura a entrada humana. Inteiros são sorteados entre valores inteiros representáveis do intervalo; reais usam a grade decimal configurada. A geração para texto ou lógico é rejeitada. Não há promessa de reproduzir a sequência aleatória do VisualG. `eco` não controla o eco da entrada humana, que pertence ao adaptador.

Argumentos usam vírgulas e grupos de parâmetros usam ponto e vírgula. A IDE importa UTF-8 com fallback Windows-1252 e salva em UTF-8. O núcleo recebe uma `String`: a decodificação de arquivos pertence ao host.

## Limitações e evidências

Não há suporte a registros, parâmetros vetoriais, acesso a arquivos, comandos gráficos ou opções específicas da aplicação original como `timer`. Comandos ou funções desconhecidos produzem erro na análise ou execução. Não são ignorados silenciosamente. O núcleo emite ANSI em `limpatela`; sua apresentação depende do consumidor. A IDE trata a limpeza do próprio terminal.

A cobertura deve ser avaliada em três níveis: aceitação pelo parser, execução sem erro e equivalência de saída com o original. Um arquivo sintaticamente aceito ainda pode referenciar recursos não implementados. Os testes JUnit exercitam algoritmos originais e os comportamentos esperados do Algora.

A investigação REA foi estática. Não houve comparação automatizada da execução com o VisualG. Mensagens de erro, formatação completa e casos extremos exigem verificações específicas. O [levantamento do protótipo TypeScript](historico/levantamento-typescript.md), incluindo seus 59 de 73 arquivos aceitos, é histórico e não mede a implementação Java.

Linux e Windows utilizam o mesmo núcleo, com bibliotecas JavaFX e instaladores específicos. Uma compilação bem-sucedida não substitui testes gráficos, de entrada, arquivos e instalação em cada sistema.
