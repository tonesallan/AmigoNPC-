package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Locale;
import java.util.UUID;

final class NpcFollowRescueSupport {
   private NpcFollowRescueSupport() {
   }

   static void tryRescue(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d ownerPos,
      TransformComponent ownerTransform,
      TransformComponent npcTransform,
      long now,
      double horizontal,
      double dy,
      boolean inCombatOrAssist,
      boolean ownerUnderground,
      double undergroundTeleportDistance,
      long assistGraceMillis,
      Object npcEntityComponentType,
      NpcFollowRescueSupport.ComponentGetter componentGetter,
      NpcFollowRescueSupport.LockedTargetSetter lockedTargetSetter,
      NpcFollowRescueSupport.MarkedTargetSetter markedTargetSetter,
      NpcFollowRescueSupport.YawExtractor yawExtractor,
      NpcFollowRescueSupport.OffsetInFrontResolver offsetInFrontResolver,
      NpcFollowRescueSupport.Vector3dCoercer vector3dCoercer,
      NpcFollowRescueSupport.DebugLogger debugLogger
   ) {
      if (store != null && rec != null && ownerPos != null && ownerTransform != null && npcTransform != null) {
         boolean shouldRescue = NpcFollowMaintenanceSupport.shouldRescue(
            rec, now, horizontal, dy, inCombatOrAssist, ownerUnderground, undergroundTeleportDistance
         );
         if (shouldRescue && NpcFollowMaintenanceSupport.beginRescue(rec, now, assistGraceMillis)) {
            teleportNearOwner(ownerPos, ownerTransform, npcTransform, yawExtractor, offsetInFrontResolver, vector3dCoercer);
            reattachFollow(store, rec, ownerRefObj, npcEntityComponentType, componentGetter, lockedTargetSetter, markedTargetSetter);
            debugLogger.log(rec, ownerId, "hardTeleport: h=" + String.format(Locale.US, "%.2f", horizontal) + " dy=" + String.format(Locale.US, "%.2f", dy));
         }
      }
   }

   private static void teleportNearOwner(
      Vector3d ownerPos,
      TransformComponent ownerTransform,
      TransformComponent npcTransform,
      NpcFollowRescueSupport.YawExtractor yawExtractor,
      NpcFollowRescueSupport.OffsetInFrontResolver offsetInFrontResolver,
      NpcFollowRescueSupport.Vector3dCoercer vector3dCoercer
   ) {
      try {
         Float yaw = null;

         try {
            yaw = yawExtractor.extract(ownerTransform.getRotation());
         } catch (Throwable var9) {
         }

         Object targetPosObj = offsetInFrontResolver.resolve(ownerPos, yaw, 4.0);
         Vector3d targetPos = targetPosObj instanceof Vector3d ? (Vector3d)targetPosObj : (Vector3d)vector3dCoercer.coerce(targetPosObj);
         if (targetPos != null) {
            npcTransform.teleportPosition(targetPos);
         }
      } catch (Throwable var10) {
      }
   }

   private static void reattachFollow(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Object npcEntityComponentType,
      NpcFollowRescueSupport.ComponentGetter componentGetter,
      NpcFollowRescueSupport.LockedTargetSetter lockedTargetSetter,
      NpcFollowRescueSupport.MarkedTargetSetter markedTargetSetter
   ) {
      try {
         Object npcEntityObj = componentGetter.get(store, rec.refObj, npcEntityComponentType);
         if (npcEntityObj != null) {
            lockedTargetSetter.set(npcEntityObj, ownerRefObj);
            markedTargetSetter.set(npcEntityObj, "CombatTarget", null);
         }
      } catch (Throwable var8) {
      }
   }

   @FunctionalInterface
   interface ComponentGetter {
      Object get(Object var1, Object var2, Object var3);
   }

   @FunctionalInterface
   interface DebugLogger {
      void log(AmigoNpcManager.NpcRecord var1, UUID var2, String var3);
   }

   @FunctionalInterface
   interface LockedTargetSetter {
      void set(Object var1, Object var2);
   }

   @FunctionalInterface
   interface MarkedTargetSetter {
      void set(Object var1, String var2, Object var3);
   }

   @FunctionalInterface
   interface OffsetInFrontResolver {
      Object resolve(Object var1, Float var2, double var3);
   }

   @FunctionalInterface
   interface Vector3dCoercer {
      Object coerce(Object var1);
   }

   @FunctionalInterface
   interface YawExtractor {
      Float extract(Object var1);
   }
}
