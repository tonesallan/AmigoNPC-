package br.tones.amigonpc.api.events;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import java.util.UUID;

public final class NpcExperienceGainedEvent {
   private final UUID ownerId;
   private final long amount;
   private final long oldTotalXp;
   private final long newTotalXp;
   private final int oldLevel;
   private final int newLevel;
   private final NpcXpSource source;
   private final NpcXpContext context;

   public NpcExperienceGainedEvent(
      UUID ownerId, long amount, long oldTotalXp, long newTotalXp, int oldLevel, int newLevel, NpcXpSource source, NpcXpContext context
   ) {
      this.ownerId = ownerId;
      this.amount = amount;
      this.oldTotalXp = oldTotalXp;
      this.newTotalXp = newTotalXp;
      this.oldLevel = oldLevel;
      this.newLevel = newLevel;
      this.source = source;
      this.context = context;
   }

   public UUID getOwnerId() {
      return this.ownerId;
   }

   public long getAmount() {
      return this.amount;
   }

   public long getOldTotalXp() {
      return this.oldTotalXp;
   }

   public long getNewTotalXp() {
      return this.newTotalXp;
   }

   public int getOldLevel() {
      return this.oldLevel;
   }

   public int getNewLevel() {
      return this.newLevel;
   }

   public NpcXpSource getSource() {
      return this.source;
   }

   public NpcXpContext getContext() {
      return this.context;
   }
}
