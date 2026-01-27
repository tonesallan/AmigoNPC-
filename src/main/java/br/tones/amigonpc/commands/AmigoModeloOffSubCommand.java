package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /amigo modelooff
 *
 * Desativa o modelo custom e volta ao spawn normal (spawnNPC).
 */
public final class AmigoModeloOffSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoModeloOffSubCommand() {
        super("modelooff", "Remove o modelo custom do AmigoNPC");
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        AmigoPersistence.saveModel(playerRef.getUuid(), null, 1.0);
        ctx.sendMessage(Message.raw("§a[AmigoNPC] Modelo custom removido."));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Para aplicar: use §f/amigo despawn§7 e depois §f/amigo spawn§7."));
    }
}
