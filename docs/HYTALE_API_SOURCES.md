# Hytale API — fontes de referência do AmigoNPC

Este arquivo registra as fontes que devem ser consultadas durante a reconstrução do AmigoNPC.

## Prioridade das fontes

1. Documentação oficial atual da Hytale Server API
   - https://docs.hytale.com/
   - Java API: https://docs.hytale.com/api/
2. HytaleServer.jar da mesma build usada no ambiente de teste
   - Usar para confirmar assinaturas e comportamento quando a documentação não for suficiente.
3. Materiais locais do projeto
   - Hytale API Index / FullBundle / dossiês já mantidos fora do repositório.
4. Exemplos públicos e documentação comunitária
   - Usar como referência prática, nunca como autoridade superior à API oficial/JAR.

## APIs prioritárias para o AmigoNPC

### Plugins / hot reload
- PluginManager
  - https://docs.hytale.com/api/com/hypixel/hytale/server/core/plugin/PluginManager
  - Métodos relevantes: load, unload, reload.

### NPC
- NPCPlugin
  - https://docs.hytale.com/api/com/hypixel/hytale/server/npc/NPCPlugin
  - Métodos relevantes: spawnNPC, spawnEntity, reloadNPCsWithRole.
- NPCEntity e Role/Builder/Blackboard
  - Consultar pelo índice oficial quando o módulo correspondente for iniciado.

### Busca de entidades / targeting
- TargetUtil
  - Consultar na API oficial antes de implementar scans manuais de chunks.
- AttitudeMap / Attitude
  - Usar para distinguir relações hostis, neutras e amigáveis quando aplicável.

### ECS / entidades
- Ref, Store, EntityStore
- TransformComponent
- EntityStatMap
- DeathComponent
- Damage / DamageSystems
- EventRegistry

### Interações / revive
- UseEntityEvent.Pre / UseEntityEvent.Post
- Interactions
- InteractionContext
- InteractionType

### Inventário
- SimpleItemContainer
- ItemContainer
- ItemStack
- ItemStackTransaction

### UI
- CustomUIPage
- InteractiveCustomUIPage
- UICommandBuilder
- UIEventBuilder
- PageManager

## Regra para reflection

O core do AmigoNPC deve usar a API tipada da build alvo sempre que ela estiver disponível.

Reflection deve ficar restrita a integrações opcionais ou compatibilidade explicitamente documentada. Não usar reflection como padrão para APIs conhecidas.

## Regra de atualização

Antes de alterar um módulo que dependa da API do Hytale:

1. consultar a documentação oficial atual;
2. confirmar a assinatura no HytaleServer.jar da build de teste quando necessário;
3. registrar no commit qualquer mudança relevante de API;
4. evitar copiar APIs internas/deprecated quando existir alternativa pública atual.
