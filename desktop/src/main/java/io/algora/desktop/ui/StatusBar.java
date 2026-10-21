package io.algora.desktop.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.*;

/** Existing application status and cursor-position display. */
public final class StatusBar {
  private final Label position = new Label("Ln 1, Col 1");
  private final Label status = new Label("Pronto");
  private final HBox view;

  public StatusBar() {
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
    view = box;
  }

  public HBox view() {
    return view;
  }

  public Label status() {
    return status;
  }

  public Label position() {
    return position;
  }
}
