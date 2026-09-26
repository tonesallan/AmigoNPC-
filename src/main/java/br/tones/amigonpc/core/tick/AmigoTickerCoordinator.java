package br.tones.amigonpc.core.tick;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.debug.ErrorDumpService;
import br.tones.amigonpc.core.downed.ManualReviveService;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class AmigoTickerCoordinator {
   private ScheduledExecutorService scheduler;
   private ScheduledFuture<?> downedTicker;
   private ScheduledFuture<?> followTicker;
   private ScheduledFuture<?> hudTicker;

   public void startDownedTicker() {
      try {
         this.ensureScheduler();

         if (this.downedTicker != null && !this.downedTicker.isDone()) {
            return;
         }

         this.downedTicker = this.scheduler.scheduleAtFixedRate(() -> {
            try {
               AmigoNpcManager.getShared().tickDowned();
            } catch (Throwable var3) {
               Throwable t = var3;

               try {
                  ErrorDumpService.getShared().dumpOnServerError(null, "ticker:downed", t);
               } catch (Throwable var2x) {
               }
            }
         }, 1L, 1L, TimeUnit.SECONDS);
      } catch (Throwable var2) {
      }
   }

   private void ensureScheduler() {
      if (this.scheduler == null || this.scheduler.isShutdown() || this.scheduler.isTerminated()) {
         this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AmigoNPC-DownedTicker");
            t.setDaemon(true);
            return t;
         });
      }
   }

   public void startFollowTicker() {
      try {
         this.ensureScheduler();

         if (this.followTicker != null && !this.followTicker.isDone()) {
            return;
         }

         this.followTicker = this.scheduler.scheduleAtFixedRate(() -> {
            try {
               AmigoNpcManager.getShared().tickFollow();
               ManualReviveService.getShared().tick();
            } catch (Throwable var3) {
               Throwable t = var3;

               try {
                  ErrorDumpService.getShared().dumpOnServerError(null, "ticker:follow", t);
               } catch (Throwable var2x) {
               }
            }
         }, 200L, 200L, TimeUnit.MILLISECONDS);
      } catch (Throwable var2) {
      }
   }

   public void startHudTicker() {
      try {
         this.ensureScheduler();

         if (this.hudTicker != null && !this.hudTicker.isDone()) {
            return;
         }

         this.hudTicker = this.scheduler.scheduleAtFixedRate(() -> {
            try {
               LevelProgressHudService.getShared().tick();
            } catch (Throwable var3) {
               Throwable t = var3;

               try {
                  ErrorDumpService.getShared().dumpOnServerError(null, "ticker:hud", t);
               } catch (Throwable var2x) {
               }
            }
         }, 250L, 250L, TimeUnit.MILLISECONDS);
      } catch (Throwable var2) {
      }
   }

   public void shutdown() {
      try {
         if (this.downedTicker != null) {
            this.downedTicker.cancel(false);
         }
      } catch (Throwable var5) {
      }

      try {
         if (this.followTicker != null) {
            this.followTicker.cancel(false);
         }
      } catch (Throwable var4) {
      }

      try {
         if (this.hudTicker != null) {
            this.hudTicker.cancel(false);
         }
      } catch (Throwable var3) {
      }

      try {
         if (this.scheduler != null) {
            this.scheduler.shutdownNow();
         }
      } catch (Throwable var2) {
      } finally {
         this.downedTicker = null;
         this.followTicker = null;
         this.hudTicker = null;
         this.scheduler = null;
      }
   }
}
