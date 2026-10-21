package io.algora.desktop.execution;

import static io.algora.desktop.ui.components.IdeControls.*;

import io.algora.desktop.ui.components.Icons;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Existing execution buttons and their enabled/paused presentation. */
public final class ExecutionToolbar {
  private final Button execute, debug, pause, step, stop;
  private final HBox view;

  public ExecutionToolbar(
      Runnable runAction,
      Runnable debugAction,
      Runnable pauseAction,
      Runnable stepAction,
      Runnable stopAction) {
    execute = button("Executar   F5", "Executar ou continuar", runAction);
    execute.setGraphic(Icons.create("play"));
    execute.getStyleClass().add("run-button");
    execute.setId("runButton");
    debug = button("Depurar", "Depurar desde a primeira instrução", debugAction);
    debug.setGraphic(Icons.create("debug"));
    pause = iconButton("pause", "Pausar", pauseAction);
    step = iconButton("step", "Próximo passo (F10)", stepAction);
    stop = iconButton("stop", "Parar execução", stopAction);
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
    view = new HBox(9, breadcrumb, space, execute, debug, pause, step, stop);
    view.getStyleClass().add("editor-toolbar");
  }

  public HBox view() {
    return view;
  }

  public void setPaused(boolean paused) {
    execute.setText(paused ? "Continuar   F5" : "Executar   F5");
  }

  public void setRunning(boolean value) {
    debug.setDisable(value);
    pause.setDisable(!value);
    step.setDisable(!value);
    stop.setDisable(!value);
  }
}
