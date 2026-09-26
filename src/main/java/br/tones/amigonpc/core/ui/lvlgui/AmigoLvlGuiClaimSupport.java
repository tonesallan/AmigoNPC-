package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.rewards.RewardsService;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiClaimSupport {
   private AmigoLvlGuiClaimSupport() {
   }

   static void claimAll(UUID ownerId, PlayerRef viewerPlayerRef) {
      String ownerName = AmigoLvlGuiPermissionSupport.bestEffortViewerName(viewerPlayerRef, ownerId);
      int npcLevel = AmigoNpcManager.getShared().getNpcLevel(ownerId);
      RewardsService.getShared().claimAll(ownerId, ownerName, npcLevel);
   }

   static boolean claimLevel(UUID ownerId, PlayerRef viewerPlayerRef, int rewardLevel) {
      if (rewardLevel <= 0) {
         return false;
      }

      String ownerName = AmigoLvlGuiPermissionSupport.bestEffortViewerName(viewerPlayerRef, ownerId);
      int npcLevel = AmigoNpcManager.getShared().getNpcLevel(ownerId);
      RewardsService.getShared().claim(ownerId, ownerName, npcLevel, rewardLevel);
      return true;
   }
}
