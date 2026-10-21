# Verificação Java 21

Data: 20/10/2026. Código validado: `a32b765ac2236223e21a62cf52a731214743a049`.

A [execução de CI](https://github.com/thurdass/Algora/actions/runs/39520018378) terminou com sucesso nos runners `ubuntu-latest` e `windows-latest`. Foram usados JDK 21, JavaFX 21.0.13, ANTLR 4.13.2, RichTextFX 0.11.7, Gradle 9.7.1 e JUnit 5.11.4. A interface redesenhada usa fontes IBM Plex distribuídas localmente e ícones vetoriais nativos. Não há dependência de Node.js, Electron ou Monaco no aplicativo Java.

## Testes

| Módulo | Testes JUnit 5 |
| --- | ---: |
| Interpretador | 14 |
| CLI | 3 |
| Interface JavaFX/RichTextFX | 5 |
| Total | 22 |

Na verificação local, todos passaram, sem falhas nem testes ignorados. O build e os testes gráficos também passaram nos dois sistemas da CI.

O núcleo cobre análise ANTLR e AST, entradas tipadas, vetores e limites, controle de fluxo, constantes, recursão, escopos, funções, formatação, depuração e cancelamento de entrada bloqueada. A CLI verifica entrada redirecionada, EOF, erros e Windows-1252. Os testes JavaFX exercitam entrada e saída interativas, pausa/passo/parada, recuperação após erro, autocompletar, pontos de parada e recolhimento dos painéis em janelas compactas.

## Pacotes nativos

`jpackageInstaller` gerou `.deb` no Linux e `.exe` no Windows. `jpackageSmokeTest` iniciou a aplicação nativa em cada sistema, usando o runtime incluído, enviou entrada pelo terminal e verificou a saída e o encerramento do algoritmo. O teste terminou com `ALGORA_SMOKE_OK` nos dois ambientes.

Os instaladores e relatórios JUnit estão nos artefatos da execução de CI. O teste de execução usa a pasta `app-image` que alimenta o instalador; não automatiza o assistente de instalação nem verifica todas as distribuições Linux, configurações de fontes ou versões de Windows.

Para reproduzir em Linux com suporte gráfico virtual:

```sh
xvfb-run -a ./gradlew build :desktop:jpackageInstaller :desktop:jpackageSmokeTest
```

No Windows, com JDK 21 e WiX 3 disponíveis:

```powershell
.\gradlew.bat build :desktop:jpackageInstaller :desktop:jpackageSmokeTest
```

## Compatibilidade da linguagem

O [levantamento Java](../interpreter/compatibility-survey.md) aceitou 58 de 73 arquivos locais no parser. Foram executados casos específicos de fatorial e ordenação. Esses números não demonstram equivalência integral com o VisualG, cujo runtime não foi executado como oráculo. Os recursos ausentes continuam registrados na [matriz de compatibilidade](compatibilidade.md).

## Atualização: temas e referências de IDE

Em 20/10/2026, a suíte passou com 23 testes (14 interpretador, 3 CLI, 6 interface). Os seis temas são exercitados com aplicação de CSS, persistência e capturas. O build Linux, instalador `.deb` e execução do aplicativo empacotado passaram (`ALGORA_SMOKE_OK`). A validação Windows desta alteração será registrada pelo workflow do commit correspondente; a validação Windows citada acima pertence à revisão anterior.

O workflow [39521317453](https://github.com/thurdass/Algora/actions/runs/39521317453) confirmou a revisão `b21f652` em Linux e Windows: 23 testes e teste de execução nativa aprovados. Seus instaladores são a origem dos artefatos da release `v0.2.0`; a alteração seguinte adiciona somente logo e documentação. A release acompanha hashes SHA-256.


### Release atualizada com a logo

A release `v0.2.0` foi atualizada com os artefatos da revisão `9d0c9dd`, produzidos pelo [workflow 39522744399](https://github.com/thurdass/Algora/actions/runs/39522744399). Linux e Windows passaram na suíte e na execução do aplicativo empacotado. Os novos instaladores incluem a logo no cabeçalho e acompanham hashes SHA-256 renovados.

### Ícones nativos do aplicativo

A revisão `78168a8` configura o emblema oficial no `jpackage` e na janela JavaFX. O [workflow 39528828461](https://github.com/thurdass/Algora/actions/runs/39528828461) passou em Linux e Windows, incluindo build, 23 testes e execução empacotada. No Linux, o PNG da aplicação e o PNG extraído do `.deb` foram comparados ao recurso original e são idênticos; o arquivo `.desktop` aponta para esse ícone. O ICO Windows contém resoluções de 16 a 256 pixels. Os instaladores atualizados substituem os da release `v0.2.0` e recebem novos hashes SHA-256.
