package br.tones.amigonpc.core;

import br.tones.amigonpc.core.autoloot.AutoLootConfig;
import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class NpcCombatLootTargetSupport {
   private NpcCombatLootTargetSupport() {
   }

   static boolean ensureLootTarget(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Vector3d npcPos,
      Vector3d ownerPos,
      long now,
      NpcCombatLootTargetSupport.PendingLootRefresher refresher,
      NpcCombatLootTargetSupport.PendingLootChooser chooser
   ) {
      if (store == null || rec == null || npcPos == null) {
         return false;
      }

      if (rec.lootTargetRefObj != null) {
         return true;
      }

      if (rec.pendingLootRefObjs.isEmpty()) {
         refresher.refresh(store, rec, npcPos, ownerPos, now);
      }

      Object chosen = chooser.choose(rec, store, npcPos);
      if (chosen != null) {
         rec.lootTargetRefObj = chosen;
         rec.lootTargetSinceMillis = now;
         return true;
      }

      if (rec.lootStickUntilMillis > 0L && now < rec.lootStickUntilMillis) {
         return false;
      }

      rec.lootingActive = false;
      return false;
   }

   static boolean validateLootTarget(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      long now,
      long lootTargetTimeoutMillis,
      long lootSkipRetryMillis,
      NpcCombatLootTargetSupport.ItemRefValidator validator
   ) {
      if (store == null || rec == null) {
         return false;
      } else if (!validator.isValid(store, rec.lootTargetRefObj)) {
         rec.lootTargetRefObj = null;
         rec.lootTargetSinceMillis = 0L;
         return false;
      } else if (rec.lootTargetSinceMillis > 0L && now - rec.lootTargetSinceMillis > lootTargetTimeoutMillis) {
         rec.lootProcessedUntil.put(rec.lootTargetRefObj, now + lootSkipRetryMillis);
         rec.lootTargetRefObj = null;
         rec.lootTargetSinceMillis = 0L;
         return false;
      } else {
         return true;
      }
   }

   static void steerNpcToLootTarget(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object npcEntityComponentType,
      NpcCombatLootTargetSupport.StoreComponentGetter componentGetter,
      NpcCombatLootTargetSupport.LockedTargetSetter lockedTargetSetter,
      NpcCombatLootTargetSupport.MarkedTargetSetter markedTargetSetter,
      NpcCombatLootTargetSupport.FlockStateSetter flockStateSetter
   ) {
      if (store != null && rec != null) {
         try {
            Object npcEntityObj = componentGetter.get(store, rec.refObj, npcEntityComponentType);
            if (npcEntityObj != null) {
               lockedTargetSetter.set(npcEntityObj, rec.lootTargetRefObj);
               markedTargetSetter.set(npcEntityObj, "CombatTarget", null);
               flockStateSetter.set(store, rec.refObj, "Run", "");
            }
         } catch (Throwable var8) {
         }
      }
   }

   static void tryPickupLootTarget(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      SimpleItemContainer bag,
      Vector3d npcPos,
      long now,
      double lootPickupDistance,
      long lootSkipRetryMillis,
      NpcCombatLootTargetSupport.LootPickupHandler pickupHandler
   ) {
      if (store != null && ownerId != null && rec != null && bag != null && npcPos != null) {
         if (rec.lootTargetRefObj instanceof Ref<EntityStore> itemRef) {
            try {
               TransformComponent itc = (TransformComponent)store.getComponent(itemRef, TransformComponent.getComponentType());
               if (itc == null || itc.getPosition() == null) {
                  return;
               }

               Vector3d ip = itc.getPosition();
               double dx = ip.x() - npcPos.x();
               double dz = ip.z() - npcPos.z();
               double h2 = dx * dx + dz * dz;
               double dy = Math.abs(ip.y() - npcPos.y());
               double lootMaxDy = 10.0;

               try {
                  AutoLootConfig cfg = AutoLootConfigService.get();
                  if (cfg != null && cfg.VerticalScanBlocks > 0) {
                     lootMaxDy = cfg.VerticalScanBlocks;
                  }
               } catch (Throwable var26) {
               }

               if (h2 <= lootPickupDistance * lootPickupDistance && dy <= lootMaxDy) {
                  boolean inserted = pickupHandler.pickup(store, ownerId, rec, bag, itemRef);
                  if (!inserted) {
                     rec.lootProcessedUntil.put(itemRef, now + lootSkipRetryMillis);
                  }

                  NpcLootStateSupport.removeRefFromList(rec.pendingLootRefObjs, itemRef);
                  rec.lootTargetRefObj = null;
                  rec.lootTargetSinceMillis = 0L;
               }
            } catch (Throwable var27) {
            }
         }
      }
   }

   @FunctionalInterface
   interface FlockStateSetter {
      void set(Store<EntityStore> var1, Object var2, String var3, String var4);
   }

   @FunctionalInterface
   interface ItemRefValidator {
      boolean isValid(Store<EntityStore> var1, Object var2);
   }

   @FunctionalInterface
   interface LockedTargetSetter {
      void set(Object var1, Object var2);
   }

   @FunctionalInterface
   interface LootPickupHandler {
      boolean pickup(Store<EntityStore> var1, UUID var2, AmigoNpcManager.NpcRecord var3, SimpleItemContainer var4, Object var5);
   }

   @FunctionalInterface
   interface MarkedTargetSetter {
      void set(Object var1, String var2, Object var3);
   }

   @FunctionalInterface
   interface PendingLootChooser {
      Object choose(AmigoNpcManager.NpcRecord var1, Store<EntityStore> var2, Vector3d var3);
   }

   @FunctionalInterface
   interface PendingLootRefresher {
      void refresh(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Vector3d var3, Vector3d var4, long var5);
   }

   @FunctionalInterface
   interface StoreComponentGetter {
      Object get(Object var1, Object var2, Object var3);
   }
}
