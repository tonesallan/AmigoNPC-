package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.List;
import java.util.UUID;

final class AmigoLvlGuiSettingsLanguageSupport {
   private static final List<String> LOCALE_ORDER = List.of(
      "pt_br", "en_us", "es_es", "ru_ru", "fr_fr", "de_de", "pt_pt", "it_it", "pl_pl", "tr_tr", "uk_ua", "ja_jp", "hu_hu", "vi_vn", "cs_cz"
   );

   private AmigoLvlGuiSettingsLanguageSupport() {
   }

   static AmigoLvlGuiSettingsImmediateActionResult handle(UUID ownerId, AmigoLvlGuiEventData data, UICommandBuilder cmd, String action) {
      if (!"settings_language_prev".equals(action) && !"settings_language_next".equals(action)) {
         return new AmigoLvlGuiSettingsImmediateActionResult(false, null);
      }

      String currentLocale = AmigoText.localeForPlayer(ownerId);
      String selectedLocale = shiftLocale(currentLocale, "settings_language_prev".equals(action) ? -1 : 1);
      if (selectedLocale.equals(currentLocale)) {
         return new AmigoLvlGuiSettingsImmediateActionResult(true, null);
      }

      AmigoPersistence.saveLanguage(ownerId, selectedLocale);
      AmigoText.clearCache();
      AmigoText.syncScopedLocaleFromPlayer(ownerId);
      LevelProgressHudService.getShared().requestImmediate(ownerId);
      AmigoLvlGuiMessageSupport.setSettingsFeedback(cmd, AmigoText.text(selectedLocale, "ui.settings.feedback.language.applied"));
      return new AmigoLvlGuiSettingsImmediateActionResult(true, null);
   }

   static String displayName(String locale) {
      return switch (AmigoText.resolveLocale(locale)) {
         case "pt_br" -> "Portugues (Brasil)";
         case "en_us" -> "English (US)";
         case "es_es" -> "Espanol";
         case "ru_ru" -> "Russkiy";
         case "fr_fr" -> "Francais";
         case "de_de" -> "Deutsch";
         case "pt_pt" -> "Portugues (Portugal)";
         case "it_it" -> "Italiano";
         case "pl_pl" -> "Polski";
         case "tr_tr" -> "Turkce";
         case "uk_ua" -> "Ukrainska";
         case "ja_jp" -> "Nihongo";
         case "hu_hu" -> "Magyar";
         case "vi_vn" -> "Tieng Viet";
         case "cs_cz" -> "Cestina";
         default -> AmigoText.resolveLocale(locale);
      };
   }

   private static String shiftLocale(String currentLocale, int delta) {
      String normalizedCurrent = AmigoText.resolveLocale(currentLocale);
      int currentIndex = LOCALE_ORDER.indexOf(normalizedCurrent);
      if (currentIndex < 0) {
         currentIndex = 0;
      }

      int size = LOCALE_ORDER.size();
      int shiftedIndex = (currentIndex + delta) % size;
      if (shiftedIndex < 0) {
         shiftedIndex += size;
      }

      return LOCALE_ORDER.get(shiftedIndex);
   }
}
