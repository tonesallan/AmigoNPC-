package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiSettingsSessionSupport {
   private AmigoLvlGuiSettingsSessionSupport() {
   }

   static AmigoLvlGuiSettingsSessionState ensureSession(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      AmigoLvlGuiSettingsSnapshot settingsLive,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsModelPrefillDone,
      boolean settingsAdminPrefillDone
   ) {
      if (settingsLive == null) {
         settingsLive = AmigoLvlGuiSettingsStateSupport.loadLiveSettings(ownerId);
         settingsModelPrefillDone = false;
         settingsAdminPrefillDone = false;
      }

      if (settingsPending == null) {
         settingsPending = AmigoLvlGuiSettingsSnapshot.copyOf(settingsLive);
      }

      AmigoLvlGuiSettingsStateSupport.tryAcquireAdminLockIfNeeded(viewerPlayerRef, ownerId);
      return new AmigoLvlGuiSettingsSessionState(
         settingsLive,
         settingsPending,
         AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending),
         settingsModelPrefillDone,
         settingsAdminPrefillDone
      );
   }

   static AmigoLvlGuiSettingsSessionState clearSession(UUID ownerId) {
      try {
         AmigoLvlGuiAdminSettingsLock.release(ownerId);
      } catch (Throwable var2) {
      }

      return new AmigoLvlGuiSettingsSessionState(null, null, false, false, false);
   }
}
