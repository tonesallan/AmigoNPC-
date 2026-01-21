package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.core.AmigoService;

public final class AmigoDespawnCommand extends AbstractCommand {

    private final AmigoService service;

    public AmigoDespawnCommand(AmigoService service) {
        super("amigodespawn", "Remove o NPC do AmigoNPC");
        this.service = service;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        service.despawn(ctx);
        return CompletableFuture.completedFuture(null);
    }
}
