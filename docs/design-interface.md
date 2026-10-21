# Interface do Algora

A direção visual segue as referências do IntelliJ IDEA e VS Code fornecidas pelo usuário: superfícies contínuas, barras compactas, abas e terminal inferior. A faixa lateral reúne os controles reais de arquivos e depuração. O explorador atual lista os algoritmos abertos; navegação completa por diretórios ainda não está implementada.

## Plano visual

Paleta base Grafite: editor `#1E1E1E`, painéis `#252526`, divisórias `#3C3C3C`, texto `#D4D4D4`, texto secundário `#969696`, destaque `#3794FF`. IBM Plex Sans na interface e IBM Plex Mono no editor e terminal, distribuídas com o aplicativo.

```text
Marca · Arquivo · Editar · Exibir · Ajuda
┌──┬────────────┬────────────────────────┬──────────┐
│  │Explorador  │ Ações de execução      │Depuração │
│  │Algoritmos  │ Abas                   │Variáveis │
│  │abertos     │ Editor RichTextFX      │Paradas   │
│  │            ├────────────────────────┤          │
│  │            │ Terminal / entrada     │          │
└──┴────────────┴────────────────────────┴──────────┘
Estado                                  Linha/coluna
```

Alinhamento à esquerda e divisórias discretas delimitam funções. A revisão das referências substituiu o anterior azul acinzentado por cinza neutro e trouxe os controles dos painéis para uma faixa lateral. Não há botões decorativos simulando Git, pesquisa ou outros recursos ausentes.

## Temas

Em **Exibir → Tema de cores**, selecione Grafite, Dracula, Nord, Solarizado escuro, Claro ou Solarizado claro. A mudança é imediata e a escolha é salva pelas preferências locais do Java, por usuário. Editor, sintaxe, seleção, linha atual, terminal, menus e depurador compartilham os tokens semânticos de cada paleta. Não há download de temas nem fontes.

As janelas compactas recolhem painéis; Ctrl+B alterna arquivos e Ctrl+Shift+D alterna depuração. Os testes gráficos exercitam os seis temas, persistência da seleção, execução, entrada, autocompletar, pontos de parada e layout compacto. As capturas em `desktop/build/screenshots` permitem revisar cada paleta.
