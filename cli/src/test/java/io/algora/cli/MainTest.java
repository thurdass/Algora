package io.algora.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
  @TempDir Path directory;

  private Path program(String source) throws IOException {
    Path file = directory.resolve("teste.alg");
    Files.writeString(file, source);
    return file;
  }

  @Test
  void redirectedInputKeepsEveryLine() throws Exception {
    Path file =
        program(
            "algoritmo \"entrada\"\n"
                + "var nome:caractere\n"
                + "idade:inteiro\n"
                + "inicio\n"
                + "leia(nome,idade)\n"
                + "escreval(nome,\": \",idade+1)\n"
                + "fimalgoritmo");
    ByteArrayOutputStream output = new ByteArrayOutputStream(), error = new ByteArrayOutputStream();
    int result =
        Main.run(
            new String[] {file.toString()},
            new ByteArrayInputStream("Ana\n20\n".getBytes(StandardCharsets.UTF_8)),
            new PrintStream(output),
            new PrintStream(error));
    assertEquals(0, result, error.toString());
    assertTrue(output.toString(StandardCharsets.UTF_8).contains("Ana: 21"));
  }

  @Test
  void exhaustedInputAndInvalidSyntaxReturnFailure() throws Exception {
    Path file = program("algoritmo \"entrada\"\nvar x:inteiro\ninicio\nleia(x)\nfimalgoritmo");
    ByteArrayOutputStream error = new ByteArrayOutputStream();
    assertEquals(
        1,
        Main.run(
            new String[] {file.toString()},
            InputStream.nullInputStream(),
            System.out,
            new PrintStream(error)));
    assertFalse(error.toString().isBlank());
    file = program("algoritmo \"erro\"\ninicio\nescreval(\nfimalgoritmo");
    assertEquals(
        1,
        Main.run(
            new String[] {file.toString()},
            InputStream.nullInputStream(),
            System.out,
            new PrintStream(error)));
  }

  @Test
  void decodesWindows1252AndReportsUsage() throws Exception {
    Path file = directory.resolve("legado.alg");
    Files.write(
        file,
        "algoritmo \"legado\"\ninicio\nescreval(\"Olá\")\nfimalgoritmo"
            .getBytes(java.nio.charset.Charset.forName("windows-1252")));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertEquals(
        0,
        Main.run(
            new String[] {file.toString()},
            InputStream.nullInputStream(),
            new PrintStream(output),
            System.err));
    assertTrue(output.toString(StandardCharsets.UTF_8).contains("Olá"));
    assertEquals(
        2,
        Main.run(
            new String[] {},
            InputStream.nullInputStream(),
            System.out,
            new PrintStream(OutputStream.nullOutputStream())));
  }
}
