package br.tones.amigonpc.commands;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import br.tones.amigonpc.core.AmigoNpcManager;

/**
 * /loot
 * Abre a mochila (45 slots) do AmigoNPC.
 *
 * IMPORTANTE:
 * Nesta build do Hytale, abrir container como baú/backpack funciona via
 * PageManager.setPageWithWindows(...), que é o mesmo fluxo usado por baús
 * (OpenContainerInteraction) e comandos internos como /inventory item.
 */
public final class LootCommand extends AbstractCommand {

    // ✅ comando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    public LootCommand() {
        super("loot", "Abre a mochila do seu AmigoNPC");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
            return CompletableFuture.completedFuture(null);
        }

        UUID owner = ctx.sender().getUuid();
        AmigoNpcManager manager = AmigoNpcManager.getShared();

        if (!manager.hasNpc(owner)) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Você ainda não tem um Amigo ativo. Use §f/amigo spawn§c."));
            return CompletableFuture.completedFuture(null);
        }

        final SimpleItemContainer bag = manager.getOrLoadBackpack(owner);

        Player sender = ctx.senderAs(Player.class);
        if (sender == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui obter o Player nesta build."));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef playerRef = sender.getPlayerRef();
        if (playerRef == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] PlayerRef indisponível. Tente relogar."));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Você não está em um mundo agora. Tente relogar."));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = ((EntityStore) store.getExternalData()).getWorld();

        world.execute(() -> {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui acessar seu componente de Player no mundo."));
                return;
            }

            ContainerWindow window = new ContainerWindow(bag);

            boolean ok = playerComponent.getPageManager().setPageWithWindows(
                    ref,
                    store,
                    Page.Bench,
                    true,
                    new Window[] { (Window) window }
            );

            if (!ok) {
                playerComponent.sendMessage(Message.raw("§c[AmigoNPC] Não foi possível abrir a mochila agora."));
            }
        });

        return CompletableFuture.completedFuture(null);
    }
}
