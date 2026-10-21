package io.algora.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.*;

class AlgoraApplicationTest {
  private Stage stage;
  private AlgoraApplication app;

  @BeforeAll
  static void initializeJavaFx() throws Exception {
    Assumptions.assumeTrue(
        !System.getProperty("os.name").toLowerCase().contains("linux")
            || System.getenv("DISPLAY") != null,
        "Execute os testes gráficos com xvfb-run -a ./gradlew test em Linux sem display.");
    CountDownLatch ready = new CountDownLatch(1);
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          ready.countDown();
        });
    assertTrue(ready.await(15, TimeUnit.SECONDS));
  }

  @AfterAll
  static void shutdownJavaFx() {
    Platform.exit();
  }

  @BeforeEach
  void launch() throws Exception {
    fx(
        () -> {
          app = new AlgoraApplication();
          stage = new Stage();
          app.start(stage);
          stage.getScene().getRoot().applyCss();
          stage.getScene().getRoot().layout();
          return null;
        });
  }

  @AfterEach
  void close() throws Exception {
    fx(
        () -> {
          app.stop();
          stage.hide();
          return null;
        });
  }

  private static <T> T fx(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private <T> T node(String id, Class<T> type) {
    return type.cast(stage.getScene().lookup("#" + id));
  }

  private String output() throws Exception {
    return fx(() -> node("terminalOutput", TextArea.class).getText());
  }

  private void capture(String name) throws Exception {
    var image = stage.getScene().snapshot(null);
    int width = (int) image.getWidth(), height = (int) image.getHeight();
    int[] pixels = new int[width * height];
    image
        .getPixelReader()
        .getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
    BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    png.setRGB(0, 0, width, height, pixels, 0, width);
    Path directory = Path.of("build/screenshots");
    Files.createDirectories(directory);
    ImageIO.write(png, "png", directory.resolve(name).toFile());
  }

  private void await(BooleanSupplier condition) throws Exception {
    long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
    while (System.nanoTime() < limit) {
      if (fx(condition::getAsBoolean)) return;
      Thread.sleep(20);
    }
    fail("A interface não atingiu o estado esperado. Terminal: " + output());
  }

  @Test
  void richTextEditorRunsWithInteractiveTerminalInput() throws Exception {
    fx(
        () -> {
          assertNotNull(node("codeEditor", CodeArea.class));
          capture("Algora.png");
          node("runButton", Button.class).fire();
          return null;
        });
    await(() -> !node("terminalInput", TextField.class).isDisabled());
    fx(
        () -> {
          TextField input = node("terminalInput", TextField.class);
          input.setText("Ada");
          input.fireEvent(new ActionEvent());
          return null;
        });
    await(() -> node("terminalOutput", TextArea.class).getText().contains("Execução finalizada"));
    assertTrue(output().contains("Bem-vindo ao Algora, Ada!"));
    assertTrue(output().contains("Passo 3"));
    assertEquals("Finalizado", fx(() -> node("executionStatus", Label.class).getText()));
    fx(
        () -> {
          capture("Algora-execucao.png");
          return null;
        });
  }

  @Test
  void debuggerPausesStepsAndCancelsWithoutError() throws Exception {
    fx(
        () -> {
          node("codeEditor", CodeArea.class)
              .replaceText(
                  "algoritmo \"debug\"\n"
                      + "var x:inteiro\n"
                      + "inicio\n"
                      + "escreval(\"um\")\n"
                      + "x <- 2\n"
                      + "fimalgoritmo");
          node("debugButton", Button.class).fire();
          return null;
        });
    await(() -> node("runButton", Button.class).getText().contains("Continuar"));
    fx(
        () -> {
          node("stepButton", Button.class).fire();
          return null;
        });
    await(() -> node("terminalOutput", TextArea.class).getText().contains("um"));
    fx(
        () -> {
          node("stopButton", Button.class).fire();
          return null;
        });
    await(() -> node("stopButton", Button.class).isDisabled());
    assertFalse(output().contains("Erro:"));
  }

  @Test
  void reportsSyntaxErrorsAndRecoversForNextRun() throws Exception {
    fx(
        () -> {
          node("codeEditor", CodeArea.class)
              .replaceText("algoritmo \"erro\"\ninicio\nescreval(\nfimalgoritmo");
          node("runButton", Button.class).fire();
          return null;
        });
    await(() -> node("terminalOutput", TextArea.class).getText().contains("Erro:"));
    fx(
        () -> {
          node("codeEditor", CodeArea.class)
              .replaceText("algoritmo \"ok\"\ninicio\nescreval(\"recuperado\")\nfimalgoritmo");
          node("runButton", Button.class).fire();
          return null;
        });
    await(() -> node("terminalOutput", TextArea.class).getText().contains("Execução finalizada"));
    assertTrue(output().contains("recuperado"));
  }

  @Test
  void completionUsesPortugolKeywordsAndGutterStoresBreakpoint() throws Exception {
    fx(
        () -> {
          CodeArea editor = node("codeEditor", CodeArea.class);
          editor.replaceText("es");
          editor.moveTo(2);
          editor.requestFocus();
          Event.fireEvent(
              editor,
              new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, true, false, false));
          return null;
        });
    await(
        () ->
            Window.getWindows().stream()
                .anyMatch(w -> w instanceof javafx.stage.PopupWindow && w.isShowing()));
    fx(
        () -> {
          ContextMenu popup =
              (ContextMenu)
                  Window.getWindows().stream()
                      .filter(w -> w instanceof ContextMenu && w.isShowing())
                      .findFirst()
                      .orElseThrow();
          MenuItem complete =
              popup.getItems().stream()
                  .filter(i -> i.getText().equals("escreval"))
                  .findFirst()
                  .orElseThrow();
          complete.fire();
          popup.hide();
          assertEquals("escreval", node("codeEditor", CodeArea.class).getText());
          stage.getScene().getAccelerators().get(new KeyCodeCombination(KeyCode.F9)).run();
          assertFalse(stage.getScene().getRoot().lookupAll(".breakpoint-item").isEmpty());
          return null;
        });
  }

  @Test
  void compactLayoutKeepsEditorUsableAndPanelsCanBeReopened() throws Exception {
    fx(
        () -> {
          stage.setWidth(820);
          stage.setHeight(620);
          return null;
        });
    await(
        () ->
            !node("filesPanel", javafx.scene.layout.VBox.class).isManaged()
                && !node("inspectionPanel", javafx.scene.layout.VBox.class).isManaged()
                && node("editorPane", javafx.scene.layout.VBox.class).getWidth() > 700);
    fx(
        () -> {
          capture("Algora-compacto.png");
          node("filesToggle", Button.class).fire();
          return null;
        });
    await(() -> node("filesPanel", javafx.scene.layout.VBox.class).isManaged());
    fx(
        () -> {
          node("inspectionToggle", Button.class).fire();
          return null;
        });
    await(
        () ->
            node("inspectionPanel", javafx.scene.layout.VBox.class).isManaged()
                && !node("filesPanel", javafx.scene.layout.VBox.class).isManaged()
                && node("editorPane", javafx.scene.layout.VBox.class).getWidth() > 480);
    fx(
        () -> {
          assertTrue(node("runButton", Button.class).isVisible());
          assertTrue(javafx.scene.text.Font.getFamilies().contains("IBM Plex Sans"));
          return null;
        });
  }
}
