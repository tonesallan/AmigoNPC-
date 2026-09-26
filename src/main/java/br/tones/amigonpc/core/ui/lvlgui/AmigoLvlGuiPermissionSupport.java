package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

final class AmigoLvlGuiPermissionSupport {
   private AmigoLvlGuiPermissionSupport() {
   }

   static String bestEffortViewerName(PlayerRef viewerPlayerRef, UUID ownerId) {
      Object sender = viewerPlayerRef;
      if (sender != null) {
         for (String m : new String[]{"getName", "getUsername", "getUserName"}) {
            try {
               Method mm = sender.getClass().getMethod(m);
               Object r = mm.invoke(sender);
               if (r != null) {
                  String s = String.valueOf(r);
                  if (!s.isBlank()) {
                     return s;
                  }
               }
            } catch (Throwable var10) {
            }
         }
      }

      return ownerId != null ? ownerId.toString() : AmigoText.text("ui.settings.fallback.player");
   }

   static boolean isAdminBestEffort(PlayerRef viewerPlayerRef, UUID ownerId) {
      try {
         UUID uid = null;

         try {
            uid = viewerPlayerRef != null ? viewerPlayerRef.getUuid() : null;
         } catch (Throwable var8) {
         }

         if (uid == null) {
            uid = ownerId;
         }

         if (uid == null) {
            return false;
         }

         PermissionsModule pm = null;

         try {
            pm = PermissionsModule.get();
         } catch (Throwable var7) {
         }

         if (pm == null) {
            return false;
         }

         Set<String> groups = null;

         try {
            groups = pm.getGroupsForUser(uid);
         } catch (Throwable var6) {
         }

         return groups == null ? false : groups.contains("OP");
      } catch (Throwable ignored) {
         return false;
      }
   }

   static boolean hasPermissionBestEffort(Object sender, String node) {
      try {
         try {
            Method m = sender.getClass().getMethod("hasPermission", String.class, boolean.class);
            if (m.invoke(sender, node, false) instanceof Boolean b) {
               return b;
            }
         } catch (Throwable var6) {
         }

         try {
            Method m = sender.getClass().getMethod("hasPermission", String.class);
            if (m.invoke(sender, node) instanceof Boolean b) {
               return b;
            }
         } catch (Throwable var5) {
         }
      } catch (Throwable var7) {
      }

      return false;
   }
}
