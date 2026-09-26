package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
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

   static String selectBestBackpackWeapon(SimpleItemContainer backpack, boolean ranged) {
      if (backpack == null) {
         return null;
      }

      String bestId = null;
      int bestLevel = Integer.MIN_VALUE;
      short capacity;
      try {
         capacity = backpack.getCapacity();
      } catch (Throwable ignored) {
         return null;
      }

      for (short slot = 0; slot < capacity; slot++) {
         ItemStack stack;
         try {
            stack = backpack.getItemStack(slot);
         } catch (Throwable ignored) {
            continue;
         }

         if (stack == null || stack.isEmpty() || stack.isBroken()) {
            continue;
         }

         Item item;
         try {
            item = stack.getItem();
         } catch (Throwable ignored) {
            continue;
         }

         if (item == null || item.getWeapon() == null || isRangedWeapon(item) != ranged) {
            continue;
         }

         int level;
         try {
            level = item.getItemLevel();
         } catch (Throwable ignored) {
            level = 0;
         }

         if (bestId == null || level > bestLevel) {
            bestId = stack.getItemId();
            bestLevel = level;
         }
      }

      return bestId;
   }

   static boolean backpackContainsWeapon(SimpleItemContainer backpack, String itemId) {
      if (backpack == null || itemId == null || itemId.isBlank()) {
         return false;
      }

      short capacity;
      try {
         capacity = backpack.getCapacity();
      } catch (Throwable ignored) {
         return false;
      }

      for (short slot = 0; slot < capacity; slot++) {
         try {
            ItemStack stack = backpack.getItemStack(slot);
            if (stack != null && !stack.isEmpty() && !stack.isBroken() && itemId.equals(stack.getItemId())) {
               Item item = stack.getItem();
               return item != null && item.getWeapon() != null;
            }
         } catch (Throwable ignored) {
         }
      }

      return false;
   }

   static boolean clearHotbar0(Store<EntityStore> store, Ref<EntityStore> npcRef) {
      try {
         InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar)store.getComponent(
            npcRef, InventoryComponent.Hotbar.getComponentType()
         );
         if (hotbarComponent == null || hotbarComponent.getInventory() == null) {
            return true;
         }

         hotbarComponent.getInventory().removeItemStackFromSlot((short)0);
         hotbarComponent.markDirty();
         hotbarComponent.setOutdatedEquipment(true);
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean isRangedWeapon(Item item) {
      if (item == null || item.getWeapon() == null) {
         return false;
      }

      String animationId = null;
      try {
         animationId = item.getPlayerAnimationsId();
      } catch (Throwable ignored) {
      }

      if (animationId == null) {
         return false;
      }

      String id = animationId.toLowerCase(java.util.Locale.ROOT);
      return id.contains("bow")
         || id.contains("crossbow")
         || id.contains("rifle")
         || id.contains("gun")
         || id.contains("handgun")
         || id.contains("staff")
         || id.contains("wand")
         || id.contains("spellbook")
         || id.contains("throwing");
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
