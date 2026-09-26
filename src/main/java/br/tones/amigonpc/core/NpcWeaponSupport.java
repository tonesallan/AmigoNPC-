package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class NpcWeaponSupport {
   private NpcWeaponSupport() {
   }

   static String getHotbar0ItemId(Store<EntityStore> store, Ref<EntityStore> npcRef) {
      try {
         InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar)store.getComponent(
            npcRef, InventoryComponent.Hotbar.getComponentType()
         );
         if (hotbarComponent == null) {
            return null;
         }

         ItemContainer hotbar = hotbarComponent.getInventory();
         if (hotbar == null) {
            return null;
         }

         ItemStack st = hotbar.getItemStack((short)0);
         return st != null && !st.isEmpty() ? st.getItemId() : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static boolean isHotbar0Item(Store<EntityStore> store, Ref<EntityStore> npcRef, String itemId) {
      if (itemId != null && !itemId.isBlank()) {
         String cur = getHotbar0ItemId(store, npcRef);
         return itemId.equals(cur);
      } else {
         return false;
      }
   }

   static boolean equipWeaponInHotbar0(Store<EntityStore> store, Ref<EntityStore> npcRef, String itemId) {
      try {
         InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar)store.getComponent(
            npcRef, InventoryComponent.Hotbar.getComponentType()
         );

         if (hotbarComponent == null) {
            hotbarComponent = new InventoryComponent.Hotbar();
            store.putComponent(npcRef, InventoryComponent.Hotbar.getComponentType(), hotbarComponent);
         }

         InventoryComponent.Tool toolComponent = (InventoryComponent.Tool)store.getComponent(
            npcRef, InventoryComponent.Tool.getComponentType()
         );
         if (toolComponent != null) {
            toolComponent.setUsingToolsItem(false);
            toolComponent.markDirty();
         }

         ItemContainer hotbar = hotbarComponent.getInventory();
         if (hotbar == null) {
            return false;
         }

         hotbar.setItemStackForSlot((short)0, new ItemStack(itemId, 1));

         ItemStack st = hotbar.getItemStack((short)0);
         if (st == null || st.isEmpty()) {
            return false;
         }

         hotbarComponent.setActiveSlot((byte)0, npcRef, store);
         hotbarComponent.markDirty();
         hotbarComponent.setOutdatedEquipment(true);
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   static double getWeaponBaseDamage(String itemId) {
      if (itemId != null && !itemId.isBlank()) {
         try {
            Item it = (Item)Item.getAssetMap().getAsset(itemId);
            if (it == null) {
               return 4.0;
            }

            int lvl = it.getItemLevel();
            return 4.0 + Math.max(0, lvl) * 0.9;
         } catch (Throwable ignored) {
            return 4.0;
         }
      } else {
         return 4.0;
      }
   }
}
