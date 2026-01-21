package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.core.AmigoService;

public final class AmigoDespawnSubCommand extends AbstractCommand {

    private final AmigoService service;

    public AmigoDespawnSubCommand(AmigoService service) {
        super("despawn", "Remove o NPC do jogador");
        this.service = service;

        // despawn não precisa aceitar args
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        service.despawn(ctx);
        return CompletableFuture.completedFuture(null);
    }
}
