package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiSettingsAccessSupport {
   private AmigoLvlGuiSettingsAccessSupport() {
   }

   static AmigoLvlGuiSettingsAccessResult ensureAdminAccess(PlayerRef viewerPlayerRef, UUID ownerId, boolean isAdmin) {
      if (!isAdmin) {
         return new AmigoLvlGuiSettingsAccessResult(false, AmigoText.text("ui.settings.feedback.admin_only"));
      } else {
         return AmigoLvlGuiSettingsStateSupport.ensureAdminLockHeld(viewerPlayerRef, ownerId)
            ? new AmigoLvlGuiSettingsAccessResult(true, null)
            : new AmigoLvlGuiSettingsAccessResult(false, AmigoText.text("ui.settings.feedback.admin_locked"));
      }
   }
}
