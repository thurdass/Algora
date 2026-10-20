import { readdir, readFile, stat } from "node:fs/promises";
import { parse } from "../src/index.ts";
const base = process.argv[2];
if (!base)
  throw new Error(
    "Uso: tsx packages/core/scripts/compatibility-survey.ts <diretorio-visualg>",
  );
async function walk(dir: string): Promise<string[]> {
  const result: string[] = [];
  for (const name of await readdir(dir)) {
    const path = dir + "/" + name;
    const s = await stat(path);
    if (s.isDirectory()) result.push(...(await walk(path)));
    else if (/\.alg$/i.test(name)) result.push(path);
  }
  return result;
}
const files = [...(await walk(base + "/Exemplos")), base + "/TESTE.alg"].sort();
const results = [];
for (const path of files) {
  const bytes = await readFile(path);
  let source: string;
  try {
    source = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
  } catch {
    source = new TextDecoder("windows-1252").decode(bytes);
  }
  try {
    parse(source);
    results.push({ file: path.slice(base.length + 1), parsed: true });
  } catch (e) {
    results.push({
      file: path.slice(base.length + 1),
      parsed: false,
      error: (e as Error).message,
    });
  }
}
console.log(
  JSON.stringify(
    {
      total: results.length,
      passed: results.filter((r) => r.parsed).length,
      results,
    },
    null,
    2,
  ),
);
