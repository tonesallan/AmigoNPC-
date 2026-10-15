package br.tones.amigonpc.core;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.api.events.NpcExperienceGainedEvent;
import br.tones.amigonpc.api.events.NpcLevelUpEvent;
import br.tones.amigonpc.core.autoloot.AutoLootConfig;
import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.events.AmigoEventBus;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.npcstats.AttributeModifierService;
import br.tones.amigonpc.core.npcstats.NpcStatsConfigService;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import br.tones.amigonpc.core.optional.AmigoWardrobePersistence;
import br.tones.amigonpc.core.progress.StatScaling;
import br.tones.amigonpc.core.progress.XpProgression;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgLevelingStage1Config;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1Formulas;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelCalculator;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelHelper;
import br.tones.amigonpc.core.ui.lvlgui.AmigoLvlGuiService;
import br.tones.amigonpc.core.xp.sources.NpcCollectXpSource;
import br.tones.amigonpc.core.zones.AmigoZonesConfigService;
import br.tones.amigonpc.core.zones.MobLevelVarianceCalculator;
import br.tones.amigonpc.core.zones.ZoneMobService;
import br.tones.amigonpc.core.zones.ZoneModel;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.ItemAnimation;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.RespondToHit;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class AmigoNpcManager {
   private static final AmigoNpcManager SHARED = new AmigoNpcManager();
   private static final String DEFAULT_MODEL_ID = "PlayerTestModel_V";
   private static final double DEFAULT_MODEL_SCALE = (double)1.0F;
   private static final String DEFAULT_ROLE_NAME = "Amigo_Follow";
   private static final boolean DEBUG_COMBAT_DEFAULT = false;
   private static final Map WEAPON_ATTACK_ANIM_CACHE = new ConcurrentHashMap();
   private final Map npcRefPorPlayer = new ConcurrentHashMap();
   private final Map debugLogByOwner = new ConcurrentHashMap();
   private final Map pendingRespawns = new ConcurrentHashMap();
   private final Map amigoRefs = new ConcurrentHashMap();
   private static volatile String LAST_ERROR;
   private volatile boolean pvpEnabled = false;
   private static final long COMBAT_WINDOW_MILLIS = 3000L;
   private static final long ASSIST_GRACE_MILLIS = 3000L;
   private static final long MELEE_COOLDOWN_MILLIS = 850L;
   private static final double DEFENDER_AUTO_ACQUIRE_RADIUS = (double)12.0F;
   private static final double DEFENDER_AUTO_ACQUIRE_MAX_DY = (double)2.5F;
   private static final double CHASE_MAX_DISTANCE = (double)30.0F;
   private static final double CHASE_REACQUIRE_DISTANCE = (double)10.0F;
   private static final double RANGED_AIR_MAX_DISTANCE = (double)20.0F;
   private static final double RANGED_AIR_MAX_DY = (double)35.0F;
   private static final long RANGED_COOLDOWN_MILLIS = 1150L;
   private static final long LEVEL_UP_FX_MILLIS = 1000L;
   private static final long LEVEL_UP_FX_TICK_MILLIS = 250L;
   private static final double AUTOLOOT_RADIUS = (double)5.0F;
   private static final double AUTOLOOT_OWNER_RADIUS = (double)20.0F;
   private static final long AUTOLOOT_INTERVAL_MS = 250L;
   private static final int AUTOLOOT_MAX_ITEMS_PER_SCAN = 8;
   private static final long AUTOLOOT_FULL_MSG_COOLDOWN_MS = 30000L;
   private static final Archetype AUTOLOOT_QUERY = Archetype.of(new ComponentType[]{TransformComponent.getComponentType(), ItemComponent.getComponentType()});
   private static final long BACKPACK_SAVE_DEBOUNCE_MS = 400L;
   private static final long UNDERGROUND_CHECK_INTERVAL_MS = 500L;
   private static final int UNDERGROUND_CEILING_SCAN_BLOCKS = 12;
   private static final double UNDERGROUND_TELEPORT_DISTANCE = (double)10.0F;
   private static final String LOCKED_TARGET_CLOSE_SLOT = "LockedTargetClose";
   private static final double LOOTING_NO_ENEMY_RADIUS = (double)4.0F;
   private static final double LOOT_TAG_SCAN_RADIUS = (double)6.0F;
   private static final double LOOT_PICKUP_DISTANCE = (double)5.0F;
   private static final String LOOT_CHAT_TEMPLATE = "{quantidade} {item} Coletado.";
   private static final long LOOT_CHAT_SUMMARY_DELAY_MS = 500L;
   private static final long COMBAT_TAG_CLEAR_MS = 25000L;
   private static final double COMBAT_TAG_CLEAR_DISTANCE = (double)25.0F;
   private static final long LOOT_SKIP_RETRY_MS = 4000L;
   private static final long LOOT_TARGET_TIMEOUT_MS = 12000L;
   private static final long LOOT_POST_COMBAT_STICK_MS = 500L;
   private static final int COMBAT_TAG_MAX = 16;
   private static final int LOOT_PENDING_MAX = 512;
   private static final long XP_PER_HIT = 0L;
   private static final long XP_PER_KILL = 40L;
   private static final RpgLevelingStage1Config RPG_STAGE1_CFG = new RpgLevelingStage1Config();
   private static final RpgStage1MobLevelCalculator RPG_STAGE1_MOB_CALC;
   private static final long SPAWN_FX_FOLLOW_MS = 1800L;
   private static final long SPAWN_FX_FOLLOW_INTERVAL_MS = 180L;
   private static final long REGEN_DELAY_MS = 3000L;
   private static final float REGEN_RATE_PER_SECOND = 0.12F;
   private static volatile Integer DEF_STAT_INDEX;
   private static volatile String RESOLVED_BOW_ID;

   public static AmigoNpcManager getShared() {
      return SHARED;
   }

   public static boolean saveNpcWardrobeNow(UUID ownerId) {
      try {
         return SHARED.saveNpcWardrobeNowInternal(ownerId);
      } catch (Throwable var2) {
         return false;
      }
   }

   public NpcDebugSnapshot getNpcDebugSnapshot(UUID ownerId) {
      NpcDebugSnapshot s = new NpcDebugSnapshot();
      s.ownerUuid = ownerId != null ? String.valueOf(ownerId) : null;
      if (ownerId == null) {
         s.hasRecord = false;
         s.hasNpcRef = false;
         s.state = "NONE";
         return s;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            s.hasRecord = false;
            s.hasNpcRef = false;
            s.state = "MISSING";
            return s;
         } else {
            s.hasRecord = true;
            s.hasNpcRef = rec.refObj != null;
            s.state = rec.state != null ? rec.state.name() : "UNKNOWN";
            s.downed = rec.downed;
            s.downedUntilMillis = rec.downedUntilMillis;
            s.deathDespawnAtMillis = rec.deathDespawnAtMillis;
            s.npcLevel = rec.npcLevelCached;
            s.totalXp = rec.totalXp;
            s.xpRemainder = rec.stage1XpRemainder;
            s.inCombatRecently = rec.wasInCombat;
            s.lastCombatTagMillis = rec.lastCombatTagMillis;
            s.lastCombatEndMillis = rec.lastCombatEndMillis;
            s.lootingActive = rec.lootingActive;
            s.lootStickUntilMillis = rec.lootStickUntilMillis;
            s.regenStartAtMillis = rec.regenStartAtMillis;
            s.regenLastApplyMillis = rec.regenLastApplyMillis;
            s.regenActive = rec.regenStartAtMillis > 0L;
            s.lootPausedInventoryFull = rec.lootPausedInventoryFull;
            s.lastOwnerPos = vec(rec.lastOwnerPos);
            s.lastNpcPos = vec(rec.lastNpcPos);
            s.lastBattleCenter = vec(rec.lastBattleCenter);

            try {
               if (rec.backpack != null) {
                  s.backpackSlotsTotal = 45;
                  int used = 0;
                  Object items = null;

                  try {
                     Method m = rec.backpack.getClass().getMethod("getItems");
                     items = m.invoke(rec.backpack);
                  } catch (Throwable var11) {
                  }

                  if (items instanceof List) {
                     for(Object it : (List)items) {
                        if (it != null) {
                           boolean empty = false;

                           try {
                              Method m2 = it.getClass().getMethod("isEmpty");
                              empty = Boolean.TRUE.equals(m2.invoke(it));
                           } catch (Throwable var10) {
                           }

                           if (!empty) {
                              ++used;
                           }
                        }
                     }
                  }

                  s.backpackSlotsUsed = used;
               } else {
                  s.backpackSlotsTotal = 45;
                  s.backpackSlotsUsed = 0;
               }
            } catch (Throwable var12) {
               s.backpackSlotsTotal = 45;
            }

            return s;
         }
      }
   }

   private static Vec3 vec(Vector3d v) {
      if (v == null) {
         return null;
      } else {
         try {
            return new Vec3(v.x, v.y, v.z);
         } catch (Throwable var2) {
            return null;
         }
      }
   }

   private boolean saveNpcWardrobeNowInternal(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null && rec.worldObj != null && rec.refObj instanceof Ref) {
            if (this.executeWardrobeSaveNow(rec, ownerId)) {
               return true;
            } else {
               CountDownLatch latch = new CountDownLatch(1);
               boolean[] result = new boolean[]{false};
               boolean queued = HytaleBridge.worldExecute(rec.worldObj, () -> {
                  try {
                     result[0] = this.executeWardrobeSaveNow(rec, ownerId);
                  } finally {
                     latch.countDown();
                  }

               });
               if (!queued) {
                  return false;
               } else {
                  try {
                     latch.await(400L, TimeUnit.MILLISECONDS);
                  } catch (InterruptedException var7) {
                     Thread.currentThread().interrupt();
                  }

                  return result[0];
               }
            }
         } else {
            return false;
         }
      }
   }

   private boolean executeWardrobeSaveNow(NpcRecord rec, UUID ownerId) {
      if (rec != null && ownerId != null && rec.worldObj != null) {
         Object componentStore = rec.refObj;
         if (componentStore instanceof Ref) {
            Ref<?> npcRefRaw = (Ref)componentStore;

            try {
               componentStore = getComponentStoreFromWorld(rec.worldObj);
               if (componentStore instanceof Store) {
                  Store<?> storeRaw = (Store)componentStore;
                  return AmigoWardrobePersistence.saveNpcWardrobe(ownerId, storeRaw, npcRefRaw);
               }

               return false;
            } catch (Throwable var8) {
               return false;
            }
         }
      }

      return false;
   }

   private static void armWardrobeRestoreRetry(NpcRecord rec, UUID ownerId) {
      if (rec != null && ownerId != null) {
         boolean hasSaved = false;

         try {
            hasSaved = AmigoWardrobePersistence.hasSavedCosmetics(ownerId);
         } catch (Throwable var4) {
         }

         rec.wardrobeRestorePending = hasSaved;
         rec.wardrobeRestoreAttempts = 0;
         rec.nextWardrobeRestoreMillis = hasSaved ? 0L : Long.MAX_VALUE;
      }
   }

   private static void clearWardrobeRestoreRetry(NpcRecord rec) {
      if (rec != null) {
         rec.wardrobeRestorePending = false;
         rec.wardrobeRestoreAttempts = 0;
         rec.nextWardrobeRestoreMillis = Long.MAX_VALUE;
      }
   }

   private static void tickWardrobeRestore(Store store, NpcRecord rec, Object ownerRefObj, long now) {
      if (store != null && rec != null && rec.wardrobeRestorePending) {
         Object var7 = rec.refObj;
         if (var7 instanceof Ref) {
            Ref<?> npcRefRaw = (Ref)var7;
            if (ownerRefObj instanceof Ref) {
               Ref<?> ownerRefRaw = (Ref)ownerRefObj;
               if (rec.state == AmigoNpcManager.State.ACTIVE && !rec.downed) {
                  if (rec.nextWardrobeRestoreMillis > now) {
                     return;
                  }

                  boolean restored = false;

                  try {
                     restored = AmigoWardrobePersistence.restoreNpcWardrobe(rec.ownerId, store, ownerRefRaw, npcRefRaw);
                  } catch (Throwable var10) {
                  }

                  ++rec.wardrobeRestoreAttempts;
                  if (restored) {
                     clearWardrobeRestoreRetry(rec);
                     return;
                  }

                  if (rec.wardrobeRestoreAttempts >= 12) {
                     clearWardrobeRestoreRetry(rec);
                     return;
                  }

                  rec.nextWardrobeRestoreMillis = now + 250L;
                  return;
               }

               return;
            }
         }

      }
   }

   private static long coerceStage1XpGainToLong(NpcRecord rec, double xpGainDouble) {
      if (rec == null) {
         return 0L;
      } else if (xpGainDouble > (double)0.0F && !Double.isNaN(xpGainDouble) && !Double.isInfinite(xpGainDouble)) {
         double rem = rec.stage1XpRemainder;
         if (Double.isNaN(rem) || Double.isInfinite(rem) || rem < (double)0.0F) {
            rem = (double)0.0F;
         }

         double sum = xpGainDouble + rem;
         long whole = (long)Math.floor(sum);
         double newRem = sum - (double)whole;
         if (!(newRem >= (double)0.0F) || Double.isNaN(newRem) || Double.isInfinite(newRem)) {
            newRem = (double)0.0F;
         }

         rec.stage1XpRemainder = newRem;
         return Math.max(0L, whole);
      } else {
         return 0L;
      }
   }

   public String getLastError() {
      return LAST_ERROR;
   }

   private static void setError(String msg) {
      LAST_ERROR = msg;
   }

   public boolean isPvpEnabled() {
      return this.pvpEnabled;
   }

   public void setPvpEnabled(boolean enabled) {
      this.pvpEnabled = enabled;

      try {
         AmigoPersistence.savePvpEnabledGlobal(enabled);
      } catch (Throwable var3) {
      }

   }

   public boolean isDefendeEnabled(UUID owner) {
      if (owner == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(owner);
         return rec != null ? rec.defendeEnabled : AmigoPersistence.loadDefenderEnabled(owner);
      }
   }

   public void setDefendeEnabled(UUID owner, boolean enabled) {
      if (owner != null) {
         AmigoPersistence.saveDefenderEnabled(owner, enabled);
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(owner);
         if (rec != null) {
            rec.defendeEnabled = enabled;
            if (!enabled) {
               this.clearAssist(rec);
            }
         }

      }
   }

   public boolean isAutoLootEnabled(UUID ownerId) {
      if (ownerId == null) {
         return true;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         return rec != null ? rec.autoLootEnabled : AmigoPersistence.loadAutoLootEnabled(ownerId);
      }
   }

   public boolean toggleAutoLoot(UUID ownerId) {
      boolean newVal = !this.isAutoLootEnabled(ownerId);
      this.setAutoLootEnabled(ownerId, newVal);
      return newVal;
   }

   public void setAutoLootEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         AmigoPersistence.saveAutoLootEnabled(ownerId, enabled);
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.autoLootEnabled = enabled;
            if (!enabled) {
               endCombatTaggedLooting(rec);
               rec.lootPausedInventoryFull = false;
               rec.lootChatAcc.clear();
               rec.lootChatSendAtMillis = 0L;
            }
         }

      }
   }

   public String getAutoLootStatus(UUID ownerId) {
      if (ownerId == null) {
         return "AutoLoot: unknown";
      } else {
         boolean enabled = this.isAutoLootEnabled(ownerId);
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         long now = System.currentTimeMillis();
         String state = rec == null ? "NO_NPC" : String.valueOf(rec.state);
         boolean downed = rec != null && rec.downed;
         boolean pausedFull = rec != null && rec.lootPausedInventoryFull;
         int pending = 0;

         try {
            pending = rec != null && rec.pendingLootRefObjs != null ? rec.pendingLootRefObjs.size() : 0;
         } catch (Throwable var16) {
         }

         long stickLeft = 0L;

         try {
            stickLeft = rec != null ? Math.max(0L, rec.autoLootStickUntilMillis - now) : 0L;
         } catch (Throwable var15) {
         }

         int freeSlots = -1;

         try {
            SimpleItemContainer bag = rec != null ? rec.backpack : null;
            if (bag == null && rec != null) {
               bag = this.getOrLoadBackpack(ownerId);
               rec.backpack = bag;
            }

            if (bag != null) {
               freeSlots = countFreeSlots(bag);
            }
         } catch (Throwable var14) {
         }

         return String.format(Locale.ROOT, "AutoLoot=%s | NPC=%s | downed=%s | pausedFull=%s | pending=%d | stick=%dms | freeSlots=%d", enabled ? "ON" : "OFF", state, downed, pausedFull, pending, stickLeft, freeSlots);
      }
   }

   private static int countFreeSlots(SimpleItemContainer bag) {
      if (bag == null) {
         return -1;
      } else {
         short cap;
         try {
            cap = bag.getCapacity();
         } catch (Throwable var6) {
            return -1;
         }

         int free = 0;

         for(short slot = 0; slot < cap; ++slot) {
            ItemStack st;
            try {
               st = bag.getItemStack(slot);
            } catch (Throwable var7) {
               continue;
            }

            if (st == null || st.isEmpty()) {
               ++free;
            }
         }

         return free;
      }
   }

   public boolean isGodMode(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         return rec != null ? rec.godMode : AmigoPersistence.loadGodMode(ownerId);
      }
   }

   public boolean toggleGodModeWithStore(Store store, UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         boolean newVal = !this.isGodMode(ownerId);
         this.setGodModeWithStore(store, ownerId, newVal);
         return true;
      }
   }

   public void setGodModeWithStore(Store store, UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         AmigoPersistence.saveGodMode(ownerId, enabled);
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.godMode = enabled;
            if (enabled && store != null && !rec.downed && rec.refObj instanceof Ref) {
               try {
                  Ref<EntityStore> npcRef = (Ref)rec.refObj;
                  EntityStatMap stats = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
                  if (stats != null) {
                     int healthIdx = DefaultEntityStatTypes.getHealth();

                     try {
                        float max = stats.get(healthIdx).getMax();
                        stats.setStatValue(healthIdx, max);
                     } catch (Throwable var9) {
                        stats.maximizeStatValue(healthIdx);
                     }

                     store.putComponent(npcRef, EntityStatMap.getComponentType(), stats);
                  }
               } catch (Throwable var10) {
               }
            }

            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
         }

      }
   }

   public void notifyNpcDamaged(UUID ownerId, long now) {
      if (ownerId != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.regenWasInCombat = true;
            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
         }
      }
   }

   private static void endCombatTaggedLooting(NpcRecord rec) {
      if (rec != null) {
         rec.lootingActive = false;
         rec.lootTargetRefObj = null;
         rec.lootTargetSinceMillis = 0L;
         if (rec.pendingLootRefObjs != null) {
            rec.pendingLootRefObjs.clear();
         }

         if (rec.lootProcessedUntil != null) {
            rec.lootProcessedUntil.clear();
         }

         if (rec.combatTags != null) {
            rec.combatTags.clear();
         }

         rec.lastBattleCenter = null;
         rec.lastCombatEndMillis = 0L;
         rec.lootPausedInventoryFull = false;
         rec.nextAutoLootMillis = 0L;
         rec.autoLootStickUntilMillis = 0L;
         rec.autoLootStickDeadRefObj = null;
         rec.autoLootStickAnchorPos = null;
      }
   }

   private void tickSpawnFollowFx(Store store, NpcRecord rec, Object ownerRefObj, long now) {
      if (store != null && rec != null) {
         if (!rec.downed) {
            if (rec.refObj instanceof Ref) {
               long until = rec.spawnFxUntilMillis;
               if (until > 0L) {
                  if (now >= until) {
                     rec.spawnFxUntilMillis = 0L;
                     rec.spawnFxNextMillis = 0L;
                  } else {
                     long next = rec.spawnFxNextMillis;
                     if (next <= 0L || now >= next) {
                        rec.spawnFxNextMillis = now + 180L;

                        try {
                           Ref<EntityStore> npcRef = (Ref)rec.refObj;
                           TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                           if (tc == null || tc.getPosition() == null) {
                              return;
                           }

                           spawnParticleToOwner(store, ownerRefObj, tc.getPosition(), "PlayerSpawn_Spawn");
                        } catch (Throwable var12) {
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private void tickAutoRegen(Store store, NpcRecord rec, Object ownerRefObj, long now) {
      if (store != null && rec != null) {
         if (!rec.godMode) {
            if (!rec.downed) {
               if (rec.refObj instanceof Ref) {
                  boolean inCombatNow = now < rec.combatUntilMillis || now < rec.assistUntilMillis || now < rec.npcCombatUntilMillis || rec.combatTargetRefObj != null || rec.assistTargetRefObj != null || rec.npcCombatTargetRefObj != null;
                  if (inCombatNow) {
                     rec.regenStartAtMillis = 0L;
                     rec.regenLastApplyMillis = 0L;
                  } else if (rec.regenStartAtMillis > 0L) {
                     if (now >= rec.regenStartAtMillis) {
                        long last = rec.regenLastApplyMillis;
                        long dt = last <= 0L ? 200L : now - last;
                        if (dt <= 0L) {
                           dt = 1L;
                        }

                        if (dt > 2000L) {
                           dt = 2000L;
                        }

                        rec.regenLastApplyMillis = now;

                        try {
                           Ref<EntityStore> npcRef = (Ref)rec.refObj;
                           EntityStatMap stats = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
                           if (stats == null) {
                              return;
                           }

                           int healthIdx = DefaultEntityStatTypes.getHealth();
                           float cur = stats.get(healthIdx).get();
                           float max = stats.get(healthIdx).getMax();
                           if (cur >= max) {
                              rec.regenStartAtMillis = 0L;
                              rec.regenLastApplyMillis = 0L;
                              return;
                           }

                           float add = max * 0.12F * ((float)dt / 1000.0F);
                           if (add < 1.0F) {
                              add = 1.0F;
                           }

                           float next = cur + add;
                           if (next >= max) {
                              next = max;
                              rec.regenStartAtMillis = 0L;
                              rec.regenLastApplyMillis = 0L;
                           }

                           stats.setStatValue(healthIdx, next);
                           store.putComponent(npcRef, EntityStatMap.getComponentType(), stats);

                           try {
                              TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                              if (tc != null && tc.getPosition() != null) {
                                 spawnParticleToOwner(store, ownerRefObj, tc.getPosition(), "BEAM_HEAL_GREEN_OLD");
                              }
                           } catch (Throwable var19) {
                           }
                        } catch (Throwable var20) {
                        }

                     }
                  }
               }
            }
         }
      }
   }

   public boolean isDebugLogEnabled(UUID owner) {
      if (owner == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(owner);
         if (rec != null) {
            return rec.debugLogEnabled;
         } else {
            Boolean v = (Boolean)this.debugLogByOwner.get(owner);
            return v != null && v;
         }
      }
   }

   public boolean toggleDebugLog(UUID owner) {
      if (owner == null) {
         return false;
      } else {
         boolean enabled = !this.isDebugLogEnabled(owner);
         this.setDebugLogEnabled(owner, enabled);
         return enabled;
      }
   }

   public void setDebugLogEnabled(UUID owner, boolean enabled) {
      if (owner != null) {
         this.debugLogByOwner.put(owner, enabled);
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(owner);
         if (rec != null) {
            rec.debugLogEnabled = enabled;
         }

      }
   }

   private static String choosePreferredNpcType(Object npcPlugin) {
      String[] FORCED_PASSIVE = new String[]{"citizen", "villager", "merchant", "trader", "worker", "farmer"};
      Object rolesObj = invokeOneArg(npcPlugin, "getRoleTemplateNames", Boolean.TYPE, true);
      List<String> roles = toStringList(rolesObj);

      for(String want : FORCED_PASSIVE) {
         for(String r : roles) {
            if (r != null && r.equalsIgnoreCase(want)) {
               return r;
            }
         }
      }

      for(String want : FORCED_PASSIVE) {
         for(String r : roles) {
            if (r != null && r.toLowerCase().contains(want)) {
               return r;
            }
         }
      }

      String pick = pickHumanLike(roles);
      if (pick != null) {
         return pick;
      } else if (!roles.isEmpty()) {
         return (String)roles.get(0);
      } else {
         return firstStringFromArray(invokeNoArg(npcPlugin, "getPresetCoverageTestNPCs"));
      }
   }

   private static List toStringList(Object listObj) {
      if (listObj instanceof List list) {
         ArrayList<String> out = new ArrayList();

         for(Object v : list) {
            if (v != null) {
               out.add(String.valueOf(v));
            }
         }

         return out;
      } else {
         return List.of();
      }
   }

   private static String pickHumanLike(List candidates) {
      if (candidates != null && !candidates.isEmpty()) {
         String[] deny = new String[]{"crow", "raven", "bird", "avian", "bat", "eagle", "hawk", "falcon", "vulture", "owl", "pigeon", "duck", "seagull", "test"};
         String[] prefer = new String[]{"citizen", "human", "villager", "guard", "soldier", "merchant", "trader", "worker", "farmer", "npc"};

         for(String p : prefer) {
            for(String s : candidates) {
               String low = s.toLowerCase();
               if (low.contains(p) && !containsAny(low, deny)) {
                  return s;
               }
            }
         }

         for(String s : candidates) {
            String low = s.toLowerCase();
            if (!containsAny(low, deny)) {
               return s;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean containsAny(String low, String[] needles) {
      for(String n : needles) {
         if (low.contains(n)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasNpc(UUID ownerId) {
      return ownerId != null && this.npcRefPorPlayer.containsKey(ownerId);
   }

   public boolean isNpcActive(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         return rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.refObj instanceof Ref && rec.worldObj != null && !rec.downed;
      }
   }

   public void scheduleAutoLootSecondScan(UUID ownerId, Vector3d anchorPos, Object deadRefObjOrNull) {
      if (ownerId != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (rec.state == AmigoNpcManager.State.ACTIVE && !rec.downed) {
               if (rec.autoLootEnabled) {
                  long now = System.currentTimeMillis();
                  rec.autoLootStickUntilMillis = Math.max(rec.autoLootStickUntilMillis, now + 900L);
                  if (anchorPos != null) {
                     rec.autoLootStickAnchorPos = anchorPos;
                  }

                  if (deadRefObjOrNull != null) {
                     rec.autoLootStickDeadRefObj = deadRefObjOrNull;
                  }

               }
            }
         }
      }
   }

   public boolean spawn(Object worldObj, UUID ownerId) {
      return this.spawn(worldObj, ownerId, (Object)null);
   }

   public boolean spawn(Object worldObj, UUID ownerId, Object senderObj) {
      if (worldObj != null && ownerId != null) {
         NpcRecord existing = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (existing != null) {
            if (existing.state != AmigoNpcManager.State.SPAWNING && existing.state != AmigoNpcManager.State.ACTIVE) {
               setError("NPC está em processo de remoção. Tente novamente em instantes.");
               return false;
            } else {
               setError("NPC já existe para este player (evitando duplicação).");
               return false;
            }
         } else {
            NpcRecord rec = new NpcRecord(worldObj, ownerId, (Object)null, AmigoNpcManager.State.SPAWNING);

            try {
               Boolean dbg = (Boolean)this.debugLogByOwner.get(ownerId);
               rec.debugLogEnabled = dbg != null ? dbg : false;
            } catch (Throwable var12) {
            }

            rec.backpack = AmigoPersistence.loadBackpack(ownerId);
            String savedModel = AmigoPersistence.loadModelId(ownerId);
            boolean hasSavedWardrobeCosmetics = hasSavedWardrobeCosmetics(ownerId);
            rec.modelId = savedModel != null && !savedModel.isBlank() ? savedModel : (hasSavedWardrobeCosmetics ? null : "PlayerTestModel_V");
            double savedScale = AmigoPersistence.loadModelScale(ownerId);
            rec.modelScale = savedScale <= (double)0.0F ? (double)1.0F : savedScale;
            rec.level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
            rec.equippedWeaponId = AmigoPersistence.loadEquippedWeaponId(ownerId);
            rec.defendeEnabled = AmigoPersistence.loadDefenderEnabled(ownerId);
            rec.autoLootEnabled = AmigoPersistence.loadAutoLootEnabled(ownerId);
            rec.godMode = AmigoPersistence.loadGodMode(ownerId);
            rec.totalXp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            rec.npcLevelCached = XpProgression.levelFromTotalXp(rec.totalXp);
            int computedSwordLevel = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.npcLevelCached);
            if (computedSwordLevel != rec.level) {
               rec.level = computedSwordLevel;
               String expectedWeapon = br.tones.amigonpc.core.swords.SwordProgression.weaponIdForLevel(rec.level);
               rec.equippedWeaponId = expectedWeapon;
               AmigoPersistence.saveSwordState(ownerId, rec.level, expectedWeapon);
            }

            rec.baseHp = AmigoPersistence.loadBaseHp(ownerId);
            rec.baseDef = AmigoPersistence.loadBaseDef(ownerId);
            this.npcRefPorPlayer.put(ownerId, rec);
            boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
               try {
                  if (this.npcRefPorPlayer.get(ownerId) != rec) {
                     return;
                  }

                  Object componentStore = getComponentStoreFromWorld(worldObj);
                  if (componentStore == null) {
                     setError("Não consegui obter Store via world.getEntityStore().getStore().");
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
                  if (npcPlugin == null) {
                     setError("NPCPlugin.get() não disponível nesta build.");
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object pos = tryGetOwnerPositionFromWorldStore(worldObj, componentStore, ownerId);
                  if (pos == null) {
                     pos = tryGetSenderPosition(senderObj);
                  }

                  pos = coerceToVector3d(pos);
                  if (pos == null) {
                     setError("Não consegui obter posição Vector3d do player.");
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Float yaw = tryGetOwnerYawFromWorldStore(worldObj, componentStore, ownerId);
                  if (yaw == null) {
                     yaw = tryGetSenderYaw(senderObj);
                  }

                  pos = offsetInFrontOfYaw(pos, yaw, (double)4.0F);
                  Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
                  if (rot == null) {
                     rot = newVector3f(0.0F, 0.0F, 0.0F);
                  }

                  Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
                  Object model = buildPreferredSpawnModel(ownerId, componentStore, ownerRef, rec.modelId, (float)rec.modelScale);
                  if (model == null) {
                     setError("ModelAsset não encontrado/ inválido: " + rec.modelId);
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, "Amigo_Follow");
                  if (roleIndex < 0) {
                     setError("Missing NPC role: Amigo_Follow");
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object pair = invokeSpawnEntity(npcPlugin, componentStore, roleIndex, pos, rot, model, ownerRef);
                  if (pair == null) {
                     setError("spawnEntity retornou null (assinatura incompatível). modelId=" + rec.modelId);
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object ref = extractRefFromPair(pair);
                  rec.refObj = ref != null ? ref : pair;
                  if (rec.refObj != null) {
                     this.amigoRefs.put(rec.refObj, ownerId);
                  }

                  if (rec.state == AmigoNpcManager.State.DESPAWNING) {
                     if (doRemoveEntity(componentStore, rec.refObj)) {
                        this.finalizeRemove(ownerId, rec);
                     } else {
                        setError("Spawn OK, mas falha ao remover imediatamente (DESPAWNING).");
                     }

                     return;
                  }

                  try {
                     Store<EntityStore> store = (Store)componentStore;
                     Ref<EntityStore> npcRef = (Ref)rec.refObj;
                     this.applySwordWeaponNow(store, npcRef, ownerId, rec, true);
                     this.applyNpcScaling(store, npcRef, ownerId, rec, true);
                     armWardrobeRestoreRetry(rec, ownerId);

                     try {
                        if (rec.wardrobeRestorePending) {
                           boolean restored = AmigoWardrobePersistence.restoreNpcWardrobe(ownerId, store, (Ref)ownerRef, npcRef);
                           if (restored) {
                              clearWardrobeRestoreRetry(rec);
                           } else {
                              rec.nextWardrobeRestoreMillis = System.currentTimeMillis() + 250L;
                           }
                        }
                     } catch (Throwable var18) {
                     }

                     playNpcSpawnFx(store, npcRef, ownerRef);
                     rec.spawnFxUntilMillis = System.currentTimeMillis() + 1800L;
                     rec.spawnFxNextMillis = 0L;
                  } catch (Throwable var19) {
                  }

                  rec.state = AmigoNpcManager.State.ACTIVE;
               } catch (Throwable t) {
                  String var10000 = t.getClass().getSimpleName();
                  setError("spawn() falhou: " + var10000 + " - " + t.getMessage());
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  if (rec.refObj != null) {
                     this.amigoRefs.remove(rec.refObj);
                  }
               }

            });
            if (!queued) {
               setError("world.execute falhou: " + HytaleBridge.getLastError());
               this.npcRefPorPlayer.remove(ownerId, rec);
               return false;
            } else {
               return true;
            }
         }
      } else {
         setError("worldObj ou ownerId null.");
         return false;
      }
   }

   private void spawnIntoExistingRecord(Object worldObj, Object componentStore, UUID ownerId, Object senderObj, NpcRecord rec) {
      if (worldObj != null && componentStore != null && ownerId != null && rec != null) {
         try {
            Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
            if (npcPlugin == null) {
               npcPlugin = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "INSTANCE");
            }

            if (npcPlugin == null) {
               setError("NPCPlugin.get() não disponível nesta build.");
               return;
            }

            Object pos = tryGetOwnerPositionFromWorldStore(worldObj, componentStore, ownerId);
            if (pos == null) {
               pos = tryGetSenderPosition(senderObj);
            }

            pos = coerceToVector3d(pos);
            if (pos == null) {
               setError("Não consegui obter posição Vector3d do player.");
               return;
            }

            Float yaw = tryGetOwnerYawFromWorldStore(worldObj, componentStore, ownerId);
            if (yaw == null) {
               yaw = tryGetSenderYaw(senderObj);
            }

            pos = offsetInFrontOfYaw(pos, yaw, (double)4.0F);
            Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
            if (rot == null) {
               rot = newVector3f(0.0F, 0.0F, 0.0F);
            }

            Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
            Object model = buildPreferredSpawnModel(ownerId, componentStore, ownerRef, rec.modelId, (float)rec.modelScale);
            if (model == null) {
               setError("ModelAsset não encontrado/ inválido: " + rec.modelId);
               return;
            }

            int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, "Amigo_Follow");
            if (roleIndex < 0) {
               setError("Missing NPC role: Amigo_Follow");
               return;
            }

            Object pair = invokeSpawnEntity(npcPlugin, componentStore, roleIndex, pos, rot, model, ownerRef);
            if (pair == null) {
               setError("spawnEntity retornou null (assinatura incompatível). modelId=" + rec.modelId);
               return;
            }

            Object ref = extractRefFromPair(pair);
            rec.refObj = ref != null ? ref : pair;
            if (rec.refObj != null) {
               this.amigoRefs.put(rec.refObj, ownerId);
            }

            try {
               Store<EntityStore> store = (Store)componentStore;
               Ref<EntityStore> npcRef = (Ref)rec.refObj;
               this.applySwordWeaponNow(store, npcRef, ownerId, rec, true);
               this.applyNpcScaling(store, npcRef, ownerId, rec, true);
               playNpcSpawnFx(store, npcRef, ownerRef);
               rec.spawnFxUntilMillis = System.currentTimeMillis() + 1800L;
               rec.spawnFxNextMillis = 0L;
               if (rec.godMode) {
                  try {
                     EntityStatMap stats = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
                     if (stats != null) {
                        int healthIdx = DefaultEntityStatTypes.getHealth();

                        try {
                           float max = stats.get(healthIdx).getMax();
                           stats.setStatValue(healthIdx, max);
                        } catch (Throwable var20) {
                           stats.maximizeStatValue(healthIdx);
                        }

                        store.putComponent(npcRef, EntityStatMap.getComponentType(), stats);
                     }
                  } catch (Throwable var21) {
                  }
               }
            } catch (Throwable var22) {
            }

            rec.downed = false;
            rec.downedUntilMillis = 0L;
            rec.deathDespawnAtMillis = 0L;
            rec.regenWasInCombat = false;
            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
            rec.state = AmigoNpcManager.State.ACTIVE;
         } catch (Throwable var23) {
         }

      }
   }

   public boolean spawnWithStore(Object worldObj, Store store, Ref playerEntityRef, UUID ownerId, Object senderObj) {
      if (worldObj != null && store != null && playerEntityRef != null && ownerId != null) {
         NpcRecord existing = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (existing != null) {
            if (existing.state != AmigoNpcManager.State.SPAWNING && existing.state != AmigoNpcManager.State.ACTIVE) {
               setError("NPC está em processo de remoção. Tente novamente em instantes.");
               return false;
            } else {
               setError("NPC já existe para este player (evitando duplicação).");
               return false;
            }
         } else {
            NpcRecord rec = new NpcRecord(worldObj, ownerId, (Object)null, AmigoNpcManager.State.SPAWNING);

            try {
               Boolean dbg = (Boolean)this.debugLogByOwner.get(ownerId);
               rec.debugLogEnabled = dbg != null ? dbg : false;
            } catch (Throwable var25) {
            }

            rec.backpack = AmigoPersistence.loadBackpack(ownerId);
            String savedModel = AmigoPersistence.loadModelId(ownerId);
            boolean hasSavedWardrobeCosmetics = hasSavedWardrobeCosmetics(ownerId);
            rec.modelId = savedModel != null && !savedModel.isBlank() ? savedModel : (hasSavedWardrobeCosmetics ? null : "PlayerTestModel_V");
            double savedScale = AmigoPersistence.loadModelScale(ownerId);
            rec.modelScale = savedScale <= (double)0.0F ? (double)1.0F : savedScale;
            rec.level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
            rec.equippedWeaponId = AmigoPersistence.loadEquippedWeaponId(ownerId);
            rec.defendeEnabled = AmigoPersistence.loadDefenderEnabled(ownerId);
            rec.autoLootEnabled = AmigoPersistence.loadAutoLootEnabled(ownerId);
            rec.godMode = AmigoPersistence.loadGodMode(ownerId);
            rec.totalXp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            rec.npcLevelCached = XpProgression.levelFromTotalXp(rec.totalXp);
            int computedSwordLevel = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.npcLevelCached);
            if (computedSwordLevel != rec.level) {
               rec.level = computedSwordLevel;
               String expectedWeapon = br.tones.amigonpc.core.swords.SwordProgression.weaponIdForLevel(rec.level);
               rec.equippedWeaponId = expectedWeapon;
               AmigoPersistence.saveSwordState(ownerId, rec.level, expectedWeapon);
            }

            rec.baseHp = AmigoPersistence.loadBaseHp(ownerId);
            rec.baseDef = AmigoPersistence.loadBaseDef(ownerId);
            this.npcRefPorPlayer.put(ownerId, rec);

            try {
               Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
               if (npcPlugin == null) {
                  setError("NPCPlugin.get() não disponível nesta build.");
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  return false;
               } else {
                  Object pos = tryGetPositionFromStore(store, playerEntityRef);
                  if (pos == null) {
                     pos = tryGetSenderPosition(senderObj);
                  }

                  pos = coerceToVector3d(pos);
                  if (pos == null) {
                     setError("Não consegui obter posição Vector3d do player.");
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return false;
                  } else {
                     Float yaw = null;

                     try {
                        TransformComponent pt = (TransformComponent)store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                        if (pt != null) {
                           yaw = extractYawFromRotation(pt.getRotation());
                        }
                     } catch (Throwable var24) {
                     }

                     if (yaw == null) {
                        yaw = tryGetSenderYaw(senderObj);
                     }

                     pos = offsetInFrontOfYaw(pos, yaw, (double)4.0F);
                     Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
                     if (rot == null) {
                        rot = newVector3f(0.0F, 0.0F, 0.0F);
                     }

                     Object model = buildPreferredSpawnModel(ownerId, store, playerEntityRef, rec.modelId, (float)rec.modelScale);
                     if (model == null) {
                        setError("ModelAsset não encontrado/ inválido: " + rec.modelId);
                        this.npcRefPorPlayer.remove(ownerId, rec);
                        return false;
                     } else {
                        int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, "Amigo_Follow");
                        if (roleIndex < 0) {
                           setError("Missing NPC role: Amigo_Follow");
                           this.npcRefPorPlayer.remove(ownerId, rec);
                           return false;
                        } else {
                           Object pair = invokeSpawnEntity(npcPlugin, store, roleIndex, pos, rot, model, playerEntityRef);
                           if (pair == null) {
                              setError("spawnEntity retornou null (assinatura incompatível). modelId=" + rec.modelId);
                              this.npcRefPorPlayer.remove(ownerId, rec);
                              return false;
                           } else {
                              Object ref = extractRefFromPair(pair);
                              rec.refObj = ref != null ? ref : pair;
                              if (rec.refObj != null) {
                                 this.amigoRefs.put(rec.refObj, ownerId);
                              }

                              if (rec.state == AmigoNpcManager.State.DESPAWNING) {
                                 if (doRemoveEntity(store, rec.refObj)) {
                                    this.finalizeRemove(ownerId, rec);
                                 } else {
                                    setError("Spawn OK, mas falha ao remover imediatamente (DESPAWNING).");
                                 }

                                 return false;
                              } else {
                                 try {
                                    this.applySwordWeaponNow(store, (Ref)rec.refObj, ownerId, rec, true);
                                    this.applyNpcScaling(store, (Ref)rec.refObj, ownerId, rec, true);
                                    armWardrobeRestoreRetry(rec, ownerId);

                                    try {
                                       if (rec.wardrobeRestorePending) {
                                          boolean restored = AmigoWardrobePersistence.restoreNpcWardrobe(ownerId, store, playerEntityRef, (Ref)rec.refObj);
                                          if (restored) {
                                             clearWardrobeRestoreRetry(rec);
                                          } else {
                                             rec.nextWardrobeRestoreMillis = System.currentTimeMillis() + 250L;
                                          }
                                       }
                                    } catch (Throwable var22) {
                                    }

                                    playNpcSpawnFx(store, (Ref)rec.refObj, playerEntityRef);
                                    rec.spawnFxUntilMillis = System.currentTimeMillis() + 1800L;
                                    rec.spawnFxNextMillis = 0L;
                                 } catch (Throwable var23) {
                                 }

                                 rec.state = AmigoNpcManager.State.ACTIVE;
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            } catch (Throwable t) {
               String var10000 = t.getClass().getSimpleName();
               setError("spawnWithStore() falhou: " + var10000 + " - " + t.getMessage());
               this.npcRefPorPlayer.remove(ownerId, rec);
               if (rec.refObj != null) {
                  this.amigoRefs.remove(rec.refObj);
               }

               return false;
            }
         }
      } else {
         setError("world/store/playerEntityRef/ownerId inválidos.");
         return false;
      }
   }

   public boolean despawnWithStore(Store store, UUID ownerId) {
      if (store != null && ownerId != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            setError("Nenhum NPC registrado para este player.");
            return false;
         } else {
            long now = System.currentTimeMillis();
            if (rec.downed) {
               try {
                  if (rec.refObj != null) {
                     doRemoveEntity(store, rec.refObj);
                     this.amigoRefs.remove(rec.refObj);
                     rec.refObj = null;
                  }
               } catch (Throwable var7) {
               }

               return true;
            } else {
               clearWardrobeRestoreRetry(rec);
               rec.state = AmigoNpcManager.State.DESPAWNING;

               try {
                  try {
                     if (rec.refObj instanceof Ref) {
                        Ref<EntityStore> npcRef = (Ref)rec.refObj;
                        AmigoWardrobePersistence.saveNpcWardrobe(ownerId, store, npcRef);
                     }
                  } catch (Throwable var9) {
                  }

                  if (rec.backpack != null) {
                     AmigoPersistence.saveBackpack(ownerId, rec.backpack);
                  }

                  try {
                     AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
                  } catch (Throwable var8) {
                  }
               } catch (Throwable var10) {
               }

               if (rec.refObj == null) {
                  return true;
               } else if (!doRemoveEntity(store, rec.refObj)) {
                  return false;
               } else {
                  this.finalizeRemove(ownerId, rec);
                  return true;
               }
            }
         }
      } else {
         setError("store ou ownerId inválidos.");
         return false;
      }
   }

   public boolean requestRespawn(Object worldObj, UUID ownerId, Object senderObj) {
      if (worldObj != null && ownerId != null) {
         long now = System.currentTimeMillis();
         long delay = 1000L + ThreadLocalRandom.current().nextLong(1001L);
         long at = now + delay;
         String msg = "Estou chegando.";
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            this.pendingRespawns.put(ownerId, new PendingRespawn(worldObj, senderObj, at, msg));
            return true;
         } else {
            rec.respawnRequested = true;
            rec.respawnWorldObj = worldObj;
            rec.respawnSenderObj = senderObj;
            rec.respawnAtMillis = at;
            rec.respawnMessage = msg;
            return this.despawn(rec.worldObj != null ? rec.worldObj : worldObj, ownerId);
         }
      } else {
         setError("worldObj ou ownerId inválidos.");
         return false;
      }
   }

   public boolean despawn(Object worldObj, UUID ownerId) {
      if (ownerId == null) {
         setError("ownerId null.");
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            setError("Nenhum NPC registrado para este player.");
            return false;
         } else {
            long now = System.currentTimeMillis();
            if (rec.downed) {
               Object worldToUse = rec.worldObj != null ? rec.worldObj : worldObj;
               if (worldToUse == null) {
                  try {
                     if (rec.refObj != null) {
                        this.amigoRefs.remove(rec.refObj);
                        rec.refObj = null;
                     }
                  } catch (Throwable var8) {
                  }

                  return true;
               } else if (rec.refObj == null) {
                  return true;
               } else {
                  boolean queued = HytaleBridge.worldExecute(worldToUse, () -> {
                     try {
                        Object componentStore = getComponentStoreFromWorld(worldToUse);
                        if (componentStore == null) {
                           return;
                        }

                        try {
                           doRemoveEntity(componentStore, rec.refObj);
                        } catch (Throwable var6) {
                        }

                        try {
                           this.amigoRefs.remove(rec.refObj);
                        } catch (Throwable var5) {
                        }

                        rec.refObj = null;
                     } catch (Throwable var7) {
                     }

                  });
                  return queued;
               }
            } else {
               clearWardrobeRestoreRetry(rec);
               rec.state = AmigoNpcManager.State.DESPAWNING;

               try {
                  if (rec.backpack != null) {
                     AmigoPersistence.saveBackpack(ownerId, rec.backpack);
                  }

                  try {
                     AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
                  } catch (Throwable var9) {
                  }
               } catch (Throwable var10) {
               }

               Object worldToUse = rec.worldObj != null ? rec.worldObj : worldObj;
               if (worldToUse == null) {
                  setError("World indisponível para despawn.");
                  return false;
               } else if (rec.refObj == null) {
                  return true;
               } else {
                  boolean queued = HytaleBridge.worldExecute(worldToUse, () -> {
                     try {
                        Object componentStore = getComponentStoreFromWorld(worldToUse);
                        if (componentStore == null) {
                           setError("Não consegui obter Store via world.getEntityStore().getStore().");
                           return;
                        }

                        try {
                           if (rec.refObj instanceof Ref) {
                              Ref<EntityStore> npcRef = (Ref)rec.refObj;
                              Store<EntityStore> store = (Store)componentStore;
                              AmigoWardrobePersistence.saveNpcWardrobe(ownerId, store, npcRef);
                           }
                        } catch (Throwable var7) {
                        }

                        if (!doRemoveEntity(componentStore, rec.refObj)) {
                           return;
                        }

                        this.finalizeRemove(ownerId, rec);
                     } catch (Throwable t) {
                        String var10000 = t.getClass().getSimpleName();
                        setError("despawn() falhou: " + var10000 + " - " + t.getMessage());
                     }

                  });
                  if (!queued) {
                     setError("world.execute falhou: " + HytaleBridge.getLastError());
                     return false;
                  } else {
                     return true;
                  }
               }
            }
         }
      }
   }

   public boolean despawnStored(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            return false;
         } else {
            return rec.worldObj == null ? false : this.despawn(rec.worldObj, ownerId);
         }
      }
   }

   private void finalizeRemove(UUID ownerId, NpcRecord rec) {
      if (ownerId != null && rec != null) {
         try {
            if (rec.refObj != null) {
               this.amigoRefs.remove(rec.refObj);
            }
         } catch (Throwable var11) {
         }

         boolean doRespawn = rec.respawnRequested && rec.respawnWorldObj != null;
         Object respawnWorld = rec.respawnWorldObj;
         Object respawnSender = rec.respawnSenderObj;
         rec.respawnRequested = false;
         rec.respawnWorldObj = null;
         rec.respawnSenderObj = null;
         this.npcRefPorPlayer.remove(ownerId, rec);
         if (doRespawn) {
            long now = System.currentTimeMillis();
            long at = rec.respawnAtMillis > 0L ? rec.respawnAtMillis : now + 1000L + ThreadLocalRandom.current().nextLong(1001L);
            String msg = rec.respawnMessage;
            this.pendingRespawns.put(ownerId, new PendingRespawn(respawnWorld, respawnSender, at, msg));
         }

      }
   }

   public SimpleItemContainer getOrLoadBackpack(UUID ownerId) {
      if (ownerId == null) {
         return new SimpleItemContainer((short)45);
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (rec.backpack == null) {
               rec.backpack = AmigoPersistence.loadBackpack(ownerId);
            }

            return rec.backpack;
         } else {
            return AmigoPersistence.loadBackpack(ownerId);
         }
      }
   }

   public boolean isAmigoRef(Object refObj) {
      return refObj != null && this.amigoRefs.containsKey(refObj);
   }

   public UUID getOwnerFromRef(Object refObj) {
      return refObj == null ? null : (UUID)this.amigoRefs.get(refObj);
   }

   public boolean isDowned(UUID ownerId) {
      NpcRecord rec = ownerId == null ? null : (NpcRecord)this.npcRefPorPlayer.get(ownerId);
      return rec != null && rec.downed;
   }

   public void markDowned(UUID ownerId) {
      NpcRecord rec = ownerId == null ? null : (NpcRecord)this.npcRefPorPlayer.get(ownerId);
      if (rec != null) {
         if (!rec.downed) {
            rec.downed = true;
            long now = System.currentTimeMillis();
            rec.downedUntilMillis = now + 40000L;

            try {
               ActionTraceService.getShared().record(ownerId, "npc_state", "downed until=" + rec.downedUntilMillis);
            } catch (Throwable var12) {
            }

            rec.deathDespawnAtMillis = now + 1200L;
            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
            rec.assistUntilMillis = 0L;
            rec.assistTargetRefObj = null;
            rec.chaseDisengaged = false;
            rec.targetLostSinceMillis = 0L;
            rec.targetStuckSinceMillis = 0L;
            rec.lastTargetHorizontal = (double)-1.0F;
            rec.lastTargetSampleMillis = 0L;

            try {
               if (rec.worldObj != null && rec.refObj instanceof Ref) {
                  Object worldObj = rec.worldObj;
                  HytaleBridge.worldExecute(worldObj, () -> {
                     try {
                        Object storeObj = getComponentStoreFromWorld(worldObj);
                        if (storeObj == null) {
                           return;
                        }

                        Store<EntityStore> store = (Store)storeObj;
                        Ref<EntityStore> npcRef = (Ref)rec.refObj;
                        Object npcEntityObj = getComponentFromStore(store, npcRef, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                           setMarkedTargetOnNpcEntity(npcEntityObj, "LockedTarget", (Object)null);
                           setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", (Object)null);
                           setRoleStateOnNpcEntity(npcEntityObj, npcRef, store, false);
                        }

                        try {
                           MovementStatesComponent ms = (MovementStatesComponent)store.getComponent(npcRef, MovementStatesComponent.getComponentType());
                           if (ms != null) {
                              MovementStates s = ms.getMovementStates();
                              if (s == null) {
                                 s = new MovementStates();
                              }

                              s.onGround = true;
                              s.idle = true;
                              s.horizontalIdle = true;
                              s.walking = false;
                              s.running = false;
                              s.sprinting = false;
                              ms.setMovementStates(s);
                              ms.setSentMovementStates(new MovementStates(s));
                              store.putComponent(npcRef, MovementStatesComponent.getComponentType(), ms);
                           }
                        } catch (Throwable var8) {
                        }
                     } catch (Throwable var9) {
                     }

                  });
               }
            } catch (Throwable var11) {
            }

            try {
               long before = rec.totalXp;
               long after = XpProgression.applyDeathPenalty(before);
               rec.totalXp = after;
               rec.npcLevelCached = XpProgression.levelFromTotalXp(after);
               AmigoPersistence.saveTotalXp(ownerId, after);
            } catch (Throwable var10) {
            }

            try {
               if (rec.state == AmigoNpcManager.State.ACTIVE && rec.worldObj != null && rec.refObj instanceof Ref) {
                  Object worldObj = rec.worldObj;
                  HytaleBridge.worldExecute(worldObj, () -> {
                     try {
                        Object storeObj = getComponentStoreFromWorld(worldObj);
                        if (storeObj == null) {
                           return;
                        }

                        Store<EntityStore> store = (Store)storeObj;
                        Ref<EntityStore> npcRef = (Ref)rec.refObj;
                        updateNpcHud(store, npcRef, rec);
                     } catch (Throwable var5) {
                     }

                  });
               }
            } catch (Throwable var9) {
            }

         }
      }
   }

   public LevelProgressSnapshot getLevelProgressSnapshot(UUID ownerId) {
      if (ownerId == null) {
         return new LevelProgressSnapshot(1, 0L, 0L);
      } else {
         try {
            XpProgression.init();
            NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
            long totalXp = rec != null ? Math.max(0L, rec.totalXp) : Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            int level = Math.max(1, XpProgression.levelFromTotalXp(totalXp));
            long start = XpProgression.xpStartOfLevel(level);
            long need = Math.max(0L, XpProgression.xpToNext(level));
            long into = start == Long.MAX_VALUE ? 0L : Math.max(0L, totalXp - start);
            if (need > 0L && into > need) {
               into = need;
            }

            return new LevelProgressSnapshot(level, into, need);
         } catch (Throwable var12) {
            return new LevelProgressSnapshot(1, 0L, 0L);
         }
      }
   }

   public Color getZoneHudColor(UUID ownerId) {
      if (ownerId == null) {
         return new Color((byte)-1, (byte)-1, (byte)-1);
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         return rec != null && rec.zoneHudColor != null ? rec.zoneHudColor : new Color((byte)-1, (byte)-1, (byte)-1);
      }
   }

   public int getZoneForHud(UUID ownerId) {
      if (ownerId == null) {
         return 0;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            return 0;
         } else {
            int raw = rec.zoneRawId;
            if (raw > 0) {
               return raw;
            } else {
               return rec.zoneInferredId > 0 ? rec.zoneInferredId : ZoneModel.expectedZoneForLevel(Math.max(1, rec.npcLevelCached));
            }
         }
      }
   }

   public String getZoneNameForHud(UUID ownerId) {
      int zid = this.getZoneForHud(ownerId);
      return zid <= 0 ? "" : ZoneModel.zoneName(zid);
   }

   public int getNpcLevel(UUID ownerId) {
      if (ownerId == null) {
         return 1;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            return Math.max(1, rec.npcLevelCached);
         } else {
            try {
               long totalXp = AmigoPersistence.loadTotalXp(ownerId);
               return XpProgression.levelFromTotalXp(totalXp);
            } catch (Throwable var5) {
               return 1;
            }
         }
      }
   }

   public long getNpcTotalXp(UUID ownerId) {
      if (ownerId == null) {
         return 0L;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            return Math.max(0L, rec.totalXp);
         } else {
            try {
               return Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            } catch (Throwable var4) {
               return 0L;
            }
         }
      }
   }

   public boolean isInCombat(UUID ownerId, long nowMillis) {
      if (ownerId == null) {
         return false;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            return false;
         } else if (rec.wasInCombat) {
            return true;
         } else {
            long last = rec.lastCombatTagMillis;
            return last > 0L && nowMillis - last < 5000L;
         }
      }
   }

   public boolean addNpcXpViaApi(UUID ownerId, long amount, NpcXpSource source, NpcXpContext ctx) {
      if (ownerId != null && amount > 0L) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.refObj instanceof Ref && rec.worldObj != null) {
            Object worldObj = rec.worldObj;
            HytaleBridge.worldExecute(worldObj, () -> {
               try {
                  Object storeObj = getComponentStoreFromWorld(worldObj);
                  if (storeObj == null) {
                     return;
                  }

                  Store<EntityStore> store = (Store)storeObj;
                  Ref<EntityStore> npcRef = (Ref)rec.refObj;
                  this.addNpcXp(store, npcRef, ownerId, rec, amount, source, ctx);
               } catch (Throwable var11) {
               }

            });
            return true;
         } else {
            try {
               XpProgression.init();
               long before = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
               long after = before + amount;
               if (after < before) {
                  after = Long.MAX_VALUE;
               }

               AmigoPersistence.saveTotalXp(ownerId, after);
               int oldLevel = Math.max(1, XpProgression.levelFromTotalXp(before));
               int newLevel = Math.max(1, XpProgression.levelFromTotalXp(after));
               NpcXpSource src = source != null ? source : NpcXpSource.COMMAND;
               NpcXpContext c = ctx != null ? ctx : NpcXpContext.now();
               AmigoEventBus.post(new NpcExperienceGainedEvent(ownerId, amount, before, after, oldLevel, newLevel, src, c));
               if (newLevel > oldLevel) {
                  AmigoEventBus.post(new NpcLevelUpEvent(ownerId, oldLevel, newLevel, after, src, c));
               }

               return true;
            } catch (Throwable var15) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void requestRescale(UUID ownerId) {
      if (ownerId != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (rec.state == AmigoNpcManager.State.ACTIVE) {
               if (rec.refObj instanceof Ref) {
                  Object worldObj = rec.worldObj;
                  if (worldObj != null) {
                     HytaleBridge.worldExecute(worldObj, () -> {
                        try {
                           Object storeObj = getComponentStoreFromWorld(worldObj);
                           if (storeObj == null) {
                              return;
                           }

                           Store<EntityStore> store = (Store)storeObj;
                           Ref<EntityStore> npcRef = (Ref)rec.refObj;
                           this.applyNpcScaling(store, npcRef, ownerId, rec, false);

                           try {
                              updateNpcHud(store, npcRef, rec);
                           } catch (Throwable var8) {
                           }
                        } catch (Throwable var9) {
                        }

                     });
                  }
               }
            }
         }
      }
   }

   public float mitigateIncomingDamage(UUID ownerId, float incomingAmount) {
      if (ownerId == null) {
         return incomingAmount;
      } else {
         float amount = Math.max(0.0F, incomingAmount);

         try {
            NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
            long totalXp = rec != null ? rec.totalXp : AmigoPersistence.loadTotalXp(ownerId);
            int lvl = XpProgression.levelFromTotalXp(totalXp);
            double mult = StatScaling.multiplier(lvl);
            if (mult <= (double)0.0F) {
               return amount;
            } else {
               float afterLevel = (float)((double)amount / mult);
               return AttributeModifierService.getShared().applyIncomingDamageMods(ownerId, afterLevel, lvl);
            }
         } catch (Throwable var11) {
            return amount;
         }
      }
   }

   private static int resolveDefenseStatIndex() {
      Integer cached = DEF_STAT_INDEX;
      if (cached != null) {
         return cached;
      } else {
         synchronized(AmigoNpcManager.class) {
            cached = DEF_STAT_INDEX;
            if (cached != null) {
               return cached;
            } else {
               int idx = -1;
               String[] candidates = new String[]{"getDefense", "getDefence", "getArmor", "getArmour", "getPhysicalDefense", "getPhysicalDefence", "getProtection", "getResistance", "getPhysicalResistance"};

               for(String name : candidates) {
                  try {
                     Method m = DefaultEntityStatTypes.class.getMethod(name);
                     Object out = m.invoke((Object)null);
                     if (out instanceof Integer) {
                        idx = (Integer)out;
                        break;
                     }
                  } catch (Throwable var11) {
                  }
               }

               DEF_STAT_INDEX = idx;
               return idx;
            }
         }
      }
   }

   private void applyNpcScaling(Store store, Ref npcRef, UUID ownerId, NpcRecord rec, boolean healToFull) {
      if (store != null && npcRef != null && ownerId != null && rec != null) {
         try {
            XpProgression.init();
            EntityStatMap stats = (EntityStatMap)store.ensureAndGetComponent(npcRef, EntityStatMap.getComponentType());
            if (stats == null) {
               return;
            }

            int healthIdx = DefaultEntityStatTypes.getHealth();
            boolean updatedBases = false;
            if (rec.baseHp <= 0L) {
               try {
                  stats.maximizeStatValue(healthIdx);
               } catch (Throwable var37) {
               }

               float base = 0.0F;

               try {
                  base = stats.get(healthIdx).get();
               } catch (Throwable var36) {
               }

               long baseHp = Math.max(1L, (long)Math.round(base));
               rec.baseHp = baseHp;
               AmigoPersistence.saveBaseHp(ownerId, baseHp);
               updatedBases = true;
            }

            if (rec.baseDef < 0L) {
               long baseDef = 1L;
               int defIdx = resolveDefenseStatIndex();
               if (defIdx >= 0) {
                  try {
                     stats.maximizeStatValue(defIdx);
                     float defVal = stats.get(defIdx).get();
                     baseDef = Math.max(0L, (long)Math.round(defVal));
                  } catch (Throwable var35) {
                     baseDef = 1L;
                  }
               }

               rec.baseDef = baseDef;
               AmigoPersistence.saveBaseDef(ownerId, baseDef);
               updatedBases = true;
            }

            int level = XpProgression.levelFromTotalXp(rec.totalXp);
            rec.npcLevelCached = Math.max(1, level);
            long desiredMaxHp = StatScaling.scaledHp(rec.baseHp, level);

            try {
               if (NpcStatsConfigService.getShared().get().EnableNpcStats) {
                  NpcStatsState st = NpcStatsService.getShared().load(ownerId);
                  long extra = AttributeModifierService.getShared().extraMaxHealth(st);
                  if (extra != 0L) {
                     desiredMaxHp = Math.max(1L, desiredMaxHp + extra);
                  }
               }
            } catch (Throwable var34) {
            }

            long defValScaled = StatScaling.scaledDef(rec.baseDef, level);
            String HP_MAX_MOD_KEY = "amigonpc_hpmax";

            try {
               stats.removeModifier(healthIdx, "amigonpc_hpmax");
            } catch (Throwable var33) {
            }

            try {
               stats.update();
            } catch (Throwable var32) {
            }

            long baseMaxHp = 1L;

            try {
               float baseMaxF = stats.get(healthIdx).getMax();
               baseMaxHp = Math.max(1L, (long)Math.round(baseMaxF));
            } catch (Throwable var31) {
               baseMaxHp = 1L;
            }

            long deltaHp = desiredMaxHp - baseMaxHp;
            if (deltaHp != 0L) {
               float amt;
               if (deltaHp > Long.MAX_VALUE) {
                  amt = Float.MAX_VALUE;
               } else if (deltaHp < Long.MIN_VALUE) {
                  amt = -Float.MAX_VALUE;
               } else {
                  amt = (float)deltaHp;
               }

               try {
                  stats.putModifier(healthIdx, "amigonpc_hpmax", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, amt));
               } catch (Throwable var30) {
               }
            }

            try {
               stats.update();
            } catch (Throwable var29) {
            }

            float maxHpNow = 1.0F;
            float curHpNow = 1.0F;

            try {
               EntityStatValue v = stats.get(healthIdx);
               maxHpNow = Math.max(1.0F, v.getMax());
               curHpNow = Math.max(1.0F, v.get());
            } catch (Throwable var28) {
            }

            float newHp;
            if (healToFull) {
               newHp = maxHpNow;
            } else {
               newHp = Math.min(curHpNow, maxHpNow);
               if (newHp <= 0.0F) {
                  newHp = 1.0F;
               }
            }

            try {
               stats.setStatValue(healthIdx, newHp);
            } catch (Throwable var27) {
            }

            int defIdx = resolveDefenseStatIndex();
            if (defIdx >= 0) {
               try {
                  stats.setStatValue(defIdx, (float)defValScaled);
               } catch (Throwable var26) {
               }
            }

            if (updatedBases) {
            }

            try {
               store.putComponent(npcRef, EntityStatMap.getComponentType(), stats);
            } catch (Throwable var25) {
            }

            try {
               updateNpcHud(store, npcRef, rec);
            } catch (Throwable var24) {
            }
         } catch (Throwable var38) {
         }

      }
   }

   private void addNpcXp(Store store, Ref npcRef, UUID ownerId, NpcRecord rec, long gain, NpcXpSource source, NpcXpContext ctx) {
      if (gain > 0L && ownerId != null && rec != null && store != null && npcRef != null) {
         long before = rec.totalXp;
         long after = before + gain;
         if (after < before) {
            after = Long.MAX_VALUE;
         }

         rec.totalXp = after;
         AmigoPersistence.saveTotalXp(ownerId, after);
         int oldLevel = Math.max(1, XpProgression.levelFromTotalXp(before));
         int newLevel = Math.max(1, XpProgression.levelFromTotalXp(after));
         if (newLevel != oldLevel) {
            rec.npcLevelCached = newLevel;
            this.applyNpcScaling(store, npcRef, ownerId, rec, false);
            if (newLevel > oldLevel) {
               long now = System.currentTimeMillis();
               rec.levelUpFxPendingCount += newLevel - oldLevel;
               rec.levelUpFxUntilMillis = now + 1000L;
               rec.levelUpFxNextTickMillis = 0L;
            }
         }

         if (newLevel != rec.level) {
            rec.level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(newLevel);
            this.applySwordWeaponNow(store, npcRef, ownerId, rec, true);
         }

         try {
            updateNpcHud(store, npcRef, rec);
         } catch (Throwable var20) {
         }

         try {
            LevelProgressHudService.getShared().requestImmediate(ownerId);
         } catch (Throwable var19) {
         }

         try {
            AmigoLvlGuiService.getShared().notifyNpcXpChanged(ownerId);
         } catch (Throwable var18) {
         }

         try {
            NpcXpSource src = source != null ? source : NpcXpSource.COMBAT_ASSIST;
            NpcXpContext c = ctx != null ? ctx : NpcXpContext.now();
            AmigoEventBus.post(new NpcExperienceGainedEvent(ownerId, gain, before, after, oldLevel, newLevel, src, c));
            if (newLevel > oldLevel) {
               AmigoEventBus.post(new NpcLevelUpEvent(ownerId, oldLevel, newLevel, after, src, c));
            }
         } catch (Throwable var17) {
         }

      }
   }

   private static void updateNpcHud(Store store, Ref npcRef, NpcRecord rec) {
      if (store != null && npcRef != null && rec != null) {
         XpProgression.init();
         long totalXp = Math.max(0L, rec.totalXp);
         int computedLevel = Math.max(1, XpProgression.levelFromTotalXp(totalXp));
         int level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
         if (level != computedLevel) {
            level = computedLevel;
            rec.level = computedLevel;
         }

         if (rec.npcLevelCached != computedLevel) {
            rec.npcLevelCached = computedLevel;
         }

         long start = XpProgression.xpStartOfLevel(level);
         long need = Math.max(1L, XpProgression.xpToNext(level));
         long into = start == Long.MAX_VALUE ? 0L : Math.max(0L, totalXp - start);
         if (into > need) {
            into = need;
         }

         int segments = 12;
         double p = Math.max((double)0.0F, Math.min((double)1.0F, (double)into / (double)need));
         int filled = (int)Math.round(p * (double)segments);
         if (filled < 0) {
            filled = 0;
         }

         if (filled > segments) {
            filled = segments;
         }

         String var10000 = repeatChar('=', filled);
         String bar = "[" + var10000 + repeatChar('-', segments - filled) + "]";
         String text = "Lv " + level + " " + bar + " " + into + "/" + need;

         try {
            Nameplate np = (Nameplate)store.ensureAndGetComponent(npcRef, Nameplate.getComponentType());
            np.setText(text);
         } catch (Throwable var20) {
         }

      }
   }

   private static String repeatChar(char c, int n) {
      if (n <= 0) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder(n);

         for(int i = 0; i < n; ++i) {
            sb.append(c);
         }

         return sb.toString();
      }
   }

   public int getSwordLevel(UUID ownerId) {
      if (ownerId == null) {
         return 1;
      } else {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            return br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
         } else {
            long xp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            return br.tones.amigonpc.core.swords.SwordProgression.clampLevel(XpProgression.levelFromTotalXp(xp));
         }
      }
   }

   public int changeSwordLevel(UUID ownerId, int delta, boolean announce) {
      if (ownerId != null && delta != 0) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         int oldLvl = rec != null ? br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level) : br.tones.amigonpc.core.swords.SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
         int newLvl = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(oldLvl + delta);
         if (newLvl == oldLvl) {
            return newLvl;
         } else {
            if (rec != null) {
               rec.level = newLvl;
            }

            String weaponId = br.tones.amigonpc.core.swords.SwordProgression.weaponIdForLevel(newLvl);
            if (rec != null) {
               rec.equippedWeaponId = weaponId;
            }

            AmigoPersistence.saveSwordState(ownerId, newLvl, weaponId);
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
                     Object storeObj = getComponentStoreFromWorld(worldObj);
                     if (storeObj == null) {
                        return;
                     }

                     Store<EntityStore> store = (Store)storeObj;
                     Ref<EntityStore> npcRef = (Ref)rec.refObj;
                     this.applySwordWeaponNow(store, npcRef, ownerId, rec, true);

                     try {
                        this.applyNpcScaling(store, npcRef, ownerId, rec, false);
                     } catch (Throwable var9) {
                     }

                     try {
                        updateNpcHud(store, npcRef, rec);
                     } catch (Throwable var8) {
                     }
                  } catch (Throwable var10) {
                  }

               });
               if (announce) {
                  String line;
                  if (delta > 0 && br.tones.amigonpc.core.swords.SwordProgression.crossedAnyMilestone(oldLvl, newLvl)) {
                     line = br.tones.amigonpc.core.swords.SwordMessages.pickEpic(newLvl);
                  } else if (delta > 0) {
                     line = br.tones.amigonpc.core.swords.SwordMessages.pickUp(newLvl);
                  } else {
                     line = br.tones.amigonpc.core.swords.SwordMessages.pickDown(newLvl);
                  }

                  this.sendToOwner(worldObj, ownerId, line);
               }
            }

            return newLvl;
         }
      } else {
         return this.getSwordLevel(ownerId);
      }
   }

   private void sendToOwner(Object worldObj, UUID ownerId, String text) {
      if (worldObj != null && ownerId != null && text != null && !text.isBlank()) {
         HytaleBridge.worldExecute(worldObj, () -> {
            try {
               Object storeObj = getComponentStoreFromWorld(worldObj);
               if (storeObj == null) {
                  return;
               }

               Store<EntityStore> store = (Store)storeObj;
               Ref<EntityStore> ownerRef = (Ref)invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
               if (ownerRef == null) {
                  return;
               }

               Player player = (Player)store.getComponent(ownerRef, Player.getComponentType());
               if (player == null) {
                  return;
               }

               player.sendMessage(Message.raw("§7[AmigoNPC] " + text));
            } catch (Throwable var7) {
            }

         });
      }
   }

   private void applySwordWeaponNow(Store store, Ref npcRef, UUID ownerId, NpcRecord rec, boolean saveNow) {
      if (store != null && npcRef != null && rec != null && ownerId != null) {
         int lvl = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
         String expected = br.tones.amigonpc.core.swords.SwordProgression.weaponIdForLevel(lvl);
         if (expected != null && !expected.isBlank()) {
            boolean slotOk = this.isHotbar0Item(store, npcRef, expected);
            if (expected.equals(rec.equippedWeaponId) && slotOk) {
               if (saveNow) {
                  AmigoPersistence.saveSwordState(ownerId, lvl, expected);
               }

            } else {
               boolean equipped = this.equipWeaponInHotbar0(store, npcRef, expected);
               if (equipped) {
                  rec.equippedWeaponId = expected;
                  if (saveNow) {
                     AmigoPersistence.saveSwordState(ownerId, lvl, expected);
                  }

                  this.debugEquip(rec, ownerId, "Equipado: " + expected + " (lvl " + lvl + ")");
               } else {
                  this.debugEquip(rec, ownerId, "FALHA ao equipar '" + expected + "' (lvl " + lvl + "). Hotbar0=" + this.getHotbar0ItemId(store, npcRef));
               }

            }
         }
      }
   }

   private String getHotbar0ItemId(Store store, Ref npcRef) {
      try {
         NPCEntity npc = (NPCEntity)store.getComponent(npcRef, NPCEntity.getComponentType());
         if (npc == null) {
            return null;
         } else {
            Inventory inv = npc.getInventory();
            if (inv == null) {
               return null;
            } else {
               ItemContainer hotbar = inv.getHotbar();
               if (hotbar == null) {
                  return null;
               } else {
                  ItemStack st = hotbar.getItemStack((short)0);
                  return st != null && !st.isEmpty() ? st.getItemId() : null;
               }
            }
         }
      } catch (Throwable var7) {
         return null;
      }
   }

   private boolean isHotbar0Item(Store store, Ref npcRef, String itemId) {
      if (itemId != null && !itemId.isBlank()) {
         String cur = this.getHotbar0ItemId(store, npcRef);
         return itemId.equals(cur);
      } else {
         return false;
      }
   }

   private boolean equipWeaponInHotbar0(Store store, Ref npcRef, String itemId) {
      try {
         NPCEntity npc = (NPCEntity)store.getComponent(npcRef, NPCEntity.getComponentType());
         if (npc == null) {
            return false;
         } else {
            Inventory inv = npc.getInventory();
            if (inv == null) {
               inv = new Inventory();
            }

            try {
               inv.setEntity(npc);
            } catch (Throwable var11) {
            }

            try {
               inv.setUsingToolsItem(false);
            } catch (Throwable var10) {
            }

            ItemContainer hotbar = inv.getHotbar();
            if (hotbar == null) {
               inv = new Inventory();

               try {
                  inv.setEntity(npc);
               } catch (Throwable var9) {
               }

               try {
                  inv.setUsingToolsItem(false);
               } catch (Throwable var8) {
               }

               hotbar = inv.getHotbar();
               if (hotbar == null) {
                  return false;
               }
            }

            hotbar.setItemStackForSlot((short)0, new ItemStack(itemId, 1));

            try {
               ItemStack st = hotbar.getItemStack((short)0);
               if (st == null || st.isEmpty()) {
                  return false;
               }
            } catch (Throwable var12) {
            }

            inv.setActiveHotbarSlot((byte)0);
            inv.markChanged();
            npc.setInventory(inv);
            npc.invalidateEquipmentNetwork();
            return true;
         }
      } catch (Throwable var13) {
         return false;
      }
   }

   private void debugEquip(NpcRecord rec, UUID ownerId, String msg) {
      if (rec != null && ownerId != null) {
         if (rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextEquipMillis) {
               rec.debugNextEquipMillis = now + 1200L;
               this.sendToOwner(rec.worldObj, ownerId, "DEBUG-EQUIP: " + msg);
            }
         }
      }
   }

   private void debugCombat(NpcRecord rec, UUID ownerId, String msg) {
      if (rec != null && ownerId != null) {
         if (rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextCombatMillis) {
               rec.debugNextCombatMillis = now + 900L;
               this.sendToOwner(rec.worldObj, ownerId, "DEBUG-COMBAT: " + msg);
            }
         }
      }
   }

   private void debugAttack(NpcRecord rec, UUID ownerId, String msg) {
      if (rec != null && ownerId != null) {
         if (rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextAttackMillis) {
               rec.debugNextAttackMillis = now + 900L;
               this.sendToOwner(rec.worldObj, ownerId, "DEBUG-ATTACK: " + msg);
            }
         }
      }
   }

   private void debugZones(NpcRecord rec, UUID ownerId, String msg) {
      if (rec != null && ownerId != null) {
         boolean cfgEnabled = false;

         try {
            cfgEnabled = AmigoZonesConfigService.get().debugLog;
         } catch (Throwable var7) {
         }

         if (cfgEnabled || rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextZonesMillis) {
               rec.debugNextZonesMillis = now + 1000L;
               this.sendToOwner(rec.worldObj, ownerId, "DEBUG-ZONES: " + msg);
            }
         }
      }
   }

   private void debugMobLevel(NpcRecord rec, UUID ownerId, String msg) {
      if (rec != null && ownerId != null) {
         boolean cfgEnabled = false;

         try {
            cfgEnabled = AmigoZonesConfigService.get().debugLog;
         } catch (Throwable var7) {
         }

         if (cfgEnabled || rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextMobLevelMillis) {
               rec.debugNextMobLevelMillis = now + 1000L;
               this.sendToOwner(rec.worldObj, ownerId, "DEBUG-MOBLEVEL: " + msg);
            }
         }
      }
   }

   public void debugDamage(UUID ownerId, String msg) {
      if (ownerId != null && msg != null && !msg.isBlank()) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (rec.debugLogEnabled) {
               long now = System.currentTimeMillis();
               if (now >= rec.debugNextDamageMillis) {
                  rec.debugNextDamageMillis = now + 1200L;
                  this.sendToOwner(rec.worldObj, ownerId, "DEBUG-DAMAGE: " + msg);
               }
            }
         }
      }
   }

   private static double getWeaponBaseDamage(String itemId) {
      if (itemId != null && !itemId.isBlank()) {
         try {
            Item it = (Item)Item.getAssetMap().getAsset(itemId);
            if (it == null) {
               return (double)4.0F;
            } else {
               int lvl = it.getItemLevel();
               return (double)4.0F + (double)Math.max(0, lvl) * 0.9;
            }
         } catch (Throwable var3) {
            return (double)4.0F;
         }
      } else {
         return (double)4.0F;
      }
   }

   private static boolean refEq(Object a, Object b) {
      if (a == b) {
         return true;
      } else if (a != null && b != null) {
         try {
            return a.equals(b);
         } catch (Throwable var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void startCombat(UUID ownerId, Object attackerRefObj) {
      if (ownerId != null && attackerRefObj != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (!rec.downed) {
               if (rec.refObj != null) {
                  if (attackerRefObj != rec.refObj) {
                     rec.combatTargetRefObj = attackerRefObj;
                     rec.combatUntilMillis = System.currentTimeMillis() + 3000L;
                     rec.npcCombatTargetRefObj = null;
                     rec.npcCombatUntilMillis = 0L;
                     rec.assistTargetRefObj = null;
                     rec.assistUntilMillis = 0L;
                     String var10003 = String.valueOf(attackerRefObj);
                     this.debugCombat(rec, ownerId, "startCombat: agressorRef=" + var10003 + " (defender=" + (rec.defendeEnabled ? "ON" : "OFF") + ")");
                  }
               }
            }
         }
      }
   }

   public void startNpcCombat(UUID ownerId, Object attackerRefObj) {
      if (ownerId != null && attackerRefObj != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (!rec.downed) {
               if (rec.refObj != null) {
                  if (attackerRefObj != rec.refObj) {
                     long now = System.currentTimeMillis();
                     if (this.getActiveCombatTarget(rec, now) == null) {
                        rec.npcCombatTargetRefObj = attackerRefObj;
                        rec.npcCombatUntilMillis = now + 3000L;

                        try {
                           ActionTraceService.getShared().record(ownerId, "npc_state", "combat_start source=npc_attacked");
                        } catch (Throwable var7) {
                        }

                        this.debugCombat(rec, ownerId, "startNpcCombat: agressorRef=" + String.valueOf(attackerRefObj));
                     }
                  }
               }
            }
         }
      }
   }

   public void startAssist(UUID ownerId, Object targetRefObj) {
      if (ownerId != null && targetRefObj != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (rec.defendeEnabled) {
               if (!rec.downed) {
                  if (rec.refObj != null) {
                     if (targetRefObj != rec.refObj) {
                        long now = System.currentTimeMillis();
                        if (this.getActiveCombatTarget(rec, now) == null) {
                           if (rec.assistTargetRefObj == null) {
                              rec.assistTargetRefObj = targetRefObj;
                              rec.assistUntilMillis = 0L;

                              try {
                                 ActionTraceService.getShared().record(ownerId, "npc_state", "combat_start source=assist");
                              } catch (Throwable var7) {
                              }

                              this.debugCombat(rec, ownerId, "startAssist: targetRef=" + String.valueOf(targetRefObj));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void recordCombatTag(UUID ownerId, Object targetRefObj, Store store) {
      if (ownerId != null && targetRefObj != null && store != null) {
         NpcRecord rec = (NpcRecord)this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            if (rec.refObj != null) {
               if (!rec.downed) {
                  if (targetRefObj instanceof Ref) {
                     long now = System.currentTimeMillis();
                     rec.lastCombatTagMillis = now;
                     rec.wasInCombat = true;

                     try {
                        Ref<EntityStore> targetRef = (Ref)targetRefObj;
                        TransformComponent tc = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
                        if (tc == null || tc.getPosition() == null) {
                           return;
                        }

                        Vector3d pos = tc.getPosition();
                        rec.lastBattleCenter = pos;
                        synchronized(rec.combatTags) {
                           CombatTag existing = null;

                           for(CombatTag t : rec.combatTags) {
                              if (t != null && refEq(t.targetRefObj, targetRefObj)) {
                                 existing = t;
                                 break;
                              }
                           }

                           if (existing != null) {
                              existing.pos = pos;
                              existing.lastSeenMillis = now;
                           } else {
                              if (rec.combatTags.size() >= 16) {
                                 rec.combatTags.remove(0);
                              }

                              rec.combatTags.add(new CombatTag(targetRefObj, pos, now));
                           }
                        }
                     } catch (Throwable var16) {
                     }

                  }
               }
            }
         }
      }
   }

   private Object getActiveAssistTarget(NpcRecord rec, long now) {
      return rec == null ? null : rec.assistTargetRefObj;
   }

   private void clearAssist(NpcRecord rec) {
      if (rec != null) {
         rec.assistTargetRefObj = null;
         rec.assistUntilMillis = 0L;
      }
   }

   private static boolean isValidEntityRef(Object store, Object refObj) {
      if (store != null && refObj != null) {
         try {
            Object tc = getComponentFromStore(store, refObj, TransformComponent.getComponentType());
            return tc != null;
         } catch (Throwable var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean isAliveEntityRef(Object store, Object refObj) {
      if (!isValidEntityRef(store, refObj)) {
         return false;
      } else {
         try {
            try {
               Object dc = getComponentFromStore(store, refObj, DeathComponent.getComponentType());
               if (dc != null) {
                  return false;
               }
            } catch (Throwable var6) {
            }

            Object statsObj = getComponentFromStore(store, refObj, EntityStatMap.getComponentType());
            if (statsObj instanceof EntityStatMap) {
               EntityStatMap stats = (EntityStatMap)statsObj;

               try {
                  float hp = stats.get(DefaultEntityStatTypes.getHealth()).get();
                  return hp > 0.0F;
               } catch (Throwable var5) {
                  return true;
               }
            } else {
               return true;
            }
         } catch (Throwable var7) {
            return true;
         }
      }
   }

   private static double getEntityHeight(Object store, Object refObj) {
      if (store != null && refObj != null) {
         try {
            Object bbObj = getComponentFromStore(store, refObj, BoundingBox.getComponentType());
            if (bbObj instanceof BoundingBox) {
               BoundingBox bb = (BoundingBox)bbObj;

               try {
                  Box box = bb.getBoundingBox();
                  if (box != null) {
                     double h = box.height();
                     if (h > 0.05) {
                        return h;
                     }
                  }
               } catch (Throwable var7) {
               }
            }
         } catch (Throwable var8) {
         }

         return 1.8;
      } else {
         return 1.8;
      }
   }

   private static double getEntityRadiusXZ(Object store, Object refObj) {
      if (store != null && refObj != null) {
         try {
            Object bbObj = getComponentFromStore(store, refObj, BoundingBox.getComponentType());
            if (bbObj instanceof BoundingBox) {
               BoundingBox bb = (BoundingBox)bbObj;
               Object box = null;

               try {
                  box = bb.getBoundingBox();
               } catch (Throwable var11) {
               }

               if (box != null) {
                  double w = readBoxDim(box, "width", "getWidth", "xSize", "getXSize");
                  double d = readBoxDim(box, "depth", "getDepth", "zSize", "getZSize");
                  if (w > 0.05 && d > 0.05) {
                     double r = (double)0.5F * Math.max(w, d);
                     return clamp(r, 0.2, (double)2.0F);
                  }
               }
            }
         } catch (Throwable var12) {
         }

         return 0.45;
      } else {
         return 0.45;
      }
   }

   private static double readBoxDim(Object box, String... methodNames) {
      if (box != null && methodNames != null) {
         for(String name : methodNames) {
            if (name != null && !name.isBlank()) {
               try {
                  Method m = box.getClass().getMethod(name);
                  Object v = m.invoke(box);
                  if (v instanceof Number) {
                     Number n = (Number)v;
                     double d = n.doubleValue();
                     if (d > (double)0.0F) {
                        return d;
                     }
                  }
               } catch (Throwable var11) {
               }
            }
         }

         return (double)-1.0F;
      } else {
         return (double)-1.0F;
      }
   }

   private static double clamp(double v, double min, double max) {
      return Math.max(min, Math.min(max, v));
   }

   private static void tickAssistHousekeeping(NpcRecord rec, Object store, long now) {
      if (rec != null) {
         if (!rec.defendeEnabled) {
            rec.assistTargetRefObj = null;
            rec.assistUntilMillis = 0L;
         } else if (rec.assistTargetRefObj != null) {
            if (!isAliveEntityRef(store, rec.assistTargetRefObj)) {
               rec.assistTargetRefObj = null;
               rec.assistUntilMillis = now + 3000L;
            }

         } else {
            if (rec.assistUntilMillis > 0L && now > rec.assistUntilMillis) {
               rec.assistUntilMillis = 0L;
            }

         }
      }
   }

   private Object findNearestDefenderTarget(Store store, NpcRecord rec, Object ownerRefObj, Vector3d ownerPos) {
      if (store != null && rec != null && ownerRefObj != null && ownerPos != null) {
         if (ownerRefObj instanceof Ref && rec.refObj instanceof Ref) {
            Ref<EntityStore> ownerRef = (Ref)ownerRefObj;
            Ref<EntityStore> npcRef = (Ref)rec.refObj;
            double r = (double)12.0F;
            double r2 = (double)144.0F;
            double maxDy = (double)2.5F;
            Object[] bestGroundRef = new Object[1];
            double[] bestGroundD2 = new double[]{Double.POSITIVE_INFINITY};
            Object[] bestAirRef = new Object[1];
            double[] bestAirD2 = new double[]{Double.POSITIVE_INFINITY};

            try {
               store.forEachChunk((chunk, cb) -> {
                  int sz;
                  try {
                     sz = chunk.size();
                  } catch (Throwable var27) {
                     return;
                  }

                  for(int i = 0; i < sz; ++i) {
                     Ref<EntityStore> ref;
                     try {
                        ref = chunk.getReferenceTo(i);
                     } catch (Throwable var34) {
                        continue;
                     }

                     if (ref != null && !refEq(ref, ownerRef) && !refEq(ref, npcRef)) {
                        try {
                           Player maybePlayer = (Player)chunk.getComponent(i, Player.getComponentType());
                           if (maybePlayer != null && !this.pvpEnabled) {
                              continue;
                           }
                        } catch (Throwable var33) {
                        }

                        try {
                           DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                           if (dc != null) {
                              continue;
                           }
                        } catch (Throwable var32) {
                        }

                        try {
                           DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                           if (dc != null) {
                              continue;
                           }
                        } catch (Throwable var31) {
                        }

                        TransformComponent tc;
                        try {
                           tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                        } catch (Throwable var30) {
                           continue;
                        }

                        if (tc != null && tc.getPosition() != null) {
                           Vector3d p = tc.getPosition();
                           double dy = Math.abs(p.getY() - ownerPos.getY());
                           if (dy > (double)2.5F) {
                              if (dy > (double)35.0F) {
                                 continue;
                              }

                              try {
                                 MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                                 MovementStates s = ms != null ? ms.getMovementStates() : null;
                                 if (s == null || s.onGround) {
                                    continue;
                                 }
                              } catch (Throwable var29) {
                                 continue;
                              }
                           }

                           double dx = p.getX() - ownerPos.getX();
                           double dz = p.getZ() - ownerPos.getZ();
                           double d2 = dx * dx + dz * dz;
                           if (!(d2 > (double)144.0F)) {
                              try {
                                 EntityStatMap stats = (EntityStatMap)chunk.getComponent(i, EntityStatMap.getComponentType());
                                 if (stats == null) {
                                    continue;
                                 }

                                 EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
                                 if (hp != null && hp.get() <= 0.0F) {
                                    continue;
                                 }
                              } catch (Throwable var28) {
                                 continue;
                              }

                              boolean isAir = false;

                              try {
                                 MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                                 MovementStates s = ms != null ? ms.getMovementStates() : null;
                                 if (s != null) {
                                    isAir = !s.onGround;
                                 }
                              } catch (Throwable var26) {
                              }

                              if (!isAir) {
                                 if (d2 < bestGroundD2[0]) {
                                    bestGroundD2[0] = d2;
                                    bestGroundRef[0] = ref;
                                 }
                              } else if (d2 < bestAirD2[0]) {
                                 bestAirD2[0] = d2;
                                 bestAirRef[0] = ref;
                              }
                           }
                        }
                     }
                  }

               });
            } catch (Throwable var18) {
            }

            return bestGroundRef[0] != null ? bestGroundRef[0] : bestAirRef[0];
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private Object findNearestTargetNearNpc(Store store, NpcRecord rec, Object ownerRefObj, Vector3d npcPos, Object excludeRefObj) {
      if (store != null && rec != null && ownerRefObj != null && npcPos != null) {
         if (ownerRefObj instanceof Ref && rec.refObj instanceof Ref) {
            Ref<EntityStore> ownerRef = (Ref)ownerRefObj;
            Ref<EntityStore> npcRef = (Ref)rec.refObj;
            double r = (double)12.0F;
            double r2 = (double)144.0F;
            double maxDy = (double)2.5F;
            Object[] bestGroundRef = new Object[1];
            double[] bestGroundD2 = new double[]{Double.POSITIVE_INFINITY};
            Object[] bestAirRef = new Object[1];
            double[] bestAirD2 = new double[]{Double.POSITIVE_INFINITY};

            try {
               store.forEachChunk((chunk, cb) -> {
                  int sz;
                  try {
                     sz = chunk.size();
                  } catch (Throwable var28) {
                     return;
                  }

                  for(int i = 0; i < sz; ++i) {
                     Ref<EntityStore> ref;
                     try {
                        ref = chunk.getReferenceTo(i);
                     } catch (Throwable var34) {
                        continue;
                     }

                     if (ref != null && !refEq(ref, ownerRef) && !refEq(ref, npcRef) && (!(excludeRefObj instanceof Ref) || !refEq(ref, (Ref)excludeRefObj))) {
                        try {
                           Player maybePlayer = (Player)chunk.getComponent(i, Player.getComponentType());
                           if (maybePlayer != null && !this.pvpEnabled) {
                              continue;
                           }
                        } catch (Throwable var33) {
                        }

                        try {
                           DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                           if (dc != null) {
                              continue;
                           }
                        } catch (Throwable var32) {
                        }

                        TransformComponent tc;
                        try {
                           tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                        } catch (Throwable var31) {
                           continue;
                        }

                        if (tc != null && tc.getPosition() != null) {
                           Vector3d p = tc.getPosition();
                           double dy = Math.abs(p.getY() - npcPos.getY());
                           if (dy > (double)2.5F) {
                              if (dy > (double)35.0F) {
                                 continue;
                              }

                              try {
                                 MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                                 MovementStates s = ms != null ? ms.getMovementStates() : null;
                                 if (s == null || s.onGround) {
                                    continue;
                                 }
                              } catch (Throwable var30) {
                                 continue;
                              }
                           }

                           double dx = p.getX() - npcPos.getX();
                           double dz = p.getZ() - npcPos.getZ();
                           double d2 = dx * dx + dz * dz;
                           if (!(d2 > (double)144.0F)) {
                              try {
                                 EntityStatMap stats = (EntityStatMap)chunk.getComponent(i, EntityStatMap.getComponentType());
                                 if (stats == null) {
                                    continue;
                                 }

                                 EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
                                 if (hp != null && hp.get() <= 0.0F) {
                                    continue;
                                 }
                              } catch (Throwable var29) {
                                 continue;
                              }

                              boolean isAir = false;

                              try {
                                 MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                                 MovementStates s = ms != null ? ms.getMovementStates() : null;
                                 if (s != null) {
                                    isAir = !s.onGround;
                                 }
                              } catch (Throwable var27) {
                              }

                              if (!isAir) {
                                 if (d2 < bestGroundD2[0]) {
                                    bestGroundD2[0] = d2;
                                    bestGroundRef[0] = ref;
                                 }
                              } else if (d2 < bestAirD2[0]) {
                                 bestAirD2[0] = d2;
                                 bestAirRef[0] = ref;
                              }
                           }
                        }
                     }
                  }

               });
            } catch (Throwable var19) {
            }

            return bestGroundRef[0] != null ? bestGroundRef[0] : bestAirRef[0];
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean isAnyEnemyNearNpc(Store store, NpcRecord rec, Object ownerRefObj, Vector3d npcPos, double radius) {
      if (store != null && rec != null && ownerRefObj != null && npcPos != null) {
         if (ownerRefObj instanceof Ref && rec.refObj instanceof Ref) {
            Ref<EntityStore> ownerRef = (Ref)ownerRefObj;
            Ref<EntityStore> npcRef = (Ref)rec.refObj;
            double r2 = radius * radius;
            double maxDy = (double)2.5F;
            boolean[] found = new boolean[]{false};

            try {
               store.forEachChunk((chunk, cb) -> {
                  if (!found[0]) {
                     int sz;
                     try {
                        sz = chunk.size();
                     } catch (Throwable var23) {
                        return;
                     }

                     int i = 0;

                     while(true) {
                        if (i < sz) {
                           label109: {
                              label116: {
                                 Ref<EntityStore> ref;
                                 try {
                                    ref = chunk.getReferenceTo(i);
                                 } catch (Throwable var29) {
                                    break label116;
                                 }

                                 if (ref != null && !refEq(ref, ownerRef) && !refEq(ref, npcRef)) {
                                    label121: {
                                       try {
                                          Player maybePlayer = (Player)chunk.getComponent(i, Player.getComponentType());
                                          if (maybePlayer != null) {
                                             break label121;
                                          }
                                       } catch (Throwable var28) {
                                       }

                                       try {
                                          DeathComponent dc = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType());
                                          if (dc != null) {
                                             break label121;
                                          }
                                       } catch (Throwable var27) {
                                       }

                                       TransformComponent tc;
                                       try {
                                          tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                                       } catch (Throwable var26) {
                                          break label121;
                                       }

                                       if (tc != null && tc.getPosition() != null) {
                                          label123: {
                                             Vector3d p = tc.getPosition();
                                             double dy = Math.abs(p.getY() - npcPos.getY());
                                             if (dy > (double)2.5F) {
                                                if (dy > (double)35.0F) {
                                                   break label123;
                                                }

                                                try {
                                                   MovementStatesComponent ms = (MovementStatesComponent)chunk.getComponent(i, MovementStatesComponent.getComponentType());
                                                   MovementStates s = ms != null ? ms.getMovementStates() : null;
                                                   if (s == null || s.onGround) {
                                                      break label123;
                                                   }
                                                } catch (Throwable var25) {
                                                   break label123;
                                                }
                                             }

                                             double dx = p.getX() - npcPos.getX();
                                             double dz = p.getZ() - npcPos.getZ();
                                             double d2 = dx * dx + dz * dz;
                                             if (!(d2 > r2)) {
                                                try {
                                                   EntityStatMap stats = (EntityStatMap)chunk.getComponent(i, EntityStatMap.getComponentType());
                                                   if (stats != null) {
                                                      EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
                                                      if (hp == null || !(hp.get() <= 0.0F)) {
                                                         break label109;
                                                      }
                                                   }
                                                } catch (Throwable var24) {
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              ++i;
                              continue;
                           }

                           found[0] = true;
                           return;
                        }

                        return;
                     }
                  }
               });
            } catch (Throwable var14) {
            }

            return found[0];
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static double dist2(Vector3d a, Vector3d b) {
      if (a != null && b != null) {
         double dx = a.getX() - b.getX();
         double dy = a.getY() - b.getY();
         double dz = a.getZ() - b.getZ();
         return dx * dx + dy * dy + dz * dz;
      } else {
         return Double.POSITIVE_INFINITY;
      }
   }

   private void clearCombatTagsAndLoot(NpcRecord rec) {
      if (rec != null) {
         synchronized(rec.combatTags) {
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

   private static boolean isItemRefValid(Store store, Object refObj) {
      if (store != null && refObj != null) {
         if (!(refObj instanceof Ref)) {
            return false;
         } else {
            try {
               Ref<EntityStore> r = (Ref)refObj;
               TransformComponent tc = (TransformComponent)store.getComponent(r, TransformComponent.getComponentType());
               ItemComponent ic = (ItemComponent)store.getComponent(r, ItemComponent.getComponentType());
               return tc != null && tc.getPosition() != null && ic != null;
            } catch (Throwable var5) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static void lootChatAccAdd(NpcRecord rec, String itemId, int qty, long now) {
      if (rec != null && itemId != null && !itemId.isBlank() && qty > 0) {
         try {
            Integer cur = (Integer)rec.lootChatAcc.get(itemId);
            rec.lootChatAcc.put(itemId, (cur == null ? 0 : cur) + qty);
            rec.lootChatSendAtMillis = now + 500L;
         } catch (Throwable var6) {
         }

      }
   }

   private void lootChatAccFlushIfDue(NpcRecord rec, UUID ownerId, Object worldObj, long now) {
      if (rec != null && ownerId != null && worldObj != null) {
         long at = rec.lootChatSendAtMillis;
         if (at > 0L && now >= at) {
            if (rec.lootChatAcc.isEmpty()) {
               rec.lootChatSendAtMillis = 0L;
            } else {
               try {
                  StringBuilder sb = new StringBuilder();
                  sb.append("AmigoNPC: Coletado ");
                  boolean first = true;

                  for(Map.Entry en : rec.lootChatAcc.entrySet()) {
                     if (en != null) {
                        String id = (String)en.getKey();
                        Integer q = (Integer)en.getValue();
                        if (id != null && !id.isBlank() && q != null && q > 0) {
                           if (!first) {
                              sb.append(", ");
                           }

                           first = false;
                           sb.append(q).append(" ").append(id);
                           if (sb.length() > 180) {
                              break;
                           }
                        }
                     }
                  }

                  sb.append(".");
                  this.sendToOwner(worldObj, ownerId, sb.toString());
               } catch (Throwable var17) {
               } finally {
                  rec.lootChatAcc.clear();
                  rec.lootChatSendAtMillis = 0L;
               }

            }
         }
      }
   }

   private boolean tryPickupGroundItemIntoBackpack(Store store, UUID ownerId, NpcRecord rec, SimpleItemContainer bag, Object itemRefObj) {
      if (store != null && ownerId != null && rec != null && bag != null && itemRefObj != null) {
         if (!(itemRefObj instanceof Ref)) {
            return false;
         } else {
            Ref<EntityStore> itemRef = (Ref)itemRefObj;

            ItemComponent ic;
            try {
               ic = (ItemComponent)store.getComponent(itemRef, ItemComponent.getComponentType());
            } catch (Throwable var22) {
               return false;
            }

            if (ic == null) {
               return false;
            } else {
               try {
                  ic.setPickupDelay(0.0F);
               } catch (Throwable var21) {
               }

               ItemStack before;
               try {
                  before = ic.getItemStack();
               } catch (Throwable var20) {
                  return false;
               }

               if (before == null) {
                  return false;
               } else {
                  int beforeQty;
                  try {
                     beforeQty = before.getQuantity();
                  } catch (Throwable var19) {
                     return false;
                  }

                  if (beforeQty <= 0) {
                     return false;
                  } else {
                     String itemId;
                     try {
                        itemId = before.getItemId();
                     } catch (Throwable var18) {
                        itemId = null;
                     }

                     if (itemId == null || itemId.isBlank()) {
                        itemId = "item";
                     }

                     try {
                        if (!AutoLootConfigService.isItemAllowed(itemId)) {
                           return false;
                        }
                     } catch (Throwable var17) {
                     }

                     try {
                        ItemStack rem;
                        try {
                           rem = ItemComponent.addToItemContainer(store, itemRef, bag);
                        } catch (Throwable var23) {
                           ItemStackTransaction tx = bag.addItemStack(before);
                           rem = tx != null ? tx.getRemainder() : null;
                        }

                        int remQty = 0;

                        try {
                           if (rem != null) {
                              remQty = rem.getQuantity();
                           }
                        } catch (Throwable var16) {
                        }

                        int inserted = beforeQty - remQty;
                        if (inserted <= 0) {
                           if (isBackpackCompletelyFull(bag)) {
                              rec.lootPausedInventoryFull = true;
                              rec.lootingActive = false;
                              this.maybeNotifyBackpackFull(rec, ownerId, rec.worldObj, System.currentTimeMillis());
                           }

                           return false;
                        } else {
                           rec.backpackDirty = true;
                           long now = System.currentTimeMillis();
                           if (rec.nextBackpackSaveMillis == 0L) {
                              rec.nextBackpackSaveMillis = now + 5000L;
                           }

                           if (rem != null && remQty > 0) {
                              ic.setItemStack(rem);
                              store.putComponent(itemRef, ItemComponent.getComponentType(), ic);
                           } else {
                              store.removeEntity(itemRef, RemoveReason.REMOVE);
                           }

                           lootChatAccAdd(rec, itemId, inserted, now);
                           return true;
                        }
                     } catch (Throwable var24) {
                        return false;
                     }
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   private void refreshPendingLootFromCombatTags(Store store, NpcRecord rec, Vector3d npcPos, Vector3d ownerPos, long now) {
      if (store != null && rec != null) {
         try {
            rec.lootProcessedUntil.entrySet().removeIf((e) -> e == null || e.getValue() == null || (Long)e.getValue() <= now);
         } catch (Throwable var13) {
         }

         ArrayList<CombatTag> tags = new ArrayList();
         synchronized(rec.combatTags) {
            tags.addAll(rec.combatTags);
         }

         if (!tags.isEmpty()) {
            double tagR2 = (double)36.0F;

            try {
               store.forEachChunk(AUTOLOOT_QUERY, (chunkObj, cb) -> {
                  if (!(chunkObj instanceof ArchetypeChunk)) {
                     return true;
                  } else {
                     ArchetypeChunk<?> rawChunk = chunkObj;

                     ArchetypeChunk<EntityStore> chunk;
                     try {
                        chunk = rawChunk;
                     } catch (Throwable var18) {
                        return true;
                     }

                     int sz;
                     try {
                        sz = chunk.size();
                     } catch (Throwable var17) {
                        return true;
                     }

                     for(int i = 0; i < sz; ++i) {
                        if (rec.pendingLootRefObjs.size() >= 512) {
                           return true;
                        }

                        Ref<EntityStore> ref;
                        try {
                           ref = chunk.getReferenceTo(i);
                        } catch (Throwable var20) {
                           continue;
                        }

                        if (ref != null && !containsRef(rec.pendingLootRefObjs, ref)) {
                           Long until = (Long)rec.lootProcessedUntil.get(ref);
                           if (until == null || until <= now) {
                              TransformComponent tc;
                              try {
                                 tc = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                              } catch (Throwable var19) {
                                 continue;
                              }

                              if (tc != null && tc.getPosition() != null) {
                                 Vector3d p = tc.getPosition();
                                 boolean nearAnyTag = false;

                                 for(CombatTag t : tags) {
                                    if (t != null && t.pos != null && dist2(p, t.pos) <= (double)36.0F) {
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
            } catch (Throwable var11) {
            }

         }
      }
   }

   private static boolean containsRef(ArrayList list, Ref ref) {
      if (list != null && ref != null) {
         for(Object o : list) {
            if (o instanceof Ref && refEq((Ref)o, ref)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private Object chooseNearestPendingLoot(NpcRecord rec, Store store, Vector3d npcPos) {
      if (rec != null && store != null && npcPos != null) {
         Object best = null;
         double bestD2 = Double.POSITIVE_INFINITY;

         for(int i = rec.pendingLootRefObjs.size() - 1; i >= 0; --i) {
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
               } catch (Throwable var12) {
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private void tickCombatTaggedLooting(Store store, UUID ownerId, NpcRecord rec, Object ownerRefObj, Vector3d npcPos, Vector3d ownerPos, long now, Object worldObj, boolean inCombatOrAssistNow) {
      if (store != null && ownerId != null && rec != null && npcPos != null) {
         if (!rec.downed) {
            if (rec.state == AmigoNpcManager.State.ACTIVE) {
               if (inCombatOrAssistNow) {
                  if (!rec.wasInCombat) {
                     try {
                        ActionTraceService.getShared().record(ownerId, "npc_state", "combat_active");
                     } catch (Throwable var26) {
                     }
                  }

                  rec.wasInCombat = true;
                  rec.lootingActive = false;
                  rec.lootTargetRefObj = null;
                  rec.lootTargetSinceMillis = 0L;
                  rec.lootStickUntilMillis = 0L;
                  rec.pendingLootRefObjs.clear();
               } else {
                  if (rec.wasInCombat) {
                     rec.wasInCombat = false;
                     rec.lastCombatEndMillis = now;

                     try {
                        ActionTraceService.getShared().record(ownerId, "npc_state", "combat_end");
                     } catch (Throwable var33) {
                     }

                     boolean hasTags;
                     synchronized(rec.combatTags) {
                        hasTags = !rec.combatTags.isEmpty();
                     }

                     rec.lootingActive = hasTags;
                     rec.lootStickUntilMillis = hasTags ? now + 500L : 0L;
                     if (hasTags) {
                        try {
                           ActionTraceService.getShared().record(ownerId, "npc_state", "looting_start");
                        } catch (Throwable var31) {
                        }
                     }

                     rec.lootTargetRefObj = null;
                     rec.lootTargetSinceMillis = 0L;
                     rec.pendingLootRefObjs.clear();
                  }

                  if (rec.lootingActive) {
                     if (rec.lastCombatEndMillis > 0L) {
                        boolean timeUp = now - rec.lastCombatEndMillis >= 25000L;
                        boolean distUp = false;
                        if (rec.lastBattleCenter != null && ownerPos != null) {
                           distUp = dist2(rec.lastBattleCenter, ownerPos) >= (double)625.0F;
                        }

                        if (timeUp || distUp) {
                           this.clearCombatTagsAndLoot(rec);
                           return;
                        }
                     }

                     if (ownerRefObj != null && isAnyEnemyNearNpc(store, rec, ownerRefObj, npcPos, (double)4.0F)) {
                        rec.lootingActive = false;
                        rec.lootTargetRefObj = null;
                        rec.lootTargetSinceMillis = 0L;
                        rec.pendingLootRefObjs.clear();

                        try {
                           ActionTraceService.getShared().record(ownerId, "npc_state", "looting_stop reason=enemy_near");
                        } catch (Throwable var27) {
                        }

                     } else {
                        if (rec.backpack == null) {
                           try {
                              rec.backpack = this.getOrLoadBackpack(ownerId);
                           } catch (Throwable var30) {
                           }
                        }

                        SimpleItemContainer bag = rec.backpack;
                        if (bag != null) {
                           rec.lootPausedInventoryFull = false;
                           if (rec.lootTargetRefObj == null) {
                              if (rec.pendingLootRefObjs.isEmpty()) {
                                 this.refreshPendingLootFromCombatTags(store, rec, npcPos, ownerPos, now);
                              }

                              Object chosen = this.chooseNearestPendingLoot(rec, store, npcPos);
                              if (chosen == null) {
                                 if (rec.lootStickUntilMillis > 0L && now < rec.lootStickUntilMillis) {
                                    return;
                                 }

                                 rec.lootingActive = false;
                                 return;
                              }

                              rec.lootTargetRefObj = chosen;
                              rec.lootTargetSinceMillis = now;
                           }

                           if (!isItemRefValid(store, rec.lootTargetRefObj)) {
                              rec.lootTargetRefObj = null;
                              rec.lootTargetSinceMillis = 0L;
                           } else if (rec.lootTargetSinceMillis > 0L && now - rec.lootTargetSinceMillis > 12000L) {
                              rec.lootProcessedUntil.put(rec.lootTargetRefObj, now + 4000L);
                              rec.lootTargetRefObj = null;
                              rec.lootTargetSinceMillis = 0L;
                           } else {
                              try {
                                 Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                                 if (npcEntityObj != null) {
                                    setLockedTargetOnNpcEntity(npcEntityObj, rec.lootTargetRefObj);
                                    setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", (Object)null);
                                    setFlockState(store, rec.refObj, "Run", "");
                                 }
                              } catch (Throwable var29) {
                              }

                              try {
                                 Ref<EntityStore> itemRef = (Ref)rec.lootTargetRefObj;
                                 TransformComponent itc = (TransformComponent)store.getComponent(itemRef, TransformComponent.getComponentType());
                                 if (itc == null || itc.getPosition() == null) {
                                    return;
                                 }

                                 Vector3d ip = itc.getPosition();
                                 double dx = ip.getX() - npcPos.getX();
                                 double dz = ip.getZ() - npcPos.getZ();
                                 double h2 = dx * dx + dz * dz;
                                 double dy = Math.abs(ip.getY() - npcPos.getY());
                                 double lootMaxDy = (double)10.0F;

                                 try {
                                    AutoLootConfig cfg = AutoLootConfigService.get();
                                    if (cfg != null && cfg.VerticalScanBlocks > 0) {
                                       lootMaxDy = (double)cfg.VerticalScanBlocks;
                                    }
                                 } catch (Throwable var28) {
                                 }

                                 if (h2 <= (double)25.0F && dy <= lootMaxDy) {
                                    boolean inserted = this.tryPickupGroundItemIntoBackpack(store, ownerId, rec, bag, itemRef);
                                    if (!inserted) {
                                       rec.lootProcessedUntil.put(itemRef, now + 4000L);
                                    }

                                    removeRefFromList(rec.pendingLootRefObjs, itemRef);
                                    rec.lootTargetRefObj = null;
                                    rec.lootTargetSinceMillis = 0L;
                                 }
                              } catch (Throwable var34) {
                              }

                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void removeRefFromList(ArrayList list, Ref ref) {
      if (list != null && ref != null) {
         for(int i = list.size() - 1; i >= 0; --i) {
            Object o = list.get(i);
            if (o instanceof Ref && refEq((Ref)o, ref)) {
               list.remove(i);
            }
         }

      }
   }

   private void tryAutoLoot(Store store, UUID ownerId, NpcRecord rec, Vector3d npcPos, Vector3d ownerPos, long now, Object worldObj) {
      if (rec != null && store != null && ownerId != null && npcPos != null) {
         if (!rec.autoLootEnabled) {
            endCombatTaggedLooting(rec);
         } else if (!rec.downed) {
            if (rec.state == AmigoNpcManager.State.ACTIVE) {
               boolean forceDelayedScan = false;
               Vector3d forcedLootCenter = null;

               try {
                  if (rec.autoLootStickUntilMillis > 0L) {
                     if (now < rec.autoLootStickUntilMillis) {
                        rec.nextAutoLootMillis = Math.max(rec.nextAutoLootMillis, rec.autoLootStickUntilMillis);
                        return;
                     }

                     forceDelayedScan = true;
                     forcedLootCenter = rec.autoLootStickAnchorPos != null ? rec.autoLootStickAnchorPos : npcPos;
                     rec.autoLootStickUntilMillis = 0L;
                     rec.autoLootStickDeadRefObj = null;
                     rec.autoLootStickAnchorPos = null;
                     rec.nextAutoLootMillis = now;
                  }
               } catch (Throwable var38) {
               }

               if (forceDelayedScan || now >= rec.nextAutoLootMillis) {
                  rec.nextAutoLootMillis = now + 250L;
                  double radius = (double)5.0F;
                  double r2 = (double)25.0F;
                  int maxPerScan = 8;
                  double maxDy = (double)5.0F;

                  try {
                     AutoLootConfig cfg = AutoLootConfigService.get();
                     if (cfg != null && cfg.VerticalScanBlocks > 0) {
                        maxDy = (double)cfg.VerticalScanBlocks;
                     }
                  } catch (Throwable var37) {
                  }

                  double maxDyFinal = maxDy;
                  Vector3d lootCenterPos = forceDelayedScan && forcedLootCenter != null ? forcedLootCenter : npcPos;
                  if (rec.backpack == null) {
                     try {
                        rec.backpack = this.getOrLoadBackpack(ownerId);
                     } catch (Throwable t) {
                        String var10003 = t.getClass().getSimpleName();
                        this.debugCombat(rec, ownerId, "AutoLoot: falha ao carregar mochila: " + var10003 + ": " + t.getMessage());
                     }
                  }

                  SimpleItemContainer bag = rec.backpack;
                  if (bag == null) {
                     this.debugCombat(rec, ownerId, "AutoLoot: mochila null (sem container)");
                  } else {
                     if (rec.lootPausedInventoryFull) {
                        if (now < rec.nextLootFullRecheckMillis) {
                           return;
                        }

                        rec.nextLootFullRecheckMillis = now + 1000L;
                        if (isBackpackCompletelyFull(bag)) {
                           this.maybeNotifyBackpackFull(rec, ownerId, worldObj, now);
                           return;
                        }

                        rec.lootPausedInventoryFull = false;
                     }

                     if (isBackpackCompletelyFull(bag)) {
                        rec.lootPausedInventoryFull = true;
                        rec.nextLootFullRecheckMillis = now + 1000L;
                        this.maybeNotifyBackpackFull(rec, ownerId, worldObj, now);
                     } else {
                        int[] picked = new int[]{0};
                        ArrayList<Ref<EntityStore>> removeLater = new ArrayList();
                        ArrayList<Ref<EntityStore>> updateLaterRef = new ArrayList();
                        ArrayList<ItemComponent> updateLaterComp = new ArrayList();

                        try {
                           store.forEachChunk(AUTOLOOT_QUERY, (chunkObj, cb) -> {
                              ArchetypeChunk<EntityStore> chunk = null;
                              if (chunkObj instanceof ArchetypeChunk) {
                                 ArchetypeChunk<?> rawChunk = chunkObj;

                                 try {
                                    chunk = rawChunk;
                                 } catch (Throwable var45) {
                                    chunk = null;
                                 }
                              }

                              int size;
                              try {
                                 size = chunk != null ? chunk.size() : chunkGetSize(chunkObj);
                              } catch (Throwable var44) {
                                 return true;
                              }

                              for(int i = 0; i < size && picked[0] < 8; ++i) {
                                 Ref<EntityStore> itemRef;
                                 try {
                                    itemRef = chunk != null ? chunk.getReferenceTo(i) : chunkGetEntity(chunkObj, i);
                                 } catch (Throwable var49) {
                                    continue;
                                 }

                                 if (itemRef != null && itemRef.isValid()) {
                                    TransformComponent tc;
                                    ItemComponent ic;
                                    try {
                                       tc = chunk != null ? (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType()) : (TransformComponent)chunkGetComponent(chunkObj, i, TransformComponent.getComponentType());
                                       ic = chunk != null ? (ItemComponent)chunk.getComponent(i, ItemComponent.getComponentType()) : (ItemComponent)chunkGetComponent(chunkObj, i, ItemComponent.getComponentType());
                                    } catch (Throwable var48) {
                                       continue;
                                    }

                                    if (tc != null && ic != null) {
                                       try {
                                          ic.setPickupDelay(0.0F);
                                       } catch (Throwable var43) {
                                       }

                                       Vector3d pos = tc.getPosition();
                                       if (pos != null) {
                                          double ldx = pos.getX() - lootCenterPos.getX();
                                          double ldz = pos.getZ() - lootCenterPos.getZ();
                                          double ldy = Math.abs(pos.getY() - lootCenterPos.getY());
                                          if (!(ldx * ldx + ldz * ldz > (double)25.0F) && !(ldy > maxDyFinal)) {
                                             ItemStack before = ic.getItemStack();
                                             if (before != null) {
                                                int beforeQty;
                                                try {
                                                   beforeQty = before.getQuantity();
                                                } catch (Throwable var47) {
                                                   continue;
                                                }

                                                if (beforeQty > 0) {
                                                   String itemIdStr = null;

                                                   try {
                                                      Object idObj = before.getItemId();
                                                      if (idObj != null) {
                                                         itemIdStr = idObj.toString();
                                                      }
                                                   } catch (Throwable var42) {
                                                   }

                                                   if (itemIdStr == null || itemIdStr.isBlank()) {
                                                      itemIdStr = "item";
                                                   }

                                                   try {
                                                      if (!AutoLootConfigService.isItemAllowed(itemIdStr)) {
                                                         continue;
                                                      }
                                                   } catch (Throwable var46) {
                                                   }

                                                   ItemStack remainder = null;

                                                   try {
                                                      remainder = ItemComponent.addToItemContainer(store, itemRef, bag);
                                                   } catch (Throwable var41) {
                                                      remainder = null;
                                                   }

                                                   if (remainder == null) {
                                                      ItemStackTransaction tx;
                                                      try {
                                                         tx = bag.addItemStack(before);
                                                      } catch (Throwable var40) {
                                                         tx = null;
                                                      }

                                                      try {
                                                         remainder = tx != null ? tx.getRemainder() : null;
                                                      } catch (Throwable var39) {
                                                      }
                                                   }

                                                   int remQty = 0;
                                                   if (remainder != null) {
                                                      try {
                                                         remQty = remainder.getQuantity();
                                                      } catch (Throwable var38) {
                                                         remQty = beforeQty;
                                                      }
                                                   }

                                                   if (remainder != null && remQty >= beforeQty) {
                                                      if (isBackpackCompletelyFull(bag)) {
                                                         rec.lootPausedInventoryFull = true;
                                                         rec.nextLootFullRecheckMillis = now + 1000L;
                                                         this.maybeNotifyBackpackFull(rec, ownerId, worldObj, now);
                                                         return false;
                                                      }
                                                   } else {
                                                      int inserted = beforeQty - (remainder != null ? remQty : 0);
                                                      if (inserted > 0) {
                                                         lootChatAccAdd(rec, itemIdStr, inserted, now);
                                                      }

                                                      int var10002 = picked[0]++;
                                                      rec.backpackDirty = true;
                                                      rec.nextBackpackSaveMillis = Math.min(rec.nextBackpackSaveMillis, now + 400L);
                                                      if (remainder != null && remQty > 0) {
                                                         try {
                                                            ic.setItemStack(remainder);
                                                            updateLaterRef.add(itemRef);
                                                            updateLaterComp.add(ic);
                                                         } catch (Throwable var37) {
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
                           });
                        } catch (Throwable t) {
                           String var45 = t.getClass().getSimpleName();
                           this.debugCombat(rec, ownerId, "AutoLoot forEachChunk falhou: " + var45 + ": " + t.getMessage());
                        }

                        for(int i = 0; i < updateLaterRef.size(); ++i) {
                           Ref<EntityStore> ref = (Ref)updateLaterRef.get(i);
                           ItemComponent comp = (ItemComponent)updateLaterComp.get(i);
                           if (ref != null && ref.isValid() && comp != null) {
                              try {
                                 store.putComponent(ref, ItemComponent.getComponentType(), comp);
                              } catch (Throwable var34) {
                              }
                           }
                        }

                        for(Ref r : removeLater) {
                           if (r != null && r.isValid()) {
                              try {
                                 store.removeEntity(r, RemoveReason.REMOVE);
                              } catch (Throwable var33) {
                                 doRemoveEntity(store, r);
                              }
                           }
                        }

                        if (rec.backpackDirty && now >= rec.nextBackpackSaveMillis) {
                           try {
                              AmigoPersistence.saveBackpack(ownerId, bag);
                              rec.backpackDirty = false;
                           } catch (Throwable t) {
                              this.debugCombat(rec, ownerId, "Falha ao salvar mochila: " + t.getMessage());
                           }
                        }

                        this.lootChatAccFlushIfDue(rec, ownerId, worldObj, now);
                        if (picked[0] > 0) {
                           String wn = null;

                           try {
                              Object n = invokeNoArg(worldObj, "getName");
                              if (n instanceof String) {
                                 String s = (String)n;
                                 if (!s.isBlank()) {
                                    wn = s;
                                 }
                              }
                           } catch (Throwable var31) {
                           }

                           try {
                              NpcCollectXpSource.onPickup(ownerId, picked[0], wn);
                           } catch (Throwable var30) {
                           }
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isBackpackCompletelyFull(SimpleItemContainer bag) {
      if (bag == null) {
         return true;
      } else {
         short cap;
         try {
            cap = bag.getCapacity();
         } catch (Throwable var7) {
            return true;
         }

         for(short slot = 0; slot < cap; ++slot) {
            ItemStack st;
            try {
               st = bag.getItemStack(slot);
            } catch (Throwable var8) {
               continue;
            }

            if (st == null || st.isEmpty()) {
               return false;
            }

            try {
               Item item = st.getItem();
               int max = item != null ? item.getMaxStack() : 1;
               if (st.getQuantity() < max) {
                  return false;
               }
            } catch (Throwable var6) {
               return false;
            }
         }

         return true;
      }
   }

   private static double distSq(Vector3d a, Vector3d b) {
      double dx = a.getX() - b.getX();
      double dy = a.getY() - b.getY();
      double dz = a.getZ() - b.getZ();
      return dx * dx + dy * dy + dz * dz;
   }

   private static int chunkGetSize(Object chunk) {
      Object v = invokeNoArg(chunk, "size");
      if (v == null) {
         v = invokeNoArg(chunk, "getSize");
      }

      if (v instanceof Integer i) {
         return i;
      } else if (v instanceof Short s) {
         return s;
      } else if (v instanceof Long l) {
         return (int)l;
      } else {
         return 0;
      }
   }

   private static Ref chunkGetEntity(Object chunk, int index) {
      if (chunk == null) {
         return null;
      } else {
         try {
            Method m = chunk.getClass().getMethod("getReferenceTo", Integer.TYPE);
            return (Ref)m.invoke(chunk, index);
         } catch (Throwable var10) {
            try {
               for(Method m : chunk.getClass().getMethods()) {
                  String name = m.getName();
                  if ((name.equals("getReferenceTo") || name.equals("getEntity") || name.equals("getReference")) && m.getParameterCount() == 1) {
                     Class<?> p0 = m.getParameterTypes()[0];
                     if (p0 == Integer.TYPE || p0 == Integer.class || p0 == Short.TYPE || p0 == Short.class) {
                        Object idx = p0 != Short.TYPE && p0 != Short.class ? index : (short)index;
                        return (Ref)m.invoke(chunk, idx);
                     }
                  }
               }
            } catch (Throwable var9) {
            }

            return null;
         }
      }
   }

   private static Object chunkGetComponent(Object chunk, int index, Object componentType) {
      if (chunk != null && componentType != null) {
         try {
            for(Method m : chunk.getClass().getMethods()) {
               if (m.getName().equals("getComponent") && m.getParameterCount() == 2) {
                  Class<?>[] p = m.getParameterTypes();
                  boolean firstIsIndex = p[0] == Integer.TYPE || p[0] == Integer.class || p[0] == Short.TYPE || p[0] == Short.class;
                  if (firstIsIndex) {
                     Object idx = p[0] != Short.TYPE && p[0] != Short.class ? index : (short)index;

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

   private void maybeNotifyBackpackFull(NpcRecord rec, UUID ownerId, Object worldObj, long now) {
      if (rec != null && ownerId != null && worldObj != null) {
         if (now >= rec.nextLootFullMsgMillis) {
            rec.nextLootFullMsgMillis = now + 30000L;

            try {
               this.sendToOwner(worldObj, ownerId, "Estou com o inventário cheio.");
            } catch (Throwable var7) {
            }

         }
      }
   }

   private Object getActiveCombatTarget(NpcRecord rec, long now) {
      if (rec == null) {
         return null;
      } else if (rec.combatTargetRefObj == null) {
         return null;
      } else if (rec.combatUntilMillis <= 0L) {
         return null;
      } else {
         return now > rec.combatUntilMillis ? null : rec.combatTargetRefObj;
      }
   }

   private Object getActiveNpcCombatTarget(NpcRecord rec, long now) {
      if (rec == null) {
         return null;
      } else if (rec.npcCombatTargetRefObj == null) {
         return null;
      } else if (rec.npcCombatUntilMillis <= 0L) {
         return null;
      } else {
         return now > rec.npcCombatUntilMillis ? null : rec.npcCombatTargetRefObj;
      }
   }

   public void tickDowned() {
      long now = System.currentTimeMillis();

      for(Map.Entry e : this.npcRefPorPlayer.entrySet()) {
         UUID owner = (UUID)e.getKey();
         NpcRecord rec = (NpcRecord)e.getValue();
         if (rec != null && rec.downed && rec.downedUntilMillis > 0L) {
            if (rec.deathDespawnAtMillis > 0L && now >= rec.deathDespawnAtMillis) {
               rec.deathDespawnAtMillis = 0L;
               if (rec.worldObj != null && rec.refObj != null) {
                  Object worldObj = rec.worldObj;
                  Object refToRemove = rec.refObj;
                  HytaleBridge.worldExecute(worldObj, () -> {
                     try {
                        Object storeObj = getComponentStoreFromWorld(worldObj);
                        if (storeObj == null) {
                           return;
                        }

                        if (rec.refObj != refToRemove) {
                           return;
                        }

                        try {
                           doRemoveEntity(storeObj, refToRemove);
                        } catch (Throwable var7) {
                        }

                        try {
                           this.amigoRefs.remove(refToRemove);
                        } catch (Throwable var6) {
                        }

                        rec.refObj = null;
                     } catch (Throwable var8) {
                     }

                  });
               }
            }

            if (now >= rec.downedUntilMillis && rec.state != AmigoNpcManager.State.SPAWNING && rec.worldObj != null) {
               Object worldObj = rec.worldObj;
               rec.state = AmigoNpcManager.State.SPAWNING;
               boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
                  try {
                     Object storeObj = getComponentStoreFromWorld(worldObj);
                     if (storeObj == null) {
                        rec.state = AmigoNpcManager.State.ACTIVE;
                        return;
                     }

                     Object ownerPos = tryGetOwnerPositionFromWorldStore(worldObj, storeObj, owner);
                     if (ownerPos == null) {
                        rec.state = AmigoNpcManager.State.ACTIVE;
                        return;
                     }

                     if (rec.refObj != null) {
                        Object oldRef = rec.refObj;

                        try {
                           doRemoveEntity(storeObj, oldRef);
                        } catch (Throwable var9) {
                        }

                        try {
                           this.amigoRefs.remove(oldRef);
                        } catch (Throwable var8) {
                        }

                        rec.refObj = null;
                     }

                     this.spawnIntoExistingRecord(worldObj, storeObj, owner, (Object)null, rec);
                     if (rec.refObj == null) {
                        rec.state = AmigoNpcManager.State.ACTIVE;
                     }
                  } catch (Throwable var10) {
                     rec.state = AmigoNpcManager.State.ACTIVE;
                  }

               });
               if (!queued) {
                  rec.state = AmigoNpcManager.State.ACTIVE;
               }
            }
         }
      }

   }

   public void tickFollow() {
      if (!this.pendingRespawns.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<Map.Entry<UUID, PendingRespawn>> it = this.pendingRespawns.entrySet().iterator();

         while(it.hasNext()) {
            Map.Entry<UUID, PendingRespawn> en = (Map.Entry)it.next();
            UUID ownerId = (UUID)en.getKey();
            PendingRespawn pr = (PendingRespawn)en.getValue();
            if (pr != null && pr.worldObj != null) {
               if (now >= pr.atMillis) {
                  if (this.npcRefPorPlayer.containsKey(ownerId)) {
                     it.remove();
                  } else {
                     if (pr.message != null && !pr.message.isBlank()) {
                        this.sendToOwner(pr.worldObj, ownerId, pr.message);
                     }

                     this.spawn(pr.worldObj, ownerId, pr.senderObj);
                     it.remove();
                  }
               }
            } else {
               it.remove();
            }
         }
      }

      for(Map.Entry e : this.npcRefPorPlayer.entrySet()) {
         UUID owner = (UUID)e.getKey();
         NpcRecord rec = (NpcRecord)e.getValue();
         if (rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.refObj != null && rec.worldObj != null && !rec.downed) {
            Object worldObj = rec.worldObj;
            HytaleBridge.worldExecute(worldObj, () -> {
               try {
                  Object storeObj = getComponentStoreFromWorld(worldObj);
                  if (!(storeObj instanceof Store)) {
                     return;
                  }

                  Store<?> rawStore = (Store)storeObj;
                  Store<EntityStore> store = rawStore;
                  Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, owner);
                  if (ownerRef == null) {
                     return;
                  }

                  Object ownerTransform = getComponentFromStore(rawStore, ownerRef, TransformComponent.getComponentType());
                  Object npcTransform = getComponentFromStore(rawStore, rec.refObj, TransformComponent.getComponentType());
                  if (!(npcTransform instanceof TransformComponent)) {
                     this.amigoRefs.remove(rec.refObj);
                     this.npcRefPorPlayer.remove(owner, rec);
                     return;
                  }

                  try {
                     Ref<EntityStore> npcRef = (Ref)rec.refObj;
                     EntityStatMap st = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
                     if (st != null) {
                        try {
                           EntityStatValue hp = st.get(DefaultEntityStatTypes.getHealth());
                           if (hp != null && hp.get() <= 0.0F) {
                              this.markDowned(owner);
                              return;
                           }
                        } catch (Throwable var77) {
                        }
                     }
                  } catch (Throwable var78) {
                  }

                  if (!(ownerTransform instanceof TransformComponent)) {
                     return;
                  }

                  TransformComponent ot = (TransformComponent)ownerTransform;
                  TransformComponent nt = (TransformComponent)npcTransform;
                  Vector3d op = ot.getPosition();
                  Vector3d np = nt.getPosition();
                  if (op == null || np == null) {
                     return;
                  }

                  try {
                     int ownerLevel = Math.max(1, rec.npcLevelCached);
                     ZoneMobService.ZoneSnapshot zs = ZoneMobService.getShared().computeZoneSnapshot(worldObj, store, ownerRef, ownerLevel);
                     rec.zoneRawId = zs.rawZoneId();
                     rec.zoneExpected = zs.expectedZone();
                     rec.zoneCurrent = zs.currentZone();
                     rec.zoneHudColor = zs.color();
                  } catch (Throwable var76) {
                     rec.zoneRawId = 0;
                     rec.zoneExpected = 0;
                     rec.zoneCurrent = 0;
                     rec.zoneHudColor = ZoneModel.WHITE;
                  }

                  double dx = op.getX() - np.getX();
                  double dz = op.getZ() - np.getZ();
                  double dy = op.getY() - np.getY();
                  double horizontal = Math.sqrt(dx * dx + dz * dz);
                  if (rec.chaseDisengaged && horizontal <= (double)10.0F) {
                     rec.chaseDisengaged = false;
                  }

                  if (horizontal > (double)25.0F) {
                     if (rec.farSinceMillis == 0L) {
                        rec.farSinceMillis = System.currentTimeMillis();
                     }
                  } else {
                     rec.farSinceMillis = 0L;
                  }

                  long now = System.currentTimeMillis();
                  if (rec.nextUndergroundCheckMillis <= 0L || now >= rec.nextUndergroundCheckMillis) {
                     rec.nextUndergroundCheckMillis = now + 500L;
                     rec.ownerUnderground = hasCeilingAboveBestEffort(rawStore, op);
                  }

                  boolean ownerUnderground = rec.ownerUnderground;
                  if (rec.lastNpcMovedMillis == 0L) {
                     rec.lastNpcMovedMillis = now;
                  }

                  if (rec.lastSampleMillis == 0L) {
                     rec.lastSampleMillis = now;
                  }

                  if (rec.lastNpcPos == null) {
                     rec.lastNpcPos = np;
                  }

                  if (now - rec.lastSampleMillis >= 400L) {
                     double mdx = np.getX() - rec.lastNpcPos.getX();
                     double mdy = np.getY() - rec.lastNpcPos.getY();
                     double mdz = np.getZ() - rec.lastNpcPos.getZ();
                     double moved = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
                     if (moved > (double)0.25F) {
                        rec.lastNpcMovedMillis = now;
                     }

                     rec.lastNpcPos = np;
                     rec.lastSampleMillis = now;
                  }

                  clearExpiredAttackAnimation(rawStore, rec.refObj, rec, now);

                  try {
                     tickLevelUpFx(store, ownerRef, rec.refObj, rec, now);
                  } catch (Throwable var75) {
                  }

                  try {
                     tickWardrobeRestore(store, rec, ownerRef, now);
                  } catch (Throwable var74) {
                  }

                  boolean lootStickActiveNow = false;

                  try {
                     if (lootStickActiveNow) {
                        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                           setMarkedTargetOnNpcEntity(npcEntityObj, "LockedTargetClose", (Object)null);
                           setLockedTargetOnNpcEntity(npcEntityObj, rec.refObj);
                           setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", (Object)null);
                        }
                     } else if (!rec.defendeEnabled) {
                        if (rec.combatUntilMillis > 0L && now > rec.combatUntilMillis) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                        }

                        if (rec.npcCombatUntilMillis > 0L && now > rec.npcCombatUntilMillis) {
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                        }

                        Object combatTarget = this.getActiveCombatTarget(rec, now);
                        if (combatTarget != null && !isAliveEntityRef(store, combatTarget)) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                           combatTarget = null;
                        }

                        Object npcCombatTarget = this.getActiveNpcCombatTarget(rec, now);
                        if (npcCombatTarget != null && !isAliveEntityRef(store, npcCombatTarget)) {
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                           npcCombatTarget = null;
                        }

                        rec.assistUntilMillis = 0L;
                        rec.assistTargetRefObj = null;
                        Object desiredTarget = combatTarget != null ? combatTarget : (npcCombatTarget != null ? npcCombatTarget : ownerRef);
                        if (desiredTarget != null && !refEq(desiredTarget, ownerRef) && horizontal > (double)30.0F) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                           combatTarget = null;
                           npcCombatTarget = null;
                           desiredTarget = ownerRef;
                        }

                        boolean inCombat = desiredTarget != null && !refEq(desiredTarget, ownerRef);
                        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                           boolean closeFollow = ownerUnderground && !inCombat;
                           boolean lootStickHold = rec.autoLootEnabled && rec.autoLootStickUntilMillis > now && !inCombat;
                           setMarkedTargetOnNpcEntity(npcEntityObj, "LockedTargetClose", closeFollow ? ownerRef : null);
                           Object lockedTarget = lootStickHold ? rec.refObj : (inCombat ? desiredTarget : ownerRef);
                           setLockedTargetOnNpcEntity(npcEntityObj, lockedTarget);
                           setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", inCombat ? desiredTarget : null);
                        }

                        if (inCombat) {
                           this.updateCurrentTargetMobLevel(store, rec, desiredTarget);
                           boolean wantsRanged = false;

                           try {
                              if (desiredTarget instanceof Ref && rec.refObj instanceof Ref) {
                                 Ref<EntityStore> tgtRef = (Ref)desiredTarget;
                                 TransformComponent npcT2 = (TransformComponent)store.getComponent((Ref)rec.refObj, TransformComponent.getComponentType());
                                 if (npcT2 != null && npcT2.getPosition() != null) {
                                    wantsRanged = isAirborneTarget(store, tgtRef, npcT2.getPosition());
                                 }
                              }
                           } catch (Throwable var73) {
                           }

                           if (wantsRanged) {
                              this.tryEquipDefaultBow(store, rec, now);
                              this.tryRangedAttack(store, rec, ownerRef, desiredTarget, now);
                           } else {
                              rec.rangedBowReadyAtMillis = 0L;
                              rec.rangedBowItemId = null;

                              try {
                                 if (rec.refObj instanceof Ref) {
                                    Ref<EntityStore> npcRef = (Ref)rec.refObj;
                                    this.applySwordWeaponNow(store, npcRef, rec.ownerId, rec, false);
                                 }
                              } catch (Throwable var72) {
                              }

                              this.tryMeleeAttack(store, rec, ownerRef, desiredTarget, now);
                           }
                        } else {
                           rec.rangedBowReadyAtMillis = 0L;
                           rec.rangedBowItemId = null;

                           try {
                              if (rec.refObj instanceof Ref) {
                                 Ref<EntityStore> npcRef = (Ref)rec.refObj;
                                 this.applySwordWeaponNow(store, npcRef, rec.ownerId, rec, false);
                              }
                           } catch (Throwable var71) {
                           }

                           rec.chaseDisengaged = false;
                           rec.targetLostSinceMillis = 0L;
                           rec.targetStuckSinceMillis = 0L;
                           rec.lastTargetHorizontal = (double)-1.0F;
                           rec.lastTargetSampleMillis = 0L;
                           rec.currentTargetMobLevel = 0;
                           rec.currentTargetMobUuid = null;
                        }
                     } else {
                        if (rec.combatUntilMillis > 0L && now > rec.combatUntilMillis) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                        }

                        if (rec.npcCombatUntilMillis > 0L && now > rec.npcCombatUntilMillis) {
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                        }

                        Object combatTarget = this.getActiveCombatTarget(rec, now);
                        if (combatTarget != null && !isAliveEntityRef(store, combatTarget)) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                           combatTarget = null;
                        }

                        Object npcCombatTarget = this.getActiveNpcCombatTarget(rec, now);
                        if (npcCombatTarget != null && !isAliveEntityRef(store, npcCombatTarget)) {
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                           npcCombatTarget = null;
                        }

                        tickAssistHousekeeping(rec, store, now);
                        if (!rec.chaseDisengaged && !rec.lootingActive && combatTarget == null && npcCombatTarget == null && rec.assistTargetRefObj == null && rec.assistUntilMillis == 0L) {
                           Object autoTarget = this.findNearestDefenderTarget(store, rec, ownerRef, op);
                           if (autoTarget != null) {
                              rec.assistTargetRefObj = autoTarget;
                              rec.assistUntilMillis = 0L;
                              this.debugCombat(rec, owner, "autoAssist: targetRef=" + String.valueOf(autoTarget));
                           }
                        }

                        Object desiredTarget = combatTarget;
                        if (combatTarget == null) {
                           desiredTarget = npcCombatTarget != null ? npcCombatTarget : null;
                        }

                        if (desiredTarget == null) {
                           Object assistTarget = this.getActiveAssistTarget(rec, now);
                           desiredTarget = assistTarget != null ? assistTarget : ownerRef;
                        }

                        boolean inCombatOrAssist = desiredTarget != null && !refEq(desiredTarget, ownerRef);
                        boolean lootStickHold = false;
                        if (inCombatOrAssist && horizontal > (double)30.0F) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                           rec.assistTargetRefObj = null;
                           rec.chaseDisengaged = true;
                           rec.assistUntilMillis = now + 3000L;
                           desiredTarget = ownerRef;
                           inCombatOrAssist = false;
                           rec.targetLostSinceMillis = 0L;
                           rec.targetStuckSinceMillis = 0L;
                           rec.lastTargetHorizontal = (double)-1.0F;
                           rec.lastTargetSampleMillis = 0L;
                        }

                        if (inCombatOrAssist && !isAliveEntityRef(store, desiredTarget)) {
                           if (rec.autoLootEnabled) {
                              boolean refresh = rec.autoLootStickUntilMillis <= 0L || now >= rec.autoLootStickUntilMillis || rec.autoLootStickDeadRefObj == null || !refEq(rec.autoLootStickDeadRefObj, desiredTarget);
                              if (refresh) {
                                 rec.autoLootStickUntilMillis = now + 900L;
                                 rec.autoLootStickDeadRefObj = desiredTarget;

                                 try {
                                    if (desiredTarget instanceof Ref) {
                                       Ref<EntityStore> deadRef = (Ref)desiredTarget;
                                       TransformComponent deadT = (TransformComponent)store.getComponent(deadRef, TransformComponent.getComponentType());
                                       if (deadT != null && deadT.getPosition() != null) {
                                          rec.autoLootStickAnchorPos = deadT.getPosition();
                                       }
                                    }

                                    if (rec.autoLootStickAnchorPos == null) {
                                       rec.autoLootStickAnchorPos = np;
                                    }
                                 } catch (Throwable var70) {
                                 }
                              }
                           }

                           if (rec.autoLootStickUntilMillis > 0L && now >= rec.autoLootStickUntilMillis) {
                           }

                           if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                              rec.combatUntilMillis = 0L;
                              rec.combatTargetRefObj = null;
                              combatTarget = null;
                           }

                           if (npcCombatTarget != null && refEq(desiredTarget, npcCombatTarget)) {
                              rec.npcCombatUntilMillis = 0L;
                              rec.npcCombatTargetRefObj = null;
                              npcCombatTarget = null;
                           }

                           if (rec.assistTargetRefObj != null && refEq(desiredTarget, rec.assistTargetRefObj)) {
                              rec.assistTargetRefObj = null;
                              rec.assistUntilMillis = now + 3000L;
                           }

                           Object rt = this.findNearestTargetNearNpc(store, rec, ownerRef, np, desiredTarget);
                           if (rt != null) {
                              rec.assistTargetRefObj = rt;
                              rec.assistUntilMillis = 0L;
                              desiredTarget = rt;
                              inCombatOrAssist = true;
                              rec.targetLostSinceMillis = 0L;
                              rec.targetStuckSinceMillis = 0L;
                              rec.lastTargetHorizontal = (double)-1.0F;
                              rec.lastTargetSampleMillis = 0L;
                              this.debugCombat(rec, owner, "retargetOnDeath: targetRef=" + String.valueOf(rt));
                           } else {
                              desiredTarget = ownerRef;
                              inCombatOrAssist = false;
                           }
                        }

                        if (inCombatOrAssist) {
                           try {
                              if (desiredTarget instanceof Ref && rec.refObj instanceof Ref) {
                                 Ref<EntityStore> tgtRef = (Ref)desiredTarget;
                                 Ref<EntityStore> npcRef = (Ref)rec.refObj;
                                 TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                                 TransformComponent tgtT = (TransformComponent)store.getComponent(tgtRef, TransformComponent.getComponentType());
                                 if (npcT != null && tgtT != null && npcT.getPosition() != null && tgtT.getPosition() != null) {
                                    Vector3d np2 = npcT.getPosition();
                                    Vector3d tp2 = tgtT.getPosition();
                                    double dx2 = tp2.getX() - np2.getX();
                                    double dz2 = tp2.getZ() - np2.getZ();
                                    double dy2 = tp2.getY() - np2.getY();
                                    double h2 = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                                    double ady2 = Math.abs(dy2);
                                    boolean airborne = false;

                                    try {
                                       airborne = isAirborneTarget(store, tgtRef, np2);
                                    } catch (Throwable var69) {
                                    }

                                    double targetHeight = getEntityHeight(store, tgtRef);
                                    boolean tallTarget = targetHeight >= 1.9;
                                    double allowedDy = airborne ? (double)35.0F : (tallTarget ? (double)3.0F : (double)2.0F);
                                    if (!airborne && tallTarget && h2 < 0.9 && now - rec.lastCombatNudgeMillis > 900L) {
                                       try {
                                          double len = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                                          double nx = len > 0.001 ? -dx2 / len : (double)1.0F;
                                          double nz = len > 0.001 ? -dz2 / len : (double)0.0F;
                                          Vector3d npos = new Vector3d(np2.getX() + nx * 0.8, np2.getY(), np2.getZ() + nz * 0.8);
                                          npcT.teleportPosition(npos);
                                          rec.lastCombatNudgeMillis = now;
                                       } catch (Throwable var68) {
                                       }
                                    }

                                    boolean tooFar = h2 > (double)30.0F;
                                    boolean tooHigh = ady2 > allowedDy;
                                    boolean stuckTooLong = false;
                                    if (airborne) {
                                       rec.targetStuckSinceMillis = 0L;
                                       rec.lastTargetHorizontal = h2;
                                       rec.lastTargetSampleMillis = now;
                                    } else {
                                       if (now - rec.lastTargetSampleMillis >= 350L) {
                                          boolean npcNotMoving = now - rec.lastNpcMovedMillis > 900L;
                                          boolean farFromTarget = h2 > 3.2;
                                          if (npcNotMoving && farFromTarget && rec.lastTargetHorizontal >= (double)0.0F && h2 >= rec.lastTargetHorizontal - 0.2) {
                                             if (rec.targetStuckSinceMillis == 0L) {
                                                rec.targetStuckSinceMillis = now;
                                             }
                                          } else {
                                             rec.targetStuckSinceMillis = 0L;
                                          }

                                          rec.lastTargetHorizontal = h2;
                                          rec.lastTargetSampleMillis = now;
                                       }

                                       stuckTooLong = rec.targetStuckSinceMillis > 0L && now - rec.targetStuckSinceMillis > 2000L;
                                    }

                                    boolean lostCond = tooFar || tooHigh || stuckTooLong;
                                    if (lostCond) {
                                       if (rec.targetLostSinceMillis == 0L) {
                                          rec.targetLostSinceMillis = now;
                                       }
                                    } else {
                                       rec.targetLostSinceMillis = 0L;
                                    }

                                    if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                                       boolean okToRenew = !lostCond && h2 <= (double)30.0F;
                                       if (okToRenew) {
                                          rec.combatUntilMillis = now + 3000L;
                                       }
                                    }

                                    if (rec.targetLostSinceMillis > 0L && now - rec.targetLostSinceMillis > 2000L) {
                                       if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                                          rec.combatUntilMillis = 0L;
                                          rec.combatTargetRefObj = null;
                                          combatTarget = null;
                                       } else if (npcCombatTarget != null && refEq(desiredTarget, npcCombatTarget)) {
                                          rec.npcCombatUntilMillis = 0L;
                                          rec.npcCombatTargetRefObj = null;
                                          npcCombatTarget = null;
                                       } else {
                                          rec.assistTargetRefObj = null;
                                          rec.assistUntilMillis = now + 3000L;
                                       }

                                       rec.targetLostSinceMillis = 0L;
                                       rec.targetStuckSinceMillis = 0L;
                                       rec.lastTargetHorizontal = (double)-1.0F;
                                       rec.lastTargetSampleMillis = 0L;
                                       desiredTarget = ownerRef;
                                       inCombatOrAssist = false;
                                    }
                                 }
                              }
                           } catch (Throwable var79) {
                           }
                        } else {
                           rec.targetLostSinceMillis = 0L;
                           rec.targetStuckSinceMillis = 0L;
                           rec.lastTargetHorizontal = (double)-1.0F;
                           rec.lastTargetSampleMillis = 0L;
                        }

                        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                           boolean closeFollow = ownerUnderground && !inCombatOrAssist;
                           setMarkedTargetOnNpcEntity(npcEntityObj, "LockedTargetClose", closeFollow ? ownerRef : null);
                           Object lockedTarget = lootStickHold ? rec.refObj : (inCombatOrAssist ? desiredTarget : ownerRef);
                           setLockedTargetOnNpcEntity(npcEntityObj, lockedTarget);
                           setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", inCombatOrAssist ? desiredTarget : null);
                           setFlockState(store, rec.refObj, inCombatOrAssist ? "Run" : "Walk", "");
                           if (inCombatOrAssist) {
                              this.updateCurrentTargetMobLevel(store, rec, desiredTarget);
                              boolean wantsRanged = false;

                              try {
                                 if (desiredTarget instanceof Ref && rec.refObj instanceof Ref) {
                                    Ref<EntityStore> tgtRef = (Ref)desiredTarget;
                                    Ref<EntityStore> npcRef = (Ref)rec.refObj;
                                    TransformComponent npcT2 = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                                    if (npcT2 != null && npcT2.getPosition() != null) {
                                       wantsRanged = isAirborneTarget(store, tgtRef, npcT2.getPosition());
                                    }
                                 }
                              } catch (Throwable var67) {
                              }

                              if (wantsRanged) {
                                 this.tryEquipDefaultBow(store, rec, now);
                                 this.tryRangedAttack(store, rec, ownerRef, desiredTarget, now);
                              } else {
                                 rec.rangedBowReadyAtMillis = 0L;
                                 rec.rangedBowItemId = null;

                                 try {
                                    if (rec.refObj instanceof Ref) {
                                       Ref<EntityStore> npcRef = (Ref)rec.refObj;
                                       this.applySwordWeaponNow(store, npcRef, rec.ownerId, rec, false);
                                    }
                                 } catch (Throwable var66) {
                                 }

                                 this.tryMeleeAttack(store, rec, ownerRef, desiredTarget, now);
                              }
                           } else {
                              rec.rangedBowReadyAtMillis = 0L;
                              rec.rangedBowItemId = null;

                              try {
                                 if (rec.refObj instanceof Ref) {
                                    Ref<EntityStore> npcRef = (Ref)rec.refObj;
                                    this.applySwordWeaponNow(store, npcRef, rec.ownerId, rec, false);
                                 }
                              } catch (Throwable var65) {
                              }

                              rec.currentTargetMobLevel = 0;
                              rec.currentTargetMobUuid = null;
                           }
                        }
                     }
                  } catch (Throwable var80) {
                  }

                  boolean inCombatOrAssistNow = now < rec.combatUntilMillis || now < rec.assistUntilMillis || now < rec.npcCombatUntilMillis || rec.combatTargetRefObj != null || rec.assistTargetRefObj != null || rec.npcCombatTargetRefObj != null;

                  try {
                     if (inCombatOrAssistNow) {
                        rec.regenWasInCombat = true;
                        rec.regenStartAtMillis = 0L;
                        rec.regenLastApplyMillis = 0L;
                     } else if (rec.regenWasInCombat) {
                        rec.regenWasInCombat = false;
                        if (!rec.godMode && !rec.downed) {
                           rec.regenStartAtMillis = now + 3000L;
                           rec.regenLastApplyMillis = 0L;
                        }

                        try {
                           if (rec.autoLootEnabled && rec.state == AmigoNpcManager.State.ACTIVE && !rec.downed) {
                              Vector3d anchorPos = op != null ? op : np;
                              if (rec.autoLootStickAnchorPos == null && anchorPos != null) {
                                 rec.autoLootStickAnchorPos = anchorPos;
                              }

                              rec.autoLootStickUntilMillis = Math.max(rec.autoLootStickUntilMillis, now + 900L);
                              rec.autoLootStickDeadRefObj = null;
                           }
                        } catch (Throwable var63) {
                        }
                     }

                     this.tickSpawnFollowFx(store, rec, ownerRef, now);
                     this.tickAutoRegen(store, rec, ownerRef, now);
                  } catch (Throwable var64) {
                  }

                  if (!rec.autoLootEnabled) {
                     endCombatTaggedLooting(rec);
                  } else {
                     boolean pauseTaggedLootNow = now < rec.combatUntilMillis || now < rec.assistUntilMillis;
                     this.tickCombatTaggedLooting(rawStore, owner, rec, ownerRef, np, op, now, worldObj, pauseTaggedLootNow);
                     this.tryAutoLoot(rawStore, owner, rec, np, op, now, worldObj);
                  }

                  boolean stalled = now - rec.lastNpcMovedMillis > 6000L;
                  boolean stalledShort = now - rec.lastNpcMovedMillis > 3500L;
                  boolean followSoft = horizontal > (double)25.0F || Math.abs(dy) > (double)12.0F;
                  boolean followHard = horizontal > (double)35.0F || Math.abs(dy) > (double)18.0F;
                  boolean farHard = horizontal > (double)120.0F || Math.abs(dy) > (double)30.0F;
                  boolean farSoft = horizontal > (double)70.0F || Math.abs(dy) > (double)20.0F;
                  boolean shouldRescue = false;
                  if (rec.farSinceMillis > 0L) {
                     long limit = inCombatOrAssistNow ? 1500L : 2500L;
                     if (now - rec.farSinceMillis > limit) {
                        shouldRescue = true;
                     }
                  }

                  if (!inCombatOrAssistNow) {
                     shouldRescue = shouldRescue || followHard || followSoft && stalledShort;
                  }

                  if (!shouldRescue) {
                     shouldRescue = farHard || farSoft && stalled;
                  }

                  double forcedTeleportDist = ownerUnderground ? (double)10.0F : (double)35.0F;
                  shouldRescue = shouldRescue || horizontal > forcedTeleportDist;
                  if (shouldRescue && now - rec.lastTeleportMillis > 1000L) {
                     rec.lastTeleportMillis = now;
                     rec.combatUntilMillis = 0L;
                     rec.combatTargetRefObj = null;
                     rec.npcCombatUntilMillis = 0L;
                     rec.npcCombatTargetRefObj = null;
                     rec.assistUntilMillis = now + 3000L;
                     rec.assistTargetRefObj = null;
                     rec.chaseDisengaged = true;
                     rec.targetLostSinceMillis = 0L;
                     rec.targetStuckSinceMillis = 0L;
                     rec.lastTargetHorizontal = (double)-1.0F;
                     rec.lastTargetSampleMillis = 0L;
                     rec.currentTargetMobLevel = 0;
                     rec.currentTargetMobUuid = null;

                     try {
                        Float yaw = null;

                        try {
                           yaw = extractYawFromRotation(ot.getRotation());
                        } catch (Throwable var61) {
                        }

                        Object tposObj = offsetInFrontOfYaw(op, yaw, (double)4.0F);
                        Vector3d tpos = tposObj instanceof Vector3d ? (Vector3d)tposObj : (Vector3d)coerceToVector3d(tposObj);
                        if (tpos != null) {
                           nt.teleportPosition(tpos);
                        }
                     } catch (Throwable var62) {
                     }

                     try {
                        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                           setLockedTargetOnNpcEntity(npcEntityObj, ownerRef);
                           setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", (Object)null);
                        }
                     } catch (Throwable var60) {
                     }

                     String var10003 = String.format(Locale.US, "%.2f", horizontal);
                     this.debugCombat(rec, owner, "hardTeleport: h=" + var10003 + " dy=" + String.format(Locale.US, "%.2f", dy));
                  }
               } catch (Throwable var81) {
               }

            });
         }
      }

   }

   private static void applyMovementAnimation(Store store, Ref npcRef, MovementStates ownerStates, double ownerSpeed, double npcSpeed, boolean moving, double distance) {
      try {
         MovementStatesComponent msComp = (MovementStatesComponent)store.ensureAndGetComponent(npcRef, MovementStatesComponent.getComponentType());
         MovementStates s = ownerStates != null ? new MovementStates(ownerStates) : new MovementStates();
         s.onGround = true;
         if (moving && !(npcSpeed < 0.2)) {
            boolean ownerSprint = ownerStates != null && ownerStates.sprinting;
            boolean ownerRun = ownerStates != null && (ownerStates.running || ownerStates.sprinting);
            boolean wantSprint = ownerSprint || ownerSpeed > (double)6.0F || distance > (double)35.0F;
            boolean wantRun = ownerRun || ownerSpeed > 4.2 || distance > (double)25.0F;
            if (npcSpeed < 1.2) {
               wantSprint = false;
               wantRun = false;
            }

            s.idle = false;
            s.horizontalIdle = false;
            s.walking = !wantRun && !wantSprint;
            s.running = wantRun && !wantSprint;
            s.sprinting = wantSprint;
         } else {
            s.idle = true;
            s.horizontalIdle = true;
            s.walking = false;
            s.running = false;
            s.sprinting = false;
         }

         msComp.setMovementStates(s);
         msComp.setSentMovementStates(new MovementStates(s));
      } catch (Throwable var16) {
      }

   }

   public void revive(UUID ownerId, boolean manual) {
      NpcRecord rec = ownerId == null ? null : (NpcRecord)this.npcRefPorPlayer.get(ownerId);
      if (rec != null) {
         if (rec.refObj != null) {
            if (rec.worldObj != null) {
               rec.downed = false;
               rec.downedUntilMillis = 0L;
               HytaleBridge.worldExecute(rec.worldObj, () -> {
                  try {
                     Object componentStore = getComponentStoreFromWorld(rec.worldObj);
                     if (!(componentStore instanceof Store)) {
                        return;
                     }

                     Store<?> rawStore = (Store)componentStore;
                     Ref<EntityStore> ref = (Ref)rec.refObj;
                     EntityStatMap stats = (EntityStatMap)rawStore.ensureAndGetComponent(ref, EntityStatMap.getComponentType());
                     stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                     this.applyNpcScaling(rawStore, ref, ownerId, rec, true);
                     rawStore.ensureComponent(ref, ActiveAnimationComponent.getComponentType());
                     rawStore.ensureComponent(ref, MovementStatesComponent.getComponentType());
                     rawStore.putComponent(ref, RespondToHit.getComponentType(), RespondToHit.INSTANCE);
                  } catch (Throwable var8) {
                  }

               });
            }
         }
      }
   }

   private static Object getComponentStoreFromWorld(Object worldObj) {
      Object entityStore = invokeNoArg(worldObj, "getEntityStore", "entityStore");
      return entityStore == null ? null : invokeNoArg(entityStore, "getStore", "store");
   }

   private static Object tryGetPositionFromStore(Object componentStore, Object playerEntityRef) {
      try {
         Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
         Method getCt = tcClass.getMethod("getComponentType");
         Object componentType = getCt.invoke((Object)null);
         if (componentType == null) {
            return null;
         } else {
            Object tc = invokeStoreGetComponent(componentStore, playerEntityRef, componentType);
            return tc == null ? null : invokeNoArg(tc, "getPosition", "position");
         }
      } catch (Throwable var6) {
         return null;
      }
   }

   private static Object tryGetOwnerPositionFromWorldStore(Object worldObj, Object componentStore, UUID ownerId) {
      try {
         Object ref = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
         if (ref == null) {
            return null;
         } else {
            Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
            Method getCt = tcClass.getMethod("getComponentType");
            Object componentType = getCt.invoke((Object)null);
            if (componentType == null) {
               return null;
            } else {
               Object tc = invokeStoreGetComponent(componentStore, ref, componentType);
               return tc == null ? null : invokeNoArg(tc, "getPosition", "position");
            }
         }
      } catch (Throwable var8) {
         return null;
      }
   }

   private static Object invokeStoreGetComponent(Object store, Object ref, Object componentType) {
      try {
         for(Method m : store.getClass().getMethods()) {
            if (m.getName().equals("getComponent") && m.getParameterCount() == 2) {
               return m.invoke(store, ref, componentType);
            }
         }
      } catch (Throwable var7) {
      }

      return null;
   }

   private static Object getComponentFromStore(Object store, Object ref, Object componentTypeObj) {
      return invokeStoreGetComponent(store, ref, componentTypeObj);
   }

   private static Object invokeSpawnNPC(Object npcPlugin, Object store, String npcType, String groupType, Object pos, Object rot) {
      try {
         for(Method m : npcPlugin.getClass().getMethods()) {
            if (m.getName().equals("spawnNPC") && m.getParameterCount() == 5) {
               try {
                  return m.invoke(npcPlugin, store, npcType, groupType, pos, rot);
               } catch (IllegalArgumentException var11) {
               }
            }
         }
      } catch (Throwable var12) {
      }

      return null;
   }

   private static int getNpcRoleIndex(Object npcPlugin, String roleName) {
      try {
         Method m = npcPlugin.getClass().getMethod("getIndex", String.class);
         Object r = m.invoke(npcPlugin, roleName);
         return r instanceof Integer ? (Integer)r : ((Number)r).intValue();
      } catch (Throwable var4) {
         return -1;
      }
   }

   private static int getNpcRoleIndexWithFallbacks(Object npcPlugin, String roleName) {
      if (roleName != null && !roleName.isBlank()) {
         String[] candidates = new String[]{roleName, roleName.endsWith(".json") ? roleName.substring(0, roleName.length() - 5) : roleName, "_Core/" + roleName, "_Core/AmigoNPC/" + roleName, "Server/NPC/Roles/" + roleName, "Server/NPC/Roles/_Core/" + roleName, "Server/NPC/Roles/_Core/AmigoNPC/" + roleName};

         for(String c : candidates) {
            if (c != null && !c.isBlank()) {
               String key = c.endsWith(".json") ? c.substring(0, c.length() - 5) : c;
               int idx = getNpcRoleIndex(npcPlugin, key);
               if (idx >= 0) {
                  return idx;
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private static Object invokeSpawnEntity(Object npcPlugin, Object store, int roleIndex, Object pos, Object rot, Object model, Object ownerRef) {
      try {
         for(Method m : npcPlugin.getClass().getMethods()) {
            if (m.getName().equals("spawnEntity") && m.getParameterCount() == 7) {
               try {
                  Object pre = buildPreAddToWorldTriConsumer();
                  Object post = buildPostSpawnTriConsumer(ownerRef);
                  return m.invoke(npcPlugin, store, roleIndex, pos, rot, model, pre, post);
               } catch (IllegalArgumentException var13) {
               }
            }
         }
      } catch (Throwable var14) {
      }

      return null;
   }

   private static Object buildPostSpawnTriConsumer(Object ownerRef) {
      if (ownerRef == null) {
         return buildNoopTriConsumer();
      } else {
         try {
            Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
            return Proxy.newProxyInstance(tri.getClassLoader(), new Class[]{tri}, (proxy, method, args) -> {
               try {
                  if (args != null && args.length >= 1 && args[0] != null) {
                     Object npcEntity = args[0];
                     setLockedTargetOnNpcEntity(npcEntity, ownerRef);
                  }
               } catch (Throwable var5) {
               }

               return null;
            });
         } catch (Throwable var2) {
            return buildNoopTriConsumer();
         }
      }
   }

   private static void setMarkedTargetOnNpcEntity(Object npcEntity, String slot, Object targetRef) {
      if (npcEntity != null && slot != null) {
         try {
            Object role = invokeNoArg(npcEntity, "getRole", "role");
            if (role == null) {
               return;
            }

            if (invokeTwoArgs(npcEntity, "onFlockSetTarget", String.class, Object.class, slot, targetRef)) {
               return;
            }

            if (invokeTwoArgs(npcEntity, "onFlockSetMarkedTarget", String.class, Object.class, slot, targetRef)) {
               return;
            }

            if (invokeTwoArgs(npcEntity, "onFlockSetMarkedEntity", String.class, Object.class, slot, targetRef)) {
               return;
            }

            if (invokeTwoArgs(role, "setMarkedTarget", String.class, Object.class, slot, targetRef)) {
               return;
            }

            if (invokeTwoArgs(role, "setMarkedEntity", String.class, Object.class, slot, targetRef)) {
               return;
            }

            if (invokeTwoArgs(role, "markEntity", String.class, Object.class, slot, targetRef)) {
               return;
            }

            if (targetRef == null) {
               invokeOneArg(npcEntity, "onFlockClearTarget", String.class, slot);
               invokeOneArg(npcEntity, "onFlockRemoveTarget", String.class, slot);
               invokeOneArg(role, "clearMarkedTarget", String.class, slot);
               invokeOneArg(role, "removeMarkedTarget", String.class, slot);
               invokeOneArg(role, "clearMarkedEntity", String.class, slot);
               invokeOneArg(role, "removeMarkedEntity", String.class, slot);
               invokeOneArg(role, "unmarkEntity", String.class, slot);
            }
         } catch (Throwable var4) {
         }

      }
   }

   private void updateCurrentTargetMobLevel(Store store, NpcRecord rec, Object desiredTargetRefObj) {
      if (store != null && rec != null) {
         if (desiredTargetRefObj instanceof Ref && rec.refObj instanceof Ref) {
            try {
               Ref<EntityStore> tgtRef = (Ref)desiredTargetRefObj;
               String instanceId = null;

               try {
                  Object n = invokeNoArg(rec.worldObj, "getName");
                  if (n instanceof String) {
                     String s = (String)n;
                     if (!s.isBlank()) {
                        instanceId = s;
                     }
                  }
               } catch (Throwable var8) {
               }

               MobLevelVarianceCalculator.Result r = ZoneMobService.getShared().computeMobLevel(store, tgtRef, rec.worldObj, rec.zoneRawId, 0, 0, instanceId);
               rec.currentTargetMobLevel = r.finalLevel();
               rec.currentTargetMobUuid = null;
               if (rec.currentTargetMobLevel > 0) {
                  int inferred = ZoneModel.inferZoneForMobLevel(rec.currentTargetMobLevel);
                  rec.zoneInferredId = inferred;
                  rec.zoneInferredFromMobLevel = rec.currentTargetMobLevel;
                  rec.zoneInferredAtMillis = System.currentTimeMillis();
               }

               int var10003 = rec.zoneRawId;
               this.debugMobLevel(rec, rec.ownerId, "rawZoneId=" + var10003 + " inferredZone=" + rec.zoneInferredId + " mobLevelFinal=" + rec.currentTargetMobLevel + " | base=" + r.baseLevel() + " offset=" + r.offset() + " final=" + r.finalLevel() + " cap=" + r.maxLevelCap() + " cacheHit=" + r.cacheHit());
            } catch (Throwable var9) {
               rec.currentTargetMobLevel = 0;
               rec.currentTargetMobUuid = null;
            }

         } else {
            rec.currentTargetMobLevel = 0;
            rec.currentTargetMobUuid = null;
         }
      }
   }

   private static void setLockedTargetOnNpcEntity(Object npcEntity, Object ownerRef) {
      if (npcEntity != null && ownerRef != null) {
         setMarkedTargetOnNpcEntity(npcEntity, "LockedTarget", ownerRef);
      }
   }

   private static void setRoleStateOnNpcEntity(Object npcEntity, Object npcRefObj, Object storeObj, boolean combat) {
      if (npcEntity != null && npcRefObj != null && storeObj != null) {
         String state = combat ? "Combat" : "Idle";
         String sub = "Default";

         try {
            Object role = invokeNoArg(npcEntity, "getRole", "role");
            if (role == null) {
               return;
            }

            Object stateSupport = invokeNoArg(role, "getStateSupport", "stateSupport");
            if (stateSupport == null) {
               return;
            }

            for(Method m : stateSupport.getClass().getMethods()) {
               if (m.getName().equals("setState") && m.getParameterCount() == 4) {
                  Class<?>[] p = m.getParameterTypes();
                  if (p.length == 4 && p[1] == String.class && p[2] == String.class) {
                     try {
                        m.invoke(stateSupport, npcRefObj, state, "Default", storeObj);
                        return;
                     } catch (IllegalArgumentException var14) {
                     }
                  }
               }
            }

            for(Method m : stateSupport.getClass().getMethods()) {
               if (m.getName().equals("flockSetState") && m.getParameterCount() == 4) {
                  try {
                     m.invoke(stateSupport, npcRefObj, state, "Default", storeObj);
                     return;
                  } catch (Throwable var15) {
                  }
               }
            }
         } catch (Throwable var16) {
         }

      }
   }

   private static void setFlockState(Store store, Object npcRefObj, String state, String subState) {
   }

   private static String resolveDefaultBowId() {
      String cached = RESOLVED_BOW_ID;
      if (cached != null) {
         return cached.isBlank() ? null : cached;
      } else {
         String found = null;
         String[] candidates = new String[]{"Weapon_Shortbow_Onyxium", "Weapon_Shortbow", "Weapon_Bow_Crude", "Weapon_Bow_Wood", "Weapon_Bow_Iron", "Weapon_Bow_Steel", "Weapon_Bow_Mithril", "Weapon_Bow_Thorium", "Weapon_Bow_Onyxium", "Weapon_Bow", "Weapon_Ranged_Bow"};

         try {
            for(String id : candidates) {
               try {
                  if (id != null && !id.isBlank()) {
                     Item it = (Item)Item.getAssetMap().getAsset(id);
                     if (it != null) {
                        found = id;
                        break;
                     }
                  }
               } catch (Throwable var8) {
               }
            }
         } catch (Throwable var9) {
         }

         RESOLVED_BOW_ID = found == null ? "" : found;
         return found;
      }
   }

   private static boolean isAirborneTarget(Store store, Ref targetRef, Vector3d npcPos) {
      try {
         if (store != null && targetRef != null && npcPos != null) {
            TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
            if (tgtT != null && tgtT.getPosition() != null) {
               Vector3d tp = tgtT.getPosition();
               double dy = tp.getY() - npcPos.getY();
               double dyAbs = Math.abs(dy);
               MovementStatesComponent ms = (MovementStatesComponent)store.getComponent(targetRef, MovementStatesComponent.getComponentType());
               if (ms != null) {
                  MovementStates s = ms.getMovementStates();
                  if (s != null) {
                     if (!s.flying && !s.gliding) {
                        if (!s.onGround) {
                           if (dyAbs >= 1.2) {
                              return true;
                           }

                           if (s.jumping || s.falling) {
                              return true;
                           }
                        }

                        return false;
                     }

                     return true;
                  }
               }

               return dyAbs >= (double)2.0F;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } catch (Throwable var11) {
         return false;
      }
   }

   private void aimNpcAtTarget(Store store, Ref npcRef, Ref targetRef) {
      if (store != null && npcRef != null && targetRef != null) {
         TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
         if (npcT != null && tgtT != null) {
            Vector3d np = npcT.getPosition();
            Vector3d tp = tgtT.getPosition();
            if (np != null && tp != null) {
               double npcH = Math.max(1.2, getEntityHeight(store, npcRef));
               double tgtH = Math.max(0.6, getEntityHeight(store, targetRef));
               double eyeY = np.getY() + npcH * 0.85;
               double aimY = tp.getY() + tgtH * 0.55;
               double dx = tp.getX() - np.getX();
               double dz = tp.getZ() - np.getZ();
               double dy = aimY - eyeY;
               double h = Math.sqrt(dx * dx + dz * dz);
               if (!(h < 1.0E-4)) {
                  float yaw = (float)Math.toDegrees(Math.atan2(-dx, -dz));
                  float pitch = (float)(-Math.toDegrees(Math.atan2(dy, h)));
                  Object rot = invokeNoArg(npcT, "getRotation", "rotation");
                  if (rot == null) {
                     rot = newVector3f(0.0F, 0.0F, 0.0F);
                  }

                  if (rot != null) {
                     boolean ok = false;

                     try {
                        invokeOneArg(rot, "setYaw", Float.TYPE, yaw);
                        invokeOneArg(rot, "setPitch", Float.TYPE, pitch);
                        ok = true;
                     } catch (Throwable var31) {
                     }

                     if (!ok) {
                        try {
                           invokeOneArg(rot, "setX", Float.TYPE, pitch);
                           invokeOneArg(rot, "setY", Float.TYPE, yaw);
                        } catch (Throwable var30) {
                        }
                     }

                     try {
                        invokeTeleportRotation(npcT, rot);
                     } catch (Throwable var29) {
                     }

                  }
               }
            }
         }
      }
   }

   private static void invokeTeleportRotation(Object transformComponent, Object rotationObj) {
      if (transformComponent != null && rotationObj != null) {
         Class<?> rc = rotationObj.getClass();

         try {
            Method m = transformComponent.getClass().getMethod("teleportRotation", rc);
            m.invoke(transformComponent, rotationObj);
         } catch (Throwable var5) {
            try {
               Method m = transformComponent.getClass().getMethod("setRotation", rc);
               m.invoke(transformComponent, rotationObj);
            } catch (Throwable var4) {
            }

         }
      }
   }

   private void tryEquipDefaultBow(Store store, NpcRecord rec, long now) {
      try {
         if (store == null || rec == null) {
            return;
         }

         if (!(rec.refObj instanceof Ref)) {
            return;
         }

         String bowId = resolveDefaultBowId();
         if (bowId == null || bowId.isBlank()) {
            return;
         }

         rec.rangedBowItemId = bowId;
         Ref<EntityStore> npcRef = (Ref)rec.refObj;
         if (this.isHotbar0Item(store, npcRef, bowId)) {
            if (rec.rangedBowReadyAtMillis <= 0L) {
               rec.rangedBowReadyAtMillis = now;
            }

            return;
         }

         boolean ok = this.equipWeaponInHotbar0(store, npcRef, bowId);
         if (ok) {
            rec.rangedBowReadyAtMillis = now + 900L;
         }
      } catch (Throwable var8) {
      }

   }

   private void tryRangedAttack(Store store, NpcRecord rec, Object ownerRefObj, Object targetRefObj, long now) {
      try {
         if (store == null || rec == null) {
            return;
         }

         if (rec.downed) {
            return;
         }

         if (targetRefObj == null || ownerRefObj == null) {
            return;
         }

         if (targetRefObj == ownerRefObj) {
            return;
         }

         boolean isOwnerAggressor = rec.combatTargetRefObj != null && refEq(targetRefObj, rec.combatTargetRefObj);
         boolean isNpcAggressor = rec.npcCombatTargetRefObj != null && refEq(targetRefObj, rec.npcCombatTargetRefObj);
         boolean isAggressor = isOwnerAggressor || isNpcAggressor;
         boolean isAssist = rec.assistTargetRefObj != null && refEq(targetRefObj, rec.assistTargetRefObj);
         if (!rec.defendeEnabled) {
            if (!isAggressor) {
               return;
            }
         } else if (!isAggressor && !isAssist) {
            return;
         }

         if (now - rec.lastRangedAttackMillis < 1150L) {
            return;
         }

         if (rec.rangedBowReadyAtMillis > 0L && now < rec.rangedBowReadyAtMillis) {
            return;
         }

         if (!(targetRefObj instanceof Ref)) {
            return;
         }

         if (!(rec.refObj instanceof Ref)) {
            return;
         }

         Ref<EntityStore> targetRef = (Ref)targetRefObj;
         Ref<EntityStore> npcRef = (Ref)rec.refObj;
         String bowId = rec.rangedBowItemId;
         if (bowId == null || bowId.isBlank()) {
            bowId = resolveDefaultBowId();
         }

         if (bowId == null || bowId.isBlank()) {
            return;
         }

         if (!this.isHotbar0Item(store, npcRef, bowId)) {
            rec.rangedBowReadyAtMillis = Math.max(rec.rangedBowReadyAtMillis, now + 200L);
            return;
         }

         String bowIdCheck = rec.rangedBowItemId != null && !rec.rangedBowItemId.isBlank() ? rec.rangedBowItemId : resolveDefaultBowId();
         if (bowIdCheck != null && !bowIdCheck.isBlank() && !this.isHotbar0Item(store, npcRef, bowIdCheck)) {
            this.tryEquipDefaultBow(store, rec, now);
            return;
         }

         try {
            Player maybePlayer = (Player)store.getComponent(targetRef, Player.getComponentType());
            if (maybePlayer != null && !this.pvpEnabled) {
               return;
            }
         } catch (Throwable var58) {
         }

         TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
         if (npcT == null || tgtT == null) {
            return;
         }

         Vector3d np = npcT.getPosition();
         Vector3d tp = tgtT.getPosition();
         if (np == null || tp == null) {
            return;
         }

         if (!isAirborneTarget(store, targetRef, np)) {
            return;
         }

         double dx = tp.getX() - np.getX();
         double dz = tp.getZ() - np.getZ();
         double dy = tp.getY() - np.getY();
         double dyAbs = Math.abs(dy);
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         if (horizontal > (double)20.0F) {
            return;
         }

         if (dyAbs > (double)35.0F) {
            return;
         }

         try {
            this.aimNpcAtTarget(store, npcRef, targetRef);
         } catch (Throwable var57) {
         }

         String weaponId = rec.equippedWeaponId;
         if (weaponId == null || weaponId.isBlank()) {
            weaponId = br.tones.amigonpc.core.swords.SwordProgression.weaponIdForLevel(rec.level);
         }

         double base = getWeaponBaseDamage(weaponId);
         float amount = (float)(base + (double)rec.level * 0.03);
         int swordLvl = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
         if (swordLvl <= 30) {
            amount /= 3.0F;
         } else if (swordLvl <= 60) {
            amount /= 2.0F;
         }

         try {
            amount = AttributeModifierService.getShared().applyOutgoingDamageMods(rec.ownerId, amount, Math.max(1, rec.npcLevelCached));
         } catch (Throwable var56) {
         }

         amount = Math.min(24.0F, amount * 1.2F);
         float hpBefore = -1.0F;

         try {
            EntityStatMap tgtStats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (tgtStats != null) {
               hpBefore = tgtStats.get(DefaultEntityStatTypes.getHealth()).get();
            }
         } catch (Throwable var55) {
         }

         this.recordCombatTag(rec.ownerId, targetRefObj, store);

         try {
            playWeaponAttackAnimation(store, npcRef, rec, bowId, now);
         } catch (Throwable var54) {
         }

         try {
            if (npcT != null && npcT.getPosition() != null) {
               playSfx3d(store, npcT.getPosition(), "SFX_Bow_T2_Shoot", 10.0F, 12.0F);
            }
         } catch (Throwable var53) {
         }

         Damage damage = new Damage(new Damage.EntitySource(npcRef), DamageCause.PHYSICAL, amount);
         DamageSystems.executeDamage(targetRef, store, damage);

         try {
            this.debugAttack(rec, rec.ownerId, String.format("RANGED HIT! dmg=%.2f bow=%s h=%.2f dy=%.2f", amount, String.valueOf(bowId), horizontal, dy));
         } catch (Throwable var52) {
         }

         this.pulseAggroToNpc(store, targetRefObj, npcRef, rec, now);
         rec.lastRangedAttackMillis = now;
         boolean killed = false;

         try {
            EntityStatMap tgtStats2 = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (tgtStats2 != null && hpBefore > 0.0F) {
               float hpAfter = tgtStats2.get(DefaultEntityStatTypes.getHealth()).get();
               killed = hpAfter <= 0.0F;
            }
         } catch (Throwable var51) {
         }

         int mobLevelForCtx = rec.currentTargetMobLevel;
         long xpGain;
         if (killed) {
            int npcLevel = Math.max(1, XpProgression.levelFromTotalXp(rec.totalXp));
            int mobLevel = RpgStage1MobLevelHelper.getMonsterLevel(store, targetRef, targetRefObj, RPG_STAGE1_MOB_CALC, RPG_STAGE1_CFG);
            mobLevelForCtx = mobLevel;
            double xpD = RpgStage1Formulas.xpFromKill(npcLevel, mobLevel, RPG_STAGE1_CFG);
            xpGain = coerceStage1XpGainToLong(rec, xpD);

            try {
               if (AmigoZonesConfigService.get().debugLog) {
                  double baseXp = RpgStage1Formulas.calculateBaseXPFromMonsterLevel(mobLevel, RPG_STAGE1_CFG);
                  double diffMult = RpgStage1Formulas.calculateLevelDiffMultiplier(npcLevel, mobLevel, RPG_STAGE1_CFG);
                  this.debugMobLevel(rec, rec.ownerId, "XP-KILL: npcLevel=" + npcLevel + " mobLevel=" + mobLevel + " base=" + String.format(Locale.ROOT, "%.2f", baseXp) + " diffMult=" + String.format(Locale.ROOT, "%.2f", diffMult) + " rateExp=" + String.format(Locale.ROOT, "%.2f", RPG_STAGE1_CFG.getRateExp()) + " => xp=" + xpGain);
               }
            } catch (Throwable var50) {
            }
         } else {
            xpGain = 0L;
         }

         if (xpGain > 0L) {
            NpcXpSource xpSrc = killed ? NpcXpSource.COMBAT_KILL : NpcXpSource.COMBAT_ASSIST;
            String wn = null;

            try {
               Object n = invokeNoArg(rec.worldObj, "getName");
               if (n instanceof String) {
                  String s = (String)n;
                  if (!s.isBlank()) {
                     wn = s;
                  }
               }
            } catch (Throwable var49) {
            }

            int zid = this.getZoneForHud(rec.ownerId);
            String mobId = rec.currentTargetMobUuid != null ? rec.currentTargetMobUuid.toString() : null;
            NpcXpContext xpCtx = new NpcXpContext(wn, wn, (String)null, zid, mobId, mobLevelForCtx, System.currentTimeMillis());
            this.addNpcXp(store, npcRef, rec.ownerId, rec, xpGain, xpSrc, xpCtx);
         }

         if (killed) {
            if (rec.autoLootEnabled) {
               rec.autoLootStickUntilMillis = Math.max(rec.autoLootStickUntilMillis, now + 900L);
               rec.autoLootStickDeadRefObj = targetRefObj;

               try {
                  if (targetRefObj instanceof Ref) {
                     Ref<EntityStore> tgtRef3 = (Ref)targetRefObj;
                     TransformComponent tgtT3 = (TransformComponent)store.getComponent(tgtRef3, TransformComponent.getComponentType());
                     if (tgtT3 != null && tgtT3.getPosition() != null) {
                        rec.autoLootStickAnchorPos = tgtT3.getPosition();
                     }
                  }

                  if (rec.autoLootStickAnchorPos == null && npcT != null && npcT.getPosition() != null) {
                     rec.autoLootStickAnchorPos = npcT.getPosition();
                  }
               } catch (Throwable var48) {
               }
            }

            this.sendToOwner(rec.worldObj, rec.ownerId, "Recebi " + xpGain + " de xp.");
            this.tryRetargetAfterKill(store, rec, ownerRefObj, npcRef, targetRefObj, now);
         }
      } catch (Throwable var59) {
      }

   }

   private void tryMeleeAttack(Store store, NpcRecord rec, Object ownerRefObj, Object targetRefObj, long now) {
      try {
         if (store == null || rec == null) {
            return;
         }

         if (rec.downed) {
            return;
         }

         if (targetRefObj == null || ownerRefObj == null) {
            return;
         }

         if (targetRefObj == ownerRefObj) {
            return;
         }

         boolean isOwnerAggressor = rec.combatTargetRefObj != null && refEq(targetRefObj, rec.combatTargetRefObj);
         boolean isNpcAggressor = rec.npcCombatTargetRefObj != null && refEq(targetRefObj, rec.npcCombatTargetRefObj);
         boolean isAggressor = isOwnerAggressor || isNpcAggressor;
         boolean isAssist = rec.assistTargetRefObj != null && refEq(targetRefObj, rec.assistTargetRefObj);
         if (!rec.defendeEnabled) {
            if (!isAggressor) {
               return;
            }
         } else if (!isAggressor && !isAssist) {
            return;
         }

         long cd = isAggressor ? 650L : 850L;
         if (now - rec.lastMeleeAttackMillis < cd) {
            return;
         }

         UUID ownerId = rec.ownerId;
         if (!(targetRefObj instanceof Ref)) {
            return;
         }

         if (!(rec.refObj instanceof Ref)) {
            return;
         }

         Ref<EntityStore> targetRef = (Ref)targetRefObj;
         Ref<EntityStore> npcRef = (Ref)rec.refObj;

         try {
            Player maybePlayer = (Player)store.getComponent(targetRef, Player.getComponentType());
            if (maybePlayer != null && !this.pvpEnabled) {
               this.debugAttack(rec, ownerId, "Bloqueado PvP (alvo é player). Arma=" + rec.equippedWeaponId);
               return;
            }
         } catch (Throwable var62) {
         }

         TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
         if (npcT == null || tgtT == null) {
            this.debugAttack(rec, ownerId, "Sem TransformComponent (npcT=" + (npcT != null) + ", tgtT=" + (tgtT != null) + ").");
            return;
         }

         Vector3d np = npcT.getPosition();
         Vector3d tp = tgtT.getPosition();
         if (np == null || tp == null) {
            this.debugAttack(rec, ownerId, "Sem posição (np=" + (np != null) + ", tp=" + (tp != null) + ").");
            return;
         }

         double dx = tp.getX() - np.getX();
         double dz = tp.getZ() - np.getZ();
         double dy = tp.getY() - np.getY();
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         double targetHeight = getEntityHeight(store, targetRef);
         boolean tallTarget = targetHeight >= 1.9;
         double allowedDy = tallTarget ? (double)3.0F : (double)2.0F;
         double targetRadius = getEntityRadiusXZ(store, targetRef);
         double effectiveH = Math.max((double)0.0F, horizontal - targetRadius);
         double reach = 2.6;
         if (effectiveH > reach) {
            this.debugAttack(rec, ownerId, String.format("Fora do alcance: h=%.2f eff=%.2f r=%.2f dy=%.2f arma=%s", horizontal, effectiveH, targetRadius, dy, String.valueOf(rec.equippedWeaponId)));
            return;
         }

         if (Math.abs(dy) > allowedDy) {
            this.debugAttack(rec, ownerId, String.format("Diferença de altura alta: h=%.2f dy=%.2f allowedDy=%.2f arma=%s", horizontal, dy, allowedDy, String.valueOf(rec.equippedWeaponId)));
            return;
         }

         String weaponId = rec.equippedWeaponId;
         if (weaponId == null || weaponId.isBlank()) {
            weaponId = br.tones.amigonpc.core.swords.SwordProgression.weaponIdForLevel(rec.level);
         }

         double base = getWeaponBaseDamage(weaponId);
         float amount = (float)Math.min((double)20.0F, base + (double)rec.level * 0.03);
         int swordLvl = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
         if (swordLvl <= 30) {
            amount /= 3.0F;
         } else if (swordLvl <= 60) {
            amount /= 2.0F;
         }

         try {
            amount = AttributeModifierService.getShared().applyOutgoingDamageMods(rec.ownerId, amount, Math.max(1, rec.npcLevelCached));
         } catch (Throwable var61) {
         }

         float hpBefore = -1.0F;

         try {
            EntityStatMap tgtStats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (tgtStats != null) {
               hpBefore = tgtStats.get(DefaultEntityStatTypes.getHealth()).get();
            }
         } catch (Throwable var60) {
         }

         this.recordCombatTag(ownerId, targetRefObj, store);
         Damage damage = new Damage(new Damage.EntitySource(npcRef), DamageCause.PHYSICAL, amount);

         try {
            Damage.Particles particles = new Damage.Particles((ModelParticle[])null, new WorldParticle[]{new WorldParticle("Impact_Sword_Basic", (Color)null, 1.0F, (Vector3f)null, (Direction)null)}, (double)64.0F);
            damage.putMetaObject(Damage.IMPACT_PARTICLES, particles);
         } catch (Throwable var59) {
         }

         try {
            if (tgtT != null && tgtT.getPosition() != null) {
               playSfx3d(store, tgtT.getPosition(), "SFX_Longsword_Special_Impact", 10.0F, 12.0F);
            }
         } catch (Throwable var58) {
         }

         DamageSystems.executeDamage(targetRef, store, damage);
         this.pulseAggroToNpc(store, targetRefObj, npcRef, rec, now);
         boolean killed = false;

         try {
            EntityStatMap tgtStats2 = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (tgtStats2 != null && hpBefore > 0.0F) {
               float hpAfter = tgtStats2.get(DefaultEntityStatTypes.getHealth()).get();
               killed = hpAfter <= 0.0F;
            }
         } catch (Throwable var57) {
         }

         int mobLevelForCtx = rec.currentTargetMobLevel;
         long xpGain;
         if (killed) {
            int npcLevel = Math.max(1, XpProgression.levelFromTotalXp(rec.totalXp));
            int mobLevel = RpgStage1MobLevelHelper.getMonsterLevel(store, targetRef, targetRefObj, RPG_STAGE1_MOB_CALC, RPG_STAGE1_CFG);
            mobLevelForCtx = mobLevel;
            double xpD = RpgStage1Formulas.xpFromKill(npcLevel, mobLevel, RPG_STAGE1_CFG);
            xpGain = coerceStage1XpGainToLong(rec, xpD);
         } else {
            xpGain = 0L;
         }

         if (xpGain > 0L) {
            NpcXpSource xpSrc = killed ? NpcXpSource.COMBAT_KILL : NpcXpSource.COMBAT_ASSIST;
            String wn = null;

            try {
               Object n = invokeNoArg(rec.worldObj, "getName");
               if (n instanceof String) {
                  String s = (String)n;
                  if (!s.isBlank()) {
                     wn = s;
                  }
               }
            } catch (Throwable var56) {
            }

            int zid = this.getZoneForHud(ownerId);
            String mobId = rec.currentTargetMobUuid != null ? rec.currentTargetMobUuid.toString() : null;
            NpcXpContext xpCtx = new NpcXpContext(wn, wn, (String)null, zid, mobId, mobLevelForCtx, System.currentTimeMillis());
            this.addNpcXp(store, npcRef, ownerId, rec, xpGain, xpSrc, xpCtx);
         }

         if (killed) {
            if (rec.autoLootEnabled) {
               rec.autoLootStickUntilMillis = Math.max(rec.autoLootStickUntilMillis, now + 900L);
               rec.autoLootStickDeadRefObj = targetRefObj;

               try {
                  if (tgtT != null && tgtT.getPosition() != null) {
                     rec.autoLootStickAnchorPos = tgtT.getPosition();
                  }

                  if (rec.autoLootStickAnchorPos == null && npcT != null && npcT.getPosition() != null) {
                     rec.autoLootStickAnchorPos = npcT.getPosition();
                  }
               } catch (Throwable var55) {
               }
            }

            this.sendToOwner(rec.worldObj, ownerId, "Recebi " + xpGain + " de xp.");
            this.tryRetargetAfterKill(store, rec, ownerRefObj, npcRef, targetRefObj, now);
         }

         this.debugAttack(rec, ownerId, String.format("HIT! dmg=%.2f arma=%s h=%.2f dy=%.2f", amount, String.valueOf(rec.equippedWeaponId), horizontal, dy));
         playWeaponAttackAnimation(store, npcRef, rec, weaponId, now);
         rec.lastMeleeAttackMillis = now;
         if (isAggressor) {
            rec.combatUntilMillis = now + 3000L;
         }
      } catch (Throwable var63) {
      }

   }

   private void pulseAggroToNpc(Store store, Object targetRefObj, Object npcRefObj, NpcRecord rec, long now) {
      try {
         if (store == null || targetRefObj == null || npcRefObj == null || rec == null) {
            return;
         }

         if (now - rec.lastAggroPulseMillis < 900L) {
            return;
         }

         rec.lastAggroPulseMillis = now;
         Object mobNpcEntityObj = getComponentFromStore(store, targetRefObj, NPCEntity.getComponentType());
         if (mobNpcEntityObj == null) {
            return;
         }

         setMarkedTargetOnNpcEntity(mobNpcEntityObj, "CombatTarget", npcRefObj);

         try {
            setLockedTargetOnNpcEntity(mobNpcEntityObj, npcRefObj);
         } catch (Throwable var9) {
         }
      } catch (Throwable var10) {
      }

   }

   private void tryRetargetAfterKill(Store store, NpcRecord rec, Object ownerRefObj, Object npcRefObj, Object deadRefObj, long now) {
      try {
         if (store == null || rec == null || !rec.defendeEnabled) {
            return;
         }

         if (!(npcRefObj instanceof Ref) || !(rec.refObj instanceof Ref)) {
            return;
         }

         Ref<EntityStore> npcRef = (Ref)rec.refObj;
         TransformComponent nt = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         if (nt == null || nt.getPosition() == null) {
            return;
         }

         Object next = this.findNearestTargetNearNpc(store, rec, ownerRefObj, nt.getPosition(), deadRefObj);
         if (next == null) {
            return;
         }

         if (rec.combatTargetRefObj != null && refEq(deadRefObj, rec.combatTargetRefObj)) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
         }

         rec.assistTargetRefObj = next;
         rec.assistUntilMillis = 0L;
         Object npcEntityObj = getComponentFromStore(store, npcRefObj, NPCEntity.getComponentType());
         if (npcEntityObj != null) {
            setLockedTargetOnNpcEntity(npcEntityObj, next);
            setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", next);
            setFlockState(store, rec.refObj, "Run", "");
         }
      } catch (Throwable var12) {
      }

   }

   private static void playWeaponAttackAnimation(Store store, Ref npcRef, NpcRecord rec, String weaponId, long now) {
      try {
         if (store == null || npcRef == null || rec == null) {
            return;
         }

         String itemAnimsId = null;
         ItemPlayerAnimations ipa = null;

         try {
            if (weaponId != null && !weaponId.isBlank()) {
               Item it = (Item)Item.getAssetMap().getAsset(weaponId);
               if (it != null) {
                  boolean usePlayerAnims = false;

                  try {
                     usePlayerAnims = it.getUsePlayerAnimations();
                  } catch (Throwable var16) {
                  }

                  try {
                     itemAnimsId = it.getPlayerAnimationsId();
                  } catch (Throwable var15) {
                  }

                  if ((itemAnimsId == null || itemAnimsId.isBlank()) && usePlayerAnims) {
                     try {
                        itemAnimsId = "Default";
                     } catch (Throwable var14) {
                     }
                  }
               }
            }
         } catch (Throwable var21) {
         }

         if (itemAnimsId == null || itemAnimsId.isBlank()) {
            try {
               itemAnimsId = "Default";
            } catch (Throwable var13) {
               itemAnimsId = null;
            }
         }

         try {
            if (itemAnimsId != null && !itemAnimsId.isBlank()) {
               ipa = (ItemPlayerAnimations)ItemPlayerAnimations.getAssetMap().getAsset(itemAnimsId);
            }
         } catch (Throwable var12) {
            ipa = null;
         }

         String key = null;

         try {
            if (ipa != null) {
               boolean isBow = false;

               try {
                  if (weaponId != null) {
                     String lw = weaponId.toLowerCase(Locale.ROOT);
                     isBow = lw.contains("bow") || lw.contains("shortbow");
                  }
               } catch (Throwable var19) {
               }

               if (isBow) {
                  key = pickBowKeyFromPlayerAnimations(ipa.getAnimations());
               }

               if (key == null || key.isBlank()) {
                  key = pickAttackKeyFromPlayerAnimations(ipa.getAnimations());
               }
            }
         } catch (Throwable var20) {
            key = null;
         }

         if (key == null || key.isBlank()) {
            boolean isBow = false;

            try {
               if (weaponId != null) {
                  String lw = weaponId.toLowerCase(Locale.ROOT);
                  isBow = lw.contains("bow") || lw.contains("shortbow");
               }
            } catch (Throwable var18) {
            }

            key = isBow ? "shoot" : "attack";
         }

         boolean played = false;

         try {
            if (itemAnimsId != null && !itemAnimsId.isBlank()) {
               AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, itemAnimsId, key, store);
               played = true;
            } else {
               AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, key, store);
               played = true;
            }
         } catch (Throwable var17) {
            played = false;
         }

         if (played) {
            rec.lastAttackAnimId = key;
            rec.clearAttackAnimAtMillis = now + 650L;
            return;
         }

         try {
            AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, "attack", true, store);
            rec.lastAttackAnimId = "attack";
            rec.clearAttackAnimAtMillis = now + 650L;
         } catch (Throwable var11) {
         }
      } catch (Throwable var22) {
      }

   }

   private static void clearExpiredAttackAnimation(Store store, Object npcRefObj, NpcRecord rec, long now) {
      try {
         if (store == null || rec == null) {
            return;
         }

         if (rec.clearAttackAnimAtMillis <= 0L) {
            return;
         }

         if (now < rec.clearAttackAnimAtMillis) {
            return;
         }

         String last = rec.lastAttackAnimId;
         rec.clearAttackAnimAtMillis = 0L;
         rec.lastAttackAnimId = null;
         if (last == null || last.isBlank()) {
            return;
         }

         if (!(npcRefObj instanceof Ref)) {
            return;
         }

         Ref<EntityStore> npcRef = (Ref)npcRefObj;
         ActiveAnimationComponent anim = (ActiveAnimationComponent)store.getComponent(npcRef, ActiveAnimationComponent.getComponentType());
         if (anim == null) {
            return;
         }

         String[] active = anim.getActiveAnimations();
         int idx = -1;

         try {
            idx = AnimationSlot.Action.getValue();
         } catch (Throwable var13) {
         }

         String current = active != null && idx >= 0 && idx < active.length ? active[idx] : null;
         if (last.equals(current)) {
            try {
               AnimationUtils.stopAnimation(npcRef, AnimationSlot.Action, store);
            } catch (Throwable var12) {
               anim.setPlayingAnimation(AnimationSlot.Action, (String)null);
            }
         }
      } catch (Throwable var14) {
      }

   }

   private static void tickLevelUpFx(Store store, Object ownerRefObj, Object npcRefObj, NpcRecord rec, long now) {
      if (store != null && rec != null && npcRefObj instanceof Ref) {
         if (rec.levelUpFxPendingCount <= 0) {
            rec.levelUpFxUntilMillis = 0L;
            rec.levelUpFxNextTickMillis = 0L;
         } else if (rec.levelUpFxUntilMillis > 0L && now > rec.levelUpFxUntilMillis) {
            rec.levelUpFxPendingCount = 0;
            rec.levelUpFxUntilMillis = 0L;
            rec.levelUpFxNextTickMillis = 0L;
         } else if (rec.levelUpFxNextTickMillis <= 0L || now >= rec.levelUpFxNextTickMillis) {
            try {
               Ref<EntityStore> npcRef = (Ref)npcRefObj;
               TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
               if (tc == null || tc.getPosition() == null) {
                  return;
               }

               spawnParticleToOwner(store, (Object)null, tc.getPosition(), "POTION_MORPH_BURST");
               rec.levelUpFxPendingCount = Math.max(0, rec.levelUpFxPendingCount - 1);
               if (rec.levelUpFxPendingCount > 0) {
                  rec.levelUpFxNextTickMillis = now + 200L;
                  if (rec.levelUpFxUntilMillis <= 0L) {
                     rec.levelUpFxUntilMillis = now + 1000L;
                  }
               } else {
                  rec.levelUpFxUntilMillis = 0L;
                  rec.levelUpFxNextTickMillis = 0L;
               }
            } catch (Throwable var8) {
            }

         }
      }
   }

   private static String resolveWeaponAttackAnimId(String weaponId, boolean moving) {
      if (weaponId != null && !weaponId.isBlank()) {
         String cacheKey = weaponId + (moving ? "|m" : "|s");
         if (WEAPON_ATTACK_ANIM_CACHE.containsKey(cacheKey)) {
            String v = (String)WEAPON_ATTACK_ANIM_CACHE.get(cacheKey);
            return v != null && !v.isBlank() ? v : null;
         } else {
            String foundKey = null;

            try {
               Item it = (Item)Item.getAssetMap().getAsset(weaponId);
               String animsId = null;
               boolean usePlayerAnims = false;
               if (it != null) {
                  try {
                     usePlayerAnims = it.getUsePlayerAnimations();
                  } catch (Throwable var10) {
                  }

                  try {
                     animsId = it.getPlayerAnimationsId();
                  } catch (Throwable var9) {
                  }
               }

               if ((animsId == null || animsId.isBlank()) && usePlayerAnims) {
                  try {
                     animsId = "Default";
                  } catch (Throwable var8) {
                  }
               }

               if (animsId != null && !animsId.isBlank()) {
                  ItemPlayerAnimations ipa = (ItemPlayerAnimations)ItemPlayerAnimations.getAssetMap().getAsset(animsId);
                  if (ipa != null) {
                     foundKey = pickAttackKeyFromPlayerAnimations(ipa.getAnimations());
                  }
               }
            } catch (Throwable var11) {
            }

            WEAPON_ATTACK_ANIM_CACHE.put(cacheKey, foundKey == null ? "" : foundKey);
            return foundKey;
         }
      } else {
         return null;
      }
   }

   private static String pickAttackKeyFromPlayerAnimations(Map map) {
      try {
         if (map != null && !map.isEmpty()) {
            String[] prefer = new String[]{"attack", "primary", "primary_attack", "melee", "melee_attack", "swing", "hit", "strike", "slash", "stab", "chop"};

            for(String p : prefer) {
               for(String k : map.keySet()) {
                  if (k != null && k.equalsIgnoreCase(p)) {
                     return k;
                  }
               }
            }

            for(String k : map.keySet()) {
               if (k != null) {
                  String lk = k.toLowerCase(Locale.ROOT);
                  if (lk.contains("attack") || lk.contains("melee") || lk.contains("swing") || lk.contains("hit") || lk.contains("strike")) {
                     return k;
                  }
               }
            }

            for(String k : map.keySet()) {
               if (k != null && !k.isBlank()) {
                  return k;
               }
            }

            return null;
         } else {
            return null;
         }
      } catch (Throwable var8) {
         return null;
      }
   }

   private static String pickBowKeyFromPlayerAnimations(Map map) {
      try {
         if (map != null && !map.isEmpty()) {
            String[] prefer = new String[]{"shoot", "fire", "release", "use", "primary", "primary_use", "charge", "draw", "aim", "bow", "bow_shoot", "ranged"};

            for(String p : prefer) {
               for(String k : map.keySet()) {
                  if (k != null && k.equalsIgnoreCase(p)) {
                     return k;
                  }
               }
            }

            for(String k : map.keySet()) {
               if (k != null) {
                  String lk = k.toLowerCase(Locale.ROOT);
                  if (lk.contains("shoot") || lk.contains("fire") || lk.contains("use") || lk.contains("charge") || lk.contains("draw") || lk.contains("aim") || lk.contains("bow") || lk.contains("ranged")) {
                     return k;
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      } catch (Throwable var8) {
         return null;
      }
   }

   private static Object buildNoopTriConsumer() {
      try {
         Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
         return Proxy.newProxyInstance(tri.getClassLoader(), new Class[]{tri}, (proxy, method, args) -> null);
      } catch (Throwable var1) {
         return null;
      }
   }

   private static Object buildPreAddToWorldTriConsumer() {
      try {
         Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
         return Proxy.newProxyInstance(tri.getClassLoader(), new Class[]{tri}, (proxy, method, args) -> {
            if (args != null && args.length >= 2) {
               Object patt0$temp = args[1];
               if (patt0$temp instanceof Holder) {
                  Holder holder = (Holder)patt0$temp;

                  try {
                     holder.ensureComponent(EntityStatMap.getComponentType());
                     EntityStatMap stats = (EntityStatMap)holder.ensureAndGetComponent(EntityStatMap.getComponentType());
                     stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                     holder.ensureComponent(ActiveAnimationComponent.getComponentType());
                     holder.ensureComponent(MovementStatesComponent.getComponentType());
                     holder.putComponent(RespondToHit.getComponentType(), RespondToHit.INSTANCE);
                  } catch (Throwable var5) {
                  }
               }
            }

            return null;
         });
      } catch (Throwable var1) {
         return buildNoopTriConsumer();
      }
   }

   private static boolean hasSavedWardrobeCosmetics(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         try {
            return AmigoWardrobePersistence.hasSavedCosmetics(ownerId);
         } catch (Throwable var2) {
            return false;
         }
      }
   }

   private static Object buildPreferredSpawnModel(UUID ownerId, Object storeObj, Object ownerRefObj, String modelId, float scale) {
      if (modelId != null && !modelId.isBlank()) {
         Object model = buildModelFromAssetId(modelId, scale);
         if (model != null) {
            return model;
         }
      }

      return buildModelFromAssetId("PlayerTestModel_V", scale <= 0.0F ? 1.0F : scale);
   }

   private static Object buildModelFromAssetId(String modelId, float scale) {
      try {
         Class<?> modelAssetClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset");
         Method getAssetMap = modelAssetClass.getMethod("getAssetMap");
         Object assetMap = getAssetMap.invoke((Object)null);
         if (assetMap == null) {
            return null;
         } else {
            Method getAsset = assetMap.getClass().getMethod("getAsset", Object.class);
            Object asset = getAsset.invoke(assetMap, modelId);
            if (asset == null) {
               return null;
            } else {
               Class<?> modelClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.Model");
               Method create = modelClass.getMethod("createScaledModel", modelAssetClass, Float.TYPE);
               return create.invoke((Object)null, asset, scale);
            }
         }
      } catch (Throwable var9) {
         return null;
      }
   }

   private static boolean looksLikeRef(Object o) {
      if (o == null) {
         return false;
      } else {
         String n = o.getClass().getName();
         return n.endsWith(".Ref") || n.endsWith("Ref") || n.contains(".Ref");
      }
   }

   private static Object extractRefFromPair(Object pair) {
      if (pair == null) {
         return null;
      } else {
         String[] methods = new String[]{"getLeft", "getFirst", "getKey", "left", "first", "key"};

         for(String mname : methods) {
            try {
               Method m = pair.getClass().getMethod(mname);
               Object v = m.invoke(pair);
               if (v != null) {
                  return v;
               }
            } catch (Throwable var8) {
            }
         }

         return null;
      }
   }

   private static boolean doRemoveEntity(Object componentStore, Object savedRefOrPair) {
      Object ref = savedRefOrPair;
      if (!looksLikeRef(savedRefOrPair)) {
         Object extracted = extractRefFromPair(savedRefOrPair);
         if (extracted != null) {
            ref = extracted;
         }
      }

      Object removeReason = getRemoveReasonBestEffort();
      if (removeReason == null) {
         setError("Não encontrei RemoveReason nesta build.");
         return false;
      } else if (!tryInvokeRemoveEntity(componentStore, ref, removeReason)) {
         setError("Não achei Store.removeEntity(ref, reason) compatível nesta build.");
         return false;
      } else {
         return true;
      }
   }

   private static Object getRemoveReasonBestEffort() {
      String[] enumCandidates = new String[]{"com.hypixel.hytale.component.RemoveReason", "com.hypixel.hytale.component.Store$RemoveReason"};

      for(String cn : enumCandidates) {
         Object r = getEnumConstantAny(cn, "DESPAWN", "COMMAND", "PLUGIN", "CUSTOM", "REMOVE", "SPAWN");
         if (r != null) {
            return r;
         }

         Object first = getFirstEnumValue(cn);
         if (first != null) {
            return first;
         }
      }

      return null;
   }

   private static Object getEnumConstantAny(String enumClassName, String... names) {
      try {
         Class<?> enumClass = Class.forName(enumClassName);
         if (!enumClass.isEnum()) {
            return null;
         }

         Class<? extends Enum> e = enumClass;

         for(String n : names) {
            try {
               return Enum.valueOf(e, n);
            } catch (IllegalArgumentException var9) {
            }
         }
      } catch (Throwable var10) {
      }

      return null;
   }

   private static Object getFirstEnumValue(String enumClassName) {
      try {
         Class<?> enumClass = Class.forName(enumClassName);
         if (!enumClass.isEnum()) {
            return null;
         } else {
            Object[] vals = enumClass.getEnumConstants();
            return vals != null && vals.length > 0 ? vals[0] : null;
         }
      } catch (Throwable var3) {
         return null;
      }
   }

   private static boolean tryInvokeRemoveEntity(Object store, Object ref, Object reason) {
      try {
         for(Method m : store.getClass().getMethods()) {
            if (m.getName().equals("removeEntity") && m.getParameterCount() == 2) {
               Class<?>[] p = m.getParameterTypes();
               if (p[1].isInstance(reason)) {
                  m.invoke(store, ref, reason);
                  return true;
               }
            }
         }
      } catch (Throwable var8) {
      }

      return false;
   }

   private static Object tryGetSenderPosition(Object senderObj) {
      if (senderObj == null) {
         return null;
      } else {
         Object p = invokeNoArg(senderObj, "getPosition", "position");
         if (p != null) {
            return p;
         } else {
            Object transform = invokeNoArg(senderObj, "getTransform", "transform");
            if (transform != null) {
               Object p2 = invokeNoArg(transform, "getPosition", "position");
               if (p2 != null) {
                  return p2;
               }
            }

            return null;
         }
      }
   }

   private static Float tryGetSenderYaw(Object senderObj) {
      if (senderObj == null) {
         return null;
      } else {
         try {
            Object transform = invokeNoArg(senderObj, "getTransformComponent", "getTransform", "transform", "transformComponent");
            if (transform != null) {
               Object rot = invokeNoArg(transform, "getRotation", "rotation");
               Float y = extractYawFromRotation(rot);
               if (y != null) {
                  return y;
               }
            }
         } catch (Throwable var4) {
         }

         return null;
      }
   }

   private static Float tryGetOwnerYawFromWorldStore(Object worldObj, Object componentStore, UUID ownerId) {
      if (worldObj != null && componentStore != null && ownerId != null) {
         try {
            Object ref = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
            if (ref == null) {
               return null;
            } else {
               Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
               Method getCt = tcClass.getMethod("getComponentType");
               Object componentType = getCt.invoke((Object)null);
               if (componentType == null) {
                  return null;
               } else {
                  Object tc = invokeStoreGetComponent(componentStore, ref, componentType);
                  if (tc == null) {
                     return null;
                  } else {
                     Object rot = invokeNoArg(tc, "getRotation", "rotation");
                     return extractYawFromRotation(rot);
                  }
               }
            }
         } catch (Throwable var9) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Float extractYawFromRotation(Object rot) {
      if (rot == null) {
         return null;
      } else {
         try {
            Object v = invokeNoArg(rot, "getYaw");
            if (v instanceof Number) {
               return ((Number)v).floatValue();
            }
         } catch (Throwable var6) {
         }

         try {
            Object v = invokeNoArg(rot, "getY", "y");
            if (v instanceof Number) {
               return ((Number)v).floatValue();
            }
         } catch (Throwable var5) {
         }

         try {
            Field f = rot.getClass().getField("y");
            Object v = f.get(rot);
            if (v instanceof Number) {
               return ((Number)v).floatValue();
            }
         } catch (Throwable var4) {
         }

         try {
            Field f = rot.getClass().getDeclaredField("y");
            f.setAccessible(true);
            Object v = f.get(rot);
            if (v instanceof Number) {
               return ((Number)v).floatValue();
            }
         } catch (Throwable var3) {
         }

         return null;
      }
   }

   private static Object offsetInFrontOfYaw(Object posVec3d, Float yawDegOrNull, double dist) {
      Object pos = coerceToVector3d(posVec3d);
      if (pos == null) {
         return posVec3d;
      } else if (yawDegOrNull == null) {
         return offsetVector3d(pos, dist, (double)0.0F, (double)0.0F);
      } else {
         double yaw = Math.toRadians((double)yawDegOrNull);
         double ox = -Math.sin(yaw) * dist;
         double oz = -Math.cos(yaw) * dist;
         return offsetVector3d(pos, ox, (double)0.0F, oz);
      }
   }

   private static boolean hasCeilingAboveBestEffort(Store store, Vector3d ownerPos) {
      try {
         if (store == null || ownerPos == null) {
            return false;
         }

         EntityStore es = (EntityStore)store.getExternalData();
         if (es == null) {
            return false;
         }

         World world = es.getWorld();
         if (world == null) {
            return false;
         }

         int x = (int)Math.floor(ownerPos.getX());
         int z = (int)Math.floor(ownerPos.getZ());
         int y = (int)Math.floor(ownerPos.getY());
         int startY = y + 2;

         for(int i = 0; i < 12; ++i) {
            int blockId = world.getBlock(x, startY + i, z);
            if (blockId != 0) {
               return true;
            }
         }
      } catch (Throwable var10) {
      }

      return false;
   }

   private static void spawnParticleToOwner(Store store, Object ownerRefObj, Vector3d pos, String particleId) {
      try {
         if (store == null || pos == null || particleId == null || particleId.isBlank()) {
            return;
         }

         if (ownerRefObj instanceof Ref) {
            List<Ref<EntityStore>> viewers = Collections.singletonList((Ref)ownerRefObj);
            ParticleUtil.spawnParticleEffect(particleId, pos, viewers, store);
            return;
         }

         ParticleUtil.spawnParticleEffect(particleId, pos, store);
      } catch (Throwable var5) {
      }

   }

   private static void playNpcSpawnFx(Store store, Ref npcRef, Object ownerRefObj) {
      try {
         if (store == null || npcRef == null) {
            return;
         }

         TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         if (tc == null || tc.getPosition() == null) {
            return;
         }

         Vector3d p = tc.getPosition();

         try {
            spawnParticleToOwner(store, ownerRefObj, p, "PlayerSpawn_Spawn");
         } catch (Throwable var6) {
         }

         playSfx3d(store, p, "SFX_DIVINE_RESPAWN", 10.0F, -9.0F);
      } catch (Throwable var7) {
      }

   }

   private static void playSfx3d(Store store, Vector3d pos, String soundId, float volumeDb, float pitchSt) {
      try {
         if (store == null || pos == null || soundId == null || soundId.isBlank()) {
            return;
         }

         int idx = soundIndex(soundId);
         if (idx == 0) {
            return;
         }

         SoundUtil.playSoundEvent3d(idx, SoundCategory.SFX, pos.getX(), pos.getY(), pos.getZ(), dbToGain(volumeDb), stToPitch(pitchSt), store);
      } catch (Throwable var6) {
      }

   }

   private static int soundIndex(String id) {
      try {
         return SoundEvent.getAssetMap().getIndexOrDefault(id, 0);
      } catch (Throwable var2) {
         return 0;
      }
   }

   private static float dbToGain(float db) {
      return (float)Math.pow((double)10.0F, (double)db / (double)20.0F);
   }

   private static float stToPitch(float st) {
      return (float)Math.pow((double)2.0F, (double)st / (double)12.0F);
   }

   private static Object coerceToVector3d(Object maybe) {
      if (maybe == null) {
         return null;
      } else if (hasXYZ(maybe)) {
         return maybe;
      } else {
         Object pos = invokeNoArg(maybe, "getPosition", "position");
         return pos != null && hasXYZ(pos) ? pos : null;
      }
   }

   private static boolean hasXYZ(Object v) {
      if (v == null) {
         return false;
      } else if (hasMethod(v, "getX") && hasMethod(v, "getY") && hasMethod(v, "getZ")) {
         return true;
      } else if (hasMethod(v, "x") && hasMethod(v, "y") && hasMethod(v, "z")) {
         return true;
      } else {
         return hasField(v, "x") && hasField(v, "y") && hasField(v, "z");
      }
   }

   private static boolean hasMethod(Object v, String name) {
      try {
         v.getClass().getMethod(name);
         return true;
      } catch (Throwable var3) {
         return false;
      }
   }

   private static boolean hasField(Object v, String name) {
      try {
         v.getClass().getField(name);
         return true;
      } catch (Throwable var5) {
         try {
            v.getClass().getDeclaredField(name);
            return true;
         } catch (Throwable var4) {
            return false;
         }
      }
   }

   private static Object offsetVector3d(Object vec, double ox, double oy, double oz) {
      try {
         double x = readCoord(vec, "x");
         double y = readCoord(vec, "y");
         double z = readCoord(vec, "z");

         try {
            return vec.getClass().getConstructor(Double.TYPE, Double.TYPE, Double.TYPE).newInstance(x + ox, y + oy, z + oz);
         } catch (Throwable var15) {
            Object v2 = newVector3d(x + ox, y + oy, z + oz);
            return v2 != null ? v2 : vec;
         }
      } catch (Throwable var16) {
         return vec;
      }
   }

   private static double readCoord(Object vec, String axis) throws Exception {
      String getName = "get" + axis.toUpperCase();

      try {
         Object r = vec.getClass().getMethod(getName).invoke(vec);
         return ((Number)r).doubleValue();
      } catch (Throwable var8) {
         try {
            Object r = vec.getClass().getMethod(axis).invoke(vec);
            return ((Number)r).doubleValue();
         } catch (Throwable var7) {
            try {
               Object r = vec.getClass().getField(axis).get(vec);
               return ((Number)r).doubleValue();
            } catch (Throwable var6) {
               Field f = vec.getClass().getDeclaredField(axis);
               f.setAccessible(true);
               Object r = f.get(vec);
               return ((Number)r).doubleValue();
            }
         }
      }
   }

   private static Object newVector3d(double x, double y, double z) {
      String[] candidates = new String[]{"com.hypixel.hytale.math.vector.Vector3d", "com.hypixel.hytale.math.Vector3d", "com.hypixel.hytale.util.math.Vector3d", "com.hypixel.hytale.protocol.util.Vector3d", "com.hypixel.hytale.server.core.math.Vector3d"};

      for(String cn : candidates) {
         try {
            Class<?> c = Class.forName(cn);
            return c.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE).newInstance(x, y, z);
         } catch (Throwable var12) {
         }
      }

      return null;
   }

   private static Object newVector3f(float a, float b, float c0) {
      String[] candidates = new String[]{"com.hypixel.hytale.math.vector.Vector3f", "com.hypixel.hytale.math.Vector3f", "com.hypixel.hytale.util.math.Vector3f", "com.hypixel.hytale.protocol.util.Vector3f", "com.hypixel.hytale.server.core.math.Vector3f"};

      for(String cn : candidates) {
         try {
            Class<?> c = Class.forName(cn);
            return c.getConstructor(Float.TYPE, Float.TYPE, Float.TYPE).newInstance(a, b, c0);
         } catch (Throwable var9) {
         }
      }

      return null;
   }

   private static Object invokeNoArg(Object target, String... methodNames) {
      if (target == null) {
         return null;
      } else {
         for(String name : methodNames) {
            try {
               Method m = target.getClass().getMethod(name);
               return m.invoke(target);
            } catch (Throwable var7) {
            }
         }

         return null;
      }
   }

   private static Object invokeOneArg(Object target, String methodName, Class argType, Object arg) {
      if (target == null) {
         return null;
      } else {
         try {
            Method m = target.getClass().getMethod(methodName, argType);
            return m.invoke(target, arg);
         } catch (Throwable var5) {
            return null;
         }
      }
   }

   private static Object invokeStaticNoArg(String className, String methodName) {
      try {
         Class<?> c = Class.forName(className);
         Method m = c.getMethod(methodName);
         return m.invoke((Object)null);
      } catch (Throwable var4) {
         return null;
      }
   }

   private static Object getStaticFieldIfExists(String className, String fieldName) {
      try {
         Class<?> c = Class.forName(className);
         Field f = c.getField(fieldName);
         return f.get((Object)null);
      } catch (Throwable var4) {
         return null;
      }
   }

   private static String firstStringFromArray(Object arr) {
      if (arr instanceof String[] a) {
         if (a.length > 0) {
            return a[0];
         }
      }

      return null;
   }

   private static boolean invokeTwoArgs(Object target, String methodName, Class argType1, Class argType2, Object arg1, Object arg2) {
      if (target == null) {
         return false;
      } else {
         try {
            try {
               Method m = target.getClass().getMethod(methodName, argType1, argType2);
               m.invoke(target, arg1, arg2);
               return true;
            } catch (NoSuchMethodException var14) {
               for(Method m : target.getClass().getMethods()) {
                  if (m.getName().equals(methodName) && m.getParameterCount() == 2) {
                     Class<?>[] p = m.getParameterTypes();
                     boolean ok1 = arg1 == null || p[0].isAssignableFrom(arg1.getClass()) || p[0].isAssignableFrom(argType1);
                     boolean ok2 = arg2 == null || p[1].isAssignableFrom(arg2.getClass()) || p[1].isAssignableFrom(argType2);
                     if (ok1 && ok2) {
                        m.invoke(target, arg1, arg2);
                        return true;
                     }
                  }
               }
            }
         } catch (Throwable var15) {
         }

         return false;
      }
   }

   static {
      RPG_STAGE1_MOB_CALC = new RpgStage1MobLevelCalculator(RPG_STAGE1_CFG);
      DEF_STAT_INDEX = null;
   }

   private static enum State {
      SPAWNING,
      ACTIVE,
      DESPAWNING;

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{SPAWNING, ACTIVE, DESPAWNING};
      }
   }

   private static final class NpcRecord {
      final Object worldObj;
      final UUID ownerId;
      volatile Object refObj;
      volatile State state;
      volatile boolean downed;
      volatile long downedUntilMillis;
      volatile SimpleItemContainer backpack;
      volatile long nextAutoLootMillis;
      volatile boolean lootPausedInventoryFull;
      volatile long nextLootFullRecheckMillis;
      volatile long nextLootFullMsgMillis;
      volatile boolean backpackDirty;
      volatile long nextBackpackSaveMillis;
      volatile long autoLootStickUntilMillis;
      volatile Object autoLootStickDeadRefObj;
      volatile Vector3d autoLootStickAnchorPos;
      volatile boolean ownerUnderground;
      volatile long nextUndergroundCheckMillis;
      final ArrayList combatTags = new ArrayList();
      volatile long lastCombatEndMillis;
      volatile long lootStickUntilMillis;
      volatile long lastCombatTagMillis;
      volatile Vector3d lastBattleCenter;
      volatile boolean wasInCombat;
      volatile boolean lootingActive;
      volatile Object lootTargetRefObj;
      volatile long lootTargetSinceMillis;
      final ArrayList pendingLootRefObjs = new ArrayList();
      final Map lootProcessedUntil = new ConcurrentHashMap();
      final Map lootChatAcc = new LinkedHashMap();
      volatile long lootChatSendAtMillis;
      volatile int level = 1;
      volatile String equippedWeaponId;
      volatile double xpInLevel = (double)0.0F;
      volatile long totalXp = 0L;
      volatile int npcLevelCached = 1;
      volatile double stage1XpRemainder = (double)0.0F;
      volatile long baseHp = -1L;
      volatile long baseDef = -1L;
      volatile String modelId;
      volatile double modelScale;
      volatile boolean respawnRequested;
      volatile Object respawnWorldObj;
      volatile Object respawnSenderObj;
      volatile long respawnAtMillis;
      volatile String respawnMessage;
      volatile long lastMoveToMillis;
      volatile long lastSampleMillis;
      volatile Vector3d lastMoveTarget;
      volatile long lastMoveIssuedMillis;
      volatile Vector3d lastOwnerPos;
      volatile Vector3d lastNpcPos;
      volatile long lastNpcMovedMillis;
      volatile long lastTeleportMillis;
      long farSinceMillis;
      volatile long spawnFxUntilMillis;
      volatile long spawnFxNextMillis;
      volatile boolean wardrobeRestorePending;
      volatile int wardrobeRestoreAttempts;
      volatile long nextWardrobeRestoreMillis;
      volatile long lastMeleeAttackMillis;
      volatile long lastRangedAttackMillis;
      volatile String rangedBowItemId;
      volatile long rangedBowReadyAtMillis;
      volatile long lastCombatNudgeMillis;
      volatile long lastAggroPulseMillis;
      volatile String lastAttackAnimId;
      volatile long clearAttackAnimAtMillis;
      volatile long debugNextEquipMillis;
      volatile long debugNextCombatMillis;
      volatile long debugNextAttackMillis;
      volatile long debugNextDamageMillis;
      volatile long debugNextZonesMillis;
      volatile long debugNextMobLevelMillis;
      volatile float idleLookYawOffset;
      volatile long idleLookNextMillis;
      volatile Object combatTargetRefObj;
      volatile long combatUntilMillis;
      volatile Object npcCombatTargetRefObj;
      volatile long npcCombatUntilMillis;
      volatile Object assistTargetRefObj;
      volatile long assistUntilMillis;
      volatile long targetLostSinceMillis;
      volatile long targetStuckSinceMillis;
      volatile double lastTargetHorizontal = (double)-1.0F;
      volatile long lastTargetSampleMillis;
      volatile boolean chaseDisengaged;
      volatile boolean defendeEnabled;
      volatile boolean autoLootEnabled = true;
      volatile boolean debugLogEnabled;
      volatile boolean godMode;
      volatile long deathDespawnAtMillis;
      volatile long levelUpFxUntilMillis;
      volatile long levelUpFxNextTickMillis;
      volatile int levelUpFxPendingCount;
      volatile boolean regenWasInCombat;
      volatile long regenStartAtMillis;
      volatile long regenLastApplyMillis;
      volatile int zoneExpected;
      volatile int zoneCurrent;
      volatile int zoneRawId;
      volatile Color zoneHudColor;
      volatile int currentTargetMobLevel;
      volatile UUID currentTargetMobUuid;
      volatile int zoneInferredId;
      volatile int zoneInferredFromMobLevel;
      volatile long zoneInferredAtMillis;

      NpcRecord(Object worldObj, UUID ownerId, Object refObj, State state) {
         this.worldObj = worldObj;
         this.ownerId = ownerId;
         this.refObj = refObj;
         this.state = state;
         this.debugLogEnabled = false;
         this.zoneHudColor = new Color((byte)-1, (byte)-1, (byte)-1);
      }
   }

   public static final class NpcDebugSnapshot {
      public String ownerUuid;
      public boolean hasRecord;
      public boolean hasNpcRef;
      public String state;
      public boolean downed;
      public long downedUntilMillis;
      public long deathDespawnAtMillis;
      public int npcLevel;
      public long totalXp;
      public double xpRemainder;
      public boolean inCombatRecently;
      public long lastCombatTagMillis;
      public long lastCombatEndMillis;
      public boolean lootingActive;
      public long lootStickUntilMillis;
      public boolean regenActive;
      public long regenStartAtMillis;
      public long regenLastApplyMillis;
      public boolean lootPausedInventoryFull;
      public Vec3 lastOwnerPos;
      public Vec3 lastNpcPos;
      public Vec3 lastBattleCenter;
      public int backpackSlotsUsed;
      public int backpackSlotsTotal;
   }

   public static final class Vec3 {
      public double x;
      public double y;
      public double z;

      public Vec3() {
      }

      public Vec3(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }

   private static final class PendingRespawn {
      final Object worldObj;
      final Object senderObj;
      final long atMillis;
      final String message;

      PendingRespawn(Object worldObj, Object senderObj, long atMillis, String message) {
         this.worldObj = worldObj;
         this.senderObj = senderObj;
         this.atMillis = atMillis;
         this.message = message;
      }
   }

   private static final class CombatTag {
      final Object targetRefObj;
      Vector3d pos;
      long lastSeenMillis;

      CombatTag(Object targetRefObj, Vector3d pos, long lastSeenMillis) {
         this.targetRefObj = targetRefObj;
         this.pos = pos;
         this.lastSeenMillis = lastSeenMillis;
      }
   }

   public static record LevelProgressSnapshot(int level, long xp, long xpNeeded) {
   }
}
