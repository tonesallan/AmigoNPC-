package br.tones.amigonpc.diag;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Grava erros do AmigoNPC em arquivo, para diagnóstico fora do console.
 * Pasta: AmigoNPC-logs/ no diretório de trabalho do servidor (ou o caminho em -Damigonpc.logdir).
 * Nunca lança exceção: falha de log não pode derrubar o mod.
 */
public final class ErrorReport {
   private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
   private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

   private ErrorReport() {
   }

   public static Path logDir() {
      String custom = System.getProperty("amigonpc.logdir");
      return custom != null && !custom.isBlank() ? Path.of(custom) : Path.of("AmigoNPC-logs");
   }

   /** Registra um erro com contexto (onde aconteceu) e stack trace completa. */
   public static void record(String where, Throwable error) {
      StringWriter sw = new StringWriter();
      if (error != null) {
         error.printStackTrace(new PrintWriter(sw));
      }
      append("ERROR", where, error == null ? "(sem exceção)" : error.toString(), sw.toString());
   }

   /** Registra uma mensagem de estado (sem exceção), útil para acompanhar o que o mod fez. */
   public static void info(String where, String message) {
      append("INFO", where, message, "");
   }

   private static synchronized void append(String level, String where, String message, String trace) {
      try {
         Path dir = logDir();
         Files.createDirectories(dir);
         Path file = dir.resolve("amigonpc-" + LocalDateTime.now().format(DAY) + ".log");
         String entry = "[" + LocalDateTime.now().format(TS) + "] " + level + " " + where + " - " + message + "\n"
            + (trace.isEmpty() ? "" : trace + "\n");
         Files.writeString(file, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException | RuntimeException ignored) {
         // falha de log não pode derrubar o mod
      }
   }
}
