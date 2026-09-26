package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.debug.ErrorDumpService;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

final class AmigoLvlGuiErrorSupport {
   private AmigoLvlGuiErrorSupport() {
   }

   static void handleEventError(
      UUID ownerId, AmigoLvlGuiEventData data, Throwable error, Consumer<UICommandBuilder> setError, Consumer<UICommandBuilder> sendUpdate
   ) {
      try {
         Map<String, Object> extra = new HashMap<>();
         extra.put("navBar", AmigoLvlGuiEventDataReadSupport.navBarOrNull(data));
         extra.put("action", AmigoLvlGuiEventDataReadSupport.actionOrNull(data));
         extra.put("statName", AmigoLvlGuiEventDataReadSupport.statNameOrNull(data));
         extra.put("amount", AmigoLvlGuiEventDataReadSupport.amountOrNull(data));
         extra.put("claimLevel", AmigoLvlGuiEventDataReadSupport.claimLevelOrNull(data));
         ErrorDumpService.getShared().dumpOnServerError(ownerId, "ui:handleDataEvent", error, extra);
      } catch (Throwable var8) {
      }

      try {
         ActionTraceService.getShared().record(ownerId, "ui_error", error.getClass().getSimpleName() + ": " + error.getMessage());
      } catch (Throwable var7) {
      }

      try {
         UICommandBuilder cmd = new UICommandBuilder();
         setError.accept(cmd);
         sendUpdate.accept(cmd);
      } catch (Throwable var6) {
      }
   }
}
