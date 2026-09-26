package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsNameBuildSupport {
   private AmigoLvlGuiSettingsNameBuildSupport() {
   }

   static void bind(UIEventBuilder events) {
      events.addEventBinding(
         CustomUIEventBindingType.Activating,
         "#SettingsApplyNameButton",
         EventData.of("Action", "settings_name_apply").append("@NpcName", "#SettingsNpcNameInput.Value"),
         false
      );
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsNameOffButton", "settings_name_off");
   }
}
