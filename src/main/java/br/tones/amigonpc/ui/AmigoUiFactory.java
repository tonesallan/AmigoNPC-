package br.tones.amigonpc.ui;

import br.tones.amigonpc.core.i18n.AmigoText;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class AmigoUiFactory {
   private static volatile String LAST_ERROR;

   private AmigoUiFactory() {
   }

   public static String getLastError() {
      return LAST_ERROR;
   }

   private static void setError(String msg) {
      LAST_ERROR = msg;
   }

   public static Object buildMainUi() {
      try {
         Class<?> builderClass = tryLoad(
            "com.hypixel.hytale.server.core.ui.UICommandBuilder",
            "com.hypixel.hytale.server.core.ui.command.UICommandBuilder",
            "com.hypixel.hytale.server.core.ui.uicommands.UICommandBuilder"
         );
         if (builderClass == null) {
            setError(AmigoText.text("ui.legacy.factory.error.builder_missing"));
            return null;
         }

         Object builder = instantiate(builderClass);
         if (builder == null) {
            setError(AmigoText.text("ui.legacy.factory.error.builder_instantiate_failed"));
            return null;
         }

         tryInvoke(builder, "title", String.class, AmigoText.text("ui.legacy.factory.title"));
         tryInvoke(builder, "setTitle", String.class, AmigoText.text("ui.legacy.factory.title"));
         tryInvoke(builder, "text", String.class, AmigoText.text("ui.legacy.factory.description"));
         tryInvoke(builder, "setText", String.class, AmigoText.text("ui.legacy.factory.description"));
         addCommandButton(builder, AmigoText.text("ui.legacy.factory.button.spawn"), "/amigo spawn");
         addCommandButton(builder, AmigoText.text("ui.legacy.factory.button.despawn"), "/amigo despawn");
         addCommandButton(builder, AmigoText.text("ui.legacy.factory.button.close"), "/amigo");
         Object uiObj = tryInvokeReturn(builder, "build");
         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "create");
         }

         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "finish");
         }

         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "getResult");
         }

         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "getCommand");
         }

         if (uiObj == null) {
            setError(AmigoText.text("ui.legacy.factory.error.result_unavailable"));
            return null;
         } else {
            return uiObj;
         }
      } catch (Throwable t) {
         setError(AmigoText.format("ui.legacy.factory.error.build_failed", t.getClass().getSimpleName(), t.getMessage()));
         return null;
      }
   }

   private static void addCommandButton(Object builder, String label, String command) {
      if (!tryInvoke(builder, "button", String.class, String.class, label, command)) {
         if (!tryInvoke(builder, "addButton", String.class, String.class, label, command)) {
            if (!tryInvoke(builder, "commandButton", String.class, String.class, label, command)) {
               if (!tryInvoke(builder, "buttonCommand", String.class, String.class, label, command)) {
                  if (!tryInvoke(builder, "option", String.class, String.class, label, command)) {
                     if (!tryInvoke(builder, "addOption", String.class, String.class, label, command)) {
                        ;
                     }
                  }
               }
            }
         }
      }
   }

   private static Class<?> tryLoad(String... names) {
      for (String n : names) {
         try {
            return Class.forName(n);
         } catch (Throwable var6) {
         }
      }

      return null;
   }

   private static Object instantiate(Class<?> c) {
      try {
         Constructor<?> ctor = c.getDeclaredConstructor();
         ctor.setAccessible(true);
         return ctor.newInstance();
      } catch (Throwable var2) {
         return null;
      }
   }

   private static boolean tryInvoke(Object target, String methodName, Class<?> p1, Object a1) {
      try {
         Method m = target.getClass().getMethod(methodName, p1);
         m.invoke(target, a1);
         return true;
      } catch (Throwable var5) {
         return false;
      }
   }

   private static boolean tryInvoke(Object target, String methodName, Class<?> p1, Class<?> p2, Object a1, Object a2) {
      try {
         Method m = target.getClass().getMethod(methodName, p1, p2);
         m.invoke(target, a1, a2);
         return true;
      } catch (Throwable var7) {
         return false;
      }
   }

   private static Object tryInvokeReturn(Object target, String methodName) {
      try {
         Method m = target.getClass().getMethod(methodName);
         return m.invoke(target);
      } catch (Throwable var3) {
         return null;
      }
   }
}
