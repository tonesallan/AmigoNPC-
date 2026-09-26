package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsModelSupport {
   private AmigoLvlGuiSettingsModelSupport() {
   }

   static AmigoLvlGuiSettingsModelPrefillState applyModel(
      UUID ownerId, PlayerRef viewerPlayerRef, Ref<EntityStore> viewerRef, Store<EntityStore> store, AmigoLvlGuiEventData data, UICommandBuilder cmd
   ) {
      String modelId = data != null ? data.modelId : null;
      if (modelId != null) {
         modelId = modelId.trim();
      }

      double scale = 1.0;
      boolean scaleAdjusted = false;

      try {
         if (data != null && data.scale != null && !Double.isNaN(data.scale) && Double.isFinite(data.scale) && data.scale > 0.0) {
            scale = data.scale;
         }
      } catch (Throwable var15) {
      }

      if (!(scale >= 0.1) || !(scale <= 5.0)) {
         scale = 1.0;
         scaleAdjusted = true;
      }

      if (modelId != null && !modelId.isBlank()) {
         AmigoPersistence.saveModel(ownerId, modelId, scale);
         AmigoNpcManager mgr = AmigoNpcManager.getShared();
         if (mgr.hasNpc(ownerId)) {
            boolean ok1 = mgr.despawnWithStore(store, ownerId);
            Object worldObj = null;

            try {
               worldObj = ((EntityStore)store.getExternalData()).getWorld();
            } catch (Throwable var14) {
            }

            boolean ok2 = mgr.spawnWithStore(worldObj, store, viewerRef, ownerId, viewerPlayerRef);
            if (ok1 && ok2) {
               AmigoLvlGuiMessageSupport.setSettingsFeedback(
                  cmd,
                  scaleAdjusted ? AmigoText.text("ui.settings.feedback.model.applied_scale_adjusted") : AmigoText.text("ui.settings.feedback.model.applied")
               );
            } else {
               AmigoLvlGuiMessageSupport.setSettingsFeedback(
                  cmd,
                  scaleAdjusted
                     ? AmigoText.text("ui.settings.feedback.model.saved_scale_adjusted_respawn_failed")
                     : AmigoText.text("ui.settings.feedback.model.saved_respawn_failed")
               );
            }
         } else {
            AmigoLvlGuiMessageSupport.setSettingsFeedback(
               cmd, scaleAdjusted ? AmigoText.text("ui.settings.feedback.model.saved_scale_adjusted") : AmigoText.text("ui.settings.feedback.model.saved")
            );
         }

         return new AmigoLvlGuiSettingsModelPrefillState(false, false);
      } else {
         AmigoLvlGuiMessageSupport.setSettingsFeedback(cmd, AmigoText.text("ui.settings.feedback.model.empty"));
         return new AmigoLvlGuiSettingsModelPrefillState(false, false);
      }
   }

   static AmigoLvlGuiSettingsModelPrefillState removeModel(
      UUID ownerId, PlayerRef viewerPlayerRef, Ref<EntityStore> viewerRef, Store<EntityStore> store, UICommandBuilder cmd
   ) {
      AmigoPersistence.saveModel(ownerId, null, 1.0);
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      if (mgr.hasNpc(ownerId)) {
         boolean ok1 = mgr.despawnWithStore(store, ownerId);
         Object worldObj = null;

         try {
            worldObj = ((EntityStore)store.getExternalData()).getWorld();
         } catch (Throwable var9) {
         }

         boolean ok2 = mgr.spawnWithStore(worldObj, store, viewerRef, ownerId, viewerPlayerRef);
         AmigoLvlGuiMessageSupport.setSettingsFeedback(
            cmd, ok1 && ok2 ? AmigoText.text("ui.settings.feedback.model.removed") : AmigoText.text("ui.settings.feedback.model.removed_respawn_failed")
         );
      } else {
         AmigoLvlGuiMessageSupport.setSettingsFeedback(cmd, AmigoText.text("ui.settings.feedback.model.removed"));
      }

      return new AmigoLvlGuiSettingsModelPrefillState(false, false);
   }
}
