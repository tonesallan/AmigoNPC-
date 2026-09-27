package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.ui.lvlgui.AmigoLvlGuiService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.CompletableFuture;

/**
 * AmigoNPC CurseForge 2.0.1 command reconstructed from the exact baseline JAR.
 *
 * Compatibility delta only:
 * - public command declaration for current Hytale
 * - CommandContext player sender is now PlayerRef instead of Player
 */
public final class AmigoCommand extends AbstractCommand {

    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoCommand(AmigoService service) {
        super("amigo", "Comandos do AmigoNPC");

        // Current Hytale replacement for legacy canGeneratePermission() == false.
        // Must happen before registration/subcommand completion.
        this.requireNoPermission();

        this.setAllowsExtraArguments(true);
        this.addSubCommand(new AmigoSpawnSubCommand(service));
        this.addSubCommand(new AmigoDespawnSubCommand(service));
        this.addSubCommand(new AmigoModeloSubCommand());
        this.addSubCommand(new AmigoModeloOffSubCommand());
        this.addSubCommand(new AmigoDefenderSubCommand());
        this.addSubCommand(new AmigoLogSubCommand());
        this.addSubCommand(new AmigoGodmodSubCommand());
        this.addSubCommand(new AmigoHudSubCommand());
        this.addSubCommand(new AmigoStatsSubCommand());
        this.addSubCommand(new AmigoRewardsSubCommand());
        this.addSubCommand(new AmigoAddNpcXpSubCommand());
        this.addSubCommand(new AmigoPlayerStatsSubCommand());
        this.addSubCommand(new AmigoMobScalingSubCommand());
        this.addSubCommand(new AmigoReloadSubCommand());
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        try {
            String in = ctx.getInputString();
            if (in != null) {
                in = in.trim();

                if (in.startsWith("/")) {
                    in = in.substring(1);
                }

                if (!in.isBlank()) {
                    String[] parts = in.split("\\s+");

                    if (parts.length > 1 && "amigo".equalsIgnoreCase(parts[0])) {
                        String sub = parts[1].toLowerCase();

                        if (!"ui".equals(sub)
                                && !"ui2".equals(sub)
                                && !"lvl".equals(sub)
                                && !"lvlup".equals(sub)
                                && !"lvldown".equals(sub)) {
                            ctx.sendMessage(Message.raw(
                                    "§c[AmigoNPC] Subcomando desconhecido. Use apenas §f/amigo§c ou um subcomando válido."
                            ));
                            return CompletableFuture.completedFuture(null);
                        }

                        return CompletableFuture.completedFuture(null);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        /*
         * Original 2.0.1:
         *   CommandSender sender = ctx.sender();
         *   if (!(sender instanceof Player)) -> "Este comando é apenas para players."
         *
         * Current Hytale:
         *   player commands are sent by PlayerRef.
         */
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();

        if (ref == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando é apenas para players."));
            return CompletableFuture.completedFuture(null);
        }

        if (!ref.isValid()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados do player indisponíveis (Ref inválida)."));
            return CompletableFuture.completedFuture(null);
        }

        try {
            ActionTraceService.getShared().record(
                    ctx.sender().getUuid(),
                    "command",
                    "/amigo"
            );
        } catch (Throwable ignored) {
        }

        Store<EntityStore> store = ref.getStore();
        Player player;

        try {
            player = store.getComponent(ref, Player.getComponentType());
        } catch (Throwable ignored) {
            player = null;
        }

        if (player == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados do player indisponíveis (Ref inválida)."));
            return CompletableFuture.completedFuture(null);
        }

        World world = null;

        try {
            Object externalData = store.getExternalData();
            if (externalData instanceof EntityStore entityStore) {
                world = entityStore.getWorld();
            }
        } catch (Throwable ignored) {
        }

        if (world == null) {
            try {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

                if (playerRef != null) {
                    AmigoLvlGuiService.getShared().toggle(
                            player,
                            ref,
                            store,
                            playerRef
                    );
                }
            } catch (Throwable ignored) {
            }

            return CompletableFuture.completedFuture(null);
        }

        World executor = world;
        Player finalPlayer = player;

        return CompletableFuture.runAsync(() -> {
            try {
                PlayerRef playerRef = store.getComponent(
                        ref,
                        PlayerRef.getComponentType()
                );

                if (playerRef != null) {
                    AmigoLvlGuiService.getShared().toggle(
                            finalPlayer,
                            ref,
                            store,
                            playerRef
                    );
                }
            } catch (Throwable ignored) {
            }
        }, executor);
    }
}
