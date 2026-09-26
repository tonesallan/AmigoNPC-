package br.tones.amigonpc.core;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.api.events.NpcExperienceGainedEvent;
import br.tones.amigonpc.api.events.NpcLevelUpEvent;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.debug.DebugVec3;
import br.tones.amigonpc.core.debug.NpcDebugSnapshot;
import br.tones.amigonpc.core.debug.NpcDebugSnapshotSupport;
import br.tones.amigonpc.core.events.AmigoEventBus;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.npcstats.AttributeModifierService;
import br.tones.amigonpc.core.npcstats.NpcStatsConfigService;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import br.tones.amigonpc.core.optional.AmigoWardrobePersistence;
import br.tones.amigonpc.core.progress.NpcLevelProgressSnapshot;
import br.tones.amigonpc.core.progress.NpcProgressionSupport;
import br.tones.amigonpc.core.progress.StatScaling;
import br.tones.amigonpc.core.progress.XpProgression;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgLevelingStage1Config;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1Formulas;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelCalculator;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelHelper;
import br.tones.amigonpc.core.ui.lvlgui.AmigoLvlGuiService;
import br.tones.amigonpc.core.zones.AmigoZonesConfigService;
import br.tones.amigonpc.core.zones.MobLevelVarianceCalculator;
import br.tones.amigonpc.core.zones.ZoneMobService;
import br.tones.amigonpc.core.zones.ZoneModel;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.ItemAnimation;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.RespondToHit;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.Particles;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class AmigoNpcManager {
   private static final AmigoNpcManager SHARED = new AmigoNpcManager();
   private static final String DEFAULT_MODEL_ID = "PlayerTestModel_V";
   private static final double DEFAULT_MODEL_SCALE = 1.0;
   private static final String DEFAULT_ROLE_NAME = "Amigo_Follow";
   private static final boolean DEBUG_COMBAT_DEFAULT = false;
   private static final Map<String, String> WEAPON_ATTACK_ANIM_CACHE = new ConcurrentHashMap<>();
   private final Map<UUID, AmigoNpcManager.NpcRecord> npcRefPorPlayer = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> debugLogByOwner = new ConcurrentHashMap<>();
   private final Map<UUID, PendingRespawn> pendingRespawns = new ConcurrentHashMap<>();
   private final Map<Object, UUID> amigoRefs = new ConcurrentHashMap<>();
   private static volatile String LAST_ERROR;
   private volatile boolean pvpEnabled = false;
   private static final long COMBAT_WINDOW_MILLIS = 3000L;
   private static final long ASSIST_GRACE_MILLIS = 3000L;
   private static final long MELEE_COOLDOWN_MILLIS = 850L;
   private static final double DEFENDER_AUTO_ACQUIRE_RADIUS = 12.0;
   private static final double DEFENDER_AUTO_ACQUIRE_MAX_DY = 2.5;
   private static final double CHASE_MAX_DISTANCE = 30.0;
   private static final double CHASE_REACQUIRE_DISTANCE = 10.0;
   private static final double RANGED_AIR_MAX_DISTANCE = 20.0;
   private static final double RANGED_AIR_MAX_DY = 35.0;
   private static final long RANGED_COOLDOWN_MILLIS = 1150L;
   private static final long LEVEL_UP_FX_MILLIS = 1000L;
   private static final long LEVEL_UP_FX_TICK_MILLIS = 250L;
   private static final double AUTOLOOT_RADIUS = 5.0;
   private static final double AUTOLOOT_OWNER_RADIUS = 20.0;
   private static final long AUTOLOOT_INTERVAL_MS = 250L;
   private static final int AUTOLOOT_MAX_ITEMS_PER_SCAN = 8;
   private static final long AUTOLOOT_FULL_MSG_COOLDOWN_MS = 30000L;
   private static final long DOWNED_AUTO_REVIVE_MILLIS = 40000L;
   private static final double DOWNED_BODY_HIDE_DISTANCE = 25.0;
   private static final long DOWNED_CHAT_MIN_INTERVAL_MILLIS = 8000L;
   private static final long DOWNED_CHAT_MAX_INTERVAL_MILLIS = 14000L;
   private static final String[] DOWNED_CHAT_MESSAGES = new String[]{
      "Ainda estou morto :x",
      "Desculpa, morto não fala…..",
      "Mortinho da Silva :x"
   };
   private static final Archetype<EntityStore> AUTOLOOT_QUERY = Archetype.of(
      new ComponentType[]{TransformComponent.getComponentType(), ItemComponent.getComponentType()}
   );
   private static final long BACKPACK_SAVE_DEBOUNCE_MS = 400L;
   private static final long UNDERGROUND_CHECK_INTERVAL_MS = 500L;
   private static final int UNDERGROUND_CEILING_SCAN_BLOCKS = 12;
   private static final double UNDERGROUND_TELEPORT_DISTANCE = 10.0;
   private static final String LOCKED_TARGET_CLOSE_SLOT = "LockedTargetClose";
   private static final double LOOTING_NO_ENEMY_RADIUS = 4.0;
   private static final double LOOT_TAG_SCAN_RADIUS = 6.0;
   private static final double LOOT_PICKUP_DISTANCE = 5.0;
   private static final String LOOT_CHAT_TEMPLATE = "{quantidade} {item} Coletado.";
   private static final long LOOT_CHAT_SUMMARY_DELAY_MS = 500L;
   private static final long COMBAT_TAG_CLEAR_MS = 25000L;
   private static final double COMBAT_TAG_CLEAR_DISTANCE = 25.0;
   private static final long LOOT_SKIP_RETRY_MS = 4000L;
   private static final long LOOT_TARGET_TIMEOUT_MS = 12000L;
   private static final long LOOT_POST_COMBAT_STICK_MS = 500L;
   private static final int COMBAT_TAG_MAX = 16;
   private static final int LOOT_PENDING_MAX = 512;
   private static final long XP_PER_HIT = 0L;
   private static final long XP_PER_KILL = 40L;
   private static final RpgLevelingStage1Config RPG_STAGE1_CFG = new RpgLevelingStage1Config();
   private static final RpgStage1MobLevelCalculator RPG_STAGE1_MOB_CALC = new RpgStage1MobLevelCalculator(RPG_STAGE1_CFG);
   private static final long SPAWN_FX_FOLLOW_MS = 1800L;
   private static final long SPAWN_FX_FOLLOW_INTERVAL_MS = 180L;
   private static final long REGEN_DELAY_MS = 3000L;
   private static final float REGEN_RATE_PER_SECOND = 0.12F;

   private AmigoNpcManager() {
      try {
         this.pvpEnabled = AmigoPersistence.loadPvpEnabledGlobal();
      } catch (Throwable ignored) {
         this.pvpEnabled = false;
      }
   }

   public static AmigoNpcManager getShared() {
      return SHARED;
   }

   public static boolean saveNpcWardrobeNow(UUID ownerId) {
      try {
         return SHARED.saveNpcWardrobeNowInternal(ownerId);
      } catch (Throwable ignored) {
         return false;
      }
   }

   public NpcDebugSnapshot getNpcDebugSnapshot(UUID ownerId) {
      String ownerUuid = ownerId != null ? String.valueOf(ownerId) : null;
      NpcDebugSnapshot s = new NpcDebugSnapshot();
      s.ownerUuid = ownerUuid;
      if (ownerId == null) {
         return NpcDebugSnapshotSupport.emptySnapshot(ownerUuid, "NONE");
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null) {
         return NpcDebugSnapshotSupport.emptySnapshot(ownerUuid, "MISSING");
      }

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
      NpcDebugSnapshotSupport.populateBackpackSummary(s, rec.backpack);
      return s;
   }

   private static DebugVec3 vec(Vector3d v) {
      return NpcDebugSnapshotSupport.toVec3(v);
   }

   private boolean saveNpcWardrobeNowInternal(UUID ownerId) {
      if (ownerId == null) {
         return false;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null || rec.worldObj == null || !(rec.refObj instanceof Ref)) {
         return false;
      }

      if (this.executeWardrobeSaveNow(rec, ownerId)) {
         return true;
      }

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
      }

      try {
         latch.await(400L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      }

      return result[0];
   }

   private boolean executeWardrobeSaveNow(AmigoNpcManager.NpcRecord rec, UUID ownerId) {
      if (rec != null && ownerId != null && rec.worldObj != null && rec.refObj instanceof Ref<?> npcRefRaw) {
         try {
            if (getComponentStoreFromWorld(rec.worldObj) instanceof Store<?> storeRaw) {
               Store<EntityStore> store = (Store<EntityStore>)storeRaw;
               Ref<EntityStore> npcRef = (Ref<EntityStore>)npcRefRaw;
               return AmigoWardrobePersistence.saveNpcWardrobe(ownerId, store, npcRef);
            } else {
               return false;
            }
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void armWardrobeRestoreRetry(AmigoNpcManager.NpcRecord rec, UUID ownerId) {
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

   private static void clearWardrobeRestoreRetry(AmigoNpcManager.NpcRecord rec) {
      if (rec != null) {
         rec.wardrobeRestorePending = false;
         rec.wardrobeRestoreAttempts = 0;
         rec.nextWardrobeRestoreMillis = Long.MAX_VALUE;
      }
   }

   private static void tickWardrobeRestore(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, long now) {
      if (store != null && rec != null && rec.wardrobeRestorePending) {
         if (rec.refObj instanceof Ref<?> npcRefRaw && ownerRefObj instanceof Ref<?> ownerRefRaw) {
            if (rec.state == AmigoNpcManager.State.ACTIVE && !rec.downed) {
               if (rec.nextWardrobeRestoreMillis <= now) {
                  boolean restored = false;

                  try {
                     Ref<EntityStore> npcRef = (Ref<EntityStore>)npcRefRaw;
                     Ref<EntityStore> ownerRef = (Ref<EntityStore>)ownerRefRaw;
                     restored = AmigoWardrobePersistence.restoreNpcWardrobe(rec.ownerId, store, ownerRef, npcRef);
                  } catch (Throwable var10) {
                  }

                  rec.wardrobeRestoreAttempts++;
                  if (restored) {
                     clearWardrobeRestoreRetry(rec);
                  } else if (rec.wardrobeRestoreAttempts >= 12) {
                     clearWardrobeRestoreRetry(rec);
                  } else {
                     rec.nextWardrobeRestoreMillis = now + 250L;
                  }
               }
            }
         }
      }
   }

   private static long coerceStage1XpGainToLong(AmigoNpcManager.NpcRecord rec, double xpGainDouble) {
      if (rec == null) {
         return 0L;
      }

      if (xpGainDouble > 0.0 && !Double.isNaN(xpGainDouble) && !Double.isInfinite(xpGainDouble)) {
         double rem = rec.stage1XpRemainder;
         if (Double.isNaN(rem) || Double.isInfinite(rem) || rem < 0.0) {
            rem = 0.0;
         }

         double sum = xpGainDouble + rem;
         long whole = (long)Math.floor(sum);
         double newRem = sum - whole;
         if (!(newRem >= 0.0) || Double.isNaN(newRem) || Double.isInfinite(newRem)) {
            newRem = 0.0;
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

   public CombatMode getCombatMode(UUID owner) {
      if (owner == null) {
         return CombatMode.PROTECT_OWNER;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(owner);
      return rec != null ? rec.combatMode : AmigoPersistence.loadCombatMode(owner);
   }

   public void setCombatMode(UUID owner, CombatMode mode) {
      if (owner == null || mode == null) {
         return;
      }

      AmigoPersistence.saveCombatMode(owner, mode);
      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(owner);
      if (rec != null) {
         rec.combatMode = mode;
         rec.defendeEnabled = mode == CombatMode.PROTECT_OWNER;
         rec.chaseDisengaged = false;
         rec.targetLostSinceMillis = 0L;
         rec.targetStuckSinceMillis = 0L;
      }
   }

   public boolean isDefendeEnabled(UUID owner) {
      return getCombatMode(owner) == CombatMode.PROTECT_OWNER;
   }

   public void setDefendeEnabled(UUID owner, boolean enabled) {
      setCombatMode(owner, enabled ? CombatMode.PROTECT_OWNER : CombatMode.WEAKEST_ENEMY);
   }

   public boolean isAutoWeaponSwitchEnabled(UUID ownerId) {
      if (ownerId == null) {
         return true;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      return rec != null ? rec.autoWeaponSwitchEnabled : AmigoPersistence.loadAutoWeaponSwitch(ownerId);
   }

   public void setAutoWeaponSwitchEnabled(UUID ownerId, boolean enabled) {
      if (ownerId == null) {
         return;
      }

      AmigoPersistence.saveAutoWeaponSwitch(ownerId, enabled);
      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec != null) {
         rec.autoWeaponSwitchEnabled = enabled;
      }
   }

   public boolean isInterruptAttacksEnabled(UUID ownerId) {
      if (ownerId == null) {
         return true;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      return rec != null ? rec.interruptAttacksEnabled : AmigoPersistence.loadInterruptAttacks(ownerId);
   }

   public void setInterruptAttacksEnabled(UUID ownerId, boolean enabled) {
      if (ownerId == null) {
         return;
      }

      AmigoPersistence.saveInterruptAttacks(ownerId, enabled);
      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec != null) {
         rec.interruptAttacksEnabled = enabled;
      }
   }

   public boolean isAutoLootEnabled(UUID ownerId) {
      if (ownerId == null) {
         return true;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      return rec != null ? rec.autoLootEnabled : AmigoPersistence.loadAutoLootEnabled(ownerId);
   }

   public boolean toggleAutoLoot(UUID ownerId) {
      boolean newVal = !this.isAutoLootEnabled(ownerId);
      this.setAutoLootEnabled(ownerId, newVal);
      return newVal;
   }

   public void setAutoLootEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         AmigoPersistence.saveAutoLootEnabled(ownerId, enabled);
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
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

   public String getCustomName(UUID ownerId) {
      if (ownerId == null) {
         return null;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      return rec != null ? normalizeCustomName(rec.customName) : normalizeCustomName(AmigoPersistence.loadCustomName(ownerId));
   }

   public void setCustomNameWithStore(Store<EntityStore> store, UUID ownerId, String customName) {
      if (ownerId != null) {
         String normalizedName = normalizeCustomName(customName);
         AmigoPersistence.saveCustomName(ownerId, normalizedName);
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.customName = normalizedName;
            if (store != null && rec.refObj instanceof Ref<?> rawNpcRef) {
               @SuppressWarnings("unchecked")
               Ref<EntityStore> npcRef = (Ref<EntityStore>)rawNpcRef;

               try {
                  NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
               } catch (Throwable var7) {
               }
            }
         }
      }
   }

   private static String normalizeCustomName(String customName) {
      if (customName == null) {
         return null;
      }

      String normalized = customName.trim();
      if (normalized.isBlank()) {
         return null;
      }

      if (normalized.length() > 20) {
         normalized = normalized.substring(0, 20);
      }

      return normalized;
   }

   public String getAutoLootStatus(UUID ownerId) {
      if (ownerId == null) {
         return AmigoText.text("core.autoloot.status.unknown");
      }

      boolean enabled = this.isAutoLootEnabled(ownerId);
      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
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

      return AmigoText.format("core.autoloot.status.summary", AmigoText.onOff(enabled), state, downed, pausedFull, pending, stickLeft, freeSlots);
   }

   private static int countFreeSlots(SimpleItemContainer bag) {
      if (bag == null) {
         return -1;
      }

      short cap;
      try {
         cap = bag.getCapacity();
      } catch (Throwable t) {
         return -1;
      }

      int free = 0;

      for (short slot = 0; slot < cap; slot++) {
         ItemStack st;
         try {
            st = bag.getItemStack(slot);
         } catch (Throwable t) {
            continue;
         }

         if (st == null || st.isEmpty()) {
            free++;
         }
      }

      return free;
   }

   public boolean isGodMode(UUID ownerId) {
      return false;
   }

   public boolean toggleGodModeWithStore(Store<EntityStore> store, UUID ownerId) {
      if (ownerId != null) {
         AmigoPersistence.saveGodMode(ownerId, false);
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.godMode = false;
         }
      }

      return false;
   }

   public void setGodModeWithStore(Store<EntityStore> store, UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         AmigoPersistence.saveGodMode(ownerId, false);
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.godMode = false;
         }
      }
   }

   public void notifyNpcDamaged(UUID ownerId, long now) {
      if (ownerId != null) {
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         if (rec != null) {
            rec.regenWasInCombat = true;
            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
         }
      }
   }

   private static void endCombatTaggedLooting(AmigoNpcManager.NpcRecord rec) {
      NpcCombatLootFlowSupport.endCombatTaggedLooting(rec);
   }

   private void tickSpawnFollowFx(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, long now) {
      NpcRecoveryEffectSupport.tickSpawnFollowFx(store, rec, ownerRefObj, now, 180L, AmigoNpcManager::spawnParticleToOwner);
   }

   private void tickAutoRegen(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, long now) {
      NpcRecoveryEffectSupport.tickAutoRegen(store, rec, ownerRefObj, now, 0.12F, AmigoNpcManager::spawnParticleToOwner);
   }

   public boolean isDebugLogEnabled(UUID owner) {
      if (owner == null) {
         return false;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(owner);
      if (rec != null) {
         return rec.debugLogEnabled;
      }

      Boolean v = this.debugLogByOwner.get(owner);
      return v != null && v;
   }

   public boolean toggleDebugLog(UUID owner) {
      if (owner == null) {
         return false;
      }

      boolean enabled = !this.isDebugLogEnabled(owner);
      this.setDebugLogEnabled(owner, enabled);
      return enabled;
   }

   public void setDebugLogEnabled(UUID owner, boolean enabled) {
      if (owner != null) {
         this.debugLogByOwner.put(owner, enabled);
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(owner);
         if (rec != null) {
            rec.debugLogEnabled = enabled;
         }
      }
   }

   private static String choosePreferredNpcType(Object npcPlugin) {
      String[] FORCED_PASSIVE = new String[]{"citizen", "villager", "merchant", "trader", "worker", "farmer"};
      Object rolesObj = invokeOneArg(npcPlugin, "getRoleTemplateNames", boolean.class, true);
      List<String> roles = toStringList(rolesObj);

      for (String want : FORCED_PASSIVE) {
         for (String r : roles) {
            if (r != null && r.equalsIgnoreCase(want)) {
               return r;
            }
         }
      }

      for (String want : FORCED_PASSIVE) {
         for (String r : roles) {
            if (r != null && r.toLowerCase().contains(want)) {
               return r;
            }
         }
      }

      String pick = pickHumanLike(roles);
      if (pick != null) {
         return pick;
      } else {
         return !roles.isEmpty() ? roles.get(0) : firstStringFromArray(invokeNoArg(npcPlugin, "getPresetCoverageTestNPCs"));
      }
   }

   private static List<String> toStringList(Object listObj) {
      if (listObj instanceof List<?> list) {
         ArrayList<String> out = new ArrayList<>();

         for (Object v : list) {
            if (v != null) {
               out.add(String.valueOf(v));
            }
         }

         return out;
      } else {
         return List.of();
      }
   }

   private static String pickHumanLike(List<String> candidates) {
      if (candidates != null && !candidates.isEmpty()) {
         String[] deny = new String[]{
            "crow", "raven", "bird", "avian", "bat", "eagle", "hawk", "falcon", "vulture", "owl", "pigeon", "duck", "seagull", "test"
         };
         String[] prefer = new String[]{"citizen", "human", "villager", "guard", "soldier", "merchant", "trader", "worker", "farmer", "npc"};

         for (String p : prefer) {
            for (String s : candidates) {
               String low = s.toLowerCase();
               if (low.contains(p) && !containsAny(low, deny)) {
                  return s;
               }
            }
         }

         for (String s : candidates) {
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
      for (String n : needles) {
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
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      return rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.refObj instanceof Ref && rec.worldObj != null && !rec.downed;
   }

   public void scheduleAutoLootSecondScan(UUID ownerId, Vector3d anchorPos, Object deadRefObjOrNull) {
      if (ownerId != null) {
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
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
      return this.spawn(worldObj, ownerId, null);
   }

   public boolean spawn(Object worldObj, UUID ownerId, Object senderObj) {
      if (worldObj != null && ownerId != null) {
         AmigoNpcManager.NpcRecord existing = this.npcRefPorPlayer.get(ownerId);
         if (existing == null) {
            AmigoNpcManager.NpcRecord rec = new AmigoNpcManager.NpcRecord(worldObj, ownerId, null, AmigoNpcManager.State.SPAWNING);

            try {
               Boolean dbg = this.debugLogByOwner.get(ownerId);
               rec.debugLogEnabled = dbg != null ? dbg : false;
            } catch (Throwable var12) {
            }

            rec.backpack = AmigoPersistence.loadBackpack(ownerId);
            String savedModel = AmigoPersistence.loadModelId(ownerId);
            boolean hasSavedWardrobeCosmetics = hasSavedWardrobeCosmetics(ownerId);
            rec.modelId = savedModel != null && !savedModel.isBlank() ? savedModel : (hasSavedWardrobeCosmetics ? null : "PlayerTestModel_V");
            double savedScale = AmigoPersistence.loadModelScale(ownerId);
            rec.modelScale = savedScale <= 0.0 ? 1.0 : savedScale;
            rec.customName = normalizeCustomName(AmigoPersistence.loadCustomName(ownerId));
            rec.level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
            rec.equippedWeaponId = AmigoPersistence.loadEquippedWeaponId(ownerId);
            rec.combatMode = AmigoPersistence.loadCombatMode(ownerId);
            rec.defendeEnabled = rec.combatMode == CombatMode.PROTECT_OWNER;
            rec.autoWeaponSwitchEnabled = AmigoPersistence.loadAutoWeaponSwitch(ownerId);
            rec.interruptAttacksEnabled = AmigoPersistence.loadInterruptAttacks(ownerId);
            rec.autoLootEnabled = AmigoPersistence.loadAutoLootEnabled(ownerId);
            rec.godMode = false;
            rec.totalXp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            rec.npcLevelCached = XpProgression.levelFromTotalXp(rec.totalXp);
            int computedSwordLevel = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.npcLevelCached);
            if (computedSwordLevel != rec.level) {
               rec.level = computedSwordLevel;
               AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
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
                     setError(AmigoText.text("core.error.store.unavailable"));
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
                  if (npcPlugin == null) {
                     setError(AmigoText.text("core.error.npcplugin.unavailable"));
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object pos = tryGetOwnerPositionFromWorldStore(worldObj, componentStore, ownerId);
                  if (pos == null) {
                     pos = tryGetSenderPosition(senderObj);
                  }

                  pos = coerceToVector3d(pos);
                  if (pos == null) {
                     setError(AmigoText.text("core.error.player_position.unavailable"));
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Float yaw = tryGetOwnerYawFromWorldStore(worldObj, componentStore, ownerId);
                  if (yaw == null) {
                     yaw = tryGetSenderYaw(senderObj);
                  }

                  pos = offsetInFrontOfYaw(pos, yaw, 4.0);
                  Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
                  if (rot == null) {
                     rot = newVector3f(0.0F, 0.0F, 0.0F);
                  }

                  Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
                  Object model = buildPreferredSpawnModel(ownerId, componentStore, ownerRef, rec.modelId, (float)rec.modelScale);
                  if (model == null) {
                     setError(AmigoText.format("core.error.model_asset.invalid", rec.modelId));
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, "Amigo_Follow");
                  if (roleIndex < 0) {
                     setError(AmigoText.format("core.error.role.missing", "Amigo_Follow"));
                     this.npcRefPorPlayer.remove(ownerId, rec);
                     return;
                  }

                  Object pair = invokeSpawnEntity(npcPlugin, componentStore, roleIndex, pos, rot, model, ownerRef);
                  if (pair == null) {
                     setError(AmigoText.format("core.error.spawn_entity.null", rec.modelId));
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
                        setError(AmigoText.text("core.error.spawn.remove_after_spawn_failed"));
                     }

                     return;
                  }

                  try {
                     Store<EntityStore> store = (Store<EntityStore>)componentStore;
                     Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                     this.applySwordWeaponNow(store, npcRef, ownerId, rec, true);
                     this.applyNpcScaling(store, npcRef, ownerId, rec, true);
                     armWardrobeRestoreRetry(rec, ownerId);

                     try {
                        if (rec.wardrobeRestorePending) {
                           boolean restored = AmigoWardrobePersistence.restoreNpcWardrobe(ownerId, store, (Ref<EntityStore>)ownerRef, npcRef);
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
                  setError(AmigoText.format("core.error.spawn.failed", t.getClass().getSimpleName(), t.getMessage()));
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  if (rec.refObj != null) {
                     this.amigoRefs.remove(rec.refObj);
                  }
               }
            });
            if (!queued) {
               setError(AmigoText.format("core.error.world_execute.failed", HytaleBridge.getLastError()));
               this.npcRefPorPlayer.remove(ownerId, rec);
               return false;
            } else {
               return true;
            }
         } else if (existing.state != AmigoNpcManager.State.SPAWNING && existing.state != AmigoNpcManager.State.ACTIVE) {
            setError(AmigoText.text("core.error.npc.removing"));
            return false;
         } else {
            setError(AmigoText.text("core.error.npc.already_exists"));
            return false;
         }
      } else {
         setError(AmigoText.text("core.error.spawn.invalid_world_owner_null"));
         return false;
      }
   }

   private void spawnIntoExistingRecord(Object worldObj, Object componentStore, UUID ownerId, Object senderObj, AmigoNpcManager.NpcRecord rec) {
      if (worldObj != null && componentStore != null && ownerId != null && rec != null) {
         try {
            Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
            if (npcPlugin == null) {
               npcPlugin = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "INSTANCE");
            }

            if (npcPlugin == null) {
               setError(AmigoText.text("core.error.npcplugin.unavailable"));
               return;
            }

            Object pos = tryGetOwnerPositionFromWorldStore(worldObj, componentStore, ownerId);
            if (pos == null) {
               pos = tryGetSenderPosition(senderObj);
            }

            pos = coerceToVector3d(pos);
            if (pos == null) {
               setError(AmigoText.text("core.error.player_position.unavailable"));
               return;
            }

            Float yaw = tryGetOwnerYawFromWorldStore(worldObj, componentStore, ownerId);
            if (yaw == null) {
               yaw = tryGetSenderYaw(senderObj);
            }

            pos = offsetInFrontOfYaw(pos, yaw, 4.0);
            Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
            if (rot == null) {
               rot = newVector3f(0.0F, 0.0F, 0.0F);
            }

            Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
            Object model = buildPreferredSpawnModel(ownerId, componentStore, ownerRef, rec.modelId, (float)rec.modelScale);
            if (model == null) {
               setError(AmigoText.format("core.error.model_asset.invalid", rec.modelId));
               return;
            }

            int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, "Amigo_Follow");
            if (roleIndex < 0) {
               setError(AmigoText.format("core.error.role.missing", "Amigo_Follow"));
               return;
            }

            Object pair = invokeSpawnEntity(npcPlugin, componentStore, roleIndex, pos, rot, model, ownerRef);
            if (pair == null) {
               setError(AmigoText.format("core.error.spawn_entity.null", rec.modelId));
               return;
            }

            Object ref = extractRefFromPair(pair);
            rec.refObj = ref != null ? ref : pair;
            if (rec.refObj != null) {
               this.amigoRefs.put(rec.refObj, ownerId);
            }

            try {
               Store<EntityStore> store = (Store<EntityStore>)componentStore;
               Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
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
                        } catch (Throwable ignored) {
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

   public boolean spawnWithStore(Object worldObj, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, UUID ownerId, Object senderObj) {
      if (worldObj != null && store != null && playerEntityRef != null && ownerId != null) {
         AmigoNpcManager.NpcRecord existing = this.npcRefPorPlayer.get(ownerId);
         if (existing == null) {
            AmigoNpcManager.NpcRecord rec = new AmigoNpcManager.NpcRecord(worldObj, ownerId, null, AmigoNpcManager.State.SPAWNING);

            try {
               Boolean dbg = this.debugLogByOwner.get(ownerId);
               rec.debugLogEnabled = dbg != null ? dbg : false;
            } catch (Throwable var25) {
            }

            rec.backpack = AmigoPersistence.loadBackpack(ownerId);
            String savedModel = AmigoPersistence.loadModelId(ownerId);
            boolean hasSavedWardrobeCosmetics = hasSavedWardrobeCosmetics(ownerId);
            rec.modelId = savedModel != null && !savedModel.isBlank() ? savedModel : (hasSavedWardrobeCosmetics ? null : "PlayerTestModel_V");
            double savedScale = AmigoPersistence.loadModelScale(ownerId);
            rec.modelScale = savedScale <= 0.0 ? 1.0 : savedScale;
            rec.customName = normalizeCustomName(AmigoPersistence.loadCustomName(ownerId));
            rec.level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
            rec.equippedWeaponId = AmigoPersistence.loadEquippedWeaponId(ownerId);
            rec.combatMode = AmigoPersistence.loadCombatMode(ownerId);
            rec.defendeEnabled = rec.combatMode == CombatMode.PROTECT_OWNER;
            rec.autoWeaponSwitchEnabled = AmigoPersistence.loadAutoWeaponSwitch(ownerId);
            rec.interruptAttacksEnabled = AmigoPersistence.loadInterruptAttacks(ownerId);
            rec.autoLootEnabled = AmigoPersistence.loadAutoLootEnabled(ownerId);
            rec.godMode = false;
            rec.totalXp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
            rec.npcLevelCached = XpProgression.levelFromTotalXp(rec.totalXp);
            int computedSwordLevel = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.npcLevelCached);
            if (computedSwordLevel != rec.level) {
               rec.level = computedSwordLevel;
               AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
            }

            rec.baseHp = AmigoPersistence.loadBaseHp(ownerId);
            rec.baseDef = AmigoPersistence.loadBaseDef(ownerId);
            this.npcRefPorPlayer.put(ownerId, rec);

            try {
               Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
               if (npcPlugin == null) {
                  setError(AmigoText.text("core.error.npcplugin.unavailable"));
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  return false;
               }

               Object pos = tryGetPositionFromStore(store, playerEntityRef);
               if (pos == null) {
                  pos = tryGetSenderPosition(senderObj);
               }

               pos = coerceToVector3d(pos);
               if (pos == null) {
                  setError(AmigoText.text("core.error.player_position.unavailable"));
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  return false;
               }

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

               pos = offsetInFrontOfYaw(pos, yaw, 4.0);
               Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
               if (rot == null) {
                  rot = newVector3f(0.0F, 0.0F, 0.0F);
               }

               Object model = buildPreferredSpawnModel(ownerId, store, playerEntityRef, rec.modelId, (float)rec.modelScale);
               if (model == null) {
                  setError(AmigoText.format("core.error.model_asset.invalid", rec.modelId));
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  return false;
               }

               int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, "Amigo_Follow");
               if (roleIndex < 0) {
                  setError(AmigoText.format("core.error.role.missing", "Amigo_Follow"));
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  return false;
               }

               Object pair = invokeSpawnEntity(npcPlugin, store, roleIndex, pos, rot, model, playerEntityRef);
               if (pair == null) {
                  setError(AmigoText.format("core.error.spawn_entity.null", rec.modelId));
                  this.npcRefPorPlayer.remove(ownerId, rec);
                  return false;
               }

               Object ref = extractRefFromPair(pair);
               rec.refObj = ref != null ? ref : pair;
               if (rec.refObj != null) {
                  this.amigoRefs.put(rec.refObj, ownerId);
               }

               if (rec.state == AmigoNpcManager.State.DESPAWNING) {
                  if (doRemoveEntity(store, rec.refObj)) {
                     this.finalizeRemove(ownerId, rec);
                  } else {
                     setError(AmigoText.text("core.error.spawn.remove_after_spawn_failed"));
                  }

                  return false;
               } else {
                  try {
                     this.applySwordWeaponNow(store, (Ref<EntityStore>)rec.refObj, ownerId, rec, true);
                     this.applyNpcScaling(store, (Ref<EntityStore>)rec.refObj, ownerId, rec, true);
                     armWardrobeRestoreRetry(rec, ownerId);

                     try {
                        if (rec.wardrobeRestorePending) {
                           boolean restored = AmigoWardrobePersistence.restoreNpcWardrobe(ownerId, store, playerEntityRef, (Ref<EntityStore>)rec.refObj);
                           if (restored) {
                              clearWardrobeRestoreRetry(rec);
                           } else {
                              rec.nextWardrobeRestoreMillis = System.currentTimeMillis() + 250L;
                           }
                        }
                     } catch (Throwable var22) {
                     }

                     playNpcSpawnFx(store, (Ref<EntityStore>)rec.refObj, playerEntityRef);
                     rec.spawnFxUntilMillis = System.currentTimeMillis() + 1800L;
                     rec.spawnFxNextMillis = 0L;
                  } catch (Throwable var23) {
                  }

                  rec.state = AmigoNpcManager.State.ACTIVE;
                  return true;
               }
            } catch (Throwable t) {
               setError(AmigoText.format("core.error.spawn_with_store.failed", t.getClass().getSimpleName(), t.getMessage()));
               this.npcRefPorPlayer.remove(ownerId, rec);
               if (rec.refObj != null) {
                  this.amigoRefs.remove(rec.refObj);
               }

               return false;
            }
         } else if (existing.state != AmigoNpcManager.State.SPAWNING && existing.state != AmigoNpcManager.State.ACTIVE) {
            setError(AmigoText.text("core.error.npc.removing"));
            return false;
         } else {
            setError(AmigoText.text("core.error.npc.already_exists"));
            return false;
         }
      } else {
         setError(AmigoText.text("core.error.spawn_with_store.invalid_args"));
         return false;
      }
   }

   public boolean despawnWithStore(Store<EntityStore> store, UUID ownerId) {
      if (store != null && ownerId != null) {
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            setError(AmigoText.text("core.error.npc.not_registered"));
            return false;
         }

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
                  if (rec.refObj instanceof Ref<?> rawNpcRef) {
                     @SuppressWarnings("unchecked")
                     Ref<EntityStore> npcRef = (Ref<EntityStore>)rawNpcRef;
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
            }

            if (!doRemoveEntity(store, rec.refObj)) {
               return false;
            }

            this.finalizeRemove(ownerId, rec);
            return true;
         }
      } else {
         setError(AmigoText.text("core.error.despawn_with_store.invalid_args"));
         return false;
      }
   }

   public boolean requestRespawn(Object worldObj, UUID ownerId, Object senderObj) {
      if (worldObj == null || ownerId == null) {
         setError(AmigoText.text("core.error.respawn.invalid_args"));
         return false;
      }

      long now = System.currentTimeMillis();
      long delay = 1000L + ThreadLocalRandom.current().nextLong(1001L);
      long at = now + delay;
      String msg = AmigoText.text("core.chat.respawn.arriving");
      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null) {
         this.pendingRespawns.put(ownerId, new PendingRespawn(worldObj, senderObj, at, msg));
         return true;
      }

      if (rec.downed) {
         long deadline = rec.downedUntilMillis;
         Object oldWorld = rec.worldObj != null ? rec.worldObj : worldObj;
         Object oldRef = rec.refObj;
         if (oldRef != null && oldWorld != null) {
            HytaleBridge.worldExecute(oldWorld, () -> {
               try {
                  Object oldStore = getComponentStoreFromWorld(oldWorld);
                  if (oldStore != null) {
                     doRemoveEntity(oldStore, oldRef);
                  }
               } catch (Throwable ignored) {
               }
               this.amigoRefs.remove(oldRef);
               if (rec.refObj == oldRef) {
                  rec.refObj = null;
               }
            });
         }

         rec.state = AmigoNpcManager.State.SPAWNING;
         return HytaleBridge.worldExecute(worldObj, () -> {
            try {
               Object storeObj = getComponentStoreFromWorld(worldObj);
               if (storeObj == null) {
                  rec.state = AmigoNpcManager.State.ACTIVE;
                  return;
               }

               this.spawnIntoExistingRecord(worldObj, storeObj, ownerId, senderObj, rec);
               if (rec.refObj != null) {
                  rec.downed = true;
                  rec.downedUntilMillis = deadline;
                  rec.deathDespawnAtMillis = 0L;
                  rec.state = AmigoNpcManager.State.ACTIVE;
                  if (rec.refObj instanceof Ref<?> rawRef && storeObj instanceof Store<?> rawStore) {
                     @SuppressWarnings("unchecked")
                     Ref<EntityStore> npcRef = (Ref<EntityStore>)rawRef;
                     @SuppressWarnings("unchecked")
                     Store<EntityStore> store = (Store<EntityStore>)rawStore;
                     MovementStatesComponent movement = store.getComponent(npcRef, MovementStatesComponent.getComponentType());
                     if (movement != null) {
                        MovementStates states = movement.getMovementStates();
                        if (states == null) {
                           states = new MovementStates();
                        }

                        states.sleeping = true;
                        states.idle = true;
                        states.horizontalIdle = true;
                        states.walking = false;
                        states.running = false;
                        states.sprinting = false;
                        states.onGround = true;
                        movement.setMovementStates(states);
                        movement.setSentMovementStates(new MovementStates(states));
                        store.putComponent(npcRef, MovementStatesComponent.getComponentType(), movement);
                     }
                  }
               }
            } catch (Throwable ignored) {
               rec.state = AmigoNpcManager.State.ACTIVE;
            }
         });
      }

      rec.respawnRequested = true;
      rec.respawnWorldObj = worldObj;
      rec.respawnSenderObj = senderObj;
      rec.respawnAtMillis = at;
      rec.respawnMessage = msg;
      return this.despawn(rec.worldObj != null ? rec.worldObj : worldObj, ownerId);
   }

   public boolean despawn(Object worldObj, UUID ownerId) {
      if (ownerId == null) {
         setError(AmigoText.text("core.error.despawn.invalid_owner"));
         return false;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null) {
         setError(AmigoText.text("core.error.npc.not_registered"));
         return false;
      }

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
         } else {
            return rec.refObj == null ? true : HytaleBridge.worldExecute(worldToUse, () -> {
               try {
                  Object componentStore = getComponentStoreFromWorld(worldToUse);
                  if (componentStore == null) {
                     return;
                  }

                  try {
                     doRemoveEntity(componentStore, rec.refObj);
                  } catch (Throwable var6x) {
                  }

                  try {
                     this.amigoRefs.remove(rec.refObj);
                  } catch (Throwable var5) {
                  }

                  rec.refObj = null;
               } catch (Throwable var7x) {
               }
            });
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
            setError(AmigoText.text("core.error.despawn.world_unavailable"));
            return false;
         } else if (rec.refObj == null) {
            return true;
         } else {
            boolean queued = HytaleBridge.worldExecute(worldToUse, () -> {
               try {
                  Object componentStore = getComponentStoreFromWorld(worldToUse);
                  if (componentStore == null) {
                     setError(AmigoText.text("core.error.store.unavailable"));
                     return;
                  }

                  try {
                     if (rec.refObj instanceof Ref<?> rawNpcRef) {
                        @SuppressWarnings("unchecked")
                        Ref<EntityStore> npcRef = (Ref<EntityStore>)rawNpcRef;
                        Store<EntityStore> store = (Store<EntityStore>)componentStore;
                        AmigoWardrobePersistence.saveNpcWardrobe(ownerId, store, npcRef);
                     }
                  } catch (Throwable var7x) {
                  }

                  if (!doRemoveEntity(componentStore, rec.refObj)) {
                     return;
                  }

                  this.finalizeRemove(ownerId, rec);
               } catch (Throwable t) {
                  setError(AmigoText.format("core.error.despawn.failed", t.getClass().getSimpleName(), t.getMessage()));
               }
            });
            if (!queued) {
               setError(AmigoText.format("core.error.world_execute.failed", HytaleBridge.getLastError()));
               return false;
            } else {
               return true;
            }
         }
      }
   }

   public boolean despawnStored(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         if (rec == null) {
            return false;
         } else {
            return rec.worldObj == null ? false : this.despawn(rec.worldObj, ownerId);
         }
      }
   }

   private void finalizeRemove(UUID ownerId, AmigoNpcManager.NpcRecord rec) {
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

   public void shutdownForReload() {
      ArrayList<AmigoNpcManager.NpcRecord> records = new ArrayList<>(this.npcRefPorPlayer.values());

      for (Entry<UUID, AmigoNpcManager.NpcRecord> entry : this.npcRefPorPlayer.entrySet()) {
         UUID ownerId = entry.getKey();
         AmigoNpcManager.NpcRecord rec = entry.getValue();
         if (ownerId == null || rec == null) {
            continue;
         }

         try {
            this.saveNpcWardrobeNowInternal(ownerId);
         } catch (Throwable ignored) {
         }

         try {
            if (rec.backpack != null) {
               AmigoPersistence.saveBackpack(ownerId, rec.backpack);
               rec.backpackDirty = false;
            }
         } catch (Throwable ignored) {
         }

         try {
            AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
         } catch (Throwable ignored) {
         }

         rec.respawnRequested = false;
         rec.respawnWorldObj = null;
         rec.respawnSenderObj = null;
      }

      this.pendingRespawns.clear();

      for (AmigoNpcManager.NpcRecord rec : records) {
         this.removeNpcForReload(rec);
      }

      this.debugLogByOwner.clear();
      this.npcRefPorPlayer.clear();
      this.amigoRefs.clear();
      WEAPON_ATTACK_ANIM_CACHE.clear();
      LAST_ERROR = null;
   }

   private void removeNpcForReload(AmigoNpcManager.NpcRecord rec) {
      if (rec == null || rec.worldObj == null || rec.refObj == null) {
         return;
      }

      Object worldObj = rec.worldObj;
      Object refObj = rec.refObj;

      try {
         if (worldObj instanceof World world && world.isInThread()) {
            Object storeObj = getComponentStoreFromWorld(worldObj);
            if (storeObj != null) {
               doRemoveEntity(storeObj, refObj);
            }
            this.amigoRefs.remove(refObj);
            rec.refObj = null;
            return;
         }
      } catch (Throwable ignored) {
      }

      CountDownLatch latch = new CountDownLatch(1);
      boolean queued = false;
      try {
         queued = HytaleBridge.worldExecute(worldObj, () -> {
            try {
               Object storeObj = getComponentStoreFromWorld(worldObj);
               if (storeObj != null) {
                  doRemoveEntity(storeObj, refObj);
               }
            } catch (Throwable ignored) {
            } finally {
               this.amigoRefs.remove(refObj);
               if (rec.refObj == refObj) {
                  rec.refObj = null;
               }
               latch.countDown();
            }
         });
      } catch (Throwable ignored) {
      }

      if (queued) {
         try {
            latch.await(2000L, TimeUnit.MILLISECONDS);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
      }

      this.amigoRefs.remove(refObj);
      if (rec.refObj == refObj) {
         rec.refObj = null;
      }
   }

   public SimpleItemContainer getOrLoadBackpack(UUID ownerId) {
      if (ownerId == null) {
         return new SimpleItemContainer((short)45);
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec != null) {
         if (rec.backpack == null) {
            rec.backpack = AmigoPersistence.loadBackpack(ownerId);
         }

         return rec.backpack;
      } else {
         return AmigoPersistence.loadBackpack(ownerId);
      }
   }

   public boolean isAmigoRef(Object refObj) {
      return refObj != null && this.amigoRefs.containsKey(refObj);
   }

   public UUID getOwnerFromRef(Object refObj) {
      return refObj == null ? null : this.amigoRefs.get(refObj);
   }

   public boolean isDowned(UUID ownerId) {
      AmigoNpcManager.NpcRecord rec = ownerId == null ? null : this.npcRefPorPlayer.get(ownerId);
      return rec != null && rec.downed;
   }

   public String getNpcDisplayName(UUID ownerId) {
      if (ownerId == null) {
         return "AmigoNPC";
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      String name = rec != null ? rec.customName : null;
      if (name == null || name.isBlank()) {
         try {
            name = AmigoPersistence.loadCustomName(ownerId);
         } catch (Throwable ignored) {
         }
      }

      return name == null || name.isBlank() ? "AmigoNPC" : name;
   }

   public String getActivityStateName(UUID ownerId) {
      if (ownerId == null) {
         return "INACTIVE";
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null) {
         return "INACTIVE";
      }

      if (rec.downed) {
         return "DOWNED";
      }

      if (rec.refObj == null || rec.state != AmigoNpcManager.State.ACTIVE) {
         return "INACTIVE";
      }

      return rec.activityState == null ? "IDLE" : rec.activityState.name();
   }

   public float getCachedHealth(UUID ownerId) {
      AmigoNpcManager.NpcRecord rec = ownerId == null ? null : this.npcRefPorPlayer.get(ownerId);
      return rec == null ? 0.0F : Math.max(0.0F, rec.cachedHealth);
   }

   public float getCachedMaxHealth(UUID ownerId) {
      AmigoNpcManager.NpcRecord rec = ownerId == null ? null : this.npcRefPorPlayer.get(ownerId);
      return rec == null ? 0.0F : Math.max(0.0F, rec.cachedMaxHealth);
   }

   public boolean hasRecentCombatParticipation(UUID ownerId, Object targetRefObj, long now) {
      if (ownerId == null || targetRefObj == null) {
         return false;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null || rec.combatTags == null) {
         return false;
      }

      synchronized (rec.combatTags) {
         for (CombatTag tag : rec.combatTags) {
            if (tag != null
               && refEq(tag.targetRefObj, targetRefObj)
               && tag.lastSeenMillis > 0L
               && now - tag.lastSeenMillis <= 30000L) {
               return true;
            }
         }
      }

      return false;
   }

   public void markDowned(UUID ownerId) {
      AmigoNpcManager.NpcRecord rec = ownerId == null ? null : this.npcRefPorPlayer.get(ownerId);
      if (rec != null) {
         if (!rec.downed) {
            rec.downed = true;
            rec.activityState = CompanionActivityState.DOWNED;
            rec.cachedHealth = 0.0F;
            long now = System.currentTimeMillis();
            rec.downedUntilMillis = now + DOWNED_AUTO_REVIVE_MILLIS;

            try {
               ActionTraceService.getShared().record(ownerId, "npc_state", "downed until=" + rec.downedUntilMillis);
            } catch (Throwable var12) {
            }

            rec.deathDespawnAtMillis = 0L;
            rec.nextDownedMessageMillis = now;
            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
            rec.assistUntilMillis = 0L;
            rec.assistTargetRefObj = null;
            rec.ownerCombatContextUntilMillis = 0L;
            rec.chaseDisengaged = false;
            rec.targetLostSinceMillis = 0L;
            rec.targetStuckSinceMillis = 0L;
            rec.lastTargetHorizontal = -1.0;
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

                        Store<EntityStore> store = (Store<EntityStore>)storeObj;
                        Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                        Object npcEntityObj = getComponentFromStore(store, npcRef, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                           setMarkedTargetOnNpcEntity(npcEntityObj, "LockedTarget", null);
                           setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", null);
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
                              s.sleeping = true;
                              ms.setMovementStates(s);
                              ms.setSentMovementStates(new MovementStates(s));
                              store.putComponent(npcRef, MovementStatesComponent.getComponentType(), ms);
                           }
                        } catch (Throwable var8) {
                        }
                     } catch (Throwable var9x) {
                     }
                  });
               }
            } catch (Throwable var11) {
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

                        Store<EntityStore> store = (Store<EntityStore>)storeObj;
                        Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                        NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
                     } catch (Throwable var5x) {
                     }
                  });
               }
            } catch (Throwable var9) {
            }
         }
      }
   }

   public NpcLevelProgressSnapshot getLevelProgressSnapshot(UUID ownerId) {
      return NpcQueryService.getLevelProgressSnapshot(this.npcRefPorPlayer, ownerId);
   }

   public Color getZoneHudColor(UUID ownerId) {
      return NpcQueryService.getZoneHudColor(this.npcRefPorPlayer, ownerId);
   }

   public int getZoneForHud(UUID ownerId) {
      return NpcQueryService.getZoneForHud(this.npcRefPorPlayer, ownerId);
   }

   public String getZoneNameForHud(UUID ownerId) {
      return NpcQueryService.getZoneNameForHud(this.npcRefPorPlayer, ownerId);
   }

   public int getNpcLevel(UUID ownerId) {
      return NpcQueryService.getNpcLevel(this.npcRefPorPlayer, ownerId);
   }

   public long getNpcTotalXp(UUID ownerId) {
      return NpcQueryService.getNpcTotalXp(this.npcRefPorPlayer, ownerId);
   }

   public boolean isInCombat(UUID ownerId, long nowMillis) {
      return NpcQueryService.isInCombat(this.npcRefPorPlayer, ownerId, nowMillis);
   }

   public boolean addNpcXpViaApi(UUID ownerId, long amount, NpcXpSource source, NpcXpContext ctx) {
      return NpcProgressionActionService.addNpcXpViaApi(
         this.npcRefPorPlayer, ownerId, amount, source, ctx, AmigoNpcManager::getComponentStoreFromWorld, this::addNpcXp
      );
   }

   public void requestRescale(UUID ownerId) {
      NpcProgressionActionService.requestRescale(
         this.npcRefPorPlayer, ownerId, AmigoNpcManager::getComponentStoreFromWorld, (store, npcRef, resolvedOwnerId, rec) -> {
            this.applyNpcScaling(store, npcRef, resolvedOwnerId, rec, false);

            try {
               NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
            } catch (Throwable var6) {
            }
         }
      );
   }

   public float mitigateIncomingDamage(UUID ownerId, float incomingAmount) {
      try {
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         long totalXp = rec != null ? rec.totalXp : AmigoPersistence.loadTotalXp(ownerId);
         return NpcProgressionSupport.mitigateIncomingDamage(ownerId, totalXp, incomingAmount);
      } catch (Throwable var6) {
         return Math.max(0.0F, incomingAmount);
      }
   }

   private void applyNpcScaling(Store<EntityStore> store, Ref<EntityStore> npcRef, UUID ownerId, AmigoNpcManager.NpcRecord rec, boolean healToFull) {
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

               long baseHp = Math.max(1L, Math.round(base));
               rec.baseHp = baseHp;
               AmigoPersistence.saveBaseHp(ownerId, baseHp);
               updatedBases = true;
            }

            if (rec.baseDef < 0L) {
               long baseDef = 1L;
               int defIdx = NpcScalingSupport.resolveDefenseStatIndex();
               if (defIdx >= 0) {
                  try {
                     stats.maximizeStatValue(defIdx);
                     float defVal = stats.get(defIdx).get();
                     baseDef = Math.max(0L, Math.round(defVal));
                  } catch (Throwable ignored) {
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

            try {
               if (NpcStatsConfigService.getShared().get().EnableNpcStats) {
                  NpcStatsState st = NpcStatsService.getShared().load(ownerId);
                  long extraDef = AttributeModifierService.getShared().extraDefense(st);
                  if (extraDef > 0L) {
                     defValScaled = Math.max(0L, defValScaled + extraDef);
                  }
               }
            } catch (Throwable ignored) {
            }

            try {
               Object ownerRefObj = rec.worldObj == null ? null : invokeOneArg(rec.worldObj, "getEntityRef", UUID.class, ownerId);
               if (ownerRefObj instanceof Ref<?> rawOwnerRef) {
                  @SuppressWarnings("unchecked")
                  Ref<EntityStore> ownerRef = (Ref<EntityStore>)rawOwnerRef;
                  EntityStatMap ownerStats = (EntityStatMap)store.getComponent(ownerRef, EntityStatMap.getComponentType());
                  if (ownerStats != null) {
                     try {
                        float ownerMaxHp = ownerStats.get(healthIdx).getMax();
                        if (ownerMaxHp > 0.0F) {
                           desiredMaxHp = Math.min(desiredMaxHp, Math.max(1L, (long)Math.floor(ownerMaxHp * 0.90)));
                        }
                     } catch (Throwable ignored) {
                     }

                     int ownerDefIdx = NpcScalingSupport.resolveDefenseStatIndex();
                     if (ownerDefIdx >= 0) {
                        try {
                           float ownerDef = ownerStats.get(ownerDefIdx).get();
                           if (ownerDef >= 0.0F) {
                              defValScaled = Math.min(defValScaled, Math.max(0L, (long)Math.floor(ownerDef * 0.90)));
                           }
                        } catch (Throwable ignored) {
                        }
                     }
                  }
               }
            } catch (Throwable ignored) {
            }

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
               baseMaxHp = Math.max(1L, Math.round(baseMaxF));
            } catch (Throwable ignored) {
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

            int defIdx = NpcScalingSupport.resolveDefenseStatIndex();
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
               NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
            } catch (Throwable var24) {
            }
         } catch (Throwable var38) {
         }
      }
   }

   private void addNpcXp(
      Store<EntityStore> store, Ref<EntityStore> npcRef, UUID ownerId, AmigoNpcManager.NpcRecord rec, long gain, NpcXpSource source, NpcXpContext ctx
   ) {
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

            try {
               AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
            } catch (Throwable var21) {
            }
         }

         try {
            NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
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

   public int getSwordLevel(UUID ownerId) {
      return NpcSwordService.getSwordLevel(this.npcRefPorPlayer, ownerId);
   }

   public int changeSwordLevel(UUID ownerId, int delta, boolean announce) {
      return NpcSwordService.changeSwordLevel(
         this.npcRefPorPlayer,
         ownerId,
         delta,
         announce,
         AmigoNpcManager::getComponentStoreFromWorld,
         this::applySwordWeaponNow,
         (store, npcRef, resolvedOwnerId, rec) -> {
            this.applyNpcScaling(store, npcRef, resolvedOwnerId, rec, false);
            NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
         },
         this::sendToOwner
      );
   }

   private void sendToOwner(Object worldObj, UUID ownerId, String text) {
      if (worldObj != null && ownerId != null && text != null && !text.isBlank()) {
         HytaleBridge.worldExecute(worldObj, () -> {
            try {
               Object storeObj = getComponentStoreFromWorld(worldObj);
               if (storeObj == null) {
                  return;
               }

               Store<EntityStore> store = (Store<EntityStore>)storeObj;
               Ref<EntityStore> ownerRef = (Ref<EntityStore>)invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
               if (ownerRef == null) {
                  return;
               }

               PlayerRef playerRef = (PlayerRef)store.getComponent(ownerRef, PlayerRef.getComponentType());
               if (playerRef == null) {
                  return;
               }

               playerRef.sendMessage(Message.raw(AmigoText.format("core.chat.prefix", text)));
            } catch (Throwable var7) {
            }
         });
      }
   }

   private static String resolveConfiguredOrLegacyMeleeWeaponId(AmigoNpcManager.NpcRecord rec) {
      return rec == null ? null : rec.equippedWeaponId;
   }

   private void applySwordWeaponNow(Store<EntityStore> store, Ref<EntityStore> npcRef, UUID ownerId, AmigoNpcManager.NpcRecord rec, boolean saveNow) {
      if (store == null || npcRef == null || rec == null || ownerId == null) {
         return;
      }

      if (rec.backpack == null) {
         rec.backpack = this.getOrLoadBackpack(ownerId);
      }

      String expected;
      if (rec.autoWeaponSwitchEnabled) {
         expected = NpcWeaponSupport.selectBestBackpackWeapon(rec.backpack, false);
      } else {
         expected = NpcWeaponSupport.backpackContainsWeapon(rec.backpack, rec.equippedWeaponId) ? rec.equippedWeaponId : null;
      }

      int lvl = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.level);
      if (expected == null || expected.isBlank()) {
         NpcWeaponSupport.clearHotbar0(store, npcRef);
         rec.equippedWeaponId = null;
         if (saveNow) {
            AmigoPersistence.saveSwordState(ownerId, lvl, null);
         }
         return;
      }

      if (NpcWeaponSupport.isHotbar0Item(store, npcRef, expected)) {
         rec.equippedWeaponId = expected;
         if (saveNow) {
            AmigoPersistence.saveSwordState(ownerId, lvl, expected);
         }
         return;
      }

      if (NpcWeaponSupport.equipWeaponInHotbar0(store, npcRef, expected)) {
         rec.equippedWeaponId = expected;
         if (saveNow) {
            AmigoPersistence.saveSwordState(ownerId, lvl, expected);
         }
         this.debugEquip(rec, ownerId, AmigoText.format("core.debug.equip.success", expected, lvl));
      } else {
         this.debugEquip(rec, ownerId, AmigoText.format("core.debug.equip.failed", expected, lvl, NpcWeaponSupport.getHotbar0ItemId(store, npcRef)));
      }
   }

   private void debugEquip(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg) {
      NpcDebugLogSupport.debugEquip(rec, ownerId, msg, this::sendToOwner);
   }

   private void debugCombat(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg) {
      NpcDebugLogSupport.debugCombat(rec, ownerId, msg, this::sendToOwner);
   }

   private void debugAttack(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg) {
      NpcDebugLogSupport.debugAttack(rec, ownerId, msg, this::sendToOwner);
   }

   private void debugZones(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg) {
      NpcDebugLogSupport.debugZones(rec, ownerId, msg, this::sendToOwner);
   }

   private void debugMobLevel(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg) {
      NpcDebugLogSupport.debugMobLevel(rec, ownerId, msg, this::sendToOwner);
   }

   public void debugDamage(UUID ownerId, String msg) {
      NpcDebugLogSupport.debugDamage(this.npcRefPorPlayer, ownerId, msg, this::sendToOwner);
   }

   private static boolean refEq(Object a, Object b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         try {
            return a.equals(b);
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void startCombat(UUID ownerId, Object attackerRefObj) {
      NpcCombatStateSupport.startCombat(this.npcRefPorPlayer.get(ownerId), ownerId, attackerRefObj, 3000L, this::debugCombat);
   }

   public void tryInterruptOwnerAttacker(Store<EntityStore> store, UUID ownerId, Object attackerRefObj) {
      if (store == null || ownerId == null || attackerRefObj == null) {
         return;
      }

      AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
      if (rec == null || rec.downed || !rec.interruptAttacksEnabled || rec.refObj == null) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - rec.lastInterruptMillis < 1500L) {
         return;
      }

      try {
         Object attackerNpc = getComponentFromStore(store, attackerRefObj, NPCEntity.getComponentType());
         if (attackerNpc == null) {
            return;
         }

         setMarkedTargetOnNpcEntity(attackerNpc, "CombatTarget", rec.refObj);
         setLockedTargetOnNpcEntity(attackerNpc, rec.refObj);
         rec.lastInterruptMillis = now;
         this.debugCombat(rec, ownerId, "interrupt: attacker redirected to companion");
      } catch (Throwable ignored) {
      }
   }

   public void startNpcCombat(UUID ownerId, Object attackerRefObj) {
      NpcCombatStateSupport.startNpcCombat(this.npcRefPorPlayer.get(ownerId), ownerId, attackerRefObj, 3000L, this::debugCombat);
   }

   public void startAssist(UUID ownerId, Object targetRefObj) {
      NpcCombatStateSupport.startAssist(this.npcRefPorPlayer.get(ownerId), ownerId, targetRefObj, this::debugCombat);
   }

   public void recordCombatTag(UUID ownerId, Object targetRefObj, Store<EntityStore> store) {
      if (ownerId != null && targetRefObj != null && store != null) {
         AmigoNpcManager.NpcRecord rec = this.npcRefPorPlayer.get(ownerId);
         NpcLootStateSupport.recordCombatTag(rec, targetRefObj, store, 16);
      }
   }

   private Object getActiveAssistTarget(AmigoNpcManager.NpcRecord rec, long now) {
      return NpcCombatStateSupport.getActiveAssistTarget(rec, now);
   }

   private void clearAssist(AmigoNpcManager.NpcRecord rec) {
      NpcCombatStateSupport.clearAssist(rec);
   }

   private static boolean isValidEntityRef(Object store, Object refObj) {
      return NpcEntityQuerySupport.isValidEntityRef(store, refObj, AmigoNpcManager::getComponentFromStore);
   }

   private static boolean isAliveEntityRef(Object store, Object refObj) {
      return NpcEntityQuerySupport.isAliveEntityRef(store, refObj, AmigoNpcManager::getComponentFromStore);
   }

   private static double getEntityHeight(Object store, Object refObj) {
      return NpcEntityQuerySupport.getEntityHeight(store, refObj, AmigoNpcManager::getComponentFromStore);
   }

   private static double getEntityRadiusXZ(Object store, Object refObj) {
      return NpcEntityQuerySupport.getEntityRadiusXZ(store, refObj, AmigoNpcManager::getComponentFromStore);
   }

   private static void tickAssistHousekeeping(AmigoNpcManager.NpcRecord rec, Object store, long now) {
      NpcEntityQuerySupport.tickAssistHousekeeping(rec, store, now, 3000L, AmigoNpcManager::getComponentFromStore);
   }

   private Object findNearestDefenderTarget(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, Vector3d ownerPos) {
      return NpcTargetAcquisitionSupport.findWeakestCombatTarget(
         store, rec, ownerRefObj, ownerPos, this.pvpEnabled, 12.0, 2.5, 35.0, this::isAmigoRef
      );
   }

   private Object findNearestTargetNearNpc(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, Vector3d npcPos, Object excludeRefObj) {
      return NpcTargetAcquisitionSupport.findWeakestCombatTarget(
         store, rec, ownerRefObj, npcPos, this.pvpEnabled, 12.0, 2.5, 35.0, this::isAmigoRef
      );
   }

   private static boolean isAnyEnemyNearNpc(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, Vector3d npcPos, double radius) {
      return NpcTargetAcquisitionSupport.isAnyEnemyNearNpc(store, rec, ownerRefObj, npcPos, radius, 2.5, 35.0);
   }

   private void clearCombatTagsAndLoot(AmigoNpcManager.NpcRecord rec) {
      NpcLootStateSupport.clearCombatTagsAndLoot(rec);
   }

   private static boolean isItemRefValid(Store<EntityStore> store, Object refObj) {
      return NpcLootStateSupport.isItemRefValid(store, refObj);
   }

   private static void lootChatAccAdd(AmigoNpcManager.NpcRecord rec, String itemId, int qty, long now) {
      NpcLootStateSupport.lootChatAccAdd(rec, itemId, qty, now, 500L);
   }

   private void lootChatAccFlushIfDue(AmigoNpcManager.NpcRecord rec, UUID ownerId, Object worldObj, long now) {
      NpcLootStateSupport.lootChatAccFlushIfDue(rec, ownerId, worldObj, now, this::sendToOwner);
   }

   private boolean tryPickupGroundItemIntoBackpack(
      Store<EntityStore> store, UUID ownerId, AmigoNpcManager.NpcRecord rec, SimpleItemContainer bag, Object itemRefObj
   ) {
      return NpcBackpackLootSupport.tryPickupGroundItemIntoBackpack(
         store, ownerId, rec, bag, itemRefObj, this::maybeNotifyBackpackFull, AmigoNpcManager::lootChatAccAdd
      );
   }

   private void refreshPendingLootFromCombatTags(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Vector3d npcPos, Vector3d ownerPos, long now) {
      NpcLootStateSupport.refreshPendingLootFromCombatTags(store, rec, npcPos, ownerPos, now, 6.0, 512, AUTOLOOT_QUERY);
   }

   private static boolean containsRef(ArrayList<Object> list, Ref<EntityStore> ref) {
      return NpcLootStateSupport.containsRef(list, ref);
   }

   private Object chooseNearestPendingLoot(AmigoNpcManager.NpcRecord rec, Store<EntityStore> store, Vector3d npcPos) {
      return NpcLootStateSupport.chooseNearestPendingLoot(rec, store, npcPos);
   }

   private void tickCombatTaggedLooting(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d npcPos,
      Vector3d ownerPos,
      long now,
      Object worldObj,
      boolean inCombatOrAssistNow
   ) {
      if (store != null && ownerId != null && rec != null && npcPos != null) {
         if (!rec.downed) {
            if (rec.state == AmigoNpcManager.State.ACTIVE) {
               NpcCombatLootFlowSupport.PreLootAction preLootAction = NpcCombatLootFlowSupport.prepareCombatTaggedLooting(
                  rec,
                  ownerId,
                  now,
                  ownerPos,
                  inCombatOrAssistNow,
                  500L,
                  25000L,
                  25.0,
                  () -> ownerRefObj != null && isAnyEnemyNearNpc(store, rec, ownerRefObj, npcPos, 4.0)
               );
               if (preLootAction != NpcCombatLootFlowSupport.PreLootAction.RETURN) {
                  if (preLootAction == NpcCombatLootFlowSupport.PreLootAction.CLEAR_AND_RETURN) {
                     this.clearCombatTagsAndLoot(rec);
                  } else {
                     if (rec.backpack == null) {
                        try {
                           rec.backpack = this.getOrLoadBackpack(ownerId);
                        } catch (Throwable var13) {
                        }
                     }

                     SimpleItemContainer bag = rec.backpack;
                     if (bag != null) {
                        rec.lootPausedInventoryFull = false;
                        if (NpcCombatLootTargetSupport.ensureLootTarget(
                           store, rec, npcPos, ownerPos, now, this::refreshPendingLootFromCombatTags, this::chooseNearestPendingLoot
                        )) {
                           if (NpcCombatLootTargetSupport.validateLootTarget(store, rec, now, 12000L, 4000L, AmigoNpcManager::isItemRefValid)) {
                              NpcCombatLootTargetSupport.steerNpcToLootTarget(
                                 store,
                                 rec,
                                 NPCEntity.getComponentType(),
                                 AmigoNpcManager::getComponentFromStore,
                                 AmigoNpcManager::setLockedTargetOnNpcEntity,
                                 AmigoNpcManager::setMarkedTargetOnNpcEntity,
                                 AmigoNpcManager::setFlockState
                              );
                              NpcCombatLootTargetSupport.tryPickupLootTarget(
                                 store, ownerId, rec, bag, npcPos, now, 5.0, 4000L, this::tryPickupGroundItemIntoBackpack
                              );
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void tryAutoLoot(
      Store<EntityStore> store, UUID ownerId, AmigoNpcManager.NpcRecord rec, Vector3d npcPos, Vector3d ownerPos, long now, Object worldObj
   ) {
      if (rec != null && store != null && ownerId != null && npcPos != null) {
         if (!rec.autoLootEnabled) {
            endCombatTaggedLooting(rec);
         } else if (!rec.downed) {
            if (rec.state == AmigoNpcManager.State.ACTIVE) {
               NpcAutoLootSupport.ScanSetup scanSetup = NpcAutoLootSupport.prepareScan(rec, npcPos, now, 250L, 5.0, 8);
               if (scanSetup != null) {
                  double r2 = scanSetup.radiusSq;
                  int maxPerScan = scanSetup.maxPerScan;
                  double maxDyFinal = scanSetup.maxDy;
                  Vector3d lootCenterPos = scanSetup.lootCenterPos;
                  SimpleItemContainer bag = NpcAutoLootSupport.prepareBackpack(
                     rec,
                     ownerId,
                     worldObj,
                     now,
                     this::getOrLoadBackpack,
                     this::debugCombat,
                     this::maybeNotifyBackpackFull,
                     AmigoNpcManager::isBackpackCompletelyFull
                  );
                  if (bag != null) {
                     NpcAutoLootSupport.ScanResult scanResult = NpcAutoLootSupport.scanNearbyDrops(
                        store,
                        ownerId,
                        rec,
                        bag,
                        worldObj,
                        now,
                        lootCenterPos,
                        ownerPos,
                        r2,
                        400.0,
                        maxDyFinal,
                        maxPerScan,
                        400L,
                        AUTOLOOT_QUERY,
                        this::debugCombat,
                        this::maybeNotifyBackpackFull,
                        AmigoNpcManager::isBackpackCompletelyFull,
                        AmigoNpcManager::lootChatAccAdd
                     );
                     NpcAutoLootSupport.finalizeScan(
                        store,
                        ownerId,
                        rec,
                        bag,
                        now,
                        worldObj,
                        scanResult.removeLater,
                        scanResult.updateLaterRef,
                        scanResult.updateLaterComp,
                        scanResult.pickedCount,
                        this::debugCombat,
                        this::lootChatAccFlushIfDue,
                        AmigoNpcManager::doRemoveEntity
                     );
                  }
               }
            }
         }
      }
   }

   private static boolean isBackpackCompletelyFull(SimpleItemContainer bag) {
      return NpcBackpackLootSupport.isBackpackCompletelyFull(bag);
   }

   private static double distSq(Vector3d a, Vector3d b) {
      double dx = a.x() - b.x();
      double dy = a.y() - b.y();
      double dz = a.z() - b.z();
      return dx * dx + dy * dy + dz * dz;
   }

   private void maybeNotifyBackpackFull(AmigoNpcManager.NpcRecord rec, UUID ownerId, Object worldObj, long now) {
      NpcBackpackLootSupport.maybeNotifyBackpackFull(rec, ownerId, worldObj, now, 30000L, this::sendToOwner);
   }

   private Object getActiveCombatTarget(AmigoNpcManager.NpcRecord rec, long now) {
      return NpcCombatStateSupport.getActiveCombatTarget(rec, now);
   }

   private Object getActiveNpcCombatTarget(AmigoNpcManager.NpcRecord rec, long now) {
      return NpcCombatStateSupport.getActiveNpcCombatTarget(rec, now);
   }

   public void tickDowned() {
      long now = System.currentTimeMillis();

      for (Entry<UUID, AmigoNpcManager.NpcRecord> e : this.npcRefPorPlayer.entrySet()) {
         UUID owner = e.getKey();
         AmigoNpcManager.NpcRecord rec = e.getValue();
         if (rec == null || !rec.downed || rec.downedUntilMillis <= 0L) {
            continue;
         }

         if (rec.worldObj != null && now >= rec.nextDownedMessageMillis) {
            String msg = DOWNED_CHAT_MESSAGES[ThreadLocalRandom.current().nextInt(DOWNED_CHAT_MESSAGES.length)];
            this.sendToOwner(rec.worldObj, owner, msg);
            rec.nextDownedMessageMillis = now + ThreadLocalRandom.current().nextLong(
               DOWNED_CHAT_MIN_INTERVAL_MILLIS, DOWNED_CHAT_MAX_INTERVAL_MILLIS + 1L
            );
         }

         if (rec.worldObj != null && rec.refObj != null) {
            Object worldObj = rec.worldObj;
            Object refAtSchedule = rec.refObj;
            HytaleBridge.worldExecute(worldObj, () -> {
               try {
                  if (rec.refObj != refAtSchedule || !rec.downed) {
                     return;
                  }

                  Object storeObj = getComponentStoreFromWorld(worldObj);
                  if (!(storeObj instanceof Store<?> rawStore)) {
                     return;
                  }

                  Store<EntityStore> store = (Store<EntityStore>)rawStore;
                  Object ownerRefObj = invokeOneArg(worldObj, "getEntityRef", UUID.class, owner);
                  if (!(ownerRefObj instanceof Ref<?> ownerRefRaw) || !(refAtSchedule instanceof Ref<?> npcRefRaw)) {
                     return;
                  }

                  @SuppressWarnings("unchecked")
                  Ref<EntityStore> ownerRef = (Ref<EntityStore>)ownerRefRaw;
                  @SuppressWarnings("unchecked")
                  Ref<EntityStore> npcRef = (Ref<EntityStore>)npcRefRaw;
                  TransformComponent ownerTransform = store.getComponent(ownerRef, TransformComponent.getComponentType());
                  TransformComponent npcTransform = store.getComponent(npcRef, TransformComponent.getComponentType());
                  if (ownerTransform == null || npcTransform == null || ownerTransform.getPosition() == null || npcTransform.getPosition() == null) {
                     return;
                  }

                  if (distSq(ownerTransform.getPosition(), npcTransform.getPosition()) <= DOWNED_BODY_HIDE_DISTANCE * DOWNED_BODY_HIDE_DISTANCE) {
                     return;
                  }

                  doRemoveEntity(store, refAtSchedule);
                  this.amigoRefs.remove(refAtSchedule);
                  if (rec.refObj == refAtSchedule) {
                     rec.refObj = null;
                  }
               } catch (Throwable ignored) {
               }
            });
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
                     } catch (Throwable ignored) {
                     }
                     this.amigoRefs.remove(oldRef);
                     rec.refObj = null;
                  }

                  this.spawnIntoExistingRecord(worldObj, storeObj, owner, null, rec);
                  if (rec.refObj == null) {
                     rec.state = AmigoNpcManager.State.ACTIVE;
                     rec.downed = true;
                     return;
                  }

                  this.applyRevivePenalty(rec, owner, false);
                  rec.downed = false;
                  rec.downedUntilMillis = 0L;
                  rec.nextDownedMessageMillis = 0L;
                  if (rec.refObj instanceof Ref<?> rawRef && storeObj instanceof Store<?> rawStore) {
                     @SuppressWarnings("unchecked")
                     Ref<EntityStore> npcRef = (Ref<EntityStore>)rawRef;
                     @SuppressWarnings("unchecked")
                     Store<EntityStore> store = (Store<EntityStore>)rawStore;
                     this.applyNpcScaling(store, npcRef, owner, rec, true);
                     NpcHudSyncSupport.updateNpcHud(store, npcRef, rec);
                  }
               } catch (Throwable ignored) {
                  rec.state = AmigoNpcManager.State.ACTIVE;
               }
            });
            if (!queued) {
               rec.state = AmigoNpcManager.State.ACTIVE;
            }
         }
      }
   }

   public void tickFollow() {
      if (!this.pendingRespawns.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<Entry<UUID, PendingRespawn>> it = this.pendingRespawns.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, PendingRespawn> en = it.next();
            UUID ownerId = en.getKey();
            PendingRespawn pr = en.getValue();
            if (pr == null || pr.worldObj == null) {
               it.remove();
            } else if (now >= pr.atMillis) {
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
         }
      }

      for (Entry<UUID, AmigoNpcManager.NpcRecord> e : this.npcRefPorPlayer.entrySet()) {
         UUID owner = e.getKey();
         AmigoNpcManager.NpcRecord rec = e.getValue();
         if (rec != null && rec.state == AmigoNpcManager.State.ACTIVE && rec.refObj != null && rec.worldObj != null && !rec.downed) {
            Object worldObj = rec.worldObj;
            HytaleBridge.worldExecute(
               worldObj,
               () -> {
                  try {
                     if (!(getComponentStoreFromWorld(worldObj) instanceof Store<?> rawStore)) {
                        return;
                     }

                     Store<EntityStore> store = (Store<EntityStore>)rawStore;
                     Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, owner);
                     if (ownerRef == null) {
                        return;
                     }

                     Object ownerTransform = getComponentFromStore(store, ownerRef, TransformComponent.getComponentType());
                     Object npcTransform = getComponentFromStore(store, rec.refObj, TransformComponent.getComponentType());
                     if (!(npcTransform instanceof TransformComponent)) {
                        this.amigoRefs.remove(rec.refObj);
                        this.npcRefPorPlayer.remove(owner, rec);
                        return;
                     }

                     try {
                        Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                        EntityStatMap st = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
                        if (st != null) {
                           try {
                              EntityStatValue hp = st.get(DefaultEntityStatTypes.getHealth());
                              if (hp != null) {
                                 rec.cachedHealth = Math.max(0.0F, hp.get());
                                 rec.cachedMaxHealth = Math.max(0.0F, hp.getMax());
                                 if (hp.get() <= 0.0F) {
                                    this.markDowned(owner);
                                    return;
                                 }
                              }
                           } catch (Throwable var26) {
                           }
                        }
                     } catch (Throwable var27) {
                     }

                     if (!(ownerTransform instanceof TransformComponent ot)) {
                        return;
                     }

                     TransformComponent nt = (TransformComponent)npcTransform;
                     Vector3d op = ot.getPosition();
                     Vector3d np = nt.getPosition();
                     if (op == null || np == null) {
                        return;
                     }

                     NpcFollowContextSupport.Context followContext = NpcFollowContextSupport.prepare(
                        store,
                        worldObj,
                        ownerRef,
                        rec,
                        op,
                        np,
                        10.0,
                        500L,
                        AmigoNpcManager::hasCeilingAboveBestEffort,
                        AmigoNpcManager::clearExpiredAttackAnimation,
                        AmigoNpcManager::tickLevelUpFx,
                        AmigoNpcManager::tickWardrobeRestore
                     );
                     long nowx = followContext.now;
                     double horizontal = followContext.horizontal;
                     double dy = followContext.dy;
                     boolean ownerUnderground = followContext.ownerUnderground;
                     boolean lootStickActiveNow = false;

                     try {
                        NpcFollowCombatSupport.tick(
                           store,
                           owner,
                           rec,
                           ownerRef,
                           op,
                           np,
                           nowx,
                           horizontal,
                           ownerUnderground,
                           lootStickActiveNow,
                           "LockedTargetClose",
                           30.0,
                           3000L,
                           3000L,
                           35.0,
                           AmigoNpcManager::getComponentFromStore,
                           AmigoNpcManager::setMarkedTargetOnNpcEntity,
                           AmigoNpcManager::setLockedTargetOnNpcEntity,
                           AmigoNpcManager::setFlockState,
                           AmigoNpcManager::isAliveEntityRef,
                           AmigoNpcManager::tickAssistHousekeeping,
                           this::findNearestDefenderTarget,
                           this::findNearestTargetNearNpc,
                           this::debugCombat,
                           AmigoNpcManager::isAirborneTarget,
                           AmigoNpcManager::getEntityHeight,
                           this::updateCurrentTargetMobLevel,
                           this::tryEquipDefaultBow,
                           this::tryRangedAttack,
                           this::applySwordWeaponNow,
                           this::tryMeleeAttack
                        );
                     } catch (Throwable var25) {
                     }

                     boolean inCombatOrAssistNow = NpcFollowRecoveryLootSupport.tick(
                        store,
                        owner,
                        rec,
                        ownerRef,
                        op,
                        np,
                        nowx,
                        worldObj,
                        3000L,
                        this::tickSpawnFollowFx,
                        this::tickAutoRegen,
                        this::tickCombatTaggedLooting,
                        this::tryAutoLoot,
                        AmigoNpcManager::endCombatTaggedLooting
                     );
                     boolean inCombatOrAssist = inCombatOrAssistNow;

                     if (rec.downed) {
                        rec.activityState = CompanionActivityState.DOWNED;
                     } else if (inCombatOrAssist) {
                        rec.activityState = CompanionActivityState.COMBAT;
                        rec.gatherTargetX = Integer.MIN_VALUE;
                        rec.gatherTargetY = Integer.MIN_VALUE;
                        rec.gatherTargetZ = Integer.MIN_VALUE;
                     } else if (worldObj instanceof World world) {
                        NpcGatheringSupport.Result gatheringResult = NpcGatheringSupport.tick(
                           world, owner, rec, np, nowx, false, this::maybeNotifyBackpackFull
                        );
                        rec.activityState = switch (gatheringResult) {
                           case GATHERED -> CompanionActivityState.GATHERING;
                           case FULL, FOLLOWING -> CompanionActivityState.FOLLOWING;
                           case COMBAT -> CompanionActivityState.COMBAT;
                           case IDLE -> CompanionActivityState.IDLE;
                        };
                     } else {
                        rec.activityState = CompanionActivityState.FOLLOWING;
                     }

                     NpcFollowRescueSupport.tryRescue(
                        store,
                        owner,
                        rec,
                        ownerRef,
                        op,
                        ot,
                        nt,
                        nowx,
                        horizontal,
                        dy,
                        inCombatOrAssist,
                        ownerUnderground,
                        10.0,
                        3000L,
                        NPCEntity.getComponentType(),
                        AmigoNpcManager::getComponentFromStore,
                        AmigoNpcManager::setLockedTargetOnNpcEntity,
                        AmigoNpcManager::setMarkedTargetOnNpcEntity,
                        AmigoNpcManager::extractYawFromRotation,
                        AmigoNpcManager::offsetInFrontOfYaw,
                        AmigoNpcManager::coerceToVector3d,
                        this::debugCombat
                     );
                  } catch (Throwable var28) {
                  }
               }
            );
         }
      }
   }

   private static void applyMovementAnimation(
      Store<EntityStore> store, Ref<EntityStore> npcRef, MovementStates ownerStates, double ownerSpeed, double npcSpeed, boolean moving, double distance
   ) {
      try {
         MovementStatesComponent msComp = (MovementStatesComponent)store.ensureAndGetComponent(npcRef, MovementStatesComponent.getComponentType());
         MovementStates s = ownerStates != null ? new MovementStates(ownerStates) : new MovementStates();
         s.onGround = true;
         if (moving && !(npcSpeed < 0.2)) {
            boolean ownerSprint = ownerStates != null && ownerStates.sprinting;
            boolean ownerRun = ownerStates != null && (ownerStates.running || ownerStates.sprinting);
            boolean wantSprint = ownerSprint || ownerSpeed > 6.0 || distance > 35.0;
            boolean wantRun = ownerRun || ownerSpeed > 4.2 || distance > 25.0;
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
      AmigoNpcManager.NpcRecord rec = ownerId == null ? null : this.npcRefPorPlayer.get(ownerId);
      if (rec == null || !rec.downed || rec.refObj == null || rec.worldObj == null) {
         return;
      }

      this.applyRevivePenalty(rec, ownerId, manual);
      rec.downed = false;
      rec.downedUntilMillis = 0L;
      rec.deathDespawnAtMillis = 0L;
      rec.nextDownedMessageMillis = 0L;
      rec.activityState = CompanionActivityState.FOLLOWING;
      HytaleBridge.worldExecute(rec.worldObj, () -> {
         try {
            if (!(getComponentStoreFromWorld(rec.worldObj) instanceof Store<?> rawStore) || !(rec.refObj instanceof Ref<?> rawRef)) {
               return;
            }

            @SuppressWarnings("unchecked")
            Store<EntityStore> store = (Store<EntityStore>)rawStore;
            @SuppressWarnings("unchecked")
            Ref<EntityStore> ref = (Ref<EntityStore>)rawRef;
            EntityStatMap stats = store.ensureAndGetComponent(ref, EntityStatMap.getComponentType());
            this.applyNpcScaling(store, ref, ownerId, rec, true);
            stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());
            try {
               EntityStatValue hp = stats.get(DefaultEntityStatTypes.getHealth());
               if (hp != null) {
                  rec.cachedHealth = Math.max(0.0F, hp.get());
                  rec.cachedMaxHealth = Math.max(0.0F, hp.getMax());
               }
            } catch (Throwable ignored) {
            }
            store.putComponent(ref, EntityStatMap.getComponentType(), stats);
            store.ensureComponent(ref, ActiveAnimationComponent.getComponentType());
            MovementStatesComponent movement = store.ensureAndGetComponent(ref, MovementStatesComponent.getComponentType());
            if (movement != null) {
               MovementStates states = movement.getMovementStates();
               if (states == null) {
                  states = new MovementStates();
               }

               states.sleeping = false;
               states.idle = true;
               states.horizontalIdle = true;
               states.onGround = true;
               movement.setMovementStates(states);
               movement.setSentMovementStates(new MovementStates(states));
               store.putComponent(ref, MovementStatesComponent.getComponentType(), movement);
            }

            store.putComponent(ref, RespondToHit.getComponentType(), RespondToHit.INSTANCE);
            NpcHudSyncSupport.updateNpcHud(store, ref, rec);
         } catch (Throwable ignored) {
         }
      });
   }

   private void applyRevivePenalty(AmigoNpcManager.NpcRecord rec, UUID ownerId, boolean manual) {
      if (rec == null || ownerId == null) {
         return;
      }

      try {
         double rate = manual ? 0.10 : 0.40;
         long after = XpProgression.applyCurrentLevelPenalty(rec.totalXp, rate);
         rec.totalXp = after;
         rec.npcLevelCached = XpProgression.levelFromTotalXp(after);
         rec.level = br.tones.amigonpc.core.swords.SwordProgression.clampLevel(rec.npcLevelCached);
         AmigoPersistence.saveTotalXp(ownerId, after);
         AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId);
      } catch (Throwable ignored) {
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
         Object componentType = getCt.invoke(null);
         if (componentType == null) {
            return null;
         }

         Object tc = invokeStoreGetComponent(componentStore, playerEntityRef, componentType);
         return tc == null ? null : invokeNoArg(tc, "getPosition", "position");
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static Object tryGetOwnerPositionFromWorldStore(Object worldObj, Object componentStore, UUID ownerId) {
      try {
         Object ref = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
         if (ref == null) {
            return null;
         }

         Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
         Method getCt = tcClass.getMethod("getComponentType");
         Object componentType = getCt.invoke(null);
         if (componentType == null) {
            return null;
         }

         Object tc = invokeStoreGetComponent(componentStore, ref, componentType);
         return tc == null ? null : invokeNoArg(tc, "getPosition", "position");
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static Object invokeStoreGetComponent(Object store, Object ref, Object componentType) {
      try {
         for (Method m : store.getClass().getMethods()) {
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
         for (Method m : npcPlugin.getClass().getMethods()) {
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
      } catch (Throwable ignored) {
         return -1;
      }
   }

   private static int getNpcRoleIndexWithFallbacks(Object npcPlugin, String roleName) {
      if (roleName != null && !roleName.isBlank()) {
         String rn = roleName;
         String[] candidates = new String[]{
            rn,
            rn.endsWith(".json") ? rn.substring(0, rn.length() - 5) : rn,
            "_Core/" + rn,
            "_Core/AmigoNPC/" + rn,
            "Server/NPC/Roles/" + rn,
            "Server/NPC/Roles/_Core/" + rn,
            "Server/NPC/Roles/_Core/AmigoNPC/" + rn
         };

         for (String c : candidates) {
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
         for (Method m : npcPlugin.getClass().getMethods()) {
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
      }

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
      } catch (Throwable ignored) {
         return buildNoopTriConsumer();
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

   private void updateCurrentTargetMobLevel(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object desiredTargetRefObj) {
      if (store != null && rec != null) {
         if (desiredTargetRefObj instanceof Ref && rec.refObj instanceof Ref) {
            try {
               Ref<EntityStore> tgtRef = (Ref<EntityStore>)desiredTargetRefObj;
               String instanceId = null;

               try {
                  if (invokeNoArg(rec.worldObj, "getName") instanceof String s && !s.isBlank()) {
                     instanceId = s;
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

               this.debugMobLevel(
                  rec,
                  rec.ownerId,
                  AmigoText.format(
                     "core.debug.moblevel.zone_result",
                     rec.zoneRawId,
                     rec.zoneInferredId,
                     rec.currentTargetMobLevel,
                     r.baseLevel(),
                     r.offset(),
                     r.finalLevel(),
                     r.maxLevelCap(),
                     r.cacheHit()
                  )
               );
            } catch (Throwable ignored) {
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

            for (Method m : stateSupport.getClass().getMethods()) {
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

            for (Method m : stateSupport.getClass().getMethods()) {
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

   private static void setFlockState(Store<EntityStore> store, Object npcRefObj, String state, String subState) {
   }

   private static boolean isAirborneTarget(Store<EntityStore> store, Ref<EntityStore> targetRef, Vector3d npcPos) {
      try {
         if (store != null && targetRef != null && npcPos != null) {
            TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
            if (tgtT != null && tgtT.getPosition() != null) {
               Vector3d tp = tgtT.getPosition();
               double dy = tp.y() - npcPos.y();
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

               return dyAbs >= 2.0;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } catch (Throwable ignored) {
         return false;
      }
   }

   private void aimNpcAtTarget(Store<EntityStore> store, Ref<EntityStore> npcRef, Ref<EntityStore> targetRef) {
      if (store != null && npcRef != null && targetRef != null) {
         TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
         if (npcT != null && tgtT != null) {
            Vector3d np = npcT.getPosition();
            Vector3d tp = tgtT.getPosition();
            if (np != null && tp != null) {
               double npcH = Math.max(1.2, getEntityHeight(store, npcRef));
               double tgtH = Math.max(0.6, getEntityHeight(store, targetRef));
               double eyeY = np.y() + npcH * 0.85;
               double aimY = tp.y() + tgtH * 0.55;
               double dx = tp.x() - np.x();
               double dz = tp.z() - np.z();
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
                        invokeOneArg(rot, "setYaw", float.class, yaw);
                        invokeOneArg(rot, "setPitch", float.class, pitch);
                        ok = true;
                     } catch (Throwable var31) {
                     }

                     if (!ok) {
                        try {
                           invokeOneArg(rot, "setX", float.class, pitch);
                           invokeOneArg(rot, "setY", float.class, yaw);
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

   private void tryEquipDefaultBow(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, long now) {
      try {
         if (store == null || rec == null || !rec.autoWeaponSwitchEnabled || !(rec.refObj instanceof Ref<?> rawRef)) {
            return;
         }

         if (rec.backpack == null) {
            rec.backpack = this.getOrLoadBackpack(rec.ownerId);
         }

         String rangedId = NpcWeaponSupport.selectBestBackpackWeapon(rec.backpack, true);
         if (rangedId == null || rangedId.isBlank()) {
            rec.rangedBowItemId = null;
            rec.rangedBowReadyAtMillis = 0L;
            return;
         }

         rec.rangedBowItemId = rangedId;
         @SuppressWarnings("unchecked")
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rawRef;
         if (NpcWeaponSupport.isHotbar0Item(store, npcRef, rangedId)) {
            if (rec.rangedBowReadyAtMillis <= 0L) {
               rec.rangedBowReadyAtMillis = now;
            }
            return;
         }

         if (NpcWeaponSupport.equipWeaponInHotbar0(store, npcRef, rangedId)) {
            rec.rangedBowReadyAtMillis = now + 900L;
         }
      } catch (Throwable ignored) {
      }
   }

   private void tryRangedAttack(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, Object targetRefObj, long now) {
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
         boolean isAssist = rec.assistTargetRefObj != null && refEq(targetRefObj, rec.assistTargetRefObj);
         boolean ownerCombatContextActive = rec.ownerCombatContextUntilMillis > 0L && now <= rec.ownerCombatContextUntilMillis;
         boolean authorized = rec.combatMode == CombatMode.PROTECT_OWNER
            ? ownerCombatContextActive && (isOwnerAggressor || isAssist)
            : isAssist;
         if (!authorized) {
            return;
         }
         boolean isAggressor = isOwnerAggressor;

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

         Ref<EntityStore> targetRef = (Ref<EntityStore>)targetRefObj;
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
         String bowId = rec.rangedBowItemId;
         if (bowId == null || bowId.isBlank() || rec.backpack == null || !NpcWeaponSupport.backpackContainsWeapon(rec.backpack, bowId)) {
            return;
         }

         if (!NpcWeaponSupport.isHotbar0Item(store, npcRef, bowId)) {
            rec.rangedBowReadyAtMillis = Math.max(rec.rangedBowReadyAtMillis, now + 200L);
            return;
         }

         try {
            Player maybePlayer = (Player)store.getComponent(targetRef, Player.getComponentType());
            if (maybePlayer != null) {
               return;
            }
         } catch (Throwable var58) {
         }

         if (this.isAmigoRef(targetRefObj) && !this.pvpEnabled) {
            return;
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

         double dx = tp.x() - np.x();
         double dz = tp.z() - np.z();
         double dy = tp.y() - np.y();
         double dyAbs = Math.abs(dy);
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         if (horizontal > 20.0) {
            return;
         }

         if (dyAbs > 35.0) {
            return;
         }

         try {
            this.aimNpcAtTarget(store, npcRef, targetRef);
         } catch (Throwable var57) {
         }

         double base = NpcWeaponSupport.getWeaponBaseDamage(bowId);
         float amount = (float)(base + rec.level * 0.03);
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
         amount = this.capNpcDamageToOwnerSupport(store, ownerRefObj, amount);
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

         Damage damage = new Damage(new EntitySource(npcRef), DamageCause.PHYSICAL, amount);
         DamageSystems.executeDamage(targetRef, store, damage);

         try {
            this.debugAttack(
               rec,
               rec.ownerId,
               AmigoText.format(
                  "core.debug.attack.ranged_hit",
                  String.format(Locale.ROOT, "%.2f", amount),
                  String.valueOf(bowId),
                  String.format(Locale.ROOT, "%.2f", horizontal),
                  String.format(Locale.ROOT, "%.2f", dy)
               )
            );
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
                  this.debugMobLevel(
                     rec,
                     rec.ownerId,
                     AmigoText.format(
                        "core.debug.moblevel.xp_kill",
                        npcLevel,
                        mobLevel,
                        String.format(Locale.ROOT, "%.2f", baseXp),
                        String.format(Locale.ROOT, "%.2f", diffMult),
                        String.format(Locale.ROOT, "%.2f", RPG_STAGE1_CFG.getRateExp()),
                        xpGain
                     )
                  );
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
               if (invokeNoArg(rec.worldObj, "getName") instanceof String s && !s.isBlank()) {
                  wn = s;
               }
            } catch (Throwable var49) {
            }

            int zid = this.getZoneForHud(rec.ownerId);
            String mobId = rec.currentTargetMobUuid != null ? rec.currentTargetMobUuid.toString() : null;
            NpcXpContext xpCtx = new NpcXpContext(wn, wn, null, zid, mobId, mobLevelForCtx, System.currentTimeMillis());
            this.addNpcXp(store, npcRef, rec.ownerId, rec, xpGain, xpSrc, xpCtx);
         }

         if (killed) {
            if (rec.autoLootEnabled) {
               rec.autoLootStickUntilMillis = Math.max(rec.autoLootStickUntilMillis, now + 900L);
               rec.autoLootStickDeadRefObj = targetRefObj;

               try {
                  if (targetRefObj instanceof Ref<?> rawTargetRef) {
                     @SuppressWarnings("unchecked")
                     Ref<EntityStore> tgtRef3 = (Ref<EntityStore>)rawTargetRef;
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

            this.sendToOwner(rec.worldObj, rec.ownerId, AmigoText.format("core.npc.xp.received", xpGain));
            this.tryRetargetAfterKill(store, rec, ownerRefObj, npcRef, targetRefObj, now);
         }
      } catch (Throwable var59) {
      }
   }

   private void tryMeleeAttack(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, Object targetRefObj, long now) {
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
         boolean isAssist = rec.assistTargetRefObj != null && refEq(targetRefObj, rec.assistTargetRefObj);
         boolean ownerCombatContextActive = rec.ownerCombatContextUntilMillis > 0L && now <= rec.ownerCombatContextUntilMillis;
         boolean authorized = rec.combatMode == CombatMode.PROTECT_OWNER
            ? ownerCombatContextActive && (isOwnerAggressor || isAssist)
            : isAssist;
         if (!authorized) {
            return;
         }
         boolean isAggressor = isOwnerAggressor;

         long cd = 1000L;
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

         Ref<EntityStore> targetRef = (Ref<EntityStore>)targetRefObj;
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;

         try {
            Player maybePlayer = (Player)store.getComponent(targetRef, Player.getComponentType());
            if (maybePlayer != null) {
               this.debugAttack(rec, ownerId, AmigoText.format("core.debug.attack.pvp_blocked", rec.equippedWeaponId));
               return;
            }
         } catch (Throwable var62) {
         }

         if (this.isAmigoRef(targetRefObj) && !this.pvpEnabled) {
            this.debugAttack(rec, ownerId, AmigoText.format("core.debug.attack.pvp_blocked", rec.equippedWeaponId));
            return;
         }

         TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
         TransformComponent tgtT = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
         if (npcT == null || tgtT == null) {
            this.debugAttack(rec, ownerId, AmigoText.format("core.debug.attack.missing_transform", npcT != null, tgtT != null));
            return;
         }

         Vector3d np = npcT.getPosition();
         Vector3d tp = tgtT.getPosition();
         if (np == null || tp == null) {
            this.debugAttack(rec, ownerId, AmigoText.format("core.debug.attack.missing_position", np != null, tp != null));
            return;
         }

         double dx = tp.x() - np.x();
         double dz = tp.z() - np.z();
         double dy = tp.y() - np.y();
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         double targetHeight = getEntityHeight(store, targetRef);
         boolean tallTarget = targetHeight >= 1.9;
         double allowedDy = tallTarget ? 3.0 : 2.0;
         double targetRadius = getEntityRadiusXZ(store, targetRef);
         double effectiveH = Math.max(0.0, horizontal - targetRadius);
         double reach = 2.6;
         if (effectiveH > reach) {
            this.debugAttack(
               rec,
               ownerId,
               AmigoText.format(
                  "core.debug.attack.out_of_range",
                  String.format(Locale.ROOT, "%.2f", horizontal),
                  String.format(Locale.ROOT, "%.2f", effectiveH),
                  String.format(Locale.ROOT, "%.2f", targetRadius),
                  String.format(Locale.ROOT, "%.2f", dy),
                  String.valueOf(rec.equippedWeaponId)
               )
            );
            return;
         }

         if (Math.abs(dy) > allowedDy) {
            this.debugAttack(
               rec,
               ownerId,
               AmigoText.format(
                  "core.debug.attack.high_vertical_diff",
                  String.format(Locale.ROOT, "%.2f", horizontal),
                  String.format(Locale.ROOT, "%.2f", dy),
                  String.format(Locale.ROOT, "%.2f", allowedDy),
                  String.valueOf(rec.equippedWeaponId)
               )
            );
            return;
         }

         String weaponId = resolveConfiguredOrLegacyMeleeWeaponId(rec);
         double base = NpcWeaponSupport.getWeaponBaseDamage(weaponId);
         float amount = (float)Math.min(20.0, base + rec.level * 0.03);
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

         amount = this.capNpcDamageToOwnerSupport(store, ownerRefObj, amount);
         float hpBefore = -1.0F;

         try {
            EntityStatMap tgtStats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (tgtStats != null) {
               hpBefore = tgtStats.get(DefaultEntityStatTypes.getHealth()).get();
            }
         } catch (Throwable var60) {
         }

         this.recordCombatTag(ownerId, targetRefObj, store);
         Damage damage = new Damage(new EntitySource(npcRef), DamageCause.PHYSICAL, amount);

         try {
            Particles particles = new Particles(null, new WorldParticle[]{new WorldParticle("Impact_Sword_Basic", null, 1.0F, null, null)}, 64.0);
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
               if (invokeNoArg(rec.worldObj, "getName") instanceof String s && !s.isBlank()) {
                  wn = s;
               }
            } catch (Throwable var56) {
            }

            int zid = this.getZoneForHud(ownerId);
            String mobId = rec.currentTargetMobUuid != null ? rec.currentTargetMobUuid.toString() : null;
            NpcXpContext xpCtx = new NpcXpContext(wn, wn, null, zid, mobId, mobLevelForCtx, System.currentTimeMillis());
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

            this.sendToOwner(rec.worldObj, ownerId, AmigoText.format("core.npc.xp.received", xpGain));
            this.tryRetargetAfterKill(store, rec, ownerRefObj, npcRef, targetRefObj, now);
         }

         this.debugAttack(
            rec,
            ownerId,
            AmigoText.format(
               "core.debug.attack.hit",
               String.format(Locale.ROOT, "%.2f", amount),
               String.valueOf(rec.equippedWeaponId),
               String.format(Locale.ROOT, "%.2f", horizontal),
               String.format(Locale.ROOT, "%.2f", dy)
            )
         );
         playWeaponAttackAnimation(store, npcRef, rec, weaponId, now);
         rec.lastMeleeAttackMillis = now;
         if (isAggressor) {
            rec.combatUntilMillis = now + 3000L;
         }
      } catch (Throwable var63) {
      }
   }

   private void pulseAggroToNpc(Store<EntityStore> store, Object targetRefObj, Object npcRefObj, AmigoNpcManager.NpcRecord rec, long now) {
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

   private void tryRetargetAfterKill(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object ownerRefObj, Object npcRefObj, Object deadRefObj, long now) {
      try {
         if (store == null || rec == null || rec.combatMode != CombatMode.WEAKEST_ENEMY) {
            return;
         }

         if (!(npcRefObj instanceof Ref<?>) || !(rec.refObj instanceof Ref<?> rawNpcRef)) {
            return;
         }

         @SuppressWarnings("unchecked")
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rawNpcRef;
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

   private float capNpcDamageToOwnerSupport(Store<EntityStore> store, Object ownerRefObj, float requestedAmount) {
      float amount = Math.max(0.0F, requestedAmount);
      if (store == null || ownerRefObj == null) {
         return Math.min(amount, 3.0F);
      }

      try {
         if (!(ownerRefObj instanceof Ref<?> rawOwnerRef)) {
            return Math.min(amount, 3.0F);
         }

         @SuppressWarnings("unchecked")
         Ref<EntityStore> ownerRef = (Ref<EntityStore>)rawOwnerRef;
         Player ownerPlayer = (Player)store.getComponent(ownerRef, Player.getComponentType());
         if (ownerPlayer == null) {
            return Math.min(amount, 3.0F);
         }

         Object inventory = invokeNoArg(ownerPlayer, "getInventory", "inventory");
         Object heldObj = inventory == null ? null : invokeNoArg(inventory, "getItemInHand", "getActiveHotbarItem");
         String ownerWeaponId = null;
         if (heldObj instanceof ItemStack stack && stack != null && !stack.isEmpty()) {
            ownerWeaponId = stack.getItemId();
         } else if (heldObj != null) {
            Object id = invokeNoArg(heldObj, "getItemId", "itemId");
            if (id instanceof String s && !s.isBlank()) {
               ownerWeaponId = s;
            }
         }

         double ownerBase = NpcWeaponSupport.getWeaponBaseDamage(ownerWeaponId);
         float supportCeiling = (float)Math.max(1.0, ownerBase * 0.85);
         return Math.min(amount, supportCeiling);
      } catch (Throwable ignored) {
         return Math.min(amount, 3.0F);
      }
   }

   private static void playWeaponAttackAnimation(Store<EntityStore> store, Ref<EntityStore> npcRef, AmigoNpcManager.NpcRecord rec, String weaponId, long now) {
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
            } catch (Throwable ignored) {
               itemAnimsId = null;
            }
         }

         try {
            if (itemAnimsId != null && !itemAnimsId.isBlank()) {
               ipa = (ItemPlayerAnimations)ItemPlayerAnimations.getAssetMap().getAsset(itemAnimsId);
            }
         } catch (Throwable ignored) {
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
         } catch (Throwable ignored) {
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
         } catch (Throwable ignored) {
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

   private static void clearExpiredAttackAnimation(Store<EntityStore> store, Object npcRefObj, AmigoNpcManager.NpcRecord rec, long now) {
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

         if (!(npcRefObj instanceof Ref<?> rawNpcRef)) {
            return;
         }

         @SuppressWarnings("unchecked")
         Ref<EntityStore> npcRef = (Ref<EntityStore>)rawNpcRef;
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
            } catch (Throwable ignored) {
               anim.setPlayingAnimation(AnimationSlot.Action, null);
            }
         }
      } catch (Throwable var14) {
      }
   }

   private static void tickLevelUpFx(Store<EntityStore> store, Object ownerRefObj, Object npcRefObj, AmigoNpcManager.NpcRecord rec, long now) {
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
               Ref<EntityStore> npcRef = (Ref<EntityStore>)npcRefObj;
               TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
               if (tc == null || tc.getPosition() == null) {
                  return;
               }

               spawnParticleToOwner(store, null, tc.getPosition(), "POTION_MORPH_BURST");
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
            String v = WEAPON_ATTACK_ANIM_CACHE.get(cacheKey);
            return v != null && !v.isBlank() ? v : null;
         }

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
      } else {
         return null;
      }
   }

   private static String pickAttackKeyFromPlayerAnimations(Map<String, ItemAnimation> map) {
      try {
         if (map != null && !map.isEmpty()) {
            String[] prefer = new String[]{"attack", "primary", "primary_attack", "melee", "melee_attack", "swing", "hit", "strike", "slash", "stab", "chop"};

            for (String p : prefer) {
               for (String k : map.keySet()) {
                  if (k != null && k.equalsIgnoreCase(p)) {
                     return k;
                  }
               }
            }

            for (String k : map.keySet()) {
               if (k != null) {
                  String lk = k.toLowerCase(Locale.ROOT);
                  if (lk.contains("attack") || lk.contains("melee") || lk.contains("swing") || lk.contains("hit") || lk.contains("strike")) {
                     return k;
                  }
               }
            }

            for (String k : map.keySet()) {
               if (k != null && !k.isBlank()) {
                  return k;
               }
            }

            return null;
         } else {
            return null;
         }
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static String pickBowKeyFromPlayerAnimations(Map<String, ItemAnimation> map) {
      try {
         if (map != null && !map.isEmpty()) {
            String[] prefer = new String[]{"shoot", "fire", "release", "use", "primary", "primary_use", "charge", "draw", "aim", "bow", "bow_shoot", "ranged"};

            for (String p : prefer) {
               for (String k : map.keySet()) {
                  if (k != null && k.equalsIgnoreCase(p)) {
                     return k;
                  }
               }
            }

            for (String k : map.keySet()) {
               if (k != null) {
                  String lk = k.toLowerCase(Locale.ROOT);
                  if (lk.contains("shoot")
                     || lk.contains("fire")
                     || lk.contains("use")
                     || lk.contains("charge")
                     || lk.contains("draw")
                     || lk.contains("aim")
                     || lk.contains("bow")
                     || lk.contains("ranged")) {
                     return k;
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static Object buildNoopTriConsumer() {
      try {
         Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
         return Proxy.newProxyInstance(tri.getClassLoader(), new Class[]{tri}, (proxy, method, args) -> null);
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static Object buildPreAddToWorldTriConsumer() {
      try {
         Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
         return Proxy.newProxyInstance(tri.getClassLoader(), new Class[]{tri}, (proxy, method, args) -> {
            if (args != null && args.length >= 2 && args[1] instanceof Holder holder) {
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

            return null;
         });
      } catch (Throwable ignored) {
         return buildNoopTriConsumer();
      }
   }

   private static boolean hasSavedWardrobeCosmetics(UUID ownerId) {
      if (ownerId == null) {
         return false;
      }

      try {
         return AmigoWardrobePersistence.hasSavedCosmetics(ownerId);
      } catch (Throwable ignored) {
         return false;
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
         Object assetMap = getAssetMap.invoke(null);
         if (assetMap == null) {
            return null;
         }

         Method getAsset = assetMap.getClass().getMethod("getAsset", Object.class);
         Object asset = getAsset.invoke(assetMap, modelId);
         if (asset == null) {
            return null;
         }

         Class<?> modelClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.Model");
         Method create = modelClass.getMethod("createScaledModel", modelAssetClass, float.class);
         return create.invoke(null, asset, scale);
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static boolean looksLikeRef(Object o) {
      if (o == null) {
         return false;
      }

      String n = o.getClass().getName();
      return n.endsWith(".Ref") || n.endsWith("Ref") || n.contains(".Ref");
   }

   private static Object extractRefFromPair(Object pair) {
      if (pair == null) {
         return null;
      }

      String[] methods = new String[]{"getLeft", "getFirst", "getKey", "left", "first", "key"};

      for (String mname : methods) {
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

   private static boolean doRemoveEntity(Object componentStore, Object savedRefOrPair) {
      Object ref = savedRefOrPair;
      if (!looksLikeRef(ref)) {
         Object extracted = extractRefFromPair(ref);
         if (extracted != null) {
            ref = extracted;
         }
      }

      Object removeReason = getRemoveReasonBestEffort();
      if (removeReason == null) {
         setError(AmigoText.text("core.error.remove_reason.unavailable"));
         return false;
      } else if (!tryInvokeRemoveEntity(componentStore, ref, removeReason)) {
         setError(AmigoText.text("core.error.remove_entity.incompatible"));
         return false;
      } else {
         return true;
      }
   }

   private static Object getRemoveReasonBestEffort() {
      String[] enumCandidates = new String[]{"com.hypixel.hytale.component.RemoveReason", "com.hypixel.hytale.component.Store$RemoveReason"};

      for (String cn : enumCandidates) {
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

         Class<? extends Enum> e = (Class<? extends Enum>)enumClass;

         for (String n : names) {
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
         }

         Object[] vals = enumClass.getEnumConstants();
         return vals != null && vals.length > 0 ? vals[0] : null;
      } catch (Throwable var3) {
         return null;
      }
   }

   private static boolean tryInvokeRemoveEntity(Object store, Object ref, Object reason) {
      try {
         for (Method m : store.getClass().getMethods()) {
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
      }

      Object p = invokeNoArg(senderObj, "getPosition", "position");
      if (p != null) {
         return p;
      }

      Object transform = invokeNoArg(senderObj, "getTransform", "transform");
      if (transform != null) {
         Object p2 = invokeNoArg(transform, "getPosition", "position");
         if (p2 != null) {
            return p2;
         }
      }

      return null;
   }

   private static Float tryGetSenderYaw(Object senderObj) {
      if (senderObj == null) {
         return null;
      }

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

   private static Float tryGetOwnerYawFromWorldStore(Object worldObj, Object componentStore, UUID ownerId) {
      if (worldObj != null && componentStore != null && ownerId != null) {
         try {
            Object ref = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
            if (ref == null) {
               return null;
            }

            Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
            Method getCt = tcClass.getMethod("getComponentType");
            Object componentType = getCt.invoke(null);
            if (componentType == null) {
               return null;
            }

            Object tc = invokeStoreGetComponent(componentStore, ref, componentType);
            if (tc == null) {
               return null;
            }

            Object rot = invokeNoArg(tc, "getRotation", "rotation");
            return extractYawFromRotation(rot);
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
      }

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

   private static Object offsetInFrontOfYaw(Object posVec3d, Float yawDegOrNull, double dist) {
      Object pos = coerceToVector3d(posVec3d);
      if (pos == null) {
         return posVec3d;
      }

      if (yawDegOrNull == null) {
         return offsetVector3d(pos, dist, 0.0, 0.0);
      }

      double yaw = Math.toRadians(yawDegOrNull.floatValue());
      double ox = -Math.sin(yaw) * dist;
      double oz = -Math.cos(yaw) * dist;
      return offsetVector3d(pos, ox, 0.0, oz);
   }

   private static boolean hasCeilingAboveBestEffort(Store<EntityStore> store, Vector3d ownerPos) {
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

         int x = (int)Math.floor(ownerPos.x());
         int z = (int)Math.floor(ownerPos.z());
         int y = (int)Math.floor(ownerPos.y());
         int startY = y + 2;

         for (int i = 0; i < 12; i++) {
            int blockId = world.getBlock(x, startY + i, z);
            if (blockId != 0) {
               return true;
            }
         }
      } catch (Throwable var10) {
      }

      return false;
   }

   private static void spawnParticleToOwner(Store<EntityStore> store, Object ownerRefObj, Vector3d pos, String particleId) {
      try {
         if (store == null || pos == null || particleId == null || particleId.isBlank()) {
            return;
         }

         if (ownerRefObj instanceof Ref) {
            List<Ref<EntityStore>> viewers = Collections.singletonList((Ref<EntityStore>)ownerRefObj);
            ParticleUtil.spawnParticleEffect(particleId, pos, viewers, store);
            return;
         }

         ParticleUtil.spawnParticleEffect(particleId, pos, store);
      } catch (Throwable var5) {
      }
   }

   private static void playNpcSpawnFx(Store<EntityStore> store, Ref<EntityStore> npcRef, Object ownerRefObj) {
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

   private static void playSfx3d(Store<EntityStore> store, Vector3d pos, String soundId, float volumeDb, float pitchSt) {
      try {
         if (store == null || pos == null || soundId == null || soundId.isBlank()) {
            return;
         }

         int idx = soundIndex(soundId);
         if (idx == 0) {
            return;
         }

         SoundUtil.playSoundEvent3d(idx, SoundCategory.SFX, pos.x(), pos.y(), pos.z(), dbToGain(volumeDb), stToPitch(pitchSt), store);
      } catch (Throwable var6) {
      }
   }

   private static int soundIndex(String id) {
      try {
         return SoundEvent.getAssetMap().getIndexOrDefault(id, 0);
      } catch (Throwable ignored) {
         return 0;
      }
   }

   private static float dbToGain(float db) {
      return (float)Math.pow(10.0, db / 20.0);
   }

   private static float stToPitch(float st) {
      return (float)Math.pow(2.0, st / 12.0);
   }

   private static Object coerceToVector3d(Object maybe) {
      if (maybe == null) {
         return null;
      }

      if (hasXYZ(maybe)) {
         return maybe;
      }

      Object pos = invokeNoArg(maybe, "getPosition", "position");
      return pos != null && hasXYZ(pos) ? pos : null;
   }

   private static boolean hasXYZ(Object v) {
      if (v == null) {
         return false;
      } else if (hasMethod(v, "getX") && hasMethod(v, "getY") && hasMethod(v, "getZ")) {
         return true;
      } else {
         return hasMethod(v, "x") && hasMethod(v, "y") && hasMethod(v, "z") ? true : hasField(v, "x") && hasField(v, "y") && hasField(v, "z");
      }
   }

   private static boolean hasMethod(Object v, String name) {
      try {
         v.getClass().getMethod(name);
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean hasField(Object v, String name) {
      try {
         v.getClass().getField(name);
         return true;
      } catch (Throwable ignoredPublic) {
         try {
            v.getClass().getDeclaredField(name);
            return true;
         } catch (Throwable ignoredDeclared) {
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
            return vec.getClass().getConstructor(double.class, double.class, double.class).newInstance(x + ox, y + oy, z + oz);
         } catch (Throwable ignoredCtor) {
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
            } catch (Throwable ignoredPublic) {
               Field f = vec.getClass().getDeclaredField(axis);
               f.setAccessible(true);
               Object r = f.get(vec);
               return ((Number)r).doubleValue();
            }
         }
      }
   }

   private static Object newVector3d(double x, double y, double z) {
      String[] candidates = new String[]{
         "org.joml.Vector3d",
         "com.hypixel.hytale.math.Vector3d",
         "com.hypixel.hytale.util.math.Vector3d",
         "com.hypixel.hytale.protocol.util.Vector3d",
         "com.hypixel.hytale.server.core.math.Vector3d"
      };

      for (String cn : candidates) {
         try {
            Class<?> c = Class.forName(cn);
            return c.getConstructor(double.class, double.class, double.class).newInstance(x, y, z);
         } catch (Throwable var12) {
         }
      }

      return null;
   }

   private static Object newVector3f(float a, float b, float c0) {
      String[] candidates = new String[]{
         "com.hypixel.hytale.math.vector.Vector3f",
         "com.hypixel.hytale.math.Vector3f",
         "com.hypixel.hytale.util.math.Vector3f",
         "com.hypixel.hytale.protocol.util.Vector3f",
         "com.hypixel.hytale.server.core.math.Vector3f"
      };

      for (String cn : candidates) {
         try {
            Class<?> c = Class.forName(cn);
            return c.getConstructor(float.class, float.class, float.class).newInstance(a, b, c0);
         } catch (Throwable var9) {
         }
      }

      return null;
   }

   private static Object invokeNoArg(Object target, String... methodNames) {
      if (target == null) {
         return null;
      }

      for (String name : methodNames) {
         try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Object invokeOneArg(Object target, String methodName, Class<?> argType, Object arg) {
      if (target == null) {
         return null;
      }

      try {
         Method m = target.getClass().getMethod(methodName, argType);
         return m.invoke(target, arg);
      } catch (Throwable var5) {
         return null;
      }
   }

   private static Object invokeStaticNoArg(String className, String methodName) {
      try {
         Class<?> c = Class.forName(className);
         Method m = c.getMethod(methodName);
         return m.invoke(null);
      } catch (Throwable var4) {
         return null;
      }
   }

   private static Object getStaticFieldIfExists(String className, String fieldName) {
      try {
         Class<?> c = Class.forName(className);
         Field f = c.getField(fieldName);
         return f.get(null);
      } catch (Throwable var4) {
         return null;
      }
   }

   private static String firstStringFromArray(Object arr) {
      return arr instanceof String[] a && a.length > 0 ? a[0] : null;
   }

   private static boolean invokeTwoArgs(Object target, String methodName, Class<?> argType1, Class<?> argType2, Object arg1, Object arg2) {
      if (target == null) {
         return false;
      }

      try {
         try {
            Method m = target.getClass().getMethod(methodName, argType1, argType2);
            m.invoke(target, arg1, arg2);
            return true;
         } catch (NoSuchMethodException ignored) {
            for (Method m : target.getClass().getMethods()) {
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

   static final class NpcRecord {
      final Object worldObj;
      final UUID ownerId;
      volatile Object refObj;
      volatile AmigoNpcManager.State state;
      volatile boolean downed;
      volatile long downedUntilMillis;
      volatile long nextDownedMessageMillis;
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
      final ArrayList<CombatTag> combatTags = new ArrayList<>();
      volatile long lastCombatEndMillis;
      volatile long lootStickUntilMillis;
      volatile long lastCombatTagMillis;
      volatile Vector3d lastBattleCenter;
      volatile boolean wasInCombat;
      volatile boolean lootingActive;
      volatile Object lootTargetRefObj;
      volatile long lootTargetSinceMillis;
      final ArrayList<Object> pendingLootRefObjs = new ArrayList<>();
      final Map<Object, Long> lootProcessedUntil = new ConcurrentHashMap<>();
      final Map<String, Integer> lootChatAcc = new LinkedHashMap<>();
      volatile long lootChatSendAtMillis;
      volatile int level = 1;
      volatile String equippedWeaponId;
      volatile double xpInLevel = 0.0;
      volatile long totalXp = 0L;
      volatile int npcLevelCached = 1;
      volatile double stage1XpRemainder = 0.0;
      volatile long baseHp = -1L;
      volatile long baseDef = -1L;
      volatile String modelId;
      volatile double modelScale;
      volatile String customName;
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
      volatile long lastInterruptMillis;
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
      volatile long ownerCombatContextUntilMillis;
      volatile long targetLostSinceMillis;
      volatile long targetStuckSinceMillis;
      volatile double lastTargetHorizontal = -1.0;
      volatile long lastTargetSampleMillis;
      volatile boolean chaseDisengaged;
      volatile CombatMode combatMode = CombatMode.PROTECT_OWNER;
      volatile boolean defendeEnabled = true;
      volatile boolean autoWeaponSwitchEnabled = true;
      volatile boolean interruptAttacksEnabled = true;
      volatile boolean autoLootEnabled = true;
      volatile float cachedHealth;
      volatile float cachedMaxHealth;
      volatile CompanionActivityState activityState = CompanionActivityState.FOLLOWING;
      volatile long nextGatherScanMillis;
      volatile int gatherTargetX = Integer.MIN_VALUE;
      volatile int gatherTargetY = Integer.MIN_VALUE;
      volatile int gatherTargetZ = Integer.MIN_VALUE;
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

      NpcRecord(Object worldObj, UUID ownerId, Object refObj, AmigoNpcManager.State state) {
         this.worldObj = worldObj;
         this.ownerId = ownerId;
         this.refObj = refObj;
         this.state = state;
         this.debugLogEnabled = false;
         this.zoneHudColor = new Color((byte)-1, (byte)-1, (byte)-1);
      }
   }

   enum State {
      SPAWNING,
      ACTIVE,
      DESPAWNING;
   }
}
