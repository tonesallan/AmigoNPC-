# AmigoNPC — minha versão (v2-own): escopo a partir da página do CurseForge

Fonte: https://www.curseforge.com/hytale/mods/amigonpc (AmigoNPC 2.0.1, Early Access, 9 mar 2026).

## Funcionalidades prometidas pela página

| # | Funcionalidade | Comando / ponto de entrada | Status nesta branch |
|---|---|---|---|
| 1 | Um companheiro por jogador | `/amigo spawn` | a implementar |
| 2 | Seguir o jogador | — (loop de IA) | a implementar |
| 3 | Combate de apoio | — | a implementar |
| 4 | Mochila de 45 slots | `/loot` | a implementar |
| 5 | Auto-loot | `/autoloot on` / `/autoloot off` | a implementar |
| 6 | Menu / UI | `/amigo` | a implementar |
| 7 | XP e nível | `/amigolvl up` / `/amigolvl down` (admin) | a implementar |
| 8 | Troca e escala de arma por nível | — | a implementar |
| 9 | Regeneração de HP após 3s fora de combate | — | a implementar |
| 10 | Ataque com arco contra inimigos voadores até 30 blocos | — | a implementar |
| 11 | PvP NPC vs NPC (protótipo) | `/amigopvp on` / `/amigopvp off` (admin) | a implementar |
| 12 | Invencibilidade | `/amigo godmod` (admin) | a implementar |

## Princípios desta versão

- Código novo, escrito a partir deste escopo. O código recuperado do jar (branch `recovery/*` e `port/*`) não é base.
- O jar original descompilado (branch `replica/original-src`) serve só como referência de comportamento, não como base de código.
- Cada funcionalidade entra em commit separado e só é marcada como pronta depois de compilar e testar no servidor 0.7.2.
- Toda API do servidor é conferida no `HytaleServer.jar` 0.7.2 antes de ser usada.

## Ordem de implementação

1. Companheiro: spawn, seguir, despawn (`/amigo spawn`).
2. Comandos básicos: `/amigo` (menu), `/amigo spawn`, `/loot`, `/autoloot`.
3. Mochila de 45 slots.
4. Combate e regeneração.
5. Nível e XP (`/amigolvl`), troca de arma e escala.
6. Arco contra voadores.
7. Admin: `/amigopvp`, `/amigo godmod`.

## Pontos ainda não verificados no 0.7.2

- Spawn e controle de NPC (`NPCPlugin` existe; falta confirmar a assinatura de spawn).
- Acesso a inventário do jogador e do NPC.
- Estrutura do menu (UI) — decidir se usamos `.ui` próprios ou o padrão do servidor.
