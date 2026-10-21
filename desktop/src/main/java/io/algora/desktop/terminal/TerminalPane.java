package io.algora.desktop.terminal;

import io.algora.desktop.Icons;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Integrated algorithm output, input queue and buffered output delivery. */
public final class TerminalPane {
  private final TextArea terminal = new TextArea();
  private final TextField input = new TextField();
  private final Label runStatus = new Label("Pronto");
  private final BlockingQueue<String> inputs = new LinkedBlockingQueue<>();
  private final StringBuilder outputBuffer = new StringBuilder();
  private final AtomicBoolean outputScheduled = new AtomicBoolean();
  private final VBox view;

  public TerminalPane() {
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
            append(text + "\n");
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
    Button clear = clearButton();
    HBox terminalHead = new HBox(10, label, flex, runStatus, clear);
    terminalHead.getStyleClass().add("terminal-heading");
    view = new VBox(terminalHead, terminal, input);
    view.setMinHeight(150);
    view.getStyleClass().add("terminal-panel");
  }

  private Button clearButton() {
    Button b = new Button("");
    b.setTooltip(new Tooltip("Limpar terminal"));
    b.setOnAction(e -> terminal.clear());
    b.setFocusTraversable(true);
    b.setAccessibleText("Limpar terminal");
    b.setGraphic(Icons.create("close"));
    b.getStyleClass().add("icon-button");
    return b;
  }

  public VBox view() {
    return view;
  }

  public TextArea output() {
    return terminal;
  }

  public TextField input() {
    return input;
  }

  public Label statusLabel() {
    return runStatus;
  }

  public void clearInputs() {
    inputs.clear();
  }

  public String takeInput() throws InterruptedException {
    return inputs.take();
  }

  public void append(String text) {
    terminal.appendText(text);
    terminal.positionCaret(terminal.getLength());
    terminal.setScrollTop(Double.MAX_VALUE);
  }

  public void appendOutput(String text) {
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
            append(pending.replaceAll("\\x1b\\[[0-9;]*[A-Za-z]", ""));
          });
  }
}
