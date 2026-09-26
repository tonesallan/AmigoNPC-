package br.tones.amigonpc.core.worldscaling;

import br.tones.amigonpc.core.zones.MobLevelVarianceCalculator;
import br.tones.amigonpc.core.zones.ZoneResolver;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldMobScalingService {
   private static final WorldMobScalingService SHARED = new WorldMobScalingService();
   private static final String HP_MOD_KEY = "amigonpc_mob_hpmax";
   private final Map<UUID, WorldMobScalingService.CacheEntry> cache = new ConcurrentHashMap<>();
   private final MobLevelVarianceCalculator lvlCalc = new MobLevelVarianceCalculator();

   public static WorldMobScalingService getShared() {
      return SHARED;
   }

   private WorldMobScalingService() {
   }

   public void clearCache() {
      this.cache.clear();
   }

   public boolean isEnabled() {
      return WorldMobScalingConfigService.get().enableWorldMobScaling;
   }

   public boolean isWhitelisted(String worldName, String instanceId) {
      WorldMobScalingConfig cfg = WorldMobScalingConfigService.get();
      if (!cfg.enableWorldMobScaling) {
         return false;
      }

      String wn = worldName == null ? "" : worldName.trim();
      String in = instanceId == null ? "" : instanceId.trim();
      boolean okWorld = false;
      if (cfg.whitelistWorlds != null) {
         for (String w : cfg.whitelistWorlds) {
            if (w != null && w.equalsIgnoreCase(wn)) {
               okWorld = true;
               break;
            }
         }
      }

      boolean okInst = false;
      if (cfg.whitelistInstances != null) {
         for (String w : cfg.whitelistInstances) {
            if (w != null && w.equalsIgnoreCase(in)) {
               okInst = true;
               break;
            }
         }
      }

      boolean anyList = cfg.whitelistWorlds != null && !cfg.whitelistWorlds.isEmpty() || cfg.whitelistInstances != null && !cfg.whitelistInstances.isEmpty();
      return !anyList ? false : okWorld || okInst;
   }

   public int maybeApplyHpScaling(Store<EntityStore> store, Ref<EntityStore> mobRef, World world) {
      if (store != null && mobRef != null && world != null) {
         WorldMobScalingConfig cfg = WorldMobScalingConfigService.get();
         if (!cfg.enableWorldMobScaling) {
            return 0;
         }

         String worldName = safeWorldName(world);
         String instanceId = worldName;
         if (!this.isWhitelisted(worldName, instanceId)) {
            this.removeHpModifier(store, mobRef);
            return 0;
         }

         if (store.getComponent(mobRef, Player.getComponentType()) != null) {
            return 0;
         }

         UUID mobUuid = null;

         try {
            UUIDComponent u = (UUIDComponent)store.getComponent(mobRef, UUIDComponent.getComponentType());
            if (u != null) {
               mobUuid = u.getUuid();
            }
         } catch (Throwable var12) {
         }

         if (mobUuid == null) {
            return 0;
         } else {
            long now = System.currentTimeMillis();
            WorldMobScalingService.CacheEntry ce = this.cache.get(mobUuid);
            if (ce != null && now - ce.atMillis < cfg.cacheTtlMs) {
               this.applyHpModifierIfMissing(store, mobRef, ce.level, cfg);
               return ce.level;
            } else {
               int mobLevel = this.computeMobLevel(store, mobRef, world, mobUuid);
               this.cache.put(mobUuid, new WorldMobScalingService.CacheEntry(mobLevel, now));
               this.applyHpModifierIfMissing(store, mobRef, mobLevel, cfg);
               return mobLevel;
            }
         }
      } else {
         return 0;
      }
   }

   public float scaleOutgoingDamage(Store<EntityStore> store, Ref<EntityStore> attackerRef, World world, float amount) {
      if (!(amount <= 0.0F) && store != null && attackerRef != null && world != null) {
         WorldMobScalingConfig cfg = WorldMobScalingConfigService.get();
         if (!cfg.enableWorldMobScaling) {
            return amount;
         }

         String worldName = safeWorldName(world);
         String instanceId = worldName;
         if (!this.isWhitelisted(worldName, instanceId)) {
            return amount;
         }

         if (store.getComponent(attackerRef, Player.getComponentType()) != null) {
            return amount;
         }

         UUID mobUuid = null;

         try {
            UUIDComponent u = (UUIDComponent)store.getComponent(attackerRef, UUIDComponent.getComponentType());
            if (u != null) {
               mobUuid = u.getUuid();
            }
         } catch (Throwable var15) {
         }

         if (mobUuid == null) {
            return amount;
         }

         long now = System.currentTimeMillis();
         WorldMobScalingService.CacheEntry ce = this.cache.get(mobUuid);
         int level;
         if (ce != null && now - ce.atMillis < cfg.cacheTtlMs) {
            level = ce.level;
         } else {
            level = this.computeMobLevel(store, attackerRef, world, mobUuid);
            this.cache.put(mobUuid, new WorldMobScalingService.CacheEntry(level, now));
         }

         double out = amount;
         if (cfg.damagePerLevelPercent != 0.0) {
            out += out * (cfg.damagePerLevelPercent * level);
         }

         if (cfg.damageAddPerLevel != 0.0) {
            out += cfg.damageAddPerLevel * level;
         }

         if (out < 0.0) {
            out = 0.0;
         }

         if (out > Float.MAX_VALUE) {
            out = Float.MAX_VALUE;
         }

         return (float)out;
      } else {
         return amount;
      }
   }

   private int computeMobLevel(Store<EntityStore> store, Ref<EntityStore> mobRef, World world, UUID mobUuid) {
      float maxHp = 1.0F;

      try {
         EntityStatMap stats = (EntityStatMap)store.getComponent(mobRef, EntityStatMap.getComponentType());
         if (stats != null) {
            maxHp = Math.max(1.0F, stats.get(DefaultEntityStatTypes.getHealth()).getMax());
         }
      } catch (Throwable var12) {
      }

      int zoneId = 0;
      int instMin = 0;
      int instMax = 0;
      String instanceId = safeWorldName(world);

      try {
         ZoneResolver.ZoneContext ctx = ZoneResolver.resolve(world, store, mobRef);
         if (ctx != null) {
            zoneId = Math.max(0, ctx.zoneId());
            instMin = Math.max(0, ctx.instanceLevelMin());
            instMax = Math.max(0, ctx.instanceLevelMax());
            if (ctx.instanceId() != null && !ctx.instanceId().isBlank()) {
               instanceId = ctx.instanceId();
            }
         }
      } catch (Throwable var11) {
      }

      try {
         MobLevelVarianceCalculator.Result r = this.lvlCalc.compute(mobUuid, maxHp, zoneId, instMin, instMax, instanceId);
         return Math.max(1, r.finalLevel());
      } catch (Throwable var13) {
         int base = Math.round(maxHp / 3.0F);
         if (base < 1) {
            base = 1;
         }

         if (base > 100) {
            base = 100;
         }

         return base;
      }
   }

   private void applyHpModifierIfMissing(Store<EntityStore> store, Ref<EntityStore> mobRef, int level, WorldMobScalingConfig cfg) {
      if (cfg.hpPerLevelPercent != 0.0 || cfg.hpAddPerLevel != 0.0) {
         try {
            EntityStatMap stats = (EntityStatMap)store.getComponent(mobRef, EntityStatMap.getComponentType());
            if (stats == null) {
               return;
            }

            int healthIdx = DefaultEntityStatTypes.getHealth();

            try {
               stats.removeModifier(healthIdx, "amigonpc_mob_hpmax");
            } catch (Throwable var19) {
            }

            try {
               stats.update();
            } catch (Throwable var18) {
            }

            float baseMax = Math.max(1.0F, stats.get(healthIdx).getMax());
            double desired = baseMax;
            if (cfg.hpPerLevelPercent != 0.0) {
               desired += baseMax * (cfg.hpPerLevelPercent * level);
            }

            if (cfg.hpAddPerLevel != 0.0) {
               desired += cfg.hpAddPerLevel * level;
            }

            if (desired < 1.0) {
               desired = 1.0;
            }

            double delta = desired - baseMax;
            if (Math.abs(delta) < 1.0E-4) {
               try {
                  store.putComponent(mobRef, EntityStatMap.getComponentType(), stats);
               } catch (Throwable var14) {
               }

               return;
            }

            float amt;
            if (delta > Float.MAX_VALUE) {
               amt = Float.MAX_VALUE;
            } else if (delta < -Float.MAX_VALUE) {
               amt = -Float.MAX_VALUE;
            } else {
               amt = (float)delta;
            }

            try {
               stats.putModifier(healthIdx, "amigonpc_mob_hpmax", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, amt));
            } catch (Throwable var17) {
            }

            try {
               stats.update();
            } catch (Throwable var16) {
            }

            try {
               store.putComponent(mobRef, EntityStatMap.getComponentType(), stats);
            } catch (Throwable var15) {
            }
         } catch (Throwable var20) {
         }
      }
   }

   private void removeHpModifier(Store<EntityStore> store, Ref<EntityStore> mobRef) {
      try {
         EntityStatMap stats = (EntityStatMap)store.getComponent(mobRef, EntityStatMap.getComponentType());
         if (stats == null) {
            return;
         }

         int healthIdx = DefaultEntityStatTypes.getHealth();

         try {
            stats.removeModifier(healthIdx, "amigonpc_mob_hpmax");
         } catch (Throwable var8) {
         }

         try {
            stats.update();
         } catch (Throwable var7) {
         }

         try {
            store.putComponent(mobRef, EntityStatMap.getComponentType(), stats);
         } catch (Throwable var6) {
         }
      } catch (Throwable var9) {
      }
   }

   private static String safeWorldName(World w) {
      if (w == null) {
         return "";
      }

      try {
         String n = w.getName();
         return n != null ? n : "";
      } catch (Throwable ignored) {
         return "";
      }
   }

   private static final class CacheEntry {
      final int level;
      final long atMillis;

      CacheEntry(int level, long atMillis) {
         this.level = level;
         this.atMillis = atMillis;
      }
   }
}
