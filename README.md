# 🤝 AmigoNPC — Companion NPC Mod for Hytale

**AmigoNPC** é um mod server-side para **Hytale**, desenvolvido com base no **Entity Component System (ECS) real da API do jogo**, que adiciona um **NPC companheiro inteligente**, persistente e configurável para cada jogador.

> 🎯 Filosofia do projeto  
> *“Um companheiro inteligente, não um bot roubado.”*

---

## 📌 Status do Projeto

🚧 **Em desenvolvimento ativo**  
📚 Arquitetura e documentação consolidadas  
🧠 ECS compatível com a API real do Hytale  
⚠️ API do Hytale ainda instável → código defensivo por padrão

---

## ✨ Funcionalidades Principais

### 🧍 NPC Companheiro
- Um NPC por jogador
- Vinculado permanentemente ao dono (Owner)
- Visível para todos no mundo
- Processado exclusivamente no servidor

---

### ⚔️ Combate (Prioridade Máxima)
- Modos de combate configuráveis:
  - **Foco em inimigos frágeis**
  - **Foco em quem ataca o jogador**
- Atua em sincronia com o player
- Interrompe automaticamente qualquer outra ação (coleta, follow, etc.)
- Troca automática de armas (opcional)

---

### 🎒 Inventário (Mochila)
- Inventário próprio do NPC
- Capacidade fixa: **45 slots (5x9)**
- Persistente entre sessões
- Acessível via:
  - UI `/amigo`
  - Comando `/loot`
- NPC avisa quando o inventário estiver cheio

---

### ⛏️ Coleta
- Coleta automática de recursos
- Suspensa automaticamente em combate
- Retoma após o combate (se houver espaço)
- Para completamente se o inventário encher

---

### 💀 Morte & Revive
- Estado **DOWNED** ao morrer
- Revive automático após **40s**
  - Penalidade: **-40% do XP do nível atual**
- Revive manual (tecla **F** por 10s)
  - Penalidade reduzida: **-10% do XP**
- Outros jogadores podem ajudar a reviver

---

### 📈 XP & Progressão
- XP e nível próprios do NPC
- Ganha XP em combate
- Atributos escalam com o nível
- **Nunca ultrapassa o jogador**
- Balanceado para suporte, não substituição

---

### 🪟 Interface Gráfica (/amigo)
- UI server-side (Custom UI do Hytale)
- Mostra:
  - Nome do NPC
  - Estado atual
  - Vida
  - Nível e barra de XP
- Permite:
  - Selecionar modo de combate
  - Ativar opções
  - Abrir mochila
- Configurações persistentes

---

## 🧠 Arquitetura Técnica

O projeto segue **ECS puro**, conforme a API real do Hytale.

### 🧩 Componentes Custom
- `OwnerComponent` — vínculo NPC ↔ jogador
- `StateComponent` — estado atual (IDLE, FOLLOW, COMBAT, DOWNED, etc.)
- `CombatModeComponent` — modo de combate e opções
- `InventoryComponent` — mochila persistente
- `XPComponent` — progressão e penalidades

### ⚙️ Sistemas Principais
- `AmigoSpawnSystem`
- `AmigoOwnerSystem`
- `AmigoFollowSystem`
- `AmigoCombatSystem`
- `AmigoInventorySystem`
- `AmigoGatherSystem`
- `AmigoDownedSystem`
- `AmigoReviveSystem`
- `AmigoXPSystem`
- `AmigoUISystem`
- `AmigoPersistenceSystem`

> 🛡️ Combate sempre tem prioridade máxima sobre todos os outros sistemas.

---

## 📦 Compatibilidade com Hytale

- ✅ Server-authoritative
- ✅ Multiplayer compatível
- ✅ Persistência real
- ✅ NetworkId obrigatório
- ⚠️ Imports e pacotes podem variar entre builds

O projeto utiliza **engenharia defensiva** (ex: reflexão quando necessário) para reduzir quebras entre versões do server.jar.

---

## 📁 Estrutura do Projeto (resumo)

br.tones.amigonpc
├─ components/
├─ systems/
├─ commands/
├─ ui/
├─ core/
└─ AmigoNPCPlugin.java


---

## 🚫 O que o AmigoNPC NÃO faz

- ❌ Não substitui o jogador
- ❌ Não joga sozinho
- ❌ Não fica mais forte que o player
- ❌ Não ignora combate para coletar
- ❌ Não altera configurações sem permissão

---

## 🧭 Objetivo do Projeto

Criar um NPC:
- Útil
- Confiável
- Previsível
- Configurável
- Balanceado

Um verdadeiro **companheiro**, não um exploit.

---

## 👤 Autor

**Tones Allan de Oliveira Alves**  
🔗 GitHub: https://github.com/tonesallan

---

## 📜 Licença

Definida futuramente.

---

> ⚠️ Nota final  
> Este projeto acompanha a evolução da API do Hytale. Mudanças estruturais podem ocorrer conforme novas builds do servidor forem lançadas.
