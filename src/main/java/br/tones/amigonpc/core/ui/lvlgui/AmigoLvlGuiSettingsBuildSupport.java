package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsBuildSupport {
   private AmigoLvlGuiSettingsBuildSupport() {
   }

   static void bindSettingsButtons(UIEventBuilder events) {
      AmigoLvlGuiSettingsPlayerBuildSupport.bind(events);
      AmigoLvlGuiSettingsLanguageBuildSupport.bind(events);
      AmigoLvlGuiSettingsNameBuildSupport.bind(events);
      AmigoLvlGuiSettingsAdminBuildSupport.bind(events);
      AmigoLvlGuiSettingsModelBuildSupport.bind(events);
      AmigoLvlGuiSettingsFooterBuildSupport.bind(events);
   }
}
