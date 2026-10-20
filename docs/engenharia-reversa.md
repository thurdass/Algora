# Investigação estática do VisualG com REA

## Alvo e método

Foi analisado o arquivo fornecido pelo usuário em `/home/thurdas/Downloads/visualg3.0.7/visualg30.exe`, identificado pelo SHA-256 `7b297fb338e2ca7004ed55b42aef215c17a1a1d44b27cff50b6dc9be6dd3becf`. A identidade importa: o nome da pasta não estabelece sozinho a versão interna do executável.

O [bundle de evidências](evidence/visualg-rea.json) conserva 12 registros REA, incluindo visão geral, busca de strings, referências, resolução de procedimentos, análise de função e inventário de recursos PE. A análise nativa foi realizada com Hopper; recursos foram inspecionados pelo parser PE do REA. Nenhuma execução do VisualG original foi observada. Não foram copiados o executável, ícones ou outros recursos proprietários para o aplicativo Algora.

## Registro de conclusões

Os identificadores abaixo apontam para registros completos no bundle; as conclusões devem ser lidas com as limitações registradas em cada operação.

| Pergunta | Resultado e natureza da evidência | Referência |
| --- | --- | --- |
| Qual é o tipo do alvo? | Observação estática: executável PE x86 com inventário de procedimentos e strings | `ev_481c97bba060dae9f0a032cda7bcc4088daa84f483f0475c2b48b744ede2993a` |
| Há vocabulário relacionado à linguagem? | Observação: buscas estáticas retornam strings de Portugol; uma string isolada não comprova que um comando é executável | `ev_0ea0692e87a08f4d8502400b1f5cdfd3d66e6aa23118ce18449ba28bab35956e`, `ev_d52a05dbe45cb41076326b6266491df784e2be7880ad078ac5824dde86f931a3` |
| Foi investigado código relacionado a esse vocabulário? | Observação: a referência de `ESCREVAL` em `0x4c15fc` aponta para `0x4c05e0`, dentro de `sub_4c0120`. A análise revela atribuições associadas a palavras de Portugol e Pascal. Inferência: inicialização de tabela de palavras/tradução; não demonstra todo o parser ou runtime | Registros `xrefs` do bundle e `ev_a03d07f3c63bd3df8a4335c41b4190bd2a33e3389b5546ed06a9606aed30e208` |
| Que recursos acompanham o executável? | Observação: árvore estática de recursos PE; o inventário não foi usado para copiar aparência ou recursos do produto | `ev_1a06800a2f310cce87290b93b570f5c7dc1064d4fb87d0704b12c71c5ef4d021` |
| O Algora reproduz exatamente o VisualG? | Não estabelecido: não houve comparação dinâmica dos dois programas | Sem evidência de runtime |

A investigação respondeu à identidade do artefato e encontrou evidência de vocabulário e de uma rotina relacionada a tabelas. A reconstrução completa do fluxo de interpretação permanece parcial. Resultados de buscas não devem ser tratados como inventário exaustivo da linguagem, e os nomes automáticos de procedimentos não são nomes originais de código fonte.

## Aplicação no Algora

O interpretador é implementação original em TypeScript: tokenização, análise sintática, avaliação de AST, escopos e checkpoints do depurador. A arquitetura Electron/React/Monaco é uma escolha de projeto para fornecer uma IDE atual em Linux e Windows; não é uma característica inferida do VisualG.

A sintaxe também foi conferida nos exemplos distribuídos na instalação local, sem incorporá-los ao repositório. Os exemplos e testes do Algora são novos. A [matriz de compatibilidade](compatibilidade.md) descreve o que o código atual implementa e o que está ausente.

## Limites e questões abertas

- A equivalência de resultados, erros, coerções, precedência e formatação em casos extremos exige comparação de execução com o original.
- A cobertura de comandos especiais, arquivos, gráficos e tipos adicionais não está completa.
- As evidências estáticas não comprovam a aparência ou a interação do programa em execução.
- A validação real do aplicativo e dos instaladores no Windows depende da CI e de testes nesse sistema.

A análise registrada orienta a implementação, mas não justifica anunciar compatibilidade integral nem a recuperação do código fonte original.
