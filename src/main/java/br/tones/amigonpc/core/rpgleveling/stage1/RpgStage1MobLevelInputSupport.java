package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class RpgStage1MobLevelInputSupport {
   private RpgStage1MobLevelInputSupport() {
   }

   static boolean isEntityRoleLevelingBlacklisted(Store<EntityStore> store, Ref<EntityStore> ref, RpgLevelingStage1Config cfg) {
      return RpgStage1EntityInputSupport.isEntityRoleLevelingBlacklisted(store, ref, cfg);
   }

   static String resolveInstanceId(Store<EntityStore> store) {
      return RpgStage1EntityInputSupport.resolveInstanceId(store);
   }

   static float resolveMaxHp(Store<EntityStore> store, Ref<EntityStore> targetRef) {
      return RpgStage1EntityInputSupport.resolveMaxHp(store, targetRef);
   }

   static UUID resolveTargetUuid(Store<EntityStore> store, Ref<EntityStore> targetRef, Object targetRefObj) {
      return RpgStage1UuidSupport.resolveTargetUuid(store, targetRef, targetRefObj);
   }
}
