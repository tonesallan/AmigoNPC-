package br.tones.amigonpc.core;

final class PendingRespawn {
   final Object worldObj;
   final Object senderObj;
   final long atMillis;
   final String message;

   PendingRespawn(Object worldObj, Object senderObj, long atMillis, String message) {
      this.worldObj = worldObj;
      this.senderObj = senderObj;
      this.atMillis = atMillis;
      this.message = message;
   }
}
