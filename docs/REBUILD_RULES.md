# AmigoNPC 2.0 — premissas atuais de desenvolvimento

Estas são as premissas atuais. Podem ser alteradas posteriormente pelo owner do projeto.

## Git

- A branch `main` permanece como histórico da implementação anterior.
- A reconstrução acontece na branch `rebuild/amigonpc-2.0`.
- Correções e implementações são feitas primeiro no GitHub.
- O ambiente local do Windows é atualizado a partir do Git.
- Commits devem ser pequenos, claros e limitados ao módulo em trabalho.

## Modularidade

- Não misturar correções independentes.
- Trabalhar um módulo por vez.
- Preservar funcionalidades existentes até existir substituto testado.
- Não avançar para o próximo módulo sem validar o anterior.
- Código antigo pode servir como referência, mas não deve ser migrado automaticamente.

## Hot reload

Hot reload é requisito arquitetural do AmigoNPC 2.0.

O código novo deve evitar referências estáticas a objetos/tipos de runtime que possam se tornar inválidos após reload.

Ao descarregar/recarregar o plugin, o objetivo é:

1. interromper tarefas e loops do AmigoNPC;
2. persistir dados necessários;
3. remover referências runtime antigas;
4. permitir que os registries pertencentes ao plugin sejam desmontados corretamente;
5. carregar novamente o plugin sem reiniciar o cliente;
6. restaurar companions de jogadores online quando essa etapa for implementada.

O mecanismo oficial de referência é `PluginManager.reload(PluginIdentifier)`.

## Compatibilidade

- Java 21.
- Preferir API pública e tipada da Hytale.
- Reflection apenas para integrações opcionais/compatibilidade justificada.
- A build deve receber o caminho do HytaleServer.jar por `HYTALE_SERVER_JAR` quando disponível.
- Não versionar HytaleServer.jar nem artefatos locais de build.

## Testes

Cada módulo deve ter critérios de teste definidos antes de ser considerado concluído.

Fluxo:

```text
GitHub -> commit -> atualizar PC -> build -> instalar JAR -> reload -> testar
```

Se um módulo falhar, corrigir no mesmo módulo antes de iniciar outro.

## Prioridade funcional do NPC

A regra central permanece:

```text
DOWNED > COMBAT > GATHERING > FOLLOWING > IDLE
```

Combate tem prioridade absoluta sobre coleta. Durante combate, o NPC não coleta itens nem continua uma tarefa de gathering.
