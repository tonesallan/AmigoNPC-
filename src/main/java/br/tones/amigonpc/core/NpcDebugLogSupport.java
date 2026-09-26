package br.tones.amigonpc.core;

import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.zones.AmigoZonesConfigService;
import java.util.Map;
import java.util.UUID;

final class NpcDebugLogSupport {
   private NpcDebugLogSupport() {
   }

   static void debugEquip(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg, NpcDebugLogSupport.OwnerMessenger messenger) {
      if (rec != null && ownerId != null) {
         if (rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextEquipMillis) {
               rec.debugNextEquipMillis = now + 1200L;
               messenger.send(rec.worldObj, ownerId, AmigoText.format("core.debug.prefix.equip", msg));
            }
         }
      }
   }

   static void debugCombat(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg, NpcDebugLogSupport.OwnerMessenger messenger) {
      if (rec != null && ownerId != null) {
         if (rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextCombatMillis) {
               rec.debugNextCombatMillis = now + 900L;
               messenger.send(rec.worldObj, ownerId, AmigoText.format("core.debug.prefix.combat", msg));
            }
         }
      }
   }

   static void debugAttack(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg, NpcDebugLogSupport.OwnerMessenger messenger) {
      if (rec != null && ownerId != null) {
         if (rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextAttackMillis) {
               rec.debugNextAttackMillis = now + 900L;
               messenger.send(rec.worldObj, ownerId, AmigoText.format("core.debug.prefix.attack", msg));
            }
         }
      }
   }

   static void debugZones(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg, NpcDebugLogSupport.OwnerMessenger messenger) {
      if (rec != null && ownerId != null) {
         boolean cfgEnabled = false;

         try {
            cfgEnabled = AmigoZonesConfigService.get().debugLog;
         } catch (Throwable var7) {
         }

         if (cfgEnabled || rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextZonesMillis) {
               rec.debugNextZonesMillis = now + 1000L;
               messenger.send(rec.worldObj, ownerId, AmigoText.format("core.debug.prefix.zones", msg));
            }
         }
      }
   }

   static void debugMobLevel(AmigoNpcManager.NpcRecord rec, UUID ownerId, String msg, NpcDebugLogSupport.OwnerMessenger messenger) {
      if (rec != null && ownerId != null) {
         boolean cfgEnabled = false;

         try {
            cfgEnabled = AmigoZonesConfigService.get().debugLog;
         } catch (Throwable var7) {
         }

         if (cfgEnabled || rec.debugLogEnabled) {
            long now = System.currentTimeMillis();
            if (now >= rec.debugNextMobLevelMillis) {
               rec.debugNextMobLevelMillis = now + 1000L;
               messenger.send(rec.worldObj, ownerId, AmigoText.format("core.debug.prefix.moblevel", msg));
            }
         }
      }
   }

   static void debugDamage(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId, String msg, NpcDebugLogSupport.OwnerMessenger messenger) {
      if (ownerId != null && msg != null && !msg.isBlank()) {
         AmigoNpcManager.NpcRecord rec = records.get(ownerId);
         if (rec != null) {
            if (rec.debugLogEnabled) {
               long now = System.currentTimeMillis();
               if (now >= rec.debugNextDamageMillis) {
                  rec.debugNextDamageMillis = now + 1200L;
                  messenger.send(rec.worldObj, ownerId, AmigoText.format("core.debug.prefix.damage", msg));
               }
            }
         }
      }
   }

   @FunctionalInterface
   interface OwnerMessenger {
      void send(Object var1, UUID var2, String var3);
   }
}
