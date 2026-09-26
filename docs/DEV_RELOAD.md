# AmigoNPC — fluxo de desenvolvimento sem reiniciar o Hytale

Plugin: `br.tones:AmigoNPC`

## Objetivo

Atualizar o JAR do AmigoNPC durante o desenvolvimento sem fechar e abrir o Hytale a cada alteração.

A API atual do servidor expõe `PluginManager.unload(...)`, `load(...)` e `reload(...)`. O unload fecha o classloader Java do plugin. O cleanup do `PluginBase` também encerra automaticamente command registry, event registry, task registry e registries ECS pertencentes ao plugin.

O AmigoNPC complementa esse cleanup no próprio `shutdown()`: para os tickers, encerra revive manual/HUD, salva estado persistente e remove os NPCs ativos antes de a nova instância ser carregada.

## Fluxo seguro para trocar o JAR

1. No jogo ou console:

```text
/plugin unload br.tones:AmigoNPC
```

2. No PowerShell:

```powershell
Set-Location "$env:USERPROFILE\Documents\AmigoNPC"
.\scripts\DEV_BUILD_DEPLOY.ps1
```

O script valida branch/working tree, usa Java 25, configura `HYTALE_SERVER_JAR`, executa `clean build`, substitui o JAR em `%APPDATA%\Hytale\UserData\Mods` e compara o SHA-256 da origem e do destino.

3. No jogo ou console:

```text
/plugin load br.tones:AmigoNPC
```

Isso atualiza o código sem reiniciar o Hytale.

## Reload de configuração

Quando nenhuma classe/JAR mudou e a alteração é apenas configuração:

```text
/amigo reload
```

## Reload direto

O servidor também possui:

```text
/plugin reload br.tones:AmigoNPC
```

Esse comando serve quando o JAR já foi atualizado no diretório de mods. No Windows, para o ciclo normal de desenvolvimento, `unload -> copiar -> load` é o fluxo seguro porque o classloader é fechado antes da substituição do arquivo.
