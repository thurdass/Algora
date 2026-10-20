import type { Value, Decl, Expr, Stmt, Program } from "./types.js";
import { AlgoraError, norm } from "./diagnostics.js";
import { parse } from "./parser.js";
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
  private cancelInput?: () => void;
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
    const slots = (ranges: [number, number][]) => {
      let product = 1,
        total = 1;
      for (const [low, high] of ranges) {
        product *= high - low + 1;
        total += product;
      }
      return total;
    };
    let allocated = this.scopes.reduce(
      (total, current) =>
        total +
        [...current.values()].reduce(
          (sum, cell) => sum + slots(cell.ranges),
          0,
        ),
      0,
    );
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
      allocated += slots(d.ranges);
      if (allocated > 1000000)
        throw new AlgoraError("Vetor excede limite de memória", 1);
      scope.set(d.name, { value: init(0), type: d.type, ranges: d.ranges });
    }
    return scope;
  }
  getSnapshot(): Snapshot {
    const variables: Record<string, Value> = {};
    for (const scope of [this.scopes[0], this.scopes[this.scopes.length - 1]])
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
    this.cancelInput?.();
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
    const local = this.scopes[this.scopes.length - 1].get(name);
    const global = this.scopes[0].get(name);
    if (local ?? global) return (local ?? global)!;
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
      const values: Value[] = [];
      for (const arg of args) values.push(await this.eval(arg));
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
  private async readInput(): Promise<string> {
    this.check();
    const abort = () => this.stop();
    const canceled = new Promise<never>((_, reject) => {
      this.cancelInput = () =>
        reject(new DOMException("Execução interrompida", "AbortError"));
    });
    this.options.signal?.addEventListener("abort", abort, { once: true });
    try {
      if (this.options.signal?.aborted) this.stop();
      return await Promise.race([this.options.input!(""), canceled]);
    } finally {
      this.cancelInput = undefined;
      this.options.signal?.removeEventListener("abort", abort);
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
          const response = await this.readInput();
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
