# Better RPG Progression

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Pack-owned RPG stats and diminishing-returns system for Forge `1.20.1`.

## Life aspects

Players commit Life points irreversibly during a life to Strength, Hand Speed, Tool Handling, Athletics, Endurance, Hardiness, Detoxification, and Marksmanship. Each stat maps respectively to the Impact, Tempo, Work, Mobility, Endurance, Robustness, Renewal, and Control aspects. One aspect may project into several concrete attributes owned by vanilla, Epic Fight, TConstruct, TACZ, Goety, Cold Sweat, or this mod; those integrations do not share a global aspect meter. Twenty points reach half of each configured cap through the `cap × points / (points + 20)` curve, and death clears the allocation ledger. Strength supplies a bounded percentage to direct player, projectile, and player-attributed spell damage once at the server damage boundary; owned creatures and environmental damage remain outside that channel.

The capability names describe their practical use; body-part badges, stable stat IDs, aspect colors, and audio motifs retain the shared identity with nutrition and ore geology. Strength increases damage and knockback; Hand Speed increases attack and item-use speed; Tool Handling improves mining and reach; Athletics improves movement and breathing; Endurance conserves food and water and increases stamina; Hardiness resists temperature and knockback; Detoxification shortens harmful effects and extends beneficial ones without granting healing; Marksmanship improves projectile handling and supported spell range. Athletics also increases underwater air capacity toward twice vanilla capacity. The full-screen allocation screen presents all eight capabilities in a four-by-two learning grid. Each card explains its purpose and shows every active effect even before investment, with the Life-point bonus and marginal gain from one more point side by side. Effect names and units are visible without hovering. Pigment strips and body badges identify the capability; brighter, hue-preserving headings and neutral body text keep dark identities readable. The screen fits its own readable canvas without changing the player's global GUI scale, and uses the full viewport on larger displays.

The card's plus/minus controls edit a reversible draft; committed Life points cannot be removed. Reset draft restores the opening allocations. Auto plan and window/GUI-scale changes preserve the same draft. Commit sends the existing server-validated allocation packet; this screen does not own gameplay attributes. Bonuses exclude equipment and temporary bodily-state effects. Land-speed bonuses are expressed relative to the player's movement base; resistance and duration/spread reductions use readable percentages and signs.


## Heart fragments

A confirmed final death captures the XP level held at death and records a UUID entitlement. The server calculates `ceil(scale × (2^(level / 4) - 1))`, with the server setting `heart_fragments.entitlement_scale` defaulting to 4. Counts are stored as decimal arbitrary-precision values in playerdata and are inserted as at most one normal 64-item stack per player tick; a full inventory leaves the exact remainder pending.

The carrier supports exact computation through level 4096. A larger level is retained as an explicit unresolved UUID-and-level row instead of crashing, clamping, or discarding the entitlement; it needs a future larger-computation migration before delivery. Pending rows use a monotonic completion watermark, so receipts do not grow with death count.


The **Auto plan** editor holds an ordered, persistent per-player list. It can be enabled, paused, reordered, cleared, and edited across deaths or reconnects. Each newly earned point advances one position in the ordered plan. The server validates every entry against active definitions and caps before changing the plan or spending a point; an unavailable or capped choice is skipped and no unearned power is retained.

## Common commands

```bash
./gradlew verifyFast
./gradlew verifyFull
```

For isolated native visual review, prepare the Better Gameplay Notices provider from its current
source, then launch the development-only harness. On a headless lane with Xvfb:

```bash
TMPDIR="$HOME/.tmp" ALSOFT_DRIVERS=null xvfb-run -a -s '-screen 0 1920x1080x24' ./gradlew runVisualHarness --no-daemon
```

The client renders the actual StatsScreen at 1920×1080 across GUI scales 2, 3 and 4, plus a
1280×720 fallback. It captures empty, draft, exhausted and capped allocations, exercises native
button callbacks for draft/reset/undo and Auto plan return, checks text against the real Minecraft
font, and exits. It does not synthesize pointer events, join a server, or alter a world. The fixture
reads and hashes the authored stat JSON rather than maintaining a separate table of effects.
All-authored-integration cases stress the presentation; the loaded-mods case omits unavailable
optional effects. Neither establishes third-party gameplay compatibility or full-pack inventory
acceptance. Captures are disposable outputs under `run-visual-harness/screenshots/` during the
active review. Audio/narrator availability is an independent environment check, not inferred from
visual success.

`verifyFull` runs `verifyFast` plus the transformed production GameTest server, including capability-clone and effect-semantics regressions.

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
