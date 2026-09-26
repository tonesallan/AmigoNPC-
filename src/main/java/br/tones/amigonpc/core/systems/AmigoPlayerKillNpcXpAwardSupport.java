package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.api.AmigoNPCApi;
import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgLevelingStage1Config;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1Formulas;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelCalculator;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoPlayerKillNpcXpAwardSupport {
   private AmigoPlayerKillNpcXpAwardSupport() {
   }

   static boolean isNpcActive(AmigoNpcManager manager, UUID ownerId) {
      if (manager != null && ownerId != null) {
         try {
            return manager.isNpcActive(ownerId);
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   static int resolveNpcLevel(AmigoNpcManager manager, UUID ownerId) {
      if (manager != null && ownerId != null) {
         try {
            return Math.max(1, manager.getNpcLevel(ownerId));
         } catch (Throwable ignored) {
            return 1;
         }
      } else {
         return 1;
      }
   }

   static int resolveMobLevel(Store<EntityStore> store, Ref<EntityStore> targetRef, RpgStage1MobLevelCalculator calc, RpgLevelingStage1Config cfg) {
      if (store != null && targetRef != null && calc != null && cfg != null) {
         try {
            int mobLevel = RpgStage1MobLevelHelper.getMonsterLevel(store, targetRef, targetRef, calc, cfg);
            return Math.max(1, mobLevel);
         } catch (Throwable ignored) {
            return 1;
         }
      } else {
         return 1;
      }
   }

   static double resolveXpFromKill(int npcLevel, int mobLevel, RpgLevelingStage1Config cfg) {
      try {
         return RpgStage1Formulas.xpFromKill(npcLevel, mobLevel, cfg);
      } catch (Throwable ignored) {
         return 0.0;
      }
   }

   static NpcXpContext buildKillContext(Store<EntityStore> store, String mobId, int mobLevel) {
      String worldName = null;

      try {
         if (store.getExternalData() instanceof EntityStore es && es.getWorld() != null) {
            worldName = es.getWorld().getName();
         }
      } catch (Throwable var6) {
      }

      return new NpcXpContext(worldName, worldName, null, 0, mobId, mobLevel, System.currentTimeMillis());
   }

   static void awardXp(UUID ownerId, long xpGain, NpcXpContext ctx) {
      if (ownerId != null && xpGain > 0L && ctx != null) {
         try {
            AmigoNPCApi.addNpcXp(ownerId, xpGain, NpcXpSource.COMBAT_KILL, ctx);
         } catch (Throwable var5) {
         }
      }
   }
}
