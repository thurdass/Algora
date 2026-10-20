export type Value = number | string | boolean | Value[];
export type Token = {
  text: string;
  line: number;
  kind: "word" | "number" | "string" | "symbol" | "newline" | "eof";
};
export type Expr =
  | { kind: "literal"; value: Value }
  | { kind: "ref"; name: string; indices: Expr[] }
  | { kind: "unary"; op: string; right: Expr }
  | { kind: "binary"; op: string; left: Expr; right: Expr }
  | { kind: "call"; name: string; args: Expr[] };
export type Decl = {
  name: string;
  type: string;
  ranges: [number, number][];
  constant?: Value;
};
export type Stmt = { kind: string; line: number; [key: string]: any };
export type Routine = {
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
