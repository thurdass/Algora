import type { Token, Expr, Decl, Stmt, Routine, Program } from "./types.js";
import { AlgoraError } from "./diagnostics.js";
import { tokenize } from "./lexer.js";
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
      if (
        k !== "leia" &&
        (this.t.kind === "newline" || this.t.kind === "eof" || this.is(";"))
      )
        return { kind: k, line, args: [] };
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
