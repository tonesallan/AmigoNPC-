package br.tones.amigonpc.core;

import br.tones.amigonpc.core.i18n.AmigoText;
import java.lang.reflect.Method;

public final class Permissions {
   public static final String AMIGO_USE = "amigonpc.use";
   public static final String AMIGO_SPAWN = "amigonpc.spawn";
   public static final String AMIGO_DESPAWN = "amigonpc.despawn";
   public static final String AMIGO_UI = "amigonpc.ui";
   public static final String AMIGO_DEBUG = "amigonpc.debug";
   private static volatile String LAST_ERROR;

   private Permissions() {
   }

   public static String getLastError() {
      return LAST_ERROR;
   }

   private static void setError(String msg) {
      LAST_ERROR = msg;
   }

   public static boolean has(Object sender, String permissionNode) {
      if (sender != null && permissionNode != null && !permissionNode.isBlank()) {
         String[] candidates = new String[]{"hasPermission", "hasPerm", "can"};

         for (String name : candidates) {
            try {
               Method m = sender.getClass().getMethod(name, String.class);
               if (m.invoke(sender, permissionNode) instanceof Boolean b) {
                  return b;
               }
            } catch (Throwable var12) {
            }
         }

         Object pm = tryNoArg(sender, "getPermissionManager", "permissionManager", "getPermissions", "permissions");
         if (pm != null) {
            for (String name : candidates) {
               try {
                  Method m = pm.getClass().getMethod(name, String.class);
                  if (m.invoke(pm, permissionNode) instanceof Boolean b) {
                     return b;
                  }
               } catch (Throwable var11) {
               }
            }
         }

         setError(AmigoText.text("core.permissions.error.no_permission_method"));
         return true;
      } else {
         return true;
      }
   }

   private static Object tryNoArg(Object target, String... names) {
      for (String n : names) {
         try {
            Method m = target.getClass().getMethod(n);
            return m.invoke(target);
         } catch (Throwable var7) {
         }
      }

      return null;
   }
}
