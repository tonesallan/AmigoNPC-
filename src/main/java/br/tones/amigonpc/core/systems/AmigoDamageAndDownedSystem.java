package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.core.AmigoNpcManager;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Vida real / DOWNED para o AmigoNPC, implementado na API correta (Damage = EcsEvent).
 *
 * Observação: este sistema roda no filterDamageGroup e cancela o evento depois de processar,
 * evitando que o pipeline padrão tente aplicar regras adicionais que podem exigir componentes
 * que nosso NPC ainda não tem.
 */
public final class AmigoDamageAndDownedSystem extends DamageEventSystem {

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Simples e compatível: o evento de dano já traz o alvo, não precisamos filtrar por componentes aqui.
        return Query.any();
    }

    @Override
    public void handle(int entityIndex,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer,
                       Damage damage) {

        if (damage == null || damage.isCancelled()) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(entityIndex);
        if (targetRef == null) return;

        AmigoNpcManager manager = AmigoNpcManager.getShared();

        // ===== Assistencia (player deu dano) =====
        try {
            Damage.Source src = damage.getSource();
            if (src instanceof Damage.EntitySource es && es.getRef() != null) {
                Ref<EntityStore> srcRef = es.getRef();
                Player srcPlayer = store.getComponent(srcRef, Player.getComponentType());
                if (srcPlayer != null) {
                    UUIDComponent uuidComp = store.getComponent(srcRef, UUIDComponent.getComponentType());
                    java.util.UUID ownerId = uuidComp == null ? null : uuidComp.getUuid();
                    if (ownerId != null && manager.hasNpc(ownerId)) {
                        manager.startAssist(ownerId, targetRef);
                    manager.recordCombatTag(ownerId, targetRef, store);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // ===== Combate (etapa inicial) =====
        // Se o dano atingiu um Player que tem AmigoNPC e a fonte é uma entidade,
        // pedimos para o NPC priorizar o agressor por alguns segundos.
        try {
            Player maybePlayer = store.getComponent(targetRef, Player.getComponentType());
            if (maybePlayer != null) {
                UUIDComponent uuidComp = store.getComponent(targetRef, UUIDComponent.getComponentType());
                java.util.UUID ownerId = uuidComp == null ? null : uuidComp.getUuid();

                if (ownerId != null && manager.hasNpc(ownerId)) {
                    Damage.Source src = damage.getSource();
                    if (src instanceof Damage.EntitySource es && es.getRef() != null) {
                        manager.startCombat(ownerId, es.getRef());
                    }
                }

                // Não interferimos no dano do player.
                return;
            }
        } catch (Throwable ignored) {}

        if (!manager.isAmigoRef(targetRef)) return;

        // Se já está DOWNED: ignora danos adicionais
        java.util.UUID owner = manager.getOwnerFromRef(targetRef);
        if (owner != null && manager.isDowned(owner)) {
            damage.setCancelled(true);
            return;
        }

        // === HP (EntityStatMap / Health) ===
        EntityStatMap stats = buffer.ensureAndGetComponent(targetRef, EntityStatMap.getComponentType());
        int healthIdx = DefaultEntityStatTypes.getHealth();

        float currentHp;
        try {
            currentHp = stats.get(healthIdx).get();
        } catch (Throwable t) {
            // Se ainda não inicializou, assume cheio
            stats.maximizeStatValue(healthIdx);
            currentHp = stats.get(healthIdx).get();
        }

        float incoming = damage.getAmount();
        if (owner != null) {
            // Aplica DEF scaling (cap lvl 100): mais level => menos dano recebido
            incoming = manager.mitigateIncomingDamage(owner, incoming);
        }

        float newHp = currentHp - incoming;

        // Componentes de animação/movimento
        ActiveAnimationComponent anim = buffer.ensureAndGetComponent(targetRef, ActiveAnimationComponent.getComponentType());
        MovementStatesComponent move = buffer.ensureAndGetComponent(targetRef, MovementStatesComponent.getComponentType());
        MovementStates states = move.getMovementStates();
        if (states == null) {
            states = new MovementStates();
            states.idle = true;
            states.onGround = true;
            move.setMovementStates(states);
            buffer.replaceComponent(targetRef, MovementStatesComponent.getComponentType(), move);
        }

        if (newHp <= 0f) {
            // DOWNED
            // IMPORTANTE:
            // Algumas builds despawnam/limpam automaticamente entidades com HP=0.
            // Para manter o "corpo" visível no chão, mantemos HP mínimo (>0)
            // e tocamos a animação de morte manualmente.
            stats.setStatValue(healthIdx, 1f);
            buffer.replaceComponent(targetRef, EntityStatMap.getComponentType(), stats);

            // Animação de morte (o modelo define quais ids existem)
            try {
                String[] deathIds = Entity.DefaultAnimations.getDeathAnimationIds(states, damage.getCause());
                if (deathIds != null && deathIds.length > 0 && deathIds[0] != null) {
                    anim.setPlayingAnimation(AnimationSlot.Action, deathIds[0]);
                    buffer.replaceComponent(targetRef, ActiveAnimationComponent.getComponentType(), anim);
                }
            } catch (Throwable ignored) {}

            if (owner != null) manager.markDowned(owner);

            // Cancelamos para impedir que o pipeline padrão finalize o kill/corpse
            damage.setCancelled(true);
            return;
        }

        // Dano normal
        stats.setStatValue(healthIdx, newHp);
        buffer.replaceComponent(targetRef, EntityStatMap.getComponentType(), stats);

        // Animação de hit
        try {
            String[] hurtIds = Entity.DefaultAnimations.getHurtAnimationIds(states, damage.getCause());
            if (hurtIds != null && hurtIds.length > 0 && hurtIds[0] != null) {
                anim.setPlayingAnimation(AnimationSlot.Action, hurtIds[0]);
                buffer.replaceComponent(targetRef, ActiveAnimationComponent.getComponentType(), anim);
            }
        } catch (Throwable ignored) {}

        // Cancelamos para evitar regras extras do pipeline padrão (crash em entidades incompletas)
        damage.setCancelled(true);
    }
}
