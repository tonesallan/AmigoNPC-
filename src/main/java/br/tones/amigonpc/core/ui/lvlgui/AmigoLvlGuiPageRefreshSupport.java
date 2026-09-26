package br.tones.amigonpc.core.ui.lvlgui;

import java.util.Map;
import java.util.UUID;

final class AmigoLvlGuiPageRefreshSupport {
   private static final long UPDATE_THROTTLE_NANOS = 250000000L;

   private AmigoLvlGuiPageRefreshSupport() {
   }

   static void notifyNpcXpChanged(UUID ownerId, Map<UUID, AmigoLvlGuiPage> openPages, Map<UUID, Long> nextUpdateNanos) {
      if (ownerId != null) {
         AmigoLvlGuiPage page = openPages.get(ownerId);
         if (page != null) {
            long now = System.nanoTime();
            long next = nextUpdateNanos.getOrDefault(ownerId, 0L);
            if (now >= next) {
               nextUpdateNanos.put(ownerId, now + 250000000L);

               try {
                  page.requestHeaderRefresh();
               } catch (Throwable var9) {
               }
            }
         }
      }
   }
}
