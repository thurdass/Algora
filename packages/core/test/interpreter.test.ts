import { test } from "node:test";
import assert from "node:assert/strict";
import {
  execute,
  createInterpreter,
  parse,
  AlgoraError,
} from "../src/index.ts";
const program = (declarations: string, body: string) =>
  `algoritmo "teste"\nvar\n${declarations}\ninicio\n${body}\nfimalgoritmo`;
test("typed input, precedence, format and Portuguese case insensitive keywords", async () => {
  let output = "";
  const s = await execute(
    program(
      "n: inteiro\nr:real",
      'Leia(n)\nr <- n * 2 + 1\nEscreval("resultado: ",r:0:2)',
    ),
    { input: async () => "4", output: (t) => (output += t) },
  );
  assert.equal(output, "resultado: 9.00\n");
  assert.equal(s.variables.r, 9);
});
test("arrays, nested control flow and descending loops", async () => {
  const s = await execute(
    program(
      "v: vetor[-1..1] de inteiro\ni,s:inteiro",
      "s <- 0\npara i de 1 ate -1 passo -1 faca\nv[i] <- i * i\ns <- s + v[i]\nfimpara\nenquanto s < 4 faca\ns <- s + 1\nfimenquanto\nrepita\ns <- s - 1\nate s = 2\nse s = 2 entao\ns <- s + 10\nsenao\ns <- 0\nfimse",
    ),
  );
  assert.equal(s.variables.s, 12);
  assert.deepEqual(s.variables.v, [1, 0, 1]);
});
test("functions, recursion and procedures by reference", async () => {
  const source = `algoritmo "rotinas"
var
x: inteiro
funcao fat(n: inteiro): inteiro
inicio
se n <= 1 entao
retorne 1
fimse
retorne n * fat(n-1)
fimfuncao
procedimento dobro(var n: inteiro)
inicio
n <- n * 2
fimprocedimento
inicio
x <- fat(5)
dobro(x)
fimalgoritmo`;
  assert.equal((await execute(source)).variables.x, 240);
});
test("choice and short circuit", async () => {
  const s = await execute(
    program(
      "n:inteiro",
      "n <- 2\nescolha n\ncaso 1\nn <- 9\ncaso 2,3\nn <- 8\noutrocaso\nn <- 0\nfimescolha\nse falso e 1/0 = 0 entao\nn <- 1\nfimse",
    ),
  );
  assert.equal(s.variables.n, 8);
});
test("runtime rejects invalid memory and infinite loops", async () => {
  await assert.rejects(
    execute(program("v:vetor[1..2] de inteiro", "v[0] <- 9")),
    /Índice fora/,
  );
  await assert.rejects(
    execute(program("", "enquanto verdadeiro faca\nfimenquanto"), {
      maxSteps: 10,
    }),
    /Limite de instruções/,
  );
  await assert.rejects(
    execute(program("x:inteiro", "x <- 1/0")),
    /Divisão por zero/,
  );
});
test("debugger pauses, steps and resumes with inspectable state", async () => {
  let interpreter: ReturnType<typeof createInterpreter>;
  const paused: number[] = [];
  interpreter = createInterpreter(
    program("x:inteiro", "x <- 1\nx <- 2\nx <- 3"),
    {
      onStep: (s) => {
        if (s.status === "paused") {
          paused.push(s.line);
          assert.equal(s.variables.x, paused.length === 1 ? 0 : 1);
          queueMicrotask(() =>
            paused.length === 1 ? interpreter.step() : interpreter.continue(),
          );
        }
      },
    },
  );
  interpreter.pause();
  const result = await interpreter.run();
  assert.equal(result.variables.x, 3);
  assert.equal(paused.length, 2);
});
test("stop while paused and abort signal cancel promptly", async () => {
  const interpreter = createInterpreter(program("x:inteiro", "x <- 1"), {
    onStep: (s) => {
      if (s.status === "paused") interpreter.stop();
    },
  });
  interpreter.pause();
  await assert.rejects(interpreter.run(), { name: "AbortError" });
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(
    execute(program("", ""), { signal: controller.signal }),
    { name: "AbortError" },
  );
});
test("malformed syntax and unsupported commands produce source diagnostics", () => {
  assert.throws(
    () => parse(program("x:inteiro", 'x <- "unfinished')),
    AlgoraError,
  );
  assert.throws(
    () => parse(program("x:inteiro", "se verdadeiro\nx <- 1\nfimse")),
    /Esperado 'entao'/,
  );
});
