package br.tones.amigonpc.api;

public enum NpcXpSource {
   COMBAT_KILL,
   COMBAT_ASSIST,
   COLLECT_MINING,
   COLLECT_WOOD,
   COLLECT_PICKUP,
   COMMAND,
   QUEST;

   // $FF: synthetic method
   private static NpcXpSource[] $values() {
      return new NpcXpSource[]{COMBAT_KILL, COMBAT_ASSIST, COLLECT_MINING, COLLECT_WOOD, COLLECT_PICKUP, COMMAND, QUEST};
   }
}
