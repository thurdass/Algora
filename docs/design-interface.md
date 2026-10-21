# Direção visual da interface

A interface do Algora é uma bancada de programação: o conteúdo principal é o algoritmo, e a inspeção acompanha a execução. A stack permanece JavaFX 21 e RichTextFX.

## Tokens

| Papel | Cor |
| --- | --- |
| Editor / grafite | `#171C24` |
| Painéis / ardósia | `#202733` |
| Divisores | `#303A49` |
| Texto principal | `#E1E6ED` |
| Texto secundário | `#94A0B1` |
| Ação e seleção / azul técnico | `#85ADD6` |

IBM Plex Sans nos controles e IBM Plex Mono no código e terminal. As fontes são distribuídas localmente com sua licença OFL; a interface não depende de fontes instaladas ou de rede. Escala de 11, 12, 13, 14 e 18 px. Texto corrente alinhado à esquerda, rótulos em caixa de frase, foco de teclado visível.

## Layout e revisão antes da implementação

Foram comparadas duas estruturas:

```text
A: ferramentas duplicadas em uma barra lateral
[marca e menus                                      ]
[ícones][explorador][editor             ][inspeção    ]
                  [terminal           ]

B: bancada com painéis recolhíveis (escolhida)
[algora | menus                 arquivo | painéis   ]
[Arquivos  ][aba do arquivo / ações     ][Depuração  ]
[          ][código                    ][variáveis  ]
[          ][                          ][paradas    ]
[          ][Terminal / estado         ][           ]
[estado de execução                     linha/coluna]
```

A revisão eliminou a barra de ícones duplicados, avatar sem função, slogans, cartões de dicas e rótulos em caixa alta. O azul identifica interação e o ponto de execução; a identidade vem da composição e da tipografia. Separadores indicam limites funcionais, com superfícies contínuas e sem sombras decorativas. Os ícones são vetores com traço consistente.

Os painéis laterais podem ser recolhidos. Em janelas compactas, a inspeção e depois os arquivos cedem espaço ao código. O terminal continua redimensionável. Não há animação automática. Estados vazios orientam a ação: executar, depurar ou adicionar um ponto de parada.

## Verificação visual

A revisão usa capturas reais do JavaFX em tamanho normal e compacto, além dos testes de entrada, execução, depuração e autocompletar. Os testes de composição verificam recolhimento dos painéis e tamanho útil do editor.
