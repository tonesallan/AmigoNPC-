package br.tones.amigonpc.core;

import java.util.UUID;

public final class WorldNpcSpawner {
   private WorldNpcSpawner() {
   }

   public static boolean spawn(Object worldObj, UUID ownerId) {
      return HytaleBridge.spawnBasicNpc(worldObj, ownerId);
   }
}
