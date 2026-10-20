# Compatibilidade com arquivos .alg

O Algora implementa um subconjunto do dialeto Portugol do VisualG com um interpretador próprio em Java 21 e parser ANTLR4. Arquivos `.alg` de outros ambientes podem usar sintaxes diferentes. A extensão não garante compatibilidade.

A cobertura deve ser avaliada em três níveis: aceitação pelo parser, execução sem erro e equivalência de saída com o original. Um arquivo sintaticamente aceito ainda pode referenciar recursos não implementados. Testes de algoritmos originais comprovam os comportamentos exercitados, sem estabelecer equivalência para todos os programas existentes.

## Limites da validação

A investigação REA do executável original foi estática. Não houve comparação automatizada da execução com o VisualG. Mensagens de erro, coerções, arredondamento, precedência em casos extremos e formatação completa exigem verificações específicas.

O [levantamento do protótipo TypeScript](historico/levantamento-typescript.md) registra 59 de 73 arquivos aceitos pelo parser daquela implementação. Esse número é histórico e não é uma medição do parser Java. As execuções pontuais descritas nesse documento também pertencem ao protótipo anterior.

Registros, acesso a arquivos, gráficos e opções específicas da aplicação original devem ser tratados como recursos sem garantia de suporte até sua implementação e validação no núcleo Java. Consulte os testes JUnit do módulo `interpreter` para os casos efetivamente exercitados.

Linux e Windows utilizam o mesmo núcleo, com bibliotecas JavaFX e instaladores específicos. Uma compilação bem-sucedida não substitui testes gráficos, de arquivos, entrada e instalação em cada sistema.
