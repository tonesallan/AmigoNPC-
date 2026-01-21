package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.ui.UiBridge;

public final class AmigoUiCommand extends AbstractCommand {

    public AmigoUiCommand() {
        super("amigoui", "Abre a interface do AmigoNPC");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
            return CompletableFuture.completedFuture(null);
        }

        Object player = ctx.sender();

        boolean ok = UiBridge.openAmigoMainUi(player);
        if (!ok) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui abrir a UI nesta build."));
            String err = UiBridge.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return CompletableFuture.completedFuture(null);
        }

        ctx.sendMessage(Message.raw("§a[AmigoNPC] UI aberta."));
        return CompletableFuture.completedFuture(null);
    }
}
