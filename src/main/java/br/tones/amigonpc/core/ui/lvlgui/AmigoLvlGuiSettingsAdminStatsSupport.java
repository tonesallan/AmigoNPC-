package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminStatsSupport {
   private AmigoLvlGuiSettingsAdminStatsSupport() {
   }

   static String addStats(UUID ownerId) {
      return addStats(ownerId, null, null);
   }

   static String addStats(UUID ownerId, String attrTxt, Integer amount) {
      if (attrTxt != null) {
         attrTxt = attrTxt.trim();
      }

      int amt = amount != null ? amount : 1;
      if (amt <= 0) {
         amt = 1;
      }

      NpcAttribute attr = NpcAttribute.parse(attrTxt);
      if (attr == null) {
         return AmigoText.text("ui.settings.feedback.admin_stats.invalid_attribute");
      }

      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      int lvl = 1;

      try {
         lvl = mgr.getNpcLevel(ownerId);
      } catch (Throwable var10) {
      }

      NpcStatsService.AddResult res = NpcStatsService.getShared().addForce(ownerId, lvl, attr, amt);
      if (!res.ok()) {
         return AmigoText.format("ui.settings.feedback.admin_stats.failed", res.error());
      }

      try {
         mgr.requestRescale(ownerId);
      } catch (Throwable var9) {
      }

      return AmigoText.format("ui.settings.feedback.admin_stats.add", res.applied(), attr.name());
   }

   static String resetStats(UUID ownerId) {
      NpcStatsService.getShared().resetAll(ownerId);

      try {
         AmigoNpcManager.getShared().requestRescale(ownerId);
      } catch (Throwable var2) {
      }

      return AmigoText.text("ui.settings.feedback.admin_stats.reset");
   }
}
