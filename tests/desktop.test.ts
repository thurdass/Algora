import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, writeFile, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { _electron } from "playwright";
test(
  "desktop opens legacy files, executes and saves through isolated preload",
  { timeout: 30000 },
  async () => {
    const directory = await mkdtemp(join(tmpdir(), "algora-test-"));
    const source = join(directory, "legado.alg"),
      target = join(directory, "salvo.alg");
    await writeFile(
      source,
      Buffer.from(
        'algoritmo "legado"\ninicio\nescreval("Ol\xe1")\nfimalgoritmo',
        "latin1",
      ),
    );
    const packaged = process.env.ALGORA_EXECUTABLE;
    const app = await _electron.launch({
      ...(packaged ? { executablePath: resolve(packaged) } : {}),
      args: [
        ...(!packaged ? [resolve("apps/desktop/electron/main.cjs")] : []),
        "--user-data-dir=" + join(directory, "profile"),
      ],
    });
    try {
      const page = await app.firstWindow();
      await page.waitForSelector(".monaco-editor");
      assert.equal(
        await page.evaluate(() => typeof (window as any).require),
        "undefined",
      );
      assert.equal(
        await page.evaluate(() => typeof (window as any).algora.openFile),
        "function",
      );
      await app.evaluate(({ dialog }, p) => {
        dialog.showOpenDialog = async () => ({
          canceled: false,
          filePaths: [p],
        });
      }, source);
      await page.getByTitle("Abrir arquivo").click();
      await page.locator(".tab.active").getByText("legado.alg").waitFor();
      await page.locator(".run-button").click();
      await page.locator(".terminal-finished").waitFor();
      assert.match(await page.locator(".terminal-content").innerText(), /Olá/);
      await app.evaluate(({ dialog }, p) => {
        dialog.showSaveDialog = async () => ({ canceled: false, filePath: p });
      }, target);
      // A renderer-supplied path is not writable until selected by the native dialog.
      const saved = await page.evaluate(
        async (p) =>
          (window as any).algora.saveFile({
            path: p,
            content: 'algoritmo "salvo"\ninicio\nfimalgoritmo',
          }),
        target,
      );
      assert.equal(saved.path, target);
      assert.match(await readFile(target, "utf8"), /salvo/);
    } finally {
      await app.close();
      await rm(directory, { recursive: true, force: true });
    }
  },
);
