# Levantamento de compatibilidade dos exemplos fornecidos

Data: 20/10/2026. Ambiente: Linux, Node.js 24.19.0, TypeScript via tsx. O conjunto analisado contém todos os arquivos com extensão `.alg` (sem distinção de caixa) encontrados recursivamente em `Exemplos`, mais o `TESTE.alg` da raiz da instalação fornecida. Arquivos com nomes como `.alg.ALG` contam como arquivos separados; não houve deduplicação. Nenhum exemplo original foi redistribuído.

## Método e resultado

Cada arquivo foi lido sem modificações. A decodificação tenta UTF-8 estrito, com fallback para Windows-1252. Foi executado apenas `parse(source)`, sem executar o programa nem verificar semanticamente chamadas, nomes e tipos. Um arquivo aceito pode chamar comandos/funções que falharão na execução. O levantamento registra apenas o primeiro erro de cada arquivo; corrigir esse erro pode revelar outros.

- Baseline anterior ao suporte a `escreval` sem parênteses (commit `a9cfca8`): **41 de 73** arquivos aceitos (56,2%); 32 rejeitados.
- Após adicionar e testar essa forma de linha vazia: **47 de 73** arquivos aceitos (64,4%); 26 rejeitados.
- Esses números medem aceitação sintática deste conjunto local, não compatibilidade integral com VisualG.

Identidade do conjunto (SHA-256 dos pares ordenados de caminho relativo, NUL e digest binário de cada arquivo): `99e236290b481fa8e1bc6b4cf8aaf3706963b81b83b046442727c04057bdd788`.

| Resultado / primeiro motivo | Arquivos |
| --- | ---: |
| Aceitos pelo parser | 47 |
| Seções de declaração não suportadas (`dos`, `const`, `tipo`) | 11 |
| Comando `aleatorio` não suportado | 3 |
| Cabeçalho com palavra chave grafada incorretamente | 2 |
| Comando `timer` não suportado | 1 |
| Declarações locais em contexto não suportado | 1 |
| Literal de texto sem fechamento | 1 |
| Tokens extras após `fimalgoritmo` | 1 |
| Arquivo de funções sem programa principal | 1 |
| Limite superior ausente no laço `para` | 1 |
| Intervalos de valores em `caso` não suportados | 1 |
| Tipo `numerico` não suportado | 1 |
| Estrutura `repita` sem `ate` reconhecido | 1 |
| Comando `ate_que` não suportado | 1 |

## Execução pontual

Foi executado localmente o arquivo original `Exemplos/fatorial.alg`, sem copiá-lo para o projeto. Entrada fornecida: `5`, solicitada uma vez. Resultado: execução concluída, 13 instruções, valor apresentado para o fatorial: **120**. Essa verificação cobre somente esse caminho de entrada nesse arquivo. A saída do Algora foi verificada; não houve comparação automatizada com a execução no aplicativo VisualG.

## Reprodução

Com uma instalação local contendo os mesmos arquivos:

```sh
npx tsx packages/core/scripts/compatibility-survey.ts /caminho/visualg3.0.7
```

O script escreve JSON com nome relativo, aceitação e primeiro diagnóstico. Não altera nem executa os arquivos. O diretório precisa conter `Exemplos` e `TESTE.alg`.

## Inventário final

| Arquivo relativo | Parser |
| --- | --- |
| `Exemplos/CALCULOMEDIA2.ALG` | Aceito |
| `Exemplos/CALENDARIO.ALG` | Aceito |
| `Exemplos/CARACOL.ALG` | Aceito |
| `Exemplos/CHECA_CPF.alg` | Aceito |
| `Exemplos/Calculo_media2.alg` | Cabeçalho com palavra chave grafada incorretamente |
| `Exemplos/Calculo_media2.alg.ALG` | Cabeçalho com palavra chave grafada incorretamente |
| `Exemplos/Caracol2.ALG` | Aceito |
| `Exemplos/Cronometro.alg` | Comando `timer` não suportado |
| `Exemplos/DESTAQUES.ALG` | Aceito |
| `Exemplos/ELEMENTO_OCUPADO.alg` | Aceito |
| `Exemplos/ENCRYPT.ALG` | Aceito |
| `Exemplos/ESCOLA1.alg` | Declarações locais em contexto não suportado |
| `Exemplos/EXEMPLO1.alg` | Aceito |
| `Exemplos/EXEMPLO1.alg.ALG` | Aceito |
| `Exemplos/EXTENSO4.ALG` | Literal de texto sem fechamento |
| `Exemplos/Jogo_velha.alg` | Comando `aleatorio` não suportado |
| `Exemplos/MEDIA_REGISTRO.ALG` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/MEDIA_SIMPLES.ALG` | Aceito |
| `Exemplos/MEDIA_VETOR.ALG` | Aceito |
| `Exemplos/MENU_COM_CASE.alg` | Aceito |
| `Exemplos/MENU_PRINCIPAL.alg` | Aceito |
| `Exemplos/MODULO.ALG` | Tokens extras após `fimalgoritmo` |
| `Exemplos/Nome_inverso.alg` | Aceito |
| `Exemplos/Numeros_primos.alg` | Aceito |
| `Exemplos/PERFEITOS.ALG` | Aceito |
| `Exemplos/PRIMOS.ALG` | Aceito |
| `Exemplos/REGISTROS com VETORES.ALG` | Aceito |
| `Exemplos/REGISTROS.ALG` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/RELACIONAR.ALG` | Aceito |
| `Exemplos/SEMNOME.ALG` | Aceito |
| `Exemplos/TABOADA.ALG` | Aceito |
| `Exemplos/TESTE.ALG` | Aceito |
| `Exemplos/Tabela_ASCII4.alg` | Aceito |
| `Exemplos/Troca de Valores.alg` | Aceito |
| `Exemplos/UDF.ALG` | Arquivo de funções sem programa principal |
| `Exemplos/ajustes.alg` | Aceito |
| `Exemplos/aleatorio1.alg` | Limite superior ausente no laço `para` |
| `Exemplos/bbsort.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/bbsortp.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/bbsortreg.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/bin2dec.alg` | Aceito |
| `Exemplos/bubblrec.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/buscabin.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/buscaseq.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/buscaseqreg.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/buscbinr.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/caracfun.alg` | Aceito |
| `Exemplos/cntsort.alg` | Comando `aleatorio` não suportado |
| `Exemplos/combin.alg` | Aceito |
| `Exemplos/decomp.alg` | Aceito |
| `Exemplos/decpoutras.alg` | Aceito |
| `Exemplos/decpoutrasproc.alg` | Seções de declaração não suportadas (`dos`, `const`, `tipo`) |
| `Exemplos/dectobin.alg` | Aceito |
| `Exemplos/dectohex.alg` | Aceito |
| `Exemplos/escolha.alg` | Intervalos de valores em `caso` não suportados |
| `Exemplos/estcivil.alg` | Tipo `numerico` não suportado |
| `Exemplos/fatorial.alg` | Aceito |
| `Exemplos/fatorial2.alg` | Aceito |
| `Exemplos/graus.alg` | Aceito |
| `Exemplos/inssort.alg` | Comando `aleatorio` não suportado |
| `Exemplos/interrompa.alg` | Estrutura `repita` sem `ate` reconhecido |
| `Exemplos/media_aluno.alg` | Aceito |
| `Exemplos/mediaar.alg` | Aceito |
| `Exemplos/menorde3.alg` | Aceito |
| `Exemplos/passo.alg` | Aceito |
| `Exemplos/randomicos.alg` | Aceito |
| `Exemplos/randomicos.alg.ALG` | Aceito |
| `Exemplos/rqpaprox.alg` | Aceito |
| `Exemplos/taxaspop.alg` | Aceito |
| `Exemplos/times.alg` | Aceito |
| `Exemplos/troca.alg` | Aceito |
| `Exemplos/vetr2dim.alg` | Aceito |
| `TESTE.alg` | Comando `ate_que` não suportado |
