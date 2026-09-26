package br.tones.amigonpc.core;

import br.tones.amigonpc.core.progress.XpProgression;
import br.tones.amigonpc.core.swords.SwordMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;

final class NpcSwordService {
   private NpcSwordService() {
   }

   static int getSwordLevel(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      if (ownerId == null) {
         return 1;
      }

      AmigoNpcManager.NpcRecord rec = records.get(ownerId);
      if (rec != null) {
         return br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
      }

      long xp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
      return br.tones.amigonpc.core.swords.SwordProgression.clampLevel(XpProgression.levelFromTotalXp(xp));
   }

   static int changeSwordLevel(
      Map<UUID, AmigoNpcManager.NpcRecord> records,
      UUID ownerId,
      int delta,
      boolean announce,
      NpcSwordService.StoreResolver storeResolver,
      NpcSwordService.SwordWeaponApplier weaponApplier,
      NpcSwordService.NpcScalingApplier scalingApplier,
      NpcSwordService.OwnerMessenger messenger
   ) {
      if (ownerId != null && delta != 0) {
         AmigoNpcManager.NpcRecord rec = records.get(ownerId);
         int oldLvl = rec != null
            ? br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level)
            : br.tones.amigonpc.core.swords.SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
         int newLvl = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(oldLvl + delta);
         if (newLvl == oldLvl) {
            return newLvl;
         }

         if (rec != null) {
            rec.level = newLvl;
         }

         String equippedWeaponId = rec != null ? rec.equippedWeaponId : AmigoPersistence.loadEquippedWeaponId(ownerId);
         AmigoPersistence.saveSwordState(ownerId, newLvl, equippedWeaponId);
         XpProgression.init();
         long newTotalXp = XpProgression.xpStartOfLevel(newLvl);
         if (newTotalXp < 0L) {
            newTotalXp = 0L;
         }

         if (rec != null) {
            rec.totalXp = newTotalXp;
            rec.npcLevelCached = newLvl;
         }

         AmigoPersistence.saveTotalXp(ownerId, newTotalXp);
         if (rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.worldObj != null && rec.refObj != null) {
            Object worldObj = rec.worldObj;
            HytaleBridge.worldExecute(worldObj, () -> {
               try {
                  Object storeObj = storeResolver.resolve(worldObj);
                  if (storeObj == null) {
                     return;
                  }

                  Store<EntityStore> store = (Store<EntityStore>)storeObj;
                  Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                  weaponApplier.apply(store, npcRef, ownerId, rec, true);

                  try {
                     scalingApplier.apply(store, npcRef, ownerId, rec);
                  } catch (Throwable var10x) {
                  }
               } catch (Throwable var11x) {
               }
            });
            if (announce) {
               messenger.send(worldObj, ownerId, selectAnnounceLine(delta, oldLvl, newLvl));
            }
         }

         return newLvl;
      } else {
         return getSwordLevel(records, ownerId);
      }
   }

   private static String selectAnnounceLine(int delta, int oldLvl, int newLvl) {
      if (delta > 0 && br.tones.amigonpc.core.swords.SwordProgression.crossedAnyMilestone(oldLvl, newLvl)) {
         return SwordMessages.pickEpic(newLvl);
      } else {
         return delta > 0 ? SwordMessages.pickUp(newLvl) : SwordMessages.pickDown(newLvl);
      }
   }

   @FunctionalInterface
   interface NpcScalingApplier {
      void apply(Store<EntityStore> var1, Ref<EntityStore> var2, UUID var3, AmigoNpcManager.NpcRecord var4);
   }

   @FunctionalInterface
   interface OwnerMessenger {
      void send(Object var1, UUID var2, String var3);
   }

   @FunctionalInterface
   interface StoreResolver {
      Object resolve(Object var1);
   }

   @FunctionalInterface
   interface SwordWeaponApplier {
      void apply(Store<EntityStore> var1, Ref<EntityStore> var2, UUID var3, AmigoNpcManager.NpcRecord var4, boolean var5);
   }
}
