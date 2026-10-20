# Levantamento Java 21 / ANTLR 4

Data: 20/10/2026. Implementação analisada: módulo `interpreter`, Java 21.0.12.1 e ANTLR 4.13.2 no Linux. Este resultado foi medido novamente na implementação Java, sem reutilizar os números da implementação anterior em TypeScript.

## Limite da verificação

Foram lidos, sem alterações, os arquivos `.alg` recursivos da pasta `Exemplos` (extensão sem distinção de caixa) e `TESTE.alg` da raiz da instalação fornecida. Arquivos distintos não foram deduplicados. A decodificação tenta UTF-8 estrito e usa Windows-1252 quando necessário. Nenhum arquivo original foi incorporado ao repositório.

Cada arquivo passou por `AlgoraEngine.parse`: lexer e parser ANTLR, seguidos do visitor que constrói a AST independente. Isso valida sintaxe e construção da árvore, **não** valida a execução de todas as chamadas, nem equivalência com o VisualG. O primeiro erro de cada arquivo limita a classificação; corrigir um erro pode revelar outros.

**58 de 73 arquivos aceitos (79,5%); 15 rejeitados.** O parser Java rejeita `cronometro on` já na análise sintática, portanto o resultado não deve ser comparado diretamente com a aceitação permissiva de comandos pela implementação anterior.

Identidade SHA-256 do conjunto (concatenação ordenada de caminho relativo, NUL e SHA-256 binário do arquivo): `99e236290b481fa8e1bc6b4cf8aaf3706963b81b83b046442727c04057bdd788`.

| Primeiro resultado / motivo | Arquivos |
| --- | ---: |
| Aceito pelo parser Java/ANTLR | 58 |
| Registros não suportados | 5 |
| Cabeçalho com palavra chave grafada incorretamente | 2 |
| Comando timer não suportado | 1 |
| Estrutura se sem fechamento aceito (linha 84) | 1 |
| Palavra TO em laço para (linha 37) | 1 |
| Tokens após fimalgoritmo | 1 |
| Funções sem programa principal | 1 |
| Limite superior ausente no para | 1 |
| Comando cronometro on não suportado | 1 |
| Tipo numerico não suportado | 1 |

## Execuções pontuais na implementação Java

| Arquivo original local | Entrada / verificação | Resultado |
| --- | --- | --- |
| `Exemplos/fatorial.alg` | Entrada humana simulada `5`; saída inspecionada | Concluído em 13 passos; fatorial apresentado: 120 |
| `Exemplos/inssort.alg` | Entrada numérica aleatória; vetor final verificado | Concluído em 604 passos; vetor em ordem crescente |
| `Exemplos/bbsort.alg` | Entrada numérica aleatória; vetor final verificado | Concluído em 301 passos; vetor em ordem crescente |
| `Exemplos/interrompa.alg` | Caminho padrão sem entrada | Concluído em 75 passos |

Os testes aleatórios usaram gerador determinístico com semente 73, multiplicador 16807 e módulo 2147483647. Nenhuma dessas execuções comparou automaticamente a saída com um processo VisualG; os resultados estabelecem comportamento do Algora nesses caminhos. O algoritmo original não foi copiado.

## Reprodução

Após `./gradlew :interpreter:classes`, execute a ferramenta Java incluindo as classes compiladas e o JAR de `antlr4-runtime:4.13.2` no classpath:

```sh
java --class-path "interpreter/build/classes/java/main:/caminho/antlr4-runtime-4.13.2.jar" \
  interpreter/tools/CompatibilitySurvey.java /caminho/visualg3.0.7
```

No Windows, o separador de classpath é `;`. A ferramenta apenas lê os arquivos durante o levantamento de parser; depois executa os quatro smoke tests declarados acima, com limite padrão de instruções. Ela não modifica arquivos nem inicia o VisualG.

## Inventário

| Arquivo relativo | Resultado |
| --- | --- |
| `Exemplos/CALCULOMEDIA2.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/CALENDARIO.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/CARACOL.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/CHECA_CPF.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/Calculo_media2.alg` | Cabeçalho com palavra chave grafada incorretamente |
| `Exemplos/Calculo_media2.alg.ALG` | Cabeçalho com palavra chave grafada incorretamente |
| `Exemplos/Caracol2.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/Cronometro.alg` | Comando timer não suportado |
| `Exemplos/DESTAQUES.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/ELEMENTO_OCUPADO.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/ENCRYPT.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/ESCOLA1.alg` | Estrutura se sem fechamento aceito (linha 84) |
| `Exemplos/EXEMPLO1.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/EXEMPLO1.alg.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/EXTENSO4.ALG` | Palavra TO em laço para (linha 37) |
| `Exemplos/Jogo_velha.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/MEDIA_REGISTRO.ALG` | Registros não suportados |
| `Exemplos/MEDIA_SIMPLES.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/MEDIA_VETOR.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/MENU_COM_CASE.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/MENU_PRINCIPAL.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/MODULO.ALG` | Tokens após fimalgoritmo |
| `Exemplos/Nome_inverso.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/Numeros_primos.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/PERFEITOS.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/PRIMOS.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/REGISTROS com VETORES.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/REGISTROS.ALG` | Registros não suportados |
| `Exemplos/RELACIONAR.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/SEMNOME.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/TABOADA.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/TESTE.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/Tabela_ASCII4.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/Troca de Valores.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/UDF.ALG` | Funções sem programa principal |
| `Exemplos/ajustes.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/aleatorio1.alg` | Limite superior ausente no para |
| `Exemplos/bbsort.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/bbsortp.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/bbsortreg.alg` | Registros não suportados |
| `Exemplos/bin2dec.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/bubblrec.alg` | Registros não suportados |
| `Exemplos/buscabin.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/buscaseq.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/buscaseqreg.alg` | Registros não suportados |
| `Exemplos/buscbinr.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/caracfun.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/cntsort.alg` | Comando cronometro on não suportado |
| `Exemplos/combin.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/decomp.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/decpoutras.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/decpoutrasproc.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/dectobin.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/dectohex.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/escolha.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/estcivil.alg` | Tipo numerico não suportado |
| `Exemplos/fatorial.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/fatorial2.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/graus.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/inssort.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/interrompa.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/media_aluno.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/mediaar.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/menorde3.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/passo.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/randomicos.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/randomicos.alg.ALG` | Aceito pelo parser Java/ANTLR |
| `Exemplos/rqpaprox.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/taxaspop.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/times.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/troca.alg` | Aceito pelo parser Java/ANTLR |
| `Exemplos/vetr2dim.alg` | Aceito pelo parser Java/ANTLR |
| `TESTE.alg` | Aceito pelo parser Java/ANTLR |
