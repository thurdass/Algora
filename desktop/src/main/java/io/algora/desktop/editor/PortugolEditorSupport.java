package io.algora.desktop.editor;

import java.util.*;
import java.util.regex.Pattern;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/** Syntax coloring and completion for the existing Portugol editor. */
public final class PortugolEditorSupport {
  private PortugolEditorSupport() {}

  private static final String[] KEYWORDS =
      ("algoritmo var inicio fimalgoritmo inteiro real caractere caracter logico verdadeiro falso"
              + " se entao senao fimse para de ate passo faca fimpara enquanto fimenquanto repita"
              + " escolha caso outrocaso fimescolha procedimento fimprocedimento funcao fimfuncao"
              + " retorne leia escreva escreval e ou nao div mod interrompa")
          .split(" ");
  private static final Pattern TOKENS =
      Pattern.compile(
          "(?<COMMENT>//[^\\n]*)|(?<STRING>\"[^\"\\n]*\"|'[^'\\n]*')|(?<KEYWORD>\\b(?:"
              + String.join("|", KEYWORDS)
              + ")\\b)|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  public static void highlight(CodeArea editor) {
    String text = editor.getText();
    var matcher = TOKENS.matcher(text);
    StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
    int end = 0;
    while (matcher.find()) {
      spans.add(List.of(), matcher.start() - end);
      String kind =
          matcher.group("COMMENT") != null
              ? "comment"
              : matcher.group("STRING") != null
                  ? "string"
                  : matcher.group("NUMBER") != null ? "number" : "keyword";
      spans.add(List.of(kind), matcher.end() - matcher.start());
      end = matcher.end();
    }
    spans.add(List.of(), text.length() - end);
    editor.setStyleSpans(0, spans.create());
  }

  public static void showCompletions(CodeArea editor, ContextMenu completions, boolean running) {
    if (running) return;
    int caret = editor.getCaretPosition();
    String before = editor.getText(0, caret);
    int start = caret;
    while (start > 0 && Character.isLetter(before.charAt(start - 1))) start--;
    String prefix = before.substring(start).toLowerCase(Locale.ROOT);
    final int from = start;
    completions.getItems().clear();
    for (String keyword : KEYWORDS)
      if (keyword.startsWith(prefix)) {
        MenuItem item = new MenuItem(keyword);
        item.setOnAction(
            e -> {
              String text =
                  switch (keyword) {
                    case "se" -> "se condicao entao\n   \nfimse";
                    case "para" -> "para i de 1 ate 10 faca\n   \nfimpara";
                    case "enquanto" -> "enquanto condicao faca\n   \nfimenquanto";
                    default -> keyword;
                  };
              editor.replaceText(from, caret, text);
            });
        completions.getItems().add(item);
      }
    editor
        .getCaretBounds()
        .ifPresent(bounds -> completions.show(editor, bounds.getMinX(), bounds.getMaxY() + 4));
  }
}
