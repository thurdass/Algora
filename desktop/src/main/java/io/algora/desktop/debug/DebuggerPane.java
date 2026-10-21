package io.algora.desktop.debug;

import static io.algora.desktop.ui.IdeControls.*;

import io.algora.desktop.documents.EditorDocument;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Debugger presentation: variables, breakpoint list and editor gutter refresh. */
public final class DebuggerPane {
  private final VBox variables = new VBox(7);
  private final VBox breakpointList = new VBox(6);
  private final VBox view;

  public DebuggerPane() {
    view = new VBox();
    view.setId("inspectionPanel");
    view.getStyleClass().add("inspector");
    view.setPrefWidth(240);
    view.setMinWidth(215);
    view.setMaxWidth(240);
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
    view.getChildren().addAll(top, vars, variables, points, breakpointList);
  }

  public VBox view() {
    return view;
  }

  public void clearVariables() {
    variables.getChildren().clear();
  }

  public void updateVariables(Map<String, Object> values) {
    variables.getChildren().clear();
    values.forEach(
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
    if (values.isEmpty()) emptyVariables();
  }

  public void refreshBreakpoints(EditorDocument d) {
    breakpointList.getChildren().clear();
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

  private String format(Object value) {
    if (value instanceof String s) return '"' + s + '"';
    if (value instanceof Map<?, ?> map) return map.toString();
    if (value instanceof Object[] array) return Arrays.deepToString(array);
    return String.valueOf(value);
  }

  public void refreshGutter(EditorDocument d) {
    for (int i = 0; i < d.editor.getParagraphs().size(); i++) d.editor.recreateParagraphGraphic(i);
  }
}
