package br.tones.amigonpc.ui;

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
         setError("playerSender é null.");
         return false;
      } else {
         try {
            Class<?> builderClass = tryLoad("com.hypixel.hytale.server.core.ui.UICommandBuilder", "com.hypixel.hytale.server.core.ui.command.UICommandBuilder", "com.hypixel.hytale.server.core.ui.uicommands.UICommandBuilder");
            if (builderClass == null) {
               setError("UICommandBuilder não encontrado nesta build.");
               return false;
            } else {
               Object builder = instantiate(builderClass);
               if (builder == null) {
                  setError("Falha ao instanciar UICommandBuilder.");
                  return false;
               } else {
                  tryInvoke(builder, "title", String.class, "AmigoNPC");
                  tryInvoke(builder, "setTitle", String.class, "AmigoNPC");
                  tryInvoke(builder, "text", String.class, "UI base carregada.\nPróximo: botões e status do NPC.");
                  tryInvoke(builder, "setText", String.class, "UI base carregada.\nPróximo: botões e status do NPC.");
                  tryInvoke(builder, "button", String.class, String.class, "Fechar", "close");
                  tryInvoke(builder, "addButton", String.class, String.class, "Fechar", "close");
                  tryInvoke(builder, "option", String.class, String.class, "Fechar", "close");
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
                     setError("Não consegui obter o objeto final da UI (build/create/finish/getResult).");
                     return false;
                  } else if (tryInvoke(playerSender, "openUi", Object.class, uiObj)) {
                     return true;
                  } else if (tryInvoke(playerSender, "openUI", Object.class, uiObj)) {
                     return true;
                  } else if (tryInvoke(playerSender, "showUi", Object.class, uiObj)) {
                     return true;
                  } else if (tryInvoke(playerSender, "showUI", Object.class, uiObj)) {
                     return true;
                  } else if (tryInvoke(playerSender, "sendUi", Object.class, uiObj)) {
                     return true;
                  } else if (tryInvoke(playerSender, "sendUI", Object.class, uiObj)) {
                     return true;
                  } else {
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

                     setError("Não achei método para abrir UI no player (openUi/showUi/sendUi/getUiManager...).");
                     return false;
                  }
               }
            }
         } catch (Throwable t) {
            String var10000 = t.getClass().getSimpleName();
            setError("openAmigoMainUi falhou: " + var10000 + " - " + t.getMessage());
            return false;
         }
      }
   }

   private static Class tryLoad(String... names) {
      for(String n : names) {
         try {
            return Class.forName(n);
         } catch (Throwable var6) {
         }
      }

      return null;
   }

   private static Object instantiate(Class c) {
      try {
         Constructor<?> ctor = c.getDeclaredConstructor();
         ctor.setAccessible(true);
         return ctor.newInstance();
      } catch (Throwable var2) {
         return null;
      }
   }

   private static boolean tryInvoke(Object target, String methodName, Class p1, Object a1) {
      try {
         Method m = target.getClass().getMethod(methodName, p1);
         m.invoke(target, a1);
         return true;
      } catch (Throwable var5) {
         return false;
      }
   }

   private static boolean tryInvoke(Object target, String methodName, Class p1, Class p2, Object a1, Object a2) {
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
