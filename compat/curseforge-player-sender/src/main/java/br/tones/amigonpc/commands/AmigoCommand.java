package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.i18n.AmigoText;
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
 * CurseForge 2.0.1 AmigoCommand with only the current-Hytale sender bridge fixed.
 * Gameplay/UI routing is intentionally preserved.
 */
public final class AmigoCommand extends AbstractCommand {
    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoCommand(AmigoService service) {
        super("amigo", AmigoText.text("cmd.desc.amigo"));

        // Current Hytale replacement for the legacy canGeneratePermission=false behavior.
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
                            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.unknown_subcommand")));
                            return CompletableFuture.completedFuture(null);
                        }

                        return CompletableFuture.completedFuture(null);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Old 2.0.1 checked: ctx.sender() instanceof Player.
        // Current Hytale exposes the command sender as PlayerRef.
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.only_players")));
            return CompletableFuture.completedFuture(null);
        }

        if (!ref.isValid()) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.invalid_player_ref")));
            return CompletableFuture.completedFuture(null);
        }

        try {
            ActionTraceService.getShared().record(ctx.sender().getUuid(), "command", "/amigo");
        } catch (Throwable ignored) {
        }

        Store<EntityStore> store = ref.getStore();
        World world;

        try {
            world = store.getExternalData().getWorld();
        } catch (Throwable ignored) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.invalid_player_ref")));
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                Player player = store.getComponent(ref, Player.getComponentType());

                if (playerRef == null || player == null) {
                    ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.invalid_player_ref")));
                    return;
                }

                AmigoLvlGuiService.getShared().toggle(player, ref, store, playerRef);
            } catch (Throwable ignored) {
                ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.invalid_player_ref")));
            }
        }, world);
    }
}
