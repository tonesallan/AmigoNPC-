package br.tones.amigonpc.core;

import br.tones.amigonpc.core.autoloot.AutoLootConfig;
import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.xp.sources.NpcCollectXpSource;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

final class NpcAutoLootSupport {
   private NpcAutoLootSupport() {
   }

   static NpcAutoLootSupport.ScanSetup prepareScan(
      AmigoNpcManager.NpcRecord rec, Vector3d npcPos, long now, long autoLootIntervalMs, double autoLootRadius, int autoLootMaxItemsPerScan
   ) {
      if (rec != null && npcPos != null) {
         boolean forceDelayedScan = false;
         Vector3d forcedLootCenter = null;

         try {
            if (rec.autoLootStickUntilMillis > 0L) {
               if (now < rec.autoLootStickUntilMillis) {
                  rec.nextAutoLootMillis = Math.max(rec.nextAutoLootMillis, rec.autoLootStickUntilMillis);
                  return null;
               }

               forceDelayedScan = true;
               forcedLootCenter = rec.autoLootStickAnchorPos != null ? rec.autoLootStickAnchorPos : npcPos;
               rec.autoLootStickUntilMillis = 0L;
               rec.autoLootStickDeadRefObj = null;
               rec.autoLootStickAnchorPos = null;
               rec.nextAutoLootMillis = now;
            }
         } catch (Throwable var15) {
         }

         if (!forceDelayedScan && now < rec.nextAutoLootMillis) {
            return null;
         }

         rec.nextAutoLootMillis = now + autoLootIntervalMs;
         double maxDy = autoLootRadius;

         try {
            AutoLootConfig cfg = AutoLootConfigService.get();
            if (cfg != null && cfg.VerticalScanBlocks > 0) {
               maxDy = cfg.VerticalScanBlocks;
            }
         } catch (Throwable var14) {
         }

         Vector3d lootCenterPos = forceDelayedScan && forcedLootCenter != null ? forcedLootCenter : npcPos;
         return new NpcAutoLootSupport.ScanSetup(lootCenterPos, autoLootRadius * autoLootRadius, autoLootMaxItemsPerScan, maxDy);
      } else {
         return null;
      }
   }

   static SimpleItemContainer prepareBackpack(
      AmigoNpcManager.NpcRecord rec,
      UUID ownerId,
      Object worldObj,
      long now,
      NpcAutoLootSupport.BackpackLoader loader,
      NpcAutoLootSupport.CombatDebugger debugger,
      NpcAutoLootSupport.FullBackpackNotifier notifier,
      NpcAutoLootSupport.BackpackFullChecker checker
   ) {
      if (rec != null && ownerId != null) {
         if (rec.backpack == null) {
            try {
               rec.backpack = loader.load(ownerId);
            } catch (Throwable t) {
               debugger.log(rec, ownerId, AmigoText.format("core.debug.autoloot.load_backpack_failed", t.getClass().getSimpleName(), t.getMessage()));
            }
         }

         SimpleItemContainer bag = rec.backpack;
         if (bag == null) {
            debugger.log(rec, ownerId, AmigoText.text("core.debug.autoloot.backpack_null"));
            return null;
         }

         if (rec.lootPausedInventoryFull) {
            if (now < rec.nextLootFullRecheckMillis) {
               return null;
            }

            rec.nextLootFullRecheckMillis = now + 1000L;
            if (checker.isFull(bag)) {
               notifier.notify(rec, ownerId, worldObj, now);
               return null;
            }

            rec.lootPausedInventoryFull = false;
         }

         if (checker.isFull(bag)) {
            rec.lootPausedInventoryFull = true;
            rec.nextLootFullRecheckMillis = now + 1000L;
            notifier.notify(rec, ownerId, worldObj, now);
            return null;
         } else {
            return bag;
         }
      } else {
         return null;
      }
   }

   static NpcAutoLootSupport.ScanResult scanNearbyDrops(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      SimpleItemContainer bag,
      Object worldObj,
      long now,
      Vector3d lootCenterPos,
      Vector3d ownerPos,
      double radiusSq,
      double ownerRadiusSq,
      double maxDy,
      int maxPerScan,
      long backpackSaveDebounceMs,
      Archetype<EntityStore> autoLootQuery,
      NpcAutoLootSupport.CombatDebugger debugger,
      NpcAutoLootSupport.FullBackpackNotifier notifier,
      NpcAutoLootSupport.BackpackFullChecker checker,
      NpcAutoLootSupport.LootAccumulator accumulator
   ) {
      ArrayList<Ref<EntityStore>> removeLater = new ArrayList<>();
      ArrayList<Ref<EntityStore>> updateLaterRef = new ArrayList<>();
      ArrayList<ItemComponent> updateLaterComp = new ArrayList<>();
      int[] picked = new int[]{0};

      try {
         store.forEachChunk(
            autoLootQuery,
            (chunkObj, cb) -> {
               ArchetypeChunk<EntityStore> chunk = null;
               if (chunkObj instanceof ArchetypeChunk<?> rawChunk) {
                  try {
                     chunk = (ArchetypeChunk<EntityStore>)rawChunk;
                  } catch (Throwable ignored) {
                     chunk = null;
                  }
               }

               int size;
               try {
                  size = chunk != null ? chunk.size() : chunkGetSize(chunkObj);
               } catch (Throwable t) {
                  return true;
               }

               for (int i = 0; i < size && picked[0] < maxPerScan; i++) {
                  Ref<EntityStore> itemRef;
                  try {
                     itemRef = chunk != null ? chunk.getReferenceTo(i) : chunkGetEntity(chunkObj, i);
                  } catch (Throwable t) {
                     continue;
                  }

                  if (itemRef != null && itemRef.isValid()) {
                     TransformComponent tc;
                     ItemComponent ic;
                     try {
                        tc = chunk != null
                           ? (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType())
                           : (TransformComponent)chunkGetComponent(chunkObj, i, TransformComponent.getComponentType());
                        ic = chunk != null
                           ? (ItemComponent)chunk.getComponent(i, ItemComponent.getComponentType())
                           : (ItemComponent)chunkGetComponent(chunkObj, i, ItemComponent.getComponentType());
                     } catch (Throwable t) {
                        continue;
                     }

                     if (tc != null && ic != null) {
                        try {
                           ic.setPickupDelay(0.0F);
                        } catch (Throwable var53) {
                        }

                        Vector3d pos = tc.getPosition();
                        if (pos != null) {
                           double ldx = pos.x() - lootCenterPos.x();
                           double ldz = pos.z() - lootCenterPos.z();
                           double ldy = Math.abs(pos.y() - lootCenterPos.y());
                           if (!(ldx * ldx + ldz * ldz > radiusSq) && !(ldy > maxDy)) {
                              if (ownerPos != null) {
                                 double odx = pos.x() - ownerPos.x();
                                 double odz = pos.z() - ownerPos.z();
                                 if (odx * odx + odz * odz > ownerRadiusSq) {
                                    continue;
                                 }
                              }

                              ItemStack before = ic.getItemStack();
                              if (before != null) {
                                 int beforeQty;
                                 try {
                                    beforeQty = before.getQuantity();
                                 } catch (Throwable t) {
                                    continue;
                                 }

                                 if (beforeQty > 0) {
                                    String itemIdStr = null;

                                    try {
                                       Object idObj = before.getItemId();
                                       if (idObj != null) {
                                          itemIdStr = idObj.toString();
                                       }
                                    } catch (Throwable var52) {
                                    }

                                    if (itemIdStr == null || itemIdStr.isBlank()) {
                                       itemIdStr = "item";
                                    }

                                    try {
                                       if (!AutoLootConfigService.isItemAllowed(itemIdStr)) {
                                          continue;
                                       }
                                    } catch (Throwable var56) {
                                    }

                                    ItemStack remainder = null;

                                    try {
                                       remainder = ItemComponent.addToItemContainer(store, itemRef, bag);
                                    } catch (Throwable ignored) {
                                       remainder = null;
                                    }

                                    if (remainder == null) {
                                       ItemStackTransaction tx;
                                       try {
                                          tx = bag.addItemStack(before);
                                       } catch (Throwable t) {
                                          tx = null;
                                       }

                                       try {
                                          remainder = tx != null ? tx.getRemainder() : null;
                                       } catch (Throwable var49) {
                                       }
                                    }

                                    int remQty = 0;
                                    if (remainder != null) {
                                       try {
                                          remQty = remainder.getQuantity();
                                       } catch (Throwable ignored) {
                                          remQty = beforeQty;
                                       }
                                    }

                                    if (remainder != null && remQty >= beforeQty) {
                                       if (checker.isFull(bag)) {
                                          rec.lootPausedInventoryFull = true;
                                          rec.nextLootFullRecheckMillis = now + 1000L;
                                          notifier.notify(rec, ownerId, worldObj, now);
                                          return false;
                                       }
                                    } else {
                                       int inserted = beforeQty - (remainder != null ? remQty : 0);
                                       if (inserted > 0) {
                                          accumulator.add(rec, itemIdStr, inserted, now);
                                       }

                                       picked[0]++;
                                       rec.backpackDirty = true;
                                       rec.nextBackpackSaveMillis = Math.min(rec.nextBackpackSaveMillis, now + backpackSaveDebounceMs);
                                       if (remainder != null && remQty > 0) {
                                          try {
                                             ic.setItemStack(remainder);
                                             updateLaterRef.add(itemRef);
                                             updateLaterComp.add(ic);
                                          } catch (Throwable var47) {
                                          }
                                       } else {
                                          removeLater.add(itemRef);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               return true;
            }
         );
      } catch (Throwable t) {
         debugger.log(rec, ownerId, AmigoText.format("core.debug.autoloot.foreach_chunk_failed", t.getClass().getSimpleName(), t.getMessage()));
      }

      return new NpcAutoLootSupport.ScanResult(picked[0], removeLater, updateLaterRef, updateLaterComp);
   }

   static void finalizeScan(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      SimpleItemContainer bag,
      long now,
      Object worldObj,
      ArrayList<Ref<EntityStore>> removeLater,
      ArrayList<Ref<EntityStore>> updateLaterRef,
      ArrayList<ItemComponent> updateLaterComp,
      int pickedCount,
      NpcAutoLootSupport.CombatDebugger debugger,
      NpcAutoLootSupport.LootSummaryFlusher summaryFlusher,
      NpcAutoLootSupport.FallbackEntityRemover fallbackEntityRemover
   ) {
      if (store != null && ownerId != null && rec != null && bag != null) {
         for (int i = 0; i < updateLaterRef.size(); i++) {
            Ref<EntityStore> ref = updateLaterRef.get(i);
            ItemComponent comp = updateLaterComp.get(i);
            if (ref != null && ref.isValid() && comp != null) {
               try {
                  store.putComponent(ref, ItemComponent.getComponentType(), comp);
               } catch (Throwable var22) {
               }
            }
         }

         for (Ref<EntityStore> r : removeLater) {
            if (r != null && r.isValid()) {
               try {
                  store.removeEntity(r, RemoveReason.REMOVE);
               } catch (Throwable ignored) {
                  fallbackEntityRemover.remove(store, r);
               }
            }
         }

         if (rec.backpackDirty && now >= rec.nextBackpackSaveMillis) {
            try {
               AmigoPersistence.saveBackpack(ownerId, bag);
               rec.backpackDirty = false;
            } catch (Throwable t) {
               debugger.log(rec, ownerId, AmigoText.format("core.debug.autoloot.save_backpack_failed", t.getMessage()));
            }
         }

         summaryFlusher.flush(rec, ownerId, worldObj, now);
         if (pickedCount > 0) {
            String wn = null;

            try {
               if (worldObj.getClass().getMethod("getName").invoke(worldObj) instanceof String s && !s.isBlank()) {
                  wn = s;
               }
            } catch (Throwable var19) {
            }

            try {
               NpcCollectXpSource.onPickup(ownerId, pickedCount, wn);
            } catch (Throwable var18) {
            }
         }
      }
   }

   private static int chunkGetSize(Object chunk) {
      Object v = invokeNoArg(chunk, "size", "getSize");
      if (v instanceof Integer i) {
         return i;
      } else if (v instanceof Short s) {
         return s;
      } else {
         return v instanceof Long l ? (int)l.longValue() : 0;
      }
   }

   private static Ref<EntityStore> chunkGetEntity(Object chunk, int index) {
      if (chunk == null) {
         return null;
      }

      try {
         Method m = chunk.getClass().getMethod("getReferenceTo", int.class);
         return (Ref<EntityStore>)m.invoke(chunk, index);
      } catch (Throwable var10) {
         try {
            for (Method m : chunk.getClass().getMethods()) {
               String name = m.getName();
               if ((name.equals("getReferenceTo") || name.equals("getEntity") || name.equals("getReference")) && m.getParameterCount() == 1) {
                  Class<?> p0 = m.getParameterTypes()[0];
                  if (p0 == int.class || p0 == Integer.class || p0 == short.class || p0 == Short.class) {
                     Object idx = p0 != short.class && p0 != Short.class ? index : (short)index;
                     return (Ref<EntityStore>)m.invoke(chunk, idx);
                  }
               }
            }
         } catch (Throwable var9) {
         }

         return null;
      }
   }

   private static Object chunkGetComponent(Object chunk, int index, Object componentType) {
      if (chunk != null && componentType != null) {
         try {
            for (Method m : chunk.getClass().getMethods()) {
               if (m.getName().equals("getComponent") && m.getParameterCount() == 2) {
                  Class<?>[] p = m.getParameterTypes();
                  boolean firstIsIndex = p[0] == int.class || p[0] == Integer.class || p[0] == short.class || p[0] == Short.class;
                  if (firstIsIndex) {
                     Object idx = p[0] != short.class && p[0] != Short.class ? index : (short)index;

                     try {
                        return m.invoke(chunk, idx, componentType);
                     } catch (Throwable var11) {
                     }
                  }
               }
            }
         } catch (Throwable var12) {
         }

         return null;
      } else {
         return null;
      }
   }

   private static Object invokeNoArg(Object target, String... methodNames) {
      if (target != null && methodNames != null) {
         for (String methodName : methodNames) {
            if (methodName != null && !methodName.isBlank()) {
               try {
                  return target.getClass().getMethod(methodName).invoke(target);
               } catch (Throwable var7) {
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @FunctionalInterface
   interface BackpackFullChecker {
      boolean isFull(SimpleItemContainer var1);
   }

   @FunctionalInterface
   interface BackpackLoader {
      SimpleItemContainer load(UUID var1);
   }

   @FunctionalInterface
   interface CombatDebugger {
      void log(AmigoNpcManager.NpcRecord var1, UUID var2, String var3);
   }

   @FunctionalInterface
   interface FallbackEntityRemover {
      void remove(Object var1, Object var2);
   }

   @FunctionalInterface
   interface FullBackpackNotifier {
      void notify(AmigoNpcManager.NpcRecord var1, UUID var2, Object var3, long var4);
   }

   @FunctionalInterface
   interface LootAccumulator {
      void add(AmigoNpcManager.NpcRecord var1, String var2, int var3, long var4);
   }

   @FunctionalInterface
   interface LootSummaryFlusher {
      void flush(AmigoNpcManager.NpcRecord var1, UUID var2, Object var3, long var4);
   }

   static final class ScanResult {
      final int pickedCount;
      final ArrayList<Ref<EntityStore>> removeLater;
      final ArrayList<Ref<EntityStore>> updateLaterRef;
      final ArrayList<ItemComponent> updateLaterComp;

      ScanResult(int pickedCount, ArrayList<Ref<EntityStore>> removeLater, ArrayList<Ref<EntityStore>> updateLaterRef, ArrayList<ItemComponent> updateLaterComp) {
         this.pickedCount = pickedCount;
         this.removeLater = removeLater;
         this.updateLaterRef = updateLaterRef;
         this.updateLaterComp = updateLaterComp;
      }
   }

   static final class ScanSetup {
      final Vector3d lootCenterPos;
      final double radiusSq;
      final int maxPerScan;
      final double maxDy;

      ScanSetup(Vector3d lootCenterPos, double radiusSq, int maxPerScan, double maxDy) {
         this.lootCenterPos = lootCenterPos;
         this.radiusSq = radiusSq;
         this.maxPerScan = maxPerScan;
         this.maxDy = maxDy;
      }
   }
}
