package io.algora.interpreter;

import io.algora.interpreter.grammar.*;
import java.text.Normalizer;
import java.util.function.Consumer;
import org.antlr.v4.runtime.*;

public final class AlgoraEngine {
  private AlgoraEngine() {}

  public static Program parse(String source) {
    var errors =
        new BaseErrorListener() {
          @Override
          public void syntaxError(
              Recognizer<?, ?> recognizer,
              Object offending,
              int line,
              int column,
              String message,
              RecognitionException error) {
            String detail =
                offending instanceof Token token
                    ? "Encontrado '" + token.getText() + "'"
                    : "Caractere ou texto inválido";
            if (recognizer instanceof Parser p)
              detail += "; esperado " + p.getExpectedTokens().toString(p.getVocabulary());
            throw new AlgoraException(
                line, "Sintaxe inválida na coluna " + (column + 1) + ": " + detail);
          }
        };
    var lexer = new PortugolLexer(CharStreams.fromString(normalize(source)));
    lexer.removeErrorListeners();
    lexer.addErrorListener(errors);
    var parser = new PortugolParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(errors);
    return (Program) new AstBuilder().visit(parser.program());
  }

  public static DebugSession createSession(
      String source, InputProvider input, Consumer<String> output, Consumer<Snapshot> observer) {
    return new DebugSession(parse(source), input, output, observer);
  }

  public static Snapshot run(String source, InputProvider input, Consumer<String> output) {
    return createSession(source, input, output, s -> {}).start(false).join();
  }

  private static String normalize(String source) {
    StringBuilder result = new StringBuilder();
    char quote = 0;
    boolean comment = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\n') comment = false;
      if (quote == 0
          && !comment
          && c == '/'
          && i + 1 < source.length()
          && source.charAt(i + 1) == '/') comment = true;
      if (!comment && (c == '"' || c == '\'')) {
        if (quote == 0) quote = c;
        else if (quote == c) quote = 0;
        result.append(c);
      } else if (quote != 0 || comment) result.append(c);
      else
        result.append(
            Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}", ""));
    }
    return result.toString();
  }
}
