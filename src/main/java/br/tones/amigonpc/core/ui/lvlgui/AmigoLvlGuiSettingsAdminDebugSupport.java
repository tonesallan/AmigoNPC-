package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.core.debug.BuildInfo;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.ui.UiBridge;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminDebugSupport {
   private AmigoLvlGuiSettingsAdminDebugSupport() {
   }

   static String buildDebugInfoMessage() {
      String v = null;
      String bid = null;
      String hb = null;
      String ub = null;

      try {
         v = BuildInfo.getModVersion();
      } catch (Throwable var8) {
      }

      try {
         bid = BuildInfo.getBuildId();
      } catch (Throwable var7) {
      }

      try {
         hb = HytaleBridge.getLastError();
      } catch (Throwable var6) {
      }

      try {
         ub = UiBridge.getLastError();
      } catch (Throwable var5) {
      }

      if (v == null) {
         v = AmigoText.text("ui.settings.feedback.unknown");
      }

      if (bid == null) {
         bid = AmigoText.text("ui.settings.feedback.unknown");
      }

      if (hb == null || hb.isBlank()) {
         hb = AmigoText.text("ui.settings.feedback.ok");
      }

      if (ub == null || ub.isBlank()) {
         ub = AmigoText.text("ui.settings.feedback.ok");
      }

      String msg = AmigoText.format("ui.settings.feedback.admin_debug_info", v, bid, hb, ub);
      if (msg.length() > 140) {
         msg = msg.substring(0, 140) + "...";
      }

      return msg;
   }

   static String toggleDebugLog(UUID ownerId) {
      boolean enabled = false;

      try {
         enabled = AmigoNpcManager.getShared().toggleDebugLog(ownerId);
      } catch (Throwable var3) {
      }

      return AmigoText.format("ui.settings.feedback.admin_debuglog", AmigoText.onOff(enabled));
   }
}
