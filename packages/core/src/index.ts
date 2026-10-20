/** Independent Portugol interpreter. No code from VisualG is used. */
export type Value = number | string | boolean | Value[];
type Token = {
  text: string;
  line: number;
  kind: "word" | "number" | "string" | "symbol" | "newline" | "eof";
};
export class AlgoraError extends Error {
  constructor(
    message: string,
    public line: number,
  ) {
    super(`Linha ${line}: ${message}`);
    this.name = "AlgoraError";
  }
}
const norm = (s: string) =>
  s
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
export function tokenize(source: string): Token[] {
  const out: Token[] = [];
  let i = 0,
    line = 1;
  while (i < source.length) {
    const c = source[i];
    if (c === "\r" || c === " " || c === "\t") {
      i++;
      continue;
    }
    if (c === "\n") {
      out.push({ text: "\n", line: line++, kind: "newline" });
      i++;
      continue;
    }
    if (source.slice(i, i + 2) === "//") {
      while (i < source.length && source[i] !== "\n") i++;
      continue;
    }
    if (c === '"' || c === "'") {
      const quote = c;
      let text = "";
      i++;
      while (i < source.length && source[i] !== quote) {
        if (source[i] === "\n")
          throw new AlgoraError("Texto não terminado", line);
        text += source[i++];
      }
      if (i === source.length)
        throw new AlgoraError("Texto não terminado", line);
      i++;
      out.push({ text, line, kind: "string" });
      continue;
    }
    const number = source.slice(i).match(/^\d+(?:\.(?!\.)\d+)?/);
    if (number) {
      out.push({ text: number[0], line, kind: "number" });
      i += number[0].length;
      continue;
    }
    const word = source.slice(i).match(/^[\p{L}_][\p{L}\p{N}_]*/u);
    if (word) {
      out.push({ text: norm(word[0]), line, kind: "word" });
      i += word[0].length;
      continue;
    }
    const op =
      ["<-", ":=", "<=", ">=", "<>", "!=", ".."].find((x) =>
        source.startsWith(x, i),
      ) ?? c;
    if (!"<-+*/^%=():,[];\\.>".includes(c) && op.length === 1)
      throw new AlgoraError(`Caractere inválido: ${c}`, line);
    out.push({ text: op, line, kind: "symbol" });
    i += op.length;
  }
  out.push({ text: "", line, kind: "eof" });
  return out;
}
type Expr =
  | { kind: "literal"; value: Value }
  | { kind: "ref"; name: string; indices: Expr[] }
  | { kind: "unary"; op: string; right: Expr }
  | { kind: "binary"; op: string; left: Expr; right: Expr }
  | { kind: "call"; name: string; args: Expr[] };
type Decl = { name: string; type: string; ranges: [number, number][] };
type Stmt = { kind: string; line: number; [key: string]: any };
type Routine = {
  name: string;
  params: { name: string; type: string; byRef: boolean }[];
  declarations: Decl[];
  body: Stmt[];
};
export type Program = {
  name: string;
  declarations: Decl[];
  routines: Routine[];
  body: Stmt[];
};
class Parser {
  tokens: Token[];
  i = 0;
  constructor(source: string) {
    this.tokens = tokenize(source);
  }
  get t() {
    return this.tokens[this.i];
  }
  is(s: string) {
    return this.t.text === s;
  }
  take() {
    return this.tokens[this.i++];
  }
  accept(s: string) {
    if (this.is(s)) {
      this.take();
      return true;
    }
    return false;
  }
  expect(s: string) {
    if (!this.accept(s))
      throw new AlgoraError(
        `Esperado '${s}', encontrado '${this.t.text}'`,
        this.t.line,
      );
  }
  name() {
    if (this.t.kind !== "word")
      throw new AlgoraError("Identificador esperado", this.t.line);
    return this.take().text;
  }
  lines() {
    while (this.t.kind === "newline" || this.is(";")) this.take();
  }
  expr(min = 0): Expr {
    let left: Expr;
    const t = this.take();
    if (t.kind === "number") left = { kind: "literal", value: Number(t.text) };
    else if (t.kind === "string") left = { kind: "literal", value: t.text };
    else if (["verdadeiro", "falso"].includes(t.text))
      left = { kind: "literal", value: t.text === "verdadeiro" };
    else if (["-", "+", "nao"].includes(t.text))
      left = { kind: "unary", op: t.text, right: this.expr(7) };
    else if (t.text === "(") {
      left = this.expr();
      this.expect(")");
    } else if (t.kind === "word") {
      if (this.accept("(")) {
        const args: Expr[] = [];
        if (!this.is(")"))
          do {
            args.push(this.expr());
          } while (this.accept(","));
        this.expect(")");
        left = { kind: "call", name: t.text, args };
      } else {
        const indices: Expr[] = [];
        if (this.accept("[")) {
          do {
            indices.push(this.expr());
          } while (this.accept(","));
          this.expect("]");
        }
        left = { kind: "ref", name: t.text, indices };
      }
    } else throw new AlgoraError("Expressão esperada", t.line);
    const prec: Record<string, number> = {
      ou: 1,
      xou: 1,
      e: 2,
      "=": 3,
      "<>": 3,
      "!=": 3,
      "<": 3,
      ">": 3,
      "<=": 3,
      ">=": 3,
      "+": 4,
      "-": 4,
      "*": 5,
      "/": 5,
      "\\": 5,
      div: 5,
      mod: 5,
      "%": 5,
      "^": 6,
    };
    while ((prec[this.t.text] ?? -1) >= min) {
      const op = this.take().text;
      left = {
        kind: "binary",
        op,
        left,
        right: this.expr(prec[op] + (op === "^" ? 0 : 1)),
      };
    }
    return left;
  }
  declarations(): Decl[] {
    const out: Decl[] = [];
    this.accept("var");
    this.lines();
    while (!["inicio", "funcao", "procedimento", ""].includes(this.t.text)) {
      const names = [this.name()];
      while (this.accept(",")) names.push(this.name());
      this.expect(":");
      const ranges: [number, number][] = [];
      let type = this.name();
      if (type === "vetor") {
        this.expect("[");
        do {
          let sign = this.accept("-") ? -1 : 1;
          const low = sign * Number(this.take().text);
          this.expect("..");
          sign = this.accept("-") ? -1 : 1;
          const high = sign * Number(this.take().text);
          if (
            !Number.isInteger(low) ||
            !Number.isInteger(high) ||
            high < low ||
            high - low > 100000
          )
            throw new AlgoraError("Limites de vetor inválidos", this.t.line);
          ranges.push([low, high]);
        } while (this.accept(","));
        this.expect("]");
        this.expect("de");
        type = this.name();
      }
      if (
        ![
          "inteiro",
          "real",
          "caractere",
          "caracter",
          "literal",
          "logico",
        ].includes(type)
      )
        throw new AlgoraError(`Tipo não suportado: ${type}`, this.t.line);
      for (const name of names) out.push({ name, type, ranges });
      this.lines();
    }
    return out;
  }
  block(stops: string[]): Stmt[] {
    const out: Stmt[] = [];
    this.lines();
    while (this.t.kind !== "eof" && !stops.includes(this.t.text)) {
      out.push(this.statement());
      this.lines();
    }
    return out;
  }
  statement(): Stmt {
    const t = this.take(),
      line = t.line,
      k = t.text;
    if (k === "se") {
      const condition = this.expr();
      this.expect("entao");
      const yes = this.block(["senao", "fimse"]);
      const no = this.accept("senao") ? this.block(["fimse"]) : [];
      this.expect("fimse");
      return { kind: k, line, condition, yes, no };
    }
    if (k === "enquanto") {
      const condition = this.expr();
      this.expect("faca");
      const body = this.block(["fimenquanto"]);
      this.expect("fimenquanto");
      return { kind: k, line, condition, body };
    }
    if (k === "repita") {
      const body = this.block(["ate"]);
      this.expect("ate");
      return { kind: k, line, body, condition: this.expr() };
    }
    if (k === "para") {
      const name = this.name();
      this.expect("de");
      const from = this.expr();
      this.expect("ate");
      const to = this.expr();
      const step = this.accept("passo")
        ? this.expr()
        : { kind: "literal", value: 1 };
      this.expect("faca");
      const body = this.block(["fimpara"]);
      this.expect("fimpara");
      return { kind: k, line, name, from, to, step, body };
    }
    if (k === "escolha") {
      const value = this.expr();
      this.accept("faca");
      this.lines();
      const cases: { values: Expr[]; body: Stmt[] }[] = [];
      while (this.accept("caso")) {
        const values = [this.expr()];
        while (this.accept(",")) values.push(this.expr());
        cases.push({
          values,
          body: this.block(["caso", "outrocaso", "fimescolha"]),
        });
      }
      const otherwise = this.accept("outrocaso")
        ? this.block(["fimescolha"])
        : [];
      this.expect("fimescolha");
      return { kind: k, line, value, cases, otherwise };
    }
    if (["escreva", "escreval", "leia"].includes(k)) {
      this.expect("(");
      const args: { expr: Expr; width?: Expr; precision?: Expr }[] = [];
      if (!this.is(")"))
        do {
          const arg: { expr: Expr; width?: Expr; precision?: Expr } = {
            expr: this.expr(),
          };
          if (this.accept(":")) {
            arg.width = this.expr();
            if (this.accept(":")) arg.precision = this.expr();
          }
          args.push(arg);
        } while (this.accept(","));
      this.expect(")");
      return { kind: k, line, args };
    }
    if (k === "retorne") return { kind: k, line, value: this.expr() };
    if (["interrompa", "limpatela", "pausa"].includes(k))
      return { kind: k, line };
    this.i--;
    const target = this.expr();
    if (this.accept("<-") || this.accept(":=")) {
      if (target.kind !== "ref")
        throw new AlgoraError("Destino inválido", line);
      return { kind: "assign", line, target, value: this.expr() };
    }
    if (target.kind === "ref" && !target.indices.length)
      return {
        kind: "call",
        line,
        expr: { kind: "call", name: target.name, args: [] },
      };
    if (target.kind === "call") return { kind: "call", line, expr: target };
    throw new AlgoraError(`Comando inválido: ${k}`, line);
  }
  parse(): Program {
    this.lines();
    this.expect("algoritmo");
    const name = this.t.kind === "string" ? this.take().text : "Sem título";
    this.lines();
    const declarations = this.declarations(),
      routines: Routine[] = [];
    while (this.is("funcao") || this.is("procedimento")) {
      const kind = this.take().text;
      const name = this.name(),
        params: Routine["params"] = [];
      if (this.accept("(")) {
        if (!this.is(")"))
          do {
            const byRef = this.accept("var");
            const names = [this.name()];
            while (this.accept(",")) names.push(this.name());
            this.expect(":");
            const type = this.name();
            for (const n of names) params.push({ name: n, type, byRef });
          } while (this.accept(";"));
        this.expect(")");
      }
      if (this.accept(":")) this.name();
      this.lines();
      const declarations = this.declarations();
      this.expect("inicio");
      const body = this.block([
        kind === "funcao" ? "fimfuncao" : "fimprocedimento",
      ]);
      this.take();
      this.lines();
      routines.push({ name, params, declarations, body });
    }
    this.expect("inicio");
    const body = this.block(["fimalgoritmo"]);
    this.expect("fimalgoritmo");
    this.lines();
    if (this.t.kind !== "eof")
      throw new AlgoraError("Conteúdo após fimalgoritmo", this.t.line);
    return { name, declarations, routines, body };
  }
}
export const parse = (source: string): Program => new Parser(source).parse();
export type Snapshot = {
  line: number;
  variables: Record<string, Value>;
  status: "idle" | "running" | "paused" | "completed" | "stopped" | "error";
  steps: number;
};
export type InterpreterOptions = {
  input?: (prompt: string) => Promise<string>;
  output?: (text: string) => void;
  onStep?: (snapshot: Snapshot) => void | Promise<void>;
  signal?: AbortSignal;
  maxSteps?: number;
  maxCallDepth?: number;
};
type Cell = { value: Value; type: string; ranges: [number, number][] };
class BreakSignal {}
class ReturnSignal {
  constructor(public value: Value) {}
}
export class Interpreter {
  readonly program: Program;
  private scopes: Map<string, Cell>[] = [];
  private line = 0;
  private steps = 0;
  private status: Snapshot["status"] = "idle";
  private breakpoints = new Set<number>();
  private release?: () => void;
  private single = false;
  private stopped = false;
  private depth = 0;
  constructor(
    source: string,
    private options: InterpreterOptions = {},
  ) {
    this.program = parse(source);
    this.scopes = [this.declare(this.program.declarations)];
  }
  private declare(declarations: Decl[]) {
    const scope = new Map<string, Cell>();
    for (const d of declarations) {
      if (scope.has(d.name))
        throw new AlgoraError(`Variável duplicada: ${d.name}`, 1);
      const scalar = () =>
        ["caractere", "caracter", "literal"].includes(d.type)
          ? ""
          : d.type === "logico"
            ? false
            : 0;
      const init = (i: number): Value =>
        i === d.ranges.length
          ? scalar()
          : Array.from({ length: d.ranges[i][1] - d.ranges[i][0] + 1 }, () =>
              init(i + 1),
            );
      const size = d.ranges.reduce((n, [l, h]) => n * (h - l + 1), 1);
      if (size > 1000000)
        throw new AlgoraError("Vetor excede limite de memória", 1);
      scope.set(d.name, { value: init(0), type: d.type, ranges: d.ranges });
    }
    return scope;
  }
  getSnapshot(): Snapshot {
    const variables: Record<string, Value> = {};
    for (const scope of this.scopes)
      for (const [n, c] of scope) variables[n] = structuredClone(c.value);
    return {
      line: this.line,
      variables,
      status: this.status,
      steps: this.steps,
    };
  }
  setBreakpoints(lines: Iterable<number>) {
    this.breakpoints = new Set(lines);
  }
  pause() {
    this.single = true;
  }
  step() {
    this.single = true;
    this.release?.();
    this.release = undefined;
  }
  continue() {
    this.single = false;
    this.release?.();
    this.release = undefined;
  }
  stop() {
    this.stopped = true;
    this.release?.();
    this.release = undefined;
  }
  private check() {
    if (this.stopped || this.options.signal?.aborted)
      throw new DOMException("Execução interrompida", "AbortError");
    if (this.steps > (this.options.maxSteps ?? 100000))
      throw new AlgoraError("Limite de instruções excedido", this.line);
  }
  private async checkpoint(line: number) {
    this.line = line;
    this.steps++;
    this.check();
    if (this.single || this.breakpoints.has(line)) {
      this.status = "paused";
      const waiting = new Promise<void>((resolve) => {
        this.release = resolve;
      });
      await this.options.onStep?.(this.getSnapshot());
      const abort = () => this.stop();
      this.options.signal?.addEventListener("abort", abort, { once: true });
      if (this.options.signal?.aborted) this.stop();
      await waiting;
      this.options.signal?.removeEventListener("abort", abort);
      this.check();
    }
    this.status = "running";
    await this.options.onStep?.(this.getSnapshot());
    if (this.steps % 100 === 0)
      await new Promise((resolve) => setTimeout(resolve, 0));
  }
  private cell(name: string) {
    for (let i = this.scopes.length - 1; i >= 0; i--) {
      const c = this.scopes[i].get(name);
      if (c) return c;
    }
    throw new AlgoraError(`Variável não declarada: ${name}`, this.line);
  }
  private async reference(expr: Expr) {
    if (expr.kind !== "ref")
      throw new AlgoraError("Variável esperada", this.line);
    const cell = this.cell(expr.name);
    if (expr.indices.length !== cell.ranges.length)
      throw new AlgoraError(`Dimensões inválidas: ${expr.name}`, this.line);
    const indices: number[] = [];
    for (let i = 0; i < expr.indices.length; i++) {
      const value = Number(await this.eval(expr.indices[i]));
      const [low, high] = cell.ranges[i];
      if (!Number.isInteger(value) || value < low || value > high)
        throw new AlgoraError(`Índice fora dos limites: ${value}`, this.line);
      indices.push(value - low);
    }
    return { cell, indices };
  }
  private async get(expr: Expr) {
    const { cell, indices } = await this.reference(expr);
    let value = cell.value;
    for (const i of indices) value = (value as Value[])[i];
    return value;
  }
  private convert(value: Value, type: string): Value {
    if (type === "inteiro" || type === "real") {
      const n = Number(value);
      if (!Number.isFinite(n))
        throw new AlgoraError("Valor numérico inválido", this.line);
      return type === "inteiro" ? Math.trunc(n) : n;
    }
    if (type === "logico") {
      if (typeof value === "boolean") return value;
      if (norm(String(value)) === "verdadeiro") return true;
      if (norm(String(value)) === "falso") return false;
      throw new AlgoraError("Valor lógico inválido", this.line);
    }
    return String(value);
  }
  private async set(expr: Expr, value: Value) {
    const { cell, indices } = await this.reference(expr);
    value = this.convert(value, cell.type);
    if (!indices.length) cell.value = value;
    else {
      let container = cell.value as Value[];
      for (const i of indices.slice(0, -1)) container = container[i] as Value[];
      container[indices.at(-1)!] = value;
    }
  }
  private text(value: Value) {
    return typeof value === "boolean"
      ? value
        ? "VERDADEIRO"
        : "FALSO"
      : String(value);
  }
  private async eval(expr: Expr): Promise<Value> {
    if (expr.kind === "literal") return expr.value;
    if (expr.kind === "ref") return this.get(expr);
    if (expr.kind === "call") return this.call(expr.name, expr.args);
    if (expr.kind === "unary") {
      const v = await this.eval(expr.right);
      return expr.op === "nao" ? !v : expr.op === "-" ? -Number(v) : Number(v);
    }
    const a = await this.eval(expr.left);
    if (expr.op === "e" && !a) return false;
    if (expr.op === "ou" && a) return true;
    const b = await this.eval(expr.right);
    let result: Value;
    switch (expr.op) {
      case "+":
        result =
          typeof a === "string" || typeof b === "string"
            ? String(a) + String(b)
            : Number(a) + Number(b);
        break;
      case "-":
        result = Number(a) - Number(b);
        break;
      case "*":
        result = Number(a) * Number(b);
        break;
      case "/":
      case "\\":
      case "div":
      case "%":
      case "mod":
        if (Number(b) === 0)
          throw new AlgoraError("Divisão por zero", this.line);
        result =
          expr.op === "/"
            ? Number(a) / Number(b)
            : ["%", "mod"].includes(expr.op)
              ? Number(a) % Number(b)
              : Math.trunc(Number(a) / Number(b));
        break;
      case "^":
        result = Number(a) ** Number(b);
        break;
      case "=":
        result = a === b;
        break;
      case "<>":
      case "!=":
        result = a !== b;
        break;
      case "<":
        result = a < b;
        break;
      case ">":
        result = a > b;
        break;
      case "<=":
        result = a <= b;
        break;
      case ">=":
        result = a >= b;
        break;
      case "e":
        result = Boolean(a && b);
        break;
      case "ou":
        result = Boolean(a || b);
        break;
      case "xou":
        result = Boolean(a) !== Boolean(b);
        break;
      default:
        throw new AlgoraError("Operador inválido", this.line);
    }
    if (typeof result === "number" && !Number.isFinite(result))
      throw new AlgoraError("Resultado numérico inválido", this.line);
    return result;
  }
  private async call(name: string, args: Expr[]): Promise<Value> {
    const builtins: Record<string, (...a: any[]) => Value> = {
      abs: Math.abs,
      raizq: Math.sqrt,
      quad: (n: number) => n * n,
      int: Math.trunc,
      sen: Math.sin,
      cos: Math.cos,
      tan: Math.tan,
      arctan: Math.atan,
      exp: Math.exp,
      log: Math.log10,
      logn: Math.log,
      pi: () => Math.PI,
      compr: (s: Value) => String(s).length,
      maiusc: (s: Value) => String(s).toUpperCase(),
      minusc: (s: Value) => String(s).toLowerCase(),
      copia: (s: Value, p: number, n: number) =>
        String(s).slice(p - 1, p - 1 + n),
      pos: (part: Value, s: Value) => String(s).indexOf(String(part)) + 1,
      asc: (s: Value) => String(s).charCodeAt(0),
      carac: (n: number) => String.fromCharCode(n),
      numpcarac: (n: Value) => String(n),
      caracpnum: (s: Value) => Number(s),
      randi: (n: number) => Math.floor(Math.random() * n),
      rand: () => Math.random(),
    };
    if (builtins[name]) {
      const values = await Promise.all(args.map((a) => this.eval(a)));
      const result = builtins[name](...values);
      if (typeof result === "number" && !Number.isFinite(result))
        throw new AlgoraError(`Resultado inválido: ${name}`, this.line);
      return result;
    }
    const routine = this.program.routines.find((r) => r.name === name);
    if (!routine)
      throw new AlgoraError(
        `Função/procedimento desconhecido: ${name}`,
        this.line,
      );
    if (args.length !== routine.params.length)
      throw new AlgoraError(
        `Quantidade de argumentos inválida: ${name}`,
        this.line,
      );
    if (this.depth >= (this.options.maxCallDepth ?? 100))
      throw new AlgoraError("Limite de chamadas excedido", this.line);
    const scope = this.declare(routine.declarations);
    for (let i = 0; i < args.length; i++) {
      const p = routine.params[i];
      if (p.byRef) {
        const ref = await this.reference(args[i]);
        if (ref.indices.length)
          throw new AlgoraError(
            "Parâmetro por referência requer variável escalar",
            this.line,
          );
        scope.set(p.name, ref.cell);
      } else
        scope.set(p.name, {
          value: this.convert(await this.eval(args[i]), p.type),
          type: p.type,
          ranges: [],
        });
    }
    this.depth++;
    this.scopes.push(scope);
    try {
      await this.block(routine.body);
      return 0;
    } catch (e) {
      if (e instanceof ReturnSignal) return e.value;
      throw e;
    } finally {
      this.scopes.pop();
      this.depth--;
    }
  }
  private async block(body: Stmt[]) {
    for (const stmt of body) await this.statement(stmt);
  }
  private async statement(s: Stmt) {
    await this.checkpoint(s.line);
    switch (s.kind) {
      case "assign":
        await this.set(s.target, await this.eval(s.value));
        break;
      case "call":
        await this.eval(s.expr);
        break;
      case "escreva":
      case "escreval": {
        let text = "";
        for (const a of s.args) {
          const value = await this.eval(a.expr);
          let part = a.precision
            ? Number(value).toFixed(
                Math.max(
                  0,
                  Math.min(100, Number(await this.eval(a.precision))),
                ),
              )
            : this.text(value);
          if (a.width)
            part = part.padStart(
              Math.min(10000, Math.max(0, Number(await this.eval(a.width)))),
            );
          text += part;
        }
        this.options.output?.(text + (s.kind === "escreval" ? "\n" : ""));
        break;
      }
      case "leia":
        for (const a of s.args) {
          if (!this.options.input)
            throw new AlgoraError("Entrada não disponível", s.line);
          const response = await this.options.input("");
          this.check();
          await this.set(a.expr, response);
        }
        break;
      case "se":
        await this.block((await this.eval(s.condition)) ? s.yes : s.no);
        break;
      case "enquanto":
        try {
          while (await this.eval(s.condition)) {
            await this.block(s.body);
            await this.checkpoint(s.line);
          }
        } catch (e) {
          if (!(e instanceof BreakSignal)) throw e;
        }
        break;
      case "repita":
        try {
          do {
            await this.block(s.body);
            await this.checkpoint(s.line);
          } while (!(await this.eval(s.condition)));
        } catch (e) {
          if (!(e instanceof BreakSignal)) throw e;
        }
        break;
      case "para": {
        const from = Number(await this.eval(s.from)),
          to = Number(await this.eval(s.to)),
          step = Number(await this.eval(s.step));
        if (!Number.isFinite(step) || step === 0)
          throw new AlgoraError("Passo inválido", s.line);
        const ref: Expr = { kind: "ref", name: s.name, indices: [] };
        try {
          for (let i = from; step > 0 ? i <= to : i >= to; i += step) {
            await this.set(ref, i);
            await this.block(s.body);
            await this.checkpoint(s.line);
          }
        } catch (e) {
          if (!(e instanceof BreakSignal)) throw e;
        }
        break;
      }
      case "escolha": {
        const value = await this.eval(s.value);
        let body = s.otherwise;
        outer: for (const c of s.cases)
          for (const e of c.values)
            if ((await this.eval(e)) === value) {
              body = c.body;
              break outer;
            }
        await this.block(body);
        break;
      }
      case "interrompa":
        throw new BreakSignal();
      case "retorne":
        throw new ReturnSignal(await this.eval(s.value));
      case "limpatela":
        this.options.output?.("\x1b[2J\x1b[H");
        break;
      case "pausa":
        break;
    }
  }
  async run(): Promise<Snapshot> {
    if (this.status !== "idle")
      throw new AlgoraError("Crie uma nova execução para reiniciar", this.line);
    this.status = "running";
    try {
      await this.block(this.program.body);
      this.check();
      this.status = "completed";
      return this.getSnapshot();
    } catch (e) {
      this.status =
        e instanceof DOMException && e.name === "AbortError"
          ? "stopped"
          : "error";
      if (e instanceof BreakSignal)
        throw new AlgoraError("interrompa fora de um laço", this.line);
      if (e instanceof ReturnSignal)
        throw new AlgoraError("retorne fora de uma função", this.line);
      throw e;
    } finally {
      await this.options.onStep?.(this.getSnapshot());
    }
  }
}
export const createInterpreter = (
  source: string,
  options: InterpreterOptions = {},
) => new Interpreter(source, options);
export const execute = async (
  source: string,
  options: InterpreterOptions = {},
) => createInterpreter(source, options).run();
