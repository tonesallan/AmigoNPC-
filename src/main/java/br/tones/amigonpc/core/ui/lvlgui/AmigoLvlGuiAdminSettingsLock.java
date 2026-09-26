package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import java.util.UUID;

final class AmigoLvlGuiAdminSettingsLock {
   static final long TIMEOUT_MS = 180000L;
   static volatile UUID ownerId;
   static volatile String ownerName;
   static volatile long lastTouchMillis;

   private AmigoLvlGuiAdminSettingsLock() {
   }

   static boolean isHeldBy(UUID id) {
      return id != null && id.equals(ownerId);
   }

   static synchronized boolean tryAcquire(UUID id, String name) {
      long now = System.currentTimeMillis();
      if (ownerId != null && now - lastTouchMillis > 180000L) {
         ownerId = null;
         ownerName = null;
         lastTouchMillis = 0L;
      }

      if (ownerId != null && !ownerId.equals(id)) {
         return false;
      }

      ownerId = id;
      ownerName = name != null && !name.isBlank() ? name : (id != null ? id.toString() : AmigoText.text("ui.settings.fallback.admin"));
      lastTouchMillis = now;
      return true;
   }

   static synchronized void touch(UUID id) {
      if (id != null) {
         if (ownerId != null && ownerId.equals(id)) {
            lastTouchMillis = System.currentTimeMillis();
         }
      }
   }

   static synchronized void release(UUID id) {
      if (id != null) {
         if (ownerId != null && ownerId.equals(id)) {
            ownerId = null;
            ownerName = null;
            lastTouchMillis = 0L;
         }
      }
   }
}
