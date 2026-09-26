package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

final class NpcWeaponSupport {
   private NpcWeaponSupport() {
   }

   static String getHotbar0ItemId(Store<EntityStore> store, Ref<EntityStore> npcRef) {
      try {
         NPCEntity npc = (NPCEntity)store.getComponent(npcRef, NPCEntity.getComponentType());
         if (npc == null) {
            return null;
         }

         Inventory inv = npc.getInventory();
         if (inv == null) {
            return null;
         }

         ItemContainer hotbar = inv.getHotbar();
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
         NPCEntity npc = (NPCEntity)store.getComponent(npcRef, NPCEntity.getComponentType());
         if (npc == null) {
            return false;
         }

         Inventory inv = npc.getInventory();
         if (inv == null) {
            inv = new Inventory();
         }

         try {
            inv.setEntity(npc);
         } catch (Throwable var10) {
         }

         try {
            inv.setUsingToolsItem(false);
         } catch (Throwable var9) {
         }

         ItemContainer hotbar = inv.getHotbar();
         if (hotbar == null) {
            inv = new Inventory();

            try {
               inv.setEntity(npc);
            } catch (Throwable var8) {
            }

            try {
               inv.setUsingToolsItem(false);
            } catch (Throwable var7) {
            }

            hotbar = inv.getHotbar();
            if (hotbar == null) {
               return false;
            }
         }

         hotbar.setItemStackForSlot((short)0, new ItemStack(itemId, 1));

         try {
            ItemStack st = hotbar.getItemStack((short)0);
            if (st == null || st.isEmpty()) {
               return false;
            }
         } catch (Throwable var11) {
         }

         inv.setActiveHotbarSlot((byte)0);
         inv.markChanged();
         npc.setInventory(inv);
         npc.invalidateEquipmentNetwork();
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
