/** Independent Portugol interpreter. No code from VisualG is used. */
export type { Value, Program } from "./types.js";
export { AlgoraError } from "./diagnostics.js";
export { tokenize } from "./lexer.js";
export { parse } from "./parser.js";
export { Interpreter, createInterpreter, execute } from "./runtime.js";
export type { Snapshot, InterpreterOptions } from "./runtime.js";
