import { test, expect } from "@playwright/test";
test("executes an algorithm with interactive terminal input", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.locator(".monaco-editor")).toBeVisible();
  await expect(page.locator(".view-lines")).toContainText("fimalgoritmo");
  await page.locator(".run-button").click();
  const input = page.getByRole("textbox", { name: "Entrada do algoritmo" });
  await expect(input).toBeVisible();
  await input.fill("Ada");
  await input.press("Enter");
  await expect(page.locator(".terminal-content")).toContainText(
    "Bem-vindo ao Algora, Ada!",
  );
  await expect(page.locator(".terminal-content")).toContainText("Passo 3");
  await expect(page.locator(".terminal-finished")).toContainText("finalizada");
  expect(errors).toEqual([]);
});
test("debugger steps through statements and stops without error", async ({
  page,
}) => {
  await page.goto("/");
  await expect(page.locator(".monaco-editor")).toBeVisible();
  await expect(page.locator(".view-lines")).toContainText("fimalgoritmo");
  await page.getByTitle("Depurar: pontos de parada (F9)").click();
  await expect(page.locator(".terminal-tools")).toContainText("Pausado");
  await expect(page.locator(".variables")).toContainText("contador");
  await page.getByTitle("Próximo passo (F10)").click();
  await expect(page.locator(".terminal-content")).toContainText("Olá!");
  await page.getByTitle("Parar execução").click();
  await expect(page.locator(".terminal-error")).toHaveCount(0);
  await expect(page.getByTitle("Parar execução")).toBeDisabled();
});
test("reports syntax errors and recovers for another run", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator(".monaco-editor")).toBeVisible();
  await expect(page.locator(".view-lines")).toContainText("fimalgoritmo");
  const chooser1 = page.waitForEvent("filechooser");
  await page.getByTitle("Abrir arquivo").click();
  await (
    await chooser1
  ).setFiles({
    name: "erro.alg",
    mimeType: "text/plain",
    buffer: Buffer.from('algoritmo "erro"\ninicio\nescreval(\nfimalgoritmo'),
  });
  await expect(page.locator(".view-lines")).toContainText("erro");
  await page.locator(".run-button").click();
  await expect(page.locator(".terminal-error")).toContainText("Linha");
  const chooser2 = page.waitForEvent("filechooser");
  await page.getByTitle("Abrir arquivo").click();
  await (
    await chooser2
  ).setFiles({
    name: "ok.alg",
    mimeType: "text/plain",
    buffer: Buffer.from(
      'algoritmo "ok"\ninicio\nescreval("recuperado")\nfimalgoritmo',
    ),
  });
  await expect(page.locator(".view-lines")).toContainText("recuperado");
  await page.locator(".run-button").click();
  await expect(page.locator(".terminal-content")).toContainText("recuperado");
  await expect(page.locator(".terminal-error")).toHaveCount(0);
});

test("offers Portugol completion and records a line breakpoint", async ({
  page,
}) => {
  await page.goto("/");
  await expect(page.locator(".view-lines")).toContainText("fimalgoritmo");
  await page.getByTitle("Novo arquivo").click();
  await page.locator(".view-lines .view-line").nth(3).click();
  await page.keyboard.press("End");
  await page.keyboard.press("Control+Space");
  await expect(page.locator(".suggest-widget")).toBeVisible();
  await expect(page.locator(".suggest-widget")).toContainText("algoritmo");
  await page.keyboard.press("Escape");
  await page.keyboard.press("F9");
  await expect(page.locator(".breakpoint-row")).toContainText(":4");
});
