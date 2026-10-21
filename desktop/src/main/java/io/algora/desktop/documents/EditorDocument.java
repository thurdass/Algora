package io.algora.desktop.documents;

import io.algora.desktop.editor.PortugolEditorSupport;
import io.algora.desktop.ui.components.Icons;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

/** An open algorithm and its existing editor, gutter and tab event handlers. */
public final class EditorDocument {
  public final CodeArea editor = new CodeArea();
  public final Tab tab = new Tab();
  public final Set<Integer> breakpoints = new TreeSet<>();
  final String title;
  Path path;
  String saved;
  public int executionLine;
  final ContextMenu completions = new ContextMenu();

  EditorDocument(
      String title,
      Path path,
      String content,
      BooleanSupplier running,
      BiConsumer<EditorDocument, Integer> toggleBreakpoint,
      Predicate<EditorDocument> confirmDiscard,
      Consumer<EditorDocument> closed,
      Runnable refreshExplorer,
      Label position) {
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
          point.setOnMouseClicked(e -> toggleBreakpoint.accept(this, line + 1));
          return new HBox(point, LineNumberFactory.get(editor).apply(line));
        });
    editor.replaceText(content);
    PortugolEditorSupport.highlight(editor);
    editor
        .textProperty()
        .addListener(
            (obs, old, text) -> {
              PortugolEditorSupport.highlight(editor);
              updateTitle();
              refreshExplorer.run();
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
            PortugolEditorSupport.showCompletions(editor, completions, running.getAsBoolean());
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
          if (running.getAsBoolean() || !confirmDiscard.test(this)) event.consume();
        });
    tab.setOnClosed(event -> closed.accept(this));
  }

  public String name() {
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
