package io.algora.cli;

import io.algora.interpreter.AlgoraEngine;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.CompletionException;

/** Command line adapter. The interpreter has no dependency on this input/output layer. */
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        int exitCode = run(args, System.in, System.out, System.err);
        if (exitCode != 0) System.exit(exitCode);
    }
    public static int run(String[] args, InputStream in, PrintStream out, PrintStream error) {
        if (args.length != 1) {
            error.println("Uso: algora-cli arquivo.alg");
            return 2;
        }
        try {
            byte[] bytes = Files.readAllBytes(Path.of(args[0]));
            String source;
            try {
                source = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException e) {
                source = Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString();
            }
            BufferedReader input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            AlgoraEngine.run(source, () -> {
                String line = input.readLine();
                if (line == null) throw new EOFException("Fim da entrada: o algoritmo solicitou mais valores.");
                return line;
            }, out::print);
            out.flush();
            return 0;
        } catch (Exception e) {
            Throwable cause = e;
            while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
            error.println(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
            return 1;
        }
    }
}
