package br.tones.amigonpc.core.hud.levelprogress;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.awt.Color;

final class LevelProgressHud extends CustomUIHud {
   private volatile String levelText = "LEVEL: 1 XP: 0 / 0";
   private volatile float progressValue = 0.0F;
   private volatile Color textColor;

   LevelProgressHud(PlayerRef playerRef) {
      super(playerRef);
      this.textColor = Color.BLACK;
   }

   void setLevelInfo(int level, long xp, long xpNeeded) {
      long safeXp = Math.max(0L, xp);
      long safeNeed = Math.max(0L, xpNeeded);
      this.levelText = "LEVEL: " + Math.max(1, level) + " XP: " + safeXp + " / " + safeNeed;
      float p = 0.0F;
      if (safeNeed > 0L) {
         p = (float)((double)safeXp / (double)safeNeed);
         if (p < 0.0F) {
            p = 0.0F;
         }

         if (p > 1.0F) {
            p = 1.0F;
         }
      }

      this.progressValue = p;
   }

   void setLevelInfoFromPreformatted(String text, float progress) {
      if (text != null) {
         this.levelText = text;
      }

      float p = progress;
      if (progress < 0.0F) {
         p = 0.0F;
      }

      if (p > 1.0F) {
         p = 1.0F;
      }

      this.progressValue = p;
   }

   void requestUpdate() {
      UICommandBuilder b = new UICommandBuilder();
      b.set("#LevelLabel.TextSpans", Message.raw(this.levelText).color(this.textColor));
      b.set("#ProgressBar.Value", this.progressValue);
      this.update(false, b);
   }

   void setRootVisible(boolean visible) {
      UICommandBuilder b = new UICommandBuilder();
      b.set("#LevelProgressHud.Visible", visible);
      this.update(false, b);
   }

   void setTextColor(com.hypixel.hytale.protocol.Color c) {
      this.textColor = Color.BLACK;
   }

   protected void build(UICommandBuilder builder) {
      builder.append("HUD/AmigoNPC_LevelProgress.ui");
      builder.set("#LevelLabel.TextSpans", Message.raw(this.levelText).color(this.textColor));
      builder.set("#ProgressBar.Value", this.progressValue);
   }
}
