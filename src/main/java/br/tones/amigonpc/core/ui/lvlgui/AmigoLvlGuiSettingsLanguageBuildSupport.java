package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsLanguageBuildSupport {
   private AmigoLvlGuiSettingsLanguageBuildSupport() {
   }

   static void bind(UIEventBuilder events) {
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsLanguagePrevButton", "settings_language_prev");
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsLanguageNextButton", "settings_language_next");
   }
}
