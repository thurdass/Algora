package io.algora.desktop;

/** Entry point kept separate from Application for packaged and classpath launches. */
public final class AlgoraLauncher {
  private AlgoraLauncher() {}

  public static void main(String[] args) {
    AlgoraApplication.main(args);
  }
}
