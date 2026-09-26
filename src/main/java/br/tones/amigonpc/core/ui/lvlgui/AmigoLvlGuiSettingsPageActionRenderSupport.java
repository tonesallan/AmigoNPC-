package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AmigoLvlGuiSettingsPageActionRenderSupport {
   private AmigoLvlGuiSettingsPageActionRenderSupport() {
   }

   static boolean applyResult(
      AmigoLvlGuiSettingsPageActionResult actionResult,
      UICommandBuilder cmd,
      Consumer<AmigoLvlGuiSettingsPageState> applyPageState,
      BiConsumer<UICommandBuilder, String> setFeedback,
      Consumer<UICommandBuilder> respondSettingsUpdate,
      Runnable closePage
   ) {
      if (actionResult != null && actionResult.handled) {
         applyPageState.accept(actionResult.pageState);
         if (actionResult.shouldClose) {
            closePage.run();
            return true;
         }

         if (actionResult.feedbackMessage != null) {
            setFeedback.accept(cmd, actionResult.feedbackMessage);
         }

         respondSettingsUpdate.accept(cmd);
         return true;
      } else {
         return false;
      }
   }
}
