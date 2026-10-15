package br.tones.amigonpc.diag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gera um relatório de texto com os elementos de cada página .ui do AmigoNPC,
 * para conferir o menu sem depender de captura de tela.
 * Saída: AmigoNPC-logs/ui-dump-<página>.txt
 */
public final class UiDump {
   private static final Pattern ELEMENT = Pattern.compile("(?m)^\\s*(?:\\$C\\.@)?([A-Za-z]+)\\s+#([A-Za-z0-9_]+)");

   private UiDump() {
   }

   /** Lista de páginas .ui a inspecionar, dentro dos recursos do mod. */
   public static final String[] PAGES = {
      "Common/UI/Custom/Pages/AmigoNPC_Leveling_Main.ui",
      "Common/UI/Custom/Pages/AmigoNPC_Leveling_LeaderboardPage.ui",
      "Common/UI/Custom/Pages/AmigoNPC_Leveling_StatsPage.ui",
      "Common/UI/Custom/Pages/AmigoNPC_Leveling_RewardsPage.ui",
      "Common/UI/Custom/Pages/AmigoNPC_Leveling_PlayerHeader.ui",
      "Common/UI/Custom/Pages/Nav/AmigoNPC_Leveling_Sidebar.ui",
      "Common/UI/Custom/HUD/AmigoNPC_LevelProgress.ui"
   };

   /** Escreve um relatório por página. Retorna quantas páginas foram geradas. */
   public static int dumpAll() {
      int written = 0;
      for (String page : PAGES) {
         try {
            written += dumpOne(page) ? 1 : 0;
         } catch (IOException | RuntimeException e) {
            ErrorReport.record("UiDump " + page, e);
         }
      }
      return written;
   }

   private static boolean dumpOne(String resource) throws IOException {
      String text;
      try (InputStream in = UiDump.class.getClassLoader().getResourceAsStream(resource)) {
         if (in == null) {
            ErrorReport.info("UiDump", "recurso não encontrado: " + resource);
            return false;
         }
         text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }

      Set<String> ids = new LinkedHashSet<>();
      Matcher m = ELEMENT.matcher(text);
      StringBuilder out = new StringBuilder();
      out.append("Página: ").append(resource).append("\n");
      out.append("Elementos com id (tipo #id):\n");
      while (m.find()) {
         String line = m.group(1) + " #" + m.group(2);
         if (ids.add(line)) {
            out.append("  ").append(line).append("\n");
         }
      }
      out.append("Total: ").append(ids.size()).append("\n");

      Path dir = ErrorReport.logDir();
      Files.createDirectories(dir);
      String name = resource.substring(resource.lastIndexOf('/') + 1).replace(".ui", "");
      Files.writeString(dir.resolve("ui-dump-" + name + ".txt"), out.toString(), StandardCharsets.UTF_8);
      return true;
   }
}
