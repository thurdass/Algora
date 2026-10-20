algoritmo "Fatorial recursivo"
funcao fatorial(n: inteiro): inteiro
inicio
   se n <= 1 entao
      retorne 1
   senao
      retorne n * fatorial(n - 1)
   fimse
fimfuncao
inicio
   escreval("5! = ", fatorial(5))
fimalgoritmo
