# Skins do AmigoNPC

O AmigoNPC cria e usa a pasta:

```text
%APPDATA%\Hytale\UserData\Mods\Skins
```

Os arquivos de skin personalizados devem ser fornecidos como `.zip`.

## Formato do ZIP

Cada ZIP deve ser um asset pack válido do Hytale e conter `manifest.json` na raiz. Os assets do modelo, texturas e demais arquivos permanecem no formato nativo do Hytale dentro do próprio pack.

No carregamento do AmigoNPC:

1. a pasta `Skins` é criada caso não exista;
2. todos os `.zip` válidos são lidos em ordem estável;
3. o `manifest.json` de cada pack é validado;
4. o pack é registrado no `AssetModule` do servidor;
5. os modelos disponibilizados pelo pack podem ser usados pelo sistema de modelo/aparência existente do AmigoNPC.

O AmigoNPC não reescreve o conteúdo do ZIP.

## Reload

Os packs registrados pelo AmigoNPC são removidos no shutdown do plugin. Ao usar o fluxo de desenvolvimento:

```text
/plugin unload br.tones:AmigoNPC
# substituir JAR ou ZIPs de skin
/plugin load br.tones:AmigoNPC
```

os ZIPs presentes na pasta `Skins` são registrados novamente.

O sistema de Wardrobe continua separado e mantém sua persistência de cosméticos, variantes, opções e tipos ocultos.
