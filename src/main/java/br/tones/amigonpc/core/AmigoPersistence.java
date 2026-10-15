package br.tones.amigonpc.core;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.util.BsonUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;

public final class AmigoPersistence {
   private static final int FORMAT_VERSION = 4;
   private static final short BACKPACK_CAPACITY = 45;
   private static final String KEY_MODEL_ID = "modelId";
   private static final String KEY_MODEL_SCALE = "modelScale";
   private static final String KEY_NPC_TYPE = "npcType";
   private static final String KEY_SWORD_LEVEL = "swordLevel";
   private static final String KEY_EQUIPPED_WEAPON_ID = "equippedWeaponId";
   private static final String KEY_DEFENDER_ENABLED = "defenderEnabled";
   private static final String KEY_AUTOLOOT_ENABLED = "autoLootEnabled";
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
      if (ownerId == null) {
         return new SimpleItemContainer((short)45);
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return new SimpleItemContainer((short)45);
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return new SimpleItemContainer((short)45);
               } else if (!doc.containsKey("backpack")) {
                  return new SimpleItemContainer((short)45);
               } else {
                  BsonValue val = doc.get("backpack");
                  return val == null ? new SimpleItemContainer((short)45) : (SimpleItemContainer)SimpleItemContainer.CODEC.decode(val, new ExtraInfo());
               }
            } catch (Throwable var4) {
               return new SimpleItemContainer((short)45);
            }
         }
      }
   }

   public static String loadModelId(UUID ownerId) {
      if (ownerId == null) {
         return null;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return null;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return null;
               } else if (!doc.containsKey("modelId")) {
                  return null;
               } else {
                  BsonValue v = doc.get("modelId");
                  return v != null && v.isString() ? v.asString().getValue() : null;
               }
            } catch (Throwable var4) {
               return null;
            }
         }
      }
   }

   public static double loadModelScale(UUID ownerId) {
      if (ownerId == null) {
         return (double)1.0F;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return (double)1.0F;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return (double)1.0F;
               } else if (!doc.containsKey("modelScale")) {
                  return (double)1.0F;
               } else {
                  BsonValue v = doc.get("modelScale");
                  if (v == null) {
                     return (double)1.0F;
                  } else if (v.isDouble()) {
                     return v.asDouble().getValue();
                  } else {
                     return v.isInt32() ? (double)v.asInt32().getValue() : (double)1.0F;
                  }
               }
            } catch (Throwable var4) {
               return (double)1.0F;
            }
         }
      }
   }

   public static void saveModel(UUID ownerId, String modelId, double scale) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            if (modelId != null && !modelId.isBlank()) {
               doc.put("modelId", new BsonString(modelId));
               doc.put("modelScale", new BsonDouble(scale));
            } else {
               doc.remove("modelId");
               doc.remove("modelScale");
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var6) {
         } catch (Throwable var7) {
         }

      }
   }

   public static String loadNpcType(UUID ownerId) {
      if (ownerId == null) {
         return null;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return null;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return null;
               } else if (!doc.containsKey("npcType")) {
                  return null;
               } else {
                  BsonValue v = doc.get("npcType");
                  return v != null && v.isString() ? v.asString().getValue() : null;
               }
            } catch (Throwable var4) {
               return null;
            }
         }
      }
   }

   public static void saveNpcType(UUID ownerId, String npcType) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            if (npcType != null && !npcType.isBlank()) {
               doc.put("npcType", new BsonString(npcType));
            } else {
               doc.remove("npcType");
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   public static int loadSwordLevel(UUID ownerId) {
      if (ownerId == null) {
         return 1;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return 1;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return 1;
               } else if (!doc.containsKey("swordLevel")) {
                  return 1;
               } else {
                  BsonValue v = doc.get("swordLevel");
                  int lvl = 1;
                  if (v != null) {
                     if (v.isInt32()) {
                        lvl = v.asInt32().getValue();
                     } else if (v.isDouble()) {
                        lvl = (int)v.asDouble().getValue();
                     }
                  }

                  return Math.max(1, lvl);
               }
            } catch (Throwable var5) {
               return 1;
            }
         }
      }
   }

   public static String loadEquippedWeaponId(UUID ownerId) {
      if (ownerId == null) {
         return null;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return null;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return null;
               } else if (!doc.containsKey("equippedWeaponId")) {
                  return null;
               } else {
                  BsonValue v = doc.get("equippedWeaponId");
                  return v != null && v.isString() ? v.asString().getValue() : null;
               }
            } catch (Throwable var4) {
               return null;
            }
         }
      }
   }

   public static void saveSwordState(UUID ownerId, int swordLevel, String equippedWeaponId) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            doc.put("swordLevel", new BsonInt32(Math.max(1, swordLevel)));
            if (equippedWeaponId != null && !equippedWeaponId.isBlank()) {
               doc.put("equippedWeaponId", new BsonString(equippedWeaponId));
            } else {
               doc.remove("equippedWeaponId");
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var5) {
         } catch (Throwable var6) {
         }

      }
   }

   public static boolean loadDefenderEnabled(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return false;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return false;
               } else if (!doc.containsKey("defenderEnabled")) {
                  return false;
               } else {
                  BsonValue v = doc.get("defenderEnabled");
                  if (v == null) {
                     return false;
                  } else if (v.isBoolean()) {
                     return v.asBoolean().getValue();
                  } else if (v.isInt32()) {
                     return v.asInt32().getValue() != 0;
                  } else if (v.isDouble()) {
                     return v.asDouble().getValue() != (double)0.0F;
                  } else if (v.isString()) {
                     String s = v.asString().getValue();
                     if (s == null) {
                        return false;
                     } else {
                        s = s.trim().toLowerCase();
                        return s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim") || s.equals("yes");
                     }
                  } else {
                     return false;
                  }
               }
            } catch (Throwable var5) {
               return false;
            }
         }
      }
   }

   public static void saveDefenderEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            doc.put("defenderEnabled", new BsonInt32(enabled ? 1 : 0));
            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   public static boolean loadAutoLootEnabled(UUID ownerId) {
      if (ownerId == null) {
         return true;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return true;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return true;
               } else if (!doc.containsKey("autoLootEnabled")) {
                  return true;
               } else {
                  BsonValue v = doc.get("autoLootEnabled");
                  if (v == null) {
                     return true;
                  } else if (v.isBoolean()) {
                     return v.asBoolean().getValue();
                  } else if (v.isInt32()) {
                     return v.asInt32().getValue() != 0;
                  } else if (v.isDouble()) {
                     return v.asDouble().getValue() != (double)0.0F;
                  } else if (v.isString()) {
                     String s = v.asString().getValue();
                     if (s == null) {
                        return true;
                     } else {
                        s = s.trim().toLowerCase();
                        return s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim") || s.equals("yes");
                     }
                  } else {
                     return true;
                  }
               }
            } catch (Throwable var5) {
               return true;
            }
         }
      }
   }

   public static void saveAutoLootEnabled(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            doc.put("autoLootEnabled", new BsonInt32(enabled ? 1 : 0));
            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   public static boolean loadGodMode(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return false;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return false;
               } else if (!doc.containsKey("godMode")) {
                  return false;
               } else {
                  BsonValue v = doc.get("godMode");
                  if (v == null) {
                     return false;
                  } else if (v.isBoolean()) {
                     return v.asBoolean().getValue();
                  } else if (v.isInt32()) {
                     return v.asInt32().getValue() != 0;
                  } else if (v.isDouble()) {
                     return v.asDouble().getValue() != (double)0.0F;
                  } else if (v.isString()) {
                     String s = v.asString().getValue();
                     if (s == null) {
                        return false;
                     } else {
                        s = s.trim().toLowerCase();
                        return s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim") || s.equals("yes");
                     }
                  } else {
                     return false;
                  }
               }
            } catch (Throwable var5) {
               return false;
            }
         }
      }
   }

   public static void saveGodMode(UUID ownerId, boolean enabled) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            doc.put("godMode", new BsonInt32(enabled ? 1 : 0));
            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   public static boolean loadPvpEnabledGlobal() {
      try {
         Path file = serverFile();
         if (!Files.exists(file, new LinkOption[0])) {
            return false;
         } else {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) {
               return false;
            } else if (!doc.containsKey("pvpEnabled")) {
               return false;
            } else {
               BsonValue v = doc.get("pvpEnabled");
               if (v == null) {
                  return false;
               } else if (v.isBoolean()) {
                  return v.asBoolean().getValue();
               } else if (v.isInt32()) {
                  return v.asInt32().getValue() != 0;
               } else if (v.isDouble()) {
                  return v.asDouble().getValue() != (double)0.0F;
               } else if (v.isString()) {
                  String s = v.asString().getValue();
                  if (s == null) {
                     return false;
                  } else {
                     s = s.trim().toLowerCase();
                     return s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim") || s.equals("yes");
                  }
               } else {
                  return false;
               }
            }
         }
      } catch (Throwable var4) {
         return false;
      }
   }

   public static void savePvpEnabledGlobal(boolean enabled) {
      try {
         Path file = serverFile();
         Files.createDirectories(file.getParent());
         BsonDocument doc = BsonUtil.readDocumentNow(file);
         if (doc == null) {
            doc = new BsonDocument();
         }

         doc.put("formatVersion", new BsonInt32(4));
         doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
         doc.put("pvpEnabled", new BsonInt32(enabled ? 1 : 0));
         BsonUtil.writeDocument(file, doc, true).join();
      } catch (IOException var3) {
      } catch (Throwable var4) {
      }

   }

   public static long loadTotalXp(UUID ownerId) {
      if (ownerId == null) {
         return 0L;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return 0L;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return 0L;
               } else if (!doc.containsKey("totalXp")) {
                  return 0L;
               } else {
                  BsonValue v = doc.get("totalXp");
                  if (v == null) {
                     return 0L;
                  } else if (v.isInt64()) {
                     return v.asInt64().getValue();
                  } else if (v.isInt32()) {
                     return (long)v.asInt32().getValue();
                  } else if (v.isDouble()) {
                     return (long)v.asDouble().getValue();
                  } else {
                     if (v.isString()) {
                        try {
                           return Long.parseLong(v.asString().getValue());
                        } catch (Throwable var5) {
                        }
                     }

                     return 0L;
                  }
               }
            } catch (Throwable var6) {
               return 0L;
            }
         }
      }
   }

   public static long loadBaseHp(UUID ownerId) {
      if (ownerId == null) {
         return -1L;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return -1L;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return -1L;
               } else if (!doc.containsKey("baseHp")) {
                  return -1L;
               } else {
                  BsonValue v = doc.get("baseHp");
                  if (v == null) {
                     return -1L;
                  } else if (v.isInt64()) {
                     return v.asInt64().getValue();
                  } else if (v.isInt32()) {
                     return (long)v.asInt32().getValue();
                  } else if (v.isDouble()) {
                     return (long)v.asDouble().getValue();
                  } else {
                     if (v.isString()) {
                        try {
                           return Long.parseLong(v.asString().getValue());
                        } catch (Throwable var5) {
                        }
                     }

                     return -1L;
                  }
               }
            } catch (Throwable var6) {
               return -1L;
            }
         }
      }
   }

   public static long loadBaseDef(UUID ownerId) {
      if (ownerId == null) {
         return -1L;
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return -1L;
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return -1L;
               } else if (!doc.containsKey("baseDef")) {
                  return -1L;
               } else {
                  BsonValue v = doc.get("baseDef");
                  if (v == null) {
                     return -1L;
                  } else if (v.isInt64()) {
                     return v.asInt64().getValue();
                  } else if (v.isInt32()) {
                     return (long)v.asInt32().getValue();
                  } else if (v.isDouble()) {
                     return (long)v.asDouble().getValue();
                  } else {
                     if (v.isString()) {
                        try {
                           return Long.parseLong(v.asString().getValue());
                        } catch (Throwable var5) {
                        }
                     }

                     return -1L;
                  }
               }
            } catch (Throwable var6) {
               return -1L;
            }
         }
      }
   }

   public static void saveNpcProgress(UUID ownerId, long totalXp, long baseHp, long baseDef) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            if (totalXp >= 0L) {
               try {
                  doc.put("totalXp", new BsonInt64(totalXp));
               } catch (Throwable var12) {
                  doc.put("totalXp", new BsonDouble((double)totalXp));
               }
            }

            if (baseHp >= 0L) {
               try {
                  doc.put("baseHp", new BsonInt64(baseHp));
               } catch (Throwable var11) {
                  doc.put("baseHp", new BsonDouble((double)baseHp));
               }
            }

            if (baseDef >= 0L) {
               try {
                  doc.put("baseDef", new BsonInt64(baseDef));
               } catch (Throwable var10) {
                  doc.put("baseDef", new BsonDouble((double)baseDef));
               }
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var13) {
         } catch (Throwable var14) {
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
      if (ownerId == null) {
         return new BsonDocument();
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return new BsonDocument();
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return new BsonDocument();
               } else if (!doc.containsKey("npcStats")) {
                  return new BsonDocument();
               } else {
                  BsonValue v = doc.get("npcStats");
                  return v != null && v.isDocument() ? v.asDocument() : new BsonDocument();
               }
            } catch (Throwable var4) {
               return new BsonDocument();
            }
         }
      }
   }

   public static void saveNpcStats(UUID ownerId, BsonDocument npcStatsDoc) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            if (npcStatsDoc != null && !npcStatsDoc.isEmpty()) {
               doc.put("npcStats", npcStatsDoc);
            } else {
               doc.remove("npcStats");
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   public static BsonDocument loadAmigoRewards(UUID ownerId) {
      if (ownerId == null) {
         return new BsonDocument();
      } else {
         Path file = fileFor(ownerId);
         if (!Files.exists(file, new LinkOption[0])) {
            return new BsonDocument();
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return new BsonDocument();
               } else if (!doc.containsKey("amigoRewards")) {
                  return new BsonDocument();
               } else {
                  BsonValue v = doc.get("amigoRewards");
                  return v != null && v.isDocument() ? v.asDocument() : new BsonDocument();
               }
            } catch (Throwable var4) {
               return new BsonDocument();
            }
         }
      }
   }

   public static void saveAmigoRewards(UUID ownerId, BsonDocument rewardsDoc) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            if (rewardsDoc != null && !rewardsDoc.isEmpty()) {
               doc.put("amigoRewards", rewardsDoc);
            } else {
               doc.remove("amigoRewards");
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   public static void saveBaseHpDef(UUID ownerId, long baseHp, long baseDef) {
      saveNpcProgress(ownerId, -1L, baseHp, baseDef);
   }

   public static BsonDocument loadWardrobeState(UUID ownerId) {
      if (ownerId == null) {
         return new BsonDocument();
      } else {
         Path file = cosmeticsFile();
         if (!Files.exists(file, new LinkOption[0])) {
            return new BsonDocument();
         } else {
            try {
               BsonDocument doc = BsonUtil.readDocumentNow(file);
               if (doc == null) {
                  return new BsonDocument();
               } else {
                  BsonValue v = doc.get(ownerId.toString());
                  if (v != null && v.isDocument()) {
                     BsonDocument entry = v.asDocument();
                     BsonValue state = entry.get("wardrobeState");
                     if (state != null && state.isDocument()) {
                        return state.asDocument();
                     } else {
                        return looksLikeWardrobeStateDoc(entry) ? entry : new BsonDocument();
                     }
                  } else {
                     return new BsonDocument();
                  }
               }
            } catch (Throwable var6) {
               return new BsonDocument();
            }
         }
      }
   }

   public static void saveWardrobeState(UUID ownerId, BsonDocument wardrobeDoc) {
      if (ownerId != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            Path file = cosmeticsFile();
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) {
               doc = new BsonDocument();
            }

            if (wardrobeDoc != null && !wardrobeDoc.isEmpty()) {
               BsonValue existing = doc.get(ownerId.toString());
               BsonDocument entry;
               if (existing != null && existing.isDocument()) {
                  entry = existing.asDocument();
               } else {
                  entry = new BsonDocument();
               }

               entry.put("formatVersion", new BsonInt32(4));
               entry.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
               entry.put("wardrobeState", wardrobeDoc);
               doc.put(ownerId.toString(), entry);
            } else {
               doc.remove(ownerId.toString());
            }

            BsonUtil.writeDocument(file, doc, true).join();
         } catch (IOException var7) {
         } catch (Throwable var8) {
         }

      }
   }

   private static boolean looksLikeWardrobeStateDoc(BsonDocument doc) {
      if (doc != null && !doc.isEmpty()) {
         return doc.containsKey("cosmetics") || doc.containsKey("cosmeticIds") || doc.containsKey("cosmeticIdSet") || doc.containsKey("hiddenCosmeticTypes") || doc.containsKey("hiddenTypes") || doc.containsKey("PlayerWardrobe") || doc.containsKey("playerWardrobe") || doc.containsKey("Cosmetics");
      } else {
         return false;
      }
   }

   public static void saveBackpack(UUID ownerId, SimpleItemContainer backpack) {
      if (ownerId != null && backpack != null) {
         try {
            Path dir = baseDir();
            Files.createDirectories(dir);
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) {
               doc = new BsonDocument();
            }

            doc.put("formatVersion", new BsonInt32(4));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            doc.put("backpack", SimpleItemContainer.CODEC.encode(backpack, new ExtraInfo()));
            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
         } catch (IOException var4) {
         } catch (Throwable var5) {
         }

      }
   }

   private AmigoPersistence() {
   }
}
