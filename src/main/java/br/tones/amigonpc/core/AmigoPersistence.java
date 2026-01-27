package br.tones.amigonpc.core;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonDouble;
import org.bson.BsonString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Persistência simples do estado do AmigoNPC por player.
 *
 * - 1 arquivo por player (UUID.json)
 * - Guarda apenas o que importa (por enquanto: mochila 45 slots + campos base)
 * - Salva dentro da pasta do universo (Constants.UNIVERSE_PATH)
 */
public final class AmigoPersistence {

    private static final int FORMAT_VERSION = 4;
    private static final short BACKPACK_CAPACITY = 45; // 5 x 9

    // Aparência (opcional)
    private static final String KEY_MODEL_ID = "modelId";
    private static final String KEY_MODEL_SCALE = "modelScale";

    // Tipo de NPC (opcional)
    private static final String KEY_NPC_TYPE = "npcType";

    // Progressão (espadas corpo a corpo – por enquanto)
    private static final String KEY_SWORD_LEVEL = "swordLevel";
    private static final String KEY_EQUIPPED_WEAPON_ID = "equippedWeaponId";

    // Combate: modo Defender (persistente)
    private static final String KEY_DEFENDER_ENABLED = "defenderEnabled";

    // Loot automático (toggle via /autoloot)
    private static final String KEY_AUTOLOOT_ENABLED = "autoLootEnabled";

    // Progressão do NPC (XP total + stats base para scaling)
    private static final String KEY_TOTAL_XP = "totalXp";
    private static final String KEY_BASE_HP = "baseHp";
    private static final String KEY_BASE_DEF = "baseDef";

    private static Path baseDir() {
        // Mantém junto do save do universo (mesma raiz do player data)
        Path root = Constants.UNIVERSE_PATH;
        return root.resolve("amigonpc").resolve("players");
    }

    public static Path fileFor(UUID ownerId) {
        return baseDir().resolve(ownerId.toString() + ".json");
    }

    public static SimpleItemContainer loadBackpack(UUID ownerId) {
        if (ownerId == null) return new SimpleItemContainer(BACKPACK_CAPACITY);

        Path file = fileFor(ownerId);
        if (!Files.exists(file)) {
            return new SimpleItemContainer(BACKPACK_CAPACITY);
        }

        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return new SimpleItemContainer(BACKPACK_CAPACITY);

            // Compat: se não tiver, cria novo
            if (!doc.containsKey("backpack")) {
                return new SimpleItemContainer(BACKPACK_CAPACITY);
            }

            var val = doc.get("backpack");
            if (val == null) return new SimpleItemContainer(BACKPACK_CAPACITY);

            // Decode via CODEC
            return SimpleItemContainer.CODEC.decode(val, new ExtraInfo());

        } catch (Throwable t) {
            // Falha de leitura/codec -> não quebra o servidor
            return new SimpleItemContainer(BACKPACK_CAPACITY);
        }
    }

    public static String loadModelId(UUID ownerId) {
        if (ownerId == null) return null;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return null;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return null;
            if (!doc.containsKey(KEY_MODEL_ID)) return null;
            var v = doc.get(KEY_MODEL_ID);
            return (v != null && v.isString()) ? v.asString().getValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static double loadModelScale(UUID ownerId) {
        if (ownerId == null) return 1.0;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return 1.0;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return 1.0;
            if (!doc.containsKey(KEY_MODEL_SCALE)) return 1.0;
            var v = doc.get(KEY_MODEL_SCALE);
            if (v == null) return 1.0;
            if (v.isDouble()) return v.asDouble().getValue();
            if (v.isInt32()) return v.asInt32().getValue();
            return 1.0;
        } catch (Throwable ignored) {
            return 1.0;
        }
    }

    public static void saveModel(UUID ownerId, String modelId, double scale) {
        if (ownerId == null) return;
        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));

            if (modelId == null || modelId.isBlank()) {
                doc.remove(KEY_MODEL_ID);
                doc.remove(KEY_MODEL_SCALE);
            } else {
                doc.put(KEY_MODEL_ID, new BsonString(modelId));
                doc.put(KEY_MODEL_SCALE, new BsonDouble(scale));
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }

    public static String loadNpcType(UUID ownerId) {
        if (ownerId == null) return null;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return null;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return null;
            if (!doc.containsKey(KEY_NPC_TYPE)) return null;
            var v = doc.get(KEY_NPC_TYPE);
            return (v != null && v.isString()) ? v.asString().getValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void saveNpcType(UUID ownerId, String npcType) {
        if (ownerId == null) return;
        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));

            if (npcType == null || npcType.isBlank()) {
                doc.remove(KEY_NPC_TYPE);
            } else {
                doc.put(KEY_NPC_TYPE, new BsonString(npcType));
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }


    public static int loadSwordLevel(UUID ownerId) {
        if (ownerId == null) return 1;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return 1;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return 1;
            if (!doc.containsKey(KEY_SWORD_LEVEL)) return 1;
            var v = doc.get(KEY_SWORD_LEVEL);
            int lvl = 1;
            if (v != null) {
                if (v.isInt32()) lvl = v.asInt32().getValue();
                else if (v.isDouble()) lvl = (int) v.asDouble().getValue();
            }
            return Math.max(1, lvl);
        } catch (Throwable ignored) {
            return 1;
        }
    }

    public static String loadEquippedWeaponId(UUID ownerId) {
        if (ownerId == null) return null;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return null;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return null;
            if (!doc.containsKey(KEY_EQUIPPED_WEAPON_ID)) return null;
            var v = doc.get(KEY_EQUIPPED_WEAPON_ID);
            return (v != null && v.isString()) ? v.asString().getValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void saveSwordState(UUID ownerId, int swordLevel, String equippedWeaponId) {
        if (ownerId == null) return;
        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));

            doc.put(KEY_SWORD_LEVEL, new BsonInt32(Math.max(1, swordLevel)));
            if (equippedWeaponId == null || equippedWeaponId.isBlank()) {
                doc.remove(KEY_EQUIPPED_WEAPON_ID);
            } else {
                doc.put(KEY_EQUIPPED_WEAPON_ID, new BsonString(equippedWeaponId));
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }


    public static boolean loadDefenderEnabled(UUID ownerId) {
        if (ownerId == null) return false;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return false;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return false;
            if (!doc.containsKey(KEY_DEFENDER_ENABLED)) return false;
            var v = doc.get(KEY_DEFENDER_ENABLED);
            if (v == null) return false;

            // aceitamos algumas formas por compatibilidade
            if (v.isBoolean()) return v.asBoolean().getValue();
            if (v.isInt32()) return v.asInt32().getValue() != 0;
            if (v.isDouble()) return v.asDouble().getValue() != 0.0;
            if (v.isString()) {
                String s = v.asString().getValue();
                if (s == null) return false;
                s = s.trim().toLowerCase();
                return s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim") || s.equals("yes");
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void saveDefenderEnabled(UUID ownerId, boolean enabled) {
        if (ownerId == null) return;
        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));

            // salva como int para não depender de BsonBoolean na build
            doc.put(KEY_DEFENDER_ENABLED, new BsonInt32(enabled ? 1 : 0));

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }

    public static boolean loadAutoLootEnabled(UUID ownerId) {
        if (ownerId == null) return true;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return true;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return true;
            if (!doc.containsKey(KEY_AUTOLOOT_ENABLED)) return true;
            var v = doc.get(KEY_AUTOLOOT_ENABLED);
            if (v == null) return true;
            if (v.isBoolean()) return v.asBoolean().getValue();
            if (v.isInt32()) return v.asInt32().getValue() != 0;
            if (v.isDouble()) return v.asDouble().getValue() != 0.0;
            if (v.isString()) {
                String s = v.asString().getValue();
                if (s == null) return true;
                s = s.trim().toLowerCase();
                return s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim") || s.equals("yes");
            }
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void saveAutoLootEnabled(UUID ownerId, boolean enabled) {
        if (ownerId == null) return;
        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));

            doc.put(KEY_AUTOLOOT_ENABLED, new BsonInt32(enabled ? 1 : 0));
            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }

    // =========================================================
    // XP total + stats base (HP/DEF)
    // =========================================================

    public static long loadTotalXp(UUID ownerId) {
        if (ownerId == null) return 0L;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return 0L;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return 0L;
            if (!doc.containsKey(KEY_TOTAL_XP)) return 0L;
            var v = doc.get(KEY_TOTAL_XP);
            if (v == null) return 0L;
            if (v.isInt64()) return v.asInt64().getValue();
            if (v.isInt32()) return v.asInt32().getValue();
            if (v.isDouble()) return (long) v.asDouble().getValue();
            if (v.isString()) {
                try { return Long.parseLong(v.asString().getValue()); } catch (Throwable ignored) {}
            }
            return 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public static long loadBaseHp(UUID ownerId) {
        if (ownerId == null) return -1L;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return -1L;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return -1L;
            if (!doc.containsKey(KEY_BASE_HP)) return -1L;
            var v = doc.get(KEY_BASE_HP);
            if (v == null) return -1L;
            if (v.isInt64()) return v.asInt64().getValue();
            if (v.isInt32()) return v.asInt32().getValue();
            if (v.isDouble()) return (long) v.asDouble().getValue();
            if (v.isString()) {
                try { return Long.parseLong(v.asString().getValue()); } catch (Throwable ignored) {}
            }
            return -1L;
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    public static long loadBaseDef(UUID ownerId) {
        if (ownerId == null) return -1L;
        Path file = fileFor(ownerId);
        if (!Files.exists(file)) return -1L;
        try {
            BsonDocument doc = BsonUtil.readDocumentNow(file);
            if (doc == null) return -1L;
            if (!doc.containsKey(KEY_BASE_DEF)) return -1L;
            var v = doc.get(KEY_BASE_DEF);
            if (v == null) return -1L;
            if (v.isInt64()) return v.asInt64().getValue();
            if (v.isInt32()) return v.asInt32().getValue();
            if (v.isDouble()) return (long) v.asDouble().getValue();
            if (v.isString()) {
                try { return Long.parseLong(v.asString().getValue()); } catch (Throwable ignored) {}
            }
            return -1L;
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    /** Salva totalXp e/ou baseHp/baseDef (qualquer valor < 0 é ignorado). */
    public static void saveNpcProgress(UUID ownerId, long totalXp, long baseHp, long baseDef) {
        if (ownerId == null) return;
        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));

            if (totalXp >= 0) {
                try {
                    doc.put(KEY_TOTAL_XP, new org.bson.BsonInt64(totalXp));
                } catch (Throwable ignored) {
                    doc.put(KEY_TOTAL_XP, new BsonDouble((double) totalXp));
                }
            }
            if (baseHp >= 0) {
                try {
                    doc.put(KEY_BASE_HP, new org.bson.BsonInt64(baseHp));
                } catch (Throwable ignored) {
                    doc.put(KEY_BASE_HP, new BsonDouble((double) baseHp));
                }
            }
            if (baseDef >= 0) {
                try {
                    doc.put(KEY_BASE_DEF, new org.bson.BsonInt64(baseDef));
                } catch (Throwable ignored) {
                    doc.put(KEY_BASE_DEF, new BsonDouble((double) baseDef));
                }
            }

            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }

    public static void saveTotalXp(UUID ownerId, long totalXp) {
        saveNpcProgress(ownerId, Math.max(0L, totalXp), -1L, -1L);
    }

    // Wrappers (compat) - usados pelo manager para salvar separadamente
    public static void saveBaseHp(UUID ownerId, long baseHp) {
        saveNpcProgress(ownerId, -1L, baseHp, -1L);
    }

    public static void saveBaseDef(UUID ownerId, long baseDef) {
        saveNpcProgress(ownerId, -1L, -1L, baseDef);
    }

    public static void saveBaseHpDef(UUID ownerId, long baseHp, long baseDef) {
        saveNpcProgress(ownerId, -1L, baseHp, baseDef);
    }

    public static void saveBackpack(UUID ownerId, SimpleItemContainer backpack) {
        if (ownerId == null || backpack == null) return;

        try {
            Path dir = baseDir();
            Files.createDirectories(dir);

            // ✅ merge: não apagar outros campos (ex.: aparência)
            BsonDocument doc = BsonUtil.readDocumentNow(fileFor(ownerId));
            if (doc == null) doc = new BsonDocument();

            doc.put("formatVersion", new BsonInt32(FORMAT_VERSION));
            doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
            doc.put("backpack", SimpleItemContainer.CODEC.encode(backpack, new ExtraInfo()));

            // writeDocument já cria .bak se pedir; por enquanto false
            BsonUtil.writeDocument(fileFor(ownerId), doc, true).join();

        } catch (IOException ignored) {
            // createDirectories pode lançar IOException
        } catch (Throwable ignored) {
            // join/codec
        }
    }

    private AmigoPersistence() {}
}
