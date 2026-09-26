package br.tones.amigonpc.core.zones;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class ZoneMobService {
   private static final ZoneMobService SHARED = new ZoneMobService();
   private final MobLevelVarianceCalculator mobLevels = new MobLevelVarianceCalculator();

   public static ZoneMobService getShared() {
      return SHARED;
   }

   private ZoneMobService() {
   }

   public void reload() {
      AmigoZonesConfigService.reload();
      this.mobLevels.reloadConfig();
   }

   public ZoneMobService.ZoneSnapshot computeZoneSnapshot(Object worldObj, Store<EntityStore> store, Object ownerRefObj, int ownerLevel) {
      AmigoZonesConfig cfg = AmigoZonesConfigService.get();
      if (!cfg.enableZones) {
         return new ZoneMobService.ZoneSnapshot(0, 0, 0, ZoneModel.WHITE);
      }

      ZoneResolver.ZoneContext ctx = ZoneResolver.resolve(worldObj, store, ownerRefObj);
      int expected = ZoneModel.expectedZoneForLevel(ownerLevel);
      Integer current = ZoneModel.currentZoneFromContext(ctx.zoneId(), ctx.instanceLevelMin(), ctx.instanceLevelMax());
      int currentZone = current != null ? current : 0;
      Color color = currentZone > 0 ? ZoneModel.colorForNpcLevel(ownerLevel, currentZone) : ZoneModel.WHITE;
      if (cfg.debugLog) {
         System.out
            .println(
               "[AmigoNPC][Zones] rawZoneId="
                  + ctx.zoneId()
                  + " expected="
                  + expected
                  + " current="
                  + currentZone
                  + " biome="
                  + ctx.biomeName()
                  + " instanceId="
                  + ctx.instanceId()
            );
      }

      return new ZoneMobService.ZoneSnapshot(ctx.zoneId(), expected, currentZone, color);
   }

   public MobLevelVarianceCalculator.Result computeMobLevel(
      Store<EntityStore> store, Ref<EntityStore> targetRef, Object worldObj, int zoneId, int instMin, int instMax, String instanceId
   ) {
      AmigoZonesConfig cfg = AmigoZonesConfigService.get();
      if (!cfg.enableMobLevelVariance && !cfg.enableZones) {
         return new MobLevelVarianceCalculator.Result(0, 0, 0, 0, true);
      }

      float maxHp = extractMaxHp(store, targetRef);
      if (!(maxHp > 0.0F)) {
         return new MobLevelVarianceCalculator.Result(0, 0, 0, 0, true);
      }

      UUID uuid = extractUuid(store, targetRef);
      return this.mobLevels.compute(uuid, maxHp, zoneId, instMin, instMax, instanceId);
   }

   private static float extractMaxHp(Store<EntityStore> store, Ref<EntityStore> targetRef) {
      try {
         EntityStatMap stats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
         if (stats == null) {
            return 0.0F;
         }

         int idx = DefaultEntityStatTypes.getHealth();
         EntityStatValue v = stats.get(idx);
         return v == null ? 0.0F : v.getMax();
      } catch (Throwable ignored) {
         return 0.0F;
      }
   }

   private static UUID extractUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
      try {
         UUIDComponent uc = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
         if (uc != null) {
            return uc.getUuid();
         }
      } catch (Throwable var4) {
      }

      try {
         return UUID.nameUUIDFromBytes(("AmigoNPC:mob:" + ref).getBytes());
      } catch (Throwable ignored) {
         return UUID.randomUUID();
      }
   }

   public record ZoneSnapshot(int rawZoneId, int expectedZone, int currentZone, Color color) {
   }
}
