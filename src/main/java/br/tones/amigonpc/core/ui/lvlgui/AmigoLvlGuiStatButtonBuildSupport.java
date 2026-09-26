package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiStatButtonBuildSupport {
   private AmigoLvlGuiStatButtonBuildSupport() {
   }

   static void bindStatButtons(UIEventBuilder events, String[] statNames, String actionIncrement, String actionDecrement) {
      for (String stat : statNames) {
         events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#" + stat + "Decrement5",
            EventData.of("StatName", stat).append("Action", actionDecrement).append("Amount", "5"),
            false
         );
         events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#" + stat + "Decrement1",
            EventData.of("StatName", stat).append("Action", actionDecrement).append("Amount", "1"),
            false
         );
         events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#" + stat + "Increment1",
            EventData.of("StatName", stat).append("Action", actionIncrement).append("Amount", "1"),
            false
         );
         events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#" + stat + "Increment5",
            EventData.of("StatName", stat).append("Action", actionIncrement).append("Amount", "5"),
            false
         );
      }
   }
}
