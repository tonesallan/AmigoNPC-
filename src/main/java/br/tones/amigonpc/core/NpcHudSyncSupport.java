package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class NpcHudSyncSupport {
   private NpcHudSyncSupport() {
   }

   static void updateNpcHud(Store<EntityStore> store, Ref<EntityStore> npcRef, AmigoNpcManager.NpcRecord rec) {
      if (store != null && npcRef != null && rec != null) {
         try {
            String customName = rec.customName;
            if (customName == null || customName.isBlank()) {
               store.tryRemoveComponent(npcRef, Nameplate.getComponentType());
               return;
            }

            Nameplate nameplate = (Nameplate)store.getComponent(npcRef, Nameplate.getComponentType());
            if (nameplate == null) {
               nameplate = new Nameplate();
            }

            nameplate.setText(customName);
            store.putComponent(npcRef, Nameplate.getComponentType(), nameplate);
         } catch (Throwable var5) {
         }
      }
   }
}
