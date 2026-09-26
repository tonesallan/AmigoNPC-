package br.tones.amigonpc.core.xp.sources;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import java.util.UUID;

public final class NpcCollectXpSource {
   private NpcCollectXpSource() {
   }

   public static void onPickup(UUID ownerId, int pickedCount, String worldName) {
      if (ownerId != null) {
         if (pickedCount > 0) {
            NpcXpContext ctx = new NpcXpContext(worldName, worldName, null, 0, null, 0, System.currentTimeMillis());
            NpcXpAwardService.getShared().awardCollect(ownerId, NpcXpSource.COLLECT_PICKUP, ctx);
         }
      }
   }
}
