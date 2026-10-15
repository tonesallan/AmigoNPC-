package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.Entity.DefaultAnimations;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class AmigoDamageAndDownedSystem extends DamageEventSystem {
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

   public SystemGroup getGroup() {
      return DamageModule.get().getFilterDamageGroup();
   }

   public Query getQuery() {
      return Query.any();
   }

   public void handle(int entityIndex, ArchetypeChunk chunk, Store store, CommandBuffer buffer, Damage damage) {
      if (damage != null && !damage.isCancelled()) {
         Ref<EntityStore> targetRef = chunk.getReferenceTo(entityIndex);
         if (targetRef != null) {
            AmigoNpcManager manager = AmigoNpcManager.getShared();
            World world = null;

            try {
               Object ext = store.getExternalData();
               if (ext instanceof EntityStore) {
                  EntityStore es = (EntityStore)ext;
                  world = es.getWorld();
               }
            } catch (Throwable var31) {
            }

            try {
               Damage.Source src = damage.getSource();
               if (src instanceof Damage.EntitySource) {
                  Damage.EntitySource es = (Damage.EntitySource)src;
                  if (es.getRef() != null) {
                     Ref<EntityStore> srcRef = es.getRef();
                     Player srcPlayer = (Player)store.getComponent(srcRef, Player.getComponentType());
                     if (srcPlayer != null) {
                        UUIDComponent uuidComp = (UUIDComponent)store.getComponent(srcRef, UUIDComponent.getComponentType());
                        UUID ownerId = uuidComp == null ? null : uuidComp.getUuid();
                        if (ownerId != null && manager.hasNpc(ownerId)) {
                           manager.startAssist(ownerId, targetRef);
                           manager.recordCombatTag(ownerId, targetRef, store);
                        }
                     }
                  }
               }
            } catch (Throwable var30) {
            }

            try {
               Player maybePlayer = (Player)store.getComponent(targetRef, Player.getComponentType());
               if (maybePlayer != null) {
                  UUIDComponent uuidComp = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
                  UUID ownerId = uuidComp == null ? null : uuidComp.getUuid();
                  if (ownerId != null && manager.hasNpc(ownerId)) {
                     Ref<EntityStore> attackerRef = extractAttackerRef(damage.getSource());
                     if (attackerRef != null) {
                        manager.startCombat(ownerId, attackerRef);
                     } else {
                        try {
                           String srcName = damage.getSource() == null ? "null" : damage.getSource().getClass().getSimpleName();
                           manager.debugDamage(ownerId, "player tomou dano, mas não consegui extrair attackerRef (src=" + srcName + ")");
                        } catch (Throwable var28) {
                        }
                     }
                  }

                  try {
                     if (world != null) {
                        Ref<EntityStore> attackerRef = extractAttackerRef(damage.getSource());
                        if (attackerRef != null) {
                           float scaled = WorldMobScalingService.getShared().scaleOutgoingDamage(store, attackerRef, world, damage.getAmount());
                           damage.setAmount(scaled);
                           WorldMobScalingService.getShared().maybeApplyHpScaling(store, attackerRef, world);
                        }
                     }
                  } catch (Throwable var27) {
                  }

                  return;
               }
            } catch (Throwable var29) {
            }

            if (!manager.isAmigoRef(targetRef)) {
               try {
                  if (world != null) {
                     WorldMobScalingService.getShared().maybeApplyHpScaling(store, targetRef, world);
                  }
               } catch (Throwable var19) {
               }

            } else {
               UUID owner = manager.getOwnerFromRef(targetRef);

               try {
                  if (owner != null) {
                     Ref<EntityStore> attackerRef = extractAttackerRef(damage.getSource());
                     if (attackerRef != null) {
                        UUIDComponent attackerUuid = (UUIDComponent)store.getComponent(attackerRef, UUIDComponent.getComponentType());
                        if (attackerUuid != null && owner.equals(attackerUuid.getUuid())) {
                           damage.setCancelled(true);
                           return;
                        }
                     }
                  }
               } catch (Throwable var26) {
               }

               if (owner != null && manager.isDowned(owner)) {
                  damage.setCancelled(true);
               } else if (owner != null && manager.isGodMode(owner)) {
                  damage.setCancelled(true);
               } else {
                  try {
                     if (owner != null) {
                        Ref<EntityStore> attackerRef = extractAttackerRef(damage.getSource());
                        if (attackerRef != null) {
                           try {
                              if (world != null) {
                                 float scaled = WorldMobScalingService.getShared().scaleOutgoingDamage(store, attackerRef, world, damage.getAmount());
                                 damage.setAmount(scaled);
                                 WorldMobScalingService.getShared().maybeApplyHpScaling(store, attackerRef, world);
                              }
                           } catch (Throwable var25) {
                           }

                           Player maybePlayerAttacker = (Player)store.getComponent(attackerRef, Player.getComponentType());
                           if (maybePlayerAttacker == null || manager.isPvpEnabled()) {
                              manager.startNpcCombat(owner, attackerRef);
                           }
                        } else {
                           try {
                              String srcName = damage.getSource() == null ? "null" : damage.getSource().getClass().getSimpleName();
                              manager.debugDamage(owner, "NPC tomou dano, mas não consegui extrair attackerRef (src=" + srcName + ")");
                           } catch (Throwable var24) {
                           }
                        }
                     }
                  } catch (Throwable var32) {
                  }

                  if (owner != null) {
                     long now = System.currentTimeMillis();

                     try {
                        manager.notifyNpcDamaged(owner, now);
                     } catch (Throwable var23) {
                     }
                  }

                  EntityStatMap stats = (EntityStatMap)buffer.ensureAndGetComponent(targetRef, EntityStatMap.getComponentType());
                  int healthIdx = DefaultEntityStatTypes.getHealth();

                  float currentHp;
                  try {
                     currentHp = stats.get(healthIdx).get();
                  } catch (Throwable var22) {
                     stats.maximizeStatValue(healthIdx);
                     currentHp = stats.get(healthIdx).get();
                  }

                  float incoming = damage.getAmount();
                  if (owner != null) {
                     incoming = manager.mitigateIncomingDamage(owner, incoming);
                  }

                  float newHp = currentHp - incoming;
                  ActiveAnimationComponent anim = (ActiveAnimationComponent)buffer.ensureAndGetComponent(targetRef, ActiveAnimationComponent.getComponentType());
                  MovementStatesComponent move = (MovementStatesComponent)buffer.ensureAndGetComponent(targetRef, MovementStatesComponent.getComponentType());
                  MovementStates states = move.getMovementStates();
                  if (states == null) {
                     states = new MovementStates();
                     states.idle = true;
                     states.onGround = true;
                     move.setMovementStates(states);
                     buffer.replaceComponent(targetRef, MovementStatesComponent.getComponentType(), move);
                  }

                  if (newHp <= 0.0F) {
                     stats.setStatValue(healthIdx, 1.0F);
                     buffer.replaceComponent(targetRef, EntityStatMap.getComponentType(), stats);

                     try {
                        String[] deathIds = DefaultAnimations.getDeathAnimationIds(states, damage.getCause());
                        if (deathIds != null && deathIds.length > 0 && deathIds[0] != null) {
                           anim.setPlayingAnimation(AnimationSlot.Action, deathIds[0]);
                           buffer.replaceComponent(targetRef, ActiveAnimationComponent.getComponentType(), anim);
                        }
                     } catch (Throwable var20) {
                     }

                     if (owner != null) {
                        manager.markDowned(owner);
                     }

                     damage.setCancelled(true);
                  } else {
                     stats.setStatValue(healthIdx, newHp);
                     buffer.replaceComponent(targetRef, EntityStatMap.getComponentType(), stats);

                     try {
                        String[] hurtIds = DefaultAnimations.getHurtAnimationIds(states, damage.getCause());
                        if (hurtIds != null && hurtIds.length > 0 && hurtIds[0] != null) {
                           anim.setPlayingAnimation(AnimationSlot.Action, hurtIds[0]);
                           buffer.replaceComponent(targetRef, ActiveAnimationComponent.getComponentType(), anim);
                        }
                     } catch (Throwable var21) {
                     }

                     damage.setCancelled(true);
                  }
               }
            }
         }
      }
   }
}
