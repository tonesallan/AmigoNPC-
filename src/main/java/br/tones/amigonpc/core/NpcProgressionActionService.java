package br.tones.amigonpc.core;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.progress.NpcProgressionSupport;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;

final class NpcProgressionActionService {
   private NpcProgressionActionService() {
   }

   static boolean addNpcXpViaApi(
      Map<UUID, AmigoNpcManager.NpcRecord> records,
      UUID ownerId,
      long amount,
      NpcXpSource source,
      NpcXpContext ctx,
      NpcProgressionActionService.StoreResolver storeResolver,
      NpcProgressionActionService.OnlineXpApplier onlineXpApplier
   ) {
      if (ownerId != null && amount > 0L) {
         AmigoNpcManager.NpcRecord rec = records.get(ownerId);
         if (rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.refObj instanceof Ref && rec.worldObj != null) {
            Object worldObj = rec.worldObj;
            HytaleBridge.worldExecute(worldObj, () -> {
               try {
                  Object storeObj = storeResolver.resolve(worldObj);
                  if (storeObj == null) {
                     return;
                  }

                  Store<EntityStore> store = (Store<EntityStore>)storeObj;
                  Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                  onlineXpApplier.apply(store, npcRef, ownerId, rec, amount, source, ctx);
               } catch (Throwable var12) {
               }
            });
            return true;
         } else {
            return NpcProgressionSupport.awardOfflineXp(ownerId, amount, source, ctx);
         }
      } else {
         return false;
      }
   }

   static void requestRescale(
      Map<UUID, AmigoNpcManager.NpcRecord> records,
      UUID ownerId,
      NpcProgressionActionService.StoreResolver storeResolver,
      NpcProgressionActionService.OnlineRescaleApplier rescaleApplier
   ) {
      if (ownerId != null) {
         AmigoNpcManager.NpcRecord rec = records.get(ownerId);
         if (rec != null) {
            if (rec.state == AmigoNpcManager.State.ACTIVE) {
               if (rec.refObj instanceof Ref) {
                  Object worldObj = rec.worldObj;
                  if (worldObj != null) {
                     HytaleBridge.worldExecute(worldObj, () -> {
                        try {
                           Object storeObj = storeResolver.resolve(worldObj);
                           if (storeObj == null) {
                              return;
                           }

                           Store<EntityStore> store = (Store<EntityStore>)storeObj;
                           Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                           rescaleApplier.apply(store, npcRef, ownerId, rec);
                        } catch (Throwable var8) {
                        }
                     });
                  }
               }
            }
         }
      }
   }

   @FunctionalInterface
   interface OnlineRescaleApplier {
      void apply(Store<EntityStore> var1, Ref<EntityStore> var2, UUID var3, AmigoNpcManager.NpcRecord var4);
   }

   @FunctionalInterface
   interface OnlineXpApplier {
      void apply(Store<EntityStore> var1, Ref<EntityStore> var2, UUID var3, AmigoNpcManager.NpcRecord var4, long var5, NpcXpSource var7, NpcXpContext var8);
   }

   @FunctionalInterface
   interface StoreResolver {
      Object resolve(Object var1);
   }
}
