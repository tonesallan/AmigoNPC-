package br.tones.amigonpc.commands;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandAccess {
   private CommandAccess() {
   }

   public static void makePublicRecursive(Object cmd) {
      if (cmd != null) {
         tryInvoke(cmd, "setRequiresPermission", boolean.class, false);
         tryInvoke(cmd, "setPermissionRequired", boolean.class, false);
         tryInvoke(cmd, "setRequiresPermissions", boolean.class, false);
         tryInvoke(cmd, "setRequirePermission", boolean.class, false);
         tryInvoke(cmd, "setRequiresOp", boolean.class, false);
         tryInvoke(cmd, "setOpOnly", boolean.class, false);
         tryInvoke(cmd, "setAdminOnly", boolean.class, false);
         tryInvoke(cmd, "setPermission", String.class, (String)null);
         tryInvoke(cmd, "setPermissionNode", String.class, (String)null);
         tryInvoke(cmd, "setPermission", String.class, "");
         tryInvoke(cmd, "setPermissionNode", String.class, "");
         tryClearPermissionFields(cmd);

         for (Object sub : findSubCommands(cmd)) {
            makePublicRecursive(sub);
         }
      }
   }

   private static void tryInvoke(Object target, String methodName, Class<?> paramType, Object arg) {
      try {
         Method m = target.getClass().getMethod(methodName, paramType);
         m.setAccessible(true);
         m.invoke(target, arg);
      } catch (Throwable var6) {
         try {
            Method m = target.getClass().getDeclaredMethod(methodName, paramType);
            m.setAccessible(true);
            m.invoke(target, arg);
         } catch (Throwable var5) {
         }
      }
   }

   private static void tryClearPermissionFields(Object cmd) {
      try {
         Field[] fields = cmd.getClass().getDeclaredFields();

         for (Field f : fields) {
            String n = f.getName();
            if (n != null) {
               String ln = n.toLowerCase(Locale.ROOT);
               if (ln.contains("permission") || ln.contains("perm") || ln.contains("oponly") || ln.contains("admin")) {
                  try {
                     f.setAccessible(true);
                     Class<?> t = f.getType();
                     if (t == boolean.class) {
                        f.setBoolean(cmd, false);
                     } else if (t == Boolean.class) {
                        f.set(cmd, Boolean.FALSE);
                     } else if (t == String.class) {
                        f.set(cmd, null);
                     }
                  } catch (Throwable var9) {
                  }
               }
            }
         }
      } catch (Throwable var10) {
      }
   }

   private static List<Object> findSubCommands(Object cmd) {
      if (cmd == null) {
         return Collections.emptyList();
      }

      try {
         Method m = cmd.getClass().getMethod("getSubCommands");
         Object v = m.invoke(cmd);
         List<Object> out = toList(v);
         if (!out.isEmpty()) {
            return out;
         }
      } catch (Throwable var10) {
      }

      for (String mn : new String[]{"getChildren", "children", "subCommands", "subcommands"}) {
         try {
            Method m = cmd.getClass().getMethod(mn);
            Object v = m.invoke(cmd);
            List<Object> out = toList(v);
            if (!out.isEmpty()) {
               return out;
            }
         } catch (Throwable var9) {
         }
      }

      for (String fn : new String[]{"subCommands", "subcommands", "children", "subCommandList", "subCommandMap"}) {
         try {
            Field f = cmd.getClass().getDeclaredField(fn);
            f.setAccessible(true);
            Object v = f.get(cmd);
            List<Object> out = toList(v);
            if (!out.isEmpty()) {
               return out;
            }
         } catch (Throwable var8) {
         }
      }

      return Collections.emptyList();
   }

   private static List<Object> toList(Object v) {
      if (v == null) {
         return Collections.emptyList();
      }

      List<Object> out = new ArrayList<>();

      try {
         if (v instanceof Iterable) {
            for (Object o : (Iterable)v) {
               if (o != null) {
                  out.add(o);
               }
            }

            return out;
         }

         if (v instanceof Map<?, ?> map) {
            for (Object o : map.values()) {
               if (o != null) {
                  out.add(o);
               }
            }

            return out;
         }

         if (v.getClass().isArray()) {
            int n = Array.getLength(v);

            for (int i = 0; i < n; i++) {
               Object o = Array.get(v, i);
               if (o != null) {
                  out.add(o);
               }
            }

            return out;
         }
      } catch (Throwable var5) {
      }

      return out;
   }
}
