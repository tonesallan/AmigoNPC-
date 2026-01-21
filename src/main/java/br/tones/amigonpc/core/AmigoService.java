package br.tones.amigonpc.core;

import java.util.UUID;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

public final class AmigoService {

    private final AmigoNpcManager manager = AmigoNpcManager.getShared();

    public AmigoService() {}

    public void spawn(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
            return;
        }

        UUID ownerId = ctx.sender().getUuid();

        Object world = HytaleBridge.tryGetWorldFromCommandContext(ctx);
        if (world == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui obter o World do jogador nesta build."));
            String err = HytaleBridge.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }

        // ✅ passa o sender para pegarmos posição do player
        boolean ok = manager.spawn(world, ownerId, ctx.sender());
        if (!ok) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao spawnar o NPC."));
            String err = manager.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }

        ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC criado! Use §f/amigo despawn §apara remover."));
    }

    public void despawn(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
            return;
        }

        UUID ownerId = ctx.sender().getUuid();

        Object world = HytaleBridge.tryGetWorldFromCommandContext(ctx);
        if (world == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui obter o World do jogador nesta build."));
            String err = HytaleBridge.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }

        boolean ok = manager.despawn(world, ownerId);
        if (!ok) {
            ctx.sendMessage(Message.raw("§e[AmigoNPC] Não foi possível remover o NPC agora."));
            String err = manager.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }

        ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC removido!"));
    }
}
