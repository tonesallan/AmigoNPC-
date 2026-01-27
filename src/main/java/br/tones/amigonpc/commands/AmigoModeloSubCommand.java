package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /amigo modelo <modelId> [scale]
 *
 * Define um ModelAsset ID para o AmigoNPC usar no próximo spawn.
 * Exemplo (igual ao mod de referência):
 *  - /amigo modelo <id_do_modelo> 1.0
 */
public final class AmigoModeloSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final RequiredArg modelArg;
    private final OptionalArg scaleArg;

    public AmigoModeloSubCommand() {
        super("modelo", "Define o modelo (aparência) do AmigoNPC");

        this.modelArg = this.withRequiredArg(
                "modelId",
                "Model asset id (ex.: npc.male_head_01)",
                ArgTypes.STRING
        );

        this.scaleArg = this.withOptionalArg(
                "scale",
                "Escala do modelo (padrão: 1.0)",
                ArgTypes.DOUBLE
        );
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        String modelId = (String) modelArg.get(ctx);
        double scale = 1.0;
        if (scaleArg.provided(ctx)) {
            Object v = scaleArg.get(ctx);
            if (v instanceof Double d) scale = d;
        }

        AmigoPersistence.saveModel(playerRef.getUuid(), modelId, scale);

        ctx.sendMessage(Message.raw("§a[AmigoNPC] Modelo salvo: §f" + modelId + "§a (scale=" + scale + ")"));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Para aplicar: use §f/amigo despawn§7 e depois §f/amigo spawn§7."));
    }
}
