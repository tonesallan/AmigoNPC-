package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AmigoLvlGuiMainActionRenderSupport {
   private AmigoLvlGuiMainActionRenderSupport() {
   }

   static boolean applyResult(
      AmigoLvlGuiMainActionResult actionResult,
      BiConsumer<UICommandBuilder, String> setError,
      Consumer<UICommandBuilder> clearError,
      Consumer<UICommandBuilder> updateStats,
      Consumer<UICommandBuilder> updateRewards,
      Consumer<UICommandBuilder> sendUpdate
   ) {
      if (actionResult == null || !actionResult.handled) {
         return false;
      }

      if (!actionResult.updateStats && !actionResult.updateRewards && !actionResult.clearError && actionResult.errorMessage == null) {
         return true;
      }

      UICommandBuilder cmd = new UICommandBuilder();
      if (actionResult.errorMessage != null) {
         setError.accept(cmd, actionResult.errorMessage);
      } else if (actionResult.clearError) {
         clearError.accept(cmd);
      }

      if (actionResult.updateStats) {
         updateStats.accept(cmd);
      }

      if (actionResult.updateRewards) {
         updateRewards.accept(cmd);
      }

      sendUpdate.accept(cmd);
      return true;
   }
}
