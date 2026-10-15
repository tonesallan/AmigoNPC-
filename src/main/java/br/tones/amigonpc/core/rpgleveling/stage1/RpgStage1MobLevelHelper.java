package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RpgStage1MobLevelHelper {
   private RpgStage1MobLevelHelper() {
   }

   private static String getRoleName(NPCEntity npc) {
      if (npc == null) {
         return null;
      } else {
         String roleName = null;

         try {
            roleName = npc.getRoleName();
         } catch (Throwable var4) {
         }

         if (roleName != null && !roleName.isEmpty()) {
            return roleName;
         } else {
            try {
               Role r = npc.getRole();
               return r != null ? r.getRoleName() : null;
            } catch (Throwable var3) {
               return null;
            }
         }
      }
   }

   private static boolean isEntityRoleLevelingBlacklisted(Store store, Ref ref, RpgLevelingStage1Config cfg) {
      if (cfg == null) {
         return false;
      } else {
         try {
            NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) {
               return false;
            } else {
               String roleName = getRoleName(npc);
               return cfg.isEntityRoleLevelingBlacklisted(roleName);
            }
         } catch (Throwable var5) {
            return false;
         }
      }
   }

   private static UUID tryExtractUuidFromRefObject(Object refObj) {
      if (refObj == null) {
         return null;
      } else {
         try {
            Method m = refObj.getClass().getMethod("getUuid");
            Object v = m.invoke(refObj);
            if (v instanceof UUID) {
               UUID u = (UUID)v;
               return u;
            }

            if (v instanceof String) {
               String s = (String)v;

               try {
                  return UUID.fromString(s);
               } catch (Throwable var9) {
               }
            }
         } catch (Throwable var10) {
         }

         try {
            Method m = refObj.getClass().getMethod("getId");
            Object v = m.invoke(refObj);
            if (v instanceof UUID) {
               UUID u = (UUID)v;
               return u;
            }

            if (v instanceof String) {
               String s = (String)v;

               try {
                  return UUID.fromString(s);
               } catch (Throwable var7) {
               }
            }
         } catch (Throwable var8) {
         }

         try {
            Method m = refObj.getClass().getMethod("id");
            Object v = m.invoke(refObj);
            if (v instanceof UUID) {
               UUID u = (UUID)v;
               return u;
            }

            if (v instanceof String) {
               String s = (String)v;

               try {
                  return UUID.fromString(s);
               } catch (Throwable var5) {
               }
            }
         } catch (Throwable var6) {
         }

         return null;
      }
   }

   public static int getMonsterLevel(Store store, Ref targetRef, Object targetRefObj, RpgStage1MobLevelCalculator calc, RpgLevelingStage1Config cfg) {
      if (store != null && targetRef != null && calc != null && cfg != null) {
         if (isEntityRoleLevelingBlacklisted(store, targetRef, cfg)) {
            return 0;
         } else {
            World world = null;

            try {
               Object ext = store.getExternalData();
               if (ext instanceof EntityStore) {
                  EntityStore es = (EntityStore)ext;
                  world = es.getWorld();
               }
            } catch (Throwable var13) {
            }

            String instanceId = RpgStage1InstanceLevelConfig.resolveInstanceId(world);
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
            } catch (Throwable var12) {
            }

            if (!(maxHp > 0.0F)) {
               return 0;
            } else {
               UUID uuid = null;

               try {
                  UUIDComponent uc = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
                  if (uc != null) {
                     uuid = uc.getUuid();
                  }
               } catch (Throwable var11) {
               }

               if (uuid == null) {
                  uuid = tryExtractUuidFromRefObject(targetRefObj);
               }

               if (uuid == null) {
                  String seed = "AmigoNPC:ref:" + String.valueOf(targetRefObj != null ? targetRefObj : targetRef);
                  uuid = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
               }

               return calc.computeLevel(uuid, maxHp, (Integer)null, (String)null, instanceId);
            }
         }
      } else {
         return 0;
      }
   }
}
