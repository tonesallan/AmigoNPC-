package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AmigoLvlGuiPage extends InteractiveCustomUIPage<AmigoLvlGuiEventData> {
   private final UUID ownerId;
   private final AmigoLvlGuiService ownerService;
   private final PlayerRef viewerPlayerRef;
   private final Map<String, Integer> pendingChanges = new HashMap<>();
   private AmigoLvlGuiSettingsPageState settingsPageState = AmigoLvlGuiSettingsPageStateSupport.initialState();
   private String currentTab = "stats";

   public AmigoLvlGuiPage(PlayerRef playerRef, AmigoLvlGuiService ownerService) {
      super(playerRef, CustomPageLifetime.CanDismiss, AmigoLvlGuiEventData.CODEC);
      this.ownerId = playerRef.getUuid();
      this.ownerService = ownerService;
      this.viewerPlayerRef = playerRef;
   }

   public void build(Ref<EntityStore> viewerRef, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
      AmigoText.initializePlayerLocale(this.ownerId, this.viewerPlayerRef);
      AmigoText.withPlayerLocale(this.ownerId, () -> {
         AmigoLvlGuiBuildSupport.appendBasePages(cmd);
         AmigoLvlGuiBuildSupport.bindStatButtons(events);
         AmigoLvlGuiBuildSupport.bindTabButtons(events);
         AmigoLvlGuiBuildSupport.bindActionButtons(events);
         AmigoLvlGuiBuildSupport.bindSettingsButtons(events);
         AmigoLvlGuiStaticTextSupport.apply(cmd);
         this.applyNavBarTab(cmd, "stats");
         this.updateUIWithPending(cmd);
      });
   }

   public void handleDataEvent(Ref<EntityStore> viewerRef, Store<EntityStore> store, AmigoLvlGuiEventData data) {
      AmigoText.initializePlayerLocale(this.ownerId, this.viewerPlayerRef);
      AmigoText.pushPlayerLocale(this.ownerId);

      try {
         super.handleDataEvent(viewerRef, store, data);
         if (AmigoLvlGuiTabEventSupport.handleNavBarEvent(
            this.ownerId,
            AmigoLvlGuiEventDataReadSupport.navBarOrNull(data),
            this::applyNavBarTab,
            this::updateHeader,
            this::updateUIWithPending,
            this::ensureSettingsSession,
            this::updateSettingsUI,
            this::updateRewardsList,
            cmd -> this.sendUpdate(cmd, false)
         )) {
            return;
         }

         String action = AmigoLvlGuiEventDataReadSupport.actionOrEmpty(data);
         if (action == null || !action.startsWith("settings_")) {
            AmigoLvlGuiMainActionResult mainActionResult = AmigoLvlGuiMainActionSupport.handleAction(
               this.ownerId, this.viewerPlayerRef, this.pendingChanges, data, action
            );
            if (!AmigoLvlGuiMainActionRenderSupport.applyResult(
               mainActionResult,
               AmigoLvlGuiPage::setError,
               AmigoLvlGuiPage::clearError,
               this::updateUIWithPending,
               this::updateRewardsList,
               cmd -> this.sendUpdate(cmd, false)
            )) {
               return;
            }

            return;
         }

         this.handleSettingsAction(viewerRef, store, data, action);
      } catch (Throwable t) {
         AmigoLvlGuiErrorSupport.handleEventError(
            this.ownerId, data, t, cmd -> setError(cmd, AmigoText.text("ui.error.internal")), cmd -> this.sendUpdate(cmd, false)
         );
         return;
      } finally {
         AmigoText.popLocale();
      }
   }

   public void onDismiss(Ref<EntityStore> viewerRef, Store<EntityStore> store) {
      super.onDismiss(viewerRef, store);
      this.applySettingsPageState(AmigoLvlGuiSettingsPageStateSupport.clearSession(this.ownerId));
      if (this.ownerService != null) {
         this.ownerService.markClosed(this.ownerId);
      }
   }

   public void requestHeaderRefresh() {
      AmigoText.initializePlayerLocale(this.ownerId, this.viewerPlayerRef);
      AmigoText.withPlayerLocale(this.ownerId, () -> {
         UICommandBuilder cmd = new UICommandBuilder();
         this.updateHeader(cmd);
         if ("rewards".equals(this.currentTab)) {
            this.updateRewardsList(cmd);
         }

         this.sendUpdate(cmd, false);
      });
   }

   public void requestClose() {
      try {
         this.close();
      } catch (Throwable var2) {
      }
   }

   private void applyNavBarTab(UICommandBuilder cmd, String tab) {
      this.currentTab = tab;
      AmigoLvlGuiRenderSupport.applyNavBarTab(cmd, tab);
   }

   private void updateUIWithPending(UICommandBuilder cmd) {
      AmigoLvlGuiStatsRenderSupport.updateUIWithPending(this.ownerId, this.viewerPlayerRef, this.pendingChanges, AmigoLvlGuiIds.STAT_NAMES, cmd);
   }

   private void updateHeader(UICommandBuilder cmd) {
      AmigoLvlGuiRenderSupport.updateHeader(this.ownerId, cmd);
   }

   private void ensureSettingsSession() {
      this.applySettingsPageState(
         AmigoLvlGuiSettingsPageStateSupport.ensureSession(
            this.ownerId,
            this.viewerPlayerRef,
            this.settingsPageState.settingsLive,
            this.settingsPageState.settingsPending,
            this.settingsPageState.settingsModelPrefillDone,
            this.settingsPageState.settingsAdminPrefillDone
         )
      );
   }

   private void respondSettingsUpdate(UICommandBuilder cmd) {
      AmigoLvlGuiStaticTextSupport.apply(cmd);
      this.updateHeader(cmd);
      this.updateSettingsUI(cmd);
      this.sendUpdate(cmd, false);
   }

   private void handleSettingsAction(Ref<EntityStore> viewerRef, Store<EntityStore> store, AmigoLvlGuiEventData data, String action) {
      boolean isAdmin = AmigoLvlGuiPermissionSupport.isAdminBestEffort(this.viewerPlayerRef, this.ownerId);
      AmigoLvlGuiSettingsActionRouterSupport.handle(
         this::ensureSettingsSession,
         AmigoLvlGuiPage::clearSettingsFeedback,
         this::respondSettingsUpdate,
         AmigoLvlGuiPage::setSettingsFeedback,
         cmd -> {
            AmigoLvlGuiSettingsPageActionResult actionResult = AmigoLvlGuiSettingsPageActionSupport.handle(
               this.ownerId, this.viewerPlayerRef, viewerRef, store, data, action, isAdmin, this.settingsPageState, cmd
            );
            return AmigoLvlGuiSettingsPageActionRenderSupport.applyResult(
               actionResult, cmd, this::applySettingsPageState, AmigoLvlGuiPage::setSettingsFeedback, this::respondSettingsUpdate, () -> {
                  try {
                     this.close();
                  } catch (Throwable var2x) {
                  }
               }
            );
         }
      );
   }

   private void updateSettingsUI(UICommandBuilder cmd) {
      this.applySettingsPageState(
         AmigoLvlGuiSettingsPageStateSupport.renderSettings(
            this.ownerId,
            this.viewerPlayerRef,
            this.settingsPageState.settingsLive,
            this.settingsPageState.settingsPending,
            this.settingsPageState.settingsDirty,
            this.settingsPageState.settingsModelPrefillDone,
            this.settingsPageState.settingsAdminPrefillDone,
            cmd
         )
      );
   }

   private void applySettingsPageState(AmigoLvlGuiSettingsPageState pageState) {
      if (pageState != null) {
         this.settingsPageState = pageState;
      }
   }

   private static void setSettingsFeedback(UICommandBuilder cmd, String msg) {
      AmigoLvlGuiMessageSupport.setSettingsFeedback(cmd, msg);
   }

   private static void clearSettingsFeedback(UICommandBuilder cmd) {
      AmigoLvlGuiMessageSupport.clearSettingsFeedback(cmd);
   }

   private void updateRewardsList(UICommandBuilder cmd) {
      AmigoLvlGuiRewardsSupport.updateRewardsList(this.ownerId, cmd);
   }

   private static void setError(UICommandBuilder cmd, String msg) {
      AmigoLvlGuiMessageSupport.setError(cmd, msg);
   }

   private static void clearError(UICommandBuilder cmd) {
      AmigoLvlGuiMessageSupport.clearError(cmd);
   }
}
