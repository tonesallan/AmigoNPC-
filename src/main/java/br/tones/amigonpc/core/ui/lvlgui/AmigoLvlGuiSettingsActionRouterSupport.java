package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AmigoLvlGuiSettingsActionRouterSupport {
   private AmigoLvlGuiSettingsActionRouterSupport() {
   }

   @SafeVarargs
   static void handle(
      Runnable ensureSession,
      Consumer<UICommandBuilder> clearFeedback,
      Consumer<UICommandBuilder> respondUpdate,
      BiConsumer<UICommandBuilder, String> setFeedback,
      AmigoLvlGuiSettingsActionRouterSupport.CheckedActionHandler... handlers
   ) {
      ensureSession.run();
      UICommandBuilder cmd = new UICommandBuilder();
      clearFeedback.accept(cmd);

      try {
         for (AmigoLvlGuiSettingsActionRouterSupport.CheckedActionHandler handler : handlers) {
            if (handler != null && handler.handle(cmd)) {
               return;
            }
         }

         respondUpdate.accept(cmd);
      } catch (Throwable ignored) {
         try {
            setFeedback.accept(cmd, AmigoText.text("ui.settings.feedback.internal"));
         } catch (Throwable var11) {
         }

         try {
            respondUpdate.accept(cmd);
         } catch (Throwable var10) {
         }
      }
   }

   @FunctionalInterface
   interface CheckedActionHandler {
      boolean handle(UICommandBuilder var1) throws Throwable;
   }
}
