package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminActionSupport {
   private AmigoLvlGuiSettingsAdminActionSupport() {
   }

   static AmigoLvlGuiSettingsAdminActionResult handle(
      UUID ownerId, String action, AmigoLvlGuiEventData data, Store<EntityStore> store, boolean settingsAdminPrefillDone
   ) {
      if ("settings_admin_debug_info".equals(action)) {
         return new AmigoLvlGuiSettingsAdminActionResult(true, AmigoLvlGuiSettingsAdminDebugSupport.buildDebugInfoMessage(), settingsAdminPrefillDone);
      } else if ("settings_admin_debug_toggle".equals(action)) {
         return new AmigoLvlGuiSettingsAdminActionResult(true, AmigoLvlGuiSettingsAdminDebugSupport.toggleDebugLog(ownerId), settingsAdminPrefillDone);
      } else if ("settings_admin_add_xp".equals(action)) {
         return new AmigoLvlGuiSettingsAdminActionResult(true, AmigoLvlGuiSettingsAdminXpSupport.addXp(ownerId, store, data), settingsAdminPrefillDone);
      } else if ("settings_admin_stats_add".equals(action)) {
         return new AmigoLvlGuiSettingsAdminActionResult(
            true,
            AmigoLvlGuiSettingsAdminStatsSupport.addStats(
               ownerId, AmigoLvlGuiEventDataReadSupport.statNameOrNull(data), AmigoLvlGuiEventDataReadSupport.amountOrDefault(data, 1)
            ),
            settingsAdminPrefillDone
         );
      } else if ("settings_admin_stats_reset".equals(action)) {
         return new AmigoLvlGuiSettingsAdminActionResult(true, AmigoLvlGuiSettingsAdminStatsSupport.resetStats(ownerId), settingsAdminPrefillDone);
      } else if ("settings_admin_rewards_reload".equals(action)) {
         AmigoLvlGuiSettingsAdminRewardsActionResult rewardsResult = AmigoLvlGuiSettingsAdminRewardsSupport.reloadRewards(settingsAdminPrefillDone);
         return new AmigoLvlGuiSettingsAdminActionResult(true, rewardsResult.message, rewardsResult.adminPrefillDone);
      } else if ("settings_admin_rewards_defaults".equals(action)) {
         AmigoLvlGuiSettingsAdminRewardsActionResult rewardsResult = AmigoLvlGuiSettingsAdminRewardsSupport.restoreRewardDefaults(settingsAdminPrefillDone);
         return new AmigoLvlGuiSettingsAdminActionResult(true, rewardsResult.message, rewardsResult.adminPrefillDone);
      } else if ("settings_admin_rewards_setreset".equals(action)) {
         AmigoLvlGuiSettingsAdminRewardsActionResult rewardsResult = AmigoLvlGuiSettingsAdminRewardsSupport.setResetPoints(
            ownerId, AmigoLvlGuiEventDataReadSupport.amountOrDefault(data, 0)
         );
         return new AmigoLvlGuiSettingsAdminActionResult(true, rewardsResult.message, rewardsResult.adminPrefillDone);
      } else if ("settings_admin_swordlvl_up".equals(action)) {
         return new AmigoLvlGuiSettingsAdminActionResult(true, AmigoLvlGuiSettingsAdminSwordSupport.changeSwordLevel(ownerId, 1), settingsAdminPrefillDone);
      } else {
         return "settings_admin_swordlvl_down".equals(action)
            ? new AmigoLvlGuiSettingsAdminActionResult(true, AmigoLvlGuiSettingsAdminSwordSupport.changeSwordLevel(ownerId, -1), settingsAdminPrefillDone)
            : new AmigoLvlGuiSettingsAdminActionResult(false, null, settingsAdminPrefillDone);
      }
   }
}
