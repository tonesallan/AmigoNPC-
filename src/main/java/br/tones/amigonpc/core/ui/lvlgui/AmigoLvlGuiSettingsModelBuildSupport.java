package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiSettingsModelBuildSupport {
   private AmigoLvlGuiSettingsModelBuildSupport() {
   }

   static void bind(UIEventBuilder events) {
      events.addEventBinding(
         CustomUIEventBindingType.Activating,
         "#SettingsApplyModelButton",
         EventData.of("Action", "settings_model_apply").append("@ModelId", "#SettingsModelIdInput.Value").append("@Scale", "#SettingsModelScaleInput.Value"),
         false
      );
      AmigoLvlGuiEventBindingSupport.addSimpleActionBinding(events, "#SettingsModelOffButton", "settings_model_off");
   }
}
