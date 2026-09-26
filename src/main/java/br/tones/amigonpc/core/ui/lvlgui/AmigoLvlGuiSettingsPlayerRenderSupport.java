package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.UUID;

final class AmigoLvlGuiSettingsPlayerRenderSupport {
   private AmigoLvlGuiSettingsPlayerRenderSupport() {
   }

   static AmigoLvlGuiSettingsPlayerRenderState render(
      UUID ownerId, AmigoLvlGuiSettingsSnapshot settingsPending, boolean settingsDirty, boolean settingsModelPrefillDone, UICommandBuilder cmd
   ) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      boolean hasNpc = false;

      try {
         hasNpc = mgr.hasNpc(ownerId);
      } catch (Throwable var15) {
      }

      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNpcStatus.Text",
         AmigoText.format("ui.settings.status.npc", AmigoText.text(hasNpc ? "ui.settings.status.npc.active" : "ui.settings.status.npc.inactive"))
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPendingStatus.Text",
         settingsDirty ? AmigoText.text("ui.settings.status.pending.dirty") : AmigoText.text("ui.settings.status.pending.none")
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAutoLootToggle.Text",
         AmigoText.format("ui.settings.toggle.autoloot", AmigoText.onOff(settingsPending.autoLoot))
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsDefenderToggle.Text",
         AmigoText.format("ui.settings.toggle.defender", AmigoText.onOff(settingsPending.defender))
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsHudToggle.Text",
         AmigoText.format("ui.settings.toggle.hud", AmigoText.onOff(settingsPending.hud))
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsLanguageValue.Text",
         AmigoLvlGuiSettingsLanguageSupport.displayName(AmigoText.localeForPlayer(ownerId))
      );
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsSpawnButton.Disabled", hasNpc);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsDespawnButton.Disabled", !hasNpc);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsLootButton.Disabled", !hasNpc);
      String modelId = "";
      double scale = 1.0;
      String npcName = "";

      try {
         String m = AmigoPersistence.loadModelId(ownerId);
         if (m != null) {
            modelId = m;
         }
      } catch (Throwable var14) {
      }

      try {
         scale = AmigoPersistence.loadModelScale(ownerId);
      } catch (Throwable var13) {
      }

      try {
         String savedName = AmigoPersistence.loadCustomName(ownerId);
         if (savedName != null) {
            npcName = savedName;
         }
      } catch (Throwable var12) {
      }

      if (!(scale > 0.0) || Double.isNaN(scale) || Double.isInfinite(scale)) {
         scale = 1.0;
      }

      if (!settingsModelPrefillDone) {
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNpcNameInput.Value", npcName);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelIdInput.Value", modelId);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelScaleInput.Value", String.valueOf(scale));
         settingsModelPrefillDone = true;
      }

      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsApplyButton.Disabled", !settingsDirty);
      return new AmigoLvlGuiSettingsPlayerRenderState(hasNpc, settingsModelPrefillDone);
   }
}
