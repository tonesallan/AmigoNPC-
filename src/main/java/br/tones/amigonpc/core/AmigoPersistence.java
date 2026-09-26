package br.tones.amigonpc.core;

import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.bson.BsonDocument;

public final class AmigoPersistence {
   private static final int FORMAT_VERSION = 4;
   private static final short BACKPACK_CAPACITY = 45;
   private static final String KEY_MODEL_ID = "modelId";
   private static final String KEY_MODEL_SCALE = "modelScale";
   private static final String KEY_NPC_NAME = "npcName";
   private static final String KEY_LANGUAGE = "language";
   private static final String KEY_NPC_TYPE = "npcType";
   private static final String KEY_SWORD_LEVEL = "swordLevel";
   private static final String KEY_EQUIPPED_WEAPON_ID = "equippedWeaponId";
   private static final String KEY_DEFENDER_ENABLED = "defenderEnabled";
   private static final String KEY_AUTOLOOT_ENABLED = "autoLootEnabled";
   private static final String KEY_HUD_ENABLED = "hudEnabled";
   private static final String KEY_GODMODE = "godMode";
   private static final String KEY_PVP_ENABLED_GLOBAL = "pvpEnabled";
   private static final String KEY_TOTAL_XP = "totalXp";
   private static final String KEY_BASE_HP = "baseHp";
   private static final String KEY_BASE_DEF = "baseDef";
   private static final String KEY_NPC_STATS = "npcStats";
   private static final String KEY_AMIGO_REWARDS = "amigoRewards";
   private static final String KEY_WARDROBE_STATE = "wardrobeState";
   private static final String COSMETICS_CONFIG_FILE_NAME = "AmigoNPC Cosmetics Configuration.json";

   private static Path baseDir() {
      Path root = Constants.UNIVERSE_PATH;
      return root.resolve("amigonpc").resolve("players");
   }

   private static Path serverFile() {
      Path root = Constants.UNIVERSE_PATH;
      return root.resolve("amigonpc").resolve("server.json");
   }

   public static Path fileFor(UUID ownerId) {
      return baseDir().resolve(ownerId.toString() + ".json");
   }

   public static Path cosmeticsFile() {
      return baseDir().resolve("AmigoNPC Cosmetics Configuration.json");
   }

   public static SimpleItemContainer loadBackpack(UUID ownerId) {
      return ownerId == null ? new SimpleItemContainer((short)45) : AmigoPersistenceBackpackSupport.loadBackpack(fileFor(ownerId), (short)45);
   }

   public static String loadModelId(UUID ownerId) {
      return ownerId == null ? null : AmigoPersistenceProfileSupport.loadString(fileFor(ownerId), "modelId");
   }

   public static double loadModelScale(UUID ownerId) {
      return ownerId == null ? 1.0 : AmigoPersistenceProfileSupport.loadDouble(fileFor(ownerId), "modelScale", 1.0);
   }

   public static void saveModel(UUID ownerId, String modelId, double scale) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProfileSupport.saveModel(fileFor(ownerId), 4, "modelId", "modelScale", modelId, scale);
         } catch (IOException var5) {
         } catch (Throwable var6) {
         }
      }
   }

   public static String loadCustomName(UUID ownerId) {
      return ownerId == null ? null : AmigoPersistenceProfileSupport.loadString(fileFor(ownerId), "npcName");
   }

   public static void saveCustomName(UUID ownerId, String customName) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProfileSupport.saveOptionalString(fileFor(ownerId), 4, "npcName", customName);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static String loadLanguage(UUID ownerId) {
      return ownerId == null ? null : AmigoPersistenceProfileSupport.loadString(fileFor(ownerId), "language");
   }

   public static void saveLanguage(UUID ownerId, String language) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProfileSupport.saveOptionalString(fileFor(ownerId), 4, "language", language);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static String loadNpcType(UUID ownerId) {
      return ownerId == null ? null : AmigoPersistenceProfileSupport.loadString(fileFor(ownerId), "npcType");
   }

   public static void saveNpcType(UUID ownerId, String npcType) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProfileSupport.saveOptionalString(fileFor(ownerId), 4, "npcType", npcType);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static int loadSwordLevel(UUID ownerId) {
      return ownerId == null ? 1 : AmigoPersistenceProfileSupport.loadPositiveInt(fileFor(ownerId), "swordLevel", 1);
   }

   public static String loadEquippedWeaponId(UUID ownerId) {
      return ownerId == null ? null : AmigoPersistenceProfileSupport.loadString(fileFor(ownerId), "equippedWeaponId");
   }

   public static void saveSwordState(UUID ownerId, int swordLevel, String equippedWeaponId) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProfileSupport.saveSwordState(fileFor(ownerId), 4, "swordLevel", "equippedWeaponId", swordLevel, equippedWeaponId);
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }
      }
   }

   public static boolean loadDefenderEnabled(UUID ownerId) {
      return ownerId == null ? false : AmigoPersistenceFlagSupport.loadFlag(fileFor(ownerId), "defenderEnabled", false);
   }

   public static void saveDefenderEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            AmigoPersistenceFlagSupport.saveFlag(fileFor(ownerId), 4, "defenderEnabled", enabled);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static boolean loadAutoLootEnabled(UUID ownerId) {
      return ownerId == null ? true : AmigoPersistenceFlagSupport.loadFlag(fileFor(ownerId), "autoLootEnabled", true);
   }

   public static void saveAutoLootEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            AmigoPersistenceFlagSupport.saveFlag(fileFor(ownerId), 4, "autoLootEnabled", enabled);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static boolean loadHudEnabled(UUID ownerId) {
      return ownerId == null ? true : AmigoPersistenceFlagSupport.loadFlag(fileFor(ownerId), "hudEnabled", true);
   }

   public static void saveHudEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            AmigoPersistenceFlagSupport.saveFlag(fileFor(ownerId), 4, "hudEnabled", enabled);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static boolean loadGodMode(UUID ownerId) {
      return ownerId == null ? false : AmigoPersistenceFlagSupport.loadFlag(fileFor(ownerId), "godMode", false);
   }

   public static void saveGodMode(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            AmigoPersistenceFlagSupport.saveFlag(fileFor(ownerId), 4, "godMode", enabled);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static boolean loadPvpEnabledGlobal() {
      return AmigoPersistenceFlagSupport.loadFlag(serverFile(), "pvpEnabled", false);
   }

   public static void savePvpEnabledGlobal(boolean enabled) {
      try {
         AmigoPersistenceFlagSupport.saveFlag(serverFile(), 4, "pvpEnabled", enabled);
      } catch (IOException var2) {
      } catch (Throwable var3) {
      }
   }

   public static long loadTotalXp(UUID ownerId) {
      return ownerId == null ? 0L : AmigoPersistenceProgressionSupport.loadLong(fileFor(ownerId), "totalXp", 0L);
   }

   public static long loadBaseHp(UUID ownerId) {
      return ownerId == null ? -1L : AmigoPersistenceProgressionSupport.loadLong(fileFor(ownerId), "baseHp", -1L);
   }

   public static long loadBaseDef(UUID ownerId) {
      return ownerId == null ? -1L : AmigoPersistenceProgressionSupport.loadLong(fileFor(ownerId), "baseDef", -1L);
   }

   public static void saveNpcProgress(UUID ownerId, long totalXp, long baseHp, long baseDef) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProgressionSupport.saveNpcProgress(fileFor(ownerId), 4, "totalXp", "baseHp", "baseDef", totalXp, baseHp, baseDef);
         } catch (IOException var8) {
         } catch (Throwable var9) {
         }
      }
   }

   public static void saveTotalXp(UUID ownerId, long totalXp) {
      saveNpcProgress(ownerId, Math.max(0L, totalXp), -1L, -1L);
   }

   public static void saveBaseHp(UUID ownerId, long baseHp) {
      saveNpcProgress(ownerId, -1L, baseHp, -1L);
   }

   public static void saveBaseDef(UUID ownerId, long baseDef) {
      saveNpcProgress(ownerId, -1L, -1L, baseDef);
   }

   public static BsonDocument loadNpcStats(UUID ownerId) {
      return ownerId == null ? new BsonDocument() : AmigoPersistenceProgressionSupport.loadDocumentSection(fileFor(ownerId), "npcStats");
   }

   public static void saveNpcStats(UUID ownerId, BsonDocument npcStatsDoc) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProgressionSupport.saveDocumentSection(fileFor(ownerId), 4, "npcStats", npcStatsDoc);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static BsonDocument loadAmigoRewards(UUID ownerId) {
      return ownerId == null ? new BsonDocument() : AmigoPersistenceProgressionSupport.loadDocumentSection(fileFor(ownerId), "amigoRewards");
   }

   public static void saveAmigoRewards(UUID ownerId, BsonDocument rewardsDoc) {
      if (ownerId != null) {
         try {
            AmigoPersistenceProgressionSupport.saveDocumentSection(fileFor(ownerId), 4, "amigoRewards", rewardsDoc);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static void saveBaseHpDef(UUID ownerId, long baseHp, long baseDef) {
      saveNpcProgress(ownerId, -1L, baseHp, baseDef);
   }

   public static BsonDocument loadWardrobeState(UUID ownerId) {
      return ownerId == null ? new BsonDocument() : AmigoPersistenceWardrobeSupport.loadWardrobeState(cosmeticsFile(), ownerId, "wardrobeState");
   }

   public static void saveWardrobeState(UUID ownerId, BsonDocument wardrobeDoc) {
      if (ownerId != null) {
         try {
            AmigoPersistenceWardrobeSupport.saveWardrobeState(cosmeticsFile(), 4, ownerId, wardrobeDoc, "wardrobeState");
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   public static void saveBackpack(UUID ownerId, SimpleItemContainer backpack) {
      if (ownerId != null && backpack != null) {
         try {
            AmigoPersistenceBackpackSupport.saveBackpack(fileFor(ownerId), 4, backpack);
         } catch (IOException var3) {
         } catch (Throwable var4) {
         }
      }
   }

   private AmigoPersistence() {
   }
}
