package io.algora.desktop;

import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Small code-native icons with the same geometry on Linux and Windows. */
final class Icons {
  private Icons() {}

  static Pane create(String name) {
    String data =
        switch (name) {
          case "play" -> "M5 3 L14 9 L5 15 Z";
          case "pause" -> "M6 4 V14 M12 4 V14";
          case "stop" -> "M5 5 H13 V13 H5 Z";
          case "step" -> "M3 5 L9 9 L3 13 Z M13 4 V14";
          case "debug" ->
              "M6 6 H12 V12 Q12 15 9 15 Q6 15 6 12 Z M7 6 V4 Q9 2 11 4 V6 M3 7 H6 M12 7 H15 M3 11"
                  + " H6 M12 11 H15 M5 15 L7 13 M13 15 L11 13";
          case "files" -> "M3 4 H8 L10 6 H15 V14 H3 Z";
          case "file" -> "M5 2 H10 L14 6 V16 H5 Z M10 2 V6 H14";
          case "sidebar" -> "M2 3 H16 V15 H2 Z M7 3 V15";
          case "inspector" -> "M2 3 H16 V15 H2 Z M11 3 V15";
          case "plus" -> "M9 3 V15 M3 9 H15";
          case "close" -> "M5 5 L13 13 M13 5 L5 13";
          case "terminal" -> "M3 5 L7 9 L3 13 M9 13 H15";
          default -> throw new IllegalArgumentException("Ícone desconhecido: " + name);
        };
    SVGPath path = new SVGPath();
    path.setContent(data);
    path.setFill(null);
    path.setStrokeWidth(1.4);
    path.setStrokeLineCap(StrokeLineCap.ROUND);
    path.setStrokeLineJoin(StrokeLineJoin.ROUND);
    path.getStyleClass().add("icon");
    Pane box = new Pane(path);
    box.setMinSize(18, 18);
    box.setPrefSize(18, 18);
    box.setMaxSize(18, 18);
    box.setMouseTransparent(true);
    return box;
  }
}
