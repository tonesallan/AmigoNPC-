package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfigService;
import br.tones.amigonpc.core.rewards.RewardsState;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminRewardsSupport {
   private AmigoLvlGuiSettingsAdminRewardsSupport() {
   }

   static AmigoLvlGuiSettingsAdminRewardsActionResult reloadRewards(boolean settingsAdminPrefillDone) {
      AmigoLevelRewardsConfigService.getShared().reload();
      return new AmigoLvlGuiSettingsAdminRewardsActionResult(AmigoText.text("ui.settings.feedback.admin_rewards.reloaded"), settingsAdminPrefillDone);
   }

   static AmigoLvlGuiSettingsAdminRewardsActionResult restoreRewardDefaults(boolean settingsAdminPrefillDone) {
      AmigoLevelRewardsConfigService.getShared().restoreDefaults(true);
      return new AmigoLvlGuiSettingsAdminRewardsActionResult(AmigoText.text("ui.settings.feedback.admin_rewards.defaults"), settingsAdminPrefillDone);
   }

   static AmigoLvlGuiSettingsAdminRewardsActionResult setResetPoints(UUID ownerId, Integer amount) {
      int safeAmount = amount != null ? amount : 0;
      if (safeAmount < 0) {
         safeAmount = 0;
      }

      RewardsState st = RewardsStateService.getShared().load(ownerId);
      if (st != null) {
         st.resetPoints = safeAmount;
         RewardsStateService.getShared().save(ownerId, st);
      }

      return new AmigoLvlGuiSettingsAdminRewardsActionResult(AmigoText.format("ui.settings.feedback.admin_rewards.reset_points", safeAmount), false);
   }
}
