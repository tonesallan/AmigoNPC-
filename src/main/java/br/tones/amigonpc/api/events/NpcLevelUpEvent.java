package br.tones.amigonpc.api.events;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import java.util.UUID;

public final class NpcLevelUpEvent {
   private final UUID ownerId;
   private final int oldLevel;
   private final int newLevel;
   private final long totalXp;
   private final NpcXpSource source;
   private final NpcXpContext context;

   public NpcLevelUpEvent(UUID ownerId, int oldLevel, int newLevel, long totalXp, NpcXpSource source, NpcXpContext context) {
      this.ownerId = ownerId;
      this.oldLevel = oldLevel;
      this.newLevel = newLevel;
      this.totalXp = totalXp;
      this.source = source;
      this.context = context;
   }

   public UUID getOwnerId() {
      return this.ownerId;
   }

   public int getOldLevel() {
      return this.oldLevel;
   }

   public int getNewLevel() {
      return this.newLevel;
   }

   public long getTotalXp() {
      return this.totalXp;
   }

   public NpcXpSource getSource() {
      return this.source;
   }

   public NpcXpContext getContext() {
      return this.context;
   }
}
