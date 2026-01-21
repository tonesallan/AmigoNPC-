package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.core.AmigoService;

public final class AmigoSpawnSubCommand extends AbstractCommand {

    private final AmigoService service;

    public AmigoSpawnSubCommand(AmigoService service) {
        super("spawn", "Spawna o NPC do jogador");
        this.service = service;

        // spawn não precisa aceitar args
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        service.spawn(ctx);
        return CompletableFuture.completedFuture(null);
    }
}
