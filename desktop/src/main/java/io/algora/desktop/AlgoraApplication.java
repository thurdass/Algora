package io.algora.desktop;

import io.algora.interpreter.AlgoraEngine;
import io.algora.interpreter.DebugSession;
import io.algora.interpreter.Snapshot;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpansBuilder;

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
  private static final String[] KEYWORDS =
      ("algoritmo var inicio fimalgoritmo inteiro real caractere caracter logico verdadeiro falso"
              + " se entao senao fimse para de ate passo faca fimpara enquanto fimenquanto repita"
              + " escolha caso outrocaso fimescolha procedimento fimprocedimento funcao fimfuncao"
              + " retorne leia escreva escreval e ou nao div mod interrompa")
          .split(" ");
  private static final Pattern TOKENS =
      Pattern.compile(
          "(?<COMMENT>//[^\\n]*)|(?<STRING>\"[^\"\\n]*\"|'[^'\\n]*')|(?<KEYWORD>\\b(?:"
              + String.join("|", KEYWORDS)
              + ")\\b)|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private final TabPane tabs = new TabPane();
  private final ListView<Document> explorer = new ListView<>();
  private final Map<Tab, Document> documents = new LinkedHashMap<>();
  private final TextArea terminal = new TextArea();
  private final TextField input = new TextField();
  private final VBox variables = new VBox(7);
  private final VBox breakpointList = new VBox(6);
  private final Label runStatus = new Label("Pronto");
  private final Label position = new Label("Ln 1, Col 1");
  private final Label status = new Label("Pronto");
  private final BlockingQueue<String> inputs = new LinkedBlockingQueue<>();
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private DebugSession session;
  private volatile boolean running;
  private boolean switching;
  private Document executionDocument;
  private Stage stage;
  private final AtomicReference<Snapshot> latestSnapshot = new AtomicReference<>();
  private final AtomicBoolean snapshotScheduled = new AtomicBoolean();
  private final StringBuilder outputBuffer = new StringBuilder();
  private final AtomicBoolean outputScheduled = new AtomicBoolean();
  private Button execute, debug, pause, step, stop;
  private int nextFile = 1;
  private VBox filesPanel, inspectionPanel;
  private Button filesToggle, inspectionToggle;

  private final class Document {
    final CodeArea editor = new CodeArea();
    final Tab tab = new Tab();
    final Set<Integer> breakpoints = new TreeSet<>();
    final String title;
    Path path;
    String saved;
    int executionLine;
    final ContextMenu completions = new ContextMenu();

    Document(String title, Path path, String content) {
      this.title = title;
      this.path = path;
      this.saved = content;
      editor.getStyleClass().add("code-editor");
      editor.setId("codeEditor");
      editor.setParagraphGraphicFactory(
          line -> {
            Label point =
                new Label(
                    breakpoints.contains(line + 1) ? "●" : executionLine == line + 1 ? "▶" : " ");
            point.setMinWidth(22);
            point.getStyleClass().add("gutter-marker");
            if (breakpoints.contains(line + 1)) point.getStyleClass().add("breakpoint");
            if (executionLine == line + 1) point.getStyleClass().add("current-marker");
            point.setOnMouseClicked(e -> toggleBreakpoint(this, line + 1));
            return new HBox(point, LineNumberFactory.get(editor).apply(line));
          });
      editor.replaceText(content);
      highlight(this);
      editor
          .textProperty()
          .addListener(
              (obs, old, text) -> {
                highlight(this);
                updateTitle();
                refreshExplorer();
              });
      editor
          .caretPositionProperty()
          .addListener(
              (o, a, b) -> {
                position.setText(
                    "Ln "
                        + (editor.getCurrentParagraph() + 1)
                        + ", Col "
                        + (editor.getCaretColumn() + 1));
              });
      editor.setOnKeyPressed(
          event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
              showCompletions(this);
              event.consume();
            }
            if (event.getCode() == KeyCode.ESCAPE) completions.hide();
            if (event.getCode() == KeyCode.ENTER && !completions.isShowing()) {
              String paragraph = editor.getParagraph(editor.getCurrentParagraph()).getText();
              String prefix =
                  paragraph.substring(0, Math.min(editor.getCaretColumn(), paragraph.length()));
              String indent = prefix.substring(0, prefix.length() - prefix.stripLeading().length());
              if (prefix.stripTrailing().matches("(?i).*(entao|faca|inicio)$")) indent += "   ";
              editor.insertText(editor.getCaretPosition(), "\n" + indent);
              event.consume();
            }
          });
      tab.setGraphic(Icons.create("file"));
      tab.setContent(new VirtualizedScrollPane<>(editor));
      updateTitle();
      tab.setOnCloseRequest(
          event -> {
            if (running || !confirmDiscard(this)) event.consume();
          });
      tab.setOnClosed(
          event -> {
            documents.remove(tab);
            refreshExplorer();
            if (documents.isEmpty())
              addDocument(
                  "algoritmo.alg",
                  null,
                  "algoritmo \"Novo algoritmo\"\nvar\ninicio\n\nfimalgoritmo\n");
          });
    }

    String name() {
      return path == null ? title : path.getFileName().toString();
    }

    boolean dirty() {
      return !editor.getText().equals(saved);
    }

    void updateTitle() {
      tab.setText(name() + (dirty() ? "  ●" : ""));
    }

    @Override
    public String toString() {
      return name() + (dirty() ? "  •" : "");
    }
  }

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    this.stage = stage;
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
    filesPanel = sidebar();
    inspectionPanel = inspector();
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
            () -> save(active(), false));
    scene
        .getAccelerators()
        .put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::open);
    scene
        .getAccelerators()
        .put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::newFile);
    scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> run(false));
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F9),
            () -> {
              Document d = active();
              if (d != null) toggleBreakpoint(d, d.editor.getCurrentParagraph() + 1);
            });
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F10),
            () -> {
              if (session != null) session.step();
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
          for (Document d : new ArrayList<>(documents.values()))
            if (!confirmDiscard(d)) {
              event.consume();
              return;
            }
          if (session != null) session.stop();
          executor.shutdownNow();
        });
    tabs.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, old, value) -> {
              if (running
                  && !switching
                  && executionDocument != null
                  && value != executionDocument.tab) {
                switching = true;
                tabs.getSelectionModel().select(executionDocument.tab);
                switching = false;
                return;
              }
              refreshBreakpoints();
              if (value != null) explorer.getSelectionModel().select(documents.get(value));
            });
    addDocument("boas-vindas.alg", null, SAMPLE);
    stage.show();
    List<String> args = getParameters() == null ? List.of() : getParameters().getRaw();
    for (String arg : args) if (arg.endsWith(".alg")) load(Path.of(arg));
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
    fresh.setOnAction(e -> newFile());
    MenuItem open = new MenuItem("Abrir…    Ctrl+O");
    open.setOnAction(e -> open());
    MenuItem save = new MenuItem("Salvar    Ctrl+S");
    save.setOnAction(e -> save(active(), false));
    MenuItem saveAs = new MenuItem("Salvar como…");
    saveAs.setOnAction(e -> save(active(), true));
    file.getItems().addAll(fresh, open, save, saveAs);
    Menu edit = new Menu("Editar");
    for (String command : List.of("Desfazer", "Refazer", "Copiar", "Colar", "Selecionar tudo")) {
      MenuItem item = new MenuItem(command);
      item.setOnAction(
          e -> {
            Document d = active();
            if (d == null
                || running && !command.equals("Copiar") && !command.equals("Selecionar tudo"))
              return;
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

  private VBox sidebar() {
    Label title = heading("Explorador");
    Button plus = iconButton("plus", "Novo algoritmo (Ctrl+N)", this::newFile);
    Button open = iconButton("files", "Abrir arquivo (Ctrl+O)", this::open);
    Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    HBox top = new HBox(5, title, space, open, plus);
    top.getStyleClass().add("panel-heading");
    Label project = new Label("▾ Algoritmos abertos");
    project.setGraphic(Icons.create("files"));
    project.setGraphicTextGap(8);
    project.getStyleClass().add("project-heading");
    explorer.getStyleClass().add("explorer");
    explorer.setCellFactory(
        list ->
            new ListCell<>() {
              @Override
              protected void updateItem(Document document, boolean empty) {
                super.updateItem(document, empty);
                setText(empty || document == null ? null : document.toString());
                setGraphic(empty || document == null ? null : Icons.create("file"));
                setGraphicTextGap(9);
              }
            });
    explorer.setOnMouseClicked(
        e -> {
          Document d = explorer.getSelectionModel().getSelectedItem();
          if (d != null && !running) tabs.getSelectionModel().select(d.tab);
        });
    VBox.setVgrow(explorer, Priority.ALWAYS);
    Button fresh = button("Novo arquivo", "Novo algoritmo (Ctrl+N)", this::newFile);
    fresh.setGraphic(Icons.create("plus"));
    fresh.getStyleClass().add("new-file");
    VBox box = new VBox(top, project, explorer, fresh);
    box.setId("filesPanel");
    box.getStyleClass().add("sidebar");
    box.setPrefWidth(220);
    box.setMinWidth(190);
    box.setMaxWidth(220);
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
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    tabs.getStyleClass().add("editor-tabs");
    VBox.setVgrow(tabs, Priority.ALWAYS);
    execute = button("Executar   F5", "Executar ou continuar", () -> run(false));
    execute.setGraphic(Icons.create("play"));
    execute.getStyleClass().add("run-button");
    execute.setId("runButton");
    debug = button("Depurar", "Depurar desde a primeira instrução", () -> run(true));
    debug.setGraphic(Icons.create("debug"));
    pause =
        iconButton(
            "pause",
            "Pausar",
            () -> {
              if (session != null) session.pause();
            });
    step =
        iconButton(
            "step",
            "Próximo passo (F10)",
            () -> {
              if (session != null) session.step();
            });
    stop = iconButton("stop", "Parar execução", this::stopExecution);
    debug.setId("debugButton");
    step.setId("stepButton");
    stop.setId("stopButton");
    pause.setDisable(true);
    step.setDisable(true);
    stop.setDisable(true);
    Label breadcrumb = new Label("Portugol");
    breadcrumb.getStyleClass().add("subtle");
    Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    HBox toolbar = new HBox(9, breadcrumb, space, execute, debug, pause, step, stop);
    toolbar.getStyleClass().add("editor-toolbar");
    VBox top = new VBox(toolbar, tabs);
    VBox.setVgrow(tabs, Priority.ALWAYS);
    runStatus.setId("executionStatus");
    terminal.setId("terminalOutput");
    terminal.setEditable(false);
    terminal.setWrapText(true);
    terminal.getStyleClass().add("terminal-output");
    terminal.setText("A saída do algoritmo aparece aqui.\nPressione F5 para executar.\n");
    VBox.setVgrow(terminal, Priority.ALWAYS);
    input.setId("terminalInput");
    input.setPromptText("Digite um valor e pressione Enter");
    input.setDisable(true);
    input.visibleProperty().bind(input.disabledProperty().not());
    input.managedProperty().bind(input.visibleProperty());
    input.getStyleClass().add("terminal-input");
    input.setOnAction(
        e -> {
          if (!input.isDisabled()) {
            String text = input.getText();
            appendTerminal(text + "\n");
            input.clear();
            input.setDisable(true);
            inputs.offer(text);
          }
        });
    Label label = new Label("Terminal");
    label.setGraphic(Icons.create("terminal"));
    label.setGraphicTextGap(8);
    label.getStyleClass().add("terminal-label");
    Region flex = new Region();
    HBox.setHgrow(flex, Priority.ALWAYS);
    Button clear = iconButton("close", "Limpar terminal", () -> terminal.clear());
    HBox terminalHead = new HBox(10, label, flex, runStatus, clear);
    terminalHead.getStyleClass().add("terminal-heading");
    VBox lower = new VBox(terminalHead, terminal, input);
    lower.setMinHeight(150);
    lower.getStyleClass().add("terminal-panel");
    SplitPane split = new SplitPane(top, lower);
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

  private VBox inspector() {
    VBox box = new VBox();
    box.setId("inspectionPanel");
    box.getStyleClass().add("inspector");
    box.setPrefWidth(240);
    box.setMinWidth(215);
    box.setMaxWidth(240);
    Label title = heading("Depuração");
    HBox top = new HBox(title);
    top.getStyleClass().add("panel-heading");
    Label vars = heading("Variáveis");
    vars.getStyleClass().add("section-heading");
    variables.setId("variablesTable");
    variables.getStyleClass().add("variables");
    emptyVariables();
    Label points = heading("Pontos de parada");
    points.getStyleClass().add("section-heading");
    breakpointList.getStyleClass().add("breakpoints");
    box.getChildren().addAll(top, vars, variables, points, breakpointList);
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

  private Label heading(String text) {
    Label l = new Label(text);
    l.getStyleClass().add("heading");
    return l;
  }

  private Button button(String text, String tooltip, Runnable action) {
    Button b = new Button(text);
    b.setTooltip(new Tooltip(tooltip));
    b.setOnAction(e -> action.run());
    b.setFocusTraversable(true);
    b.setAccessibleText(tooltip);
    return b;
  }

  private Button iconButton(String icon, String tooltip, Runnable action) {
    Button b = button("", tooltip, action);
    b.setGraphic(Icons.create(icon));
    b.getStyleClass().add("icon-button");
    return b;
  }

  private Document active() {
    return documents.get(tabs.getSelectionModel().getSelectedItem());
  }

  private void addDocument(String name, Path path, String content) {
    Document d = new Document(name, path, content);
    documents.put(d.tab, d);
    tabs.getTabs().add(d.tab);
    tabs.getSelectionModel().select(d.tab);
    refreshExplorer();
    Platform.runLater(d.editor::requestFocus);
  }

  private void newFile() {
    if (running) return;
    addDocument(
        "algoritmo-" + (nextFile++) + ".alg",
        null,
        "algoritmo \"Novo algoritmo\"\nvar\ninicio\n   \nfimalgoritmo\n");
  }

  private void refreshExplorer() {
    explorer.getItems().setAll(documents.values());
    if (active() != null) explorer.getSelectionModel().select(active());
  }

  private FileChooser chooser() {
    FileChooser f = new FileChooser();
    f.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Algoritmos VisualG (*.alg)", "*.alg"));
    f.getExtensionFilters().add(new FileChooser.ExtensionFilter("Todos os arquivos", "*.*"));
    return f;
  }

  private void open() {
    if (running) return;
    java.io.File file = chooser().showOpenDialog(stage);
    if (file != null) load(file.toPath());
  }

  private void load(Path path) {
    try {
      byte[] bytes = Files.readAllBytes(path);
      String source;
      try {
        source =
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
      } catch (CharacterCodingException e) {
        source = Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString();
      }
      if (source.startsWith("\uFEFF")) source = source.substring(1);
      addDocument(path.getFileName().toString(), path, source);
    } catch (IOException e) {
      error("Não foi possível abrir o arquivo", e);
    }
  }

  private boolean save(Document d, boolean choose) {
    if (d == null) return false;
    try {
      Path target = d.path;
      if (target == null || choose) {
        FileChooser dialog = chooser();
        dialog.setInitialFileName(d.name());
        java.io.File selected = dialog.showSaveDialog(stage);
        if (selected == null) return false;
        target = selected.toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".alg"))
          target = target.resolveSibling(target.getFileName() + ".alg");
      }
      Files.writeString(target, d.editor.getText(), StandardCharsets.UTF_8);
      d.path = target;
      d.saved = d.editor.getText();
      d.updateTitle();
      refreshExplorer();
      status.setText("Arquivo salvo");
      return true;
    } catch (IOException e) {
      error("Não foi possível salvar", e);
      return false;
    }
  }

  private boolean confirmDiscard(Document d) {
    if (!d.dirty()) return true;
    Alert alert =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Salvar alterações em " + d.name() + "?",
            ButtonType.YES,
            ButtonType.NO,
            ButtonType.CANCEL);
    alert.initOwner(stage);
    alert.setHeaderText("Seu algoritmo tem alterações não salvas");
    ButtonType choice = alert.showAndWait().orElse(ButtonType.CANCEL);
    return choice == ButtonType.NO || choice == ButtonType.YES && save(d, false);
  }

  private void error(String title, Throwable throwable) {
    Alert alert =
        new Alert(
            Alert.AlertType.ERROR,
            Objects.toString(throwable.getMessage(), throwable.toString()),
            ButtonType.OK);
    alert.setHeaderText(title);
    alert.initOwner(stage);
    alert.show();
  }

  private void highlight(Document d) {
    String text = d.editor.getText();
    var matcher = TOKENS.matcher(text);
    StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
    int end = 0;
    while (matcher.find()) {
      spans.add(List.of(), matcher.start() - end);
      String kind =
          matcher.group("COMMENT") != null
              ? "comment"
              : matcher.group("STRING") != null
                  ? "string"
                  : matcher.group("NUMBER") != null ? "number" : "keyword";
      spans.add(List.of(kind), matcher.end() - matcher.start());
      end = matcher.end();
    }
    spans.add(List.of(), text.length() - end);
    d.editor.setStyleSpans(0, spans.create());
  }

  private void showCompletions(Document d) {
    if (running) return;
    int caret = d.editor.getCaretPosition();
    String before = d.editor.getText(0, caret);
    int start = caret;
    while (start > 0 && Character.isLetter(before.charAt(start - 1))) start--;
    String prefix = before.substring(start).toLowerCase(Locale.ROOT);
    final int from = start;
    d.completions.getItems().clear();
    for (String keyword : KEYWORDS)
      if (keyword.startsWith(prefix)) {
        MenuItem item = new MenuItem(keyword);
        item.setOnAction(
            e -> {
              String text =
                  switch (keyword) {
                    case "se" -> "se condicao entao\n   \nfimse";
                    case "para" -> "para i de 1 ate 10 faca\n   \nfimpara";
                    case "enquanto" -> "enquanto condicao faca\n   \nfimenquanto";
                    default -> keyword;
                  };
              d.editor.replaceText(from, caret, text);
            });
        d.completions.getItems().add(item);
      }
    d.editor
        .getCaretBounds()
        .ifPresent(bounds -> d.completions.show(d.editor, bounds.getMinX(), bounds.getMaxY() + 4));
  }

  private void toggleBreakpoint(Document d, int line) {
    if (!d.breakpoints.add(line)) d.breakpoints.remove(line);
    if (session != null && executionDocument == d)
      session.setBreakpoints(Set.copyOf(d.breakpoints));
    refreshGutter(d);
    refreshBreakpoints();
  }

  private void refreshGutter(Document d) {
    for (int i = 0; i < d.editor.getParagraphs().size(); i++) d.editor.recreateParagraphGraphic(i);
  }

  private void refreshBreakpoints() {
    breakpointList.getChildren().clear();
    Document d = active();
    if (d == null || d.breakpoints.isEmpty()) {
      Label hint = new Label("Nenhum ponto de parada.\nUse a margem do editor ou F9.");
      hint.setWrapText(true);
      hint.getStyleClass().add("hint");
      breakpointList.getChildren().add(hint);
      return;
    }
    for (int line : d.breakpoints) {
      Button b =
          button(
              "●  " + d.name() + " :" + line,
              "Ir para ponto de parada",
              () -> {
                d.editor.moveTo(Math.min(line - 1, d.editor.getParagraphs().size() - 1), 0);
                d.editor.requestFollowCaret();
              });
      b.getStyleClass().add("breakpoint-item");
      breakpointList.getChildren().add(b);
    }
  }

  private void emptyVariables() {
    Label empty = new Label("Execute ou depure para\ninspecionar as variáveis.");
    empty.getStyleClass().add("empty-state");
    variables.getChildren().setAll(empty);
  }

  private void run(boolean debugMode) {
    if (running) {
      if (session != null) session.resume();
      return;
    }
    Document d = active();
    if (d == null) return;
    executionDocument = d;
    inputs.clear();
    terminal.clear();
    variables.getChildren().clear();
    setRunning(true);
    appendTerminal("Executando " + d.name() + "\n\n");
    String source = d.editor.getText();
    executor.submit(
        () -> {
          try {
            session =
                AlgoraEngine.createSession(
                    source,
                    () -> {
                      Platform.runLater(
                          () -> {
                            runStatus.setText("Aguardando entrada");
                            input.setDisable(false);
                            input.requestFocus();
                          });
                      return inputs.take();
                    },
                    this::appendOutput,
                    this::scheduleSnapshot);
            session.setBreakpoints(Set.copyOf(d.breakpoints));
            session
                .start(debugMode)
                .whenComplete(
                    (result, exception) ->
                        Platform.runLater(
                            () -> {
                              if (exception != null) {
                                appendTerminal(
                                    "\nErro: " + rootCause(exception).getMessage() + "\n");
                                status.setText("Erro na execução");
                                runStatus.setText("Erro");
                              } else
                                appendTerminal(
                                    "\n\n"
                                        + (result.status().name().equals("STOPPED")
                                            ? "Execução interrompida."
                                            : "Execução finalizada.")
                                        + "\n");
                              if (exception == null) {
                                String finalStatus =
                                    result.status().name().equals("STOPPED")
                                        ? "Interrompido"
                                        : "Finalizado";
                                runStatus.setText(finalStatus);
                                status.setText(finalStatus);
                              }
                              setRunning(false);
                            }));
          } catch (Throwable exception) {
            Platform.runLater(
                () -> {
                  appendTerminal("Erro: " + exception.getMessage() + "\n");
                  status.setText("Erro no algoritmo");
                  runStatus.setText("Erro");
                  setRunning(false);
                });
          }
        });
  }

  private void appendTerminal(String text) {
    terminal.appendText(text);
    terminal.positionCaret(terminal.getLength());
    terminal.setScrollTop(Double.MAX_VALUE);
  }

  private void appendOutput(String text) {
    synchronized (outputBuffer) {
      outputBuffer.append(text);
    }
    if (outputScheduled.compareAndSet(false, true))
      Platform.runLater(
          () -> {
            String pending;
            synchronized (outputBuffer) {
              pending = outputBuffer.toString();
              outputBuffer.setLength(0);
              outputScheduled.set(false);
            }
            if (pending.contains("\u001b[2J")) {
              terminal.clear();
              pending = pending.substring(pending.lastIndexOf("\u001b[2J") + 4);
            }
            appendTerminal(pending.replaceAll("\\x1b\\[[0-9;]*[A-Za-z]", ""));
          });
  }

  private void scheduleSnapshot(Snapshot snapshot) {
    latestSnapshot.set(snapshot);
    if (snapshotScheduled.compareAndSet(false, true))
      Platform.runLater(
          () -> {
            snapshotScheduled.set(false);
            Snapshot latest = latestSnapshot.getAndSet(null);
            if (latest != null) updateSnapshot(latest);
          });
  }

  private Throwable rootCause(Throwable e) {
    while (e.getCause() != null) e = e.getCause();
    return e;
  }

  private void updateSnapshot(Snapshot snapshot) {
    String state = snapshot.status().name();
    boolean paused = state.equals("PAUSED");
    runStatus.setText(
        paused
            ? "Pausado na linha " + snapshot.line()
            : ""
                + switch (state) {
                  case "RUNNING" -> "Em execução";
                  case "COMPLETED" -> "Finalizado";
                  case "ERROR" -> "Erro";
                  case "STOPPED" -> "Interrompido";
                  default -> "Pronto";
                });
    status.setText(runStatus.getText());
    execute.setText(paused ? "Continuar   F5" : "Executar   F5");
    variables.getChildren().clear();
    snapshot
        .variables()
        .forEach(
            (name, value) -> {
              Label key = new Label(name);
              key.getStyleClass().add("variable-name");
              Label val = new Label(format(value));
              val.getStyleClass().add("variable-value");
              val.setMaxWidth(125);
              val.setTooltip(new Tooltip(format(value)));
              Region spacer = new Region();
              HBox.setHgrow(spacer, Priority.ALWAYS);
              variables.getChildren().add(new HBox(5, key, spacer, val));
            });
    if (snapshot.variables().isEmpty()) emptyVariables();
    if (executionDocument != null) {
      executionDocument.executionLine = paused ? snapshot.line() : 0;
      refreshGutter(executionDocument);
      if (paused) {
        int line =
            Math.max(
                0,
                Math.min(snapshot.line() - 1, executionDocument.editor.getParagraphs().size() - 1));
        executionDocument.editor.moveTo(line, 0);
        executionDocument.editor.requestFollowCaret();
      }
    }
  }

  private String format(Object value) {
    if (value instanceof String s) return '"' + s + '"';
    if (value instanceof Map<?, ?> map) return map.toString();
    if (value instanceof Object[] array) return Arrays.deepToString(array);
    return String.valueOf(value);
  }

  private void setRunning(boolean value) {
    running = value;
    if (value) {
      runStatus.setText("Em execução");
      status.setText("Em execução");
    }
    for (Document d : documents.values()) d.editor.setEditable(!value);
    debug.setDisable(value);
    pause.setDisable(!value);
    step.setDisable(!value);
    stop.setDisable(!value);
    if (!value) {
      input.setDisable(true);
      if (executionDocument != null) {
        executionDocument.executionLine = 0;
        refreshGutter(executionDocument);
      }
    }
  }

  private void stopExecution() {
    if (session != null) session.stop();
    input.setDisable(true);
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
                  if (!input.isDisabled()) {
                    input.setText("Java 21");
                    input.fireEvent(new javafx.event.ActionEvent());
                  }
                  if (terminal.getText().contains("Erro:")) {
                    System.err.println("ALGORA_SMOKE_FAILED: " + terminal.getText());
                    stop();
                    Platform.exit();
                    System.exit(1);
                  }
                  if (terminal.getText().contains("Execução finalizada")) {
                    if (!terminal.getText().contains("Bem-vindo ao Algora, Java 21!")
                        || !terminal.getText().contains("Passo 3")) {
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
    run(false);
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
    if (session != null) session.stop();
    executor.shutdownNow();
  }
}
