package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminRenderSupport {
   private AmigoLvlGuiSettingsAdminRenderSupport() {
   }

   static AmigoLvlGuiSettingsAdminRenderState render(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsAdminPrefillDone,
      boolean hasNpc,
      UICommandBuilder cmd
   ) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      boolean isAdmin = AmigoLvlGuiPermissionSupport.isAdminBestEffort(viewerPlayerRef, ownerId);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSection.Visible", isAdmin);
      if (!isAdmin) {
         return new AmigoLvlGuiSettingsAdminRenderState(settingsAdminPrefillDone);
      }

      boolean lockHeldByMe = AmigoLvlGuiAdminSettingsLock.isHeldBy(ownerId);
      String lockName = AmigoLvlGuiAdminSettingsLock.ownerName;
      boolean lockedByOther = AmigoLvlGuiAdminSettingsLock.ownerId != null && !lockHeldByMe;
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminLockLabel.Visible", lockedByOther);
      if (lockedByOther) {
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminLockLabel.Text",
            AmigoText.format("ui.settings.admin.locked_by", lockName != null ? lockName : "-")
         );
      } else {
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminLockLabel.Text", "");
      }

      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminPvpToggle.Text",
         AmigoText.format("ui.settings.toggle.mod_pvp", AmigoText.onOff(settingsPending.pvp))
      );
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminPvpToggle.Disabled", lockedByOther);
      boolean dbg = false;

      try {
         dbg = mgr.isDebugLogEnabled(ownerId);
      } catch (Throwable var14) {
      }

      cmd.set(
         "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugLogButton.Text",
         AmigoText.format("ui.settings.toggle.debuglog", AmigoText.onOff(dbg))
      );
      if (!settingsAdminPrefillDone) {
         try {
            int rp = RewardsStateService.getShared().getResetPoints(ownerId);
            cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsResetPointsInput.Value", String.valueOf(rp));
         } catch (Throwable var13) {
         }

         settingsAdminPrefillDone = true;
      }

      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugInfoButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugLogButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminAddXpButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsAddButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsResetButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsReloadButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsDefaultsButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsSetResetButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordLvlUpButton.Disabled", lockedByOther);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordLvlDownButton.Disabled", lockedByOther);
      return new AmigoLvlGuiSettingsAdminRenderState(settingsAdminPrefillDone);
   }
}
