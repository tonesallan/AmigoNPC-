package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.CombatMode;

final class AmigoLvlGuiSettingsSnapshot {
   boolean autoLoot;
   CombatMode combatMode = CombatMode.PROTECT_OWNER;
   boolean autoWeaponSwitch = true;
   boolean interruptAttacks = true;
   boolean hud;
   boolean godMode;
   boolean pvp;

   static AmigoLvlGuiSettingsSnapshot copyOf(AmigoLvlGuiSettingsSnapshot src) {
      AmigoLvlGuiSettingsSnapshot snapshot = new AmigoLvlGuiSettingsSnapshot();
      if (src != null) {
         snapshot.autoLoot = src.autoLoot;
         snapshot.combatMode = src.combatMode;
         snapshot.autoWeaponSwitch = src.autoWeaponSwitch;
         snapshot.interruptAttacks = src.interruptAttacks;
         snapshot.hud = src.hud;
         snapshot.godMode = src.godMode;
         snapshot.pvp = src.pvp;
      }
      return snapshot;
   }

   boolean sameToggles(AmigoLvlGuiSettingsSnapshot other) {
      return other != null
         && this.autoLoot == other.autoLoot
         && this.combatMode == other.combatMode
         && this.autoWeaponSwitch == other.autoWeaponSwitch
         && this.interruptAttacks == other.interruptAttacks
         && this.hud == other.hud
         && this.godMode == other.godMode
         && this.pvp == other.pvp;
   }
}
