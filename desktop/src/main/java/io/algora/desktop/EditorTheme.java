package io.algora.desktop;

import java.util.prefs.Preferences;

/** Complete, semantic palettes shared by every work surface. */
enum EditorTheme {
  GRAPHITE(
      "Grafite",
      "#1E1E1E #252526 #3C3C3C #D4D4D4 #969696 #3794FF #264F78 #303030 #282828 #569CD6 #CE9178"
          + " #B5CEA8 #6A9955 #F48771"),
  DRACULA(
      "Dracula",
      "#282A36 #21222C #44475A #F8F8F2 #A3A6BA #BD93F9 #44475A #343746 #303340 #FF79C6 #F1FA8C"
          + " #BD93F9 #8A96BD #FF5555"),
  NORD(
      "Nord",
      "#2E3440 #3B4252 #4C566A #ECEFF4 #B2BDCF #88C0D0 #434C5E #434C5E #363E4C #81A1C1 #A3BE8C"
          + " #B48EAD #91A0B8 #BF616A"),
  SOLARIZED_DARK(
      "Solarizado escuro",
      "#002B36 #073642 #315461 #93A1A1 #839496 #2AA198 #174C5B #12424E #0B3540 #268BD2 #B58900"
          + " #D33682 #839496 #DC322F"),
  LIGHT(
      "Claro",
      "#FFFFFF #F3F3F3 #D4D4D4 #242424 #616161 #0067B8 #D5E8FA #E5E5E5 #F4F4F4 #0000AA #A31515"
          + " #176836 #387D31 #B91C1C"),
  SOLARIZED_LIGHT(
      "Solarizado claro",
      "#FDF6E3 #EEE8D5 #D2CAB5 #405861 #657B83 #007E80 #D8E7DD #E4DDC7 #F3EDD9 #006EAA #8A6500"
          + " #AC246E #687D6A #C52D28");

  final String label;
  private final String colors;
  private static final String[] TOKENS = {
    "editor",
    "panel",
    "line",
    "text",
    "muted",
    "accent",
    "selection",
    "hover",
    "current",
    "keyword",
    "string",
    "number",
    "comment",
    "error"
  };

  EditorTheme(String label, String colors) {
    this.label = label;
    this.colors = colors;
  }

  String css() {
    String[] values = colors.split(" ");
    StringBuilder style = new StringBuilder();
    for (int i = 0; i < TOKENS.length; i++)
      style.append("-alg-").append(TOKENS[i]).append(": ").append(values[i]).append(';');
    return style.toString();
  }

  static EditorTheme saved() {
    try {
      return valueOf(Preferences.userNodeForPackage(EditorTheme.class).get("theme", "GRAPHITE"));
    } catch (IllegalArgumentException | SecurityException e) {
      return GRAPHITE;
    }
  }

  static void save(EditorTheme theme) {
    try {
      Preferences.userNodeForPackage(EditorTheme.class).put("theme", theme.name());
    } catch (SecurityException ignored) {
      /* Theme switching remains available without preference access. */
    }
  }
}
