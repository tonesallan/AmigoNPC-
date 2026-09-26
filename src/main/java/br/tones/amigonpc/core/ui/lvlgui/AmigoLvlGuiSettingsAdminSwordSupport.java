package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminSwordSupport {
   private AmigoLvlGuiSettingsAdminSwordSupport() {
   }

   static String changeSwordLevel(UUID ownerId, int delta) {
      int newLvl = AmigoNpcManager.getShared().changeSwordLevel(ownerId, delta, true);
      return AmigoText.format("ui.settings.feedback.admin_sword.level", newLvl);
   }
}
