package br.tones.amigonpc.ui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;
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
         setError("playerSender é null.");
         return false;
      } else if (actionId != null && !actionId.isBlank()) {
         try {
            UUID ownerId = tryGetUuid(playerSender);
            if (ownerId == null) {
               setError("Não consegui obter UUID do playerSender.");
               return false;
            } else {
               Object world = null;
               if (commandContext != null) {
                  world = HytaleBridge.tryGetWorldFromCommandContext(commandContext);
               }

               if (world == null) {
                  world = tryGetWorldFromSender(playerSender);
               }

               if (world == null) {
                  setError("Não consegui obter World para executar a ação.");
                  return false;
               } else {
                  switch (actionId.toLowerCase()) {
                     case "spawn":
                        boolean ok = MANAGER.spawn(world, ownerId);
                        if (!ok) {
                           setError("Spawn falhou: " + MANAGER.getLastError());
                           return false;
                        }

                        return true;
                     case "despawn":
                        boolean ok = MANAGER.despawn(world, ownerId);
                        if (!ok) {
                           setError("Despawn falhou: " + MANAGER.getLastError());
                           return false;
                        }

                        return true;
                     case "close":
                        return true;
                     default:
                        setError("ActionId desconhecido: " + actionId);
                        return false;
                  }
               }
            }
         } catch (Throwable t) {
            String var10000 = t.getClass().getSimpleName();
            setError("handle falhou: " + var10000 + " - " + t.getMessage());
            return false;
         }
      } else {
         setError("actionId vazio.");
         return false;
      }
   }

   private static UUID tryGetUuid(Object sender) {
      try {
         Method m = sender.getClass().getMethod("getUuid");
         Object r = m.invoke(sender);
         if (r instanceof UUID u) {
            return u;
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   private static Object tryGetWorldFromSender(Object sender) {
      String[] names = new String[]{"getWorld", "world", "getCurrentWorld", "getPlayerWorld"};

      for(String n : names) {
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
