package br.tones.amigonpc.core;

import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class NpcBackpackLootSupport {
   private static final String[] BACKPACK_FULL_MESSAGES = new String[]{
      "Hey amigo, estou com o inventário cheio.",
      "Hey, que tal voltarmos? Estou com o inventário cheio.",
      "Estou com o inventário cheio.",
      "Não consigo carregar mais nada."
   };

   private NpcBackpackLootSupport() {
   }

   static boolean isBackpackCompletelyFull(SimpleItemContainer bag) {
      if (bag == null) {
         return true;
      }

      short cap;
      try {
         cap = bag.getCapacity();
      } catch (Throwable t) {
         return true;
      }

      for (short slot = 0; slot < cap; slot++) {
         ItemStack st;
         try {
            st = bag.getItemStack(slot);
         } catch (Throwable t) {
            continue;
         }

         if (st == null || st.isEmpty()) {
            return false;
         }

         try {
            Item item = st.getItem();
            int max = item != null ? item.getMaxStack() : 1;
            if (st.getQuantity() < max) {
               return false;
            }
         } catch (Throwable ignored) {
            return false;
         }
      }

      return true;
   }

   static void maybeNotifyBackpackFull(
      AmigoNpcManager.NpcRecord rec, UUID ownerId, Object worldObj, long now, long cooldownMillis, NpcBackpackLootSupport.OwnerMessenger messenger
   ) {
      if (rec != null && ownerId != null && worldObj != null) {
         if (now >= rec.nextLootFullMsgMillis) {
            rec.nextLootFullMsgMillis = now + cooldownMillis;

            try {
               messenger.send(worldObj, ownerId, BACKPACK_FULL_MESSAGES[ThreadLocalRandom.current().nextInt(BACKPACK_FULL_MESSAGES.length)]);
            } catch (Throwable var9) {
            }
         }
      }
   }

   static boolean tryPickupGroundItemIntoBackpack(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      SimpleItemContainer bag,
      Object itemRefObj,
      NpcBackpackLootSupport.FullBackpackNotifier fullBackpackNotifier,
      NpcBackpackLootSupport.LootAccumulator lootAccumulator
   ) {
      if (store == null || ownerId == null || rec == null || bag == null || itemRefObj == null) {
         return false;
      } else if (!(itemRefObj instanceof Ref<?> rawItemRef)) {
         return false;
      } else {
         @SuppressWarnings("unchecked")
         Ref<EntityStore> itemRef = (Ref<EntityStore>)rawItemRef;
         ItemComponent ic;
         try {
            ic = (ItemComponent)store.getComponent(itemRef, ItemComponent.getComponentType());
         } catch (Throwable t) {
            return false;
         }

         if (ic == null) {
            return false;
         }

         try {
            ic.setPickupDelay(0.0F);
         } catch (Throwable var22) {
         }

         ItemStack before;
         try {
            before = ic.getItemStack();
         } catch (Throwable t) {
            return false;
         }

         if (before == null) {
            return false;
         }

         int beforeQty;
         try {
            beforeQty = before.getQuantity();
         } catch (Throwable t) {
            return false;
         }

         if (beforeQty <= 0) {
            return false;
         }

         String itemId;
         try {
            itemId = before.getItemId();
         } catch (Throwable t) {
            itemId = null;
         }

         if (itemId == null || itemId.isBlank()) {
            itemId = "item";
         }

         try {
            if (!AutoLootConfigService.isItemAllowed(itemId)) {
               return false;
            }
         } catch (Throwable var18) {
         }

         try {
            ItemStackTransaction tx = bag.addItemStack(before);
            if (tx == null || !tx.succeeded()) {
               if (isBackpackCompletelyFull(bag)) {
                  rec.lootPausedInventoryFull = true;
                  rec.lootingActive = false;
                  fullBackpackNotifier.notify(rec, ownerId, rec.worldObj, System.currentTimeMillis());
               }
               return false;
            }

            ItemStack remainder = tx.getRemainder();
            int remQty = remainder != null && !remainder.isEmpty() ? Math.max(0, remainder.getQuantity()) : 0;
            int inserted = Math.max(0, beforeQty - remQty);
            if (inserted <= 0) {
               return false;
            }

            if (remQty > 0) {
               ic.setItemStack(remainder);
               store.putComponent(itemRef, ItemComponent.getComponentType(), ic);
            } else if (itemRef.isValid()) {
               store.removeEntity(itemRef, RemoveReason.REMOVE);
            }

            rec.backpackDirty = true;
            long now = System.currentTimeMillis();
            if (rec.nextBackpackSaveMillis <= now) {
               rec.nextBackpackSaveMillis = now + 500L;
            }

            lootAccumulator.add(rec, itemId, inserted, now);
            return true;
         } catch (Throwable ignored) {
            return false;
         }
      }
   }

   @FunctionalInterface
   interface FullBackpackNotifier {
      void notify(AmigoNpcManager.NpcRecord var1, UUID var2, Object var3, long var4);
   }

   @FunctionalInterface
   interface LootAccumulator {
      void add(AmigoNpcManager.NpcRecord var1, String var2, int var3, long var4);
   }

   @FunctionalInterface
   interface OwnerMessenger {
      void send(Object var1, UUID var2, String var3);
   }
}
