package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsFooterSupport {
   private AmigoLvlGuiSettingsFooterSupport() {
   }

   static AmigoLvlGuiSettingsFooterState apply(
      UUID ownerId, Store<EntityStore> store, AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending, boolean isAdmin
   ) {
      if (settingsLive == null) {
         settingsLive = AmigoLvlGuiSettingsStateSupport.loadLiveSettings(ownerId);
      }

      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      boolean changedAny = false;
      if (settingsPending.autoLoot != settingsLive.autoLoot) {
         mgr.setAutoLootEnabled(ownerId, settingsPending.autoLoot);
         changedAny = true;
      }

      if (settingsPending.defender != settingsLive.defender) {
         mgr.setDefendeEnabled(ownerId, settingsPending.defender);
         changedAny = true;
      }

      if (settingsPending.hud != settingsLive.hud) {
         boolean curHud = LevelProgressHudService.getShared().isEnabled(ownerId);
         if (settingsPending.hud != curHud) {
            LevelProgressHudService.getShared().toggleEnabledPersisted(ownerId);
         }

         changedAny = true;
      }

      if (isAdmin && AmigoLvlGuiAdminSettingsLock.isHeldBy(ownerId)) {
         if (settingsPending.godMode != settingsLive.godMode && mgr.hasNpc(ownerId)) {
            mgr.setGodModeWithStore(store, ownerId, settingsPending.godMode);
            changedAny = true;
         }

         if (settingsPending.pvp != settingsLive.pvp) {
            mgr.setPvpEnabled(settingsPending.pvp);
            changedAny = true;
         }
      }

      AmigoLvlGuiSettingsSnapshot refreshedLive = AmigoLvlGuiSettingsStateSupport.loadLiveSettings(ownerId);
      AmigoLvlGuiSettingsSnapshot refreshedPending = AmigoLvlGuiSettingsSnapshot.copyOf(refreshedLive);
      boolean refreshedDirty = AmigoLvlGuiSettingsStateSupport.isDirty(refreshedLive, refreshedPending);
      if (isAdmin) {
         AmigoLvlGuiAdminSettingsLock.release(ownerId);
      }

      return new AmigoLvlGuiSettingsFooterState(
         refreshedLive,
         refreshedPending,
         refreshedDirty,
         changedAny ? AmigoText.text("ui.settings.footer.applied") : AmigoText.text("ui.settings.footer.no_changes")
      );
   }

   static AmigoLvlGuiSettingsFooterState close(UUID ownerId, AmigoLvlGuiSettingsSnapshot settingsLive, boolean isAdmin) {
      AmigoLvlGuiSettingsSnapshot nextPending = AmigoLvlGuiSettingsSnapshot.copyOf(settingsLive);
      boolean nextDirty = AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, nextPending);
      if (isAdmin) {
         AmigoLvlGuiAdminSettingsLock.release(ownerId);
      }

      return new AmigoLvlGuiSettingsFooterState(settingsLive, nextPending, nextDirty, null);
   }
}
