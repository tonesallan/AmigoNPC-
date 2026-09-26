package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsPageAdminRouteSupport {
   private AmigoLvlGuiSettingsPageAdminRouteSupport() {
   }

   static AmigoLvlGuiSettingsPageActionResult handle(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      Store<EntityStore> store,
      AmigoLvlGuiEventData data,
      String action,
      boolean isAdmin,
      AmigoLvlGuiSettingsPageState pageState
   ) {
      if ("settings_admin_toggle_godmode".equals(action) || "settings_admin_toggle_pvp".equals(action)) {
         AmigoLvlGuiSettingsAccessResult accessResult = AmigoLvlGuiSettingsAccessSupport.ensureAdminAccess(viewerPlayerRef, ownerId, isAdmin);
         if (!accessResult.granted) {
            return new AmigoLvlGuiSettingsPageActionResult(true, pageState, accessResult.feedbackMessage, false);
         }

         AmigoLvlGuiSettingsToggleActionResult adminToggleResult = AmigoLvlGuiSettingsToggleActionSupport.handleAdminAction(
            ownerId, action, pageState.settingsLive, pageState.settingsPending
         );
         return new AmigoLvlGuiSettingsPageActionResult(
            true, AmigoLvlGuiSettingsPageStateSupport.withDirty(pageState, adminToggleResult.dirty), adminToggleResult.message, false
         );
      } else {
         if (!"settings_admin_debug_info".equals(action)
            && !"settings_admin_debug_toggle".equals(action)
            && !"settings_admin_add_xp".equals(action)
            && !"settings_admin_stats_add".equals(action)
            && !"settings_admin_stats_reset".equals(action)
            && !"settings_admin_rewards_reload".equals(action)
            && !"settings_admin_rewards_defaults".equals(action)
            && !"settings_admin_rewards_setreset".equals(action)
            && !"settings_admin_swordlvl_up".equals(action)
            && !"settings_admin_swordlvl_down".equals(action)) {
            return new AmigoLvlGuiSettingsPageActionResult(false, pageState, null, false);
         }

         AmigoLvlGuiSettingsAccessResult accessResult = AmigoLvlGuiSettingsAccessSupport.ensureAdminAccess(viewerPlayerRef, ownerId, isAdmin);
         if (!accessResult.granted) {
            return new AmigoLvlGuiSettingsPageActionResult(true, pageState, accessResult.feedbackMessage, false);
         }

         AmigoLvlGuiSettingsAdminActionResult adminActionResult = AmigoLvlGuiSettingsAdminActionSupport.handle(
            ownerId, action, data, store, pageState.settingsAdminPrefillDone
         );
         return new AmigoLvlGuiSettingsPageActionResult(
            true, AmigoLvlGuiSettingsPageStateSupport.withAdminPrefillDone(pageState, adminActionResult.adminPrefillDone), adminActionResult.message, false
         );
      }
   }
}
