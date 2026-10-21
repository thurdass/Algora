package io.algora.desktop;

import static io.algora.desktop.ui.IdeControls.*;

import io.algora.desktop.debug.DebuggerPane;
import io.algora.desktop.documents.DocumentManager;
import io.algora.desktop.documents.EditorDocument;
import io.algora.desktop.execution.ExecutionController;
import io.algora.desktop.terminal.TerminalPane;
import java.io.IOException;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
  private final Label position = new Label("Ln 1, Col 1");
  private final Label status = new Label("Pronto");
  private Stage stage;
  private DocumentManager documentManager;
  private ExecutionController execution;
  private VBox filesPanel, inspectionPanel;
  private Button filesToggle, inspectionToggle;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    this.stage = stage;
    documentManager =
        new DocumentManager(
            stage,
            status,
            position,
            () -> execution.isRunning(),
            (d, line) -> execution.toggleBreakpoint(d, line),
            () -> execution.executionDocument(),
            () -> debuggerPane.refreshBreakpoints(documentManager.active()));
    execution = new ExecutionController(documentManager, terminalPane, debuggerPane, status);
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
    root.setTop(header());
    filesPanel = documentManager.sidebar();
    inspectionPanel = debuggerPane.view();
    VBox editor = editorPane();
    VBox rail = new VBox(8, filesToggle, inspectionToggle);
    rail.getStyleClass().add("activity-rail");
    HBox workspace = new HBox(rail, filesPanel, editor, inspectionPanel);
    workspace.setId("workspace");
    HBox.setHgrow(editor, Priority.ALWAYS);
    root.setCenter(workspace);
    root.setBottom(statusbar());
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
    stage
        .widthProperty()
        .addListener(
            (o, old, width) -> {
              showPanel(filesPanel, filesToggle, width.doubleValue() >= 900);
              showPanel(inspectionPanel, inspectionToggle, width.doubleValue() >= 1160);
            });
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN),
            () -> togglePanel(filesPanel));
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(
                KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
            () -> togglePanel(inspectionPanel));
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
    if (args.contains("--smoke-test")) smokeTest();
  }

  void applyTheme(EditorTheme theme) {
    stage.getScene().getRoot().setStyle(theme.css());
    EditorTheme.save(theme);
  }

  private HBox header() {
    ImageView logo =
        new ImageView(
            new Image(
                Objects.requireNonNull(getClass().getResource("algora-logo.png"))
                    .toExternalForm()));
    // Use the emblem from the supplied full lockup; the adjacent label supplies the name.
    logo.setViewport(new Rectangle2D(245, 210, 780, 620));
    logo.setFitWidth(28);
    logo.setFitHeight(24);
    logo.setPreserveRatio(true);
    logo.setSmooth(true);
    logo.setId("projectLogo");
    logo.setAccessibleText("Logo do Algora");
    Label brand = new Label("algora");
    brand.getStyleClass().add("brand");
    Menu file = new Menu("Arquivo");
    MenuItem fresh = new MenuItem("Novo algoritmo    Ctrl+N");
    fresh.setOnAction(e -> documentManager.newFile());
    MenuItem open = new MenuItem("Abrir…    Ctrl+O");
    open.setOnAction(e -> documentManager.open());
    MenuItem save = new MenuItem("Salvar    Ctrl+S");
    save.setOnAction(e -> documentManager.save(documentManager.active(), false));
    MenuItem saveAs = new MenuItem("Salvar como…");
    saveAs.setOnAction(e -> documentManager.save(documentManager.active(), true));
    file.getItems().addAll(fresh, open, save, saveAs);
    Menu edit = new Menu("Editar");
    for (String command : List.of("Desfazer", "Refazer", "Copiar", "Colar", "Selecionar tudo")) {
      MenuItem item = new MenuItem(command);
      item.setOnAction(
          e -> {
            EditorDocument d = documentManager.active();
            if (d == null
                || execution.isRunning()
                    && !command.equals("Copiar")
                    && !command.equals("Selecionar tudo")) return;
            switch (command) {
              case "Desfazer" -> d.editor.undo();
              case "Refazer" -> d.editor.redo();
              case "Copiar" -> d.editor.copy();
              case "Colar" -> d.editor.paste();
              default -> d.editor.selectAll();
            }
          });
      edit.getItems().add(item);
    }
    Menu help = new Menu("Ajuda");
    MenuItem guide = new MenuItem("Guia rápido e atalhos");
    guide.setOnAction(e -> showGuide());
    help.getItems().add(guide);
    Menu view = new Menu("Exibir");
    MenuItem showFiles = new MenuItem("Arquivos");
    showFiles.setOnAction(e -> togglePanel(filesPanel));
    MenuItem showInspector = new MenuItem("Depuração");
    showInspector.setOnAction(e -> togglePanel(inspectionPanel));
    Menu themes = new Menu("Tema de cores");
    ToggleGroup themeGroup = new ToggleGroup();
    for (EditorTheme theme : EditorTheme.values()) {
      RadioMenuItem item = new RadioMenuItem(theme.label);
      item.setToggleGroup(themeGroup);
      item.setSelected(theme == EditorTheme.saved());
      item.setOnAction(e -> applyTheme(theme));
      themes.getItems().add(item);
    }
    view.getItems().addAll(showFiles, showInspector, new SeparatorMenuItem(), themes);
    MenuBar menu = new MenuBar(file, edit, view, help);
    Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    filesToggle =
        iconButton("files", "Mostrar ou ocultar arquivos (Ctrl+B)", () -> togglePanel(filesPanel));
    filesToggle.setId("filesToggle");
    inspectionToggle =
        iconButton(
            "debug",
            "Mostrar ou ocultar depuração (Ctrl+Shift+D)",
            () -> togglePanel(inspectionPanel));
    inspectionToggle.setId("inspectionToggle");
    HBox box = new HBox(9, logo, brand, menu, space);
    box.getStyleClass().add("header");
    return box;
  }

  private void showPanel(VBox panel, Button toggle, boolean visible) {
    panel.setVisible(visible);
    panel.setManaged(visible);
    toggle.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), visible);
  }

  private void togglePanel(VBox panel) {
    boolean visible = !panel.isVisible();
    if (visible && stage.getWidth() < 1100) {
      VBox other = panel == filesPanel ? inspectionPanel : filesPanel;
      showPanel(other, other == filesPanel ? filesToggle : inspectionToggle, false);
    }
    showPanel(panel, panel == filesPanel ? filesToggle : inspectionToggle, visible);
  }

  private VBox editorPane() {
    documentManager.tabs().setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    documentManager.tabs().getStyleClass().add("editor-tabs");
    VBox.setVgrow(documentManager.tabs(), Priority.ALWAYS);
    VBox top = new VBox(execution.toolbar().view(), documentManager.tabs());
    VBox.setVgrow(documentManager.tabs(), Priority.ALWAYS);
    SplitPane split = new SplitPane(top, terminalPane.view());
    split.setOrientation(Orientation.VERTICAL);
    split.setDividerPositions(.73);
    split.setId("editorSplit");
    split.getStyleClass().add("editor-split");
    VBox.setVgrow(split, Priority.ALWAYS);
    VBox box = new VBox(split);
    box.setMinWidth(360);
    box.setId("editorPane");
    return box;
  }

  private HBox statusbar() {
    Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    HBox box =
        new HBox(
            24,
            status,
            new Label("Algora 0.2"),
            space,
            position,
            new Label("UTF-8"),
            new Label("Portugol"));
    box.getStyleClass().add("statusbar");
    return box;
  }

  /** Exercise the installed runtime without requiring an external JDK or test framework. */
  private void smokeTest() {
    javafx.animation.Timeline pulse = new javafx.animation.Timeline();
    javafx.animation.PauseTransition deadline =
        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(15));
    deadline.setOnFinished(
        event -> {
          System.err.println("ALGORA_SMOKE_FAILED: timeout");
          stop();
          Platform.exit();
          System.exit(1);
        });
    pulse
        .getKeyFrames()
        .add(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(80),
                event -> {
                  if (!terminalPane.input().isDisabled()) {
                    terminalPane.input().setText("Java 21");
                    terminalPane.input().fireEvent(new javafx.event.ActionEvent());
                  }
                  if (terminalPane.output().getText().contains("Erro:")) {
                    System.err.println("ALGORA_SMOKE_FAILED: " + terminalPane.output().getText());
                    stop();
                    Platform.exit();
                    System.exit(1);
                  }
                  if (terminalPane.output().getText().contains("Execução finalizada")) {
                    if (!terminalPane.output().getText().contains("Bem-vindo ao Algora, Java 21!")
                        || !terminalPane.output().getText().contains("Passo 3")) {
                      System.err.println("ALGORA_SMOKE_FAILED: unexpected output");
                      System.exit(1);
                    }
                    pulse.stop();
                    deadline.stop();
                    System.out.println("ALGORA_SMOKE_OK");
                    stop();
                    Platform.exit();
                  }
                }));
    pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
    deadline.play();
    pulse.play();
    execution.run(false);
  }

  private void showGuide() {
    Alert alert =
        new Alert(
            Alert.AlertType.INFORMATION,
            "F5  Executar / continuar\n"
                + "F9  Alternar ponto de parada\n"
                + "F10  Próximo passo\n"
                + "Ctrl+Espaço  Autocompletar e modelos\n"
                + "Ctrl+O  Abrir arquivo .alg\n"
                + "Ctrl+S  Salvar em UTF-8\n"
                + "Ctrl+N  Novo algoritmo\n"
                + "Ctrl+B  Mostrar ou ocultar arquivos\n"
                + "Ctrl+Shift+D  Mostrar ou ocultar depuração\n\n"
                + "Clique na margem esquerda para adicionar pontos de parada.\n"
                + "O terminal aceita os valores solicitados por leia().\n"
                + "Arquivos legados Windows-1252 são reconhecidos na abertura.",
            ButtonType.OK);
    alert.setHeaderText("Do primeiro passo à próxima ideia.");
    alert.initOwner(stage);
    alert.show();
  }

  @Override
  public void stop() {
    if (execution != null) execution.stop();
  }
}
