package io.algora.desktop;

import io.algora.desktop.debug.DebuggerPane;
import io.algora.desktop.documents.DocumentManager;
import io.algora.desktop.documents.EditorDocument;
import io.algora.desktop.execution.ExecutionController;
import io.algora.desktop.terminal.TerminalPane;
import io.algora.desktop.ui.ApplicationHeader;
import io.algora.desktop.ui.StatusBar;
import io.algora.desktop.ui.WorkspaceLayout;
import io.algora.desktop.verification.PackagedApplicationProbe;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** Desktop shell; all language execution belongs to the independent interpreter module. */
public final class AlgoraApplication extends Application {
  private static final String SAMPLE =
      "algoritmo \"Boas-vindas\"\n"
          + "// Todo grande projeto começa com uma ideia.\n"
          + "var\n"
          + "   nome: caractere\n"
          + "   contador: inteiro\n"
          + "inicio\n"
          + "   escreval(\"Olá! Vamos criar algo incrível?\")\n"
          + "   escreva(\"Qual é o seu nome? \")\n"
          + "   leia(nome)\n"
          + "   escreval(\"Bem-vindo ao Algora, \", nome, \"!\")\n\n"
          + "   para contador de 1 ate 3 faca\n"
          + "      escreval(\"Sua próxima ideia começa aqui. Passo \", contador)\n"
          + "   fimpara\n"
          + "fimalgoritmo\n";
  private final TerminalPane terminalPane = new TerminalPane();
  private final DebuggerPane debuggerPane = new DebuggerPane();
  private final StatusBar statusBar = new StatusBar();
  private Stage stage;
  private DocumentManager documentManager;
  private ExecutionController execution;
  private WorkspaceLayout workspace;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    this.stage = stage;
    documentManager =
        new DocumentManager(
            stage,
            statusBar.status(),
            statusBar.position(),
            () -> execution.isRunning(),
            (d, line) -> execution.toggleBreakpoint(d, line),
            () -> execution.executionDocument(),
            () -> debuggerPane.refreshBreakpoints(documentManager.active()));
    execution =
        new ExecutionController(documentManager, terminalPane, debuggerPane, statusBar.status());
    for (String file :
        List.of("IBMPlexSans-Regular.ttf", "IBMPlexSans-Medium.ttf", "IBMPlexMono-Regular.ttf")) {
      try (var stream = getClass().getResourceAsStream("fonts/" + file)) {
        if (stream == null || Font.loadFont(stream, 13) == null)
          throw new IOException("Fonte ausente: " + file);
      } catch (IOException e) {
        throw new IllegalStateException("Não foi possível carregar a tipografia", e);
      }
    }
    BorderPane root = new BorderPane();
    root.getStyleClass().add("root-shell");
    ApplicationHeader header =
        new ApplicationHeader(
            stage,
            documentManager,
            execution::isRunning,
            () -> workspace.toggleFiles(),
            () -> workspace.toggleInspector(),
            themeMenu());
    root.setTop(header.view());
    workspace =
        new WorkspaceLayout(
            stage,
            documentManager.tabs(),
            execution.toolbar().view(),
            terminalPane.view(),
            documentManager.sidebar(),
            debuggerPane.view(),
            header.filesToggle(),
            header.inspectionToggle());
    root.setCenter(workspace.view());
    root.setBottom(statusBar.view());
    Scene scene = new Scene(root, 1360, 860);
    scene
        .getStylesheets()
        .add(Objects.requireNonNull(getClass().getResource("algora.css")).toExternalForm());
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
            () -> documentManager.save(documentManager.active(), false));
    scene
        .getAccelerators()
        .put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), documentManager::open);
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
            documentManager::newFile);
    scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> execution.run(false));
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F9),
            () -> {
              EditorDocument d = documentManager.active();
              if (d != null) execution.toggleBreakpoint(d, d.editor.getCurrentParagraph() + 1);
            });
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F10),
            () -> {
              execution.step();
            });
    stage.setScene(scene);
    applyTheme(EditorTheme.saved());
    stage.setTitle("Algora — seu próximo algoritmo");
    stage.setMinWidth(760);
    stage.setMinHeight(560);
    workspace.installResizeListener();
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN),
            () -> workspace.toggleFiles());
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(
                KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
            () -> workspace.toggleInspector());
    stage.setOnCloseRequest(
        event -> {
          for (EditorDocument d : new ArrayList<>(documentManager.all()))
            if (!documentManager.confirmDiscard(d)) {
              event.consume();
              return;
            }
          execution.stop();
        });
    documentManager.installSelectionListener();
    documentManager.addDocument("boas-vindas.alg", null, SAMPLE);
    stage.show();
    List<String> args = getParameters() == null ? List.of() : getParameters().getRaw();
    for (String arg : args) if (arg.endsWith(".alg")) documentManager.load(Path.of(arg));
    if (args.contains("--smoke-test"))
      PackagedApplicationProbe.verify(terminalPane, this::stop, () -> execution.run(false));
  }

  void applyTheme(EditorTheme theme) {
    stage.getScene().getRoot().setStyle(theme.css());
    EditorTheme.save(theme);
  }

  private Menu themeMenu() {
    Menu themes = new Menu("Tema de cores");
    ToggleGroup themeGroup = new ToggleGroup();
    for (EditorTheme theme : EditorTheme.values()) {
      RadioMenuItem item = new RadioMenuItem(theme.label);
      item.setToggleGroup(themeGroup);
      item.setSelected(theme == EditorTheme.saved());
      item.setOnAction(e -> applyTheme(theme));
      themes.getItems().add(item);
    }
    return themes;
  }

  @Override
  public void stop() {
    if (execution != null) execution.stop();
  }
}
