package br.tones.amigonpc.api.events;

@FunctionalInterface
public interface NpcLevelUpListener {
   void onNpcLevelUp(NpcLevelUpEvent var1);
}
