package br.tones.amigonpc.core.debug;

import java.util.HashMap;
import java.util.UUID;

public final class ServerErrorHook {
   private static volatile boolean INSTALLED;
   private static volatile Thread.UncaughtExceptionHandler PREV;

   private ServerErrorHook() {
   }

   public static void install() {
      if (!INSTALLED) {
         INSTALLED = true;

         try {
            PREV = Thread.getDefaultUncaughtExceptionHandler();
         } catch (Throwable var2) {
            PREV = null;
         }

         try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
               try {
                  HashMap<String, Object> extra = new HashMap();

                  try {
                     extra.put("threadName", thread != null ? thread.getName() : null);
                  } catch (Throwable var6) {
                  }

                  try {
                     extra.put("threadId", thread != null ? thread.getId() : null);
                  } catch (Throwable var5) {
                  }

                  ErrorDumpService.getShared().dumpOnServerError((UUID)null, "uncaught_exception", throwable, extra);
               } catch (Throwable var7) {
               }

               try {
                  if (PREV != null) {
                     PREV.uncaughtException(thread, throwable);
                  }
               } catch (Throwable var4) {
               }

            });
         } catch (Throwable var1) {
         }

      }
   }
}
