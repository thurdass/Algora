algoritmo "Quadrados em um vetor"
var
   valores: vetor [1..5] de inteiro
   i, soma: inteiro
inicio
   soma <- 0
   para i de 1 ate 5 faca
      valores[i] <- i ^ 2
      soma <- soma + valores[i]
      escreval(i, " ao quadrado = ", valores[i])
   fimpara
   escreval("Soma = ", soma)
fimalgoritmo
