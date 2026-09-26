package br.tones.amigonpc.core;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map.Entry;

final class NpcLootStateSupport {
   private NpcLootStateSupport() {
   }

   static void recordCombatTag(AmigoNpcManager.NpcRecord rec, Object targetRefObj, Store<EntityStore> store, int combatTagMax) {
      if (targetRefObj != null && store != null && rec != null) {
         if (rec.refObj != null) {
            if (!rec.downed) {
               if (targetRefObj instanceof Ref) {
                  long now = System.currentTimeMillis();
                  rec.lastCombatTagMillis = now;
                  rec.wasInCombat = true;

                  try {
                     Ref<EntityStore> targetRef = (Ref<EntityStore>)targetRefObj;
                     TransformComponent tc = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
                     if (tc == null || tc.getPosition() == null) {
                        return;
                     }

                     Vector3d pos = tc.getPosition();
                     rec.lastBattleCenter = pos;
                     synchronized (rec.combatTags) {
                        CombatTag existing = null;

                        for (CombatTag t : rec.combatTags) {
                           if (t != null && refEq(t.targetRefObj, targetRefObj)) {
                              existing = t;
                              break;
                           }
                        }

                        if (existing != null) {
                           existing.pos = pos;
                           existing.lastSeenMillis = now;
                        } else {
                           if (rec.combatTags.size() >= combatTagMax) {
                              rec.combatTags.remove(0);
                           }

                           rec.combatTags.add(new CombatTag(targetRefObj, pos, now));
                        }
                     }
                  } catch (Throwable var15) {
                  }
               }
            }
         }
      }
   }

   static void clearCombatTagsAndLoot(AmigoNpcManager.NpcRecord rec) {
      if (rec != null) {
         synchronized (rec.combatTags) {
            rec.combatTags.clear();
         }

         rec.pendingLootRefObjs.clear();
         rec.lootTargetRefObj = null;
         rec.lootTargetSinceMillis = 0L;
         rec.lootingActive = false;
         rec.lastCombatEndMillis = 0L;
         rec.lootStickUntilMillis = 0L;
         rec.lastBattleCenter = null;
      }
   }

   static void lootChatAccAdd(AmigoNpcManager.NpcRecord rec, String itemId, int qty, long now, long summaryDelayMillis) {
      if (rec != null && itemId != null && !itemId.isBlank() && qty > 0) {
         try {
            Integer cur = rec.lootChatAcc.get(itemId);
            rec.lootChatAcc.put(itemId, (cur == null ? 0 : cur) + qty);
            rec.lootChatSendAtMillis = now + summaryDelayMillis;
         } catch (Throwable var8) {
         }
      }
   }

   static void lootChatAccFlushIfDue(AmigoNpcManager.NpcRecord rec, UUID ownerId, Object worldObj, long now, NpcLootStateSupport.OwnerMessenger messenger) {
      if (rec != null && ownerId != null && worldObj != null) {
         long at = rec.lootChatSendAtMillis;
         if (at > 0L && now >= at) {
            if (rec.lootChatAcc.isEmpty()) {
               rec.lootChatSendAtMillis = 0L;
            } else {
               try {
                  StringBuilder items = new StringBuilder();
                  boolean first = true;

                  for (Entry<String, Integer> en : rec.lootChatAcc.entrySet()) {
                     if (en != null) {
                        String id = en.getKey();
                        Integer q = en.getValue();
                        if (id != null && !id.isBlank() && q != null && q > 0) {
                           if (!first) {
                              items.append(", ");
                           }

                           first = false;
                           items.append(q).append(" ").append(id);
                           if (items.length() > 180) {
                              break;
                           }
                        }
                     }
                  }

                  messenger.send(worldObj, ownerId, AmigoText.format("core.loot.summary", items.toString()));
               } catch (Throwable var17) {
               } finally {
                  rec.lootChatAcc.clear();
                  rec.lootChatSendAtMillis = 0L;
               }
            }
         }
      }
   }

   static boolean isItemRefValid(Store<EntityStore> store, Object refObj) {
      if (store == null || refObj == null) {
         return false;
      } else if (!(refObj instanceof Ref<?> rawRef)) {
         return false;
      } else {
         @SuppressWarnings("unchecked")
         Ref<EntityStore> r = (Ref<EntityStore>)rawRef;

         try {
            TransformComponent tc = (TransformComponent)store.getComponent(r, TransformComponent.getComponentType());
            Object ic = store.getComponent(r, ItemComponent.getComponentType());
            return tc != null && tc.getPosition() != null && ic != null;
         } catch (Throwable var5) {
            return false;
         }
      }
   }

   static boolean containsRef(ArrayList<Object> list, Ref<EntityStore> ref) {
      if (list != null && ref != null) {
         for (Object o : list) {
            if (o instanceof Ref && refEq((Ref)o, ref)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   static Object chooseNearestPendingLoot(AmigoNpcManager.NpcRecord rec, Store<EntityStore> store, Vector3d npcPos) {
      if (rec != null && store != null && npcPos != null) {
         Object best = null;
         double bestD2 = Double.POSITIVE_INFINITY;

         for (int i = rec.pendingLootRefObjs.size() - 1; i >= 0; i--) {
            Object r = rec.pendingLootRefObjs.get(i);
            if (!isItemRefValid(store, r)) {
               rec.pendingLootRefObjs.remove(i);
            } else if (r instanceof Ref) {
               try {
                  TransformComponent tc = (TransformComponent)store.getComponent((Ref)r, TransformComponent.getComponentType());
                  if (tc != null && tc.getPosition() != null) {
                     double d2 = dist2(tc.getPosition(), npcPos);
                     if (d2 < bestD2) {
                        bestD2 = d2;
                        best = r;
                     }
                  }
               } catch (Throwable var11) {
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   static void removeRefFromList(ArrayList<Object> list, Ref<EntityStore> ref) {
      if (list != null && ref != null) {
         for (int i = list.size() - 1; i >= 0; i--) {
            Object o = list.get(i);
            if (o instanceof Ref && refEq((Ref)o, ref)) {
               list.remove(i);
            }
         }
      }
   }

   static void refreshPendingLootFromCombatTags(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Vector3d npcPos,
      Vector3d ownerPos,
      long now,
      double lootTagScanRadius,
      int lootPendingMax,
      Archetype<EntityStore> autoLootQuery
   ) {
      if (store != null && rec != null) {
         try {
            rec.lootProcessedUntil.entrySet().removeIf(e -> e == null || e.getValue() == null || e.getValue() <= now);
         } catch (Throwable var16) {
         }

         ArrayList<CombatTag> tags = new ArrayList<>();
         synchronized (rec.combatTags) {
            tags.addAll(rec.combatTags);
         }

         if (!tags.isEmpty()) {
            double tagR2 = lootTagScanRadius * lootTagScanRadius;

            try {
               store.forEachChunk(autoLootQuery, (chunkObj, cb) -> {
                  if (!(chunkObj instanceof ArchetypeChunk<?> rawChunk)) {
                     return true;
                  } else {
                     ArchetypeChunk<EntityStore> chunk;
                     try {
                        chunk = (ArchetypeChunk<EntityStore>)rawChunk;
                     } catch (Throwable t) {
                        return true;
                     }

                     int sz;
                     try {
                        sz = chunk.size();
                     } catch (Throwable t) {
                        return true;
                     }

                     for (int i = 0; i < sz; i++) {
                        if (rec.pendingLootRefObjs.size() >= lootPendingMax) {
                           return true;
                        }

                        Ref<EntityStore> ref;
                        try {
                           ref = chunk.getReferenceTo(i);
                        } catch (Throwable t) {
                           continue;
                        }

                        if (ref != null && !containsRef(rec.pendingLootRefObjs, ref)) {
                           Long until = rec.lootProcessedUntil.get(ref);
                           if (until == null || until <= now) {
                              TransformComponent tc;
                              try {
                                 tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                              } catch (Throwable t) {
                                 continue;
                              }

                              if (tc != null && tc.getPosition() != null) {
                                 Vector3d p = tc.getPosition();
                                 boolean nearAnyTag = false;

                                 for (CombatTag t : tags) {
                                    if (t != null && t.pos != null && dist2(p, t.pos) <= tagR2) {
                                       nearAnyTag = true;
                                       break;
                                    }
                                 }

                                 if (nearAnyTag) {
                                    rec.pendingLootRefObjs.add(ref);
                                 }
                              }
                           }
                        }
                     }

                     return true;
                  }
               });
            } catch (Throwable var14) {
            }
         }
      }
   }

   private static double dist2(Vector3d a, Vector3d b) {
      if (a != null && b != null) {
         double dx = a.x() - b.x();
         double dy = a.y() - b.y();
         double dz = a.z() - b.z();
         return dx * dx + dy * dy + dz * dz;
      } else {
         return Double.POSITIVE_INFINITY;
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

   @FunctionalInterface
   interface OwnerMessenger {
      void send(Object var1, UUID var2, String var3);
   }
}
