package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.CombatMode;
import br.tones.amigonpc.core.i18n.AmigoText;

final class AmigoLvlGuiSettingsPlayerToggleSupport {
   private AmigoLvlGuiSettingsPlayerToggleSupport() {
   }

   static AmigoLvlGuiSettingsToggleResult toggleAutoLoot(AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending) {
      pending.autoLoot = !pending.autoLoot;
      return result(live, pending, "ui.settings.feedback.autoloot_pending", pending.autoLoot);
   }

   static AmigoLvlGuiSettingsToggleResult selectCombatMode(
      AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending, CombatMode mode
   ) {
      pending.combatMode = mode == null ? CombatMode.PROTECT_OWNER : mode;
      return new AmigoLvlGuiSettingsToggleResult(
         AmigoLvlGuiSettingsStateSupport.isDirty(live, pending),
         AmigoText.text(pending.combatMode == CombatMode.WEAKEST_ENEMY
            ? "ui.settings.feedback.combat_weakest_pending"
            : "ui.settings.feedback.combat_protect_pending")
      );
   }

   static AmigoLvlGuiSettingsToggleResult toggleAutoWeaponSwitch(AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending) {
      pending.autoWeaponSwitch = !pending.autoWeaponSwitch;
      return result(live, pending, "ui.settings.feedback.auto_weapon_pending", pending.autoWeaponSwitch);
   }

   static AmigoLvlGuiSettingsToggleResult toggleInterruptAttacks(AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending) {
      pending.interruptAttacks = !pending.interruptAttacks;
      return result(live, pending, "ui.settings.feedback.interrupt_pending", pending.interruptAttacks);
   }

   static AmigoLvlGuiSettingsToggleResult toggleHud(AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending) {
      pending.hud = !pending.hud;
      return result(live, pending, "ui.settings.feedback.hud_pending", pending.hud);
   }

   private static AmigoLvlGuiSettingsToggleResult result(
      AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending, String key, boolean value
   ) {
      return new AmigoLvlGuiSettingsToggleResult(
         AmigoLvlGuiSettingsStateSupport.isDirty(live, pending),
         AmigoText.format(key, AmigoText.onOff(value))
      );
   }
}
