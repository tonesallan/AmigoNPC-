package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiEventBindingSupport {
   private AmigoLvlGuiEventBindingSupport() {
   }

   static void addNavBinding(UIEventBuilder events, String selector, String tab) {
      events.addEventBinding(
         CustomUIEventBindingType.Activating, selector, EventData.of("NavBar", tab).append("StatName", "").append("Action", "").append("Amount", "0"), false
      );
   }

   static void addActionBindingWithDefaults(UIEventBuilder events, String selector, String action) {
      events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action).append("StatName", "").append("Amount", "0"), false);
   }

   static void addSimpleActionBinding(UIEventBuilder events, String selector, String action) {
      events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
   }
}
