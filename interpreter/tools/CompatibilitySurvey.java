import io.algora.interpreter.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class CompatibilitySurvey {
  static String read(Path p) throws Exception {
    byte[] b = Files.readAllBytes(p);
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(b))
          .toString();
    } catch (CharacterCodingException e) {
      return Charset.forName("windows-1252").decode(ByteBuffer.wrap(b)).toString();
    }
  }

  public static void main(String[] args) throws Exception {
    Path base = Path.of(args[0]);
    List<Path> paths = new ArrayList<>();
    try (var files = Files.walk(base.resolve("Exemplos"))) {
      files
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase().endsWith(".alg"))
          .forEach(paths::add);
    }
    paths.add(base.resolve("TESTE.alg"));
    paths.sort(Comparator.naturalOrder());
    int passed = 0;
    for (Path p : paths) {
      try {
        AlgoraEngine.parse(read(p));
        passed++;
        System.out.println("PASS\t" + base.relativize(p));
      } catch (Exception e) {
        System.out.println("FAIL\t" + base.relativize(p) + "\t" + e.getMessage());
      }
    }
    System.out.println("TOTAL\t" + paths.size() + "\tPASS\t" + passed);
    for (String name : List.of("fatorial.alg", "inssort.alg", "bbsort.alg", "interrompa.alg")) {
      StringBuilder output = new StringBuilder();
      var session =
          AlgoraEngine.createSession(
              read(base.resolve("Exemplos").resolve(name)), () -> "5", output::append, null);
      long[] seed = {73};
      session.random(
          () -> {
            seed[0] = seed[0] * 16807 % 2147483647;
            return seed[0] / 2147483647.0;
          });
      try {
        var result = session.start(false).join();
        Object array = result.variables().getOrDefault("v", result.variables().get("x"));
        boolean sorted = true;
        if (array instanceof List<?> list)
          for (int i = 1; i < list.size(); i++)
            if (((Number) list.get(i - 1)).doubleValue() > ((Number) list.get(i)).doubleValue())
              sorted = false;
        System.out.println(
            "RUN\t"
                + name
                + "\t"
                + result.status()
                + "\t"
                + result.steps()
                + "\tsorted="
                + sorted
                + "\toutput="
                + output.toString().replace("\n", "\\n"));
      } catch (Exception e) {
        System.out.println("RUNFAIL\t" + name + "\t" + e);
      }
    }
  }
}
