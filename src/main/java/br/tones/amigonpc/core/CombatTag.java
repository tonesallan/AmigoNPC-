package br.tones.amigonpc.core;

import com.hypixel.hytale.math.vector.Vector3d;

final class CombatTag {
   final Object targetRefObj;
   Vector3d pos;
   long lastSeenMillis;

   CombatTag(Object targetRefObj, Vector3d pos, long lastSeenMillis) {
      this.targetRefObj = targetRefObj;
      this.pos = pos;
      this.lastSeenMillis = lastSeenMillis;
   }
}
