package br.tones.amigonpc.core.playerstats;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStatTweaksService {
   private static final PlayerStatTweaksService SHARED = new PlayerStatTweaksService();
   private static final String KEY_STAMINA_ADD = "amigonpc_player_stamina_add";
   private static final String KEY_STAMINA_MUL = "amigonpc_player_stamina_mul";
   private static final String KEY_MANA_ADD = "amigonpc_player_mana_add";
   private static final String KEY_MANA_MUL = "amigonpc_player_mana_mul";
   private static final String KEY_OXY_ADD = "amigonpc_player_oxygen_add";
   private static final String KEY_OXY_MUL = "amigonpc_player_oxygen_mul";
   private static final String KEY_AMMO_ADD = "amigonpc_player_ammo_add";
   private static final String KEY_AMMO_MUL = "amigonpc_player_ammo_mul";
   private final ConcurrentHashMap<String, Boolean> applied = new ConcurrentHashMap<>();

   public static PlayerStatTweaksService getShared() {
      return SHARED;
   }

   private PlayerStatTweaksService() {
   }

   public void onPlayerJoin(Player player, PlayerRef pref, UUID uuid) {
      if (player != null && uuid != null) {
         this.applyOrRemove(player, uuid, false);
      }
   }

   public void onPlayerLeave(Player player, PlayerRef pref) {
      if (player != null) {
         try {
            UUID uuid = null;

            try {
               uuid = pref != null ? pref.getUuid() : null;
            } catch (Throwable var5) {
            }

            if (uuid != null) {
               this.applied.remove(uuid.toString());
            }
         } catch (Throwable var6) {
         }
      }
   }

   public void applyOrRemove(Player player, UUID uuid, boolean force) {
      if (player != null && uuid != null) {
         PlayerStatTweaksConfig cfg = PlayerStatTweaksConfigService.get();
         boolean enabled = cfg.enablePlayerStatTweaks;
         if (enabled && cfg.disableIfRPGLevelingPresent && isRpgLevelingPresent()) {
            enabled = false;
         }

         String k = uuid.toString();
         Boolean was = this.applied.get(k);
         if (force || was == null || was != enabled) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
               Store<EntityStore> store = ref.getStore();
               if (store != null) {
                  if (!enabled) {
                     removeAll(store, ref);
                     this.applied.put(k, Boolean.FALSE);
                  } else {
                     applyNow(cfg, store, ref);
                     this.applied.put(k, Boolean.TRUE);
                  }
               }
            }
         }
      }
   }

   private static boolean isRpgLevelingPresent() {
      try {
         PluginManager pm = PluginManager.get();
         if (pm == null) {
            return false;
         }

         for (PluginBase p : pm.getPlugins()) {
            if (p != null) {
               String name = null;

               try {
                  name = p.getName();
               } catch (Throwable var7) {
               }

               if (name != null && name.equalsIgnoreCase("RPGLeveling")) {
                  return true;
               }

               try {
                  PluginIdentifier id = p.getIdentifier();
                  if (id != null) {
                     String s = id.toString();
                     if (s.toLowerCase(Locale.ROOT).contains("rpgleveling")) {
                        return true;
                     }
                  }
               } catch (Throwable var6) {
               }
            }
         }
      } catch (Throwable var8) {
      }

      return false;
   }

   private static void applyNow(PlayerStatTweaksConfig cfg, Store<EntityStore> store, Ref<EntityStore> playerRef) {
      if (cfg != null && store != null && playerRef != null) {
         EntityStatMap stats;
         try {
            stats = (EntityStatMap)store.ensureAndGetComponent(playerRef, EntityStatMap.getComponentType());
         } catch (Throwable t) {
            return;
         }

         int staminaIdx = safeIndex(() -> DefaultEntityStatTypes.getStamina());
         int manaIdx = safeIndex(() -> DefaultEntityStatTypes.getMana());
         int oxygenIdx = safeIndex(() -> DefaultEntityStatTypes.getOxygen());
         int ammoIdx = safeIndex(() -> DefaultEntityStatTypes.getAmmo());
         if (cfg.enableStamina && staminaIdx >= 0) {
            applyOne(stats, staminaIdx, "amigonpc_player_stamina_add", "amigonpc_player_stamina_mul", cfg.staminaAddMax, cfg.staminaMultMax);
         } else if (staminaIdx >= 0) {
            removeOne(stats, staminaIdx, "amigonpc_player_stamina_add", "amigonpc_player_stamina_mul");
         }

         if (cfg.enableMana && manaIdx >= 0) {
            applyOne(stats, manaIdx, "amigonpc_player_mana_add", "amigonpc_player_mana_mul", cfg.manaAddMax, cfg.manaMultMax);
         } else if (manaIdx >= 0) {
            removeOne(stats, manaIdx, "amigonpc_player_mana_add", "amigonpc_player_mana_mul");
         }

         if (cfg.enableOxygen && oxygenIdx >= 0) {
            applyOne(stats, oxygenIdx, "amigonpc_player_oxygen_add", "amigonpc_player_oxygen_mul", cfg.oxygenAddMax, cfg.oxygenMultMax);
         } else if (oxygenIdx >= 0) {
            removeOne(stats, oxygenIdx, "amigonpc_player_oxygen_add", "amigonpc_player_oxygen_mul");
         }

         if (cfg.enableAmmo && ammoIdx >= 0) {
            applyOne(stats, ammoIdx, "amigonpc_player_ammo_add", "amigonpc_player_ammo_mul", cfg.ammoAddMax, cfg.ammoMultMax);
         } else if (ammoIdx >= 0) {
            removeOne(stats, ammoIdx, "amigonpc_player_ammo_add", "amigonpc_player_ammo_mul");
         }

         try {
            stats.update();
         } catch (Throwable var10) {
         }

         try {
            store.putComponent(playerRef, EntityStatMap.getComponentType(), stats);
         } catch (Throwable var9) {
         }
      }
   }

   private static void removeAll(Store<EntityStore> store, Ref<EntityStore> playerRef) {
      if (store != null && playerRef != null) {
         try {
            EntityStatMap stats = (EntityStatMap)store.getComponent(playerRef, EntityStatMap.getComponentType());
            if (stats == null) {
               return;
            }

            int staminaIdx = safeIndex(() -> DefaultEntityStatTypes.getStamina());
            int manaIdx = safeIndex(() -> DefaultEntityStatTypes.getMana());
            int oxygenIdx = safeIndex(() -> DefaultEntityStatTypes.getOxygen());
            int ammoIdx = safeIndex(() -> DefaultEntityStatTypes.getAmmo());
            if (staminaIdx >= 0) {
               removeOne(stats, staminaIdx, "amigonpc_player_stamina_add", "amigonpc_player_stamina_mul");
            }

            if (manaIdx >= 0) {
               removeOne(stats, manaIdx, "amigonpc_player_mana_add", "amigonpc_player_mana_mul");
            }

            if (oxygenIdx >= 0) {
               removeOne(stats, oxygenIdx, "amigonpc_player_oxygen_add", "amigonpc_player_oxygen_mul");
            }

            if (ammoIdx >= 0) {
               removeOne(stats, ammoIdx, "amigonpc_player_ammo_add", "amigonpc_player_ammo_mul");
            }

            try {
               stats.update();
            } catch (Throwable var9) {
            }

            try {
               store.putComponent(playerRef, EntityStatMap.getComponentType(), stats);
            } catch (Throwable var8) {
            }
         } catch (Throwable var10) {
         }
      }
   }

   private static void removeOne(EntityStatMap stats, int idx, String addKey, String mulKey) {
      try {
         stats.removeModifier(idx, addKey);
      } catch (Throwable var6) {
      }

      try {
         stats.removeModifier(idx, mulKey);
      } catch (Throwable var5) {
      }
   }

   private static void applyOne(EntityStatMap stats, int idx, String addKey, String mulKey, float add, float mul) {
      removeOne(stats, idx, addKey, mulKey);
      if (add != 0.0F) {
         try {
            stats.putModifier(idx, addKey, new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, add));
         } catch (Throwable var8) {
         }
      }

      if (mul != 0.0F) {
         try {
            stats.putModifier(idx, mulKey, new StaticModifier(ModifierTarget.MAX, CalculationType.MULTIPLICATIVE, mul));
         } catch (Throwable var7) {
         }
      }
   }

   private static int safeIndex(PlayerStatTweaksService.IntSupplier s) {
      try {
         int v = s.get();
         return v >= 0 ? v : -1;
      } catch (Throwable t) {
         return -1;
      }
   }

   private interface IntSupplier {
      int get();
   }
}
