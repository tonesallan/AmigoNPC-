package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsAdminBuildSupport {
   private AmigoLvlGuiSettingsAdminBuildSupport() {
   }

   static void bind(UIEventBuilder events) {
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminGodModeToggle", "settings_admin_toggle_godmode");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminPvpToggle", "settings_admin_toggle_pvp");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminDebugInfoButton", "settings_admin_debug_info");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminDebugLogButton", "settings_admin_debug_toggle");
      events.addEventBinding(
         CustomUIEventBindingType.Activating,
         "#SettingsAdminAddXpButton",
         EventData.of("Action", "settings_admin_add_xp")
            .append("Amount", "#SettingsAdminAddXpAmountInput.Value")
            .append("StatName", "#SettingsAdminAddXpSourceInput.Value"),
         false
      );
      events.addEventBinding(
         CustomUIEventBindingType.Activating,
         "#SettingsAdminStatsAddButton",
         EventData.of("Action", "settings_admin_stats_add")
            .append("Amount", "#SettingsAdminStatAmountInput.Value")
            .append("StatName", "#SettingsAdminStatAttrInput.Value"),
         false
      );
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminStatsResetButton", "settings_admin_stats_reset");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminRewardsReloadButton", "settings_admin_rewards_reload");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminRewardsDefaultsButton", "settings_admin_rewards_defaults");
      events.addEventBinding(
         CustomUIEventBindingType.Activating,
         "#SettingsAdminRewardsSetResetButton",
         EventData.of("Action", "settings_admin_rewards_setreset").append("Amount", "#SettingsAdminRewardsResetPointsInput.Value"),
         false
      );
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminSwordLvlUpButton", "settings_admin_swordlvl_up");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAdminSwordLvlDownButton", "settings_admin_swordlvl_down");
   }
}
