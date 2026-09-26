package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiSettingsRenderSupport {
   private AmigoLvlGuiSettingsRenderSupport() {
   }

   static AmigoLvlGuiSettingsRenderState updateSettingsUI(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsDirty,
      boolean settingsModelPrefillDone,
      boolean settingsAdminPrefillDone,
      UICommandBuilder cmd
   ) {
      AmigoLvlGuiSettingsPlayerRenderState playerRenderState = AmigoLvlGuiSettingsPlayerRenderSupport.render(
         ownerId, settingsPending, settingsDirty, settingsModelPrefillDone, cmd
      );
      AmigoLvlGuiSettingsAdminRenderState adminRenderState = AmigoLvlGuiSettingsAdminRenderSupport.render(
         ownerId, viewerPlayerRef, settingsPending, settingsAdminPrefillDone, playerRenderState.hasNpc, cmd
      );
      return new AmigoLvlGuiSettingsRenderState(playerRenderState.modelPrefillDone, adminRenderState.adminPrefillDone);
   }
}
