# AmigoNPC 2.0.1 — Baseline CurseForge

## Fonte de verdade

Arquivo fornecido pelo autor/usuário:

- Nome: `AmigoNPC-2.0.1.jar`
- SHA-256: `31B3515FD5897F2D81818030EC9692B485B0850DEE0E452B1804D5630328ED17`

Esta branch existe para reconstruir uma réplica funcional fiel deste JAR antes de qualquer mudança de design.

## Inventário confirmado do JAR

- 151 arquivos `.class`
- 7 arquivos `.ui`
- 7 arquivos `.json`
- 0 arquivos `.properties`

## Regra de reconstrução

Durante esta etapa:

1. O comportamento observado/extraído deste JAR é a referência.
2. Não adicionar coleta/mineração, revive manual, novos modos de combate, troca de arma limitada à mochila, skins ZIP, i18n ou outras funcionalidades posteriores.
3. Alterações exigidas pela API atual do Hytale devem ser de compatibilidade, preservando o comportamento da versão publicada.
4. Recursos UI/JSON devem permanecer equivalentes aos empacotados no JAR.
5. A validação final deve comparar comandos, estados, constantes, UI, persistência, combate, loot, XP/nível, GodMode, PvP, HUD, cosméticos e lifecycle.
6. Somente depois da equivalência funcional desta baseline serão retomadas as mudanças novas.

## Observação importante

O snapshot anterior chamado de “2.0.1 decompiled baseline” no histórico foi produzido a partir de outro artefato e não deve ser usado como fonte de verdade para esta reconstrução.