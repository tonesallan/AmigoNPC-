# AmigoNPC 2.0.1 — recovery baseline

## Status

The recovered 2.0.1 source compiles successfully against the current local Hytale server JAR used for validation.

Validated recovery HEAD:

```text
e3526fe0f24864db4cf8f69e1d38b2a2c490e3e8
```

Branch:

```text
recovery/amigonpc-2.0.1-source
```

## Original preserved artifact

The original installed 2.0.1 JAR was preserved before source recovery.

Known original SHA-256:

```text
4D047A91785A9EECF0B0C2C7F4064EBD468062E1AD4D34B4231326CAC3E08725
```

Original structural counts:

- 394 `.class` entries
- 7 `.ui` entries
- 7 `.json` entries

## Recovered build

The recovered source was compiled with Java 25 and Gradle 9.3.

Validated generated artifact:

```text
build/libs/AmigoNPC-2.0.1.jar
```

SHA-256 from the validated build:

```text
990F933AA611F0BE688115488D528A1CF270612DB74DD20ABD12D8AC79050686
```

Structural counts:

- 394 `.class` entries
- 7 `.ui` entries
- 7 `.json` entries

Plugin manifest:

- Group: `br.tones`
- Name: `AmigoNPC`
- Version: `2.0.1`
- Main: `br.tones.amigonpc.AmigoNPCPlugin`
- IncludesAssetPack: `true`

Java manifest:

- Implementation-Title: `AmigoNPC`
- Implementation-Version: `2.0.1`

## Recovery API migrations already applied

The recovered source required only compatibility/source-reconstruction fixes needed to compile on the current target:

1. legacy Hytale `Vector3d` -> JOML `Vector3d`;
2. decompiler-generated generic `Ref<EntityStore>` pattern casts;
3. decompiler source artifacts around enum lookup and duplicate local variables;
4. owner chat messaging through `PlayerRef`;
5. legacy NPC `Inventory` weapon access -> ECS `InventoryComponent`;
6. current HUD constructors/layering API;
7. typed reconstruction of the level GUI event `BuilderCodec`;
8. build artifact version aligned to 2.0.1.

## Important limitation

Matching class/resource counts does not prove byte-for-byte or behavioral equivalence.

The rebuilt JAR is intentionally not expected to have the same hash as the original because it was recompiled from recovered Java source with the current toolchain.

The recovery baseline is considered **compile-valid**, not yet **runtime-valid**.

## Next validation stages

1. compare original and rebuilt JAR entry-name sets;
2. compare non-class resources where practical;
3. perform a controlled runtime load with the original JAR preserved;
4. validate plugin setup/shutdown and core commands;
5. validate NPC spawn/despawn;
6. validate HUD/UI;
7. validate backpack/inventory;
8. validate combat/follow/downed/progression;
9. validate plugin unload/reload behavior before adopting hot reload as the normal development loop.

Do not overwrite or delete the preserved original 2.0.1 artifact during these tests.
