package br.tones.amigonpc.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;

import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.RespondToHit;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;

import br.tones.amigonpc.core.swords.SwordMessages;
import br.tones.amigonpc.core.swords.SwordProgression;
import br.tones.amigonpc.core.progress.XpProgression;
import br.tones.amigonpc.core.progress.StatScaling;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ItemAnimation;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * 1 NPC por player.
 *
 * Spawn VISÍVEL: usa NPCPlugin.spawnNPC(Store,...), que já cria a entidade com setup/modelo correto.
 * Despawn: remove pelo Store.removeEntity(ref, RemoveReason.*).
 *
 * Tudo por reflexão pra aguentar variação de build.
 *
 * CORREÇÃO PRINCIPAL:
 * - world.execute(...) pode executar no próximo tick.
 * - NÃO aguardar latch/timeout, pois o spawn pode acontecer e o plugin "achar que falhou"
 *   (não registra ref) -> duplica e não consegue despawn.
 *
 * Solução:
 * - registrar SPAWNING no map ANTES de enfileirar
 * - quando executar, preencher ref e virar ACTIVE
 * - se pedir despawn durante spawn, marcar DESPAWNING e remover assim que tiver ref
 */
public final class AmigoNpcManager {

    private static final AmigoNpcManager SHARED = new AmigoNpcManager();
    public static AmigoNpcManager getShared() { return SHARED; }

    // Modelo padrão fixo conforme solicitado (passivo + Wraith)
    private static final String DEFAULT_MODEL_ID = "Wraith";
    private static final double DEFAULT_MODEL_SCALE = 1.0;
    // Role padrão: manter passivo e compatível com qualquer build.
    // (Alguns servidores carregam apenas o .jar e NÃO montam asset-pack do mod,
    // então não dependemos de Role custom aqui.)
    private static final String DEFAULT_ROLE_NAME = "Amigo_Follow";

    // Debug de combate/equip (use false para desligar logs)
    private static final boolean DEBUG_COMBAT_DEFAULT = false;

    // Cache: resolve chave de animação de ataque por arma (best-effort via ItemPlayerAnimations)
    private static final Map<String, String> WEAPON_ATTACK_ANIM_CACHE = new ConcurrentHashMap<>();

    private enum State { SPAWNING, ACTIVE, DESPAWNING }

    private static final class NpcRecord {
        final Object worldObj;
        final UUID ownerId;
        volatile Object refObj;  // null enquanto spawnando
        volatile State state;

        // Vida real / DOWNED
        volatile boolean downed;
        volatile long downedUntilMillis;

        // Persistência (1 arquivo por player): mochila 45 slots
        volatile SimpleItemContainer backpack;

        // Auto-loot (passivo): coleta itens dropados num raio curto
        volatile long nextAutoLootMillis;
        volatile boolean lootPausedInventoryFull;
        volatile long nextLootFullRecheckMillis;
        volatile long nextLootFullMsgMillis;
        volatile boolean backpackDirty;
        volatile long nextBackpackSaveMillis;


// Loot pós-combate: combat tags (anti-roubo) + LOOTING correndo até o item
final java.util.ArrayList<CombatTag> combatTags = new java.util.ArrayList<>();
volatile long lastCombatEndMillis;
volatile long lastCombatTagMillis;
volatile Vector3d lastBattleCenter;
volatile boolean wasInCombat;
volatile boolean lootingActive;
volatile Object lootTargetRefObj;
volatile long lootTargetSinceMillis;
final java.util.ArrayList<Object> pendingLootRefObjs = new java.util.ArrayList<>();
final java.util.Map<Object, Long> lootProcessedUntil = new java.util.concurrent.ConcurrentHashMap<>();


        
// Loot chat summary (reduz poluição): acumula quantidades e envia após um pequeno atraso
final java.util.Map<String, Integer> lootChatAcc = new java.util.LinkedHashMap<>();
volatile long lootChatSendAtMillis;
// XP/nível (simples, etapa inicial)
        volatile int level = 1;
        volatile String equippedWeaponId;
        volatile double xpInLevel = 0.0; // progresso dentro do nível atual

        // Progressão do NPC (XP acumulativa + level calculado)
        volatile long totalXp = 0L;
        volatile int npcLevelCached = 1;

        // Stats base (capturados 1x) para scaling (HP/DEF)
        volatile long baseHp = -1L;
        volatile long baseDef = -1L;

        // Aparência opcional (model spawn via spawnEntity)
        volatile String modelId;
        volatile double modelScale;

        // Re-spawn automático (ex.: teleport do dono)
        volatile boolean respawnRequested;
        volatile Object respawnWorldObj;
        volatile Object respawnSenderObj;
        volatile long respawnAtMillis;
        volatile String respawnMessage;

        // Follow/anim: suavização para evitar "correndo parado" e ficar colado
        volatile long lastMoveToMillis;
        volatile long lastSampleMillis;
        volatile Vector3d lastMoveTarget;
        volatile long lastMoveIssuedMillis;
        volatile Vector3d lastOwnerPos;
        volatile Vector3d lastNpcPos;
        volatile long lastNpcMovedMillis;
        volatile long lastTeleportMillis;
        long farSinceMillis;

        // Combate corpo a corpo (cooldown simples)
        volatile long lastMeleeAttackMillis;

        // Reposicionamento em combate (best-effort, para mobs altos)
        volatile long lastCombatNudgeMillis;

        // "Aggro pulse": tenta fazer o inimigo mirar no NPC também
        volatile long lastAggroPulseMillis;

        // Animação de ataque (limpeza best-effort para não travar na pose)
        volatile String lastAttackAnimId;
        volatile long clearAttackAnimAtMillis;

        // Debug (rate limit)
        volatile long debugNextEquipMillis;
        volatile long debugNextCombatMillis;
        volatile long debugNextAttackMillis;

        // idle look-around (sutil)
        volatile float idleLookYawOffset;
        volatile long idleLookNextMillis;

        // Combate (etapa inicial): quando o dono sofre dano, o NPC troca o LockedTarget
        // para o agressor por alguns segundos, depois volta a seguir.
        volatile Object combatTargetRefObj;
        volatile long combatUntilMillis;

        // Assistência: alvo do primeiro ataque do player (apenas quando defender ON e sem agressor)
        volatile Object assistTargetRefObj;
        volatile long assistUntilMillis;

        // Target lost / stuck (para não ficar parado esperando o mob voltar)
        volatile long targetLostSinceMillis;
        volatile long targetStuckSinceMillis;
        volatile double lastTargetHorizontal = -1.0;
        volatile long lastTargetSampleMillis;

        // Controle de perseguição: quando estoura o limite de chase, só volta a adquirir alvo
        // ao retornar perto do dono (evita o NPC "sumir" longe e ficar trocando alvo).
        volatile boolean chaseDisengaged;

        // Estado do Role (para permitir follow diferente em combate)


        // Flags de comportamento (alguns comandos antigos dependem disso)
        volatile boolean defendeEnabled;

        // Auto-loot (toggle via /autoloot)
        volatile boolean autoLootEnabled = true;

        // Log de debug no chat (/amigo log)
        volatile boolean debugLogEnabled;

        NpcRecord(Object worldObj, UUID ownerId, Object refObj, State state) {
            this.worldObj = worldObj;
            this.ownerId = ownerId;
            this.refObj = refObj;
            this.state = state;
            this.debugLogEnabled = DEBUG_COMBAT_DEFAULT;
        }
    }

    private static final class PendingRespawn {
        final Object worldObj;
        final Object senderObj;
        final long atMillis;
        final String message;

        PendingRespawn(Object worldObj, Object senderObj, long atMillis, String message) {
            this.worldObj = worldObj;
            this.senderObj = senderObj;
            this.atMillis = atMillis;
            this.message = message;
        }
    }


private static final class CombatTag {
    final Object targetRefObj;
    Vector3d pos;
    long lastSeenMillis;

    CombatTag(Object targetRefObj, Vector3d pos, long lastSeenMillis) {
        this.targetRefObj = targetRefObj;
        this.pos = pos;
        this.lastSeenMillis = lastSeenMillis;
    }
}

    private final Map<UUID, NpcRecord> npcRefPorPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> debugLogByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRespawn> pendingRespawns = new ConcurrentHashMap<>();
    // Set rápido para identificar o NPC do Amigo em eventos (ex.: Damage)
    private final Map<Object, UUID> amigoRefs = new ConcurrentHashMap<>();
    private static volatile String LAST_ERROR;

    // PvP entre NPCs (controlado por /amigopvp on|off – admin)
    private volatile boolean pvpEnabled = false;

    // Janela de combate (tuning): 3s, renovada por sinais de combate
    private static final long COMBAT_WINDOW_MILLIS = 3_000L;
    private static final long ASSIST_GRACE_MILLIS = 3_000L;
    // Cooldown base (o real é calculado por estilo agressivo/defensivo)
    private static final long MELEE_COOLDOWN_MILLIS = 850L;
    // Auto-aquisicao de alvo quando Defender esta ON (sem precisar do player dar hit)
    private static final double DEFENDER_AUTO_ACQUIRE_RADIUS = 12.0; // blocos (horizontal)
    private static final double DEFENDER_AUTO_ACQUIRE_MAX_DY = 2.5;  // tolerancia vertical

    // Limite de perseguição: se o NPC se afastar demais do dono, ele desiste e volta.
    private static final double CHASE_MAX_DISTANCE = 15.0;
    // Após desistir, só volta a adquirir alvo quando estiver perto do dono.
    private static final double CHASE_REACQUIRE_DISTANCE = 10.0;

    // Auto-loot passivo (raio fixo)
    private static final double AUTOLOOT_RADIUS = 3.0;
    private static final double AUTOLOOT_OWNER_RADIUS = 20.0; // barreira simples para nao sugar loot de outros players
    private static final long AUTOLOOT_INTERVAL_MS = 250L;
    private static final int AUTOLOOT_MAX_ITEMS_PER_SCAN = 8;
    private static final long AUTOLOOT_FULL_MSG_COOLDOWN_MS = 30_000L;
    private static final Archetype<EntityStore> AUTOLOOT_QUERY = Archetype.of(
            TransformComponent.getComponentType(),
            ItemComponent.getComponentType()
    );

    // Debounce de persistência da mochila quando itens são inseridos rapidamente.
    private static final long BACKPACK_SAVE_DEBOUNCE_MS = 400L;

    
// =========================================================
// Loot pós-combate (combat tag) — LOOTING correndo até o item
// =========================================================
// Só entra em LOOTING quando não há inimigo muito próximo do NPC.
private static final double LOOTING_NO_ENEMY_RADIUS = 4.0;
// Raio para considerar drops "do combate" ao redor de cada combat tag.
private static final double LOOT_TAG_SCAN_RADIUS = 6.0;
// Distância para efetivar o pickup (chegou perto do item).
// Um pouco maior para compensar o path/offset do Role (o NPC tende a parar "do lado").
private static final double LOOT_PICKUP_DISTANCE = 3.2;

private static final String LOOT_CHAT_TEMPLATE = "{quantidade} {item} Coletado.";


private static final long LOOT_CHAT_SUMMARY_DELAY_MS = 500L;
// Limpa combat tags: 25 segundos após o fim do combate OU ao sair 25 blocos do local.
private static final long COMBAT_TAG_CLEAR_MS = 25_000L;
private static final double COMBAT_TAG_CLEAR_DISTANCE = 25.0;

// Anti-loop: se um item não couber/der erro, ignora por alguns segundos e tenta outros.
private static final long LOOT_SKIP_RETRY_MS = 4_000L;
// Se ficar tempo demais tentando alcançar um item, troca para outro.
private static final long LOOT_TARGET_TIMEOUT_MS = 12_000L;

private static final int COMBAT_TAG_MAX = 16;
private static final int LOOT_PENDING_MAX = 512;

// XP do NPC (totalXp): XP por hit foi desativado para manter a barra/nível
    // sincronizados com a XP “por monstro derrotado” (mensagem no chat ocorre no kill).
    private static final long XP_PER_HIT = 0L;
    private static final long XP_PER_KILL = 40L;


    public String getLastError() { return LAST_ERROR; }
    private static void setError(String msg) { LAST_ERROR = msg; }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean enabled) { this.pvpEnabled = enabled; }

    // =========================================================
    // Defender (persistente)
    // =========================================================

    /** Se não houver NPC em memória, lê do arquivo do player. */
    public boolean isDefendeEnabled(UUID owner) {
        if (owner == null) return false;
        NpcRecord rec = npcRefPorPlayer.get(owner);
        if (rec != null) return rec.defendeEnabled;
        return AmigoPersistence.loadDefenderEnabled(owner);
    }

    /**
     * Seta o modo Defender e persiste.
     * - OFF limpa assistência imediatamente
     */
    public void setDefendeEnabled(UUID owner, boolean enabled) {
        if (owner == null) return;

        // persistência (1 arquivo por player)
        AmigoPersistence.saveDefenderEnabled(owner, enabled);

        NpcRecord rec = npcRefPorPlayer.get(owner);
        if (rec != null) {
            rec.defendeEnabled = enabled;
            if (!enabled) {
                clearAssist(rec);
            }
        }
    }

    // =========================================================
    // =========================================================
    // AutoLoot (/autoloot)
    // =========================================================

    public boolean isAutoLootEnabled(UUID ownerId) {
        if (ownerId == null) return true;
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec != null) return rec.autoLootEnabled;
        return AmigoPersistence.loadAutoLootEnabled(ownerId);
    }

    public boolean toggleAutoLoot(UUID ownerId) {
        boolean newVal = !isAutoLootEnabled(ownerId);
        setAutoLootEnabled(ownerId, newVal);
        return newVal;
    }

    public void setAutoLootEnabled(UUID ownerId, boolean enabled) {
        if (ownerId == null) return;
        AmigoPersistence.saveAutoLootEnabled(ownerId, enabled);
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec != null) {
            rec.autoLootEnabled = enabled;
            if (!enabled) {
                // ao desligar, garantir que não fica em estado de looting/pausa
                endCombatTaggedLooting(rec);
                rec.lootPausedInventoryFull = false;
                rec.lootChatAcc.clear();
                rec.lootChatSendAtMillis = 0L;
            }
        }
    }

    // Finaliza e limpa qualquer estado interno de LOOTING/combattag.
    // Mantemos simples para evitar ficar preso em itens/ref inválidos.
    private static void endCombatTaggedLooting(NpcRecord rec) {
        if (rec == null) return;
        rec.lootingActive = false;
        rec.lootTargetRefObj = null;
        rec.lootTargetSinceMillis = 0L;
        if (rec.pendingLootRefObjs != null) rec.pendingLootRefObjs.clear();
        if (rec.lootProcessedUntil != null) rec.lootProcessedUntil.clear();
        if (rec.combatTags != null) rec.combatTags.clear();
        rec.lastBattleCenter = null;
        rec.lastCombatEndMillis = 0L;
        rec.lootPausedInventoryFull = false;
        // Reutiliza o timer já existente do auto-loot para evitar um campo extra.
        rec.nextAutoLootMillis = 0L;
    }

    // Log de debug (chat)
    // =========================================================

    public boolean isDebugLogEnabled(UUID owner) {
        if (owner == null) return false;
        NpcRecord rec = npcRefPorPlayer.get(owner);
        if (rec != null) return rec.debugLogEnabled;
        Boolean v = debugLogByOwner.get(owner);
        return v != null && v;
    }

    public boolean toggleDebugLog(UUID owner) {
        if (owner == null) return false;
        boolean enabled = !isDebugLogEnabled(owner);
        setDebugLogEnabled(owner, enabled);
        return enabled;
    }

    public void setDebugLogEnabled(UUID owner, boolean enabled) {
        if (owner == null) return;
        debugLogByOwner.put(owner, enabled);
        NpcRecord rec = npcRefPorPlayer.get(owner);
        if (rec != null) rec.debugLogEnabled = enabled;
    }

    // =========================================================
    // Escolha do tipo (com preferência passiva temporária)
    // =========================================================

    /**
     * Em algumas builds, getPresetCoverageTestNPCs() devolve um "corvo"/pássaro de teste.
     * Para o AmigoNPC ficar com cara de companheiro, tentamos preferir templates humanoides.
     *
     * AJUSTE TEMPORÁRIO: prioriza tipos civis/passivos se existirem.
     */
    private static String choosePreferredNpcType(Object npcPlugin) {
        // ✅ ajuste temporário: tenta forçar passivos
        final String[] FORCED_PASSIVE = { "citizen", "villager", "merchant", "trader", "worker", "farmer" };

        // 1) Role templates spawnáveis
        Object rolesObj = invokeOneArg(npcPlugin, "getRoleTemplateNames", boolean.class, true);
        List<String> roles = toStringList(rolesObj);

        // 1.1) tenta match exato (case-insensitive)
        for (String want : FORCED_PASSIVE) {
            for (String r : roles) {
                if (r != null && r.equalsIgnoreCase(want)) return r;
            }
        }

        // 1.2) tenta contains (ex.: human_citizen)
        for (String want : FORCED_PASSIVE) {
            for (String r : roles) {
                if (r == null) continue;
                if (r.toLowerCase().contains(want)) return r;
            }
        }

        // 2) fallback: humanoides (evita aves/test)
        String pick = pickHumanLike(roles);
        if (pick != null) return pick;
        if (!roles.isEmpty()) return roles.get(0);

        // 3) Fallback: presets de coverage test (podem ser pássaros)
        return firstStringFromArray(invokeNoArg(npcPlugin, "getPresetCoverageTestNPCs"));
    }

    private static List<String> toStringList(Object listObj) {
        if (listObj instanceof List<?> list) {
            java.util.ArrayList<String> out = new java.util.ArrayList<>();
            for (Object v : list) {
                if (v != null) out.add(String.valueOf(v));
            }
            return out;
        }
        return List.of();
    }

    private static String pickHumanLike(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;

        // evita os mais comuns de teste que saem voando
        String[] deny = {"crow", "raven", "bird", "avian", "bat", "eagle", "hawk", "falcon", "vulture", "owl", "pigeon", "duck", "seagull", "test"};

        // preferências (ordem importa)
        String[] prefer = {"citizen", "human", "villager", "guard", "soldier", "merchant", "trader", "worker", "farmer", "npc"};

        for (String p : prefer) {
            for (String s : candidates) {
                String low = s.toLowerCase();
                if (low.contains(p) && !containsAny(low, deny)) return s;
            }
        }

        // se não achou um "humanoide", pelo menos evite aves/test
        for (String s : candidates) {
            String low = s.toLowerCase();
            if (!containsAny(low, deny)) return s;
        }

        return null;
    }

    private static boolean containsAny(String low, String[] needles) {
        for (String n : needles) {
            if (low.contains(n)) return true;
        }
        return false;
    }

    public boolean hasNpc(UUID ownerId) {
        return ownerId != null && npcRefPorPlayer.containsKey(ownerId);
    }

    /** Compat com chamadas antigas (ex.: UI/Service). */
    public boolean spawn(Object worldObj, UUID ownerId) {
        return spawn(worldObj, ownerId, null);
    }

    /**
     * Spawn real via NPCPlugin.
     * @param senderObj opcional (player/sender) pra ajudar a pegar posição; pode ser null.
     *
     * ✅ Agora é assíncrono: retorna true quando foi enfileirado com sucesso.
     * Isso evita duplicação causada por timeout falso.
     */
    public boolean spawn(Object worldObj, UUID ownerId, Object senderObj) {
        if (worldObj == null || ownerId == null) {
            setError("worldObj ou ownerId null.");
            return false;
        }

        // Anti-dup: se já existe em SPAWNING/ACTIVE, não cria outro
        NpcRecord existing = npcRefPorPlayer.get(ownerId);
        if (existing != null) {
            if (existing.state == State.SPAWNING || existing.state == State.ACTIVE) {
                setError("NPC já existe para este player (evitando duplicação).");
                return false;
            }
            setError("NPC está em processo de remoção. Tente novamente em instantes.");
            return false;
        }

        // Registra SPAWNING antes do execute (chave do fix)
        final NpcRecord rec = new NpcRecord(worldObj, ownerId, null, State.SPAWNING);
        // aplica preferencia de log (runtime)
        try {
            Boolean dbg = debugLogByOwner.get(ownerId);
            rec.debugLogEnabled = (dbg != null) ? dbg.booleanValue() : DEBUG_COMBAT_DEFAULT;
        } catch (Throwable ignored) {}
        // ✅ carrega mochila do disco (1 arquivo por player)
        rec.backpack = AmigoPersistence.loadBackpack(ownerId);
        // ✅ carrega aparência (opcional)
        String savedModel = AmigoPersistence.loadModelId(ownerId);
        rec.modelId = (savedModel == null || savedModel.isBlank()) ? DEFAULT_MODEL_ID : savedModel;
        double savedScale = AmigoPersistence.loadModelScale(ownerId);
        rec.modelScale = (savedScale <= 0.0) ? DEFAULT_MODEL_SCALE : savedScale;
        // ✅ carrega nível de espadas + arma equipada (progressão corpo a corpo)
        rec.level = SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
        rec.equippedWeaponId = AmigoPersistence.loadEquippedWeaponId(ownerId);
        // ✅ carrega modo Defender (persistente)
        rec.defendeEnabled = AmigoPersistence.loadDefenderEnabled(ownerId);
        rec.autoLootEnabled = AmigoPersistence.loadAutoLootEnabled(ownerId);

        // ✅ Progressão do NPC (XP total + stats base para scaling)
        rec.totalXp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
        rec.npcLevelCached = XpProgression.levelFromTotalXp(rec.totalXp);

        // ✅ Sincroniza o nível de espadas com o level calculado do totalXp (HUD e combate usam nível da espada)
        int computedSwordLevel = SwordProgression.clampLevel(rec.npcLevelCached);
        if (computedSwordLevel != rec.level) {
            rec.level = computedSwordLevel;
            String expectedWeapon = SwordProgression.weaponIdForLevel(rec.level);
            rec.equippedWeaponId = expectedWeapon;
            AmigoPersistence.saveSwordState(ownerId, rec.level, expectedWeapon);
        }
        rec.baseHp = AmigoPersistence.loadBaseHp(ownerId);
        rec.baseDef = AmigoPersistence.loadBaseDef(ownerId);
        npcRefPorPlayer.put(ownerId, rec);

        boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
            try {
                // Se o record já foi removido/substituído, aborta
                if (npcRefPorPlayer.get(ownerId) != rec) return;

                // 1) Store = world.getEntityStore().getStore()
                Object componentStore = getComponentStoreFromWorld(worldObj);
                if (componentStore == null) {
                    setError("Não consegui obter Store via world.getEntityStore().getStore().");
                    npcRefPorPlayer.remove(ownerId, rec);
                    return;
                }

                // 2) NPCPlugin.get()
                Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
                if (npcPlugin == null) {
                    setError("NPCPlugin.get() não disponível nesta build.");
                    npcRefPorPlayer.remove(ownerId, rec);
                    return;
                }

                // 4) posição
                Object pos = tryGetOwnerPositionFromWorldStore(worldObj, componentStore, ownerId);
                if (pos == null) pos = tryGetSenderPosition(senderObj);

                pos = coerceToVector3d(pos);
                if (pos == null) {
                    setError("Não consegui obter posição Vector3d do player.");
                    npcRefPorPlayer.remove(ownerId, rec);
                    return;
                }

                // spawn pertinho
                pos = offsetVector3d(pos, 1.5, 0.0, 1.5);

                // 5) rotação
                Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
                if (rot == null) rot = newVector3f(0f, 0f, 0f);                // 6) spawnNPC(Store store, String npcType, String groupType, Vector3d pos, Vector3f rot)
                Object pair;

                // ✅ Padrão: sempre spawnEntity com role passivo (Empty_Role) + ModelAsset.
                // Se o player configurou um modelId via comando, ele substitui o padrão Wraith.
                Object model = buildModelFromAssetId(rec.modelId, (float) rec.modelScale);
                if (model == null) {
                    setError("ModelAsset não encontrado/ inválido: " + rec.modelId);
                    npcRefPorPlayer.remove(ownerId, rec);
                    return;
                }

                int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, DEFAULT_ROLE_NAME);
                if (roleIndex < 0) {
                    setError("Missing NPC role: " + DEFAULT_ROLE_NAME);
                    npcRefPorPlayer.remove(ownerId, rec);
                    return;
                }

                Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
                pair = invokeSpawnEntity(npcPlugin, componentStore, roleIndex, pos, rot, model, ownerRef);
                if (pair == null) {
                    setError("spawnEntity retornou null (assinatura incompatível). modelId=" + rec.modelId);
                    npcRefPorPlayer.remove(ownerId, rec);
                    return;
                }

                // Pair normalmente tem a Ref no "left/first/key"
                Object ref = extractRefFromPair(pair);
                rec.refObj = (ref != null ? ref : pair);

                // Marca como "Amigo" para filtros (ex.: imunidade a dano)
                if (rec.refObj != null) {
                    amigoRefs.put(rec.refObj, ownerId);
                }

                // Se pediram despawn enquanto spawnava: remove já (e respeita respawn automático)
                if (rec.state == State.DESPAWNING) {
                    if (doRemoveEntity(componentStore, rec.refObj)) {
                        finalizeRemove(ownerId, rec);
                    } else {
                        // mantém record pra tentar despawn depois
                        setError("Spawn OK, mas falha ao remover imediatamente (DESPAWNING).");
                    }
                    return;
                }

                // ✅ equipa arma corpo-a-corpo conforme nível (pode trocar até em combate)
                try {
                    @SuppressWarnings("unchecked")
                    Store<EntityStore> store = (Store<EntityStore>) componentStore;
                    @SuppressWarnings("unchecked")
                    Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;
                    applySwordWeaponNow(store, npcRef, ownerId, rec, true);

                    // ✅ captura baseHp/baseDef (1x) e aplica scaling (HP/DEF) conforme level (cap 100)
                    applyNpcScaling(store, npcRef, ownerId, rec, true);
                } catch (Throwable ignored) {}

                rec.state = State.ACTIVE;

            } catch (Throwable t) {
                setError("spawn() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                npcRefPorPlayer.remove(ownerId, rec);
                if (rec.refObj != null) amigoRefs.remove(rec.refObj);
            }
        });

        if (!queued) {
            setError("world.execute falhou: " + HytaleBridge.getLastError());
            npcRefPorPlayer.remove(ownerId, rec);
            return false;
        }

        return true; // enfileirado com record registrado
    }

    /**
     * Spawn síncrono usando o Store entregue pela API de comandos.
     *
     * Motivo: em algumas builds, o spawn assíncrono via world.execute pode finalizar,
     * mas o registro interno ser removido por erro de reflection (Store/posição),
     * causando "NPC criado" e depois "/amigo despawn" dizer que não existe.
     *
     * Esse caminho evita reflection para obter Store e posição.
     */
    public boolean spawnWithStore(Object worldObj,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> playerEntityRef,
                                 UUID ownerId,
                                 Object senderObj) {
        if (worldObj == null || store == null || playerEntityRef == null || ownerId == null) {
            setError("world/store/playerEntityRef/ownerId inválidos.");
            return false;
        }

        // Anti-dup
        NpcRecord existing = npcRefPorPlayer.get(ownerId);
        if (existing != null) {
            if (existing.state == State.SPAWNING || existing.state == State.ACTIVE) {
                setError("NPC já existe para este player (evitando duplicação)." );
                return false;
            }
            setError("NPC está em processo de remoção. Tente novamente em instantes.");
            return false;
        }

        final NpcRecord rec = new NpcRecord(worldObj, ownerId, null, State.SPAWNING);
        // aplica preferencia de log (runtime)
        try {
            Boolean dbg = debugLogByOwner.get(ownerId);
            rec.debugLogEnabled = (dbg != null) ? dbg.booleanValue() : DEBUG_COMBAT_DEFAULT;
        } catch (Throwable ignored) {}
        rec.backpack = AmigoPersistence.loadBackpack(ownerId);
        String savedModel = AmigoPersistence.loadModelId(ownerId);
        rec.modelId = (savedModel == null || savedModel.isBlank()) ? DEFAULT_MODEL_ID : savedModel;
        double savedScale = AmigoPersistence.loadModelScale(ownerId);
        rec.modelScale = (savedScale <= 0.0) ? DEFAULT_MODEL_SCALE : savedScale;
        // ✅ carrega nível de espadas + arma equipada (progressão corpo a corpo)
        rec.level = SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
        rec.equippedWeaponId = AmigoPersistence.loadEquippedWeaponId(ownerId);
        // ✅ carrega modo Defender (persistente)
        rec.defendeEnabled = AmigoPersistence.loadDefenderEnabled(ownerId);
        rec.autoLootEnabled = AmigoPersistence.loadAutoLootEnabled(ownerId);

        // ✅ Progressão do NPC (XP total + stats base para scaling)
        rec.totalXp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
        rec.npcLevelCached = XpProgression.levelFromTotalXp(rec.totalXp);

        // ✅ Sincroniza o nível de espadas com o level calculado do totalXp (HUD e combate usam nível da espada)
        int computedSwordLevel = SwordProgression.clampLevel(rec.npcLevelCached);
        if (computedSwordLevel != rec.level) {
            rec.level = computedSwordLevel;
            String expectedWeapon = SwordProgression.weaponIdForLevel(rec.level);
            rec.equippedWeaponId = expectedWeapon;
            AmigoPersistence.saveSwordState(ownerId, rec.level, expectedWeapon);
        }
        rec.baseHp = AmigoPersistence.loadBaseHp(ownerId);
        rec.baseDef = AmigoPersistence.loadBaseDef(ownerId);
        npcRefPorPlayer.put(ownerId, rec);

        try {
            Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
            if (npcPlugin == null) {
                setError("NPCPlugin.get() não disponível nesta build.");
                npcRefPorPlayer.remove(ownerId, rec);
                return false;
            }

            // posição do player via store + TransformComponent
            Object pos = tryGetPositionFromStore(store, playerEntityRef);
            if (pos == null) {
                // fallback
                pos = tryGetSenderPosition(senderObj);
            }

            pos = coerceToVector3d(pos);
            if (pos == null) {
                setError("Não consegui obter posição Vector3d do player.");
                npcRefPorPlayer.remove(ownerId, rec);
                return false;
            }

            pos = offsetVector3d(pos, 1.5, 0.0, 1.5);
            Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
            if (rot == null) rot = newVector3f(0f, 0f, 0f);
            Object pair;

            // ✅ Padrão: sempre spawnEntity com role passivo (Empty_Role) + ModelAsset.
            Object model = buildModelFromAssetId(rec.modelId, (float) rec.modelScale);
            if (model == null) {
                setError("ModelAsset não encontrado/ inválido: " + rec.modelId);
                npcRefPorPlayer.remove(ownerId, rec);
                return false;
            }

            int roleIndex = getNpcRoleIndexWithFallbacks(npcPlugin, DEFAULT_ROLE_NAME);
            if (roleIndex < 0) {
                setError("Missing NPC role: " + DEFAULT_ROLE_NAME);
                npcRefPorPlayer.remove(ownerId, rec);
                return false;
            }

            pair = invokeSpawnEntity(npcPlugin, store, roleIndex, pos, rot, model, playerEntityRef);
            if (pair == null) {
                setError("spawnEntity retornou null (assinatura incompatível). modelId=" + rec.modelId);
                npcRefPorPlayer.remove(ownerId, rec);
                return false;
            }

            Object ref = extractRefFromPair(pair);
            rec.refObj = (ref != null ? ref : pair);

            // Marca como "Amigo" para filtros (ex.: imunidade a dano)
            if (rec.refObj != null) {
                amigoRefs.put(rec.refObj, ownerId);
            }

            if (rec.state == State.DESPAWNING) {
                if (doRemoveEntity(store, rec.refObj)) {
                    finalizeRemove(ownerId, rec);
                } else {
                    setError("Spawn OK, mas falha ao remover imediatamente (DESPAWNING)." );
                }
                return false;
            }

            // ✅ equipa arma corpo-a-corpo conforme nível (pode trocar até em combate)
            try {
                applySwordWeaponNow(store, (Ref<EntityStore>) rec.refObj, ownerId, rec, true);

                // ✅ captura baseHp/baseDef (1x) e aplica scaling (HP/DEF) conforme level (cap 100)
                applyNpcScaling(store, (Ref<EntityStore>) rec.refObj, ownerId, rec, true);
            } catch (Throwable ignored) {}

            rec.state = State.ACTIVE;
            return true;

        } catch (Throwable t) {
            setError("spawnWithStore() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            npcRefPorPlayer.remove(ownerId, rec);
            if (rec.refObj != null) amigoRefs.remove(rec.refObj);
            return false;
        }
    }

    /**
     * Despawn síncrono usando o Store entregue pela API de comandos.
     */
    public boolean despawnWithStore(Store<EntityStore> store, UUID ownerId) {
        if (store == null || ownerId == null) {
            setError("store ou ownerId inválidos.");
            return false;
        }

        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec == null) {
            setError("Nenhum NPC registrado para este player.");
            return false;
        }

        rec.state = State.DESPAWNING;

        try {
            if (rec.backpack != null) {
                AmigoPersistence.saveBackpack(ownerId, rec.backpack);
            }
            try { AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId); } catch (Throwable ignored2) {}
        } catch (Throwable ignored) {}

        if (rec.refObj == null) {
            return true;
        }

        if (!doRemoveEntity(store, rec.refObj)) {
            return false;
        }

        finalizeRemove(ownerId, rec);
        return true;
    }

    /**
     * Teleport/troca de mundo do dono: pede re-spawn automático no novo mundo.
     * Se já existe NPC: marca respawnRequested e despawna primeiro (salvando mochila).
     * Se não existe: só spawna.
     */
    public boolean requestRespawn(Object worldObj, UUID ownerId, Object senderObj) {
        if (worldObj == null || ownerId == null) {
            setError("worldObj ou ownerId inválidos.");
            return false;
        }

        // 1~2s depois do teleport do player, o NPC spawna perto e avisa.
        long now = System.currentTimeMillis();

        long delay = 1000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(1001L);
        long at = now + delay;
        String msg = "Estou chegando.";

        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec == null) {
            pendingRespawns.put(ownerId, new PendingRespawn(worldObj, senderObj, at, msg));
            return true;
        }

        rec.respawnRequested = true;
        rec.respawnWorldObj = worldObj;
        rec.respawnSenderObj = senderObj;
        rec.respawnAtMillis = at;
        rec.respawnMessage = msg;

        // Força despawn do atual (no mundo antigo), e o respawn acontecerá quando remover.
        return despawn(rec.worldObj != null ? rec.worldObj : worldObj, ownerId);
    }

    public boolean despawn(Object worldObj, UUID ownerId) {
        if (ownerId == null) {
            setError("ownerId null.");
            return false;
        }

        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec == null) {
            setError("Nenhum NPC registrado para este player.");
            return false;
        }

        // marca DESPAWNING imediatamente (impede re-spawn)
        rec.state = State.DESPAWNING;

        // ✅ Salva imediatamente o que importa (mochila)
        // Mesmo que o despawn físico ocorra no próximo tick, o estado fica persistido.
        try {
            if (rec.backpack != null) {
                AmigoPersistence.saveBackpack(ownerId, rec.backpack);
            }
            try { AmigoPersistence.saveSwordState(ownerId, rec.level, rec.equippedWeaponId); } catch (Throwable ignored2) {}
        } catch (Throwable ignored) {}

        // prefere o world salvo no spawn
        Object worldToUse = (rec.worldObj != null ? rec.worldObj : worldObj);
        if (worldToUse == null) {
            setError("World indisponível para despawn.");
            return false;
        }

        // Se ainda não tem ref (spawn ainda não executou),
        // retorna true: quando o spawn terminar, ele verá DESPAWNING e removerá.
        if (rec.refObj == null) {
            return true;
        }

        boolean queued = HytaleBridge.worldExecute(worldToUse, () -> {
            try {
                Object componentStore = getComponentStoreFromWorld(worldToUse);
                if (componentStore == null) {
                    setError("Não consegui obter Store via world.getEntityStore().getStore().");
                    return;
                }

                if (!doRemoveEntity(componentStore, rec.refObj)) {
                    // LAST_ERROR já setado dentro
                    return;
                }

                finalizeRemove(ownerId, rec);

            } catch (Throwable t) {
                setError("despawn() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });

        if (!queued) {
            setError("world.execute falhou: " + HytaleBridge.getLastError());
            return false;
        }

        return true;
    }

    /**
     * Despawn usando o world guardado no spawn (útil para cleanup em logout).
     */
    public boolean despawnStored(UUID ownerId) {
        if (ownerId == null) return false;
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec == null) return false;
        if (rec.worldObj == null) return false;
        return despawn(rec.worldObj, ownerId);
    }

    /**
     * Finaliza remoção do record (remove índices) e, se existir, executa re-spawn automático.
     * Deve ser chamado APÓS a entidade ter sido removida do store.
     */
    private void finalizeRemove(UUID ownerId, NpcRecord rec) {
        if (ownerId == null || rec == null) return;

        // remove do índice rápido (ref pode já ter sido nulado)
        try {
            if (rec.refObj != null) {
                amigoRefs.remove(rec.refObj);
            }
        } catch (Throwable ignored) {}

        boolean doRespawn = rec.respawnRequested && rec.respawnWorldObj != null;
        Object respawnWorld = rec.respawnWorldObj;
        Object respawnSender = rec.respawnSenderObj;

        rec.respawnRequested = false;
        rec.respawnWorldObj = null;
        rec.respawnSenderObj = null;

        npcRefPorPlayer.remove(ownerId, rec);

        if (doRespawn) {
            long now = System.currentTimeMillis();
            long at = rec.respawnAtMillis > 0L ? rec.respawnAtMillis : (now + 1000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(1001L));
            String msg = rec.respawnMessage;
            pendingRespawns.put(ownerId, new PendingRespawn(respawnWorld, respawnSender, at, msg));
        }
    }

    /**
     * Mochila persistente do Amigo (45 slots).
     * Retorna sempre um container (cria/puxa do disco se necessário).
     */
    public SimpleItemContainer getOrLoadBackpack(UUID ownerId) {
        if (ownerId == null) return new SimpleItemContainer((short) 45);
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec != null) {
            if (rec.backpack == null) {
                rec.backpack = AmigoPersistence.loadBackpack(ownerId);
            }
            return rec.backpack;
        }
        return AmigoPersistence.loadBackpack(ownerId);
    }

    /**
     * Usado por sistemas/eventos (ex.: filtro de dano) para identificar se um Ref pertence ao AmigoNPC.
     */
    public boolean isAmigoRef(Object refObj) {
        return refObj != null && amigoRefs.containsKey(refObj);
    }

    /** Retorna o dono (UUID) para um Ref de entidade do Amigo, ou null. */
    public UUID getOwnerFromRef(Object refObj) {
        return refObj == null ? null : amigoRefs.get(refObj);
    }

    /** Se o NPC do owner está em estado DOWNED. */
    public boolean isDowned(UUID ownerId) {
        NpcRecord rec = ownerId == null ? null : npcRefPorPlayer.get(ownerId);
        return rec != null && rec.downed;
    }

    /** Marca como DOWNED e agenda revive automático (40s). */
    public void markDowned(UUID ownerId) {
        NpcRecord rec = ownerId == null ? null : npcRefPorPlayer.get(ownerId);
        if (rec == null) return;
        if (rec.downed) return;
        rec.downed = true;
        rec.downedUntilMillis = System.currentTimeMillis() + 40_000L;

        // Ao entrar em DOWNED, interrompe combate/assist imediatamente
        rec.combatUntilMillis = 0L;
        rec.combatTargetRefObj = null;
        rec.assistUntilMillis = 0L;
        rec.assistTargetRefObj = null;
        rec.chaseDisengaged = false;
        rec.targetLostSinceMillis = 0L;
        rec.targetStuckSinceMillis = 0L;
        rec.lastTargetHorizontal = -1.0;
        rec.lastTargetSampleMillis = 0L;

        // E limpa os marked targets para não continuar correndo com um alvo antigo.
        try {
            if (rec.worldObj != null && (rec.refObj instanceof Ref)) {
                Object worldObj = rec.worldObj;
                HytaleBridge.worldExecute(worldObj, () -> {
                    try {
                        Object storeObj = getComponentStoreFromWorld(worldObj);
                        if (storeObj == null) return;
                        @SuppressWarnings("unchecked")
                        Store<EntityStore> store = (Store<EntityStore>) storeObj;
                        @SuppressWarnings("unchecked")
                        Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

                        Object npcEntityObj = getComponentFromStore(store, npcRef, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                            setMarkedTargetOnNpcEntity(npcEntityObj, "LockedTarget", null);
                            setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", null);
                            // Volta estado do role para Idle (best-effort)
                            setRoleStateOnNpcEntity(npcEntityObj, npcRef, store, false);
                        }

                        // Opcional: força flags do MovementStates para parar imediatamente (MovementStates é um struct)
                        try {
                            MovementStatesComponent ms = store.getComponent(npcRef, MovementStatesComponent.getComponentType());
                            if (ms != null) {
                                MovementStates s = ms.getMovementStates();
                                if (s == null) s = new MovementStates();

                                s.onGround = true;
                                s.idle = true;
                                s.horizontalIdle = true;
                                s.walking = false;
                                s.running = false;
                                s.sprinting = false;

                                ms.setMovementStates(s);
                                // manter "sent" junto ajuda o client a refletir rápido
                                ms.setSentMovementStates(new MovementStates(s));
                                store.putComponent(npcRef, MovementStatesComponent.getComponentType(), ms);
                            }
                        } catch (Throwable ignored2) {}
                    } catch (Throwable ignored2) {}
                });
            }
        } catch (Throwable ignored) {}

        // ✅ Punição por morte (sem deslevelar): remove % do totalXp limitado ao progresso do nível atual
        try {
            long before = rec.totalXp;
            long after = XpProgression.applyDeathPenalty(before);
            rec.totalXp = after;
            rec.npcLevelCached = XpProgression.levelFromTotalXp(after);
            AmigoPersistence.saveTotalXp(ownerId, after);
        } catch (Throwable ignored) {}

        // Atualiza HUD imediatamente (XP/progresso do nível), se o NPC estiver ativo
        try {
            if (rec.state == State.ACTIVE && rec.worldObj != null && (rec.refObj instanceof Ref)) {
                Object worldObj = rec.worldObj;
                HytaleBridge.worldExecute(worldObj, () -> {
                    try {
                        Object storeObj = getComponentStoreFromWorld(worldObj);
                        if (storeObj == null) return;
                        @SuppressWarnings("unchecked")
                        Store<EntityStore> store = (Store<EntityStore>) storeObj;
                        @SuppressWarnings("unchecked")
                        Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;
                        updateNpcHud(store, npcRef, rec);
                    } catch (Throwable ignored2) {}
                });
            }
        } catch (Throwable ignored3) {}
    }


    // =========================================================
    // Progressão: XP total + level calculado + scaling de HP/DEF (cap lvl 100)
    // =========================================================

    /** Level calculado a partir do totalXp (cached no record quando possível). */
    public int getNpcLevel(UUID ownerId) {
        if (ownerId == null) return 1;
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec != null) return Math.max(1, rec.npcLevelCached);
        try {
            long totalXp = AmigoPersistence.loadTotalXp(ownerId);
            return XpProgression.levelFromTotalXp(totalXp);
        } catch (Throwable ignored) {}
        return 1;
    }

    /**
     * Mitigação de dano recebida do NPC via DEF scaling.
     * Implementação minimalista: como o multiplicador vai até 10x no lvl 100,
     * o dano recebido é dividido por esse multiplicador.
     */
    public float mitigateIncomingDamage(UUID ownerId, float incomingAmount) {
        if (ownerId == null) return incomingAmount;
        float amount = Math.max(0f, incomingAmount);
        try {
            NpcRecord rec = npcRefPorPlayer.get(ownerId);
            long totalXp = rec != null ? rec.totalXp : AmigoPersistence.loadTotalXp(ownerId);
            int lvl = XpProgression.levelFromTotalXp(totalXp);
            double mult = StatScaling.multiplier(lvl); // cap 100
            if (mult <= 0.0) return amount;
            return (float) (amount / mult);
        } catch (Throwable ignored) {}
        return amount;
    }

    private static volatile Integer DEF_STAT_INDEX = null;

    /** Tenta resolver um índice de stat de DEF/ARMOR via reflection (best-effort). */
    private static int resolveDefenseStatIndex() {
        Integer cached = DEF_STAT_INDEX;
        if (cached != null) return cached;
        synchronized (AmigoNpcManager.class) {
            cached = DEF_STAT_INDEX;
            if (cached != null) return cached;

            int idx = -1;
            String[] candidates = new String[] {
                    "getDefense", "getDefence", "getArmor", "getArmour",
                    "getPhysicalDefense", "getPhysicalDefence", "getProtection",
                    "getResistance", "getPhysicalResistance"
            };

            for (String name : candidates) {
                try {
                    Method m = DefaultEntityStatTypes.class.getMethod(name);
                    Object out = m.invoke(null);
                    if (out instanceof Integer) {
                        idx = (Integer) out;
                        break;
                    }
                } catch (Throwable ignored) {}
            }

            DEF_STAT_INDEX = idx;
            return idx;
        }
    }

    /**
     * Captura baseHp/baseDef (1x) e aplica scaling de HP/DEF conforme level.
     * healToFull=true: seta HP atual para o HP máximo escalado.
     */
    private void applyNpcScaling(Store<EntityStore> store,
                                Ref<EntityStore> npcRef,
                                UUID ownerId,
                                NpcRecord rec,
                                boolean healToFull) {
        if (store == null || npcRef == null || ownerId == null || rec == null) return;

        try {
            // garante tabelas
            XpProgression.init();

            EntityStatMap stats = store.ensureAndGetComponent(npcRef, EntityStatMap.getComponentType());
            if (stats == null) return;

            int healthIdx = DefaultEntityStatTypes.getHealth();

            // Captura baseHp (1x): usa o "max" atual como referência natural do lvl 1
            boolean updatedBases = false;

            if (rec.baseHp <= 0L) {
                try {
                    stats.maximizeStatValue(healthIdx);
                } catch (Throwable ignored) {}
                float base = 0f;
                try {
                    base = stats.get(healthIdx).get();
                } catch (Throwable ignored) {}
                long baseHp = Math.max(1L, Math.round(base));
                rec.baseHp = baseHp;
                AmigoPersistence.saveBaseHp(ownerId, baseHp);
                updatedBases = true;
            }

            if (rec.baseDef < 0L) {
                long baseDef = 1L;
                int defIdx = resolveDefenseStatIndex();
                if (defIdx >= 0) {
                    try {
                        stats.maximizeStatValue(defIdx);
                        float defVal = stats.get(defIdx).get();
                        baseDef = Math.max(0L, Math.round(defVal));
                    } catch (Throwable ignored) {
                        baseDef = 1L;
                    }
                }
                rec.baseDef = baseDef;
                AmigoPersistence.saveBaseDef(ownerId, baseDef);
                updatedBases = true;
            }

            // Level calculado
            int level = XpProgression.levelFromTotalXp(rec.totalXp);
            rec.npcLevelCached = Math.max(1, level);

            // Scaling (cap lvl 100)
            long hpMax = StatScaling.scaledHp(rec.baseHp, level);
            long defValScaled = StatScaling.scaledDef(rec.baseDef, level);

            // Aplica HP: apenas ajusta o HP atual (o jogo pode ter um "max" interno,
            // mas como nosso damage system lê o stat atual, isso já aumenta o pool.)
            float curHp = 0f;
            try {
                curHp = stats.get(healthIdx).get();
            } catch (Throwable ignored) {}

            float newHp;
            if (healToFull) {
                newHp = (float) hpMax;
            } else {
                newHp = Math.min(curHp, (float) hpMax);
                if (newHp <= 0f) newHp = 1f;
            }

            stats.setStatValue(healthIdx, newHp);

            // Aplica DEF em stat (se existir) como best-effort
            int defIdx = resolveDefenseStatIndex();
            if (defIdx >= 0) {
                try {
                    stats.setStatValue(defIdx, (float) defValScaled);
                } catch (Throwable ignored) {}
            }

            // persistência de totalXp (se for primeira vez, garantimos que exista)
            if (updatedBases) {
                // nada extra
            }

            // garante que a alteração vá para o store
            try {
                store.putComponent(npcRef, EntityStatMap.getComponentType(), stats);
            } catch (Throwable ignored) {}

            // HUD (nome acima do NPC): level + barra de XP (fica junto da barra de vida)
            try {
                updateNpcHud(store, npcRef, rec);
            } catch (Throwable ignored) {}

        } catch (Throwable ignored) {}
    }

    /**
     * Ganha XP total do NPC (totalXp) e atualiza o level calculado.
     * Por padrão não cura (apenas aplica clamp se necessário).
     */
    private void addNpcXp(Store<EntityStore> store, Ref<EntityStore> npcRef, UUID ownerId, NpcRecord rec, long gain) {
        if (gain <= 0L || ownerId == null || rec == null || store == null || npcRef == null) return;

        long before = rec.totalXp;
        long after = before + gain;
        if (after < before) after = Long.MAX_VALUE; // overflow protection

        rec.totalXp = after;
        AmigoPersistence.saveTotalXp(ownerId, after);

        int newLevel = XpProgression.levelFromTotalXp(after);
        if (newLevel != rec.npcLevelCached) {
            rec.npcLevelCached = newLevel;
            // reaplica HP/DEF (cap lvl 100). Não cura aqui.
            applyNpcScaling(store, npcRef, ownerId, rec, false);
        }


        // Mantém nível da espada sincronizado com o level calculado (HUD/combate usam nível da espada)
        if (newLevel != rec.level) {
            rec.level = SwordProgression.clampLevel(newLevel);
            applySwordWeaponNow(store, npcRef, ownerId, rec, true);
        }

        // Atualiza HUD em toda mudança de XP (mesmo que o level não mude)
        try {
            updateNpcHud(store, npcRef, rec);
        } catch (Throwable ignored) {}
    }

    /**
     * HUD simples acima do NPC (DisplayName): level + barra de XP do level atual.
     * Fica junto da barra de vida padrão do jogo.
     */
    private static void updateNpcHud(Store<EntityStore> store, Ref<EntityStore> npcRef, NpcRecord rec) {
        if (store == null || npcRef == null || rec == null) return;

        XpProgression.init();
        long totalXp = Math.max(0L, rec.totalXp);

        // Level exibido = nível da espada (combate/armas usam este nível)
        int computedLevel = Math.max(1, XpProgression.levelFromTotalXp(totalXp));
        int level = SwordProgression.clampLevel(rec.level);

        // Mantém tudo sincronizado com o totalXp (fonte de verdade do XP)
        if (level != computedLevel) {
            level = computedLevel;
            rec.level = computedLevel;
        }
        if (rec.npcLevelCached != computedLevel) {
            rec.npcLevelCached = computedLevel;
        }

        long start = XpProgression.xpStartOfLevel(level);
        long need = Math.max(1L, XpProgression.xpToNext(level));
        long into = (start == Long.MAX_VALUE) ? 0L : Math.max(0L, totalXp - start);
        if (into > need) into = need;

        int segments = 12;
        double p = Math.max(0.0, Math.min(1.0, (double) into / (double) need));
        int filled = (int) Math.round(p * segments);
        if (filled < 0) filled = 0;
        if (filled > segments) filled = segments;

        String bar = "[" + repeatChar('=', filled) + repeatChar('-', segments - filled) + "]";
        String text = "Lv " + level + " " + bar + " " + into + "/" + need;

        try {
            Nameplate np = store.ensureAndGetComponent(npcRef, Nameplate.getComponentType());
            np.setText(text);
        } catch (Throwable ignored) {}
    }

    private static String repeatChar(char c, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }


    // =========================================================
    // Progressão: nível de espada + troca automática de armas (corpo a corpo)
    // =========================================================

    /** Retorna o nível atual (carrega do disco se o NPC não estiver ativo). */
    public int getSwordLevel(UUID ownerId) {
        if (ownerId == null) return 1;
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec != null) return SwordProgression.clampLevel(rec.level);

        // fonte de verdade: totalXp -> level calculado
        long xp = Math.max(0L, AmigoPersistence.loadTotalXp(ownerId));
        return SwordProgression.clampLevel(XpProgression.levelFromTotalXp(xp));
    }

    /**
     * Altera nível em +/- (usado por comandos admin e, futuramente, por XP).
     * - salva sempre
     * - se o NPC existir: equipa arma imediatamente e manda mensagem aleatória
     */
    public int changeSwordLevel(UUID ownerId, int delta, boolean announce) {
        if (ownerId == null || delta == 0) return getSwordLevel(ownerId);

        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        int oldLvl = (rec != null) ? SwordProgression.clampLevel(rec.level) : SwordProgression.clampLevel(AmigoPersistence.loadSwordLevel(ownerId));
        int newLvl = SwordProgression.clampLevel(oldLvl + delta);
        if (newLvl == oldLvl) return newLvl;

        // atualiza memória
        if (rec != null) {
            rec.level = newLvl;
        }

        // determina arma e salva
        String weaponId = SwordProgression.weaponIdForLevel(newLvl);
        if (rec != null) {
            rec.equippedWeaponId = weaponId;
        }
        AmigoPersistence.saveSwordState(ownerId, newLvl, weaponId);

        // ✅ Mantém o totalXp consistente com o nível de espada (HUD/XP/armas)
        XpProgression.init();
        long newTotalXp = XpProgression.xpStartOfLevel(newLvl);
        if (newTotalXp < 0L) newTotalXp = 0L;
        if (rec != null) {
            rec.totalXp = newTotalXp;
            rec.npcLevelCached = newLvl;
        }
        AmigoPersistence.saveTotalXp(ownerId, newTotalXp);

        // se NPC está ativo, equipa na hora (pode trocar em combate)
        if (rec != null && rec.state == State.ACTIVE && rec.worldObj != null && rec.refObj != null) {
            Object worldObj = rec.worldObj;

            // Equipar
            HytaleBridge.worldExecute(worldObj, () -> {
                try {
                    Object storeObj = getComponentStoreFromWorld(worldObj);
                    if (storeObj == null) return;
                    @SuppressWarnings("unchecked")
                    Store<EntityStore> store = (Store<EntityStore>) storeObj;
                    @SuppressWarnings("unchecked")
                    Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;
                    applySwordWeaponNow(store, npcRef, ownerId, rec, true);
                    // reaplica scaling (HP/DEF) e HUD com base no novo nível/XP
                    try { applyNpcScaling(store, npcRef, ownerId, rec, false); } catch (Throwable ignored2) {}
                    try { updateNpcHud(store, npcRef, rec); } catch (Throwable ignored2) {}
                } catch (Throwable ignored) {}
            });

            // Mensagens
            if (announce) {
                String line;
                if (delta > 0 && SwordProgression.crossedAnyMilestone(oldLvl, newLvl)) {
                    line = SwordMessages.pickEpic(newLvl);
                } else if (delta > 0) {
                    line = SwordMessages.pickUp(newLvl);
                } else {
                    line = SwordMessages.pickDown(newLvl);
                }
                sendToOwner(worldObj, ownerId, line);
            }
        }

        return newLvl;
    }

    private void sendToOwner(Object worldObj, UUID ownerId, String text) {
        if (worldObj == null || ownerId == null || text == null || text.isBlank()) return;
        HytaleBridge.worldExecute(worldObj, () -> {
            try {
                Object storeObj = getComponentStoreFromWorld(worldObj);
                if (storeObj == null) return;
                @SuppressWarnings("unchecked")
                Store<EntityStore> store = (Store<EntityStore>) storeObj;

                @SuppressWarnings("unchecked")
                Ref<EntityStore> ownerRef = (Ref<EntityStore>) invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
                if (ownerRef == null) return;

                Player player = store.getComponent(ownerRef, Player.getComponentType());
                if (player == null) return;

                player.sendMessage(Message.raw("§7[AmigoNPC] " + text));
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Equipar arma e atualizar persistência (idempotente).
     *
     * Importante: mesmo se rec.equippedWeaponId já estiver correto, precisamos garantir
     * que o NPC realmente está com a arma na mão (hotbar slot 0), senão ele aparece desarmado.
     *
     * @param saveNow se true, persiste após equipar
     */
    private void applySwordWeaponNow(Store<EntityStore> store, Ref<EntityStore> npcRef, UUID ownerId, NpcRecord rec, boolean saveNow) {
        if (store == null || npcRef == null || rec == null || ownerId == null) return;

        int lvl = SwordProgression.clampLevel(rec.level);
        String expected = SwordProgression.weaponIdForLevel(lvl);
        if (expected == null || expected.isBlank()) return;

        boolean slotOk = isHotbar0Item(store, npcRef, expected);

        // Se já está correto no record e no slot 0, só persiste (se pedir) e sai
        if (expected.equals(rec.equippedWeaponId) && slotOk) {
            if (saveNow) {
                AmigoPersistence.saveSwordState(ownerId, lvl, expected);
            }
            return;
        }

        boolean equipped = equipWeaponInHotbar0(store, npcRef, expected);
        if (equipped) {
            rec.equippedWeaponId = expected;
            if (saveNow) {
                AmigoPersistence.saveSwordState(ownerId, lvl, expected);
            }
            debugEquip(rec, ownerId, "Equipado: " + expected + " (lvl " + lvl + ")");
        } else {
            debugEquip(rec, ownerId, "FALHA ao equipar '" + expected + "' (lvl " + lvl + "). Hotbar0=" + getHotbar0ItemId(store, npcRef));
        }
    }

    /** Lê o itemId do slot 0 da hotbar (ou null). */
    private String getHotbar0ItemId(Store<EntityStore> store, Ref<EntityStore> npcRef) {
        try {
            NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
            if (npc == null) return null;
            Inventory inv = npc.getInventory();
            if (inv == null) return null;
            ItemContainer hotbar = inv.getHotbar();
            if (hotbar == null) return null;
            ItemStack st = hotbar.getItemStack((short) 0);
            if (st == null || st.isEmpty()) return null;
            return st.getItemId();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isHotbar0Item(Store<EntityStore> store, Ref<EntityStore> npcRef, String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        String cur = getHotbar0ItemId(store, npcRef);
        return itemId.equals(cur);
    }

    /**
     * Define a arma no slot 0 da hotbar e marca como ativa.
     *
     * Nota: alguns builds só mostram o item na mão quando:
     * - o Inventory está associado à entidade (inv.setEntity)
     * - usandoToolsItem = false (senão ele mostra tool-slot)
     */
    private boolean equipWeaponInHotbar0(Store<EntityStore> store, Ref<EntityStore> npcRef, String itemId) {
        try {
            NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
            if (npc == null) return false;

            Inventory inv = npc.getInventory();
            if (inv == null) inv = new Inventory();

            // importante para o item aparecer na mão
            try { inv.setEntity(npc); } catch (Throwable ignored) {}
            try { inv.setUsingToolsItem(false); } catch (Throwable ignored) {}

            ItemContainer hotbar = inv.getHotbar();
            if (hotbar == null) {
                // fallback: inventário novo
                inv = new Inventory();
                try { inv.setEntity(npc); } catch (Throwable ignored) {}
                try { inv.setUsingToolsItem(false); } catch (Throwable ignored) {}
                hotbar = inv.getHotbar();
                if (hotbar == null) return false;
            }

            hotbar.setItemStackForSlot((short) 0, new ItemStack(itemId, 1));

            // valida se o item existe (alguns ids inválidos viram stack vazio)
            try {
                ItemStack st = hotbar.getItemStack((short) 0);
                if (st == null || st.isEmpty()) {
                    return false;
                }
            } catch (Throwable ignored) {}

            inv.setActiveHotbarSlot((byte) 0);
            inv.markChanged();

            // garante broadcast de equipamento
            npc.setInventory(inv);
            npc.invalidateEquipmentNetwork();
            return true;

        } catch (Throwable ignored) {
            return false;
        }
    }

    private void debugEquip(NpcRecord rec, UUID ownerId, String msg) {
        if (rec == null || ownerId == null) return;
        if (!rec.debugLogEnabled) return;
        long now = System.currentTimeMillis();
        if (now < rec.debugNextEquipMillis) return;
        rec.debugNextEquipMillis = now + 1200L;
        sendToOwner(rec.worldObj, ownerId, "DEBUG-EQUIP: " + msg);
    }

    private void debugCombat(NpcRecord rec, UUID ownerId, String msg) {
        if (rec == null || ownerId == null) return;
        if (!rec.debugLogEnabled) return;
        long now = System.currentTimeMillis();
        if (now < rec.debugNextCombatMillis) return;
        rec.debugNextCombatMillis = now + 900L;
        sendToOwner(rec.worldObj, ownerId, "DEBUG-COMBAT: " + msg);
    }

    private void debugAttack(NpcRecord rec, UUID ownerId, String msg) {
        if (rec == null || ownerId == null) return;
        if (!rec.debugLogEnabled) return;
        long now = System.currentTimeMillis();
        if (now < rec.debugNextAttackMillis) return;
        rec.debugNextAttackMillis = now + 900L;
        sendToOwner(rec.worldObj, ownerId, "DEBUG-ATTACK: " + msg);
    }

    // Best-effort: usa Item.itemLevel como base de dano (porque o damage real do jogo
    // vem do pipeline interno de Interactions). Serve só para testes.
    private static double getWeaponBaseDamage(String itemId) {
        if (itemId == null || itemId.isBlank()) return 4.0;
        try {
            Item it = Item.getAssetMap().getAsset(itemId);
            if (it == null) return 4.0;
            int lvl = it.getItemLevel();
            // escala simples e estável: itemLevel 0..?? -> dano 4..~15
            return 4.0 + Math.max(0, lvl) * 0.9;
        } catch (Throwable ignored) {
            return 4.0;
        }
    }

    private static boolean refEq(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        try {
            return a.equals(b);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // =========================================================
    // Combate (etapa inicial)
    // =========================================================

    /**
     * Chamado por sistemas/eventos quando o dono sofre dano.
     * Faz o NPC priorizar o agressor por alguns segundos (prioridade máxima sobre follow).
     */
    public void startCombat(UUID ownerId, Object attackerRefObj) {
        if (ownerId == null || attackerRefObj == null) return;
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec == null) return;
        if (rec.downed) return;
        if (rec.refObj == null) return;

        // Evita auto-target esquisito
        if (attackerRefObj == rec.refObj) return;

        rec.combatTargetRefObj = attackerRefObj;
        rec.combatUntilMillis = System.currentTimeMillis() + COMBAT_WINDOW_MILLIS;

        // Ao entrar em combate real (alguém te atingiu), limpamos a assistência
        rec.assistTargetRefObj = null;
        rec.assistUntilMillis = 0L;
        debugCombat(rec, ownerId, "startCombat: agressorRef=" + attackerRefObj + " (defender=" + (rec.defendeEnabled ? "ON" : "OFF") + ")");
    }

    /**
     * Chamado quando o player dá dano em um alvo (primeiro ataque).
     * Só vale se /amigo defender estiver ON e se não existir agressor ativo no momento.
     */

    public void startAssist(UUID ownerId, Object targetRefObj) {
        if (ownerId == null || targetRefObj == null) return;
        NpcRecord rec = npcRefPorPlayer.get(ownerId);
        if (rec == null) return;
        if (!rec.defendeEnabled) return;
        if (rec.downed) return;
        if (rec.refObj == null) return;
        if (targetRefObj == rec.refObj) return;

        long now = System.currentTimeMillis();
        // Se alguem ja te atacou (agressor ativo), nao troca para assistencia
        if (getActiveCombatTarget(rec, now) != null) return;

        // Mantem o primeiro alvo de assistencia ate acabar (nao troca a cada hit)
        if (rec.assistTargetRefObj != null) return;

        rec.assistTargetRefObj = targetRefObj;
        rec.assistUntilMillis = 0L; // sem timeout enquanto houver alvo
        debugCombat(rec, ownerId, "startAssist: targetRef=" + targetRefObj);
    }


/**
 * Registra um "combat tag" (alvo + posição + timestamp) para validar loot pós-combate.
 * Chamado quando o dono (via Assist) ou o NPC dão dano / matam.
 *
 * Regras:
 * - não salva em disco (é janela curta)
 * - mantém poucos registros (cap) para não crescer
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public void recordCombatTag(UUID ownerId, Object targetRefObj, Store<EntityStore> store) {
    if (ownerId == null || targetRefObj == null || store == null) return;
    NpcRecord rec = npcRefPorPlayer.get(ownerId);
    if (rec == null) return;
    if (rec.refObj == null) return;
    if (rec.downed) return;
    if (!(targetRefObj instanceof Ref)) return;

    long now = System.currentTimeMillis();
    rec.lastCombatTagMillis = now;
    rec.wasInCombat = true;


    try {
        Ref<EntityStore> targetRef = (Ref<EntityStore>) targetRefObj;
        TransformComponent tc = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (tc == null || tc.getPosition() == null) return;

        Vector3d pos = tc.getPosition();
        rec.lastBattleCenter = pos;

        synchronized (rec.combatTags) {
            // Atualiza se já existe
            CombatTag existing = null;
            for (CombatTag t : rec.combatTags) {
                if (t != null && refEq(t.targetRefObj, targetRefObj)) { existing = t; break; }
            }
            if (existing != null) {
                existing.pos = pos;
                existing.lastSeenMillis = now;
            } else {
                if (rec.combatTags.size() >= COMBAT_TAG_MAX) {
                    rec.combatTags.remove(0);
                }
                rec.combatTags.add(new CombatTag(targetRefObj, pos, now));
            }
        }
    } catch (Throwable ignored) {}
}


    /** Retorna o alvo de assistencia (se ativo), ou null. */
    private Object getActiveAssistTarget(NpcRecord rec, long now) {
        if (rec == null) return null;
        return rec.assistTargetRefObj;
    }

    private void clearAssist(NpcRecord rec) {
        if (rec == null) return;
        rec.assistTargetRefObj = null;
        rec.assistUntilMillis = 0L;
    }

    private static boolean isValidEntityRef(Object store, Object refObj) {
        if (store == null || refObj == null) return false;
        try {
            Object tc = getComponentFromStore(store, refObj, TransformComponent.getComponentType());
            return tc != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Considera "vivo" quando:
     * - ainda tem TransformComponent
     * - e (se houver EntityStatMap) health > 0
     *
     * Motivo: o corpo pode demorar a sumir e o NPC ficava "esperando" no alvo morto.
     */
    private static boolean isAliveEntityRef(Object store, Object refObj) {
        if (!isValidEntityRef(store, refObj)) return false;
        try {
            // Se o motor já marcou como morto, não trate como vivo (evita ficar preso no cadáver)
            try {
                Object dc = getComponentFromStore(store, refObj, DeathComponent.getComponentType());
                if (dc != null) return false;
            } catch (Throwable ignored) {}

            Object statsObj = getComponentFromStore(store, refObj, EntityStatMap.getComponentType());
            if (!(statsObj instanceof EntityStatMap stats)) return true; // sem stats => assume vivo
            try {
                float hp = stats.get(DefaultEntityStatTypes.getHealth()).get();
                return hp > 0f;
            } catch (Throwable ignored) {
                return true;
            }
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** Altura do alvo (best-effort) via BoundingBox. Usado para tolerância vertical e reposicionamento. */
    private static double getEntityHeight(Object store, Object refObj) {
        if (store == null || refObj == null) return 1.8;
        try {
            Object bbObj = getComponentFromStore(store, refObj, BoundingBox.getComponentType());
            if (bbObj instanceof BoundingBox bb) {
                try {
                    var box = bb.getBoundingBox();
                    if (box != null) {
                        double h = box.height();
                        if (h > 0.05) return h;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return 1.8;
    }

    /**
     * Raio aproximado no plano XZ (best-effort) via BoundingBox.
     * Importante: a distância centro-a-centro pode ser enganosa para mobs maiores,
     * então subtrair o raio do alvo melhora o "alcance" real sem aumentar a espada.
     */
    private static double getEntityRadiusXZ(Object store, Object refObj) {
        if (store == null || refObj == null) return 0.45;
        try {
            Object bbObj = getComponentFromStore(store, refObj, BoundingBox.getComponentType());
            if (bbObj instanceof BoundingBox bb) {
                Object box = null;
                try { box = bb.getBoundingBox(); } catch (Throwable ignored) {}
                if (box != null) {
                    double w = readBoxDim(box, "width", "getWidth", "xSize", "getXSize");
                    double d = readBoxDim(box, "depth", "getDepth", "zSize", "getZSize");
                    if (w > 0.05 && d > 0.05) {
                        double r = 0.5 * Math.max(w, d);
                        return clamp(r, 0.20, 2.00);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0.45;
    }

    private static double readBoxDim(Object box, String... methodNames) {
        if (box == null || methodNames == null) return -1;
        for (String name : methodNames) {
            if (name == null || name.isBlank()) continue;
            try {
                java.lang.reflect.Method m = box.getClass().getMethod(name);
                Object v = m.invoke(box);
                if (v instanceof Number n) {
                    double d = n.doubleValue();
                    if (d > 0.0) return d;
                }
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static void tickAssistHousekeeping(NpcRecord rec, Object store, long now) {
        if (rec == null) return;

        if (!rec.defendeEnabled) {
            rec.assistTargetRefObj = null;
            rec.assistUntilMillis = 0L;
            return;
        }

        if (rec.assistTargetRefObj != null) {
            if (!isAliveEntityRef(store, rec.assistTargetRefObj)) {
                rec.assistTargetRefObj = null;
                rec.assistUntilMillis = now + ASSIST_GRACE_MILLIS;
            }
            return;
        }

        // Sem alvo: termina apos 3s sem inimigos.
        if (rec.assistUntilMillis > 0L && now > rec.assistUntilMillis) {
            rec.assistUntilMillis = 0L;
        }
    }


    /**
     * Auto-alvo do Defender:
     * - Se nao existe agressor e nao existe assist atual, tenta pegar a entidade viva mais proxima do dono
     *   dentro de um raio fixo (horizontal) e tolerancia vertical.
     * - Nao mira no dono, no proprio NPC, nem em players quando amigopvp estiver OFF.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object findNearestDefenderTarget(Store<EntityStore> store, NpcRecord rec, Object ownerRefObj, com.hypixel.hytale.math.vector.Vector3d ownerPos) {
        if (store == null || rec == null || ownerRefObj == null || ownerPos == null) return null;
        if (!(ownerRefObj instanceof Ref) || !(rec.refObj instanceof Ref)) return null;

        Ref<EntityStore> ownerRef = (Ref<EntityStore>) ownerRefObj;
        Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

        final double r = DEFENDER_AUTO_ACQUIRE_RADIUS;
        final double r2 = r * r;
        final double maxDy = DEFENDER_AUTO_ACQUIRE_MAX_DY;

        final Object[] bestRef = new Object[1];
        final double[] bestD2 = new double[]{Double.POSITIVE_INFINITY};

        try {
            store.forEachChunk((chunk, cb) -> {
                int sz;
                try { sz = chunk.size(); } catch (Throwable t) { return; }

                for (int i = 0; i < sz; i++) {
                    Ref<EntityStore> ref;
                    try { ref = (Ref<EntityStore>) chunk.getReferenceTo(i); } catch (Throwable t) { continue; }
                    if (ref == null) continue;
                    if (refEq(ref, ownerRef) || refEq(ref, npcRef)) continue;

                    // Nao perseguir players quando PvP estiver OFF
                    try {
                        Player maybePlayer = (Player) chunk.getComponent(i, Player.getComponentType());
                        if (maybePlayer != null && !pvpEnabled) continue;
                    } catch (Throwable ignored) {}

                    // Ignora entidades já marcadas como mortas (cadáver / death animation)
                    try {
                        DeathComponent dc = (DeathComponent) chunk.getComponent(i, DeathComponent.getComponentType());
                        if (dc != null) continue;
                    } catch (Throwable ignored) {}

                    // Ignora entidades já marcadas como mortas (cadáver / death animation)
                    try {
                        DeathComponent dc = (DeathComponent) chunk.getComponent(i, DeathComponent.getComponentType());
                        if (dc != null) continue;
                    } catch (Throwable ignored) {}

                    TransformComponent tc;
                    try { tc = (TransformComponent) chunk.getComponent(i, TransformComponent.getComponentType()); } catch (Throwable t) { continue; }
                    if (tc == null || tc.getPosition() == null) continue;

                    var p = tc.getPosition();
                    double dy = Math.abs(p.getY() - ownerPos.getY());
                    if (dy > maxDy) continue;

                    double dx = p.getX() - ownerPos.getX();
                    double dz = p.getZ() - ownerPos.getZ();
                    double d2 = dx * dx + dz * dz;
                    if (d2 > r2) continue;

                    // So entidades "vivas" (tem stats + vida > 0). Ajuda a nao mirar em projeteis/itens.
                    try {
                        EntityStatMap stats = (EntityStatMap) chunk.getComponent(i, EntityStatMap.getComponentType());
                        if (stats == null) continue;
                        var hp = stats.get(DefaultEntityStatTypes.getHealth());
                        if (hp != null && hp.get() <= 0.0f) continue;
                    } catch (Throwable ignored) {
                        continue;
                    }

                    if (d2 < bestD2[0]) {
                        bestD2[0] = d2;
                        bestRef[0] = ref;
                    }
                }
            });
        } catch (Throwable ignored) {}

        return bestRef[0];
    }

    /**
     * Retarget (após a morte do alvo): procura o inimigo mais próximo do PRÓPRIO NPC.
     * Motivo: evita o NPC ficar preso no cadáver que demora a sumir.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object findNearestTargetNearNpc(Store<EntityStore> store,
                                           NpcRecord rec,
                                           Object ownerRefObj,
                                           com.hypixel.hytale.math.vector.Vector3d npcPos,
                                           Object excludeRefObj) {
        if (store == null || rec == null || ownerRefObj == null || npcPos == null) return null;
        if (!(ownerRefObj instanceof Ref) || !(rec.refObj instanceof Ref)) return null;

        Ref<EntityStore> ownerRef = (Ref<EntityStore>) ownerRefObj;
        Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

        final double r = DEFENDER_AUTO_ACQUIRE_RADIUS;
        final double r2 = r * r;
        final double maxDy = DEFENDER_AUTO_ACQUIRE_MAX_DY;

        final Object[] bestRef = new Object[1];
        final double[] bestD2 = new double[]{Double.POSITIVE_INFINITY};

        try {
            store.forEachChunk((chunk, cb) -> {
                int sz;
                try { sz = chunk.size(); } catch (Throwable t) { return; }

                for (int i = 0; i < sz; i++) {
                    Ref<EntityStore> ref;
                    try { ref = (Ref<EntityStore>) chunk.getReferenceTo(i); } catch (Throwable t) { continue; }
                    if (ref == null) continue;
                    if (refEq(ref, ownerRef) || refEq(ref, npcRef)) continue;
                    if (excludeRefObj instanceof Ref && refEq(ref, (Ref<EntityStore>) excludeRefObj)) continue;

                    // Nao perseguir players quando PvP estiver OFF
                    try {
                        Player maybePlayer = (Player) chunk.getComponent(i, Player.getComponentType());
                        if (maybePlayer != null && !pvpEnabled) continue;
                    } catch (Throwable ignored) {}

                    // Ignora entidades já marcadas como mortas (cadáver / death animation)
                    try {
                        DeathComponent dc = (DeathComponent) chunk.getComponent(i, DeathComponent.getComponentType());
                        if (dc != null) continue;
                    } catch (Throwable ignored) {}

                    TransformComponent tc;
                    try { tc = (TransformComponent) chunk.getComponent(i, TransformComponent.getComponentType()); } catch (Throwable t) { continue; }
                    if (tc == null || tc.getPosition() == null) continue;

                    var p = tc.getPosition();
                    double dy = Math.abs(p.getY() - npcPos.getY());
                    if (dy > maxDy) continue;

                    double dx = p.getX() - npcPos.getX();
                    double dz = p.getZ() - npcPos.getZ();
                    double d2 = dx * dx + dz * dz;
                    if (d2 > r2) continue;

                    // So entidades vivas
                    try {
                        EntityStatMap stats = (EntityStatMap) chunk.getComponent(i, EntityStatMap.getComponentType());
                        if (stats == null) continue;
                        var hp = stats.get(DefaultEntityStatTypes.getHealth());
                        if (hp != null && hp.get() <= 0.0f) continue;
                    } catch (Throwable ignored) {
                        continue;
                    }

                    if (d2 < bestD2[0]) {
                        bestD2[0] = d2;
                        bestRef[0] = ref;
                    }
                }
            });
        } catch (Throwable ignored) {}

        return bestRef[0];
    }

    
// =========================================================
// Loot pós-combate (combat tag)
// =========================================================

private static boolean isAnyEnemyNearNpc(Store<EntityStore> store,
                                        NpcRecord rec,
                                        Object ownerRefObj,
                                        Vector3d npcPos,
                                        double radius) {
    if (store == null || rec == null || ownerRefObj == null || npcPos == null) return false;
    if (!(ownerRefObj instanceof Ref) || !(rec.refObj instanceof Ref)) return false;

    @SuppressWarnings("unchecked")
    Ref<EntityStore> ownerRef = (Ref<EntityStore>) ownerRefObj;
    @SuppressWarnings("unchecked")
    Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

    final double r2 = radius * radius;
    final double maxDy = DEFENDER_AUTO_ACQUIRE_MAX_DY;

    final boolean[] found = new boolean[]{false};

    try {
        store.forEachChunk((chunk, cb) -> {
            if (found[0]) return;

            int sz;
            try { sz = chunk.size(); } catch (Throwable t) { return; }

            for (int i = 0; i < sz; i++) {
                Ref<EntityStore> ref;
                try { ref = (Ref<EntityStore>) chunk.getReferenceTo(i); } catch (Throwable t) { continue; }
                if (ref == null) continue;
                if (refEq(ref, ownerRef) || refEq(ref, npcRef)) continue;

                // Não perseguir players (mantém comportamento seguro; PvP entre NPCs é controlado em /amigopvp)
                try {
                    Player maybePlayer = (Player) chunk.getComponent(i, Player.getComponentType());
                    if (maybePlayer != null) continue;
                } catch (Throwable ignored) {}
// Ignora entidades já marcadas como mortas (cadáver / death animation)
                try {
                    DeathComponent dc = (DeathComponent) chunk.getComponent(i, DeathComponent.getComponentType());
                    if (dc != null) continue;
                } catch (Throwable ignored) {}

                TransformComponent tc;
                try { tc = (TransformComponent) chunk.getComponent(i, TransformComponent.getComponentType()); } catch (Throwable t) { continue; }
                if (tc == null || tc.getPosition() == null) continue;

                var p = tc.getPosition();
                double dy = Math.abs(p.getY() - npcPos.getY());
                if (dy > maxDy) continue;

                double dx = p.getX() - npcPos.getX();
                double dz = p.getZ() - npcPos.getZ();
                double d2 = dx * dx + dz * dz;
                if (d2 > r2) continue;

                // Só entidades vivas (tem stats + vida > 0).
                try {
                    EntityStatMap stats = (EntityStatMap) chunk.getComponent(i, EntityStatMap.getComponentType());
                    if (stats == null) continue;
                    var hp = stats.get(DefaultEntityStatTypes.getHealth());
                    if (hp != null && hp.get() <= 0.0f) continue;
                } catch (Throwable ignored) {
                    continue;
                }

                found[0] = true;
                return;
            }
        });
    } catch (Throwable ignored) {}

    return found[0];
}

private static double dist2(Vector3d a, Vector3d b) {
    if (a == null || b == null) return Double.POSITIVE_INFINITY;
    double dx = a.getX() - b.getX();
    double dy = a.getY() - b.getY();
    double dz = a.getZ() - b.getZ();
    return dx*dx + dy*dy + dz*dz;
}

private void clearCombatTagsAndLoot(NpcRecord rec) {
    if (rec == null) return;
    synchronized (rec.combatTags) {
        rec.combatTags.clear();
    }
    rec.pendingLootRefObjs.clear();
    rec.lootTargetRefObj = null;
    rec.lootTargetSinceMillis = 0L;
    rec.lootingActive = false;
    rec.lastCombatEndMillis = 0L;
    rec.lastBattleCenter = null;
}

private static boolean isItemRefValid(Store<EntityStore> store, Object refObj) {
    if (store == null || refObj == null) return false;
    if (!(refObj instanceof Ref)) return false;
    try {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> r = (Ref<EntityStore>) refObj;
        // Basta existir Transform + ItemComponent
        TransformComponent tc = store.getComponent(r, TransformComponent.getComponentType());
        ItemComponent ic = store.getComponent(r, ItemComponent.getComponentType());
        return tc != null && tc.getPosition() != null && ic != null;
    } catch (Throwable ignored) {}
    return false;
}


/**
 * Acumula itens coletados para enviar um resumo no chat após um pequeno atraso,
 * reduzindo poluição no chat durante LOOTING.
 */
private static void lootChatAccAdd(NpcRecord rec, String itemId, int qty, long now) {
    if (rec == null || itemId == null || itemId.isBlank() || qty <= 0) return;
    try {
        Integer cur = rec.lootChatAcc.get(itemId);
        rec.lootChatAcc.put(itemId, (cur == null ? 0 : cur) + qty);
        rec.lootChatSendAtMillis = now + LOOT_CHAT_SUMMARY_DELAY_MS;
    } catch (Throwable ignored) {}
}

/** Se o atraso expirou, envia o resumo do loot no chat e limpa o acumulador. */
private void lootChatAccFlushIfDue(NpcRecord rec, UUID ownerId, Object worldObj, long now) {
    if (rec == null || ownerId == null || worldObj == null) return;
    long at = rec.lootChatSendAtMillis;
    if (at <= 0L || now < at) return;

    if (rec.lootChatAcc.isEmpty()) {
        rec.lootChatSendAtMillis = 0L;
        return;
    }

    try {
        StringBuilder sb = new StringBuilder();
        sb.append("AmigoNPC: Coletado ");
        boolean first = true;
        for (java.util.Map.Entry<String, Integer> en : rec.lootChatAcc.entrySet()) {
            if (en == null) continue;
            String id = en.getKey();
            Integer q = en.getValue();
            if (id == null || id.isBlank() || q == null || q <= 0) continue;
            if (!first) sb.append(", ");
            first = false;
            sb.append(q).append(" ").append(id);
            // evita mensagens gigantes
            if (sb.length() > 180) break;
        }
        sb.append(".");
        sendToOwner(worldObj, ownerId, sb.toString());
    } catch (Throwable ignored) {
        // não quebra tick por causa de chat
    } finally {
        rec.lootChatAcc.clear();
        rec.lootChatSendAtMillis = 0L;
    }
}


/**
 * Tenta inserir o stack de uma entidade item no chão na mochila do NPC.
 * Retorna true se inseriu algo (mesmo que parcial).
 * - Sem duplicação: só remove/atualiza o item do chão após inserir no inventário.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
private boolean tryPickupGroundItemIntoBackpack(Store<EntityStore> store,
                                               UUID ownerId,
                                               NpcRecord rec,
                                               SimpleItemContainer bag,
                                               Object itemRefObj) {
    if (store == null || ownerId == null || rec == null || bag == null || itemRefObj == null) return false;
    if (!(itemRefObj instanceof Ref)) return false;

    Ref<EntityStore> itemRef = (Ref<EntityStore>) itemRefObj;

    ItemComponent ic;
    try { ic = store.getComponent(itemRef, ItemComponent.getComponentType()); } catch (Throwable t) { return false; }
    if (ic == null) return false;

    ItemStack before;
    try { before = ic.getItemStack(); } catch (Throwable t) { return false; }
    if (before == null) return false;

    int beforeQty;
    try { beforeQty = before.getQuantity(); } catch (Throwable t) { return false; }
    if (beforeQty <= 0) return false;

    String itemId;
    try { itemId = before.getItemId(); } catch (Throwable t) { itemId = null; }
    if (itemId == null || itemId.isBlank()) itemId = "item";

    try {
        // Usa a lógica do container do jogo (respeita max stack e compatibilidade)
        ItemStackTransaction tx = bag.addItemStack(before);
        ItemStack rem = tx.getRemainder();

        int remQty = 0;
        try { if (rem != null) remQty = rem.getQuantity(); } catch (Throwable ignored) {}

        int inserted = beforeQty - remQty;
        if (inserted <= 0) {
            // Se estiver totalmente cheio, abandona o looting e avisa (com cooldown)
            if (isBackpackCompletelyFull(bag)) {
                rec.lootPausedInventoryFull = true;
                rec.lootingActive = false;
                maybeNotifyBackpackFull(rec, ownerId, rec.worldObj, System.currentTimeMillis());
            } else {
            }
            return false;
        }

        // Marca dirty (persistência)
        rec.backpackDirty = true;
        long now = System.currentTimeMillis();
        if (rec.nextBackpackSaveMillis == 0L) rec.nextBackpackSaveMillis = now + 5_000L;

        // Atualiza chão: remove se entrou tudo, senão ajusta quantidade restante
        if (rem == null || remQty <= 0) {
            store.removeEntity(itemRef, RemoveReason.REMOVE);
        } else {
            ic.setItemStack(rem);
            store.putComponent(itemRef, ItemComponent.getComponentType(), ic);
        }
        // Resumo no chat após pequeno atraso (reduz poluição)
        lootChatAccAdd(rec, itemId, inserted, now);
        return true;
    } catch (Throwable t) {
    }

    return false;
}


/**
 * Monta/atualiza a lista de itens pendentes para loot, procurando drops perto das combat tags.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
private void refreshPendingLootFromCombatTags(Store<EntityStore> store,
                                             NpcRecord rec,
                                             Vector3d npcPos,
                                             Vector3d ownerPos,
                                             long now) {
    if (store == null || rec == null) return;

    // Prune do mapa de "ignorados por um tempo"
    try {
        rec.lootProcessedUntil.entrySet().removeIf(e -> e == null || e.getValue() == null || e.getValue() <= now);
    } catch (Throwable ignored) {}

    // Snapshot tags
    java.util.ArrayList<CombatTag> tags = new java.util.ArrayList<>();
    synchronized (rec.combatTags) { tags.addAll(rec.combatTags); }

    if (tags.isEmpty()) return;

    final double tagR2 = LOOT_TAG_SCAN_RADIUS * LOOT_TAG_SCAN_RADIUS;

    // IMPORTANTE: iterar apenas sobre entidades que são Item drops (Transform + ItemComponent)
    // para evitar tentar "loot" de entidades normais.
    try {
        store.forEachChunk(AUTOLOOT_QUERY, (chunkObj, cb) -> {
            if (!(chunkObj instanceof com.hypixel.hytale.component.ArchetypeChunk<?> rawChunk)) return true;

            com.hypixel.hytale.component.ArchetypeChunk<EntityStore> chunk;
            try { chunk = (com.hypixel.hytale.component.ArchetypeChunk<EntityStore>) rawChunk; }
            catch (Throwable t) { return true; }

            int sz;
            try { sz = chunk.size(); } catch (Throwable t) { return true; }

            for (int i = 0; i < sz; i++) {
                if (rec.pendingLootRefObjs.size() >= LOOT_PENDING_MAX) return true;

                Ref<EntityStore> ref;
                try { ref = (Ref<EntityStore>) chunk.getReferenceTo(i); } catch (Throwable t) { continue; }
                if (ref == null) continue;

                // Se já está pendente, não repete
                if (containsRef(rec.pendingLootRefObjs, ref)) continue;

                // Ignora se foi marcado como "não tentar agora"
                Long until = rec.lootProcessedUntil.get(ref);
                if (until != null && until > now) continue;

                TransformComponent tc;
                try { tc = (TransformComponent) chunk.getComponent(i, TransformComponent.getComponentType()); }
                catch (Throwable t) { continue; }
                if (tc == null || tc.getPosition() == null) continue;

                Vector3d p = tc.getPosition();

                boolean nearAnyTag = false;
                for (CombatTag t : tags) {
                    if (t == null || t.pos == null) continue;
                    if (dist2(p, t.pos) <= tagR2) { nearAnyTag = true; break; }
                }
                if (!nearAnyTag) continue;

                rec.pendingLootRefObjs.add(ref);
            }

            return true;
        });
    } catch (Throwable ignored) {}
}

private static boolean containsRef(java.util.ArrayList<Object> list, Ref<EntityStore> ref) {
    if (list == null || ref == null) return false;
    for (Object o : list) {
        if (o instanceof Ref && refEq((Ref<EntityStore>) o, ref)) return true;
    }
    return false;
}

/**
 * Escolhe o item pendente mais próximo do NPC.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
private Object chooseNearestPendingLoot(NpcRecord rec, Store<EntityStore> store, Vector3d npcPos) {
    if (rec == null || store == null || npcPos == null) return null;
    Object best = null;
    double bestD2 = Double.POSITIVE_INFINITY;

    // Limpa refs inválidos enquanto escolhe
    for (int i = rec.pendingLootRefObjs.size() - 1; i >= 0; i--) {
        Object r = rec.pendingLootRefObjs.get(i);
        if (!isItemRefValid(store, r)) {
            rec.pendingLootRefObjs.remove(i);
            continue;
        }

        if (!(r instanceof Ref)) continue;
        try {
            TransformComponent tc = store.getComponent((Ref<EntityStore>) r, TransformComponent.getComponentType());
            if (tc == null || tc.getPosition() == null) continue;
            double d2 = dist2(tc.getPosition(), npcPos);
            if (d2 < bestD2) { bestD2 = d2; best = r; }
        } catch (Throwable ignored) {}
    }
    return best;
}

/**
 * Tick do LOOTING:
 * - só fora de combate/assist e só quando não há inimigo muito perto do NPC
 * - NPC corre até o item e só então insere na mochila
 */
@SuppressWarnings({"unchecked", "rawtypes"})
private void tickCombatTaggedLooting(Store<EntityStore> store,
                                    UUID ownerId,
                                    NpcRecord rec,
                                    Object ownerRefObj,
                                    Vector3d npcPos,
                                    Vector3d ownerPos,
                                    long now,
                                    Object worldObj,
                                    boolean inCombatOrAssistNow) {
    if (store == null || ownerId == null || rec == null || npcPos == null) return;
    if (rec.downed) return;
    if (rec.state != State.ACTIVE) return;

    // Se entrou em combate, pausa LOOTING imediatamente
    if (inCombatOrAssistNow) {
        rec.wasInCombat = true;
        rec.lootingActive = false;
        rec.lootTargetRefObj = null;
        rec.lootTargetSinceMillis = 0L;
        rec.pendingLootRefObjs.clear();
        return;
    }

    // Transição: acabou o combate → inicia LOOTING (se houver tags)
    if (rec.wasInCombat) {
        rec.wasInCombat = false;
        rec.lastCombatEndMillis = now;
        // se não tiver tags, não inicia
        boolean hasTags;
        synchronized (rec.combatTags) { hasTags = !rec.combatTags.isEmpty(); }
        rec.lootingActive = hasTags;
        rec.lootTargetRefObj = null;
        rec.lootTargetSinceMillis = 0L;
        rec.pendingLootRefObjs.clear();
    }

    if (!rec.lootingActive) return;

    // Limpa tags ao sair do local da batalha ou após timeout
    if (rec.lastCombatEndMillis > 0L) {
        boolean timeUp = (now - rec.lastCombatEndMillis) >= COMBAT_TAG_CLEAR_MS;
        boolean distUp = false;
        if (rec.lastBattleCenter != null && ownerPos != null) {
            distUp = dist2(rec.lastBattleCenter, ownerPos) >= (COMBAT_TAG_CLEAR_DISTANCE * COMBAT_TAG_CLEAR_DISTANCE);
        }
        if (timeUp || distUp) {
            clearCombatTagsAndLoot(rec);
            return;
        }
    }

    // Só entra/continua loot se não há inimigo muito perto do NPC
    if (ownerRefObj != null && isAnyEnemyNearNpc(store, rec, ownerRefObj, npcPos, LOOTING_NO_ENEMY_RADIUS)) {
        // libera o Defender retomar (não bloqueia)
        rec.lootingActive = false;
        rec.lootTargetRefObj = null;
        rec.lootTargetSinceMillis = 0L;
        rec.pendingLootRefObjs.clear();
        return;
    }

    // mochila carregada
    if (rec.backpack == null) {
        try { rec.backpack = getOrLoadBackpack(ownerId); } catch (Throwable ignored) {}
    }
    SimpleItemContainer bag = rec.backpack;
    if (bag == null) return;
    // Mochila cheia é tratada por item no momento do pickup (para permitir empilhar)
    rec.lootPausedInventoryFull = false;
    // Se não há target atual, tenta montar pendentes e escolher um
    if (rec.lootTargetRefObj == null) {
        if (rec.pendingLootRefObjs.isEmpty()) {
            refreshPendingLootFromCombatTags(store, rec, npcPos, ownerPos, now);
        }
        Object chosen = chooseNearestPendingLoot(rec, store, npcPos);
        if (chosen == null) {
            // acabou o loot dessa luta
            rec.lootingActive = false;
            return;
        }
        rec.lootTargetRefObj = chosen;
        rec.lootTargetSinceMillis = now;
    }

    // Se o target ficou inválido, troca
    if (!isItemRefValid(store, rec.lootTargetRefObj)) {
        rec.lootTargetRefObj = null;
        rec.lootTargetSinceMillis = 0L;
        return;
    }

    // Timeout para não ficar preso em item inalcançável
    if (rec.lootTargetSinceMillis > 0L && (now - rec.lootTargetSinceMillis) > LOOT_TARGET_TIMEOUT_MS) {
        rec.lootProcessedUntil.put(rec.lootTargetRefObj, now + LOOT_SKIP_RETRY_MS);
        rec.lootTargetRefObj = null;
        rec.lootTargetSinceMillis = 0L;
        return;
    }

    // Faz o NPC correr até o item (LockedTarget)
    try {
        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
        if (npcEntityObj != null) {
            setLockedTargetOnNpcEntity(npcEntityObj, rec.lootTargetRefObj);
            setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", null);
            setFlockState(store, rec.refObj, "Run", "");
        }
    } catch (Throwable ignored) {}

    // Se chegou perto do item, tenta pickup
    try {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> itemRef = (Ref<EntityStore>) rec.lootTargetRefObj;
        TransformComponent itc = store.getComponent(itemRef, TransformComponent.getComponentType());
        if (itc == null || itc.getPosition() == null) return;

        Vector3d ip = itc.getPosition();
        double dx = ip.getX() - npcPos.getX();
        double dz = ip.getZ() - npcPos.getZ();
        double h2 = dx*dx + dz*dz;
        double dy = Math.abs(ip.getY() - npcPos.getY());
        if (h2 <= (LOOT_PICKUP_DISTANCE * LOOT_PICKUP_DISTANCE) && dy <= 3.25) {
            boolean inserted = tryPickupGroundItemIntoBackpack(store, ownerId, rec, bag, itemRef);

            if (!inserted) {
                // Não coube / erro: ignora esse item por um tempo e tenta outro
                rec.lootProcessedUntil.put(itemRef, now + LOOT_SKIP_RETRY_MS);
            }
            // Remove da lista pendente (independente de sucesso) para evitar loop imediato
            removeRefFromList(rec.pendingLootRefObjs, itemRef);

            // troca alvo
            rec.lootTargetRefObj = null;
            rec.lootTargetSinceMillis = 0L;
        }
    } catch (Throwable ignored) {}
}

private static void removeRefFromList(java.util.ArrayList<Object> list, Ref<EntityStore> ref) {
    if (list == null || ref == null) return;
    for (int i = list.size() - 1; i >= 0; i--) {
        Object o = list.get(i);
        if (o instanceof Ref && refEq((Ref<EntityStore>) o, ref)) {
            list.remove(i);
        }
    }
}

// =========================================================
    // Auto-loot (passivo)
    // =========================================================

    /**
     * Coleta itens dropados num raio curto e insere na mochila do NPC.
     * - Server-authoritative (usa a própria lógica do ItemContainer)
     * - Não roda em combate/assist/Downed (checado no caller)
     * - Sem duplicação: a entidade de item é consumida apenas na medida do que couber
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tryAutoLoot(Store<EntityStore> store,
                             UUID ownerId,
                             NpcRecord rec,
                             Vector3d npcPos,
                             Vector3d ownerPos,
                             long now,
                             Object worldObj) {

        // /autoloot OFF => não coleta nada.
        if (rec == null || store == null || ownerId == null || npcPos == null) return;
        if (!rec.autoLootEnabled) {
            // garante que não fique preso em estados antigos
            endCombatTaggedLooting(rec);
            return;
        }

        // Não coleta se estiver DOWNED
        if (rec.downed) return;
        if (rec.state != State.ACTIVE) return;

        // /autoloot: raio fixo 4, sem throttling e sem limite por ciclo
        final double radius = 4.0;
        final double r2 = radius * radius;
        final int maxPerScan = Integer.MAX_VALUE;

        // mochila carregada (mesma do /loot)
        if (rec.backpack == null) {
            try {
                rec.backpack = getOrLoadBackpack(ownerId);
            } catch (Throwable ignored) {
            }
        }
        SimpleItemContainer bag = rec.backpack;
        if (bag == null) return;

        // Se estava pausado por mochila cheia, só retoma quando houver espaço
        if (rec.lootPausedInventoryFull) {
            if (now < rec.nextLootFullRecheckMillis) return;
            rec.nextLootFullRecheckMillis = now + 1_000L;
            if (isBackpackCompletelyFull(bag)) {
                maybeNotifyBackpackFull(rec, ownerId, worldObj, now);
                return;
            }
            rec.lootPausedInventoryFull = false;
        }

        if (isBackpackCompletelyFull(bag)) {
            rec.lootPausedInventoryFull = true;
            rec.nextLootFullRecheckMillis = now + 1_000L;
            maybeNotifyBackpackFull(rec, ownerId, worldObj, now);
            return;
        }

        final int[] picked = new int[]{0};
        final java.util.ArrayList<Ref<EntityStore>> removeLater = new java.util.ArrayList<>();
        final java.util.ArrayList<Ref<EntityStore>> updateLaterRef = new java.util.ArrayList<>();
        final java.util.ArrayList<ItemComponent> updateLaterComp = new java.util.ArrayList<>();

        try {
            store.forEachChunk(AUTOLOOT_QUERY, (chunkObj, cb) -> {
                // Preferir acesso direto ao ArchetypeChunk (mais confiável). Mantém fallback por reflexão.
                com.hypixel.hytale.component.ArchetypeChunk<EntityStore> chunk = null;
                if (chunkObj instanceof com.hypixel.hytale.component.ArchetypeChunk<?> rawChunk) {
                    try { chunk = (com.hypixel.hytale.component.ArchetypeChunk<EntityStore>) rawChunk; } catch (Throwable ignored) { chunk = null; }
                }

                final int size;
                try {
                    size = (chunk != null) ? chunk.size() : chunkGetSize(chunkObj);
                } catch (Throwable t) {
                    return true;
                }

                for (int i = 0; i < size; i++) {
                    if (picked[0] >= maxPerScan) break;

                    Ref<EntityStore> itemRef;
                    try {
                        itemRef = (chunk != null) ? (Ref<EntityStore>) chunk.getReferenceTo(i) : chunkGetEntity(chunkObj, i);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (itemRef == null || !itemRef.isValid()) continue;

                    TransformComponent tc;
                    ItemComponent ic;
                    try {
                        tc = (chunk != null)
                                ? (TransformComponent) chunk.getComponent(i, TransformComponent.getComponentType())
                                : (TransformComponent) chunkGetComponent(chunkObj, i, TransformComponent.getComponentType());
                        ic = (chunk != null)
                                ? (ItemComponent) chunk.getComponent(i, ItemComponent.getComponentType())
                                : (ItemComponent) chunkGetComponent(chunkObj, i, ItemComponent.getComponentType());
                    } catch (Throwable t) {
                        continue;
                    }
                    if (tc == null || ic == null) continue;

                    Vector3d pos = tc.getPosition();
                    if (pos == null) continue;
                    if (distSq(pos, npcPos) > r2) continue;

                    ItemStack before = ic.getItemStack();
                    if (before == null) continue;

                    int beforeQty;
                    try {
                        beforeQty = before.getQuantity();
                    } catch (Throwable t) {
                        continue;
                    }
                    if (beforeQty <= 0) continue;

                    // inserir no inventário do NPC (server-authoritative)
                    ItemStackTransaction tx;
                    try {
                        tx = bag.addItemStack(before);
                    } catch (Throwable t) {
                        tx = null;
                    }

                    ItemStack remainder = null;
                    try {
                        remainder = (tx != null) ? tx.getRemainder() : null;
                    } catch (Throwable ignored) {
                    }

                    int remQty = 0;
                    if (remainder != null) {
                        try {
                            remQty = remainder.getQuantity();
                        } catch (Throwable ignored) {
                            remQty = beforeQty;
                        }
                    }

                    if (remainder != null && remQty >= beforeQty) {
                        // não entrou nada
                        if (isBackpackCompletelyFull(bag)) {
                            rec.lootPausedInventoryFull = true;
                            rec.nextLootFullRecheckMillis = now + 1_000L;
                            maybeNotifyBackpackFull(rec, ownerId, worldObj, now);
                            return false; // pausa scan
                        }
                        continue;
                    }

                    int inserted = beforeQty - (remainder != null ? remQty : 0);
                    if (inserted > 0) {
                        String itemId;
                        try {
                            Object id = before.getItemId();
                            itemId = (id != null) ? id.toString() : "item";
                        } catch (Throwable t) {
                            itemId = "item";
                        }
                        lootChatAccAdd(rec, itemId, inserted, now);
                    }

                    picked[0]++;
                    rec.backpackDirty = true;
                    rec.nextBackpackSaveMillis = Math.min(rec.nextBackpackSaveMillis, now + BACKPACK_SAVE_DEBOUNCE_MS);

                    // remover do chão SOMENTE o que entrou
                    if (remainder == null || remQty <= 0) {
                        removeLater.add(itemRef);
                    } else {
                        try {
                            ic.setItemStack(remainder);
                            updateLaterRef.add(itemRef);
                            updateLaterComp.add(ic);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                return true;
            });
        } catch (Throwable t) {
            debugCombat(rec, ownerId, "AutoLoot forEachChunk falhou: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        // Aplica updates fora do loop do ECS
        for (int i = 0; i < updateLaterRef.size(); i++) {
            Ref<EntityStore> ref = updateLaterRef.get(i);
            ItemComponent comp = updateLaterComp.get(i);
            if (ref == null || !ref.isValid() || comp == null) continue;
            try {
                store.putComponent(ref, ItemComponent.getComponentType(), comp);
            } catch (Throwable ignored) {
            }
        }

        // Remove entidades de item totalmente coletadas
        for (Ref<EntityStore> r : removeLater) {
            if (r == null || !r.isValid()) continue;
            try {
                store.removeEntity(r, RemoveReason.REMOVE);
            } catch (Throwable ignored) {
                doRemoveEntity(store, r);
            }
        }

        // Persistência do inventário
        if (rec.backpackDirty && now >= rec.nextBackpackSaveMillis) {
            try {
                AmigoPersistence.saveBackpack(ownerId, bag);
                rec.backpackDirty = false;
            } catch (Throwable t) {
                debugCombat(rec, ownerId, "Falha ao salvar mochila: " + t.getMessage());
            }
        }

        // Flush do resumo no chat (0.5s)
        lootChatAccFlushIfDue(rec, ownerId, worldObj, now);
    }

    private static boolean isBackpackCompletelyFull(SimpleItemContainer bag) {
        if (bag == null) return true;
        short cap;
        try { cap = bag.getCapacity(); } catch (Throwable t) { return true; }
        for (short slot = 0; slot < cap; slot++) {
            ItemStack st;
            try { st = bag.getItemStack(slot); } catch (Throwable t) { continue; }
            if (st == null || st.isEmpty()) return false;

            try {
                Item item = st.getItem();
                int max = (item != null) ? item.getMaxStack() : 1;
                if (st.getQuantity() < max) return false;
            } catch (Throwable ignored) {
                // Se não conseguimos ler o max stack, não bloqueia o loot (evita falsa detecção de mochila cheia)
                return false;
            }
        }
        return true;
    }

    private static double distSq(Vector3d a, Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx*dx + dy*dy + dz*dz;
    }

    // Compat: algumas builds não expõem o tipo concreto do "chunk" no classpath.
    // Para manter compilação estável, acessamos por reflexão.
    // OBS: a API real do chunk costuma ser size() + getReferenceTo(i).
    private static int chunkGetSize(Object chunk) {
        // Hytale ECS: ArchetypeChunk#size()
        Object v = invokeNoArg(chunk, "size");
        if (v == null) {
            // fallback (algumas versões podem expor getSize())
            v = invokeNoArg(chunk, "getSize");
        }
        if (v instanceof Integer i) return i;
        if (v instanceof Short s) return s;
        if (v instanceof Long l) return (int) (long) l;
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static Ref<EntityStore> chunkGetEntity(Object chunk, int index) {
        if (chunk == null) return null;
        try {
            // API mais comum: getReferenceTo(i)
            java.lang.reflect.Method m = chunk.getClass().getMethod("getReferenceTo", int.class);
            return (Ref<EntityStore>) m.invoke(chunk, index);
        } catch (Throwable ignored) {
        }
        try {
            for (java.lang.reflect.Method m : chunk.getClass().getMethods()) {
                String name = m.getName();
                if (!(name.equals("getReferenceTo") || name.equals("getEntity") || name.equals("getReference"))) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> p0 = m.getParameterTypes()[0];
                if (!(p0 == int.class || p0 == Integer.class || p0 == short.class || p0 == Short.class)) continue;
                Object idx = (p0 == short.class || p0 == Short.class) ? (short) index : index;
                return (Ref<EntityStore>) m.invoke(chunk, idx);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object chunkGetComponent(Object chunk, int index, Object componentType) {
        if (chunk == null || componentType == null) return null;
        try {
            for (java.lang.reflect.Method m : chunk.getClass().getMethods()) {
                if (!m.getName().equals("getComponent")) continue;
                if (m.getParameterCount() != 2) continue;
                Class<?>[] p = m.getParameterTypes();
                boolean firstIsIndex = (p[0] == int.class || p[0] == Integer.class || p[0] == short.class || p[0] == Short.class);
                if (!firstIsIndex) continue;
                Object idx = (p[0] == short.class || p[0] == Short.class) ? (short) index : index;
                try {
                    return m.invoke(chunk, idx, componentType);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void maybeNotifyBackpackFull(NpcRecord rec, UUID ownerId, Object worldObj, long now) {
        if (rec == null || ownerId == null || worldObj == null) return;
        if (now < rec.nextLootFullMsgMillis) return;
        rec.nextLootFullMsgMillis = now + AUTOLOOT_FULL_MSG_COOLDOWN_MS;

        // Mensagem simples (sem spam)
        try {
            sendToOwner(worldObj, ownerId, "Estou com o inventário cheio.");
        } catch (Throwable ignored) {}
    }

    /** Retorna o Ref atual de combate (se ativo), ou null. */
    private Object getActiveCombatTarget(NpcRecord rec, long now) {
        if (rec == null) return null;
        if (rec.combatTargetRefObj == null) return null;
        if (rec.combatUntilMillis <= 0L) return null;
        if (now > rec.combatUntilMillis) return null;
        return rec.combatTargetRefObj;
    }


    /** Ticker (1s) chamado pelo plugin: revive automático quando o timer expirar. */
    public void tickDowned() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, NpcRecord> e : npcRefPorPlayer.entrySet()) {
            UUID owner = e.getKey();
            NpcRecord rec = e.getValue();
            if (rec == null) continue;
            if (!rec.downed) continue;
            if (rec.downedUntilMillis <= 0L) continue;

            // Se o corpo sumiu (despawn por algum sistema), limpa o registro para evitar "NPC já existe"
            // e permite respawn/revive correto.
            if (rec.refObj != null && rec.worldObj != null) {
                Object worldObj = rec.worldObj;
                HytaleBridge.worldExecute(worldObj, () -> {
                    try {
                        Object storeObj = getComponentStoreFromWorld(worldObj);
                        if (!(storeObj instanceof Store<?> rawStore)) return;
                        @SuppressWarnings("unchecked")
                        Store<EntityStore> store = (Store<EntityStore>) rawStore;

                        Object npcTransform = getComponentFromStore(store, rec.refObj, TransformComponent.getComponentType());
                        if (!(npcTransform instanceof TransformComponent)) {
                            amigoRefs.remove(rec.refObj);
                            npcRefPorPlayer.remove(owner, rec);
                        }
                    } catch (Throwable ignored) {}
                });
            }
            if (now < rec.downedUntilMillis) continue;
            // Revive automático. Se o corpo tiver sumido, respawna um novo.
            if (npcRefPorPlayer.get(owner) == rec) {
                revive(owner, false);
                // Se ainda está downed após tentar revive (ex.: entidade sumiu), faz respawn limpo.
                NpcRecord still = npcRefPorPlayer.get(owner);
                if (still == rec && rec.downed) {
                    amigoRefs.remove(rec.refObj);
                    npcRefPorPlayer.remove(owner, rec);
                    spawn(rec.worldObj, owner, null);
                }
            }
        }
    }

    /**
     * Ticker (1s) chamado pelo plugin: mantém follow e aplica "teleporte" seguro
     * quando distância passar dos limites (20 horizontal / 8 vertical).
     */
    public void tickFollow() {
        // Processa respawns pendentes (teleport/troca de mundo do dono)
        if (!pendingRespawns.isEmpty()) {
            long now = System.currentTimeMillis();
            java.util.Iterator<Map.Entry<UUID, PendingRespawn>> it = pendingRespawns.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, PendingRespawn> en = it.next();
                UUID ownerId = en.getKey();
                PendingRespawn pr = en.getValue();
                if (pr == null || pr.worldObj == null) {
                    it.remove();
                    continue;
                }
                if (now < pr.atMillis) continue;

                // Evita respawn duplicado se alguém já deu /amigo spawn no meio
                if (npcRefPorPlayer.containsKey(ownerId)) {
                    it.remove();
                    continue;
                }

                if (pr.message != null && !pr.message.isBlank()) {
                    sendToOwner(pr.worldObj, ownerId, pr.message);
                }
                spawn(pr.worldObj, ownerId, pr.senderObj);
                it.remove();
            }
        }

    for (Map.Entry<UUID, NpcRecord> e : npcRefPorPlayer.entrySet()) {
        UUID owner = e.getKey();
        NpcRecord rec = e.getValue();
        if (rec == null) continue;
        if (rec.state != State.ACTIVE) continue;
        if (rec.refObj == null || rec.worldObj == null) continue;
        if (rec.downed) continue;

        Object worldObj = rec.worldObj;

        // Executa no contexto do mundo (thread-safe)
        HytaleBridge.worldExecute(worldObj, () -> {
            try {
                Object storeObj = getComponentStoreFromWorld(worldObj);
                if (!(storeObj instanceof Store<?> rawStore)) return;

                @SuppressWarnings("unchecked")
                Store<EntityStore> store = (Store<EntityStore>) rawStore;

                Object ownerRef = invokeOneArg(worldObj, "getEntityRef", UUID.class, owner);
                if (ownerRef == null) return;

                Object ownerTransform = getComponentFromStore(store, ownerRef, TransformComponent.getComponentType());
                Object npcTransform = getComponentFromStore(store, rec.refObj, TransformComponent.getComponentType());
                // Se o NPC sumiu do mundo (despawn por algum sistema), limpa o registro para permitir /amigo spawn
                if (!(npcTransform instanceof TransformComponent)) {
                    amigoRefs.remove(rec.refObj);
                    npcRefPorPlayer.remove(owner, rec);
                    return;
                }

                // Segurança: se por algum motivo o HP do NPC chegou a 0 e ele não entrou em DOWNED,
                // força o estado DOWNED para não continuar correndo/atacando sem vida.
                try {
                    @SuppressWarnings("unchecked")
                    Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;
                    EntityStatMap st = store.getComponent(npcRef, EntityStatMap.getComponentType());
                    if (st != null) {
                        try {
                            var hp = st.get(DefaultEntityStatTypes.getHealth());
                            if (hp != null && hp.get() <= 0.0f) {
                                markDowned(owner);
                                return;
                            }
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
                if (!(ownerTransform instanceof TransformComponent)) return;

                TransformComponent ot = (TransformComponent) ownerTransform;
                TransformComponent nt = (TransformComponent) npcTransform;

                var op = ot.getPosition();
                var np = nt.getPosition();
                if (op == null || np == null) return;

                double dx = op.getX() - np.getX();
                double dz = op.getZ() - np.getZ();
                double dy = op.getY() - np.getY();

                double horizontal = Math.sqrt(dx * dx + dz * dz);

                // Se o NPC já voltou para perto do dono, libera novamente a aquisição de alvo.
                if (rec.chaseDisengaged && horizontal <= CHASE_REACQUIRE_DISTANCE) {
                    rec.chaseDisengaged = false;
                }

                // controle: tempo fora do raio de follow (25 blocos)
                if (horizontal > 25.0) {
                    if (rec.farSinceMillis == 0L) rec.farSinceMillis = System.currentTimeMillis();
                } else {
                    rec.farSinceMillis = 0L;
                }

                // Amostra de movimento real do NPC (anti-teleporte agressivo)
                long now = System.currentTimeMillis();
                if (rec.lastNpcMovedMillis == 0L) rec.lastNpcMovedMillis = now;
                if (rec.lastSampleMillis == 0L) rec.lastSampleMillis = now;
                if (rec.lastNpcPos == null) rec.lastNpcPos = np;
                if (now - rec.lastSampleMillis >= 400L) {
                    double mdx = np.getX() - rec.lastNpcPos.getX();
                    double mdy = np.getY() - rec.lastNpcPos.getY();
                    double mdz = np.getZ() - rec.lastNpcPos.getZ();
                    double moved = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
                    if (moved > 0.25) rec.lastNpcMovedMillis = now;
                    rec.lastNpcPos = np;
                    rec.lastSampleMillis = now;
                }

                // Limpa animação de ataque antiga (best-effort)
                clearExpiredAttackAnimation(store, rec.refObj, rec, now);

                // Defender: por padrão OFF (sem ataques). Quando ON, escolhe alvo e persegue.
                try {
                    // Defender OFF: limpa qualquer alvo em memória e apenas segue.
                    if (!rec.defendeEnabled) {
                        rec.combatUntilMillis = 0L;
                        rec.combatTargetRefObj = null;
                        rec.assistUntilMillis = 0L;
                        rec.assistTargetRefObj = null;
                        rec.chaseDisengaged = false;
                        rec.targetLostSinceMillis = 0L;
                        rec.targetStuckSinceMillis = 0L;
                        rec.lastTargetHorizontal = -1.0;
                        rec.lastTargetSampleMillis = 0L;

                        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                            setLockedTargetOnNpcEntity(npcEntityObj, ownerRef);
                            setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", null);
                        }
                    } else {
                        // expira combate por tempo
                        if (rec.combatUntilMillis > 0L && now > rec.combatUntilMillis) {
                            rec.combatUntilMillis = 0L;
                            rec.combatTargetRefObj = null;
                        }

                        // se o agressor sumiu/morreu, encerra combate
                        Object combatTarget = getActiveCombatTarget(rec, now);
                        if (combatTarget != null && !isAliveEntityRef(store, combatTarget)) {
                            rec.combatUntilMillis = 0L;
                            rec.combatTargetRefObj = null;
                            combatTarget = null;
                        }

                        // assistência: termina 3s após ficar sem alvo
                        tickAssistHousekeeping(rec, store, now);

                        // Auto-aquisicao: com Defender ON, se nao ha agressor nem assist atual, pega um alvo no raio.
                        // Se estourou o limite de chase, aguarda voltar para perto do dono.
                        if (!rec.chaseDisengaged && !rec.lootingActive && combatTarget == null && rec.assistTargetRefObj == null && rec.assistUntilMillis == 0L) {
                            Object autoTarget = findNearestDefenderTarget(store, rec, ownerRef, op);
                            if (autoTarget != null) {
                                rec.assistTargetRefObj = autoTarget;
                                rec.assistUntilMillis = 0L;
                                debugCombat(rec, owner, "autoAssist: targetRef=" + autoTarget);
                            }
                        }


                        Object desiredTarget = combatTarget;
                        if (desiredTarget == null) {
                            Object assistTarget = getActiveAssistTarget(rec, now);
                            desiredTarget = (assistTarget != null) ? assistTarget : ownerRef;
                        }

                        boolean inCombatOrAssist = (desiredTarget != null && !refEq(desiredTarget, ownerRef));

                        // Limite de perseguição: se o NPC se afastar demais do dono,
                        // ele desiste do alvo e volta para o player. Isso evita o NPC
                        // correr para longe e também ajuda o auto-loot voltar após o combate.
                        if (inCombatOrAssist && horizontal > CHASE_MAX_DISTANCE) {
                            // Cancela agressor/assist atual
                            rec.combatUntilMillis = 0L;
                            rec.combatTargetRefObj = null;
                            rec.assistTargetRefObj = null;
                            rec.chaseDisengaged = true;
                            // Dá um pequeno grace para não retargetar imediatamente enquanto retorna
                            rec.assistUntilMillis = now + ASSIST_GRACE_MILLIS;

                            desiredTarget = ownerRef;
                            inCombatOrAssist = false;

                            rec.targetLostSinceMillis = 0L;
                            rec.targetStuckSinceMillis = 0L;
                            rec.lastTargetHorizontal = -1.0;
                            rec.lastTargetSampleMillis = 0L;
                        }

                        // Se o alvo já está morto (HP zerou), não fique esperando o corpo sumir.
                        // - Defender ON: retarget imediato para um alvo perto do próprio NPC.
                        if (inCombatOrAssist && !isAliveEntityRef(store, desiredTarget)) {
                            // limpa refs antigas
                            if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                                rec.combatUntilMillis = 0L;
                                rec.combatTargetRefObj = null;
                                combatTarget = null;
                            }
                            if (rec.assistTargetRefObj != null && refEq(desiredTarget, rec.assistTargetRefObj)) {
                                rec.assistTargetRefObj = null;
                                rec.assistUntilMillis = now + ASSIST_GRACE_MILLIS;
                            }

                            Object rt = findNearestTargetNearNpc(store, rec, ownerRef, np, desiredTarget);
                            if (rt != null) {
                                rec.assistTargetRefObj = rt;
                                rec.assistUntilMillis = 0L;
                                desiredTarget = rt;
                                inCombatOrAssist = true;

                                // reseta trackers para não herdar "lost" do alvo anterior
                                rec.targetLostSinceMillis = 0L;
                                rec.targetStuckSinceMillis = 0L;
                                rec.lastTargetHorizontal = -1.0;
                                rec.lastTargetSampleMillis = 0L;

                                debugCombat(rec, owner, "retargetOnDeath: targetRef=" + rt);
                            } else {
                                desiredTarget = ownerRef;
                                inCombatOrAssist = false;
                            }
                        }

                        // Target lost (A): se ficar longe demais / altura ruim / travado → limpa e volta a seguir.
                        if (inCombatOrAssist) {
                            try {
                                if (desiredTarget instanceof Ref && rec.refObj instanceof Ref) {
                                    Ref<EntityStore> tgtRef = (Ref<EntityStore>) desiredTarget;
                                    Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

                                    TransformComponent npcT = store.getComponent(npcRef, TransformComponent.getComponentType());
                                    TransformComponent tgtT = store.getComponent(tgtRef, TransformComponent.getComponentType());

                                    if (npcT != null && tgtT != null && npcT.getPosition() != null && tgtT.getPosition() != null) {
                                        var np2 = npcT.getPosition();
                                        var tp2 = tgtT.getPosition();

                                        double dx2 = tp2.getX() - np2.getX();
                                        double dz2 = tp2.getZ() - np2.getZ();
                                        double dy2 = tp2.getY() - np2.getY();
                                        double h2 = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                                        double ady2 = Math.abs(dy2);

                                        // Alvos altos (~2 blocos): relaxa o limite vertical e tenta reposicionar
                                        double targetHeight = getEntityHeight(store, tgtRef);
                                        boolean tallTarget = (targetHeight >= 1.9);
                                        double allowedDy = tallTarget ? 3.0 : 1.5;

                                        if (tallTarget && h2 < 0.90 && (now - rec.lastCombatNudgeMillis) > 900L) {
                                            try {
                                                double len = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                                                double nx = (len > 0.001) ? (-dx2 / len) : 1.0;
                                                double nz = (len > 0.001) ? (-dz2 / len) : 0.0;
                                                Vector3d npos = new Vector3d(np2.getX() + nx * 0.80, np2.getY(), np2.getZ() + nz * 0.80);
                                                npcT.teleportPosition(npos);
                                                rec.lastCombatNudgeMillis = now;
                                            } catch (Throwable ignored) {}
                                        }

                                        boolean tooFar = (h2 > 20.0);
                                        boolean tooHigh = (ady2 > allowedDy);

                                        // Stuck: se não conseguir reduzir distância por ~2s
                                        if (now - rec.lastTargetSampleMillis >= 350L) {
                                            if (rec.lastTargetHorizontal >= 0 && h2 >= (rec.lastTargetHorizontal - 0.20)) {
                                                if (rec.targetStuckSinceMillis == 0L) rec.targetStuckSinceMillis = now;
                                            } else {
                                                rec.targetStuckSinceMillis = 0L;
                                            }
                                            rec.lastTargetHorizontal = h2;
                                            rec.lastTargetSampleMillis = now;
                                        }
                                        boolean stuckTooLong = (rec.targetStuckSinceMillis > 0L && (now - rec.targetStuckSinceMillis) > 2000L);

                                        boolean lostCond = tooFar || tooHigh || stuckTooLong;
                                        if (lostCond) {
                                            if (rec.targetLostSinceMillis == 0L) rec.targetLostSinceMillis = now;
                                        } else {
                                            rec.targetLostSinceMillis = 0L;
                                        }

                                        // Renova janela de combate por sinais válidos (evita expirar enquanto persegue)
                                        if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                                            boolean okToRenew = (!lostCond) && (h2 <= 25.0);
                                            if (okToRenew) {
                                                rec.combatUntilMillis = now + COMBAT_WINDOW_MILLIS;
                                            }
                                        }

                                        // Se perdeu o target por ~2s → limpa alvo e volta a seguir (A)
                                        if (rec.targetLostSinceMillis > 0L && (now - rec.targetLostSinceMillis) > 2000L) {
                                            if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                                                rec.combatUntilMillis = 0L;
                                                rec.combatTargetRefObj = null;
                                                combatTarget = null;
                                            } else {
                                                // perdeu assist
                                                rec.assistTargetRefObj = null;
                                                rec.assistUntilMillis = now + ASSIST_GRACE_MILLIS;
                                            }
                                            rec.targetLostSinceMillis = 0L;
                                            rec.targetStuckSinceMillis = 0L;
                                            rec.lastTargetHorizontal = -1.0;
                                            rec.lastTargetSampleMillis = 0L;
                                            desiredTarget = ownerRef;
                                            inCombatOrAssist = false;
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                        } else {
                            rec.targetLostSinceMillis = 0L;
                            rec.targetStuckSinceMillis = 0L;
                            rec.lastTargetHorizontal = -1.0;
                            rec.lastTargetSampleMillis = 0L;
                        }

                        Object npcEntityObj = getComponentFromStore(store, rec.refObj, NPCEntity.getComponentType());
                        if (npcEntityObj != null) {
                            // Em combate/assist: LockedTarget aponta para o alvo (perseguição em movimento).
                            // Fora: LockedTarget volta a seguir o dono.
                            setLockedTargetOnNpcEntity(npcEntityObj, inCombatOrAssist ? desiredTarget : ownerRef);
                            setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", inCombatOrAssist ? desiredTarget : null);

                            // Força estado para ajudar repath/velocidade/animações
                            setFlockState(store, rec.refObj, inCombatOrAssist ? "Run" : "Walk", "");

                            // Ataque corpo a corpo quando há alvo
                            if (inCombatOrAssist) {
                                tryMeleeAttack(store, rec, ownerRef, desiredTarget, now);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
	// AUTOLOOT (/autoloot)
	                // ON: coleta tudo num raio fixo, mesmo em combate.
	                // OFF: não coleta nada.
	                // Esta flag precisa existir fora do bloco try/catch acima.
	                boolean inCombatOrAssistNow = rec.defendeEnabled && ((now < rec.combatUntilMillis) || (now < rec.assistUntilMillis));
	                if (rec.autoLootEnabled) {
                    // Loot pós-combate (combat tags): só fora de combate e quando não há inimigo muito perto.
                    // (Durante combate/assist, essa rotina pausa automaticamente.)
	                    tickCombatTaggedLooting(store, owner, rec, ownerRef, np, op, now, worldObj, inCombatOrAssistNow);

                    // Auto-loot curto: coleta itens próximos no raio fixo.
                    // Observação: o /autoloot ON permite coletar mesmo em combate.
                    tryAutoLoot(store, owner, rec, np, op, now, worldObj);
                } else {
                    // /autoloot OFF: desliga qualquer looting.
                    endCombatTaggedLooting(rec);
                }

// Teleporte de segurança (RESGATE) — só quando realmente "travou"/se perdeu.
                // Evita teleporte quando o NPC está caminhando normalmente.
                // Teleporte só como "resgate" (não como modo normal de follow)
                // - Deixa o role fazer o Seek/Walk/Run.
                // - Se o NPC ficar travado e MUITO longe, aí sim teleport.
                boolean stalled = (now - rec.lastNpcMovedMillis) > 6000L;
                boolean stalledShort = (now - rec.lastNpcMovedMillis) > 3500L;

                // Seguimento (fora de combate): se ficar muito longe e travar, faz resgate mais cedo.
	                boolean inCombatOrAssist = inCombatOrAssistNow;
                boolean followSoft = (horizontal > 25.0) || (Math.abs(dy) > 12.0);
                boolean followHard = (horizontal > 35.0) || (Math.abs(dy) > 18.0);

                boolean farHard = (horizontal > 120.0) || (Math.abs(dy) > 30.0);
                boolean farSoft = (horizontal > 70.0) || (Math.abs(dy) > 20.0);

                boolean shouldRescue = false;
                // Se ficar fora do raio de 25 blocos, faz resgate para perto.
                // - Fora de combate: ~2.5s
                // - Em combate (player fugiu): ~1.5s (para não ficar preso no mob)
                if (rec.farSinceMillis > 0L) {
                    long limit = inCombatOrAssist ? 1500L : 2500L;
                    if ((now - rec.farSinceMillis) > limit) {
                        shouldRescue = true;
                    }
                }
                if (!inCombatOrAssist) {
                    shouldRescue = shouldRescue || followHard || (followSoft && stalledShort);
                }
                // Resgate extremo sempre (mesmo em combate), para não perder o NPC.
                if (!shouldRescue) {
                    shouldRescue = farHard || (farSoft && stalled);
                }

                if (shouldRescue && (now - rec.lastTeleportMillis) > 3000L) {
                    rec.lastTeleportMillis = now;

                    // Se o player fugiu no meio do combate, limpamos o alvo e voltamos a seguir.
                    if (inCombatOrAssist) {
                        rec.combatUntilMillis = 0L;
                        rec.combatTargetRefObj = null;
                        rec.assistUntilMillis = 0L;
                        rec.assistTargetRefObj = null;
                        rec.targetLostSinceMillis = 0L;
                        rec.targetStuckSinceMillis = 0L;
                        rec.lastTargetHorizontal = -1.0;
                        rec.lastTargetSampleMillis = 0L;
                    }

                    // Posiciona um pouco afastado do player (4~7 blocos), preferindo a direção de onde o NPC veio.
                    double vx = np.getX() - op.getX();
                    double vz = np.getZ() - op.getZ();
                    double vlen = Math.sqrt(vx * vx + vz * vz);
                    double ox = (vlen > 1.0e-6) ? (vx / vlen) : 1.0;
                    double oz = (vlen > 1.0e-6) ? (vz / vlen) : 0.0;
                    double dist = 6.0;
                    nt.teleportPosition(new com.hypixel.hytale.math.vector.Vector3d(
                            op.getX() + (ox * dist),
                            op.getY(),
                            op.getZ() + (oz * dist)
                    ));
                }
            } catch (Throwable ignored) {}
        });
    }
}

/**
     * Ajusta flags de movimento do NPC para bater com o estado do dono e com a decisão de mover.
     * Isso ajuda o modelo a escolher animações corretas (idle/walk/run) sem depender de nomes específicos.
     */
    private static void applyMovementAnimation(Store<EntityStore> store,
                                               Ref<EntityStore> npcRef,
                                               MovementStates ownerStates,
                                               double ownerSpeed,
                                               double npcSpeed,
                                               boolean moving,
                                               double distance) {
        try {
            MovementStatesComponent msComp = store.ensureAndGetComponent(npcRef, MovementStatesComponent.getComponentType());

            // Copia estados do player como base (crouch/jump etc), mas decide andar/correr pelo estado real do NPC.
            MovementStates s = (ownerStates != null) ? new MovementStates(ownerStates) : new MovementStates();

            // Mantém coerência mínima
            s.onGround = true;

            // Se não está movendo, ou se a velocidade real é muito baixa, força idle
            if (!moving || npcSpeed < 0.20) {
                s.idle = true;
                s.horizontalIdle = true;

                s.walking = false;
                s.running = false;
                s.sprinting = false;
            } else {
                boolean ownerSprint = ownerStates != null && ownerStates.sprinting;
                boolean ownerRun = ownerStates != null && (ownerStates.running || ownerStates.sprinting);

                // Decide andar/correr baseado no contexto (espelha o player quando fizer sentido)
                // Animação: só corre quando realmente precisa (ex.: ficou distante do dono)
                boolean wantSprint = ownerSprint || ownerSpeed > 6.0 || distance > 35.0;
                boolean wantRun = ownerRun || ownerSpeed > 4.2 || distance > 25.0;

                // Se o NPC ainda não ganhou velocidade de verdade, evita "correndo parado"
                if (npcSpeed < 1.2) {
                    wantSprint = false;
                    wantRun = false;
                }

                s.idle = false;
                s.horizontalIdle = false;

                s.walking = !wantRun && !wantSprint;
                s.running = wantRun && !wantSprint;
                s.sprinting = wantSprint;
            }

            msComp.setMovementStates(s);
            // Em algumas builds, o engine usa "sent" para rede; manter junto ajuda.
            msComp.setSentMovementStates(new MovementStates(s));
        } catch (Throwable ignored) {}
    }


    /**
     * Revive do NPC.
     * @param manual se foi revive manual (penalidade menor – implementaremos depois)
     */
    public void revive(UUID ownerId, boolean manual) {
        NpcRecord rec = ownerId == null ? null : npcRefPorPlayer.get(ownerId);
        if (rec == null) return;
        if (rec.refObj == null) return;
        if (rec.worldObj == null) return;

        rec.downed = false;
        rec.downedUntilMillis = 0L;

        // XP já foi penalizado no momento em que o NPC entrou em DOWNED (markDowned).
        // Aqui apenas restauramos HP/estado.

        HytaleBridge.worldExecute(rec.worldObj, () -> {
            try {
                Object componentStore = getComponentStoreFromWorld(rec.worldObj);
                if (!(componentStore instanceof Store<?> rawStore)) return;

                @SuppressWarnings("unchecked")
                Store<EntityStore> store = (Store<EntityStore>) rawStore;
                @SuppressWarnings("unchecked")
                Ref<EntityStore> ref = (Ref<EntityStore>) rec.refObj;

                // Garante stats/vida
                EntityStatMap stats = store.ensureAndGetComponent(ref, EntityStatMap.getComponentType());
                stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());

                // Aplica scaling de HP/DEF conforme level (cap lvl 100) e cura ao máximo
                applyNpcScaling(store, ref, ownerId, rec, true);

                // Garante componentes de animação (para o engine conseguir tocar hurt/death etc)
                store.ensureComponent(ref, ActiveAnimationComponent.getComponentType());
                store.ensureComponent(ref, MovementStatesComponent.getComponentType());
                store.putComponent(ref, RespondToHit.getComponentType(), RespondToHit.INSTANCE);

            } catch (Throwable ignored) {
            }
        });
    }

    // =========================================================
    // Store / World helpers
    // =========================================================

    private static Object getComponentStoreFromWorld(Object worldObj) {
        Object entityStore = invokeNoArg(worldObj, "getEntityStore", "entityStore");
        if (entityStore == null) return null;
        return invokeNoArg(entityStore, "getStore", "store");
    }

    /**
     * Pega a posição do player diretamente do Store entregue pela API de comandos.
     */
    private static Object tryGetPositionFromStore(Object componentStore, Object playerEntityRef) {
        try {
            Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
            Method getCt = tcClass.getMethod("getComponentType");
            Object componentType = getCt.invoke(null);
            if (componentType == null) return null;

            Object tc = invokeStoreGetComponent(componentStore, playerEntityRef, componentType);
            if (tc == null) return null;
            return invokeNoArg(tc, "getPosition", "position");
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Pega a posição do dono via:
     * world.getEntityRef(UUID) -> Store.getComponent(ref, TransformComponent.getComponentType()).getPosition()
     */
    private static Object tryGetOwnerPositionFromWorldStore(Object worldObj, Object componentStore, UUID ownerId) {
        try {
            Object ref = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
            if (ref == null) return null;

            Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
            Method getCt = tcClass.getMethod("getComponentType");
            Object componentType = getCt.invoke(null);
            if (componentType == null) return null;

            Object tc = invokeStoreGetComponent(componentStore, ref, componentType);
            if (tc == null) return null;

            return invokeNoArg(tc, "getPosition", "position");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeStoreGetComponent(Object store, Object ref, Object componentType) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("getComponent")) continue;
                if (m.getParameterCount() != 2) continue;
                return m.invoke(store, ref, componentType);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Helper compat: busca componente no Store via reflection (Store.getComponent(ref, componentType)).
     */
    private static Object getComponentFromStore(Object store, Object ref, Object componentTypeObj) {
        return invokeStoreGetComponent(store, ref, componentTypeObj);
    }


    // =========================================================
    // spawnNPC + Pair/ref extraction
    // =========================================================

    private static Object invokeSpawnNPC(Object npcPlugin, Object store, String npcType, String groupType, Object pos, Object rot) {
        try {
            for (Method m : npcPlugin.getClass().getMethods()) {
                if (!m.getName().equals("spawnNPC")) continue;
                if (m.getParameterCount() != 5) continue;
                try {
                    return m.invoke(npcPlugin, store, npcType, groupType, pos, rot);
                } catch (IllegalArgumentException ignoredTryOtherOverload) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int getNpcRoleIndex(Object npcPlugin, String roleName) {
        try {
            Method m = npcPlugin.getClass().getMethod("getIndex", String.class);
            Object r = m.invoke(npcPlugin, roleName);
            return (r instanceof Integer) ? (Integer) r : ((Number) r).intValue();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /**
     * Em algumas builds/modloaders, o nome do role pode ser registrado com o caminho relativo
     * (ex.: "_Core/Tests/Magic_Lantern"). Então tentamos alguns formatos comuns.
     */
        private static int getNpcRoleIndexWithFallbacks(Object npcPlugin, String roleName) {
        if (roleName == null || roleName.isBlank()) return -1;

        // O ID pode variar conforme como o builder registra (apenas nome, ou caminho relativo).
        String rn = roleName;

        String[] candidates = new String[] {
            rn,
            rn.endsWith(".json") ? rn.substring(0, rn.length() - 5) : rn,
            "_Core/" + rn,
            "_Core/AmigoNPC/" + rn,
            "Server/NPC/Roles/" + rn,
            "Server/NPC/Roles/_Core/" + rn,
            "Server/NPC/Roles/_Core/AmigoNPC/" + rn
        };

        for (String c : candidates) {
            if (c == null || c.isBlank()) continue;
            // remove extensão se alguém passar com .json
            String key = c.endsWith(".json") ? c.substring(0, c.length() - 5) : c;
            int idx = getNpcRoleIndex(npcPlugin, key);
            if (idx >= 0) return idx;
        }

        return -1;
    }


    private static Object invokeSpawnEntity(Object npcPlugin, Object store, int roleIndex, Object pos, Object rot, Object model, Object ownerRef) {
        try {
            // assinatura: spawnEntity(Store, int, Vector3d, Vector3f, Model, TriConsumer, TriConsumer)
            for (Method m : npcPlugin.getClass().getMethods()) {
                if (!m.getName().equals("spawnEntity")) continue;
                if (m.getParameterCount() != 7) continue;
                try {
                    Object pre = buildPreAddToWorldTriConsumer();
                    Object post = buildPostSpawnTriConsumer(ownerRef);
                    return m.invoke(npcPlugin, store, roleIndex, pos, rot, model, pre, post);
                } catch (IllegalArgumentException ignoredTryOtherOverload) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Pós-spawn: fixa o alvo do role no dono (LockedTarget), pra usar Sensor Type=Target no role.
     * Assinatura esperada: TriConsumer<NPCEntity, Ref<EntityStore>, Store<EntityStore>>
     */
    private static Object buildPostSpawnTriConsumer(Object ownerRef) {
        if (ownerRef == null) return buildNoopTriConsumer();
        try {
            Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
            return java.lang.reflect.Proxy.newProxyInstance(
                    tri.getClassLoader(),
                    new Class<?>[]{tri},
                    (proxy, method, args) -> {
                        try {
                            if (args != null && args.length >= 1 && args[0] != null) {
                                Object npcEntity = args[0];
                                setLockedTargetOnNpcEntity(npcEntity, ownerRef);
                            }
                        } catch (Throwable ignored) {}
                        return null;
                    }
            );
        } catch (Throwable ignored) {
            return buildNoopTriConsumer();
        }
    }

    /**
     * Tenta fixar o marked target "LockedTarget" no role do NPC.
     * Implementado por reflexão para aguentar variações de build.
     */
        private static void setMarkedTargetOnNpcEntity(Object npcEntity, String slot, Object targetRef) {
        if (npcEntity == null || slot == null) {
            return;
        }

        try {
            Object role = invokeNoArg(npcEntity, "getRole", "role");
            if (role == null) {
                return;
            }

            // Tentativas direto no NPCEntity (algumas builds não usam setMarkedTarget no role)
            if (invokeTwoArgs(npcEntity, "onFlockSetTarget", String.class, Object.class, slot, targetRef)) {
                return;
            }
            if (invokeTwoArgs(npcEntity, "onFlockSetMarkedTarget", String.class, Object.class, slot, targetRef)) {
                return;
            }
            if (invokeTwoArgs(npcEntity, "onFlockSetMarkedEntity", String.class, Object.class, slot, targetRef)) {
                return;
            }

            // A API pode expor nomes diferentes (setMarkedTarget / setMarkedEntity / markEntity).
            if (invokeTwoArgs(role, "setMarkedTarget", String.class, Object.class, slot, targetRef)) {
                return;
            }
            if (invokeTwoArgs(role, "setMarkedEntity", String.class, Object.class, slot, targetRef)) {
                return;
            }
            if (invokeTwoArgs(role, "markEntity", String.class, Object.class, slot, targetRef)) {
                return;
            }

            // Se targetRef for null, tentamos limpar o slot.
            if (targetRef == null) {
                // Alguns builds expõem APIs diferentes para limpar/altera slots de alvo.
                // Aqui é best-effort: tentamos várias assinaturas sem depender de retorno boolean.
                invokeOneArg(npcEntity, "onFlockClearTarget", String.class, slot);
                invokeOneArg(npcEntity, "onFlockRemoveTarget", String.class, slot);
                invokeOneArg(role, "clearMarkedTarget", String.class, slot);
                invokeOneArg(role, "removeMarkedTarget", String.class, slot);
                invokeOneArg(role, "clearMarkedEntity", String.class, slot);
                invokeOneArg(role, "removeMarkedEntity", String.class, slot);
                invokeOneArg(role, "unmarkEntity", String.class, slot);
            }
        } catch (Throwable ignored) {
            // best-effort
        }
    }

    private static void setLockedTargetOnNpcEntity(Object npcEntity, Object ownerRef) {
        if (npcEntity == null || ownerRef == null) {
            return;
        }
        setMarkedTargetOnNpcEntity(npcEntity, "LockedTarget", ownerRef);
    }


    /**
     * Troca o State do Role para permitir seguir com distância diferente em combate.
     * - Idle (fora de combate)
     * - Combat (em combate/assist)
     *
     * Implementado por reflexão para aguentar variações de build.
     */
    private static void setRoleStateOnNpcEntity(Object npcEntity, Object npcRefObj, Object storeObj, boolean combat) {
        if (npcEntity == null || npcRefObj == null || storeObj == null) return;
        final String state = combat ? "Combat" : "Idle";
        final String sub = "Default";
        try {
            Object role = invokeNoArg(npcEntity, "getRole", "role");
            if (role == null) return;

            Object stateSupport = invokeNoArg(role, "getStateSupport", "stateSupport");
            if (stateSupport == null) return;

            // Preferência: StateSupport.setState(Ref, String, String, ComponentAccessor)
            for (Method m : stateSupport.getClass().getMethods()) {
                if (!m.getName().equals("setState")) continue;
                if (m.getParameterCount() != 4) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 4) continue;
                if (p[1] != String.class) continue;
                if (p[2] != String.class) continue;
                try {
                    m.invoke(stateSupport, npcRefObj, state, sub, storeObj);
                    return;
                } catch (IllegalArgumentException ignoredTryOther) {
                    // tenta outras assinaturas
                }
            }

            // Fallback: tentar flockSetState(ref, state, sub, accessor)
            for (Method m : stateSupport.getClass().getMethods()) {
                if (!m.getName().equals("flockSetState")) continue;
                if (m.getParameterCount() != 4) continue;
                try {
                    m.invoke(stateSupport, npcRefObj, state, sub, storeObj);
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        }
    }

    /**
    * Compat: algumas versões do mod tentavam alternar "Walk/Run" via um helper.
    * No role atual (Amigo_Follow.json) isso já é controlado por RelativeSpeed no Seek,
    * então aqui fazemos apenas best-effort/no-op para não quebrar compilação.
    */
    private static void setFlockState(Store<EntityStore> store, Object npcRefObj, String state, String subState) {
        // Intencionalmente vazio (best-effort). Mantido para compatibilidade.
        // Se no futuro quisermos alternar controladores/velocidade via API, fazemos por reflexão aqui.
    }

    /**
     * Ataque corpo a corpo simples e robusto: dispara dano PHYSICAL usando o pipeline oficial (DamageSystems).
     *
     * Por que isso é necessário?
     * - Nosso Role custom (Amigo_Follow) só faz Seek/Follow; ele não contém Action=Attack.
     * - Então 'LockedTarget' sozinho faz o NPC perseguir o alvo, mas não bater.
     *
     * Regras:
     * - Só ataca quando existir alvo diferente do owner.
     * - Se defender OFF: só ataca agressor ativo (combatTarget).
     * - Se defender ON: ataca agressor, senão ataca assistTarget.
     * - Respeita cooldown e bloqueia PvP (player alvo) quando amigopvp estiver OFF.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tryMeleeAttack(Store<EntityStore> store, NpcRecord rec, Object ownerRefObj, Object targetRefObj, long now) {
        try {
            if (store == null || rec == null) return;
            if (rec.downed) return;
            if (targetRefObj == null || ownerRefObj == null) return;
            if (targetRefObj == ownerRefObj) return;

            // Só ataca quando /amigo defender está ON
            if (!rec.defendeEnabled) return;

            boolean isAggressor = (rec.combatTargetRefObj != null && refEq(targetRefObj, rec.combatTargetRefObj));
            boolean isAssist = (rec.assistTargetRefObj != null && refEq(targetRefObj, rec.assistTargetRefObj));
            if (!isAggressor && !isAssist) return;

            // Cooldown por estilo: agressivo (agressor) ~650ms; defensivo (assistência) ~850ms
            long cd = isAggressor ? 650L : MELEE_COOLDOWN_MILLIS;
            if (now - rec.lastMeleeAttackMillis < cd) return;

            UUID ownerId = rec.ownerId;

            if (!(targetRefObj instanceof Ref)) return;
            if (!(rec.refObj instanceof Ref)) return;

            Ref<EntityStore> targetRef = (Ref<EntityStore>) targetRefObj;
            Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

            // PvP: não bater em players se amigopvp estiver OFF
            try {
                Player maybePlayer = store.getComponent(targetRef, Player.getComponentType());
                if (maybePlayer != null && !pvpEnabled) {
                    debugAttack(rec, ownerId, "Bloqueado PvP (alvo é player). Arma=" + rec.equippedWeaponId);
                    return;
                }
            } catch (Throwable ignored) {}

            TransformComponent npcT = store.getComponent(npcRef, TransformComponent.getComponentType());
            TransformComponent tgtT = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (npcT == null || tgtT == null) {
                debugAttack(rec, ownerId, "Sem TransformComponent (npcT=" + (npcT!=null) + ", tgtT=" + (tgtT!=null) + ").");
                return;
            }

            var np = npcT.getPosition();
            var tp = tgtT.getPosition();
            if (np == null || tp == null) {
                debugAttack(rec, ownerId, "Sem posição (np=" + (np!=null) + ", tp=" + (tp!=null) + ").");
                return;
            }

            double dx = tp.getX() - np.getX();
            double dz = tp.getZ() - np.getZ();
            double dy = tp.getY() - np.getY();
            double horizontal = Math.sqrt(dx*dx + dz*dz);

            // Pode atacar com tolerância vertical baseada na altura do alvo (mobs ~2 blocos ou mais)
            double targetHeight = getEntityHeight(store, targetRef);
            boolean tallTarget = (targetHeight >= 1.9);
            double allowedDy = tallTarget ? 3.0 : 1.5;

            // Alcance de espada (tuning): usa distância efetiva até a "borda" do alvo
            // (centro-a-centro é enganoso para mobs maiores e gerava "Fora do alcance" mesmo colado)
            double targetRadius = getEntityRadiusXZ(store, targetRef);
            double effectiveH = Math.max(0.0, horizontal - targetRadius);
            double reach = 2.6;
            if (effectiveH > reach) {
                debugAttack(rec, ownerId, String.format(
                        "Fora do alcance: h=%.2f eff=%.2f r=%.2f dy=%.2f arma=%s",
                        horizontal, effectiveH, targetRadius, dy, String.valueOf(rec.equippedWeaponId)));
                return;
            }
            if (Math.abs(dy) > allowedDy) {
                debugAttack(rec, ownerId, String.format("Diferença de altura alta: h=%.2f dy=%.2f allowedDy=%.2f arma=%s", horizontal, dy, allowedDy, String.valueOf(rec.equippedWeaponId)));
                return;
            }

            // Dano: base da arma (best-effort via Item.itemLevel) + bônus leve da perícia
            String weaponId = rec.equippedWeaponId;
            if (weaponId == null || weaponId.isBlank()) {
                weaponId = SwordProgression.weaponIdForLevel(rec.level);
            }
            double base = getWeaponBaseDamage(weaponId);
            float amount = (float) Math.min(20.0, base + (rec.level * 0.03));

            // ✅ Balanceamento do dano por nível da perícia (espadas):
            // lvl 1..30  => 1/3 do poder
            // lvl 31..60 => 1/2 do poder
            // lvl 61+    => normal
            int swordLvl = SwordProgression.clampLevel(rec.level);
            if (swordLvl <= 30) {
                amount = amount / 3.0f;
            } else if (swordLvl <= 60) {
                amount = amount / 2.0f;
            }

            // HP do alvo (best-effort) p/ detectar kill e dar XP ao NPC
            float hpBefore = -1f;
            try {
                EntityStatMap tgtStats = store.getComponent(targetRef, EntityStatMap.getComponentType());
                if (tgtStats != null) {
                    hpBefore = tgtStats.get(DefaultEntityStatTypes.getHealth()).get();
                }
            } catch (Throwable ignored) {
            }


// Combat tag (anti-roubo do loot): registra alvo e posição do combate
recordCombatTag(ownerId, targetRefObj, store);

            Damage damage = new Damage(new Damage.EntitySource(npcRef), DamageCause.PHYSICAL, amount);
            DamageSystems.executeDamage(targetRef, (ComponentAccessor) store, damage);

            // Best-effort: tenta fazer o inimigo também mirar no NPC (assim como mira no player)
            pulseAggroToNpc(store, targetRefObj, npcRef, rec, now);

            boolean killed = false;
            try {
                EntityStatMap tgtStats2 = store.getComponent(targetRef, EntityStatMap.getComponentType());
                if (tgtStats2 != null && hpBefore > 0f) {
                    float hpAfter = tgtStats2.get(DefaultEntityStatTypes.getHealth()).get();
                    killed = hpAfter <= 0f;
                }
            } catch (Throwable ignored) {
            }

            long xpGain = killed ? XP_PER_KILL : XP_PER_HIT;
            if (xpGain > 0L) {
                addNpcXp(store, npcRef, ownerId, rec, xpGain);
            }

            if (killed) {
                // Mensagem no chat (o sendToOwner já aplica o prefixo [AmigoNPC])
                sendToOwner(rec.worldObj, ownerId, "Recebi " + xpGain + " de xp.");

                // Defender ON: retarget imediato para outro alvo perto do NPC, sem esperar o corpo sumir
                tryRetargetAfterKill(store, rec, ownerRefObj, npcRef, targetRefObj, now);
            }

            debugAttack(rec, ownerId, String.format("HIT! dmg=%.2f arma=%s h=%.2f dy=%.2f", amount, String.valueOf(rec.equippedWeaponId), horizontal, dy));

            // Animação de ataque correspondente à arma (best-effort)
            playWeaponAttackAnimation(store, npcRef, rec, weaponId, now);

            rec.lastMeleeAttackMillis = now;

            // Sinal de combate: hit renova janela do agressor
            if (isAggressor) {
                rec.combatUntilMillis = now + COMBAT_WINDOW_MILLIS;
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Best-effort: tenta fazer o inimigo atacar também o NPC (e não só o player), marcando o NPC como alvo.
     * Não é perfeito (depende da IA do mob), mas ajuda a "dividir aggro".
     */
    private void pulseAggroToNpc(Store<EntityStore> store, Object targetRefObj, Object npcRefObj, NpcRecord rec, long now) {
        try {
            if (store == null || targetRefObj == null || npcRefObj == null || rec == null) return;
            if (now - rec.lastAggroPulseMillis < 900L) return; // rate-limit
            rec.lastAggroPulseMillis = now;

            Object mobNpcEntityObj = getComponentFromStore(store, targetRefObj, NPCEntity.getComponentType());
            if (mobNpcEntityObj == null) return;

            // Não mexe em PvP de player aqui; isso é só para mobs/NPCs.
            setMarkedTargetOnNpcEntity(mobNpcEntityObj, "CombatTarget", npcRefObj);
            try {
                setLockedTargetOnNpcEntity(mobNpcEntityObj, npcRefObj);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {
        }
    }

    /**
     * Defender ON: após kill do NPC, pega imediatamente outro alvo próximo do NPC.
     * Evita ficar "parado" esperando o corpo do mob sumir.
     */
    private void tryRetargetAfterKill(Store<EntityStore> store, NpcRecord rec, Object ownerRefObj, Object npcRefObj, Object deadRefObj, long now) {
        try {
            if (store == null || rec == null || !rec.defendeEnabled) return;
            if (!(npcRefObj instanceof Ref) || !(rec.refObj instanceof Ref)) return;

            @SuppressWarnings("unchecked")
            Ref<EntityStore> npcRef = (Ref<EntityStore>) rec.refObj;

            TransformComponent nt = store.getComponent(npcRef, TransformComponent.getComponentType());
            if (nt == null || nt.getPosition() == null) return;

            Object next = findNearestTargetNearNpc(store, rec, ownerRefObj, nt.getPosition(), deadRefObj);
            if (next == null) return;

            // limpa agressor se for o que morreu
            if (rec.combatTargetRefObj != null && refEq(deadRefObj, rec.combatTargetRefObj)) {
                rec.combatUntilMillis = 0L;
                rec.combatTargetRefObj = null;
            }

            rec.assistTargetRefObj = next;
            rec.assistUntilMillis = 0L;

            Object npcEntityObj = getComponentFromStore(store, npcRefObj, NPCEntity.getComponentType());
            if (npcEntityObj != null) {
                setLockedTargetOnNpcEntity(npcEntityObj, next);
                setMarkedTargetOnNpcEntity(npcEntityObj, "CombatTarget", next);
                setFlockState(store, rec.refObj, "Run", "");
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Toca uma animação de ataque baseada na arma atual do NPC (best-effort).
     *
     * Fonte de verdade: ItemPlayerAnimations apontado pelo Item (playerAnimationsId).
     * Se não encontrar, não toca nada (evita travar animação errada).
     */
    private static void playWeaponAttackAnimation(Store<EntityStore> store, Ref<EntityStore> npcRef, NpcRecord rec, String weaponId, long now) {
        try {
            if (store == null || npcRef == null || rec == null) return;

            // 1) Tenta tocar swing/attack usando ItemPlayerAnimations (padrão do player).
            // Aqui o "animationId" é a CHAVE (ex.: "attack"), não o id thirdPerson.
            String itemAnimsId = null;
            ItemPlayerAnimations ipa = null;
            try {
                if (weaponId != null && !weaponId.isBlank()) {
                    Item it = Item.getAssetMap().getAsset(weaponId);
                    if (it != null) {
                        boolean usePlayerAnims = false;
                        try { usePlayerAnims = it.getUsePlayerAnimations(); } catch (Throwable ignored) {}
                        try { itemAnimsId = it.getPlayerAnimationsId(); } catch (Throwable ignored) {}
                        if ((itemAnimsId == null || itemAnimsId.isBlank()) && usePlayerAnims) {
                            try { itemAnimsId = ItemPlayerAnimations.DEFAULT_ID; } catch (Throwable ignored) {}
                        }
                    }
                }
            } catch (Throwable ignored) {}

            if (itemAnimsId == null || itemAnimsId.isBlank()) {
                try { itemAnimsId = ItemPlayerAnimations.DEFAULT_ID; } catch (Throwable ignored) { itemAnimsId = null; }
            }

            try {
                if (itemAnimsId != null && !itemAnimsId.isBlank()) {
                    ipa = ItemPlayerAnimations.getAssetMap().getAsset(itemAnimsId);
                }
            } catch (Throwable ignored) {
                ipa = null;
            }

            String key = null;
            try {
                if (ipa != null) {
                    key = pickAttackKeyFromPlayerAnimations(ipa.getAnimations());
                }
            } catch (Throwable ignored) {
                key = null;
            }

            if (key == null || key.isBlank()) {
                // fallback seguro (chave comum)
                key = "attack";
            }

            boolean played = false;
            try {
                if (itemAnimsId != null && !itemAnimsId.isBlank()) {
                    AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, itemAnimsId, key, (ComponentAccessor) store);
                    played = true;
                } else {
                    // overload sem itemAnimationsId
                    AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, key, (ComponentAccessor) store);
                    played = true;
                }
            } catch (Throwable ignored) {
                played = false;
            }

            if (played) {
                rec.lastAttackAnimId = key;
                rec.clearAttackAnimAtMillis = now + 650L;
                return;
            }

            // 2) Fallback (best-effort): tenta uma animação de ataque do próprio modelo (ex.: Wraith)
            try {
                AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, "attack", true, (ComponentAccessor) store);
                rec.lastAttackAnimId = "attack";
                rec.clearAttackAnimAtMillis = now + 650L;
            } catch (Throwable ignored) {
            }

        } catch (Throwable ignored) {
        }
    }

    /** Limpa a animação de ataque anterior (somente se ainda estiver ativa no slot Action). */
    @SuppressWarnings("unchecked")
    private static void clearExpiredAttackAnimation(Store<EntityStore> store, Object npcRefObj, NpcRecord rec, long now) {
        try {
            if (store == null || rec == null) return;
            if (rec.clearAttackAnimAtMillis <= 0L) return;
            if (now < rec.clearAttackAnimAtMillis) return;

            String last = rec.lastAttackAnimId;
            rec.clearAttackAnimAtMillis = 0L;
            rec.lastAttackAnimId = null;
            if (last == null || last.isBlank()) return;
            if (!(npcRefObj instanceof Ref)) return;

            Ref<EntityStore> npcRef = (Ref<EntityStore>) npcRefObj;
            ActiveAnimationComponent anim = store.getComponent(npcRef, ActiveAnimationComponent.getComponentType());
            if (anim == null) return;

            String[] active = anim.getActiveAnimations();
            int idx = -1;
            try { idx = AnimationSlot.Action.getValue(); } catch (Throwable ignored) {}
            String current = (active != null && idx >= 0 && idx < active.length) ? active[idx] : null;

            // Só limpa se ainda for exatamente a animação que nós colocamos (não cortar hurt/death)
            if (last.equals(current)) {
                try {
                    AnimationUtils.stopAnimation(npcRef, AnimationSlot.Action, (ComponentAccessor) store);
                } catch (Throwable ignored) {
                    anim.setPlayingAnimation(AnimationSlot.Action, null);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Resolve a chave de animação de ataque para um itemId (cache + heurística de keys). */
    private static String resolveWeaponAttackAnimId(String weaponId, boolean moving) {
        if (weaponId == null || weaponId.isBlank()) return null;
        String cacheKey = weaponId + (moving ? "|m" : "|s");
        if (WEAPON_ATTACK_ANIM_CACHE.containsKey(cacheKey)) {
            String v = WEAPON_ATTACK_ANIM_CACHE.get(cacheKey);
            return (v == null || v.isBlank()) ? null : v;
        }

        String foundKey = null;
        try {
            Item it = Item.getAssetMap().getAsset(weaponId);
            String animsId = null;
            boolean usePlayerAnims = false;
            if (it != null) {
                try { usePlayerAnims = it.getUsePlayerAnimations(); } catch (Throwable ignored) {}
                try { animsId = it.getPlayerAnimationsId(); } catch (Throwable ignored) {}
            }
            if ((animsId == null || animsId.isBlank()) && usePlayerAnims) {
                try { animsId = ItemPlayerAnimations.DEFAULT_ID; } catch (Throwable ignored) {}
            }
            if (animsId != null && !animsId.isBlank()) {
                ItemPlayerAnimations ipa = ItemPlayerAnimations.getAssetMap().getAsset(animsId);
                if (ipa != null) {
                    foundKey = pickAttackKeyFromPlayerAnimations(ipa.getAnimations());
                }
            }
        } catch (Throwable ignored) {
        }

        WEAPON_ATTACK_ANIM_CACHE.put(cacheKey, foundKey == null ? "" : foundKey);
        return foundKey;
    }

    /**
     * Heurística para achar a CHAVE de ataque dentro do mapa de ItemPlayerAnimations.
     * Preferimos keys com 'attack'/'melee'/'swing' etc.
     */
    private static String pickAttackKeyFromPlayerAnimations(java.util.Map<String, ItemAnimation> map) {
        try {
            if (map == null || map.isEmpty()) return null;

            String[] prefer = {
                    "attack", "primary", "primary_attack", "melee", "melee_attack", "swing", "hit", "strike", "slash", "stab", "chop"
            };

            // 1) match exato (ignore-case)
            for (String p : prefer) {
                for (String k : map.keySet()) {
                    if (k != null && k.equalsIgnoreCase(p)) {
                        return k;
                    }
                }
            }

            // 2) match parcial (contains)
            for (String k : map.keySet()) {
                if (k == null) continue;
                String lk = k.toLowerCase(Locale.ROOT);
                if (lk.contains("attack") || lk.contains("melee") || lk.contains("swing") || lk.contains("hit") || lk.contains("strike")) {
                    return k;
                }
            }

            // 3) fallback: primeira key
            for (String k : map.keySet()) {
                if (k != null && !k.isBlank()) return k;
            }

            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

private static Object buildNoopTriConsumer() {
        try {
            // com.hypixel.hytale.function.consumer.TriConsumer
            Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
            // Proxy com invoke vazio
            return java.lang.reflect.Proxy.newProxyInstance(
                    tri.getClassLoader(),
                    new Class<?>[]{tri},
                    (proxy, method, args) -> null
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Pré-add: garante componentes mínimos para dano/vida/animações.
     * Assinatura esperada: TriConsumer<NPCEntity, Holder<EntityStore>, Store<EntityStore>>
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private static Object buildPreAddToWorldTriConsumer() {
        try {
            Class<?> tri = Class.forName("com.hypixel.hytale.function.consumer.TriConsumer");
            return java.lang.reflect.Proxy.newProxyInstance(
                    tri.getClassLoader(),
                    new Class<?>[]{tri},
                    (proxy, method, args) -> {
                        if (args != null && args.length >= 2 && args[1] instanceof com.hypixel.hytale.component.Holder holder) {
                            try {
                                // Vida (EntityStats)
                                holder.ensureComponent(EntityStatMap.getComponentType());
                                EntityStatMap stats = (EntityStatMap) holder.ensureAndGetComponent(EntityStatMap.getComponentType());
                                stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());

                                // Animação de hit/death
                                holder.ensureComponent(ActiveAnimationComponent.getComponentType());
                                holder.ensureComponent(MovementStatesComponent.getComponentType());
                                holder.putComponent(RespondToHit.getComponentType(), RespondToHit.INSTANCE);
                            } catch (Throwable ignored) {
                            }
                        }
                        return null;
                    }
            );
        } catch (Throwable ignored) {
            return buildNoopTriConsumer();
        }
    }

    private static Object buildModelFromAssetId(String modelId, float scale) {
        try {
            // ModelAsset.getAssetMap().getAsset(modelId)
            Class<?> modelAssetClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset");
            Method getAssetMap = modelAssetClass.getMethod("getAssetMap");
            Object assetMap = getAssetMap.invoke(null);
            if (assetMap == null) return null;

            Method getAsset = assetMap.getClass().getMethod("getAsset", Object.class);
            Object asset = getAsset.invoke(assetMap, modelId);
            if (asset == null) return null;

            // Model.createScaledModel(ModelAsset, float)
            Class<?> modelClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.Model");
            Method create = modelClass.getMethod("createScaledModel", modelAssetClass, float.class);
            return create.invoke(null, asset, scale);

        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean looksLikeRef(Object o) {
        if (o == null) return false;
        String n = o.getClass().getName();
        return n.endsWith(".Ref") || n.endsWith("Ref") || n.contains(".Ref");
    }

    private static Object extractRefFromPair(Object pair) {
        if (pair == null) return null;
        String[] methods = { "getLeft", "getFirst", "getKey", "left", "first", "key" };
        for (String mname : methods) {
            try {
                Method m = pair.getClass().getMethod(mname);
                Object v = m.invoke(pair);
                if (v != null) return v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // =========================================================
    // RemoveEntity helpers (Store.removeEntity(ref, reason))
    // =========================================================

    private static boolean doRemoveEntity(Object componentStore, Object savedRefOrPair) {
        Object ref = savedRefOrPair;

        if (!looksLikeRef(ref)) {
            Object extracted = extractRefFromPair(ref);
            if (extracted != null) ref = extracted;
        }

        Object removeReason = getRemoveReasonBestEffort();
        if (removeReason == null) {
            setError("Não encontrei RemoveReason nesta build.");
            return false;
        }

        if (!tryInvokeRemoveEntity(componentStore, ref, removeReason)) {
            setError("Não achei Store.removeEntity(ref, reason) compatível nesta build.");
            return false;
        }

        return true;
    }

    private static Object getRemoveReasonBestEffort() {
        String[] enumCandidates = {
                "com.hypixel.hytale.component.RemoveReason",
                "com.hypixel.hytale.component.Store$RemoveReason"
        };
        for (String cn : enumCandidates) {
            Object r = getEnumConstantAny(cn, "DESPAWN", "COMMAND", "PLUGIN", "CUSTOM", "REMOVE", "SPAWN");
            if (r != null) return r;

            Object first = getFirstEnumValue(cn);
            if (first != null) return first;
        }
        return null;
    }

    private static Object getEnumConstantAny(String enumClassName, String... names) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (!enumClass.isEnum()) return null;
            @SuppressWarnings("unchecked")
            Class<? extends Enum> e = (Class<? extends Enum>) enumClass;

            for (String n : names) {
                try { return Enum.valueOf(e, n); }
                catch (IllegalArgumentException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getFirstEnumValue(String enumClassName) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (!enumClass.isEnum()) return null;
            Object[] vals = enumClass.getEnumConstants();
            return (vals != null && vals.length > 0) ? vals[0] : null;
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean tryInvokeRemoveEntity(Object store, Object ref, Object reason) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("removeEntity")) continue;
                if (m.getParameterCount() != 2) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p[1].isInstance(reason)) {
                    m.invoke(store, ref, reason);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // =========================================================
    // Position helpers
    // =========================================================

    private static Object tryGetSenderPosition(Object senderObj) {
        if (senderObj == null) return null;

        Object p = invokeNoArg(senderObj, "getPosition", "position");
        if (p != null) return p;

        Object transform = invokeNoArg(senderObj, "getTransform", "transform");
        if (transform != null) {
            Object p2 = invokeNoArg(transform, "getPosition", "position");
            if (p2 != null) return p2;
        }

        return null;
    }

    private static Object coerceToVector3d(Object maybe) {
        if (maybe == null) return null;

        if (hasXYZ(maybe)) return maybe;

        Object pos = invokeNoArg(maybe, "getPosition", "position");
        if (pos != null && hasXYZ(pos)) return pos;

        return null;
    }

    private static boolean hasXYZ(Object v) {
        if (v == null) return false;

        if (hasMethod(v, "getX") && hasMethod(v, "getY") && hasMethod(v, "getZ")) return true;
        if (hasMethod(v, "x") && hasMethod(v, "y") && hasMethod(v, "z")) return true;

        return hasField(v, "x") && hasField(v, "y") && hasField(v, "z");
    }

    private static boolean hasMethod(Object v, String name) {
        try {
            v.getClass().getMethod(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasField(Object v, String name) {
        try {
            v.getClass().getField(name);
            return true;
        } catch (Throwable ignoredPublic) {
            try {
                v.getClass().getDeclaredField(name);
                return true;
            } catch (Throwable ignoredDeclared) {
                return false;
            }
        }
    }

    private static Object offsetVector3d(Object vec, double ox, double oy, double oz) {
        try {
            double x = readCoord(vec, "x");
            double y = readCoord(vec, "y");
            double z = readCoord(vec, "z");

            try {
                return vec.getClass().getConstructor(double.class, double.class, double.class)
                        .newInstance(x + ox, y + oy, z + oz);
            } catch (Throwable ignoredCtor) {
                Object v2 = newVector3d(x + ox, y + oy, z + oz);
                return (v2 != null) ? v2 : vec;
            }
        } catch (Throwable ignored) {}
        return vec;
    }

    private static double readCoord(Object vec, String axis) throws Exception {
        String getName = "get" + axis.toUpperCase();
        try {
            Object r = vec.getClass().getMethod(getName).invoke(vec);
            return ((Number) r).doubleValue();
        } catch (Throwable ignored) {}

        try {
            Object r = vec.getClass().getMethod(axis).invoke(vec);
            return ((Number) r).doubleValue();
        } catch (Throwable ignored) {}

        try {
            Object r = vec.getClass().getField(axis).get(vec);
            return ((Number) r).doubleValue();
        } catch (Throwable ignoredPublic) {
            var f = vec.getClass().getDeclaredField(axis);
            f.setAccessible(true);
            Object r = f.get(vec);
            return ((Number) r).doubleValue();
        }
    }

    private static Object newVector3d(double x, double y, double z) {
        String[] candidates = {
                "com.hypixel.hytale.math.vector.Vector3d",
                "com.hypixel.hytale.math.Vector3d",
                "com.hypixel.hytale.util.math.Vector3d",
                "com.hypixel.hytale.protocol.util.Vector3d",
                "com.hypixel.hytale.server.core.math.Vector3d"
        };
        for (String cn : candidates) {
            try {
                Class<?> c = Class.forName(cn);
                return c.getConstructor(double.class, double.class, double.class).newInstance(x, y, z);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object newVector3f(float a, float b, float c0) {
        String[] candidates = {
                "com.hypixel.hytale.math.vector.Vector3f",
                "com.hypixel.hytale.math.Vector3f",
                "com.hypixel.hytale.util.math.Vector3f",
                "com.hypixel.hytale.protocol.util.Vector3f",
                "com.hypixel.hytale.server.core.math.Vector3f"
        };
        for (String cn : candidates) {
            try {
                Class<?> c = Class.forName(cn);
                return c.getConstructor(float.class, float.class, float.class).newInstance(a, b, c0);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // =========================================================
    // Generic reflection helpers
    // =========================================================

    private static Object invokeNoArg(Object target, String... methodNames) {
        if (target == null) return null;
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object invokeOneArg(Object target, String methodName, Class<?> argType, Object arg) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName, argType);
            return m.invoke(target, arg);
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object invokeStaticNoArg(String className, String methodName) {
        try {
            Class<?> c = Class.forName(className);
            Method m = c.getMethod(methodName);
            return m.invoke(null);
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getStaticFieldIfExists(String className, String fieldName) {
        try {
            Class<?> c = Class.forName(className);
            Field f = c.getField(fieldName);
            return f.get(null);
        } catch (Throwable ignored) {}
        return null;
    }

    private static String firstStringFromArray(Object arr) {
        if (arr instanceof String[] a && a.length > 0) return a[0];
        return null;
    }


    /**
     * Invoca um método com 2 argumentos por reflexão.
     * Retorna true se encontrou e conseguiu invocar (sem exceção).
     */
    private static boolean invokeTwoArgs(Object target, String methodName,
                                         Class<?> argType1, Class<?> argType2,
                                         Object arg1, Object arg2) {
        if (target == null) return false;
        try {
            try {
                java.lang.reflect.Method m = target.getClass().getMethod(methodName, argType1, argType2);
                m.invoke(target, arg1, arg2);
                return true;
            } catch (NoSuchMethodException ignored) {
                for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                    if (!m.getName().equals(methodName)) continue;
                    if (m.getParameterCount() != 2) continue;
                    Class<?>[] p = m.getParameterTypes();
                    boolean ok1 = arg1 == null || p[0].isAssignableFrom(arg1.getClass()) || p[0].isAssignableFrom(argType1);
                    boolean ok2 = arg2 == null || p[1].isAssignableFrom(arg2.getClass()) || p[1].isAssignableFrom(argType2);
                    if (!ok1 || !ok2) continue;
                    m.invoke(target, arg1, arg2);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
