package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

/**
 * /amigolvl up
 * /amigolvl down
 *
 * Apenas admins (mesma regra do /amigopvp).
 */
public final class AmigoLvlCommand extends AbstractCommand {

    public AmigoLvlCommand() {
        super("amigolvl", "Ajusta o nível de espada do Amigo (admin)");
        this.setAllowsExtraArguments(true);
        this.addSubCommand(new AmigoLvlUpSubCommand());
        this.addSubCommand(new AmigoLvlDownSubCommand());
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigolvl up §7| §f/amigolvl down"));
        return CompletableFuture.completedFuture(null);
    }

    static AmigoNpcManager manager() {
        return AmigoNpcManager.getShared();
    }
}
