package io.algora.desktop.verification;

import io.algora.desktop.terminal.TerminalPane;
import javafx.application.Platform;

/** Existing installed-runtime smoke probe, independent of application startup wiring. */
public final class PackagedApplicationProbe {
  private PackagedApplicationProbe() {}

  public static void verify(TerminalPane terminalPane, Runnable stop, Runnable run) {
    javafx.animation.Timeline pulse = new javafx.animation.Timeline();
    javafx.animation.PauseTransition deadline =
        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(15));
    deadline.setOnFinished(
        event -> {
          System.err.println("ALGORA_SMOKE_FAILED: timeout");
          stop.run();
          Platform.exit();
          System.exit(1);
        });
    pulse
        .getKeyFrames()
        .add(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(80),
                event -> {
                  if (!terminalPane.input().isDisabled()) {
                    terminalPane.input().setText("Java 21");
                    terminalPane.input().fireEvent(new javafx.event.ActionEvent());
                  }
                  if (terminalPane.output().getText().contains("Erro:")) {
                    System.err.println("ALGORA_SMOKE_FAILED: " + terminalPane.output().getText());
                    stop.run();
                    Platform.exit();
                    System.exit(1);
                  }
                  if (terminalPane.output().getText().contains("Execução finalizada")) {
                    if (!terminalPane.output().getText().contains("Bem-vindo ao Algora, Java 21!")
                        || !terminalPane.output().getText().contains("Passo 3")) {
                      System.err.println("ALGORA_SMOKE_FAILED: unexpected output");
                      System.exit(1);
                    }
                    pulse.stop();
                    deadline.stop();
                    System.out.println("ALGORA_SMOKE_OK");
                    stop.run();
                    Platform.exit();
                  }
                }));
    pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
    deadline.play();
    pulse.play();
    run.run();
  }
}
