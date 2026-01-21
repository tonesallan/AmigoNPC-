package br.tones.amigonpc.core;

import java.util.UUID;

/**
 * Responsável EXCLUSIVAMENTE pelo spawn de NPC no mundo.
 *
 * Regras:
 * - Executar sempre dentro de world.execute(...)
 * - Criar entidade via EntityStore (via bridge/reflexão)
 * - Não conter lógica de comando ou permissão
 */
public final class WorldNpcSpawner {

    private WorldNpcSpawner() {
    }

    /**
     * Spawna um NPC básico no mundo do jogador via bridge/reflexão.
     *
     * @param worldObj mundo do jogador (não tipado para evitar imports frágeis)
     * @param ownerId  UUID do jogador dono do NPC
     * @return true se o spawn foi disparado; false se falhou
     */
    public static boolean spawn(Object worldObj, UUID ownerId) {
        return HytaleBridge.spawnBasicNpc(worldObj, ownerId);
    }
}
