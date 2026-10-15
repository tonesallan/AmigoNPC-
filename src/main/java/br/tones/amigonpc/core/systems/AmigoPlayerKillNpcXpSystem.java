package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.api.AmigoNPCApi;
import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgLevelingStage1Config;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1Formulas;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelCalculator;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelHelper;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AmigoPlayerKillNpcXpSystem extends DamageEventSystem {
   private static final RpgLevelingStage1Config STAGE1_CFG = new RpgLevelingStage1Config();
   private static final RpgStage1MobLevelCalculator STAGE1_MOB_CALC;
   private static final ConcurrentHashMap STAGE1_REMAINDER;
   private static final ConcurrentHashMap RECENT_KILLS;

   public SystemGroup getGroup() {
      try {
         return DamageModule.get().getInspectDamageGroup();
      } catch (Throwable var2) {
         return DamageModule.get().getFilterDamageGroup();
      }
   }

   public Query getQuery() {
      return Query.any();
   }

   public void handle(int entityIndex, ArchetypeChunk chunk, Store store, CommandBuffer buffer, Damage damage) {
      if (damage != null && !damage.isCancelled()) {
         Ref<EntityStore> targetRef = chunk.getReferenceTo(entityIndex);
         if (targetRef != null) {
            try {
               if (store.getComponent(targetRef, Player.getComponentType()) != null) {
                  return;
               }
            } catch (Throwable var36) {
            }

            AmigoNpcManager manager = AmigoNpcManager.getShared();

            try {
               if (manager.isAmigoRef(targetRef)) {
                  return;
               }
            } catch (Throwable var35) {
            }

            float hp = Float.NaN;
            boolean dead = false;

            try {
               DeathComponent dc = (DeathComponent)store.getComponent(targetRef, DeathComponent.getComponentType());
               if (dc != null) {
                  dead = true;
               }
            } catch (Throwable var34) {
            }

            try {
               EntityStatMap stats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
               if (stats != null) {
                  hp = stats.get(DefaultEntityStatTypes.getHealth()).get();
                  if (hp <= 0.0F) {
                     dead = true;
                  }
               }
            } catch (Throwable var33) {
            }

            if (!dead) {
               try {
                  float amt = damage.getAmount();
                  if (!Float.isNaN(hp) && hp > 0.0F && amt > 0.0F && hp - amt <= 0.001F) {
                     dead = true;
                  }
               } catch (Throwable var32) {
               }
            }

            if (dead) {
               Ref<EntityStore> attackerRef = extractAttackerRef(damage.getSource());
               if (attackerRef != null) {
                  try {
                     if (store.getComponent(attackerRef, Player.getComponentType()) == null) {
                        return;
                     }
                  } catch (Throwable var38) {
                     return;
                  }

                  UUID ownerId;
                  try {
                     UUIDComponent uuidComp = (UUIDComponent)store.getComponent(attackerRef, UUIDComponent.getComponentType());
                     ownerId = uuidComp == null ? null : uuidComp.getUuid();
                  } catch (Throwable var31) {
                     ownerId = null;
                  }

                  if (ownerId != null) {
                     try {
                        if (!manager.isNpcActive(ownerId)) {
                           return;
                        }
                     } catch (Throwable var37) {
                        return;
                     }

                     try {
                        Vector3d anchorPos = null;

                        try {
                           TransformComponent tc = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
                           if (tc != null && tc.getPosition() != null) {
                              anchorPos = tc.getPosition();
                           }
                        } catch (Throwable var29) {
                        }

                        if (anchorPos == null) {
                           try {
                              TransformComponent atc = (TransformComponent)store.getComponent(attackerRef, TransformComponent.getComponentType());
                              if (atc != null && atc.getPosition() != null) {
                                 anchorPos = atc.getPosition();
                              }
                           } catch (Throwable var28) {
                           }
                        }

                        manager.scheduleAutoLootSecondScan(ownerId, anchorPos, targetRef);
                     } catch (Throwable var30) {
                     }

                     String mobId = null;

                     try {
                        UUIDComponent mobUuid = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
                        if (mobUuid != null && mobUuid.getUuid() != null) {
                           mobId = mobUuid.getUuid().toString();
                        }
                     } catch (Throwable var27) {
                     }

                     if (mobId != null) {
                        long now = System.currentTimeMillis();
                        Long last = (Long)RECENT_KILLS.get(mobId);
                        if (last != null && now - last < 1500L) {
                           return;
                        }

                        RECENT_KILLS.put(mobId, now);
                        if (RECENT_KILLS.size() > 5000) {
                           RECENT_KILLS.clear();
                        }
                     }

                     int npcLevel;
                     try {
                        npcLevel = Math.max(1, manager.getNpcLevel(ownerId));
                     } catch (Throwable var26) {
                        npcLevel = 1;
                     }

                     int mobLevel = 1;

                     try {
                        mobLevel = RpgStage1MobLevelHelper.getMonsterLevel(store, targetRef, targetRef, STAGE1_MOB_CALC, STAGE1_CFG);
                        if (mobLevel < 1) {
                           mobLevel = 1;
                        }
                     } catch (Throwable var25) {
                     }

                     double xpD;
                     try {
                        xpD = RpgStage1Formulas.xpFromKill(npcLevel, mobLevel, STAGE1_CFG);
                     } catch (Throwable var24) {
                        xpD = (double)0.0F;
                     }

                     long xpGain = coerceStage1XpGainToLong(ownerId, xpD);
                     if (xpGain > 0L) {
                        String worldName = null;

                        try {
                           Object ext = store.getExternalData();
                           if (ext instanceof EntityStore) {
                              EntityStore es = (EntityStore)ext;
                              if (es.getWorld() != null) {
                                 worldName = es.getWorld().getName();
                              }
                           }
                        } catch (Throwable var23) {
                        }

                        NpcXpContext ctx = new NpcXpContext(worldName, worldName, (String)null, 0, mobId, mobLevel, System.currentTimeMillis());

                        try {
                           AmigoNPCApi.addNpcXp(ownerId, xpGain, NpcXpSource.COMBAT_KILL, ctx);
                        } catch (Throwable var22) {
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private static Ref extractAttackerRef(Damage.Source src) {
      if (src == null) {
         return null;
      } else {
         try {
            if (src instanceof Damage.EntitySource) {
               Damage.EntitySource es = (Damage.EntitySource)src;
               return es.getRef();
            }
         } catch (Throwable var11) {
         }

         String[] methods = new String[]{"getRef", "getEntityRef", "getSourceRef", "getOwnerRef", "getShooterRef", "getAttackerRef"};

         for(String mname : methods) {
            try {
               Method m = src.getClass().getMethod(mname);
               Object v = m.invoke(src);
               if (v instanceof Ref) {
                  return (Ref)v;
               }
            } catch (Throwable var10) {
            }
         }

         String[] fields = new String[]{"ref", "entityRef", "sourceRef", "ownerRef", "shooterRef", "attackerRef"};

         for(String fname : fields) {
            try {
               Field f = src.getClass().getDeclaredField(fname);
               f.setAccessible(true);
               Object v = f.get(src);
               if (v instanceof Ref) {
                  return (Ref)v;
               }
            } catch (Throwable var9) {
            }
         }

         return null;
      }
   }

   private static long coerceStage1XpGainToLong(UUID ownerId, double xpGainDouble) {
      if (ownerId == null) {
         return 0L;
      } else if (xpGainDouble > (double)0.0F && !Double.isNaN(xpGainDouble) && !Double.isInfinite(xpGainDouble)) {
         double rem = (Double)STAGE1_REMAINDER.getOrDefault(ownerId, (double)0.0F);
         if (Double.isNaN(rem) || Double.isInfinite(rem) || rem < (double)0.0F) {
            rem = (double)0.0F;
         }

         double sum = xpGainDouble + rem;
         long whole = (long)Math.floor(sum);
         double newRem = sum - (double)whole;
         if (!(newRem >= (double)0.0F) || Double.isNaN(newRem) || Double.isInfinite(newRem)) {
            newRem = (double)0.0F;
         }

         STAGE1_REMAINDER.put(ownerId, newRem);
         if (STAGE1_REMAINDER.size() > 5000) {
            STAGE1_REMAINDER.clear();
         }

         return Math.max(0L, whole);
      } else {
         return 0L;
      }
   }

   static {
      STAGE1_MOB_CALC = new RpgStage1MobLevelCalculator(STAGE1_CFG);
      STAGE1_REMAINDER = new ConcurrentHashMap();
      RECENT_KILLS = new ConcurrentHashMap();
   }
}
