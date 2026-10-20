# Algora

IDE moderna para aprender e executar Portugol em arquivos `.alg`, com interpretador próprio e independente do VisualG. Projeto em desenvolvimento: a compatibilidade é parcial e está documentada em [compatibilidade](docs/compatibilidade.md).

O editor Monaco oferece realce de sintaxe, autocompletar, trechos de código e abas. O terminal integrado recebe a entrada de `leia` e mostra a saída do algoritmo. O depurador permite pontos de parada, pausa, execução passo a passo e inspeção de variáveis. O aplicativo desktop usa Electron e React; o interpretador não depende da interface.

![Interface do Algora](docs/assets/algora.png)

## Executar

Requer Node.js 24 ou superior e npm. No Linux, o aplicativo Electron também requer as bibliotecas gráficas e suporte ao sandbox do Chromium. Em ambientes com namespaces restritos, use o `.deb` ou `npm run dev:web`; veja [instalação](docs/instalacao.md).

```bash
npm ci
npm run dev
```

Para usar a interface no navegador:

```bash
npm run dev:web
```

No desktop, abra e salve arquivos com `Ctrl+O` e `Ctrl+S`. Execute com `F5`, alterne pontos de parada com `F9` ou clicando na margem do editor e avance com `F10`. A versão web usa os recursos de arquivos do navegador; não recebe acesso irrestrito ao disco. O terminal é a entrada e saída do algoritmo, sem executar comandos do sistema.

Também é possível executar sem interface:

```bash
npm run cli -- examples/ola.alg
npm run cli -- examples/entrada.alg
npm run cli -- examples/fatorial.alg
npm run cli -- examples/vetores.alg
```

## Verificação e distribuição

```bash
npm run check
npm run test:e2e
npm run test:desktop
npm run dist:linux
npm run dist:win
```

`check` verifica tipos, executa os testes do interpretador e gera a interface. Os comandos de distribuição geram AppImage/deb para Linux e instalador NSIS para Windows em `release/`. A compilação do instalador Windows a partir de Linux pode exigir Wine; prefira o runner Windows da integração contínua. A execução real em Windows ainda depende de validação nesse sistema; gerar um instalador não demonstra equivalência de comportamento entre plataformas.

## Estrutura

- `packages/core`: analisador léxico, parser, interpretador e API do depurador em TypeScript.
- `packages/cli`: execução de arquivos pelo terminal.
- `apps/desktop`: editor React/Monaco e integração Electron com arquivos locais.
- `examples`: algoritmos originais para experimentar.
- `docs`: arquitetura, compatibilidade, API e investigação do executável fornecido.

O levantamento local aceita sintaticamente **59 de 73** arquivos distribuídos com o VisualG (80,8%). Isso não garante execução equivalente; veja o [levantamento reproduzível](docs/compatibility-survey.md) e os recursos ainda ausentes.

Consulte [arquitetura](docs/arquitetura.md), [API](docs/api.md) e [relatório REA](docs/engenharia-reversa.md). O código foi implementado de forma independente; o executável e os recursos proprietários do VisualG não são distribuídos. A investigação estática orienta requisitos e não comprova compatibilidade integral.

Repositório: [thurdass/Algora](https://github.com/thurdass/Algora) (privado; exige acesso autorizado). Use mensagens [Conventional Commits](https://www.conventionalcommits.org/) nas contribuições.
