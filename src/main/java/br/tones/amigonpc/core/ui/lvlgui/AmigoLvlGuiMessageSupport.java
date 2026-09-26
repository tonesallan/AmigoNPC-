package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

final class AmigoLvlGuiMessageSupport {
   private AmigoLvlGuiMessageSupport() {
   }

   static void setSettingsFeedback(UICommandBuilder cmd, String msg) {
      if (cmd != null) {
         if (msg == null) {
            msg = "";
         }

         boolean vis = !msg.isBlank();
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsFeedback.Text", msg);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsFeedback.Visible", vis);
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsFeedbackBox.Visible", vis);
      }
   }

   static void clearSettingsFeedback(UICommandBuilder cmd) {
      setSettingsFeedback(cmd, "");
   }

   static void setError(UICommandBuilder cmd, String msg) {
   }

   static void clearError(UICommandBuilder cmd) {
      setError(cmd, "");
   }
}
