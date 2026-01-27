package br.tones.amigonpc;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import br.tones.amigonpc.commands.AmigoCommand;
import br.tones.amigonpc.commands.AmigoDebugCommand;
import br.tones.amigonpc.commands.AmigoPvpCommand;
import br.tones.amigonpc.commands.AmigoLvlCommand;
import br.tones.amigonpc.commands.LootCommand;
import br.tones.amigonpc.commands.AutoLootCommand;
import br.tones.amigonpc.commands.CommandAccess;
import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.progress.XpProgression;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Plugin principal do AmigoNPC.
 *
 * ✅ Correção: remover NPC no logout usando getEventRegistry().register(...)
 * ✅ Failsafe: no login, tenta despawnStored(uuid) (se algum dia sobrar registro)
 *
 * (por reflexão para suportar variações de build).
 */
public final class AmigoNPCPlugin extends JavaPlugin {

    public static final PluginManifest MANIFEST = PluginManifest
            .corePlugin(AmigoNPCPlugin.class)
            .build();

    private AmigoService service;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> downedTicker;
    private ScheduledFuture<?> followTicker;

    public AmigoNPCPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.service = new AmigoService();

        // ✅ Pré-cálculo 1x das tabelas de XP/level (não altera nada do jogo por si só)
        try {
            XpProgression.init();
        } catch (Throwable ignored) {}

        // Comando /amigo (com subcomandos spawn/despawn)
        var amigoCmd = new AmigoCommand(service);
        // ✅ tornar comandos públicos para qualquer player (sem permissão), exceto os admin-only
        CommandAccess.makePublicRecursive(amigoCmd);
        this.getCommandRegistry().registerCommand(amigoCmd);

        // UI (comandos removidos)


        // Mochila do NPC (45 slots)
        var lootCmd = new LootCommand();
        CommandAccess.makePublicRecursive(lootCmd);
        this.getCommandRegistry().registerCommand(lootCmd);

        // AutoLoot (toggle)
        var autolootCmd = new AutoLootCommand();
        CommandAccess.makePublicRecursive(autolootCmd);
        this.getCommandRegistry().registerCommand(autolootCmd);

        // Debug
        var dbgCmd = AmigoDebugCommand.createAsStandalone();
        CommandAccess.makePublicRecursive(dbgCmd);
        this.getCommandRegistry().registerCommand(dbgCmd);

        // Admin-only: /amigopvp on|off
        this.getCommandRegistry().registerCommand(new AmigoPvpCommand());

        // Admin-only: /amigolvl up|down (teste de progressão de espada)
        this.getCommandRegistry().registerCommand(new AmigoLvlCommand());

        // ✅ Cleanup no logout (evita NPC ficar no mundo e perder vínculo)
        hookLogoutCleanupViaEventRegistry();

        // ✅ Failsafe no login (cinto de segurança)
        hookLoginFailsafeViaEventRegistry();

        // ✅ Vida real (HP), dano, estado DOWNED e revive automático
        registerDamageAndDownedSystem();
        startDownedTicker();

        // ✅ Follow + teleporte seguro (20h / 8v) e respawn automático em teleport/troca de mundo
        startFollowTicker();
        hookTeleportAutoRespawnViaEventRegistry();
    }

    private void registerDamageAndDownedSystem() {
        try {
            this.getEntityStoreRegistry().registerSystem(new br.tones.amigonpc.core.systems.AmigoDamageAndDownedSystem());
        } catch (Throwable ignored) {
            // Se a build não suportar registrar sistemas aqui, não impede o mod de carregar.
        }
    }

    private void startDownedTicker() {
        try {
            if (this.scheduler != null) return;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "AmigoNPC-DownedTicker");
                t.setDaemon(true);
                return t;
            });
            this.downedTicker = this.scheduler.scheduleAtFixedRate(
                    () -> AmigoNpcManager.getShared().tickDowned(),
                    1, 1, TimeUnit.SECONDS
            );
        } catch (Throwable ignored) {
        }
    }

    private void startFollowTicker() {
        try {
            if (this.scheduler == null) {
                this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "AmigoNPC-Ticker");
                    t.setDaemon(true);
                    return t;
                });
            }
            if (this.followTicker != null) return;
            // Follow precisa de mais frequência para parecer "natural" (andar/animação).
            this.followTicker = this.scheduler.scheduleAtFixedRate(
                    () -> AmigoNpcManager.getShared().tickFollow(),
                    200, 200, TimeUnit.MILLISECONDS
            );
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void shutdown() {
        try {
            if (downedTicker != null) downedTicker.cancel(false);
        } catch (Throwable ignored) {}
        try {
            if (followTicker != null) followTicker.cancel(false);
        } catch (Throwable ignored) {}
        try {
            if (scheduler != null) scheduler.shutdownNow();
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Logout cleanup correto (EventRegistry)
    // =========================================================

    private void hookLogoutCleanupViaEventRegistry() {
        try {
            Object registry = this.getEventRegistry();
            if (registry == null) return;

            // Classes comuns; variam entre builds. Tentamos todas.
            String[] candidates = new String[] {
                    "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectedEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerLeaveEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerQuitEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerLogoutEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedOutEvent"
            };

            for (String cn : candidates) {
                tryRegister(registry, cn, true);
            }
        } catch (Throwable ignored) {
            // Sem crash se a build não suportar eventos como esperado
        }
    }

    // =========================================================
    // Failsafe no login (EventRegistry)
    // =========================================================

    private void hookLoginFailsafeViaEventRegistry() {
        try {
            Object registry = this.getEventRegistry();
            if (registry == null) return;

            // Nomes comuns; variam entre builds. Tentamos todos.
            String[] candidates = new String[] {
                    "com.hypixel.hytale.server.core.event.events.player.PlayerJoinEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerConnectedEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerLoginEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedInEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerSpawnedEvent"
            };

            for (String cn : candidates) {
                tryRegister(registry, cn, false);
            }
        } catch (Throwable ignored) {
            // Sem crash
        }
    }

    // =========================================================
    // Respawn automático em teleport/troca de mundo
    // =========================================================

    private void hookTeleportAutoRespawnViaEventRegistry() {
        try {
            Object registry = this.getEventRegistry();
            if (registry == null) return;

            String[] candidates = new String[] {
                    "com.hypixel.hytale.server.core.event.events.player.PlayerTeleportEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerTeleportedEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerChangedWorldEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerWorldChangeEvent",
                    "com.hypixel.hytale.server.core.event.events.player.PlayerDimensionChangeEvent"
            };

            for (String cn : candidates) {
                tryRegisterTeleport(registry, cn);
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void tryRegisterTeleport(Object registry, String eventClassName) {
        try {
            Class<?> evtClass = Class.forName(eventClassName);

            Method register = null;
            for (Method m : registry.getClass().getMethods()) {
                if (!m.getName().equals("register")) continue;
                if (m.getParameterCount() != 2) continue;
                if (m.getParameterTypes()[0] == Class.class) {
                    register = m;
                    break;
                }
            }
            if (register == null) return;

            Consumer handler = (evt) -> {
                UUID uuid = extractUuid(evt);
                if (uuid == null) return;

                Object playerRef = invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
                Object worldObj = extractWorld(evt, playerRef);
                if (worldObj == null) return;

                AmigoNpcManager.getShared().requestRespawn(worldObj, uuid, playerRef != null ? playerRef : evt);
            };

            register.invoke(registry, evtClass, handler);
        } catch (Throwable ignored) {}
    }

    private Object extractWorld(Object evt, Object playerRef) {
        // 1) tenta no evento
        Object w = invokeNoArg(evt, "getWorld", "getToWorld", "getDestinationWorld", "getNewWorld", "getUniverseWorld");
        if (w != null) return w;
        // 2) tenta no playerRef
        if (playerRef != null) {
            Object w2 = invokeNoArg(playerRef, "getWorld", "getUniverseWorld", "world");
            if (w2 != null) return w2;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void tryRegister(Object registry, String eventClassName, boolean isLogout) {
        try {
            Class<?> evtClass = Class.forName(eventClassName);

            // Procura EventRegistry.register(Class, Consumer)
            Method register = null;
            for (Method m : registry.getClass().getMethods()) {
                if (!m.getName().equals("register")) continue;
                if (m.getParameterCount() != 2) continue;
                if (m.getParameterTypes()[0] == Class.class) {
                    register = m;
                    break;
                }
            }
            if (register == null) return;

            Consumer handler = (evt) -> {
                UUID uuid = extractUuid(evt);
                if (uuid != null) {
                    // ✅ Logout: salva o estado (mochila) e remove o NPC do mundo.
                    // ✅ Login (failsafe): apenas tenta remover o que estiver registrado (se sobrou algo).
                    if (isLogout) {
                        try {
                            var bag = AmigoNpcManager.getShared().getOrLoadBackpack(uuid);
                            if (bag != null) {
                                br.tones.amigonpc.core.AmigoPersistence.saveBackpack(uuid, bag);
                            }
                        } catch (Throwable ignored) {
                        }
                    }

                    AmigoNpcManager.getShared().despawnStored(uuid);
                }
            };

            register.invoke(registry, evtClass, handler);

        } catch (Throwable ignored) {
            // Evento não existe nessa build -> ok
        }
    }

    private UUID extractUuid(Object evt) {
        if (evt == null) return null;

        // 1) tenta pegar PlayerRef do evento
        Object playerRef = invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
        if (playerRef != null) {
            Object u = invokeNoArg(playerRef, "getUuid", "uuid");
            UUID id = asUuid(u);
            if (id != null) return id;
        }

        // 2) tenta direto no evento
        Object u2 = invokeNoArg(evt, "getUuid", "getPlayerUuid", "getOwnerId", "getPlayerUUID");
        return asUuid(u2);
    }

    private UUID asUuid(Object u) {
        if (u instanceof UUID) return (UUID) u;
        if (u instanceof String s) {
            try { return UUID.fromString(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private Object invokeNoArg(Object obj, String... names) {
        if (obj == null) return null;
        for (String n : names) {
            try {
                Method m = obj.getClass().getMethod(n);
                return m.invoke(obj);
            } catch (Throwable ignored) {}
            try {
                var f = obj.getClass().getField(n);
                return f.get(obj);
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
