package br.tones.amigonpc.core.downed;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ManualReviveService {
   private static final ManualReviveService SHARED = new ManualReviveService();
   private static final long REQUIRED_HOLD_MILLIS = 10000L;
   private static final long ABANDONED_ATTEMPT_TIMEOUT_MILLIS = 20000L;
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

         trace("BEGIN helper=" + helperId + " owner=" + npcOwnerId);
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
      if (attempt == null || chain.interactionType != InteractionType.Use) {
         return;
      }

      long now = System.currentTimeMillis();
      InteractionState state = chain.state;
      trace("CHAIN helper=" + helperId + " chainId=" + chain.chainId + " state=" + state
         + " attemptChainId=" + attempt.chainId + " holdConfirmed=" + attempt.holdConfirmed
         + " elapsedMs=" + (now - attempt.startedAtMillis));
      if (state == InteractionState.NotFinished) {
         if (attempt.chainId != Integer.MIN_VALUE && attempt.chainId != chain.chainId) {
            return;
         }

         attempt.chainId = chain.chainId;
         attempt.lastSignalMillis = now;
         attempt.holdConfirmed = true;
      } else if (state == InteractionState.Finished
         || state == InteractionState.Failed
         || state == InteractionState.Skip
         || state == InteractionState.ItemChanged) {
         if (attempt.chainId == Integer.MIN_VALUE || attempt.chainId == chain.chainId) {
            this.attempts.remove(helperId, attempt);
         }
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
            trace("END-NOT-DOWNED helper=" + helperId);
            this.attempts.remove(helperId, attempt);
            continue;
         }

         if (now - attempt.startedAtMillis > ABANDONED_ATTEMPT_TIMEOUT_MILLIS) {
            trace("END-ABANDONED helper=" + helperId + " holdConfirmed=" + attempt.holdConfirmed);
            this.attempts.remove(helperId, attempt);
            continue;
         }

         if (attempt.holdConfirmed && now - attempt.startedAtMillis >= REQUIRED_HOLD_MILLIS) {
            if (this.attempts.remove(helperId, attempt)) {
               trace("REVIVE helper=" + helperId + " owner=" + attempt.npcOwnerId);
               AmigoNpcManager.getShared().revive(attempt.npcOwnerId, true);
            }
         }
      }
   }

   public void cancel(UUID helperId) {
      if (helperId != null && this.attempts.remove(helperId) != null) {
         trace("CANCEL helper=" + helperId);
      }
   }

   private static void trace(String message) {
      try {
         Path dir = Constants.UNIVERSE_PATH.resolve("amigonpc");
         Files.createDirectories(dir);
         String line = Instant.now().toString() + " " + message + System.lineSeparator();
         Files.writeString(dir.resolve("revive-trace.log"), line, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException | RuntimeException ignored) {
      }
   }

   public void shutdown() {
      this.attempts.clear();
   }

   private static final class Attempt {
      private final UUID npcOwnerId;
      private final long startedAtMillis;
      private volatile long lastSignalMillis;
      private volatile int chainId = Integer.MIN_VALUE;
      private volatile boolean holdConfirmed;

      private Attempt(UUID npcOwnerId, long startedAtMillis, long lastSignalMillis) {
         this.npcOwnerId = npcOwnerId;
         this.startedAtMillis = startedAtMillis;
         this.lastSignalMillis = lastSignalMillis;
      }
   }
}
