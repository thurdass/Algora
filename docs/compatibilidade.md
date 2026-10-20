# Compatibilidade com arquivos .alg

O formato aceito é o dialeto Portugol do VisualG, com cobertura parcial. A extensão `.alg` por si só não garante execução: outros ambientes Portugol usam sintaxes diferentes. Os testes automatizados usam algoritmos originais e não substituem uma comparação de execução com o VisualG.

| Recurso | Implementação |
| --- | --- |
| Estrutura | `algoritmo`, declarações opcionais com `var`, `inicio`, `fimalgoritmo` |
| Tipos | `inteiro`, `real`, `logico`, `caractere` e aliases `caracter`/`literal` |
| Vetores | Uma ou mais dimensões, limites declarados e validação de índices |
| Constantes | Expressões aritméticas e textuais, inclusive em limites vetoriais; não podem receber atribuição |
| Atribuição | `<-` e `:=`; conversão para o tipo declarado |
| Expressões | Aritmética, potência, comparações, divisão inteira, módulo, `e`, `ou`, `xou`, `nao` |
| Entrada e saída | `leia`, `escreva`, `escreval` (também sem parênteses para linha vazia), largura e casas decimais |
| Controle | `se/senao`, `para/passo`, `enquanto`, `repita/ate` (também `fimrepita` e `ate_que`), `escolha` com intervalos de casos, `interrompa` em laços |
| Rotinas | Funções, procedimentos, recursão e parâmetros escalares por valor ou referência |
| Texto | `compr`, `maiusc`, `minusc`, `copia`, `pos`, `asc`, `carac`, `numpcarac`, `caracpnum` |
| Matemática | `abs`, `raizq`, `quad`, `int`, `sen`, `cos`, `tan`, `arctan`, `arccos`, `arcsen`, `cotan`, `exp(base,expoente)`, `grauprad`, `radpgrau`, `log`, `logn`, `pi`/`pi()` |
| Aleatoriedade | `rand`/`rand()` e `randi(n)`; gerador pode ser injetado para testes |
| Depuração | Breakpoints por linha, pausa, passo e snapshots de variáveis |

Palavras e identificadores são normalizados sem distinguir caixa ou acentos. Comentários começam com `//`. Argumentos são separados por vírgulas e grupos de parâmetros por ponto e vírgula. Textos entre aspas devem estar em uma única linha. Conteúdo depois de `fimalgoritmo` é rejeitado.

Não há suporte a registros, arquivos, parâmetros vetoriais, comandos gráficos ou às opções particulares do aplicativo original. `limpatela` emite uma sequência ANSI; sua apresentação depende do consumidor da saída. `pausa` é aceita sem bloquear. As funções numéricas usam números JavaScript; limites, arredondamento e resultados em casos extremos podem diferir do VisualG. Não há promessa de equivalência de mensagens de erro, formatação completa ou semântica em todos os programas existentes.

Arquivos novos são UTF-8. Consulte a implementação dos adaptadores de arquivos para os encodings legados aceitos ao importar; não se deve presumir detecção universal de codificação. A validação funcional real em Windows permanece pendente de execução dos testes e do aplicativo nesse sistema.

`dos` é aceito como diretiva de apresentação. `aleatorio on` gera entradas numéricas de 0 a 100; `aleatorio min,max[,decimais]` configura limites inclusivos e até 10 casas decimais. `aleatorio off` restaura a entrada humana. A geração para texto ou lógico produz erro. `eco on/off` controla a exibição da entrada automática; a interface hospedeira controla o eco da entrada humana. O gerador não pretende reproduzir a sequência do VisualG.

Consulte o [levantamento de exemplos](compatibility-survey.md) para cobertura medida de análise sintática. Um arquivo aceito pelo parser ainda pode falhar em execução; esses números não medem equivalência de resultados com o original.

As assinaturas matemáticas e a ordem dos argumentos textuais foram conferidas na documentação local fornecida (`help/funcoes.htm`, identificada como VisualG 2.0). `exp(base,expoente)` calcula potência; `logn(x)` fornece o logaritmo natural. `pi` e `rand` aceitam uso sem parênteses quando não há variável declarada com esse nome. Pi usa `Math.PI`; os resultados numéricos seguem precisão JavaScript, sem assumir que o número de casas mostrado na documentação define a precisão interna do VisualG. `pos(subtexto,texto)` retorna posição a partir de 1, ou zero quando ausente; `copia(texto,inicio,quantidade)` também começa em 1. As funções predefinidas verificam a quantidade de argumentos. `randi(n)` exige inteiro positivo; resultados fora do domínio matemático produzem erro. Os testes confirmam essas operações no Algora; não representam comparação de execução binária com o VisualG.
