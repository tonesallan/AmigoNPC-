package br.tones.amigonpc.core.debug;

import com.hypixel.hytale.math.vector.Vector3d;
import java.lang.reflect.Method;
import java.util.List;

public final class NpcDebugSnapshotSupport {
   private NpcDebugSnapshotSupport() {
   }

   public static NpcDebugSnapshot emptySnapshot(String ownerUuid, String state) {
      NpcDebugSnapshot snapshot = new NpcDebugSnapshot();
      snapshot.ownerUuid = ownerUuid;
      snapshot.hasRecord = false;
      snapshot.hasNpcRef = false;
      snapshot.state = state;
      return snapshot;
   }

   public static DebugVec3 toVec3(Vector3d v) {
      if (v == null) {
         return null;
      }

      try {
         return new DebugVec3(v.x, v.y, v.z);
      } catch (Throwable ignored) {
         return null;
      }
   }

   public static void populateBackpackSummary(NpcDebugSnapshot snapshot, Object backpack) {
      if (snapshot != null) {
         try {
            if (backpack != null) {
               snapshot.backpackSlotsTotal = 45;
               int used = 0;
               Object items = null;

               try {
                  Method m = backpack.getClass().getMethod("getItems");
                  items = m.invoke(backpack);
               } catch (Throwable var9) {
               }

               if (items instanceof List) {
                  for (Object it : (List)items) {
                     if (it != null) {
                        boolean empty = false;

                        try {
                           Method m2 = it.getClass().getMethod("isEmpty");
                           empty = Boolean.TRUE.equals(m2.invoke(it));
                        } catch (Throwable var8) {
                        }

                        if (!empty) {
                           used++;
                        }
                     }
                  }
               }

               snapshot.backpackSlotsUsed = used;
            } else {
               snapshot.backpackSlotsTotal = 45;
               snapshot.backpackSlotsUsed = 0;
            }
         } catch (Throwable ignored) {
            snapshot.backpackSlotsTotal = 45;
         }
      }
   }
}
