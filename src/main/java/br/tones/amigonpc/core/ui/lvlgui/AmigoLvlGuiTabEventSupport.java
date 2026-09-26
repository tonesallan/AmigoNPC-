package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.debug.ActionTraceService;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AmigoLvlGuiTabEventSupport {
   private AmigoLvlGuiTabEventSupport() {
   }

   static boolean handleNavBarEvent(
      UUID ownerId,
      String navBar,
      BiConsumer<UICommandBuilder, String> applyNavBarTab,
      Consumer<UICommandBuilder> updateHeader,
      Consumer<UICommandBuilder> updateUIWithPending,
      Runnable ensureSettingsSession,
      Consumer<UICommandBuilder> updateSettingsUI,
      Consumer<UICommandBuilder> updateRewardsList,
      Consumer<UICommandBuilder> sendUpdate
   ) {
      if (navBar != null && !navBar.isEmpty()) {
         try {
            ActionTraceService.getShared().record(ownerId, "ui_tab", String.valueOf(navBar));
         } catch (Throwable var11) {
         }

         if ("rewards".equals(navBar)) {
            UICommandBuilder cmd = new UICommandBuilder();
            applyNavBarTab.accept(cmd, "rewards");
            updateHeader.accept(cmd);
            sendUpdate.accept(cmd);
            UICommandBuilder cmd2 = new UICommandBuilder();
            updateRewardsList.accept(cmd2);
            sendUpdate.accept(cmd2);
            return true;
         }

         UICommandBuilder cmd = new UICommandBuilder();
         applyNavBarTab.accept(cmd, navBar);
         if ("stats".equals(navBar)) {
            updateUIWithPending.accept(cmd);
         } else if ("leaderboard".equals(navBar)) {
            ensureSettingsSession.run();
            updateSettingsUI.accept(cmd);
         } else {
            updateHeader.accept(cmd);
         }

         sendUpdate.accept(cmd);
         return true;
      } else {
         return false;
      }
   }
}
