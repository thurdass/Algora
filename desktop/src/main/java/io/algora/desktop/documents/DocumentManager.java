package io.algora.desktop.documents;

import static io.algora.desktop.ui.components.IdeControls.*;

import io.algora.desktop.ui.components.Icons;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Open documents, explorer selection and existing file/dialog lifecycle. */
public final class DocumentManager {
  private final TabPane tabs = new TabPane();
  private final ListView<EditorDocument> explorer = new ListView<>();
  private final Map<Tab, EditorDocument> documents = new LinkedHashMap<>();
  private final Stage stage;
  private final Label status, position;
  private final BooleanSupplier running;
  private final BiConsumer<EditorDocument, Integer> toggleBreakpoint;
  private final Supplier<EditorDocument> executionDocument;
  private final Runnable selectionChanged;
  private boolean switching;
  private int nextFile = 1;

  public DocumentManager(
      Stage stage,
      Label status,
      Label position,
      BooleanSupplier running,
      BiConsumer<EditorDocument, Integer> toggleBreakpoint,
      Supplier<EditorDocument> executionDocument,
      Runnable selectionChanged) {
    this.stage = stage;
    this.status = status;
    this.position = position;
    this.running = running;
    this.toggleBreakpoint = toggleBreakpoint;
    this.executionDocument = executionDocument;
    this.selectionChanged = selectionChanged;
  }

  public TabPane tabs() {
    return tabs;
  }

  public Collection<EditorDocument> all() {
    return documents.values();
  }

  private void closed(EditorDocument document) {
    documents.remove(document.tab);
    refreshExplorer();
    if (documents.isEmpty())
      addDocument(
          "algoritmo.alg", null, "algoritmo \"Novo algoritmo\"\nvar\ninicio\n\nfimalgoritmo\n");
  }

  public void installSelectionListener() {
    tabs.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, old, value) -> {
              if (running.getAsBoolean()
                  && !switching
                  && executionDocument.get() != null
                  && value != executionDocument.get().tab) {
                switching = true;
                tabs.getSelectionModel().select(executionDocument.get().tab);
                switching = false;
                return;
              }
              selectionChanged.run();
              if (value != null) explorer.getSelectionModel().select(documents.get(value));
            });
  }

  public EditorDocument active() {
    return documents.get(tabs.getSelectionModel().getSelectedItem());
  }

  public void addDocument(String name, Path path, String content) {
    EditorDocument d =
        new EditorDocument(
            name,
            path,
            content,
            running,
            toggleBreakpoint,
            this::confirmDiscard,
            this::closed,
            this::refreshExplorer,
            position);
    documents.put(d.tab, d);
    tabs.getTabs().add(d.tab);
    tabs.getSelectionModel().select(d.tab);
    refreshExplorer();
    Platform.runLater(d.editor::requestFocus);
  }

  public void newFile() {
    if (running.getAsBoolean()) return;
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

  public void open() {
    if (running.getAsBoolean()) return;
    java.io.File file = chooser().showOpenDialog(stage);
    if (file != null) load(file.toPath());
  }

  public void load(Path path) {
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

  public boolean save(EditorDocument d, boolean choose) {
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

  public boolean confirmDiscard(EditorDocument d) {
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

  public VBox sidebar() {
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
              protected void updateItem(EditorDocument document, boolean empty) {
                super.updateItem(document, empty);
                setText(empty || document == null ? null : document.toString());
                setGraphic(empty || document == null ? null : Icons.create("file"));
                setGraphicTextGap(9);
              }
            });
    explorer.setOnMouseClicked(
        e -> {
          EditorDocument d = explorer.getSelectionModel().getSelectedItem();
          if (d != null && !running.getAsBoolean()) tabs.getSelectionModel().select(d.tab);
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
}
