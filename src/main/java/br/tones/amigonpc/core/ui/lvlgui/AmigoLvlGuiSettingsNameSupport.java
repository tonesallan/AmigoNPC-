package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsNameSupport {
   private static final int MAX_NAME_LENGTH = 20;

   private AmigoLvlGuiSettingsNameSupport() {
   }

   static AmigoLvlGuiSettingsModelPrefillState applyName(UUID ownerId, Store<EntityStore> store, AmigoLvlGuiEventData data, UICommandBuilder cmd) {
      String npcName = AmigoLvlGuiEventDataReadSupport.npcNameOrNull(data);
      if (npcName != null) {
         npcName = npcName.trim();
      }

      boolean truncated = npcName != null && npcName.length() > 20;
      AmigoNpcManager.getShared().setCustomNameWithStore(store, ownerId, npcName);
      AmigoLvlGuiMessageSupport.setSettingsFeedback(
         cmd,
         npcName != null && !npcName.isBlank()
            ? (truncated ? AmigoText.text("ui.settings.feedback.name.applied_truncated") : AmigoText.text("ui.settings.feedback.name.applied"))
            : AmigoText.text("ui.settings.feedback.name.removed")
      );
      return new AmigoLvlGuiSettingsModelPrefillState(false, false);
   }

   static AmigoLvlGuiSettingsModelPrefillState removeName(UUID ownerId, Store<EntityStore> store, UICommandBuilder cmd) {
      AmigoNpcManager.getShared().setCustomNameWithStore(store, ownerId, null);
      AmigoLvlGuiMessageSupport.setSettingsFeedback(cmd, AmigoText.text("ui.settings.feedback.name.removed"));
      return new AmigoLvlGuiSettingsModelPrefillState(false, false);
   }
}
