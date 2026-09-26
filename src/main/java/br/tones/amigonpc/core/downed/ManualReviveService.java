package br.tones.amigonpc.core.downed;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ManualReviveService {
   private static final ManualReviveService SHARED = new ManualReviveService();
   private static final long REQUIRED_HOLD_MILLIS = 10000L;
   private static final long SIGNAL_GRACE_MILLIS = 1200L;
   private final ConcurrentMap<UUID, Attempt> attempts = new ConcurrentHashMap<>();

   private ManualReviveService() {
   }

   public static ManualReviveService getShared() {
      return SHARED;
   }

   public void beginOrContinue(PlayerRef helper, UUID npcOwnerId) {
      if (helper == null || npcOwnerId == null || !AmigoNpcManager.getShared().isDowned(npcOwnerId)) {
         return;
      }

      UUID helperId = helper.getUuid();
      if (helperId == null) {
         return;
      }

      long now = System.currentTimeMillis();
      this.attempts.compute(helperId, (ignored, current) -> {
         if (current != null && npcOwnerId.equals(current.npcOwnerId)) {
            current.lastSignalMillis = now;
            return current;
         }

         return new Attempt(npcOwnerId, now, now);
      });
   }

   public void onUseChain(PlayerRef helper, SyncInteractionChain chain) {
      if (helper == null || chain == null || chain.interactionType == null) {
         return;
      }

      UUID helperId = helper.getUuid();
      if (helperId == null) {
         return;
      }

      Attempt attempt = this.attempts.get(helperId);
      if (attempt == null) {
         return;
      }

      long now = System.currentTimeMillis();
      InteractionState state = chain.state;
      if (state == InteractionState.NotFinished) {
         attempt.lastSignalMillis = now;
      } else if (state == InteractionState.Finished
         || state == InteractionState.Failed
         || state == InteractionState.Skip
         || state == InteractionState.ItemChanged) {
         this.attempts.remove(helperId, attempt);
      }
   }

   public void tick() {
      long now = System.currentTimeMillis();

      for (var entry : this.attempts.entrySet()) {
         UUID helperId = entry.getKey();
         Attempt attempt = entry.getValue();
         if (attempt == null) {
            this.attempts.remove(helperId);
            continue;
         }

         if (!AmigoNpcManager.getShared().isDowned(attempt.npcOwnerId)) {
            this.attempts.remove(helperId, attempt);
            continue;
         }

         if (now - attempt.lastSignalMillis > SIGNAL_GRACE_MILLIS) {
            this.attempts.remove(helperId, attempt);
            continue;
         }

         if (now - attempt.startedAtMillis >= REQUIRED_HOLD_MILLIS) {
            if (this.attempts.remove(helperId, attempt)) {
               AmigoNpcManager.getShared().revive(attempt.npcOwnerId, true);
            }
         }
      }
   }

   public void cancel(UUID helperId) {
      if (helperId != null) {
         this.attempts.remove(helperId);
      }
   }

   public void shutdown() {
      this.attempts.clear();
   }

   private static final class Attempt {
      private final UUID npcOwnerId;
      private final long startedAtMillis;
      private volatile long lastSignalMillis;

      private Attempt(UUID npcOwnerId, long startedAtMillis, long lastSignalMillis) {
         this.npcOwnerId = npcOwnerId;
         this.startedAtMillis = startedAtMillis;
         this.lastSignalMillis = lastSignalMillis;
      }
   }
}
