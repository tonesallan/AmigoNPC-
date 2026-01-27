package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

/**
 * /autoloot (toggle)
 * /autoloot on
 * /autoloot off
 */
public final class AutoLootCommand extends AbstractCommand {

    public AutoLootCommand() {
        super("autoloot", "Ativa/desativa o auto-loot do AmigoNPC.");
        this.setAllowsExtraArguments(true);
        this.addSubCommand(new On());
        this.addSubCommand(new Off());
    }

    @Override
    public boolean canGeneratePermission() {
        // Comando público
        return false;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por players."));
            return CompletableFuture.completedFuture(null);
        }

        UUID ownerId = ctx.sender().getUuid();
        boolean enabled = AmigoNpcManager.getShared().toggleAutoLoot(ownerId);
        ctx.sendMessage(Message.raw("§7[AmigoNPC] AutoLoot: §f" + (enabled ? "ON" : "OFF")));
        return CompletableFuture.completedFuture(null);
    }

    private static final class On extends AbstractCommand {
        On() {
            super("on", "Ativar auto-loot");
        }

        @Override
        public boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por players."));
                return CompletableFuture.completedFuture(null);
            }

            UUID ownerId = ctx.sender().getUuid();
            AmigoNpcManager.getShared().setAutoLootEnabled(ownerId, true);
            ctx.sendMessage(Message.raw("§7[AmigoNPC] AutoLoot: §fON"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class Off extends AbstractCommand {
        Off() {
            super("off", "Desativar auto-loot");
        }

        @Override
        public boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por players."));
                return CompletableFuture.completedFuture(null);
            }

            UUID ownerId = ctx.sender().getUuid();
            AmigoNpcManager.getShared().setAutoLootEnabled(ownerId, false);
            ctx.sendMessage(Message.raw("§7[AmigoNPC] AutoLoot: §fOFF"));
            return CompletableFuture.completedFuture(null);
        }
    }
}
