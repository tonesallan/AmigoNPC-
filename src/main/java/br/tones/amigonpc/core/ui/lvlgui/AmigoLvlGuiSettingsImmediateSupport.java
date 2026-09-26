package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.loot.LootService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsImmediateSupport {
   private AmigoLvlGuiSettingsImmediateSupport() {
   }

   static String spawnNpc(UUID ownerId, PlayerRef viewerPlayerRef, Ref<EntityStore> viewerRef, Store<EntityStore> store) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      if (mgr.hasNpc(ownerId)) {
         return AmigoText.text("ui.settings.feedback.npc_already_active");
      }

      Object worldObj = null;

      try {
         worldObj = ((EntityStore)store.getExternalData()).getWorld();
      } catch (Throwable var7) {
      }

      boolean ok = mgr.spawnWithStore(worldObj, store, viewerRef, ownerId, viewerPlayerRef);
      return ok ? AmigoText.text("ui.settings.feedback.npc_spawned") : AmigoText.text("ui.settings.feedback.npc_spawn_failed");
   }

   static String despawnNpc(UUID ownerId, Store<EntityStore> store) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      if (!mgr.hasNpc(ownerId)) {
         return AmigoText.text("ui.settings.feedback.npc_none_active");
      }

      boolean ok = mgr.despawnWithStore(store, ownerId);
      return ok ? AmigoText.text("ui.settings.feedback.npc_despawned") : AmigoText.text("ui.settings.feedback.npc_despawn_failed");
   }

   static String openLoot(UUID ownerId, PlayerRef viewerPlayerRef, Ref<EntityStore> viewerRef, Store<EntityStore> store) {
      boolean ok = LootService.getShared().openLoot(ownerId, viewerPlayerRef, viewerRef, store);
      return ok ? AmigoText.text("ui.settings.feedback.loot_opened") : AmigoText.text("ui.settings.feedback.loot_open_failed");
   }
}
