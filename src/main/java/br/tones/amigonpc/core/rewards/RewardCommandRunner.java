package br.tones.amigonpc.core.rewards;

import java.util.Locale;
import java.util.UUID;

public final class RewardCommandRunner {
   private RewardCommandRunner() {
   }

   public static void run(UUID ownerId, String ownerName, String command, boolean debug) {
      if (command != null && !command.isBlank()) {
         String cmd = command.trim();
         if (isBuiltInAmigoSetLevel(cmd)) {
            int level = parseLastInt(cmd);
            if (level > 0) {
               RewardsService.setNpcLevelInternal(ownerId, level, debug);
               return;
            }
         }

         System.out.println("[AmigoNPC][Rewards] CommandManager indisponível; comando ignorado: " + cmd);
      }
   }

   private static boolean isBuiltInAmigoSetLevel(String cmd) {
      String s = cmd.toLowerCase(Locale.ROOT);
      return s.startsWith("amigo setlevel ");
   }

   private static int parseLastInt(String cmd) {
      try {
         String[] parts = cmd.trim().split("\\s+");
         if (parts.length < 3) {
            return -1;
         }

         String last = parts[parts.length - 1];
         return Integer.parseInt(last);
      } catch (Throwable ignored) {
         return -1;
      }
   }
}
