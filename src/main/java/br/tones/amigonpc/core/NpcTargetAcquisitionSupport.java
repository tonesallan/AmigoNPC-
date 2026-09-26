package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.NPCPlugin;

final class NpcTargetAcquisitionSupport {
   private NpcTargetAcquisitionSupport() {
   }

   static Object findWeakestCombatTarget(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d ownerPos,
      boolean pvpEnabled,
      double radius,
      double maxDy,
      double rangedAirMaxDy,
      java.util.function.Predicate<Object> amigoRefPredicate
   ) {
      if (store == null || rec == null || ownerRefObj == null || ownerPos == null || !(ownerRefObj instanceof Ref) || !(rec.refObj instanceof Ref)) {
         return null;
      }

      long now = System.currentTimeMillis();
      if (rec.ownerCombatContextUntilMillis <= 0L || now > rec.ownerCombatContextUntilMillis) {
         return null;
      }

      Ref<EntityStore> ownerRef = (Ref<EntityStore>)ownerRefObj;
      Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
      NPCEntity companion = store.getComponent(npcRef, NPCEntity.getComponentType());
      double r2 = radius * radius;
      Object[] bestRef = new Object[1];
      float[] bestHp = new float[]{Float.POSITIVE_INFINITY};
      double[] bestD2 = new double[]{Double.POSITIVE_INFINITY};

      try {
         store.forEachChunk((chunk, cb) -> {
            int size;
            try {
               size = chunk.size();
            } catch (Throwable ignored) {
               return;
            }

            for (int i = 0; i < size; i++) {
               Ref<EntityStore> ref;
               try {
                  ref = chunk.getReferenceTo(i);
               } catch (Throwable ignored) {
                  continue;
               }

               if (ref == null || refEq(ref, ownerRef) || refEq(ref, npcRef)) {
                  continue;
               }

               try {
                  Player player = chunk.getComponent(i, Player.getComponentType());
                  if (player != null) {
                     continue;
                  }
               } catch (Throwable ignored) {
               }

               boolean otherAmigo = amigoRefPredicate != null && amigoRefPredicate.test(ref);
               boolean explicitOwnerOpponent = rec.combatTargetRefObj != null && refEq(ref, rec.combatTargetRefObj);
               if (otherAmigo) {
                  if (!pvpEnabled) {
                     continue;
                  }
               } else if (!explicitOwnerOpponent) {
                  try {
                     if (companion == null || companion.getRole() == null) {
                        continue;
                     }

                     Attitude attitude = NPCPlugin.get().getAttitudeMap().getAttitude(companion.getRole(), ref, store);
                     if (attitude != Attitude.HOSTILE) {
                        continue;
                     }
                  } catch (Throwable ignored) {
                     continue;
                  }
               }

               try {
                  DeathComponent death = chunk.getComponent(i, DeathComponent.getComponentType());
                  if (death != null) {
                     continue;
                  }
               } catch (Throwable ignored) {
               }

               TransformComponent transform;
               EntityStatMap stats;
               try {
                  transform = chunk.getComponent(i, TransformComponent.getComponentType());
                  stats = chunk.getComponent(i, EntityStatMap.getComponentType());
               } catch (Throwable ignored) {
                  continue;
               }
               if (transform == null || transform.getPosition() == null || stats == null) {
                  continue;
               }

               Vector3d p = transform.getPosition();
               double dy = Math.abs(p.y() - ownerPos.y());
               if (dy > rangedAirMaxDy) {
                  continue;
               }
               if (dy > maxDy) {
                  try {
                     MovementStatesComponent movement = chunk.getComponent(i, MovementStatesComponent.getComponentType());
                     MovementStates states = movement != null ? movement.getMovementStates() : null;
                     if (states == null || states.onGround) {
                        continue;
                     }
                  } catch (Throwable ignored) {
                     continue;
                  }
               }

               double dx = p.x() - ownerPos.x();
               double dz = p.z() - ownerPos.z();
               double d2 = dx * dx + dz * dz;
               if (d2 > r2) {
                  continue;
               }

               float hp;
               try {
                  EntityStatValue value = stats.get(DefaultEntityStatTypes.getHealth());
                  hp = value == null ? Float.POSITIVE_INFINITY : value.get();
                  if (hp <= 0.0F || !Float.isFinite(hp)) {
                     continue;
                  }
               } catch (Throwable ignored) {
                  continue;
               }

               if (hp < bestHp[0] || hp == bestHp[0] && d2 < bestD2[0]) {
                  bestHp[0] = hp;
                  bestD2[0] = d2;
                  bestRef[0] = ref;
               }
            }
         });
      } catch (Throwable ignored) {
      }

      return bestRef[0];
   }

   static Object findNearestDefenderTarget(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d ownerPos,
      boolean pvpEnabled,
      double defenderAutoAcquireRadius,
      double defenderAutoAcquireMaxDy,
      double rangedAirMaxDy
   ) {
      if (store == null || rec == null || ownerRefObj == null || ownerPos == null) {
         return null;
      }

      if (ownerRefObj instanceof Ref && rec.refObj instanceof Ref) {
         Ref<EntityStore> ownerRef = (Ref<EntityStore>)ownerRefObj;
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
         double r = defenderAutoAcquireRadius;
         double r2 = r * r;
         double maxDy = defenderAutoAcquireMaxDy;
         Object[] bestGroundRef = new Object[1];
         double[] bestGroundD2 = new double[]{Double.POSITIVE_INFINITY};
         Object[] bestAirRef = new Object[1];
         double[] bestAirD2 = new double[]{Double.POSITIVE_INFINITY};

         try {
            store.forEachChunk((chunk, cb) -> {
               int sz;
               try {
                  sz = chunk.size();
               } catch (Throwable t) {
                  return;
               }

               for (int i = 0; i < sz; i++) {
                  Ref<EntityStore> ref;
                  try {
                     ref = chunk.getReferenceTo(i);
                  } catch (Throwable t) {
                     continue;
                  }

                  if (ref != null && !refEq(ref, ownerRef) && !refEq(ref, npcRef)) {
                     try {
                        Player maybePlayer = (Player)chunk.getComponent(i, Player.getComponentType());
                        if (maybePlayer != null && !pvpEnabled) {
                           continue;
                        }
                     } catch (Throwable var38) {
                     }

                     try {
                        DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                        if (dc != null) {
                           continue;
                        }
                     } catch (Throwable var37) {
                     }

                     TransformComponent tc;
                     try {
                        tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                     } catch (Throwable t) {
                        continue;
                     }

                     if (tc != null && tc.getPosition() != null) {
                        Vector3d p = tc.getPosition();
                        double dy = Math.abs(p.y() - ownerPos.y());
                        if (dy > maxDy) {
                           if (dy > rangedAirMaxDy) {
                              continue;
                           }

                           try {
                              MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                              MovementStates s = ms != null ? ms.getMovementStates() : null;
                              if (s == null || s.onGround) {
                                 continue;
                              }
                           } catch (Throwable ignored) {
                              continue;
                           }
                        }

                        double dx = p.x() - ownerPos.x();
                        double dz = p.z() - ownerPos.z();
                        double d2 = dx * dx + dz * dz;
                        if (!(d2 > r2)) {
                           try {
                              EntityStatMap stats = (EntityStatMap)chunk.getComponent(i, EntityStatMap.getComponentType());
                              if (stats == null) {
                                 continue;
                              }

                              EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
                              if (hp != null && hp.get() <= 0.0F) {
                                 continue;
                              }
                           } catch (Throwable ignored) {
                              continue;
                           }

                           boolean isAir = false;

                           try {
                              MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                              MovementStates s = ms != null ? ms.getMovementStates() : null;
                              if (s != null) {
                                 isAir = !s.onGround;
                              }
                           } catch (Throwable var32) {
                           }

                           if (!isAir) {
                              if (d2 < bestGroundD2[0]) {
                                 bestGroundD2[0] = d2;
                                 bestGroundRef[0] = ref;
                              }
                           } else if (d2 < bestAirD2[0]) {
                              bestAirD2[0] = d2;
                              bestAirRef[0] = ref;
                           }
                        }
                     }
                  }
               }
            });
         } catch (Throwable var24) {
         }

         return bestGroundRef[0] != null ? bestGroundRef[0] : bestAirRef[0];
      } else {
         return null;
      }
   }

   static Object findNearestTargetNearNpc(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d npcPos,
      Object excludeRefObj,
      boolean pvpEnabled,
      double defenderAutoAcquireRadius,
      double defenderAutoAcquireMaxDy,
      double rangedAirMaxDy
   ) {
      if (store == null || rec == null || ownerRefObj == null || npcPos == null) {
         return null;
      }

      if (ownerRefObj instanceof Ref && rec.refObj instanceof Ref) {
         Ref<EntityStore> ownerRef = (Ref<EntityStore>)ownerRefObj;
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
         double r = defenderAutoAcquireRadius;
         double r2 = r * r;
         double maxDy = defenderAutoAcquireMaxDy;
         Object[] bestGroundRef = new Object[1];
         double[] bestGroundD2 = new double[]{Double.POSITIVE_INFINITY};
         Object[] bestAirRef = new Object[1];
         double[] bestAirD2 = new double[]{Double.POSITIVE_INFINITY};

         try {
            store.forEachChunk((chunk, cb) -> {
               int sz;
               try {
                  sz = chunk.size();
               } catch (Throwable t) {
                  return;
               }

               for (int i = 0; i < sz; i++) {
                  Ref<EntityStore> ref;
                  try {
                     ref = chunk.getReferenceTo(i);
                  } catch (Throwable t) {
                     continue;
                  }

                  if (ref != null && !refEq(ref, ownerRef) && !refEq(ref, npcRef) && (!(excludeRefObj instanceof Ref) || !refEq(ref, (Ref)excludeRefObj))) {
                     try {
                        Player maybePlayer = (Player)chunk.getComponent(i, Player.getComponentType());
                        if (maybePlayer != null && !pvpEnabled) {
                           continue;
                        }
                     } catch (Throwable var39) {
                     }

                     try {
                        DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                        if (dc != null) {
                           continue;
                        }
                     } catch (Throwable var38) {
                     }

                     TransformComponent tc;
                     try {
                        tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                     } catch (Throwable t) {
                        continue;
                     }

                     if (tc != null && tc.getPosition() != null) {
                        Vector3d p = tc.getPosition();
                        double dy = Math.abs(p.y() - npcPos.y());
                        if (dy > maxDy) {
                           if (dy > rangedAirMaxDy) {
                              continue;
                           }

                           try {
                              MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                              MovementStates s = ms != null ? ms.getMovementStates() : null;
                              if (s == null || s.onGround) {
                                 continue;
                              }
                           } catch (Throwable ignored) {
                              continue;
                           }
                        }

                        double dx = p.x() - npcPos.x();
                        double dz = p.z() - npcPos.z();
                        double d2 = dx * dx + dz * dz;
                        if (!(d2 > r2)) {
                           try {
                              EntityStatMap stats = (EntityStatMap)chunk.getComponent(i, EntityStatMap.getComponentType());
                              if (stats == null) {
                                 continue;
                              }

                              EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
                              if (hp != null && hp.get() <= 0.0F) {
                                 continue;
                              }
                           } catch (Throwable ignored) {
                              continue;
                           }

                           boolean isAir = false;

                           try {
                              MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                              MovementStates s = ms != null ? ms.getMovementStates() : null;
                              if (s != null) {
                                 isAir = !s.onGround;
                              }
                           } catch (Throwable var33) {
                           }

                           if (!isAir) {
                              if (d2 < bestGroundD2[0]) {
                                 bestGroundD2[0] = d2;
                                 bestGroundRef[0] = ref;
                              }
                           } else if (d2 < bestAirD2[0]) {
                              bestAirD2[0] = d2;
                              bestAirRef[0] = ref;
                           }
                        }
                     }
                  }
               }
            });
         } catch (Throwable var25) {
         }

         return bestGroundRef[0] != null ? bestGroundRef[0] : bestAirRef[0];
      } else {
         return null;
      }
   }

   static boolean isAnyEnemyNearNpc(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d npcPos,
      double radius,
      double defenderAutoAcquireMaxDy,
      double rangedAirMaxDy
   ) {
      if (store == null || rec == null || ownerRefObj == null || npcPos == null) {
         return false;
      }

      if (ownerRefObj instanceof Ref && rec.refObj instanceof Ref) {
         Ref<EntityStore> ownerRef = (Ref<EntityStore>)ownerRefObj;
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
         double r2 = radius * radius;
         double maxDy = defenderAutoAcquireMaxDy;
         boolean[] found = new boolean[]{false};

         try {
            store.forEachChunk((chunk, cb) -> {
               if (!found[0]) {
                  int sz;
                  try {
                     sz = chunk.size();
                  } catch (Throwable t) {
                     return;
                  }

                  int i = 0;

                  while (i < sz) {
                     label109: {
                        label116: {
                           Ref<EntityStore> ref;
                           try {
                              ref = chunk.getReferenceTo(i);
                           } catch (Throwable t) {
                              break label116;
                           }

                           label105:
                           if (ref != null && !refEq(ref, ownerRef) && !refEq(ref, npcRef)) {
                              try {
                                 Player maybePlayer = (Player)chunk.getComponent(i, Player.getComponentType());
                                 if (maybePlayer != null) {
                                    break label105;
                                 }
                              } catch (Throwable var32) {
                              }

                              try {
                                 DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                                 if (dc != null) {
                                    break label105;
                                 }
                              } catch (Throwable var31) {
                              }

                              TransformComponent tc;
                              try {
                                 tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                              } catch (Throwable t) {
                                 break label105;
                              }

                              label91:
                              if (tc != null && tc.getPosition() != null) {
                                 Vector3d p = tc.getPosition();
                                 double dy = Math.abs(p.y() - npcPos.y());
                                 if (dy > maxDy) {
                                    if (dy > rangedAirMaxDy) {
                                       break label91;
                                    }

                                    try {
                                       MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                                       MovementStates s = ms != null ? ms.getMovementStates() : null;
                                       if (s == null || s.onGround) {
                                          break label91;
                                       }
                                    } catch (Throwable ignored) {
                                       break label91;
                                    }
                                 }

                                 double dx = p.x() - npcPos.x();
                                 double dz = p.z() - npcPos.z();
                                 double d2 = dx * dx + dz * dz;
                                 if (!(d2 > r2)) {
                                    try {
                                       EntityStatMap stats = (EntityStatMap)chunk.getComponent(i, EntityStatMap.getComponentType());
                                       if (stats != null) {
                                          EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
                                          if (hp == null || !(hp.get() <= 0.0F)) {
                                             break label109;
                                          }
                                       }
                                    } catch (Throwable ignored) {
                                    }
                                 }
                              }
                           }
                        }

                        i++;
                        continue;
                     }

                     found[0] = true;
                     return;
                  }

                  return;
               }
            });
         } catch (Throwable var18) {
         }

         return found[0];
      } else {
         return false;
      }
   }

   private static boolean refEq(Object a, Object b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         try {
            return a.equals(b);
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }
}
