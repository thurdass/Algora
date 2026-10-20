import type { Token } from "./types.js";
import { AlgoraError, norm } from "./diagnostics.js";
export function tokenize(source: string): Token[] {
  source = source.replace(/^\uFEFF/, "");
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
