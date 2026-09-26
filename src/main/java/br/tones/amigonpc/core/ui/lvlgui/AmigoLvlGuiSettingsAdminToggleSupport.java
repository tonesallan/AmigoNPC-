package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminToggleSupport {
   private AmigoLvlGuiSettingsAdminToggleSupport() {
   }

   static AmigoLvlGuiSettingsToggleResult toggleGodMode(UUID ownerId, AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      if (mgr.hasNpc(ownerId)) {
         settingsPending.godMode = !settingsPending.godMode;
         return new AmigoLvlGuiSettingsToggleResult(
            AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending),
            AmigoText.format("ui.settings.feedback.godmode_pending", AmigoText.onOff(settingsPending.godMode))
         );
      } else {
         settingsPending.godMode = settingsLive != null && settingsLive.godMode;
         return new AmigoLvlGuiSettingsToggleResult(
            AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending), AmigoText.text("ui.settings.feedback.godmode_no_npc")
         );
      }
   }

   static AmigoLvlGuiSettingsToggleResult togglePvp(AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending) {
      settingsPending.pvp = !settingsPending.pvp;
      return new AmigoLvlGuiSettingsToggleResult(
         AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending),
         AmigoText.format("ui.settings.feedback.pvp_pending", AmigoText.onOff(settingsPending.pvp))
      );
   }
}
