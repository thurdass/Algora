import { readFile } from "node:fs/promises";
import { createInterface } from "node:readline/promises";
import { stdin, stdout } from "node:process";
import { execute } from "@algora/core";
const file = process.argv[2];
if (!file) {
  console.error("Uso: npm run cli -- arquivo.alg");
  process.exitCode = 1;
} else {
  const terminal = createInterface({
    input: stdin,
    output: stdout,
    terminal: Boolean(stdin.isTTY),
  });
  const pending: string[] = [];
  let ended = false;
  let waiting: ((line: string | null) => void) | undefined;
  if (!stdin.isTTY) {
    terminal.on("line", (line) => {
      if (waiting) {
        const resolve = waiting;
        waiting = undefined;
        resolve(line);
      } else pending.push(line);
    });
    terminal.on("close", () => {
      ended = true;
      waiting?.(null);
      waiting = undefined;
    });
  }
  async function input(prompt: string): Promise<string> {
    if (stdin.isTTY) return terminal.question(prompt);
    stdout.write(prompt);
    const line = pending.length
      ? pending.shift()!
      : ended
        ? null
        : await new Promise<string | null>((resolve) => {
            waiting = resolve;
          });
    if (line === null)
      throw new Error("Fim da entrada: o algoritmo solicitou mais valores.");
    return line;
  }
  try {
    const bytes = await readFile(file);
    let source: string;
    try {
      source = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    } catch {
      source = new TextDecoder("windows-1252").decode(bytes);
    }
    await execute(source, { input, output: (text) => stdout.write(text) });
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  } finally {
    terminal.close();
  }
}
