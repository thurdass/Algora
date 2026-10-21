package io.algora.desktop.ui;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Existing editor/terminal arrangement and responsive panel visibility. */
public final class WorkspaceLayout {
  private final Stage stage;
  private final VBox filesPanel, inspectionPanel;
  private final Button filesToggle, inspectionToggle;
  private final HBox view;

  public WorkspaceLayout(
      Stage stage,
      TabPane tabs,
      HBox toolbar,
      VBox terminal,
      VBox filesPanel,
      VBox inspectionPanel,
      Button filesToggle,
      Button inspectionToggle) {
    this.stage = stage;
    this.filesPanel = filesPanel;
    this.inspectionPanel = inspectionPanel;
    this.filesToggle = filesToggle;
    this.inspectionToggle = inspectionToggle;
    VBox editor = editorPane(tabs, toolbar, terminal);
    VBox rail = new VBox(8, filesToggle, inspectionToggle);
    rail.getStyleClass().add("activity-rail");
    view = new HBox(rail, filesPanel, editor, inspectionPanel);
    view.setId("workspace");
    HBox.setHgrow(editor, Priority.ALWAYS);
  }

  public HBox view() {
    return view;
  }

  public void toggleFiles() {
    togglePanel(filesPanel);
  }

  public void toggleInspector() {
    togglePanel(inspectionPanel);
  }

  public void installResizeListener() {
    stage
        .widthProperty()
        .addListener(
            (o, old, width) -> {
              showPanel(filesPanel, filesToggle, width.doubleValue() >= 900);
              showPanel(inspectionPanel, inspectionToggle, width.doubleValue() >= 1160);
            });
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

  private VBox editorPane(TabPane tabs, HBox toolbar, VBox terminal) {
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    tabs.getStyleClass().add("editor-tabs");
    VBox.setVgrow(tabs, Priority.ALWAYS);
    VBox top = new VBox(toolbar, tabs);
    VBox.setVgrow(tabs, Priority.ALWAYS);
    SplitPane split = new SplitPane(top, terminal);
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
}
