import { test } from "node:test";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { resolve } from "node:path";
const run = (input: string) =>
  spawnSync(
    process.execPath,
    [
      "--import",
      "tsx",
      resolve("packages/cli/src/index.ts"),
      resolve("examples/entrada.alg"),
    ],
    { input, encoding: "utf8" },
  );
test("CLI consumes all redirected input lines without losing early data", () => {
  const result = run("Ana\n20\n");
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /Olá, Ana.*21/);
});
test("CLI returns nonzero on input exhaustion", () => {
  const result = run("Ana\n");
  assert.equal(result.status, 1);
  assert.match(result.stderr, /Fim da entrada/);
});
