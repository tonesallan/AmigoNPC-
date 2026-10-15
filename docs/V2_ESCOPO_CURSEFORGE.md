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

## Comportamento detalhado (especificação de projeto, fonte: documentos enviados pelo dono)

Estes documentos descrevem o comportamento desejado, além do que a página do CurseForge diz. Nada aqui foi comprovado no jogo.

### Estados e prioridade
- Ordem: `DOWNED > COMBAT > GATHERING > FOLLOWING > IDLE`.
- DOWNED: não luta, não coleta, não segue; aguarda revive.
- COMBAT interrompe coleta imediatamente; ao fim do combate, volta a coletar se houver espaço e recurso, senão segue o dono.

### Combate
- Dois modos mutuamente exclusivos (botões de rádio): "Foco em inimigos frágeis" e "Foco em quem ataca o jogador".
- Opções independentes (caixas de seleção): troca automática de arma e interrupção de ataques inimigos.
- Troca de arma só entre armas que estão na mochila do NPC, nunca no inventário do dono.
- Alvo inacessível não pode prender o NPC em tentativas infinitas: procura outro alvo ou volta a seguir.
- Não lutar como personagem independente; não ultrapassar a força do dono.

### Mochila
- 45 slots, 5 × 9. Acesso por `/loot` e pelo botão Mochila no menu `/amigo`.
- Só o dono abre a mochila.
- Persiste entre sessões.
- Quando cheia: para de coletar, avisa o jogador, não descarta itens.

### Morte e revive
- Morte entra em DOWNED.
- Revive automático em 40 s, com penalidade de 40% da XP do nível atual.
- Revive manual segurando F por 10 s (cancela se soltar), com penalidade de 10%. Outros jogadores podem reviver.
- Se o dono se afastar mais de 25 blocos, o corpo some, mas o contador continua.
- Ao chamar de novo, o corpo reaparece com o estado e o tempo restantes.
- Mensagens aleatórias enquanto morto não contam como revive.

### XP e nível
- Começa no nível 1, sem teto fixo.
- Ganha XP em combate e ao ajudar a derrotar inimigos.
- Sobe vida, dano e eficiência. Não pode superar o dono.
- Fórmula de XP e valores por nível ainda não definidos: decidir antes de implementar.

### Menu `/amigo`
- Mostra nome, estado, vida, nível, barra de XP, modo de combate, opções independentes e botão Mochila.
- Salva as configurações automaticamente e restaura ao voltar ao mundo.
- Só o dono altera as configurações.

### Multiplayer e permissões
- Cada jogador tem um NPC. Todos veem todos os NPCs.
- Outros jogadores podem reviver, mas não abrem a mochila nem mudam configurações.
- PvP entre NPCs é global, começa desligado, só admin liga/desliga (`/amigopvp on|off`).

### Skins e cosméticos
- Pasta `Skins` para skins em `.zip`.
- Persistência precisa guardar o snapshot completo da aparência (slot, id, variante, opção, tipos ocultos), não só uma lista de IDs.
- Integração opcional com AmigoAvatar, sem depender dele.
- Fica para a última etapa.

## Conflitos e riscos a resolver antes de implementar
- A página do CurseForge não menciona DOWNED, revive com F, nem a pasta Skins; a especificação menciona. Decidir se entram na v2.
- Os documentos citam a branch `recovery/amigonpc-2.0.1-source`, que é outra recuperação. Não usar como base.
- Fórmula de XP e valores por nível não estão definidos.
- O comportamento de revive com F depende de detectar tecla contínua e cancelamento, o que precisa confirmar na API.
