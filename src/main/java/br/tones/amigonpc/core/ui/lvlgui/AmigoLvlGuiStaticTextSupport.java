package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

final class AmigoLvlGuiStaticTextSupport {
   private AmigoLvlGuiStaticTextSupport() {
   }

   static void apply(UICommandBuilder cmd) {
      if (cmd != null) {
         cmd.set("#NavStatsButtonSelected.Text", AmigoText.text("ui.nav.stats"));
         cmd.set("#NavStatsButton.Text", AmigoText.text("ui.nav.stats"));
         cmd.set("#NavLeaderboardButtonSelected.Text", AmigoText.text("ui.nav.settings"));
         cmd.set("#NavLeaderboardButton.Text", AmigoText.text("ui.nav.settings"));
         cmd.set("#NavRewardsButtonSelected.Text", AmigoText.text("ui.nav.rewards"));
         cmd.set("#NavRewardsButton.Text", AmigoText.text("ui.nav.rewards"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsTitle.Text", AmigoText.text("ui.settings.title"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsSubtitle.Text", AmigoText.text("ui.settings.subtitle"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPlayerSectionTitle.Text", AmigoText.text("ui.settings.section.player"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPlayerQuickActionsTitle.Text", AmigoText.text("ui.settings.section.quick_actions")
         );
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPlayerPreferencesTitle.Text", AmigoText.text("ui.settings.section.preferences")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsLanguageLabel.Text", AmigoText.text("ui.settings.player.field.language"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsLanguageHint.Text", AmigoText.text("ui.settings.player.language.hint"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPlayerModelSkinTitle.Text", AmigoText.text("ui.settings.section.model_skin"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNpcNameLabel.Text", AmigoText.text("ui.settings.player.field.name"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNpcNameInput.PlaceholderText",
            AmigoText.text("ui.settings.player.field.name.placeholder")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsApplyNameButton.Text", AmigoText.text("ui.settings.player.name.apply"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsNameOffButton.Text", AmigoText.text("ui.settings.player.name.off"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelIdLabel.Text", AmigoText.text("ui.settings.player.field.model_id"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelIdInput.PlaceholderText",
            AmigoText.text("ui.settings.player.field.model_id.placeholder")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelScaleLabel.Text", AmigoText.text("ui.settings.player.field.scale"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelScaleInput.PlaceholderText",
            AmigoText.text("ui.settings.player.field.scale.placeholder")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsApplyModelButton.Text", AmigoText.text("ui.settings.player.model.apply"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsModelOffButton.Text", AmigoText.text("ui.settings.player.model.off"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsPlayerModelHint.Text", AmigoText.text("ui.settings.player.model.hint"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsSpawnButton.Text", AmigoText.text("ui.settings.player.spawn"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsDespawnButton.Text", AmigoText.text("ui.settings.player.despawn"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsLootButton.Text", AmigoText.text("ui.settings.player.loot"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSectionTitle.Text", AmigoText.text("ui.settings.section.admin"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugActionsTitle.Text", AmigoText.text("ui.settings.section.admin_actions")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminAddXpTitle.Text", AmigoText.text("ui.settings.section.admin_add_xp"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsTitle.Text", AmigoText.text("ui.settings.section.admin_stats"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsTitle.Text", AmigoText.text("ui.settings.section.admin_rewards"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordTitle.Text", AmigoText.text("ui.settings.section.admin_sword"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminDebugInfoButton.Text", AmigoText.text("ui.settings.admin.debug_info"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminAddXpAmountInput.PlaceholderText",
            AmigoText.text("ui.settings.admin.add_xp.amount.placeholder")
         );
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminAddXpSourceInput.PlaceholderText",
            AmigoText.text("ui.settings.admin.add_xp.source.placeholder")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminAddXpButton.Text", AmigoText.text("ui.settings.admin.add_xp"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatAttrInput.PlaceholderText",
            AmigoText.text("ui.settings.admin.stats.attr.placeholder")
         );
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatAmountInput.PlaceholderText",
            AmigoText.text("ui.settings.admin.stats.amount.placeholder")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsAddButton.Text", AmigoText.text("ui.settings.admin.stats.add"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminStatsResetButton.Text", AmigoText.text("ui.settings.admin.stats.reset"));
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsReloadButton.Text", AmigoText.text("ui.settings.admin.rewards.reload")
         );
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsDefaultsButton.Text",
            AmigoText.text("ui.settings.admin.rewards.defaults")
         );
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsResetPointsInput.PlaceholderText",
            AmigoText.text("ui.settings.admin.rewards.reset_points.placeholder")
         );
         cmd.set(
            "#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminRewardsSetResetButton.Text",
            AmigoText.text("ui.settings.admin.rewards.set_reset")
         );
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordLvlUpButton.Text", AmigoText.text("ui.settings.admin.sword.up"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminSwordLvlDownButton.Text", AmigoText.text("ui.settings.admin.sword.down"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsAdminHitkillButton.Text", AmigoText.text("ui.settings.admin.sword.hitkill"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsApplyButton.Text", AmigoText.text("ui.settings.footer.apply"));
         cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage #SettingsCloseButton.Text", AmigoText.text("ui.settings.footer.close"));
         cmd.set("#StatsPageContainer #StatsPage #ResetStatsButton.Text", AmigoText.text("ui.stats.reset_all"));
         cmd.set("#StatsPageContainer #StatsPage #CancelButton.Text", AmigoText.text("ui.stats.cancel"));
         cmd.set("#StatsPageContainer #StatsPage #SaveButton.Text", AmigoText.text("ui.stats.save"));
         applyStatCardText(cmd, "Health", "ui.stats.card.health");
         applyStatCardText(cmd, "Stamina", "ui.stats.card.stamina");
         applyStatCardText(cmd, "StaminaRegenDelay", "ui.stats.card.stamina_regen");
         applyStatCardText(cmd, "StaminaConsumption", "ui.stats.card.stamina_consumption");
         applyStatCardText(cmd, "Defense", "ui.stats.card.defense");
         applyStatCardText(cmd, "Damage", "ui.stats.card.damage");
         applyStatCardText(cmd, "CriticalDamage", "ui.stats.card.critical_damage");
         applyStatCardText(cmd, "Mana", "ui.stats.card.mana");
         applyStatCardText(cmd, "Ammo", "ui.stats.card.ammo");
         applyStatCardText(cmd, "Oxygen", "ui.stats.card.oxygen");
         applyStatCardText(cmd, "Mining", "ui.stats.card.mining");
         applyStatCardText(cmd, "Woodcutting", "ui.stats.card.woodcutting");
         cmd.set("#RewardsPageContainer #RewardsPage #RewardsHeaderLevel.Text", AmigoText.text("ui.rewards.header.level"));
         cmd.set("#RewardsPageContainer #RewardsPage #RewardsHeaderItems.Text", AmigoText.text("ui.rewards.header.rewards"));
         cmd.set("#RewardsPageContainer #RewardsPage #RewardsHeaderAdditional.Text", AmigoText.text("ui.rewards.header.additional"));
         cmd.set("#RewardsPageContainer #RewardsPage #RewardsHeaderStatus.Text", AmigoText.text("ui.rewards.header.status"));
         cmd.set("#RewardsPageContainer #RewardsPage #ClaimAllButton.Text", AmigoText.text("ui.rewards.claim_all"));
      }
   }

   private static void applyStatCardText(UICommandBuilder cmd, String prefix, String titleKey) {
      String selectorPrefix = "#StatsPageContainer #StatsPage #" + prefix;
      cmd.set(selectorPrefix + "Allocation.Text", AmigoText.text(titleKey));
      cmd.set(selectorPrefix + "Points.Text", AmigoText.text("ui.stats.card.points"));
      cmd.set(selectorPrefix + "Value.Text", AmigoText.text("ui.stats.card.value"));
   }
}
