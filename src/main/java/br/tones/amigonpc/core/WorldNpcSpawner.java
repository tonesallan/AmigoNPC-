package br.tones.amigonpc.core;

import java.util.UUID;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore.AddReason;

// Componentes ECS mínimos
import com.hypixel.hytale.server.core.universe.world.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.entity.component.NetworkIdComponent;
import com.hypixel.hytale.server.core.universe.world.entity.component.UUIDComponent;

/**
 * Responsável EXCLUSIVAMENTE pelo spawn de NPC no mundo.
 *
 * Regras:
 * - Executar sempre dentro de world.execute(...)
 * - Criar entidade via EntityStore
 * - Não conter lógica de comando ou permissão
 */
public final class WorldNpcSpawner {

    private WorldNpcSpawner() {
    }

    /**
     * Spawna um NPC básico no mundo do jogador.
     *
     * @param world   mundo do jogador
     * @param ownerId UUID do jogador dono do NPC
     */
    public static void spawn(World world, UUID ownerId) {
        world.execute(() -> {

            EntityStore store = world.getEntityStore();

            Holder<EntityStore> holder =
                    EntityStore.REGISTRY.newHolder();

            // Componentes mínimos obrigatórios
            holder.add(new TransformComponent());
            holder.add(new UUIDComponent(ownerId));
            holder.add(new NetworkIdComponent(
                    store.getExternalData().takeNextNetworkId()
            ));

            // Spawn efetivo
            store.addEntity(holder, AddReason.SPAWN);
        });
    }
}
