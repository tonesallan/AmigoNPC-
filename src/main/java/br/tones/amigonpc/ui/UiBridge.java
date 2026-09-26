package br.tones.amigonpc.ui;

import br.tones.amigonpc.core.i18n.AmigoText;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class UiBridge {
   private static volatile String LAST_ERROR;

   private UiBridge() {
   }

   public static String getLastError() {
      return LAST_ERROR;
   }

   private static void setError(String msg) {
      LAST_ERROR = msg;
   }

   public static boolean openAmigoMainUi(Object playerSender) {
      if (playerSender == null) {
         setError(AmigoText.text("ui.legacy.bridge.error.player_sender_null"));
         return false;
      }

      try {
         Class<?> builderClass = tryLoad(
            "com.hypixel.hytale.server.core.ui.UICommandBuilder",
            "com.hypixel.hytale.server.core.ui.command.UICommandBuilder",
            "com.hypixel.hytale.server.core.ui.uicommands.UICommandBuilder"
         );
         if (builderClass == null) {
            setError(AmigoText.text("ui.legacy.bridge.error.builder_missing"));
            return false;
         }

         Object builder = instantiate(builderClass);
         if (builder == null) {
            setError(AmigoText.text("ui.legacy.bridge.error.builder_instantiate_failed"));
            return false;
         }

         tryInvoke(builder, "title", String.class, AmigoText.text("ui.legacy.bridge.title"));
         tryInvoke(builder, "setTitle", String.class, AmigoText.text("ui.legacy.bridge.title"));
         tryInvoke(builder, "text", String.class, AmigoText.text("ui.legacy.bridge.description"));
         tryInvoke(builder, "setText", String.class, AmigoText.text("ui.legacy.bridge.description"));
         String closeLabel = AmigoText.text("ui.legacy.bridge.button.close");
         tryInvoke(builder, "button", String.class, String.class, closeLabel, "close");
         tryInvoke(builder, "addButton", String.class, String.class, closeLabel, "close");
         tryInvoke(builder, "option", String.class, String.class, closeLabel, "close");
         Object uiObj = null;
         uiObj = tryInvokeReturn(builder, "build");
         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "create");
         }

         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "finish");
         }

         if (uiObj == null) {
            uiObj = tryInvokeReturn(builder, "getResult");
            if (uiObj == null) {
               uiObj = tryInvokeReturn(builder, "getCommand");
            }
         }

         if (uiObj == null) {
            setError(AmigoText.text("ui.legacy.bridge.error.result_unavailable"));
            return false;
         }

         if (tryInvoke(playerSender, "openUi", Object.class, uiObj)) {
            return true;
         }

         if (tryInvoke(playerSender, "openUI", Object.class, uiObj)) {
            return true;
         }

         if (tryInvoke(playerSender, "showUi", Object.class, uiObj)) {
            return true;
         }

         if (tryInvoke(playerSender, "showUI", Object.class, uiObj)) {
            return true;
         }

         if (tryInvoke(playerSender, "sendUi", Object.class, uiObj)) {
            return true;
         }

         if (tryInvoke(playerSender, "sendUI", Object.class, uiObj)) {
            return true;
         }

         Object uiManager = tryInvokeReturn(playerSender, "getUiManager");
         if (uiManager != null) {
            if (tryInvoke(uiManager, "open", Object.class, uiObj)) {
               return true;
            }

            if (tryInvoke(uiManager, "show", Object.class, uiObj)) {
               return true;
            }

            if (tryInvoke(uiManager, "send", Object.class, uiObj)) {
               return true;
            }
         }

         setError(AmigoText.text("ui.legacy.bridge.error.open_method_missing"));
         return false;
      } catch (Throwable t) {
         setError(AmigoText.format("ui.legacy.bridge.error.open_failed", t.getClass().getSimpleName(), t.getMessage()));
         return false;
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
