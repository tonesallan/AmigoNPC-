package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsSnapshot {
   boolean autoLoot;
   boolean defender;
   boolean hud;
   boolean godMode;
   boolean pvp;

   static AmigoLvlGuiSettingsSnapshot copyOf(AmigoLvlGuiSettingsSnapshot src) {
      AmigoLvlGuiSettingsSnapshot snapshot = new AmigoLvlGuiSettingsSnapshot();
      if (src != null) {
         snapshot.autoLoot = src.autoLoot;
         snapshot.defender = src.defender;
         snapshot.hud = src.hud;
         snapshot.godMode = src.godMode;
         snapshot.pvp = src.pvp;
      }

      return snapshot;
   }

   boolean sameToggles(AmigoLvlGuiSettingsSnapshot other) {
      return other == null
         ? false
         : this.autoLoot == other.autoLoot
            && this.defender == other.defender
            && this.hud == other.hud
            && this.godMode == other.godMode
            && this.pvp == other.pvp;
   }
}
