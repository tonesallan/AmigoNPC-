package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsFooterBuildSupport {
   private AmigoLvlGuiSettingsFooterBuildSupport() {
   }

   static void bind(UIEventBuilder events) {
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsApplyButton", "settings_apply");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsCloseButton", "settings_close");
   }
}
