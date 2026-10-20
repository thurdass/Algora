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

test("BOM, total array allocation budget and multidimensional index checks", async () => {
  assert.equal(
    (await execute("\uFEFF" + program("x:inteiro", "x <- 7"))).variables.x,
    7,
  );
  assert.throws(
    () =>
      createInterpreter(program("v:vetor[1..10000,1..10000] de inteiro", "")),
    /limite de memória/,
  );
  assert.throws(
    () =>
      createInterpreter(program("a,b:vetor[1..60000,1..10] de inteiro", "")),
    /limite de memória/,
  );
  await assert.rejects(
    execute(program("v:vetor[1..2,1..3] de inteiro", "v[1,4] <- 1")),
    /Índice fora/,
  );
});
test("breakpoints inspect pre-execution values and continue to completion", async () => {
  const source = program("x:inteiro", "x <- 1\nx <- 2\nx <- 3");
  let pauses = 0;
  const interpreter = createInterpreter(source, {
    onStep: (s) => {
      if (s.status === "paused") {
        pauses++;
        assert.equal(s.line, 6);
        assert.equal(s.variables.x, 1);
        interpreter.continue();
      }
    },
  });
  interpreter.setBreakpoints([6]);
  assert.equal((await interpreter.run()).variables.x, 3);
  assert.equal(pauses, 1);
});
test("stop and AbortSignal cancel an unresolved input promise", async () => {
  for (const abort of [false, true]) {
    const controller = new AbortController();
    let started!: () => void;
    const waiting = new Promise<void>((r) => (started = r));
    const interpreter = createInterpreter(program("x:inteiro", "leia(x)"), {
      signal: controller.signal,
      input: async () => {
        started();
        return new Promise<string>(() => {});
      },
    });
    const run = interpreter.run();
    await waiting;
    if (abort) controller.abort();
    else interpreter.stop();
    await assert.rejects(run, { name: "AbortError" });
    assert.equal(interpreter.getSnapshot().status, "stopped");
  }
});
test("function arguments run sequentially when functions mutate global state", async () => {
  const source = `algoritmo "argumentos"
var
x:inteiro
funcao next():inteiro
inicio
x <- x + 1
retorne x
fimfuncao
inicio
escreval(copia("abcd",next(),next()))
fimalgoritmo`;
  let output = "";
  await execute(source, { output: (t) => (output += t) });
  assert.equal(output, "ab\n");
});

test("routines resolve own locals and globals without leaking caller locals", async () => {
  const source = `algoritmo "escopo"
var
x:inteiro
funcao global():inteiro
inicio
retorne x
fimfuncao
funcao caller():inteiro
var
x:inteiro
inicio
x <- 99
retorne global()
fimfuncao
inicio
x <- 7
escreval(caller())
fimalgoritmo`;
  let output = "";
  await execute(source, { output: (t) => (output += t) });
  assert.equal(output, "7\n");
});

test("bare escreval produces a blank line", async () => {
  let output = "";
  await execute(program("", 'escreval\nescreval("ok")\nescreval'), {
    output: (t) => (output += t),
  });
  assert.equal(output, "\nok\n\n");
});

test("constants evaluate expressions, define vector bounds and are immutable", async () => {
  const source = `algoritmo "constantes"
const
base = 2
limite = base * 3 + 1
saudacao = "ola" + "!"
var
v:vetor[-base..limite] de inteiro
inicio
v[limite] <- base
escreval(saudacao, v[7])
fimalgoritmo`;
  let output = "";
  const result = await execute(source, { output: (t) => (output += t) });
  assert.equal(output, "ola!2\n");
  assert.equal(result.variables.limite, 7);
  await assert.rejects(
    execute(source.replace("v[limite] <- base", "base <- 9")),
    /alterar uma constante/,
  );
  assert.throws(
    () => parse(source.replace("base = 2", "base = 1 / 0")),
    /Constante numérica inválida/,
  );
});
test("DOS directive, random input precision, echo and restoration of normal input", async () => {
  const source = `algoritmo "random"
dos
var
x:inteiro
y:real
inicio
eco off
aleatorio 2,4
leia(x)
aleatorio 1,2,2
leia(y)
eco on
aleatorio 7,7
leia(x)
aleatorio off
leia(x)
escreval(x, " ",y:0:2)
fimalgoritmo`;
  let output = "";
  let reads = 0;
  const result = await execute(source, {
    input: async () => {
      reads++;
      return "9";
    },
    output: (t) => (output += t),
    random: () => 0.5,
  });
  assert.equal(result.variables.y, 1.5);
  assert.equal(result.variables.x, 9);
  assert.equal(reads, 1);
  assert.equal(output, "7\n9 1.50\n");
  await assert.rejects(
    execute(source.replace("aleatorio 2,4", "aleatorio 4,2")),
    /Configuração aleatória inválida/,
  );
});
test("choice ranges include both bounds and preserve case ordering", async () => {
  for (const input of ["4", "5", "7", "10", "11"]) {
    let output = "";
    await execute(
      program(
        "n:inteiro",
        'leia(n)\nescolha n\ncaso 5 ate 7, 4\nescreval("medio")\ncaso 8 ate 10\nescreval("alto")\noutrocaso\nescreval("outro")\nfimescolha',
      ),
      { input: async () => input, output: (t) => (output += t) },
    );
    assert.equal(
      output,
      Number(input) <= 7
        ? "medio\n"
        : Number(input) <= 10
          ? "alto\n"
          : "outro\n",
    );
  }
});
test("unconditional repita ends with interrompa and ate_que accepts until condition", async () => {
  const result = await execute(
    program(
      "n:inteiro",
      "repita\nn <- n + 1\nse n = 3 entao\ninterrompa\nfimse\nfimrepita\nrepita\nn <- n + 1\nate_que n = 5",
    ),
  );
  assert.equal(result.variables.n, 5);
});

test("documented math signatures implement powers, inverse trig and angle conversions", async () => {
  const result = await execute(
    program(
      "power,angle,sine,cosine,cotangent,degrees:real",
      "power <- exp(2,9)\nangle <- grauprad(90)\nsine <- arcsen(1)\ncosine <- arccos(0)\ncotangent <- cotan(pi/4)\ndegrees <- radpgrau(pi)",
    ),
  );
  assert.equal(result.variables.power, 512);
  for (const name of ["angle", "sine", "cosine"])
    assert.ok(Math.abs(Number(result.variables[name]) - Math.PI / 2) < 1e-12);
  assert.ok(Math.abs(Number(result.variables.cotangent) - 1) < 1e-12);
  assert.equal(result.variables.degrees, 180);
  await assert.rejects(
    execute(program("x:real", "x <- exp(2)")),
    /Quantidade de argumentos inválida: exp/,
  );
  await assert.rejects(
    execute(program("x:real", "x <- arccos(2)")),
    /Resultado inválido: arccos/,
  );
});
test("zero argument Pi and Rand work bare or called, bound identifiers retain scope", async () => {
  const result = await execute(
    program("a,b,c,d:real", "a <- pi\nb <- pi()\nc <- rand\nd <- rand()"),
    { random: () => 0.25 },
  );
  assert.equal(result.variables.a, Math.PI);
  assert.equal(result.variables.b, Math.PI);
  assert.equal(result.variables.c, 0.25);
  assert.equal(result.variables.d, 0.25);
  assert.equal(
    (await execute(program("pi:real\nrand:real", "pi <- 7\nrand <- pi")))
      .variables.rand,
    7,
  );
  await assert.rejects(
    execute(program("x:real", "x <- pi(1)")),
    /Quantidade de argumentos inválida: pi/,
  );
  await assert.rejects(
    execute(program("x:inteiro", "x <- randi(0)")),
    /limite inteiro positivo/,
  );
  await assert.rejects(
    execute(program("x:real", "x <- rand"), { random: () => 1 }),
    /Gerador aleatório/,
  );
});
test("documented text helpers use substring first, one-based positions and ASCII conversion", async () => {
  let output = "";
  await execute(
    program(
      "",
      'escreval(pos("Brasil","O melhor do Brasil")," ",pos("z","abc")," ",copia("abcdef",2,3)," ",asc("Brasil")," ",carac(65)," ",caracpnum("9")+2," ",numpcarac(6)+"2")',
    ),
    { output: (t) => (output += t) },
  );
  assert.equal(output, "13 0 bcd 66 A 11 62\n");
  await assert.rejects(
    execute(program("", 'escreval(pos("x"))')),
    /Quantidade de argumentos inválida: pos/,
  );
});
