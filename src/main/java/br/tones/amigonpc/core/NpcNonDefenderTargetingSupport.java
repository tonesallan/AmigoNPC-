package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class NpcNonDefenderTargetingSupport {
   private NpcNonDefenderTargetingSupport() {
   }

   static NpcNonDefenderTargetingSupport.Decision resolve(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      long now,
      double horizontal,
      double chaseMaxDistance,
      NpcNonDefenderTargetingSupport.AliveChecker aliveChecker
   ) {
      if (store != null && rec != null) {
         if (rec.combatUntilMillis > 0L && now > rec.combatUntilMillis) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
         }

         if (rec.npcCombatUntilMillis > 0L && now > rec.npcCombatUntilMillis) {
            rec.npcCombatUntilMillis = 0L;
            rec.npcCombatTargetRefObj = null;
         }

         Object combatTarget = NpcCombatStateSupport.getActiveCombatTarget(rec, now);
         if (combatTarget != null && !aliveChecker.isAlive(store, combatTarget)) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
            combatTarget = null;
         }

         Object npcCombatTarget = NpcCombatStateSupport.getActiveNpcCombatTarget(rec, now);
         if (npcCombatTarget != null && !aliveChecker.isAlive(store, npcCombatTarget)) {
            rec.npcCombatUntilMillis = 0L;
            rec.npcCombatTargetRefObj = null;
            npcCombatTarget = null;
         }

         rec.assistUntilMillis = 0L;
         rec.assistTargetRefObj = null;
         Object desiredTarget = combatTarget != null ? combatTarget : (npcCombatTarget != null ? npcCombatTarget : ownerRefObj);
         if (desiredTarget != null && !refEq(desiredTarget, ownerRefObj) && horizontal > chaseMaxDistance) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
            rec.npcCombatUntilMillis = 0L;
            rec.npcCombatTargetRefObj = null;
            combatTarget = null;
            npcCombatTarget = null;
            desiredTarget = ownerRefObj;
         }

         boolean inCombat = desiredTarget != null && !refEq(desiredTarget, ownerRefObj);
         return new NpcNonDefenderTargetingSupport.Decision(combatTarget, npcCombatTarget, desiredTarget, inCombat);
      } else {
         return new NpcNonDefenderTargetingSupport.Decision(null, null, ownerRefObj, false);
      }
   }

   private static boolean refEq(Object a, Object b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         try {
            return a.equals(b);
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   @FunctionalInterface
   interface AliveChecker {
      boolean isAlive(Store<EntityStore> var1, Object var2);
   }

   static final class Decision {
      final Object combatTarget;
      final Object npcCombatTarget;
      final Object desiredTarget;
      final boolean inCombat;

      Decision(Object combatTarget, Object npcCombatTarget, Object desiredTarget, boolean inCombat) {
         this.combatTarget = combatTarget;
         this.npcCombatTarget = npcCombatTarget;
         this.desiredTarget = desiredTarget;
         this.inCombat = inCombat;
      }
   }
}
