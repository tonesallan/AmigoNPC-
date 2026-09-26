package br.tones.amigonpc.core;

import br.tones.amigonpc.core.zones.ZoneMobService;
import br.tones.amigonpc.core.zones.ZoneModel;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class NpcFollowContextSupport {
   private NpcFollowContextSupport() {
   }

   static NpcFollowContextSupport.Context prepare(
      Store<EntityStore> store,
      Object worldObj,
      Object ownerRefObj,
      AmigoNpcManager.NpcRecord rec,
      Vector3d ownerPos,
      Vector3d npcPos,
      double chaseReacquireDistance,
      long undergroundCheckIntervalMillis,
      NpcFollowContextSupport.UndergroundChecker undergroundChecker,
      NpcFollowContextSupport.AttackAnimationCleaner attackAnimationCleaner,
      NpcFollowContextSupport.LevelUpFxTicker levelUpFxTicker,
      NpcFollowContextSupport.WardrobeRestoreTicker wardrobeRestoreTicker
   ) {
      if (store != null && rec != null && ownerPos != null && npcPos != null) {
         updateZoneSnapshot(worldObj, store, ownerRefObj, rec);
         double dx = ownerPos.getX() - npcPos.getX();
         double dz = ownerPos.getZ() - npcPos.getZ();
         double dy = ownerPos.getY() - npcPos.getY();
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         updateChaseDistanceState(rec, horizontal, chaseReacquireDistance);
         long now = System.currentTimeMillis();
         updateUndergroundCache(store, rec, ownerPos, now, undergroundCheckIntervalMillis, undergroundChecker);
         updateMovementSample(rec, npcPos, now);
         attackAnimationCleaner.clear(store, rec.refObj, rec, now);

         try {
            levelUpFxTicker.tick(store, ownerRefObj, rec.refObj, rec, now);
         } catch (Throwable var26) {
         }

         try {
            wardrobeRestoreTicker.tick(store, rec, ownerRefObj, now);
         } catch (Throwable var25) {
         }

         return new NpcFollowContextSupport.Context(now, horizontal, dy, rec.ownerUnderground);
      } else {
         return new NpcFollowContextSupport.Context(System.currentTimeMillis(), 0.0, 0.0, false);
      }
   }

   private static void updateZoneSnapshot(Object worldObj, Store<EntityStore> store, Object ownerRefObj, AmigoNpcManager.NpcRecord rec) {
      try {
         int ownerLevel = Math.max(1, rec.npcLevelCached);
         ZoneMobService.ZoneSnapshot zs = ZoneMobService.getShared().computeZoneSnapshot(worldObj, store, ownerRefObj, ownerLevel);
         rec.zoneRawId = zs.rawZoneId();
         rec.zoneExpected = zs.expectedZone();
         rec.zoneCurrent = zs.currentZone();
         rec.zoneHudColor = zs.color();
      } catch (Throwable ignored) {
         rec.zoneRawId = 0;
         rec.zoneExpected = 0;
         rec.zoneCurrent = 0;
         rec.zoneHudColor = ZoneModel.WHITE;
      }
   }

   private static void updateChaseDistanceState(AmigoNpcManager.NpcRecord rec, double horizontal, double chaseReacquireDistance) {
      if (rec.chaseDisengaged && horizontal <= chaseReacquireDistance) {
         rec.chaseDisengaged = false;
      }

      if (horizontal > 25.0) {
         if (rec.farSinceMillis == 0L) {
            rec.farSinceMillis = System.currentTimeMillis();
         }
      } else {
         rec.farSinceMillis = 0L;
      }
   }

   private static void updateUndergroundCache(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Vector3d ownerPos,
      long now,
      long undergroundCheckIntervalMillis,
      NpcFollowContextSupport.UndergroundChecker undergroundChecker
   ) {
      if (rec.nextUndergroundCheckMillis <= 0L || now >= rec.nextUndergroundCheckMillis) {
         rec.nextUndergroundCheckMillis = now + undergroundCheckIntervalMillis;
         rec.ownerUnderground = undergroundChecker.check(store, ownerPos);
      }
   }

   private static void updateMovementSample(AmigoNpcManager.NpcRecord rec, Vector3d npcPos, long now) {
      if (rec.lastNpcMovedMillis == 0L) {
         rec.lastNpcMovedMillis = now;
      }

      if (rec.lastSampleMillis == 0L) {
         rec.lastSampleMillis = now;
      }

      if (rec.lastNpcPos == null) {
         rec.lastNpcPos = npcPos;
      }

      if (now - rec.lastSampleMillis >= 400L) {
         double mdx = npcPos.getX() - rec.lastNpcPos.getX();
         double mdy = npcPos.getY() - rec.lastNpcPos.getY();
         double mdz = npcPos.getZ() - rec.lastNpcPos.getZ();
         double moved = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
         if (moved > 0.25) {
            rec.lastNpcMovedMillis = now;
         }

         rec.lastNpcPos = npcPos;
         rec.lastSampleMillis = now;
      }
   }

   @FunctionalInterface
   interface AttackAnimationCleaner {
      void clear(Store<EntityStore> var1, Object var2, AmigoNpcManager.NpcRecord var3, long var4);
   }

   static final class Context {
      final long now;
      final double horizontal;
      final double dy;
      final boolean ownerUnderground;

      Context(long now, double horizontal, double dy, boolean ownerUnderground) {
         this.now = now;
         this.horizontal = horizontal;
         this.dy = dy;
         this.ownerUnderground = ownerUnderground;
      }
   }

   @FunctionalInterface
   interface LevelUpFxTicker {
      void tick(Store<EntityStore> var1, Object var2, Object var3, AmigoNpcManager.NpcRecord var4, long var5);
   }

   @FunctionalInterface
   interface UndergroundChecker {
      boolean check(Store<EntityStore> var1, Vector3d var2);
   }

   @FunctionalInterface
   interface WardrobeRestoreTicker {
      void tick(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, long var4);
   }
}
