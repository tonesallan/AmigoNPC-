package br.tones.amigonpc.core.i18n;

import br.tones.amigonpc.core.AmigoPersistence;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class AmigoText {
   private static final String DEFAULT_LOCALE = "pt_br";
   private static final String RESOURCE_PREFIX = "amigonpc/lang/";
   private static final String CONFIG_KEY_LANGUAGE = "language";
   private static final ConcurrentMap<String, Properties> CACHE = new ConcurrentHashMap<>();
   private static final ThreadLocal<Deque<String>> SCOPED_LOCALES = ThreadLocal.withInitial(ArrayDeque::new);
   private static final Set<String> SUPPORTED_LOCALES = Set.of(
      "cs_cz", "de_de", "en_us", "es_es", "fr_fr", "hu_hu", "it_it", "ja_jp", "pl_pl", "pt_br", "pt_pt", "ru_ru", "tr_tr", "uk_ua", "vi_vn"
   );
   private static final Map<String, String> LOCALE_ALIASES = Map.ofEntries(
      Map.entry("cs", "cs_cz"),
      Map.entry("de", "de_de"),
      Map.entry("en", "en_us"),
      Map.entry("es", "es_es"),
      Map.entry("fr", "fr_fr"),
      Map.entry("hu", "hu_hu"),
      Map.entry("it", "it_it"),
      Map.entry("ja", "ja_jp"),
      Map.entry("pl", "pl_pl"),
      Map.entry("pt", "pt_br"),
      Map.entry("pt_br", "pt_br"),
      Map.entry("pt_pt", "pt_pt"),
      Map.entry("ru", "ru_ru"),
      Map.entry("tr", "tr_tr"),
      Map.entry("uk", "uk_ua"),
      Map.entry("vi", "vi_vn")
   );
   private static final Map<String, String> EXTRA_DEFAULTS = Map.ofEntries(
      Map.entry("ui.settings.combat.protect_owner", "Foco em quem me ataca (combate em dupla)"),
      Map.entry("ui.settings.combat.weakest", "Foco em inimigos frágeis"),
      Map.entry("ui.settings.toggle.auto_weapon", "Troca automática de arma: {0}"),
      Map.entry("ui.settings.toggle.interrupt", "Interromper ataques inimigos: {0}"),
      Map.entry("ui.settings.feedback.combat_protect_pending", "Modo de combate: proteger o jogador (pendente)"),
      Map.entry("ui.settings.feedback.combat_weakest_pending", "Modo de combate: inimigo com menos vida (pendente)"),
      Map.entry("ui.settings.feedback.auto_weapon_pending", "Troca automática de arma: {0} (pendente)"),
      Map.entry("ui.settings.feedback.interrupt_pending", "Interromper ataques inimigos: {0} (pendente)")
   );
   private static volatile String currentLocale = "pt_br";

   private AmigoText() {
   }

   public static String text(String key) {
      return text(activeLocale(), key);
   }

   public static String text(String locale, String key) {
      if (key != null && !key.isBlank()) {
         String normalizedLocale = resolveLocale(locale);
         Properties props = CACHE.computeIfAbsent(normalizedLocale, AmigoText::loadBundle);
         String value = props.getProperty(key);
         if (value != null) {
            return value;
         }

         if (!"pt_br".equals(normalizedLocale)) {
            Properties fallback = CACHE.computeIfAbsent("pt_br", AmigoText::loadBundle);
            value = fallback.getProperty(key);
            if (value != null) {
               return value;
            }
         }

         String extra = EXTRA_DEFAULTS.get(key);
         return extra != null ? extra : key;
      } else {
         return "";
      }
   }

   public static String format(String key, Object... args) {
      String pattern = text(key);

      try {
         return new MessageFormat(pattern, Locale.ROOT).format(args == null ? new Object[0] : args);
      } catch (Throwable ignored) {
         return pattern;
      }
   }

   public static String formatLocale(String locale, String key, Object... args) {
      String pattern = text(locale, key);

      try {
         return new MessageFormat(pattern, Locale.ROOT).format(args == null ? new Object[0] : args);
      } catch (Throwable ignored) {
         return pattern;
      }
   }

   public static String textPlayer(UUID ownerId, String key) {
      return text(localeForPlayer(ownerId), key);
   }

   public static String formatPlayer(UUID ownerId, String key, Object... args) {
      return formatLocale(localeForPlayer(ownerId), key, args);
   }

   public static String onOff(boolean enabled) {
      return text(enabled ? "common.on" : "common.off");
   }

   public static String coloredOnOff(boolean enabled) {
      return (enabled ? "§a" : "§c") + onOff(enabled);
   }

   public static String pick(String prefix) {
      if (prefix != null && !prefix.isBlank()) {
         String normalizedLocale = activeLocale();
         ArrayList<String> values = collectIndexedValues(normalizedLocale, prefix);
         if (values.isEmpty() && !"pt_br".equals(normalizedLocale)) {
            values = collectIndexedValues("pt_br", prefix);
         }

         if (values.isEmpty()) {
            String single = findValue(normalizedLocale, prefix);
            if (single == null && !"pt_br".equals(normalizedLocale)) {
               single = findValue("pt_br", prefix);
            }

            return single != null ? single : "";
         } else {
            return values.get(ThreadLocalRandom.current().nextInt(values.size()));
         }
      } else {
         return "";
      }
   }

   public static void setCurrentLocale(String locale) {
      currentLocale = resolveLocale(locale);
   }

   public static String getCurrentLocale() {
      return currentLocale;
   }

   public static String normalizeLocaleId(String locale) {
      return locale != null && !locale.isBlank() ? locale.trim().toLowerCase(Locale.ROOT).replace('-', '_') : "pt_br";
   }

   public static boolean isSupportedLocale(String locale) {
      return SUPPORTED_LOCALES.contains(normalizeLocaleId(locale));
   }

   public static String resolveLocale(String locale) {
      String normalized = normalizeLocaleId(locale);
      if (SUPPORTED_LOCALES.contains(normalized)) {
         return normalized;
      }

      String directAlias = LOCALE_ALIASES.get(normalized);
      if (directAlias != null) {
         return directAlias;
      }

      int sep = normalized.indexOf(95);
      if (sep > 0) {
         String baseLanguage = normalized.substring(0, sep);
         String baseAlias = LOCALE_ALIASES.get(baseLanguage);
         if (baseAlias != null) {
            return baseAlias;
         }
      }

      return "pt_br";
   }

   public static String localeForPlayer(UUID ownerId) {
      if (ownerId == null) {
         return currentLocale;
      }

      String saved = null;

      try {
         saved = AmigoPersistence.loadLanguage(ownerId);
      } catch (Throwable var3) {
      }

      return saved != null && !saved.isBlank() ? resolveLocale(saved) : currentLocale;
   }

   public static String initializePlayerLocale(UUID ownerId, PlayerRef playerRef) {
      if (ownerId == null) {
         return currentLocale;
      }

      String saved = null;

      try {
         saved = AmigoPersistence.loadLanguage(ownerId);
      } catch (Throwable var8) {
      }

      if (saved != null && !saved.isBlank()) {
         return resolveLocale(saved);
      }

      String clientLocale = null;

      try {
         clientLocale = playerRef != null ? playerRef.getLanguage() : null;
      } catch (Throwable var7) {
      }

      if (clientLocale != null && !clientLocale.isBlank()) {
         String resolvedLocale = resolveLocale(clientLocale);

         try {
            AmigoPersistence.saveLanguage(ownerId, resolvedLocale);
         } catch (Throwable var6) {
         }

         return resolvedLocale;
      } else {
         return currentLocale;
      }
   }

   public static void clearCache() {
      CACHE.clear();
   }

   public static void reloadConfiguredLocale() {
      clearCache();
      setCurrentLocale(readConfiguredLocale());
   }

   public static void pushLocale(String locale) {
      SCOPED_LOCALES.get().push(resolveLocale(locale));
   }

   public static void pushPlayerLocale(UUID ownerId) {
      pushLocale(localeForPlayer(ownerId));
   }

   public static void popLocale() {
      Deque<String> stack = SCOPED_LOCALES.get();
      if (!stack.isEmpty()) {
         stack.pop();
      }

      if (stack.isEmpty()) {
         SCOPED_LOCALES.remove();
      }
   }

   public static void replaceScopedLocale(String locale) {
      Deque<String> stack = SCOPED_LOCALES.get();
      String resolvedLocale = resolveLocale(locale);
      if (stack.isEmpty()) {
         stack.push(resolvedLocale);
      } else {
         stack.pop();
         stack.push(resolvedLocale);
      }
   }

   public static void syncScopedLocaleFromPlayer(UUID ownerId) {
      replaceScopedLocale(localeForPlayer(ownerId));
   }

   public static void withLocale(String locale, Runnable action) {
      pushLocale(locale);

      try {
         action.run();
      } finally {
         popLocale();
      }
   }

   public static <T> T withLocale(String locale, Supplier<T> supplier) {
      pushLocale(locale);

      try {
         return supplier.get();
      } finally {
         popLocale();
      }
   }

   public static void withPlayerLocale(UUID ownerId, Runnable action) {
      withLocale(localeForPlayer(ownerId), action);
   }

   public static <T> T withPlayerLocale(UUID ownerId, Supplier<T> supplier) {
      return withLocale(localeForPlayer(ownerId), supplier);
   }

   private static String activeLocale() {
      Deque<String> stack = SCOPED_LOCALES.get();
      return stack.isEmpty() ? currentLocale : stack.peek();
   }

   private static String readConfiguredLocale() {
      try {
         Path p = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("config.properties");
         if (!Files.exists(p)) {
            return "pt_br";
         }

         Properties props = new Properties();

         try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
         }

         return resolveLocale(props.getProperty("language", "pt_br"));
      } catch (Throwable ignored) {
         return "pt_br";
      }
   }

   private static ArrayList<String> collectIndexedValues(String locale, String prefix) {
      ArrayList<String> values = new ArrayList<>();
      int i = 1;

      while (true) {
         String value = findValue(locale, prefix + "." + i);
         if (value == null) {
            return values;
         }

         values.add(value);
         i++;
      }
   }

   private static String findValue(String locale, String key) {
      Properties props = CACHE.computeIfAbsent(resolveLocale(locale), AmigoText::loadBundle);
      String value = props.getProperty(key);
      return value != null && !value.isBlank() ? value : null;
   }

   private static Properties loadBundle(String locale) {
      Properties props = new Properties();
      String resourcePath = "amigonpc/lang/" + resolveLocale(locale) + ".properties";

      try (InputStream in = AmigoText.class.getClassLoader().getResourceAsStream(resourcePath)) {
         if (in == null) {
            return props;
         }

         try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
         }
      } catch (Throwable var11) {
      }

      return props;
   }
}
