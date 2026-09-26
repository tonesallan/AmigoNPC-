package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class NpcFollowRecoveryLootSupport {
   private NpcFollowRecoveryLootSupport() {
   }

   static boolean tick(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d ownerPos,
      Vector3d npcPos,
      long now,
      Object worldObj,
      long regenDelayMillis,
      NpcFollowRecoveryLootSupport.SpawnFollowFxTicker spawnFollowFxTicker,
      NpcFollowRecoveryLootSupport.AutoRegenTicker autoRegenTicker,
      NpcFollowRecoveryLootSupport.CombatTaggedLootTicker combatTaggedLootTicker,
      NpcFollowRecoveryLootSupport.AutoLootRunner autoLootRunner,
      NpcFollowRecoveryLootSupport.CombatLootEnder combatLootEnder
   ) {
      boolean inCombatOrAssistNow = NpcFollowMaintenanceSupport.isInCombatOrAssistNow(rec, now);

      try {
         NpcFollowMaintenanceSupport.updateRegenAndAutoLootStick(rec, now, regenDelayMillis, ownerPos, npcPos, inCombatOrAssistNow);
         spawnFollowFxTicker.tick(store, rec, ownerRefObj, now);
         autoRegenTicker.tick(store, rec, ownerRefObj, now);
      } catch (Throwable var18) {
      }

      if (rec.autoLootEnabled) {
         boolean pauseTaggedLootNow = inCombatOrAssistNow;
         combatTaggedLootTicker.tick(store, ownerId, rec, ownerRefObj, npcPos, ownerPos, now, worldObj, pauseTaggedLootNow);
         if (!inCombatOrAssistNow && !rec.lootingActive) {
            autoLootRunner.run(store, ownerId, rec, npcPos, ownerPos, now, worldObj);
         }
      } else {
         combatLootEnder.end(rec);
      }

      return inCombatOrAssistNow;
   }

   @FunctionalInterface
   interface AutoLootRunner {
      void run(Store<EntityStore> var1, UUID var2, AmigoNpcManager.NpcRecord var3, Vector3d var4, Vector3d var5, long var6, Object var8);
   }

   @FunctionalInterface
   interface AutoRegenTicker {
      void tick(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, long var4);
   }

   @FunctionalInterface
   interface CombatLootEnder {
      void end(AmigoNpcManager.NpcRecord var1);
   }

   @FunctionalInterface
   interface CombatTaggedLootTicker {
      void tick(
         Store<EntityStore> var1, UUID var2, AmigoNpcManager.NpcRecord var3, Object var4, Vector3d var5, Vector3d var6, long var7, Object var9, boolean var10
      );
   }

   @FunctionalInterface
   interface SpawnFollowFxTicker {
      void tick(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, long var4);
   }
}
