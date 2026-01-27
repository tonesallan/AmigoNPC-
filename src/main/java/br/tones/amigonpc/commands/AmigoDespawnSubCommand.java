package br.tones.amigonpc.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import br.tones.amigonpc.core.AmigoService;

public final class AmigoDespawnSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final AmigoService service;

    public AmigoDespawnSubCommand(AmigoService service) {
        super("despawn", "Remove o NPC do jogador");
        this.service = service;

        // despawn não precisa aceitar args
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {
        // Usa o Store entregue pela API para remover imediatamente e manter o registro consistente.
        service.despawn(ctx, world, store, playerEntityRef, playerRef);
    }
}
