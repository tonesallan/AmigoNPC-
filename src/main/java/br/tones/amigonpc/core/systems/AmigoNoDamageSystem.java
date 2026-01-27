package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Filtro de dano: evita crash/desconexão caso o NPC ainda não tenha componentes de combate/vida.
 *
 * Por enquanto, todo dano em entidades que são "AmigoNPC" é cancelado.
 */
public final class AmigoNoDamageSystem extends DamageEventSystem {

    @Nonnull
    @Override
    public Query getQuery() {
        // Observa qualquer entidade que possa receber evento de dano
        return Query.any();
    }

    @Nullable
    @Override
    public SystemGroup getGroup() {
        // Roda antes de aplicar o dano
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> buffer,
                       @Nonnull Damage damage) {

        Object targetRef = chunk.getReferenceTo(index);
        if (AmigoNpcManager.getShared().isAmigoRef(targetRef)) {
            damage.setCancelled(true);
        }
    }
}
