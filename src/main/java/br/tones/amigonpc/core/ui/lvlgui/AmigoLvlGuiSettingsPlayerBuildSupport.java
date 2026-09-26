package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsPlayerBuildSupport {
   private AmigoLvlGuiSettingsPlayerBuildSupport() {
   }

   static void bind(UIEventBuilder events) {
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsAutoLootToggle", "settings_toggle_autoloot");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsDefenderToggle", "settings_toggle_defender");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsHudToggle", "settings_toggle_hud");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsSpawnButton", "settings_spawn");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsDespawnButton", "settings_despawn");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsLootButton", "settings_loot");
   }
}
