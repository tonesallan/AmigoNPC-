package br.tones.amigonpc.core;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoService {

    private final AmigoNpcManager manager = AmigoNpcManager.getShared();

    public AmigoService() {}

    /**
     * Caminho preferido (API atual): o comando já entrega World/Store/PlayerRef.
     * Esse caminho é síncrono (não depende de world.execute), evitando casos onde o NPC spawna
     * mas o registro interno some e o despawn falha.
     */
    public void spawn(CommandContext ctx, World world, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
        if (ctx == null || world == null || store == null || playerEntityRef == null || playerRef == null) {
            if (ctx != null) ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para spawn."));
            return;
        }

        UUID ownerId = playerRef.getUuid();

        // ✅ usa o Store direto (mais confiável)
        boolean ok = manager.spawnWithStore(world, store, playerEntityRef, ownerId, playerRef);
        if (!ok) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao spawnar o NPC."));
            String err = manager.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }

        ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC criado! Use §f/amigo despawn §apara remover."));
    }

    /**
     * Caminho preferido (API atual): o comando já entrega World/Store/PlayerRef.
     */
    public void despawn(CommandContext ctx, World world, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
        if (ctx == null || world == null || store == null || playerEntityRef == null || playerRef == null) {
            if (ctx != null) ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para despawn."));
            return;
        }

        UUID ownerId = playerRef.getUuid();

        boolean ok = manager.despawnWithStore(store, ownerId);
        if (!ok) {
            ctx.sendMessage(Message.raw("§e[AmigoNPC] Não foi possível remover o NPC agora."));
            String err = manager.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }

        ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC removido!"));
    }

    public void spawn(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
            return;
        }

        UUID ownerId = ctx.sender().getUuid();

        // ⚠️ fallback antigo (builds antigas): tenta descobrir o World via reflection.
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

        // ⚠️ fallback antigo (builds antigas): tenta descobrir o World via reflection.
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

    // Compat: builds/rotas antigas que ainda chamam spawn/d... só com world/playerRef
    public void spawn(CommandContext ctx, World world, PlayerRef playerRef) {
        if (ctx == null || world == null || playerRef == null) {
            if (ctx != null) ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para spawn."));
            return;
        }
        UUID ownerId = playerRef.getUuid();
        boolean ok = manager.spawn(world, ownerId, playerRef);
        if (!ok) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao spawnar o NPC."));
            String err = manager.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return;
        }
        ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC criado! Use §f/amigo despawn §apara remover."));
    }

    public void despawn(CommandContext ctx, World world, PlayerRef playerRef) {
        if (ctx == null || world == null || playerRef == null) {
            if (ctx != null) ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para despawn."));
            return;
        }
        UUID ownerId = playerRef.getUuid();
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
