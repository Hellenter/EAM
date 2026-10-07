# EAM

Hardcore survival Fabric mod for Minecraft 26.3, inspired by MITE (Minecraft Is Too Easy).
The owner (Maxim, GitHub `Hellenter`) writes in Russian: talk to him and write PR descriptions in Russian.
Code, identifiers, comments and commit messages are in English.

Architecture doc (source of truth for design decisions):
https://claude.ai/code/artifact/8f7d07be-d5a5-4a64-94f2-e52552ec5d37

## Stack

- Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.18-SNAPSHOT, Gradle 9.7.0.
- Minecraft is unobfuscated since 26.1: use official Mojang class and method names. There are no mappings
  (Yarn is discontinued). Dependencies use `implementation`, not `modImplementation`.
- Versions live in `gradle.properties`; check https://fabricmc.net/develop before bumping.

## Commands

- `./gradlew build` — compile, test, jar.
- `./gradlew runDatagen` — regenerate `src/main/generated/`. Commit the output; CI fails if it is stale.
- `./gradlew runClient` / `runServer` — run the game (needs a desktop for the client).
- `./gradlew genSources` — decompiled Minecraft sources for reading vanilla code.

## Layout

- `src/main/java/io/github/hellenter/eam/` — common and server code. Entry point `Eam`.
  - `core/` config, network, registry, tags, utilities.
  - `survival/` player mechanics: `hunger/`, `thirst/`, later `progression/`.
  - `world/` `mining/`, `farming/`, `items/` (stack sizes and other item component edits).
  - `mobs/` `vanilla/` (changes to vanilla mobs) and `entity/` (our mobs).
  - `content/` blocks, items, ores, tools, food. `dimension/` the new dimension.
  - `mixin/` mixins only, mirrored by the same sub-packages.
- `src/client/java/io/github/hellenter/eam/client/` — client only: HUD, renderers, models, client mixins, `datagen/`.
- `src/main/generated/` — datagen output, never edited by hand.

Each mechanic is a package with `XxxModule.init()` (registration, events), `XxxLogic` (pure logic, unit
tested) and thin mixins that only call `XxxLogic`. `Eam.onInitialize` calls each module guarded by its
flag in `EamConfig`.

## Rules

1. **Data before code.** Prefer tags, datapack JSON and item data components. Then Fabric API events.
   A mixin only where neither exists (hunger tick, destroy speed, crop growth, mob AI).
2. **Thin mixins.** Use MixinExtras (`@WrapOperation`, `@ModifyReturnValue`, `@ModifyExpressionValue`).
   Never `@Overwrite`; avoid `@Redirect`. No logic inside the mixin body.
3. **Server decides, client shows.** All rules run on the logical server. Client code never lives in
   `src/main`. The mod must work on a dedicated server.
4. **Player and world state** go in Fabric Data Attachments (`persistent` + `syncWith(..., targetOnly())`
   for what the HUD needs). Values are immutable records; per-tick counters are not synced.
5. **New item properties** are custom data components, not NBT.
6. **Content via datagen**: recipes, loot tables, tags, models, block states, worldgen features,
   translations (`en_us` and `ru_ru`).
7. **Config** (`config/eam.json`, `EamConfig`) holds module on/off flags. Balance numbers belong in data
   so datapacks can override them.
8. Verify vanilla method names and signatures against `genSources` output before writing a mixin;
   26.x names change between versions.

## Workflow

- One mechanic or content piece per branch and PR. CI (`.github/workflows/build.yml`) must be green.
- Every PR says how to check it in game.
- Subagents in `.claude/agents/`: `player`, `mobs`, `content` implement; `vanilla-research` finds hook
  points in vanilla code; `reviewer` checks a diff against the rules above before the PR is marked ready.
