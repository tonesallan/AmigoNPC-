package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RpgStage1MobLevelHelper {
   private RpgStage1MobLevelHelper() {
   }

   public static int getMonsterLevel(
      Store<EntityStore> store, Ref<EntityStore> targetRef, Object targetRefObj, RpgStage1MobLevelCalculator calc, RpgLevelingStage1Config cfg
   ) {
      if (store != null && targetRef != null && calc != null && cfg != null) {
         RpgStage1MobLevelResolvedInputSupport.ResolvedInput input = RpgStage1MobLevelResolvedInputSupport.resolve(store, targetRef, targetRefObj, cfg);
         return input == null ? 0 : calc.computeLevel(input.uuid, input.maxHp, null, null, input.instanceId);
      } else {
         return 0;
      }
   }
}
