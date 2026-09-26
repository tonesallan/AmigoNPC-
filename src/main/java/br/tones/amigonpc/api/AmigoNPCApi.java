package br.tones.amigonpc.api;

import br.tones.amigonpc.core.AmigoNpcManager;
import java.util.UUID;

public final class AmigoNPCApi {
   private AmigoNPCApi() {
   }

   public static boolean isAvailable() {
      try {
         return AmigoNpcManager.getShared() != null;
      } catch (Throwable t) {
         return false;
      }
   }

   public static long getNpcTotalXp(UUID ownerId) {
      try {
         return AmigoNpcManager.getShared().getNpcTotalXp(ownerId);
      } catch (Throwable t) {
         return 0L;
      }
   }

   public static int getNpcLevel(UUID ownerId) {
      try {
         return AmigoNpcManager.getShared().getNpcLevel(ownerId);
      } catch (Throwable t) {
         return 1;
      }
   }

   public static boolean addNpcXp(UUID ownerId, long amount, NpcXpSource source, NpcXpContext ctx) {
      try {
         return AmigoNpcManager.getShared().addNpcXpViaApi(ownerId, amount, source, ctx);
      } catch (Throwable t) {
         return false;
      }
   }
}
