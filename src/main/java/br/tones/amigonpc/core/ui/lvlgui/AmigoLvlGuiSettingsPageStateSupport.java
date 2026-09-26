package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiSettingsPageStateSupport {
   private AmigoLvlGuiSettingsPageStateSupport() {
   }

   static AmigoLvlGuiSettingsPageState initialState() {
      return new AmigoLvlGuiSettingsPageState(null, null, false, false, false);
   }

   static AmigoLvlGuiSettingsPageState ensureSession(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      AmigoLvlGuiSettingsSnapshot settingsLive,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsModelPrefillDone,
      boolean settingsAdminPrefillDone
   ) {
      AmigoLvlGuiSettingsSessionState sessionState = AmigoLvlGuiSettingsSessionSupport.ensureSession(
         ownerId, viewerPlayerRef, settingsLive, settingsPending, settingsModelPrefillDone, settingsAdminPrefillDone
      );
      return new AmigoLvlGuiSettingsPageState(
         sessionState.settingsLive,
         sessionState.settingsPending,
         sessionState.settingsDirty,
         sessionState.settingsModelPrefillDone,
         sessionState.settingsAdminPrefillDone
      );
   }

   static AmigoLvlGuiSettingsPageState clearSession(UUID ownerId) {
      AmigoLvlGuiSettingsSessionState sessionState = AmigoLvlGuiSettingsSessionSupport.clearSession(ownerId);
      return new AmigoLvlGuiSettingsPageState(
         sessionState.settingsLive,
         sessionState.settingsPending,
         sessionState.settingsDirty,
         sessionState.settingsModelPrefillDone,
         sessionState.settingsAdminPrefillDone
      );
   }

   static AmigoLvlGuiSettingsPageState renderSettings(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      AmigoLvlGuiSettingsSnapshot settingsLive,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsDirty,
      boolean settingsModelPrefillDone,
      boolean settingsAdminPrefillDone,
      UICommandBuilder cmd
   ) {
      if (settingsLive == null || settingsPending == null) {
         settingsLive = AmigoLvlGuiSettingsStateSupport.loadLiveSettings(ownerId);
         settingsPending = AmigoLvlGuiSettingsSnapshot.copyOf(settingsLive);
         settingsDirty = AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending);
      }

      AmigoLvlGuiSettingsRenderState renderState = AmigoLvlGuiSettingsRenderSupport.updateSettingsUI(
         ownerId, viewerPlayerRef, settingsPending, settingsDirty, settingsModelPrefillDone, settingsAdminPrefillDone, cmd
      );
      return new AmigoLvlGuiSettingsPageState(settingsLive, settingsPending, settingsDirty, renderState.modelPrefillDone, renderState.adminPrefillDone);
   }

   static AmigoLvlGuiSettingsPageState withDirty(AmigoLvlGuiSettingsPageState pageState, boolean settingsDirty) {
      return new AmigoLvlGuiSettingsPageState(
         pageState.settingsLive, pageState.settingsPending, settingsDirty, pageState.settingsModelPrefillDone, pageState.settingsAdminPrefillDone
      );
   }

   static AmigoLvlGuiSettingsPageState withAdminPrefillDone(AmigoLvlGuiSettingsPageState pageState, boolean adminPrefillDone) {
      return new AmigoLvlGuiSettingsPageState(
         pageState.settingsLive, pageState.settingsPending, pageState.settingsDirty, pageState.settingsModelPrefillDone, adminPrefillDone
      );
   }

   static AmigoLvlGuiSettingsPageState withModelAndAdminPrefillDone(AmigoLvlGuiSettingsPageState pageState, boolean modelPrefillDone, boolean adminPrefillDone) {
      return new AmigoLvlGuiSettingsPageState(pageState.settingsLive, pageState.settingsPending, pageState.settingsDirty, modelPrefillDone, adminPrefillDone);
   }

   static AmigoLvlGuiSettingsPageState withFooterActionResult(AmigoLvlGuiSettingsPageState pageState, AmigoLvlGuiSettingsFooterActionResult actionResult) {
      return new AmigoLvlGuiSettingsPageState(
         actionResult.settingsLive,
         actionResult.settingsPending,
         actionResult.settingsDirty,
         pageState.settingsModelPrefillDone,
         pageState.settingsAdminPrefillDone
      );
   }
}
