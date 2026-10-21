package io.algora.desktop.ui;

import javafx.scene.control.*;

/** Existing styled control factories shared by desktop views. */
public final class IdeControls {
  private IdeControls() {}

  public static Label heading(String text) {
    Label l = new Label(text);
    l.getStyleClass().add("heading");
    return l;
  }

  public static Button button(String text, String tooltip, Runnable action) {
    Button b = new Button(text);
    b.setTooltip(new Tooltip(tooltip));
    b.setOnAction(e -> action.run());
    b.setFocusTraversable(true);
    b.setAccessibleText(tooltip);
    return b;
  }

  public static Button iconButton(String icon, String tooltip, Runnable action) {
    Button b = button("", tooltip, action);
    b.setGraphic(Icons.create(icon));
    b.getStyleClass().add("icon-button");
    return b;
  }
}
