package br.tones.amigonpc.core.debug;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;

public final class ServerErrorHook {
   private static volatile boolean INSTALLED;
   private static volatile UncaughtExceptionHandler PREV;

   private ServerErrorHook() {
   }

   public static void install() {
      if (!INSTALLED) {
         INSTALLED = true;

         try {
            PREV = Thread.getDefaultUncaughtExceptionHandler();
         } catch (Throwable ignored) {
            PREV = null;
         }

         try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
               try {
                  HashMap<String, Object> extra = new HashMap<>();

                  try {
                     extra.put("threadName", thread != null ? thread.getName() : null);
                  } catch (Throwable var6) {
                  }

                  try {
                     extra.put("threadId", thread != null ? thread.getId() : null);
                  } catch (Throwable var5) {
                  }

                  ErrorDumpService.getShared().dumpOnServerError(null, "uncaught_exception", throwable, extra);
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
