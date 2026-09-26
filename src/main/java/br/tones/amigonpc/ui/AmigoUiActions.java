package br.tones.amigonpc.ui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.core.i18n.AmigoText;
import java.lang.reflect.Method;
import java.util.UUID;

public final class AmigoUiActions {
   private static final AmigoNpcManager MANAGER = AmigoNpcManager.getShared();
   private static volatile String LAST_ERROR;

   private AmigoUiActions() {
   }

   public static String getLastError() {
      return LAST_ERROR;
   }

   private static void setError(String msg) {
      LAST_ERROR = msg;
   }

   public static boolean handle(Object commandContext, Object playerSender, String actionId) {
      if (playerSender == null) {
         setError(AmigoText.text("ui.legacy.actions.error.player_sender_null"));
         return false;
      }

      if (actionId != null && !actionId.isBlank()) {
         try {
            UUID ownerId = tryGetUuid(playerSender);
            if (ownerId == null) {
               setError(AmigoText.text("ui.legacy.actions.error.player_uuid_unavailable"));
               return false;
            }

            Object world = null;
            if (commandContext != null) {
               world = HytaleBridge.tryGetWorldFromCommandContext(commandContext);
            }

            if (world == null) {
               world = tryGetWorldFromSender(playerSender);
            }

            if (world == null) {
               setError(AmigoText.text("ui.legacy.actions.error.world_unavailable"));
               return false;
            }

            switch (actionId.toLowerCase()) {
               case "spawn":
                  boolean ok = MANAGER.spawn(world, ownerId);
                  if (!ok) {
                     setError(AmigoText.format("ui.legacy.actions.error.spawn_failed", MANAGER.getLastError()));
                     return false;
                  }

                  return true;
               case "despawn":
                  boolean ok = MANAGER.despawn(world, ownerId);
                  if (!ok) {
                     setError(AmigoText.format("ui.legacy.actions.error.despawn_failed", MANAGER.getLastError()));
                     return false;
                  }

                  return true;
               case "close":
                  return true;
               default:
                  setError(AmigoText.format("ui.legacy.actions.error.unknown_action", actionId));
                  return false;
            }
         } catch (Throwable t) {
            setError(AmigoText.format("ui.legacy.actions.error.handle_failed", t.getClass().getSimpleName(), t.getMessage()));
            return false;
         }
      } else {
         setError(AmigoText.text("ui.legacy.actions.error.action_id_empty"));
         return false;
      }
   }

   private static UUID tryGetUuid(Object sender) {
      try {
         Method m = sender.getClass().getMethod("getUuid");
         if (m.invoke(sender) instanceof UUID u) {
            return u;
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   private static Object tryGetWorldFromSender(Object sender) {
      String[] names = new String[]{"getWorld", "world", "getCurrentWorld", "getPlayerWorld"};

      for (String n : names) {
         try {
            Method m = sender.getClass().getMethod(n);
            Object r = m.invoke(sender);
            if (r != null) {
               return r;
            }
         } catch (Throwable var8) {
         }
      }

      return null;
   }
}
