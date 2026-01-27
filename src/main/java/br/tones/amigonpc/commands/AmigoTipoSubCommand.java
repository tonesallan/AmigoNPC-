package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /amigo tipo <npcType>
 *
 * Define o npcType (role template) usado pelo spawnNPC.
 * Dica: use /amigo tipos para listar os nomes disponíveis no seu servidor.
 */
public final class AmigoTipoSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final RequiredArg typeArg;

    public AmigoTipoSubCommand() {
        super("tipo", "Define o tipo (npcType) do AmigoNPC");

        this.typeArg = this.withRequiredArg(
                "npcType",
                "Nome do role template (use /amigo tipos)",
                ArgTypes.STRING
        );
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        String npcType = (String) typeArg.get(ctx);
        if (npcType == null || npcType.isBlank()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] npcType inválido."));
            return;
        }

        // Salva preferência por player (1 arquivo por UUID)
        AmigoPersistence.saveNpcType(playerRef.getUuid(), npcType);

        ctx.sendMessage(Message.raw("§a[AmigoNPC] Tipo salvo: §f" + npcType));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Para aplicar: use §f/amigo despawn§7 e depois §f/amigo spawn§7."));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Obs: se você estiver usando §f/amigo modelo§7, o modelo custom tem prioridade sobre o tipo."));
    }
}
