package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;

/**
 * /amigopvp on|off
 *
 * Apenas admins (OP) podem executar.
 */
public final class AmigoPvpCommand extends AbstractCommand {

    public AmigoPvpCommand() {
        super("amigopvp", "Ativa/Desativa PvP entre NPCs do AmigoNPC (admin)");
        this.setAllowsExtraArguments(true);
        this.addSubCommand(new On());
        this.addSubCommand(new Off());
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigopvp on §7| §f/amigopvp off"));
        return CompletableFuture.completedFuture(null);
    }

    private static boolean isAdmin(CommandContext ctx) {
        try {
            var sender = ctx.sender();
            if (sender == null) return false;

            // Nó gerado pelo comando built-in /op (normalmente só admins têm)
            String opNode = HytalePermissions.fromCommand("op");

            return sender.hasPermission(opNode, false)
                    || sender.hasPermission("hytale.command.op", false)
                    || sender.hasPermission("hytale.*", false)
                    || sender.hasPermission("*", false);
        } catch (Throwable t) {
            return false;
        }
    }

    private static final class On extends AbstractCommand {
        On() {
            super("on", "Ativar PvP entre NPCs (admin)");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (!isAdmin(ctx)) {
                ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas admin do servidor pode usar este comando."));
                return CompletableFuture.completedFuture(null);
            }

            AmigoNpcManager.getShared().setPvpEnabled(true);
            ctx.sendMessage(Message.raw("§a[AmigoNPC] PvP entre NPCs: §fON"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class Off extends AbstractCommand {
        Off() {
            super("off", "Desativar PvP entre NPCs (admin)");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (!isAdmin(ctx)) {
                ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas admin do servidor pode usar este comando."));
                return CompletableFuture.completedFuture(null);
            }

            AmigoNpcManager.getShared().setPvpEnabled(false);
            ctx.sendMessage(Message.raw("§a[AmigoNPC] PvP entre NPCs: §fOFF"));
            return CompletableFuture.completedFuture(null);
        }
    }
}
