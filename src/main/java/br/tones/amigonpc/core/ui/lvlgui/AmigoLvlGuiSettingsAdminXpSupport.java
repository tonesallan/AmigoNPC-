package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.xp.sources.NpcXpAwardService;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfig;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfigService;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

final class AmigoLvlGuiSettingsAdminXpSupport {
   private AmigoLvlGuiSettingsAdminXpSupport() {
   }

   static String addXp(UUID ownerId, Store<EntityStore> store, AmigoLvlGuiEventData data) {
      int amt = AmigoLvlGuiEventDataReadSupport.amountOrDefault(data, 0);
      if (amt <= 0) {
         return AmigoText.text("ui.settings.feedback.admin_xp.invalid_amount");
      }

      NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
      if (cfg != null && cfg.enableCommandXP) {
         String srcTxt = AmigoLvlGuiEventDataReadSupport.statNameOrNull(data);
         NpcXpSource src = NpcXpSource.COMMAND;
         if (srcTxt != null && !srcTxt.isBlank()) {
            try {
               src = NpcXpSource.valueOf(srcTxt.trim().toUpperCase(Locale.ROOT));
            } catch (Throwable ignored) {
               src = NpcXpSource.COMMAND;
            }
         }

         String worldName = "";

         try {
            Object w = ((EntityStore)store.getExternalData()).getWorld();
            if (w != null) {
               try {
                  Method mm = w.getClass().getMethod("getName");
                  Object r = mm.invoke(w);
                  if (r != null) {
                     worldName = String.valueOf(r);
                  }
               } catch (Throwable var11) {
               }
            }
         } catch (Throwable var12) {
         }

         NpcXpContext ctxObj = new NpcXpContext(worldName, worldName, null, 0, null, 0, System.currentTimeMillis());
         boolean ok = NpcXpAwardService.getShared().awardFixed(ownerId, amt, src, ctxObj);
         return ok ? AmigoText.format("ui.settings.feedback.admin_xp.success", amt, src.name()) : AmigoText.text("ui.settings.feedback.admin_xp.failed");
      } else {
         return AmigoText.text("ui.settings.feedback.admin_xp.disabled");
      }
   }
}
