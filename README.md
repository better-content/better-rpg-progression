# Better RPG Progression

Pack-owned RPG stats and diminishing-returns system for Forge `1.20.1`.

## Life aspects

Players commit Life points irreversibly during a life to Arms, Hands, Fingers, Lungs, Blood, Skin, Liver, and Eyes. Each stat maps respectively to the Impact, Tempo, Work, Mobility, Endurance, Robustness, Renewal, and Control aspects. One aspect may project into several concrete attributes owned by vanilla, Epic Fight, TConstruct, TACZ, Goety, Cold Sweat, or this mod; those integrations do not share a global aspect meter. Twenty points reach half of each configured cap through the `cap × points / (points + 20)` curve, and death clears the allocation ledger. Arms supplies a bounded percentage to direct player, projectile, and player-attributed spell damage once at the server damage boundary; owned creatures and environmental damage remain outside that channel.

The stat badges use body-part pixel art while retaining the aspect colors and audio motifs shared with nutrition and ore geology. Lungs also increases underwater air capacity toward twice vanilla capacity. The allocation screen keeps all eight capabilities visible in a fixed two-column grid; only the resulting-property list scrolls, and properties with no committed or draft effect are omitted.


## Heart fragments

A confirmed final death captures the XP level held at death and records a UUID entitlement. The server calculates `ceil(scale × (2^(level / 4) - 1))`, with the server setting `heart_fragments.entitlement_scale` defaulting to 4. Counts are stored as decimal arbitrary-precision values in playerdata and are inserted as at most one normal 64-item stack per player tick; a full inventory leaves the exact remainder pending.

The carrier supports exact computation through level 4096. A larger level is retained as an explicit unresolved UUID-and-level row instead of crashing, clamping, or discarding the entitlement; it needs a future larger-computation migration before delivery. Pending rows use a monotonic completion watermark, so receipts do not grow with death count.


The **Auto plan** editor holds an ordered, persistent per-player list. It can be enabled, paused, reordered, cleared, and edited across deaths or reconnects. Each newly earned point advances one position in the ordered plan. The server validates every entry against active definitions and caps before changing the plan or spending a point; an unavailable or capped choice is skipped and no unearned power is retained.

## Common commands

```bash
./gradlew verifyFast
./gradlew verifyFull
```

For an isolated 1280×720 visual fixture containing only Minecraft, the RPG Stats
runtime, and development harness code, run:

```bash
./gradlew runVisualHarness
```

The client captures the seeded stats screen at GUI scales 3 and 2, writes screenshots under
`run-visual-harness/screenshots/`, continues through its identity/audio fixtures, and exits automatically.

`verifyFull` currently matches `verifyFast`; this repo does not yet expose a distinct GameTest or coverage-enforced full lane.

## Release artifact

Deploy the reobfuscated runtime jar from:

- `build/libs/better-rpg-progression-<version>.jar`

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Canonical identity

- Repository and release artifact: `better-rpg-progression`
- Mod ID and resource namespace: `better_rpg_progression`
- Java package: `com.bettercontent.betterrpgprogression`
- Validation: `./gradlew verifyFull`

This normalization is a clean break. Worlds, configuration files, and integrations created for earlier identities are not migrated or aliased.
