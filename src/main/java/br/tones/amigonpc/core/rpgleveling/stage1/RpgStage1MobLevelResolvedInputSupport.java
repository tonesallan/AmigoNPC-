package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class RpgStage1MobLevelResolvedInputSupport {
   private RpgStage1MobLevelResolvedInputSupport() {
   }

   static RpgStage1MobLevelResolvedInputSupport.ResolvedInput resolve(
      Store<EntityStore> store, Ref<EntityStore> targetRef, Object targetRefObj, RpgLevelingStage1Config cfg
   ) {
      if (store == null || targetRef == null || cfg == null) {
         return null;
      }

      if (RpgStage1MobLevelInputSupport.isEntityRoleLevelingBlacklisted(store, targetRef, cfg)) {
         return null;
      }

      String instanceId = RpgStage1MobLevelInputSupport.resolveInstanceId(store);
      float maxHp = RpgStage1MobLevelInputSupport.resolveMaxHp(store, targetRef);
      if (!(maxHp > 0.0F)) {
         return null;
      }

      UUID uuid = RpgStage1MobLevelInputSupport.resolveTargetUuid(store, targetRef, targetRefObj);
      return new RpgStage1MobLevelResolvedInputSupport.ResolvedInput(uuid, maxHp, instanceId);
   }

   static final class ResolvedInput {
      final UUID uuid;
      final float maxHp;
      final String instanceId;

      ResolvedInput(UUID uuid, float maxHp, String instanceId) {
         this.uuid = uuid;
         this.maxHp = maxHp;
         this.instanceId = instanceId;
      }
   }
}
