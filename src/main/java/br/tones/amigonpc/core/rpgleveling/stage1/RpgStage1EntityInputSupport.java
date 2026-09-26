package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

final class RpgStage1EntityInputSupport {
   private RpgStage1EntityInputSupport() {
   }

   static boolean isEntityRoleLevelingBlacklisted(Store<EntityStore> store, Ref<EntityStore> ref, RpgLevelingStage1Config cfg) {
      if (cfg == null) {
         return false;
      }

      try {
         NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
         if (npc == null) {
            return false;
         }

         String roleName = getRoleName(npc);
         return cfg.isEntityRoleLevelingBlacklisted(roleName);
      } catch (Throwable ignored) {
         return false;
      }
   }

   static String resolveInstanceId(Store<EntityStore> store) {
      World world = null;

      try {
         if (store.getExternalData() instanceof EntityStore es) {
            world = es.getWorld();
         }
      } catch (Throwable var4) {
      }

      return RpgStage1InstanceLevelConfig.resolveInstanceId(world);
   }

   static float resolveMaxHp(Store<EntityStore> store, Ref<EntityStore> targetRef) {
      float maxHp = 0.0F;

      try {
         EntityStatMap stats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
         if (stats != null) {
            int healthIdx = DefaultEntityStatTypes.getHealth();
            EntityStatValue v = stats.get(healthIdx);
            if (v != null) {
               maxHp = v.getMax();
            }
         }
      } catch (Throwable var6) {
      }

      return maxHp;
   }

   private static String getRoleName(NPCEntity npc) {
      if (npc == null) {
         return null;
      }

      String roleName = null;

      try {
         roleName = npc.getRoleName();
      } catch (Throwable var4) {
      }

      if (roleName != null && !roleName.isEmpty()) {
         return roleName;
      }

      try {
         Role r = npc.getRole();
         return r != null ? r.getRoleName() : null;
      } catch (Throwable ignored) {
         return null;
      }
   }
}
