package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.debug.BuildInfo;
import br.tones.amigonpc.core.debug.ErrorDumpService;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.loot.LootService;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfigService;
import br.tones.amigonpc.core.rewards.RewardsService;
import br.tones.amigonpc.core.rewards.RewardsState;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import br.tones.amigonpc.core.xp.sources.NpcXpAwardService;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfig;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfigService;
import br.tones.amigonpc.ui.UiBridge;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AmigoLvlGuiPage extends InteractiveCustomUIPage {
   private static final String TAB_STATS = "stats";
   private static final String TAB_LEADERBOARD = "leaderboard";
   private static final String TAB_REWARDS = "rewards";
   private static final String ACTION_INCREMENT = "increment";
   private static final String ACTION_DECREMENT = "decrement";
   private static final String ACTION_SAVE = "save";
   private static final String ACTION_CANCEL = "cancel";
   private static final String ACTION_RESET_STATS = "reset_stats";
   private static final String ACTION_CLAIM_ALL = "claim_all";
   private static final String ACTION_CLAIM = "claim";
   private static final String ACTION_SETTINGS_TOGGLE_AUTOLOOT = "settings_toggle_autoloot";
   private static final String ACTION_SETTINGS_TOGGLE_DEFENDER = "settings_toggle_defender";
   private static final String ACTION_SETTINGS_TOGGLE_HUD = "settings_toggle_hud";
   private static final String ACTION_SETTINGS_ADMIN_TOGGLE_GODMODE = "settings_admin_toggle_godmode";
   private static final String ACTION_SETTINGS_ADMIN_TOGGLE_PVP = "settings_admin_toggle_pvp";
   private static final String ACTION_SETTINGS_APPLY = "settings_apply";
   private static final String ACTION_SETTINGS_CLOSE = "settings_close";
   private static final String ACTION_SETTINGS_SPAWN = "settings_spawn";
   private static final String ACTION_SETTINGS_DESPAWN = "settings_despawn";
   private static final String ACTION_SETTINGS_LOOT = "settings_loot";
   private static final String ACTION_SETTINGS_MODEL_APPLY = "settings_model_apply";
   private static final String ACTION_SETTINGS_MODEL_OFF = "settings_model_off";
   private static final String ACTION_SETTINGS_ADMIN_DEBUG_INFO = "settings_admin_debug_info";
   private static final String ACTION_SETTINGS_ADMIN_DEBUG_TOGGLE = "settings_admin_debug_toggle";
   private static final String ACTION_SETTINGS_ADMIN_ADD_XP = "settings_admin_add_xp";
   private static final String ACTION_SETTINGS_ADMIN_STATS_ADD = "settings_admin_stats_add";
   private static final String ACTION_SETTINGS_ADMIN_STATS_RESET = "settings_admin_stats_reset";
   private static final String ACTION_SETTINGS_ADMIN_REWARDS_RELOAD = "settings_admin_rewards_reload";
   private static final String ACTION_SETTINGS_ADMIN_REWARDS_DEFAULTS = "settings_admin_rewards_defaults";
   private static final String ACTION_SETTINGS_ADMIN_REWARDS_SETRESET = "settings_admin_rewards_setreset";
   private static final String ACTION_SETTINGS_ADMIN_SWORD_LVL_UP = "settings_admin_swordlvl_up";
   private static final String ACTION_SETTINGS_ADMIN_SWORD_LVL_DOWN = "settings_admin_swordlvl_down";
   private static final String[] STAT_NAMES = new String[]{"Health", "Defense", "Damage", "CriticalDamage", "Oxygen", "Mining", "Woodcutting"};
   private final UUID ownerId;
   private final AmigoLvlGuiService ownerService;
   private final PlayerRef viewerPlayerRef;
   private final Map pendingChanges = new HashMap();
   private SettingsSnapshot settingsLive;
   private SettingsSnapshot settingsPending;
   private boolean settingsDirty;
   private boolean settingsModelPrefillDone;
   private boolean settingsAdminPrefillDone;
   private String currentTab = "stats";

   public AmigoLvlGuiPage(PlayerRef playerRef, AmigoLvlGuiService ownerService) {
      super(playerRef, CustomPageLifetime.CanDismiss, AmigoLvlGuiEventData.CODEC);
      this.ownerId = playerRef.getUuid();
      this.ownerService = ownerService;
      this.viewerPlayerRef = playerRef;
   }

   public void build(Ref viewerRef, UICommandBuilder cmd, UIEventBuilder events, Store store) {
      cmd.append("Pages/AmigoNPC_Leveling_Main.ui");
      cmd.append("#StatsPageContainer", "Pages/AmigoNPC_Leveling_StatsPage.ui");
      cmd.append("#RpgLvlLeaderboardPageContainer", "Pages/AmigoNPC_Leveling_LeaderboardPage.ui");
      cmd.append("#RewardsPageContainer", "Pages/AmigoNPC_Leveling_RewardsPage.ui");
      cmd.append("#StatsPageContainer #PlayerHeaderContainer", "Pages/AmigoNPC_Leveling_PlayerHeader.ui");
      cmd.append("#RewardsPageContainer #PlayerHeaderContainer", "Pages/AmigoNPC_Leveling_PlayerHeader.ui");
      this.bindStatButtons(events);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#NavStatsButton", EventData.of("NavBar", "stats").append("StatName", "").append("Action", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#NavLeaderboardButton", EventData.of("NavBar", "leaderboard").append("StatName", "").append("Action", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#NavRewardsButton", EventData.of("NavBar", "rewards").append("StatName", "").append("Action", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#NavRewardsButtonSelected", EventData.of("NavBar", "rewards").append("StatName", "").append("Action", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#ClaimAllButton", EventData.of("Action", "claim_all").append("StatName", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Action", "cancel").append("StatName", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Action", "save").append("StatName", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetStatsButton", EventData.of("Action", "reset_stats").append("StatName", "").append("Amount", "0"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAutoLootToggle", EventData.of("Action", "settings_toggle_autoloot"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsDefenderToggle", EventData.of("Action", "settings_toggle_defender"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsHudToggle", EventData.of("Action", "settings_toggle_hud"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminGodModeToggle", EventData.of("Action", "settings_admin_toggle_godmode"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminPvpToggle", EventData.of("Action", "settings_admin_toggle_pvp"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminDebugInfoButton", EventData.of("Action", "settings_admin_debug_info"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminDebugLogButton", EventData.of("Action", "settings_admin_debug_toggle"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminAddXpButton", EventData.of("Action", "settings_admin_add_xp").append("Amount", "#SettingsAdminAddXpAmountInput.Value").append("StatName", "#SettingsAdminAddXpSourceInput.Value"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminStatsAddButton", EventData.of("Action", "settings_admin_stats_add").append("Amount", "#SettingsAdminStatAmountInput.Value").append("StatName", "#SettingsAdminStatAttrInput.Value"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminStatsResetButton", EventData.of("Action", "settings_admin_stats_reset"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminRewardsReloadButton", EventData.of("Action", "settings_admin_rewards_reload"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminRewardsDefaultsButton", EventData.of("Action", "settings_admin_rewards_defaults"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminRewardsSetResetButton", EventData.of("Action", "settings_admin_rewards_setreset").append("Amount", "#SettingsAdminRewardsResetPointsInput.Value"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminSwordLvlUpButton", EventData.of("Action", "settings_admin_swordlvl_up"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsAdminSwordLvlDownButton", EventData.of("Action", "settings_admin_swordlvl_down"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsSpawnButton", EventData.of("Action", "settings_spawn"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsDespawnButton", EventData.of("Action", "settings_despawn"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsLootButton", EventData.of("Action", "settings_loot"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsApplyModelButton", EventData.of("Action", "settings_model_apply").append("@ModelId", "#SettingsModelIdInput.Value").append("@Scale", "#SettingsModelScaleInput.Value"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsModelOffButton", EventData.of("Action", "settings_model_off"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsApplyButton", EventData.of("Action", "settings_apply"), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsCloseButton", EventData.of("Action", "settings_close"), false);
      this.applyNavBarTab(cmd, "stats");
      this.updateUIWithPending(cmd);
   }

   public void handleDataEvent(Ref viewerRef, Store store, AmigoLvlGuiEventData data) {
      try {
         super.handleDataEvent(viewerRef, store, data);
         if (data != null && data.navBar != null && !data.navBar.isEmpty()) {
            try {
               ActionTraceService.getShared().record(this.ownerId, "ui_tab", String.valueOf(data.navBar));
            } catch (Throwable var25) {
            }

            if ("rewards".equals(data.navBar)) {
               UICommandBuilder cmd = new UICommandBuilder();
               this.applyNavBarTab(cmd, "rewards");
               this.updateHeader(cmd);
               this.sendUpdate(cmd, false);
               UICommandBuilder cmd2 = new UICommandBuilder();
               this.updateRewardsList(cmd2);
               this.sendUpdate(cmd2, false);
               return;
            }

            UICommandBuilder cmd = new UICommandBuilder();
            this.applyNavBarTab(cmd, data.navBar);
            if ("stats".equals(data.navBar)) {
               this.updateUIWithPending(cmd);
            } else if ("leaderboard".equals(data.navBar)) {
               this.ensureSettingsSession(store);
               this.updateSettingsUI(cmd, store);
            } else {
               this.updateHeader(cmd);
            }

            this.sendUpdate(cmd, false);
            return;
         }

         String action = data != null && data.action != null ? data.action : "";
         if (action != null && action.startsWith("settings_")) {
            this.handleSettingsAction(viewerRef, store, data, action);
            return;
         }

         if ("reset_stats".equals(action)) {
            try {
               ActionTraceService.getShared().record(this.ownerId, "ui_action", "reset_stats");
            } catch (Throwable var24) {
            }

            UICommandBuilder cmd = new UICommandBuilder();
            boolean isAdmin = this.isAdminBestEffort();
            int resetPoints = RewardsStateService.getShared().getResetPoints(this.ownerId);
            if (!isAdmin) {
               if (resetPoints <= 0) {
                  setError(cmd, "No Reset Points available.");
                  this.updateUIWithPending(cmd);
                  this.sendUpdate(cmd, false);
                  return;
               }

               boolean consumed = RewardsStateService.getShared().consumeResetPoint(this.ownerId);
               if (!consumed) {
                  setError(cmd, "Failed to consume Reset Point.");
                  this.updateUIWithPending(cmd);
                  this.sendUpdate(cmd, false);
                  return;
               }
            }

            this.pendingChanges.clear();
            NpcStatsService.getShared().resetAll(this.ownerId);

            try {
               AmigoNpcManager.getShared().requestRescale(this.ownerId);
            } catch (Throwable var23) {
            }

            clearError(cmd);
            this.updateUIWithPending(cmd);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("cancel".equals(action)) {
            try {
               ActionTraceService.getShared().record(this.ownerId, "ui_action", "cancel");
            } catch (Throwable var22) {
            }

            this.pendingChanges.clear();
            UICommandBuilder cmd = new UICommandBuilder();
            clearError(cmd);
            this.updateUIWithPending(cmd);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("save".equals(action)) {
            try {
               ActionTraceService.getShared().record(this.ownerId, "ui_action", "save");
            } catch (Throwable var21) {
            }

            UICommandBuilder cmd = new UICommandBuilder();
            int npcLevel = AmigoNpcManager.getShared().getNpcLevel(this.ownerId);
            NpcStatsService svc = NpcStatsService.getShared();

            for(Map.Entry e : (new HashMap(this.pendingChanges)).entrySet()) {
               String stat = (String)e.getKey();
               int amt = e.getValue() == null ? 0 : (Integer)e.getValue();
               if (amt > 0) {
                  NpcAttribute attr = toAttr(stat);
                  if (attr != null) {
                     NpcStatsService.AddResult res = svc.add(this.ownerId, npcLevel, attr, amt);
                     if (!res.ok()) {
                        setError(cmd, "Failed to apply " + stat + ": " + res.error());
                     }
                  }
               }
            }

            this.pendingChanges.clear();

            try {
               AmigoNpcManager.getShared().requestRescale(this.ownerId);
            } catch (Throwable var20) {
            }

            this.updateUIWithPending(cmd);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("claim_all".equals(action)) {
            try {
               ActionTraceService.getShared().record(this.ownerId, "ui_action", "claim_all");
            } catch (Throwable var19) {
            }

            UICommandBuilder cmd = new UICommandBuilder();
            String ownerName = this.bestEffortViewerName();
            int npcLevel = AmigoNpcManager.getShared().getNpcLevel(this.ownerId);
            RewardsService.getShared().claimAll(this.ownerId, ownerName, npcLevel);
            this.updateUIWithPending(cmd);
            this.updateRewardsList(cmd);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("claim".equals(action)) {
            try {
               int lvl = data != null && data.claimLevel != null ? data.claimLevel : 0;
               ActionTraceService.getShared().record(this.ownerId, "ui_action", "claim level=" + lvl);
            } catch (Throwable var29) {
            }

            int rewardLevel = data != null && data.claimLevel != null ? data.claimLevel : 0;
            if (rewardLevel > 0) {
               UICommandBuilder cmd = new UICommandBuilder();
               String ownerName = this.bestEffortViewerName();
               int npcLevel = AmigoNpcManager.getShared().getNpcLevel(this.ownerId);
               RewardsService.getShared().claim(this.ownerId, ownerName, npcLevel, rewardLevel);
               this.updateUIWithPending(cmd);
               this.updateRewardsList(cmd);
               this.sendUpdate(cmd, false);
            }

            return;
         }

         if ("increment".equals(action) || "decrement".equals(action)) {
            try {
               String statT = data != null ? data.statName : "";
               int amtT = data != null && data.amount != null ? data.amount : 0;
               ActionTraceService.getShared().record(this.ownerId, "ui_action", action + " stat=" + statT + " amount=" + amtT);
            } catch (Throwable var30) {
            }

            String stat = data != null ? data.statName : "";
            int amount = data != null && data.amount != null ? data.amount : 0;
            if (stat == null || stat.isEmpty() || amount <= 0) {
               return;
            }

            if (!isEditableStat(stat)) {
               return;
            }

            UICommandBuilder cmd = new UICommandBuilder();
            boolean changed = this.applyPendingValidated(stat, action, amount);
            if (changed) {
               clearError(cmd);
            }

            this.updateUIWithPending(cmd);
            this.sendUpdate(cmd, false);
         }
      } catch (Throwable var31) {
         Throwable t = var31;

         try {
            Map<String, Object> extra = new HashMap();

            try {
               extra.put("navBar", data != null ? data.navBar : null);
            } catch (Throwable var18) {
            }

            try {
               extra.put("action", data != null ? data.action : null);
            } catch (Throwable var17) {
            }

            try {
               extra.put("statName", data != null ? data.statName : null);
            } catch (Throwable var16) {
            }

            try {
               extra.put("amount", data != null && data.amount != null ? data.amount : null);
            } catch (Throwable var26) {
            }

            try {
               extra.put("claimLevel", data != null && data.claimLevel != null ? data.claimLevel : null);
            } catch (Throwable var27) {
            }

            ErrorDumpService.getShared().dumpOnServerError(this.ownerId, "ui:handleDataEvent", t, extra);
         } catch (Throwable var28) {
         }

         try {
            ActionTraceService var10000 = ActionTraceService.getShared();
            UUID var10001 = this.ownerId;
            String var10003 = String.valueOf(t.getClass().getSimpleName());
            var10000.record(var10001, "ui_error", var10003 + ": " + String.valueOf(t.getMessage()));
         } catch (Throwable var15) {
         }

         try {
            UICommandBuilder cmd = new UICommandBuilder();
            setError(cmd, "Internal UI error (see amigonpc/errors).");
            this.sendUpdate(cmd, false);
         } catch (Throwable var14) {
         }
      }

   }

   public void onDismiss(Ref viewerRef, Store store) {
      super.onDismiss(viewerRef, store);

      try {
         AmigoLvlGuiPage.AdminSettingsLock.release(this.ownerId);
      } catch (Throwable var5) {
      }

      try {
         this.settingsLive = null;
         this.settingsPending = null;
         this.settingsDirty = false;
         this.settingsModelPrefillDone = false;
         this.settingsAdminPrefillDone = false;
      } catch (Throwable var4) {
      }

      if (this.ownerService != null) {
         this.ownerService.markClosed(this.ownerId);
      }

   }

   public void requestHeaderRefresh() {
      UICommandBuilder cmd = new UICommandBuilder();
      this.updateHeader(cmd);
      if ("rewards".equals(this.currentTab)) {
         this.updateRewardsList(cmd);
      }

      this.sendUpdate(cmd, false);
   }

   public void requestClose() {
      try {
         this.close();
      } catch (Throwable var2) {
      }

   }

   private void bindStatButtons(UIEventBuilder events) {
      for(String stat : STAT_NAMES) {
         events.addEventBinding(CustomUIEventBindingType.Activating, "#" + stat + "Decrement5", EventData.of("StatName", stat).append("Action", "decrement").append("Amount", "5"), false);
         events.addEventBinding(CustomUIEventBindingType.Activating, "#" + stat + "Decrement1", EventData.of("StatName", stat).append("Action", "decrement").append("Amount", "1"), false);
         events.addEventBinding(CustomUIEventBindingType.Activating, "#" + stat + "Increment1", EventData.of("StatName", stat).append("Action", "increment").append("Amount", "1"), false);
         events.addEventBinding(CustomUIEventBindingType.Activating, "#" + stat + "Increment5", EventData.of("StatName", stat).append("Action", "increment").append("Amount", "5"), false);
      }

   }

   private boolean applyPendingValidated(String stat, String action, int amount) {
      int cur = (Integer)this.pendingChanges.getOrDefault(stat, 0);
      if ("decrement".equals(action)) {
         int next = Math.max(0, cur - amount);
         if (next <= 0) {
            this.pendingChanges.remove(stat);
         } else {
            this.pendingChanges.put(stat, next);
         }

         return next != cur;
      } else if (!"increment".equals(action)) {
         return false;
      } else {
         NpcAttribute attr = toAttr(stat);
         if (attr == null) {
            return false;
         } else {
            int npcLevel = AmigoNpcManager.getShared().getNpcLevel(this.ownerId);
            NpcStatsService svc = NpcStatsService.getShared();
            NpcStatsState st = svc.load(this.ownerId);
            int baseAvailable = svc.availablePoints(npcLevel, st);
            int pendingTotal = this.totalPending();
            int available = Math.max(0, baseAvailable - pendingTotal);
            if (available <= 0) {
               return false;
            } else {
               int cap = svc.maxPointsFor(attr);
               int already = st.getAllocated(attr) + cur;
               int room = cap > 0 ? Math.max(0, cap - already) : Integer.MAX_VALUE;
               if (room <= 0) {
                  return false;
               } else {
                  int allow = Math.min(amount, Math.min(available, room));
                  if (allow <= 0) {
                     return false;
                  } else {
                     this.pendingChanges.put(stat, cur + allow);
                     return true;
                  }
               }
            }
         }
      }
   }

   private void applyNavBarTab(UICommandBuilder cmd, String tab) {
      this.currentTab = tab;
      boolean isStats = "stats".equals(tab);
      boolean isLeaderboard = "leaderboard".equals(tab);
      boolean isRewards = "rewards".equals(tab);
      cmd.set("#StatsPageContainer #StatsPage.Visible", isStats);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage.Visible", isLeaderboard);
      cmd.set("#RewardsPageContainer #RewardsPage.Visible", isRewards);
      cmd.set("#NavStatsButtonSelected.Visible", isStats);
      cmd.set("#NavStatsButton.Visible", !isStats);
      cmd.set("#NavLeaderboardButtonSelected.Visible", isLeaderboard);
      cmd.set("#NavLeaderboardButton.Visible", !isLeaderboard);
      cmd.set("#NavRewardsButtonSelected.Visible", isRewards);
      cmd.set("#NavRewardsButton.Visible", !isRewards);
   }

   private void updateUIWithPending(UICommandBuilder cmd) {
      NpcLevelDataView view = NpcLevelDataView.fromOwner(this.ownerId);
      int effectiveAvailable = Math.max(0, view.availablePoints - this.totalPending());
      cmd.set("#StatsPageContainer #StatsPage #AvailablePointsLabel.Text", "Available Stat Points: " + effectiveAvailable);
      this.updateHeader(cmd);
      int resetPoints = RewardsStateService.getShared().getResetPoints(this.ownerId);
      cmd.set("#StatsPageContainer #StatsPage #ResetPointsCount.Text", "x" + resetPoints);
      boolean canReset = resetPoints > 0 || this.isAdminBestEffort();
      cmd.set("#StatsPageContainer #StatsPage #ResetStatsButton.Disabled", !canReset);

      for(String stat : STAT_NAMES) {
         boolean editable = isEditableStat(stat);
         int allocated = (Integer)view.allocatedPoints.getOrDefault(stat, 0);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsNum.Text", String.valueOf(allocated));
         String valueText = (String)view.statValueText.getOrDefault(stat, "0");
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValueNum.Text", valueText);
         int pending = (Integer)this.pendingChanges.getOrDefault(stat, 0);
         boolean hasPending = pending > 0;
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsPending.Visible", hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValuePending.Visible", hasPending);
         if (hasPending) {
            String p = "+" + pending;
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsPending.Text", p);
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValuePending.Text", p);
         } else {
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsPending.Text", "");
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValuePending.Text", "");
         }

         boolean canInc = editable && effectiveAvailable > 0;
         boolean canInc5 = editable && effectiveAvailable >= 5;
         if (editable) {
            NpcAttribute attr = toAttr(stat);
            NpcStatsService svc = NpcStatsService.getShared();
            int cap = attr != null ? svc.maxPointsFor(attr) : 0;
            int already = allocated + pending;
            int room = cap > 0 ? Math.max(0, cap - already) : Integer.MAX_VALUE;
            if (room <= 0) {
               canInc = false;
               canInc5 = false;
            } else if (room < 5) {
               canInc5 = false;
            }
         } else {
            canInc = false;
            canInc5 = false;
         }

         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Increment1.Disabled", !canInc);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Increment5.Disabled", !canInc5);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement1.Disabled", !hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement5.Disabled", !hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement1.Visible", hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement5.Visible", hasPending);
      }

      boolean hasAnyPending = !this.pendingChanges.isEmpty();
      cmd.set("#StatsPageContainer #StatsPage #SaveButton.Visible", hasAnyPending);
      cmd.set("#StatsPageContainer #StatsPage #CancelButton.Visible", hasAnyPending);
   }

   private void updateHeader(UICommandBuilder cmd) {
      NpcLevelDataView view = NpcLevelDataView.fromOwner(this.ownerId);
      String levelText = " - Level " + view.level;
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderLevelLabel.Text", levelText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderLevelLabel.Text", levelText);
      float prog = view.progress01();
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderProgressBar.Value", prog);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderProgressBar.Value", prog);
      int pct = (int)Math.floor((double)(prog * 100.0F));
      String xpText;
      if (view.xpNeeded <= 0L) {
         xpText = view.xp + " / " + view.xpNeeded + " XP (0%)";
      } else {
         xpText = view.xp + " / " + view.xpNeeded + " XP (" + pct + "%)";
      }

      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderXPProgressLabel.Text", xpText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderXPProgressLabel.Text", xpText);
   }

   private void updateRewardsUI(UICommandBuilder cmd) {
      this.updateHeader(cmd);
      this.updateRewardsList(cmd);
   }

   private void ensureSettingsSession(Store store) {
      if (this.settingsLive == null) {
         this.settingsLive = this.loadLiveSettings();
         this.settingsModelPrefillDone = false;
         this.settingsAdminPrefillDone = false;
      }

      if (this.settingsPending == null) {
         this.settingsPending = AmigoLvlGuiPage.SettingsSnapshot.copyOf(this.settingsLive);
      }

      if (this.isAdminBestEffort()) {
         try {
            AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
         } catch (Throwable var3) {
         }
      }

      this.recomputeSettingsDirty();
   }

   private SettingsSnapshot loadLiveSettings() {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      SettingsSnapshot s = new SettingsSnapshot();

      try {
         s.autoLoot = mgr.isAutoLootEnabled(this.ownerId);
      } catch (Throwable var8) {
      }

      try {
         s.defender = mgr.isDefendeEnabled(this.ownerId);
      } catch (Throwable var7) {
      }

      try {
         s.hud = LevelProgressHudService.getShared().isEnabled();
      } catch (Throwable var6) {
      }

      try {
         s.godMode = mgr.isGodMode(this.ownerId);
      } catch (Throwable var5) {
      }

      try {
         s.pvp = mgr.isPvpEnabled();
      } catch (Throwable var4) {
      }

      return s;
   }

   private void recomputeSettingsDirty() {
      if (this.settingsLive != null && this.settingsPending != null) {
         this.settingsDirty = !this.settingsPending.sameToggles(this.settingsLive);
      } else {
         this.settingsDirty = false;
      }
   }

   private void handleSettingsAction(Ref viewerRef, Store store, AmigoLvlGuiEventData data, String action) {
      this.ensureSettingsSession(store);
      UICommandBuilder cmd = new UICommandBuilder();
      clearSettingsFeedback(cmd);
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      boolean isAdmin = this.isAdminBestEffort();

      try {
         if ("settings_toggle_autoloot".equals(action)) {
            this.settingsPending.autoLoot = !this.settingsPending.autoLoot;
            this.recomputeSettingsDirty();
            setSettingsFeedback(cmd, "AutoLoot: " + (this.settingsPending.autoLoot ? "ON" : "OFF") + " (pendente)");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_toggle_defender".equals(action)) {
            this.settingsPending.defender = !this.settingsPending.defender;
            this.recomputeSettingsDirty();
            setSettingsFeedback(cmd, "Defender: " + (this.settingsPending.defender ? "ON" : "OFF") + " (pendente)");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_toggle_hud".equals(action)) {
            this.settingsPending.hud = !this.settingsPending.hud;
            this.recomputeSettingsDirty();
            setSettingsFeedback(cmd, "HUD: " + (this.settingsPending.hud ? "ON" : "OFF") + " (pendente)");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_toggle_godmode".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            if (mgr.hasNpc(this.ownerId)) {
               this.settingsPending.godMode = !this.settingsPending.godMode;
               this.recomputeSettingsDirty();
               setSettingsFeedback(cmd, "GodMode: " + (this.settingsPending.godMode ? "ON" : "OFF") + " (pendente)");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            setSettingsFeedback(cmd, "Nenhum NPC ativo para GodMode.");
            this.settingsPending.godMode = this.settingsLive != null && this.settingsLive.godMode;
            this.recomputeSettingsDirty();
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_toggle_pvp".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            this.settingsPending.pvp = !this.settingsPending.pvp;
            this.recomputeSettingsDirty();
            setSettingsFeedback(cmd, "PvP: " + (this.settingsPending.pvp ? "ON" : "OFF") + " (pendente)");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_debug_info".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            String v = null;
            String bid = null;
            String hb = null;
            String ub = null;

            try {
               v = BuildInfo.getModVersion();
            } catch (Throwable var36) {
            }

            try {
               bid = BuildInfo.getBuildId();
            } catch (Throwable var35) {
            }

            try {
               hb = HytaleBridge.getLastError();
            } catch (Throwable var34) {
            }

            try {
               ub = UiBridge.getLastError();
            } catch (Throwable var33) {
            }

            if (v == null) {
               v = "?";
            }

            if (bid == null) {
               bid = "?";
            }

            if (hb == null || hb.isBlank()) {
               hb = "OK";
            }

            if (ub == null || ub.isBlank()) {
               ub = "OK";
            }

            String msg = "V=" + v + " | Build=" + bid + " | HB=" + hb + " | UI=" + ub;
            if (msg.length() > 140) {
               msg = msg.substring(0, 140) + "...";
            }

            setSettingsFeedback(cmd, msg);
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_debug_toggle".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            boolean enabled = false;

            try {
               enabled = mgr.toggleDebugLog(this.ownerId);
            } catch (Throwable var32) {
            }

            setSettingsFeedback(cmd, "DebugLog: " + (enabled ? "ON" : "OFF"));
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_add_xp".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            int amt = 0;

            try {
               amt = data != null && data.amount != null ? data.amount : 0;
            } catch (Throwable var39) {
            }

            if (amt <= 0) {
               setSettingsFeedback(cmd, "Uso: amount > 0");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
            if (cfg != null && cfg.enableCommandXP) {
               String srcTxt = null;

               try {
                  srcTxt = data != null ? data.statName : null;
               } catch (Throwable var31) {
               }

               NpcXpSource src = NpcXpSource.COMMAND;
               if (srcTxt != null && !srcTxt.isBlank()) {
                  try {
                     src = NpcXpSource.valueOf(srcTxt.trim().toUpperCase(Locale.ROOT));
                  } catch (Throwable var30) {
                     src = NpcXpSource.COMMAND;
                  }
               }

               String wn = "";

               try {
                  Object w = ((EntityStore)store.getExternalData()).getWorld();
                  if (w != null) {
                     try {
                        Method mm = w.getClass().getMethod("getName");
                        Object r = mm.invoke(w);
                        if (r != null) {
                           wn = String.valueOf(r);
                        }
                     } catch (Throwable var28) {
                     }
                  }
               } catch (Throwable var29) {
               }

               NpcXpContext ctxObj = new NpcXpContext(wn, wn, (String)null, 0, (String)null, 0, System.currentTimeMillis());
               boolean ok = NpcXpAwardService.getShared().awardFixed(this.ownerId, (long)amt, src, ctxObj);
               setSettingsFeedback(cmd, ok ? "+" + amt + " XP (" + src.name() + ")" : "Não foi possível adicionar XP.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            setSettingsFeedback(cmd, "Command XP desativado (EnableCommandXP=false)");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_stats_add".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            String attrTxt = null;

            try {
               attrTxt = data != null ? data.statName : null;
            } catch (Throwable var27) {
            }

            if (attrTxt != null) {
               attrTxt = attrTxt.trim();
            }

            int amt = 1;

            try {
               amt = data != null && data.amount != null ? data.amount : 1;
            } catch (Throwable var38) {
            }

            if (amt <= 0) {
               amt = 1;
            }

            NpcAttribute a = NpcAttribute.parse(attrTxt);
            if (a == null) {
               setSettingsFeedback(cmd, "Atributo inválido.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            int lvl = 1;

            try {
               lvl = mgr.getNpcLevel(this.ownerId);
            } catch (Throwable var26) {
            }

            NpcStatsService.AddResult res = NpcStatsService.getShared().addForce(this.ownerId, lvl, a, amt);
            if (!res.ok()) {
               setSettingsFeedback(cmd, "Falha: " + res.error());
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            try {
               mgr.requestRescale(this.ownerId);
            } catch (Throwable var25) {
            }

            int var10001 = res.applied();
            setSettingsFeedback(cmd, "+" + var10001 + " em " + a.name());
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_stats_reset".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            NpcStatsService.getShared().resetAll(this.ownerId);

            try {
               mgr.requestRescale(this.ownerId);
            } catch (Throwable var24) {
            }

            setSettingsFeedback(cmd, "Stats resetados.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_rewards_reload".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            AmigoLevelRewardsConfigService.getShared().reload();
            setSettingsFeedback(cmd, "Rewards recarregadas.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_rewards_defaults".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            AmigoLevelRewardsConfigService.getShared().restoreDefaults(true);
            setSettingsFeedback(cmd, "Defaults restaurados.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_rewards_setreset".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            int amount = 0;

            try {
               amount = data != null && data.amount != null ? data.amount : 0;
            } catch (Throwable var37) {
            }

            if (amount < 0) {
               amount = 0;
            }

            RewardsState st = RewardsStateService.getShared().load(this.ownerId);
            if (st != null) {
               st.resetPoints = amount;
               RewardsStateService.getShared().save(this.ownerId, st);
            }

            this.settingsAdminPrefillDone = false;
            setSettingsFeedback(cmd, "resetPoints = " + amount);
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_swordlvl_up".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            int newLvl = mgr.changeSwordLevel(this.ownerId, 1, true);
            setSettingsFeedback(cmd, "Nível de espada: " + newLvl);
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_admin_swordlvl_down".equals(action)) {
            if (!isAdmin) {
               setSettingsFeedback(cmd, "Apenas admin.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               AmigoLvlGuiPage.AdminSettingsLock.tryAcquire(this.ownerId, this.bestEffortViewerName());
            }

            if (!AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               setSettingsFeedback(cmd, "Seção admin bloqueada.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            AmigoLvlGuiPage.AdminSettingsLock.touch(this.ownerId);
            int newLvl = mgr.changeSwordLevel(this.ownerId, -1, true);
            setSettingsFeedback(cmd, "Nível de espada: " + newLvl);
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_apply".equals(action)) {
            if (this.settingsLive == null) {
               this.settingsLive = this.loadLiveSettings();
            }

            boolean changedAny = false;
            if (this.settingsPending.autoLoot != this.settingsLive.autoLoot) {
               mgr.setAutoLootEnabled(this.ownerId, this.settingsPending.autoLoot);
               changedAny = true;
            }

            if (this.settingsPending.defender != this.settingsLive.defender) {
               mgr.setDefendeEnabled(this.ownerId, this.settingsPending.defender);
               changedAny = true;
            }

            if (this.settingsPending.hud != this.settingsLive.hud) {
               boolean curHud = LevelProgressHudService.getShared().isEnabled();
               if (this.settingsPending.hud != curHud) {
                  LevelProgressHudService.getShared().toggleEnabledPersisted();
               }

               changedAny = true;
            }

            if (isAdmin && AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId)) {
               if (this.settingsPending.godMode != this.settingsLive.godMode && mgr.hasNpc(this.ownerId)) {
                  mgr.setGodModeWithStore(store, this.ownerId, this.settingsPending.godMode);
                  changedAny = true;
               }

               if (this.settingsPending.pvp != this.settingsLive.pvp) {
                  mgr.setPvpEnabled(this.settingsPending.pvp);
                  changedAny = true;
               }
            }

            this.settingsLive = this.loadLiveSettings();
            this.settingsPending = AmigoLvlGuiPage.SettingsSnapshot.copyOf(this.settingsLive);
            this.recomputeSettingsDirty();
            if (isAdmin) {
               AmigoLvlGuiPage.AdminSettingsLock.release(this.ownerId);
            }

            setSettingsFeedback(cmd, changedAny ? "Aplicado." : "Sem mudanças.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_close".equals(action)) {
            this.settingsPending = AmigoLvlGuiPage.SettingsSnapshot.copyOf(this.settingsLive);
            this.recomputeSettingsDirty();
            if (isAdmin) {
               AmigoLvlGuiPage.AdminSettingsLock.release(this.ownerId);
            }

            try {
               this.close();
            } catch (Throwable var23) {
            }

            return;
         }

         if ("settings_spawn".equals(action)) {
            if (mgr.hasNpc(this.ownerId)) {
               setSettingsFeedback(cmd, "NPC já está ativo.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            Object worldObj = null;

            try {
               worldObj = ((EntityStore)store.getExternalData()).getWorld();
            } catch (Throwable var22) {
            }

            boolean ok = mgr.spawnWithStore(worldObj, store, viewerRef, this.ownerId, this.viewerPlayerRef);
            setSettingsFeedback(cmd, ok ? "NPC spawnado." : "Falha ao spawnar.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_despawn".equals(action)) {
            if (!mgr.hasNpc(this.ownerId)) {
               setSettingsFeedback(cmd, "Nenhum NPC ativo.");
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            boolean ok = mgr.despawnWithStore(store, this.ownerId);
            setSettingsFeedback(cmd, ok ? "NPC despawnado." : "Falha ao despawnar.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_loot".equals(action)) {
            boolean ok = LootService.getShared().openLoot(this.ownerId, this.viewerPlayerRef, viewerRef, store);
            setSettingsFeedback(cmd, ok ? "Loot aberto." : "Não foi possível abrir o loot.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_model_apply".equals(action)) {
            String modelId = data != null ? data.modelId : null;
            if (modelId != null) {
               modelId = modelId.trim();
            }

            double scale = (double)1.0F;
            boolean scaleAdjusted = false;

            try {
               if (data != null && data.scale != null && !Double.isNaN(data.scale) && Double.isFinite(data.scale) && data.scale > (double)0.0F) {
                  scale = data.scale;
               }
            } catch (Throwable var21) {
            }

            if (!(scale >= 0.1) || !(scale <= (double)5.0F)) {
               scale = (double)1.0F;
               scaleAdjusted = true;
            }

            if (modelId != null && !modelId.isBlank()) {
               AmigoPersistence.saveModel(this.ownerId, modelId, scale);
               if (mgr.hasNpc(this.ownerId)) {
                  boolean ok1 = mgr.despawnWithStore(store, this.ownerId);
                  Object worldObj = null;

                  try {
                     worldObj = ((EntityStore)store.getExternalData()).getWorld();
                  } catch (Throwable var20) {
                  }

                  boolean ok2 = mgr.spawnWithStore(worldObj, store, viewerRef, this.ownerId, this.viewerPlayerRef);
                  if (ok1 && ok2) {
                     setSettingsFeedback(cmd, scaleAdjusted ? "Modelo aplicado (scale ajustado)." : "Modelo aplicado.");
                  } else {
                     setSettingsFeedback(cmd, scaleAdjusted ? "Modelo salvo (scale ajustado; re-spawn falhou)." : "Modelo salvo (re-spawn falhou).");
                  }
               } else {
                  setSettingsFeedback(cmd, scaleAdjusted ? "Modelo salvo (scale ajustado)." : "Modelo salvo.");
               }

               this.settingsModelPrefillDone = false;
               this.settingsAdminPrefillDone = false;
               this.updateSettingsUI(cmd, store);
               this.sendUpdate(cmd, false);
               return;
            }

            setSettingsFeedback(cmd, "ModelId vazio.");
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         if ("settings_model_off".equals(action)) {
            AmigoPersistence.saveModel(this.ownerId, (String)null, (double)1.0F);
            if (mgr.hasNpc(this.ownerId)) {
               boolean ok1 = mgr.despawnWithStore(store, this.ownerId);
               Object worldObj = null;

               try {
                  worldObj = ((EntityStore)store.getExternalData()).getWorld();
               } catch (Throwable var19) {
               }

               boolean ok2 = mgr.spawnWithStore(worldObj, store, viewerRef, this.ownerId, this.viewerPlayerRef);
               setSettingsFeedback(cmd, ok1 && ok2 ? "Modelo removido." : "Modelo removido (re-spawn falhou).");
            } else {
               setSettingsFeedback(cmd, "Modelo removido.");
            }

            this.settingsModelPrefillDone = false;
            this.settingsAdminPrefillDone = false;
            this.updateSettingsUI(cmd, store);
            this.sendUpdate(cmd, false);
            return;
         }

         this.updateSettingsUI(cmd, store);
         this.sendUpdate(cmd, false);
      } catch (Throwable var40) {
         try {
            setSettingsFeedback(cmd, "Erro interno.");
         } catch (Throwable var18) {
         }

         try {
            this.updateSettingsUI(cmd, store);
         } catch (Throwable var17) {
         }

         try {
            this.sendUpdate(cmd, false);
         } catch (Throwable var16) {
         }
      }

   }

   private void updateSettingsUI(UICommandBuilder cmd, Store store) {
      if (this.settingsLive == null || this.settingsPending == null) {
         this.settingsLive = this.loadLiveSettings();
         this.settingsPending = AmigoLvlGuiPage.SettingsSnapshot.copyOf(this.settingsLive);
         this.recomputeSettingsDirty();
      }

      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      boolean hasNpc = false;

      try {
         hasNpc = mgr.hasNpc(this.ownerId);
      } catch (Throwable var18) {
      }

      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNpcStatus.Text", "NPC: " + (hasNpc ? "Ativo" : "Inativo"));
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPendingStatus.Text", this.settingsDirty ? "PENDENTE — clique APPLY" : "Sem alterações pendentes");
      String var10002 = this.settingsPending.autoLoot ? "ON" : "OFF";
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAutoLootToggle.Text", "AutoLoot: " + var10002);
      var10002 = this.settingsPending.defender ? "ON" : "OFF";
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsDefenderToggle.Text", "Defender: " + var10002);
      var10002 = this.settingsPending.hud ? "ON" : "OFF";
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsHudToggle.Text", "HUD: " + var10002);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsSpawnButton.Disabled", hasNpc);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsDespawnButton.Disabled", !hasNpc);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsLootButton.Disabled", !hasNpc);
      String modelId = "";
      double scale = (double)1.0F;

      try {
         String m = AmigoPersistence.loadModelId(this.ownerId);
         if (m != null) {
            modelId = m;
         }
      } catch (Throwable var17) {
      }

      try {
         scale = AmigoPersistence.loadModelScale(this.ownerId);
      } catch (Throwable var16) {
      }

      if (!(scale > (double)0.0F) || Double.isNaN(scale) || Double.isInfinite(scale)) {
         scale = (double)1.0F;
      }

      if (!this.settingsModelPrefillDone) {
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelIdInput.Value", modelId);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelScaleInput.Value", String.valueOf(scale));
         this.settingsModelPrefillDone = true;
      }

      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsApplyButton.Disabled", !this.settingsDirty);
      boolean isAdmin = this.isAdminBestEffort();
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSection.Visible", isAdmin);
      if (isAdmin) {
         boolean lockHeldByMe = AmigoLvlGuiPage.AdminSettingsLock.isHeldBy(this.ownerId);
         String lockName = AmigoLvlGuiPage.AdminSettingsLock.ownerName;
         boolean lockedByOther = AmigoLvlGuiPage.AdminSettingsLock.ownerId != null && !lockHeldByMe;
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminLockLabel.Visible", lockedByOther);
         if (lockedByOther) {
            cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminLockLabel.Text", "Em edição por: " + (lockName != null ? lockName : "-"));
         } else {
            cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminLockLabel.Text", "");
         }

         var10002 = this.settingsPending.godMode ? "ON" : "OFF";
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminGodModeToggle.Text", "GodMode: " + var10002);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminPvpToggle.Text", "PvP do Mod: " + (this.settingsPending.pvp ? "ON" : "OFF"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminGodModeToggle.Disabled", lockedByOther || !hasNpc);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminPvpToggle.Disabled", lockedByOther);
         boolean dbg = false;

         try {
            dbg = mgr.isDebugLogEnabled(this.ownerId);
         } catch (Throwable var15) {
         }

         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugLogButton.Text", "DebugLog: " + (dbg ? "ON" : "OFF"));
         if (!this.settingsAdminPrefillDone) {
            try {
               int rp = RewardsStateService.getShared().getResetPoints(this.ownerId);
               cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsResetPointsInput.Value", String.valueOf(rp));
            } catch (Throwable var14) {
            }

            this.settingsAdminPrefillDone = true;
         }

         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugInfoButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugLogButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminAddXpButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsAddButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsResetButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsReloadButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsDefaultsButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsSetResetButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordLvlUpButton.Disabled", lockedByOther);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordLvlDownButton.Disabled", lockedByOther);
      }

   }

   private static void setSettingsFeedback(UICommandBuilder cmd, String msg) {
      if (cmd != null) {
         if (msg == null) {
            msg = "";
         }

         boolean vis = !msg.isBlank();
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsFeedback.Text", msg);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsFeedback.Visible", vis);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsFeedbackBox.Visible", vis);
      }
   }

   private static void clearSettingsFeedback(UICommandBuilder cmd) {
      setSettingsFeedback(cmd, "");
   }

   private void updateRewardsList(UICommandBuilder cmd) {
      AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
      RewardsState st = RewardsStateService.getShared().load(this.ownerId);
      int npcLevel = AmigoNpcManager.getShared().getNpcLevel(this.ownerId);
      String rowsSel = "#RewardsPage #RewardRows";
      cmd.clear("#RewardsPage #RewardRows");
      if (cfg != null && cfg.Rewards != null && !cfg.Rewards.isEmpty()) {
         List<AmigoLevelRewardsConfig.RewardEntry> rewards = new ArrayList();

         for(AmigoLevelRewardsConfig.RewardEntry r : cfg.Rewards) {
            if (r != null && r.Level > 0) {
               rewards.add(r);
            }
         }

         rewards.sort(Comparator.comparingInt((a) -> a.Level));
         int firstLevel = rewards.isEmpty() ? -1 : ((AmigoLevelRewardsConfig.RewardEntry)rewards.get(0)).Level;
         int lastLevel = rewards.isEmpty() ? -1 : ((AmigoLevelRewardsConfig.RewardEntry)rewards.get(rewards.size() - 1)).Level;
         int prevLevel = 0;
         boolean anyClaimable = false;

         for(AmigoLevelRewardsConfig.RewardEntry reward : rewards) {
            int level = reward.Level;
            boolean achieved = level <= npcLevel;
            boolean alreadyClaimed = st.claimedRewardLevels.contains(level);
            boolean canClaim = achieved && !alreadyClaimed;
            if (canClaim) {
               anyClaimable = true;
            }

            boolean prevAchieved = prevLevel > 0 && prevLevel <= npcLevel || prevLevel == 0 && achieved;
            boolean isFirstRow = level == firstLevel;
            boolean isLastRow = level == lastLevel;
            String claimText = alreadyClaimed ? "Claimed" : (canClaim ? "Unclaim" : "?");
            int resetPoints = reward.ResetPoints;
            String resetPointsText = resetPoints > 0 ? "Reset Points x" + resetPoints : "";
            String commandDisplay = formatCommandDisplay(reward.Command, reward.CommandTitle);
            List<AmigoLevelRewardsConfig.ItemEntry> items = reward.Items != null ? reward.Items : List.of();
            String rowMarkup = buildRewardRowMarkup(level, isFirstRow, isLastRow, achieved, prevAchieved, items, resetPoints, resetPointsText, commandDisplay, claimText, canClaim);
            cmd.appendInline("#RewardsPage #RewardRows", rowMarkup);
            prevLevel = level;
         }

         cmd.set("#RewardsPageContainer #RewardsPage #ClaimAllButton.Visible", anyClaimable);
      } else {
         cmd.set("#RewardsPageContainer #RewardsPage #ClaimAllButton.Visible", false);
      }
   }

   private static String formatCommandDisplay(String command, String commandTitle) {
      if (commandTitle != null && !commandTitle.isEmpty()) {
         return commandTitle;
      } else {
         return command != null && !command.isEmpty() ? command : "";
      }
   }

   private static String escapeUiString(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "");
   }

   private static String escapeUiStringMultiline(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
   }

   private static int badgeWidthForQuantity(int quantity) {
      int digits = quantity > 0 ? (int)Math.ceil(Math.log10((double)quantity + (double)0.5F)) : 1;
      return Math.max(10, 5 + 5 * (1 + digits));
   }

   private static List wrapTextToLines(String text) {
      List<String> out = new ArrayList();
      if (text != null && !text.isEmpty()) {
         String t = text.trim();
         if (t.isEmpty()) {
            return out;
         } else {
            String[] parts = t.split(",\\s*", -1);

            for(String part : parts) {
               if (part != null) {
                  String s = part.trim();
                  if (!s.isEmpty()) {
                     int end;
                     if (s.length() <= 24) {
                        out.add(s);
                     } else {
                        for(int idx = 0; idx < s.length(); idx = end) {
                           end = Math.min(idx + 24, s.length());
                           if (end < s.length()) {
                              int lastSpace = s.lastIndexOf(32, end);
                              if (lastSpace > idx) {
                                 end = lastSpace + 1;
                              }
                           }

                           out.add(s.substring(idx, end).trim());
                        }
                     }
                  }
               }
            }

            return out;
         }
      } else {
         return out;
      }
   }

   private static String buildRewardRowMarkup(int level, boolean isFirstRow, boolean isLastRow, boolean achieved, boolean prevAchieved, List items, int resetPoints, String resetPointsText, String commandDisplay, String claimText, boolean canClaim) {
      String claimTextEsc = escapeUiString(claimText);
      String statusColor = canClaim ? "#90ee90" : "#888888";
      String levelColor = achieved ? "#5a8bd8" : "#e8e8e8";
      int width = 660;
      int lvlW = 76;
      int rewardsW = 240;
      int additionalW = 194;
      int statusW = 120;
      int leftPad = 15;
      int itemHGap = 10;
      int iconsPerRow = 4;
      int iconRowHeight = 52;
      List<AmigoLevelRewardsConfig.ItemEntry> safeItems = new ArrayList();
      if (items != null) {
         for(AmigoLevelRewardsConfig.ItemEntry it : items) {
            if (it != null && it.ItemId != null && !it.ItemId.isEmpty() && it.Quantity > 0) {
               safeItems.add(it);
            }
         }
      }

      int iconsCount = safeItems.size() + (resetPoints > 0 ? 1 : 0);
      int iconRows = iconsCount == 0 ? 0 : (iconsCount + 4 - 1) / 4;
      int iconsHeight = iconRows == 0 ? 52 : iconRows * 52 + 8;
      List<String> lines = wrapTextToLines(commandDisplay != null ? commandDisplay : "");
      int lineCount = Math.max(1, lines.size());
      int additionalHeight = lineCount * 18 + 8;
      String additionalTextEsc = lines.isEmpty() ? "" : escapeUiStringMultiline(String.join("\n", lines));
      int maxHeight = Math.max(70, Math.max(iconsHeight, additionalHeight + 20));
      int topPad = isFirstRow ? 10 : 0;
      int totalHeight = maxHeight + topPad + 5;
      int lvlTop = isFirstRow ? 10 : 20;
      int additionalTop = isFirstRow ? 10 : 20;
      int statusTop = isFirstRow ? 15 : 25;
      StringBuilder sb = new StringBuilder();
      sb.append("Group {");
      sb.append("LayoutMode: Left; Anchor: (Width:").append(660).append(", Height:").append(totalHeight).append(", Bottom: 6); Background: #1a1f26(0.7); Padding: (Left:").append(15).append(", Right:").append(0).append(", Top:").append(topPad).append(", Bottom: 5);");
      sb.append("Label { Style: (FontSize: 15, TextColor: ").append(levelColor).append(", RenderBold: true); Anchor: (Top:").append(lvlTop).append(", Width:").append(76).append("); Text: \"LVL").append(level).append("\"; }");
      sb.append("Group { LayoutMode: Top; Anchor: (Width:").append(240).append(", Height:").append(iconsHeight).append("); Padding: (Right: 8);");
      int iconIndex = 0;

      for(int r = 0; r < iconRows; ++r) {
         sb.append("Group { LayoutMode: Left; Anchor: (Height:").append(52).append(");");

         for(int c = 0; c < 4 && iconIndex < iconsCount; ++c) {
            if (iconIndex < safeItems.size()) {
               AmigoLevelRewardsConfig.ItemEntry it = (AmigoLevelRewardsConfig.ItemEntry)safeItems.get(iconIndex);
               String itemId = escapeUiString(it.ItemId);
               int qty = it.Quantity;
               sb.append("Group { LayoutMode: Left; Padding: (Right:").append(10).append(");");
               sb.append("Group { LayoutMode: Full; Anchor: (Width: 48, Height: 48); Background: #404858; Padding: 1;");
               sb.append("Group { LayoutMode: Full; Anchor: (Full: 0); Background: #252830(0.92);");
               sb.append("ItemIcon { Anchor: (Left: 3, Top: 3, Width: 40, Height: 40); ItemId: \"").append(itemId).append("\"; }");
               int badgeW = badgeWidthForQuantity(qty);
               int badgeLeft = 46 - badgeW - 2;
               sb.append("Group { LayoutMode: Full; Anchor: (Left:").append(badgeLeft).append(", Bottom: 2, Width:").append(badgeW).append(", Height: 12);");
               sb.append("Background: #252830(0.92);");
               sb.append("Label { Style: (FontSize: 9, TextColor: #e8e8e8); Anchor: (Full: 0); Text: \"x").append(qty).append("\"; }");
               sb.append("}");
               sb.append("} } }");
            } else {
               sb.append("Group { LayoutMode: Left; Padding: (Right:").append(10).append(");");
               sb.append("Group { LayoutMode: Full; Anchor: (Width: 48, Height: 48); Background: #404858; Padding: 1;");
               sb.append("Group { LayoutMode: Full; Anchor: (Full: 0); Background: #252830(0.92);");
               sb.append("Group { LayoutMode: Full; Anchor: (Full: 0); Background: \"Pages/AmigoNPC_ResetPointIcon.png\";");
               int badgeW = badgeWidthForQuantity(resetPoints);
               int badgeLeft = 46 - badgeW - 2;
               sb.append("Group { LayoutMode: Full; Anchor: (Left:").append(badgeLeft).append(", Bottom: 2, Width:").append(badgeW).append(", Height: 12);");
               sb.append("Background: #252830(0.92);");
               sb.append("Label { Style: (FontSize: 9, TextColor: #e8e8e8); Anchor: (Full: 0); Text: \"x").append(resetPoints).append("\"; }");
               sb.append("}");
               sb.append("} } } }");
            }

            ++iconIndex;
         }

         sb.append("}");
      }

      sb.append("}");
      sb.append("Label { Style: (FontSize: 14, TextColor: #c0c0c0); Anchor: (Top:").append(additionalTop).append(", Width:").append(194).append(", Height:").append(additionalHeight).append("); Padding: (Left: 0); Text: \"").append(additionalTextEsc).append("\"; }");
      sb.append("Group { FlexWeight: 1; Anchor: (Height:").append(maxHeight).append("); }");
      sb.append("Group { LayoutMode: Left; Anchor: (Width:").append(120).append(", Height:").append(maxHeight).append("); Padding: (Left: 4, Right: 4);");
      sb.append("Label { Style: (FontSize: 14, TextColor: ").append(statusColor).append("); Anchor: (Top:").append(statusTop).append(", Width: 96, Height: 35); Text: \"").append(claimTextEsc).append("\"; }");
      sb.append("}");
      sb.append("}");
      return sb.toString();
   }

   private String bestEffortViewerName() {
      Object sender = this.viewerPlayerRef;
      if (sender != null) {
         for(String m : new String[]{"getName", "getUsername", "getUserName"}) {
            try {
               Method mm = sender.getClass().getMethod(m);
               Object r = mm.invoke(sender);
               if (r != null) {
                  String s = String.valueOf(r);
                  if (!s.isBlank()) {
                     return s;
                  }
               }
            } catch (Throwable var9) {
            }
         }
      }

      return this.ownerId != null ? this.ownerId.toString() : "player";
   }

   private static boolean isEditableStat(String stat) {
      return "Health".equals(stat) || "Damage".equals(stat) || "CriticalDamage".equals(stat) || "Defense".equals(stat);
   }

   private static NpcAttribute toAttr(String stat) {
      if ("Health".equals(stat)) {
         return NpcAttribute.HEALTH;
      } else if ("Damage".equals(stat)) {
         return NpcAttribute.DAMAGE;
      } else if ("CriticalDamage".equals(stat)) {
         return NpcAttribute.CRITICAL_DAMAGE;
      } else {
         return "Defense".equals(stat) ? NpcAttribute.DEFENSE : null;
      }
   }

   private int totalPending() {
      int sum = 0;

      for(Map.Entry e : this.pendingChanges.entrySet()) {
         if (e.getKey() != null && isEditableStat((String)e.getKey())) {
            Integer v = (Integer)e.getValue();
            if (v != null && v > 0) {
               sum += v;
            }
         }
      }

      return sum;
   }

   private static void setError(UICommandBuilder cmd, String msg) {
   }

   private static void clearError(UICommandBuilder cmd) {
      setError(cmd, "");
   }

   private boolean isAdminBestEffort() {
      try {
         UUID uid = null;

         try {
            uid = this.viewerPlayerRef != null ? this.viewerPlayerRef.getUuid() : null;
         } catch (Throwable var7) {
         }

         if (uid == null) {
            uid = this.ownerId;
         }

         if (uid == null) {
            return false;
         } else {
            PermissionsModule pm = null;

            try {
               pm = PermissionsModule.get();
            } catch (Throwable var6) {
            }

            if (pm == null) {
               return false;
            } else {
               Set<String> groups = null;

               try {
                  groups = pm.getGroupsForUser(uid);
               } catch (Throwable var5) {
               }

               return groups == null ? false : groups.contains("OP");
            }
         }
      } catch (Throwable var8) {
         return false;
      }
   }

   private static boolean hasPermissionBestEffort(Object sender, String node) {
      try {
         try {
            Method m = sender.getClass().getMethod("hasPermission", String.class, Boolean.TYPE);
            Object r = m.invoke(sender, node, false);
            if (r instanceof Boolean b) {
               return b;
            }
         } catch (Throwable var6) {
         }

         try {
            Method m = sender.getClass().getMethod("hasPermission", String.class);
            Object r = m.invoke(sender, node);
            if (r instanceof Boolean b) {
               return b;
            }
         } catch (Throwable var5) {
         }
      } catch (Throwable var7) {
      }

      return false;
   }

   private static final class SettingsSnapshot {
      boolean autoLoot;
      boolean defender;
      boolean hud;
      boolean godMode;
      boolean pvp;

      static SettingsSnapshot copyOf(SettingsSnapshot src) {
         SettingsSnapshot s = new SettingsSnapshot();
         if (src != null) {
            s.autoLoot = src.autoLoot;
            s.defender = src.defender;
            s.hud = src.hud;
            s.godMode = src.godMode;
            s.pvp = src.pvp;
         }

         return s;
      }

      boolean sameToggles(SettingsSnapshot other) {
         if (other == null) {
            return false;
         } else {
            return this.autoLoot == other.autoLoot && this.defender == other.defender && this.hud == other.hud && this.godMode == other.godMode && this.pvp == other.pvp;
         }
      }
   }

   private static final class AdminSettingsLock {
      static final long TIMEOUT_MS = 180000L;
      static volatile UUID ownerId;
      static volatile String ownerName;
      static volatile long lastTouchMillis;

      static boolean isHeldBy(UUID id) {
         return id != null && id.equals(ownerId);
      }

      static synchronized boolean tryAcquire(UUID id, String name) {
         long now = System.currentTimeMillis();
         if (ownerId != null && now - lastTouchMillis > 180000L) {
            ownerId = null;
            ownerName = null;
            lastTouchMillis = 0L;
         }

         if (ownerId != null && !ownerId.equals(id)) {
            return false;
         } else {
            ownerId = id;
            ownerName = name != null && !name.isBlank() ? name : (id != null ? id.toString() : "admin");
            lastTouchMillis = now;
            return true;
         }
      }

      static synchronized void touch(UUID id) {
         if (id != null) {
            if (ownerId != null && ownerId.equals(id)) {
               lastTouchMillis = System.currentTimeMillis();
            }

         }
      }

      static synchronized void release(UUID id) {
         if (id != null) {
            if (ownerId != null && ownerId.equals(id)) {
               ownerId = null;
               ownerName = null;
               lastTouchMillis = 0L;
            }

         }
      }
   }
}
