package io.algora.desktop.execution;

import io.algora.desktop.debug.DebuggerPane;
import io.algora.desktop.documents.DocumentManager;
import io.algora.desktop.documents.EditorDocument;
import io.algora.desktop.terminal.TerminalPane;
import io.algora.interpreter.AlgoraEngine;
import io.algora.interpreter.DebugSession;
import io.algora.interpreter.Snapshot;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javafx.application.Platform;
import javafx.scene.control.Label;

/** Existing execution lifecycle, debugger session and JavaFX snapshot delivery. */
public final class ExecutionController {
  private final DocumentManager documentManager;
  private final TerminalPane terminalPane;
  private final DebuggerPane debuggerPane;
  private final Label status;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private DebugSession session;
  private volatile boolean running;
  private EditorDocument executionDocument;
  private final AtomicReference<Snapshot> latestSnapshot = new AtomicReference<>();
  private final AtomicBoolean snapshotScheduled = new AtomicBoolean();
  private final ExecutionToolbar executionToolbar;

  public ExecutionController(
      DocumentManager documentManager,
      TerminalPane terminalPane,
      DebuggerPane debuggerPane,
      Label status) {
    this.documentManager = documentManager;
    this.terminalPane = terminalPane;
    this.debuggerPane = debuggerPane;
    this.status = status;
    executionToolbar =
        new ExecutionToolbar(
            () -> run(false), () -> run(true), this::pause, this::step, this::stopExecution);
  }

  public boolean isRunning() {
    return running;
  }

  public EditorDocument executionDocument() {
    return executionDocument;
  }

  public ExecutionToolbar toolbar() {
    return executionToolbar;
  }

  public void pause() {
    if (session != null) session.pause();
  }

  public void step() {
    if (session != null) session.step();
  }

  public void toggleBreakpoint(EditorDocument d, int line) {
    if (!d.breakpoints.add(line)) d.breakpoints.remove(line);
    if (session != null && executionDocument == d)
      session.setBreakpoints(Set.copyOf(d.breakpoints));
    debuggerPane.refreshGutter(d);
    debuggerPane.refreshBreakpoints(documentManager.active());
  }

  public void run(boolean debugMode) {
    if (running) {
      if (session != null) session.resume();
      return;
    }
    EditorDocument d = documentManager.active();
    if (d == null) return;
    executionDocument = d;
    terminalPane.clearInputs();
    terminalPane.output().clear();
    debuggerPane.clearVariables();
    setRunning(true);
    terminalPane.append("Executando " + d.name() + "\n\n");
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
                            terminalPane.statusLabel().setText("Aguardando entrada");
                            terminalPane.input().setDisable(false);
                            terminalPane.input().requestFocus();
                          });
                      return terminalPane.takeInput();
                    },
                    terminalPane::appendOutput,
                    this::scheduleSnapshot);
            session.setBreakpoints(Set.copyOf(d.breakpoints));
            session
                .start(debugMode)
                .whenComplete(
                    (result, exception) ->
                        Platform.runLater(
                            () -> {
                              if (exception != null) {
                                terminalPane.append(
                                    "\nErro: " + rootCause(exception).getMessage() + "\n");
                                status.setText("Erro na execução");
                                terminalPane.statusLabel().setText("Erro");
                              } else
                                terminalPane.append(
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
                                terminalPane.statusLabel().setText(finalStatus);
                                status.setText(finalStatus);
                              }
                              setRunning(false);
                            }));
          } catch (Throwable exception) {
            Platform.runLater(
                () -> {
                  terminalPane.append("Erro: " + exception.getMessage() + "\n");
                  status.setText("Erro no algoritmo");
                  terminalPane.statusLabel().setText("Erro");
                  setRunning(false);
                });
          }
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
    terminalPane
        .statusLabel()
        .setText(
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
    status.setText(terminalPane.statusLabel().getText());
    executionToolbar.setPaused(paused);
    debuggerPane.updateVariables(snapshot.variables());
    if (executionDocument != null) {
      executionDocument.executionLine = paused ? snapshot.line() : 0;
      debuggerPane.refreshGutter(executionDocument);
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

  private void setRunning(boolean value) {
    running = value;
    if (value) {
      terminalPane.statusLabel().setText("Em execução");
      status.setText("Em execução");
    }
    for (EditorDocument d : documentManager.all()) d.editor.setEditable(!value);
    executionToolbar.setRunning(value);
    if (!value) {
      terminalPane.input().setDisable(true);
      if (executionDocument != null) {
        executionDocument.executionLine = 0;
        debuggerPane.refreshGutter(executionDocument);
      }
    }
  }

  public void stopExecution() {
    if (session != null) session.stop();
    terminalPane.input().setDisable(true);
  }

  public void stop() {
    if (session != null) session.stop();
    executor.shutdownNow();
  }
}
