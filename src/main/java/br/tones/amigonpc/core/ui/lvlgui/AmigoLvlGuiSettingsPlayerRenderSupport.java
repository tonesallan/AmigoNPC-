package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.CombatMode;
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

      String activityState = mgr.getActivityStateName(ownerId);
      String activityText = switch (activityState) {
         case "DOWNED" -> AmigoText.text("ui.status.downed");
         case "COMBAT" -> AmigoText.text("ui.status.combat");
         case "GATHERING" -> AmigoText.text("ui.status.gathering");
         case "FOLLOWING" -> AmigoText.text("ui.status.following");
         case "IDLE" -> AmigoText.text("ui.status.idle");
         default -> AmigoText.text("ui.status.inactive");
      };
      String runtimeStatus = mgr.getNpcDisplayName(ownerId)
         + " | " + activityText
         + " | " + Math.round(mgr.getCachedHealth(ownerId))
         + "/" + Math.round(mgr.getCachedMaxHealth(ownerId))
         + " HP";
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNpcStatus.Text",
         runtimeStatus
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
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsCombatProtectOwner.Text",
         (settingsPending.combatMode == CombatMode.PROTECT_OWNER ? "(*) " : "( ) ") + AmigoText.text("ui.settings.combat.protect_owner")
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsCombatWeakest.Text",
         (settingsPending.combatMode == CombatMode.WEAKEST_ENEMY ? "(*) " : "( ) ") + AmigoText.text("ui.settings.combat.weakest")
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAutoWeaponToggle.Text",
         AmigoText.format("ui.settings.toggle.auto_weapon", AmigoText.onOff(settingsPending.autoWeaponSwitch))
      );
      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsInterruptToggle.Text",
         AmigoText.format("ui.settings.toggle.interrupt", AmigoText.onOff(settingsPending.interruptAttacks))
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
