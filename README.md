# Better RPG Progression

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).

## Bounded playtest diagnostics

Loader SLF4J `bc.playtest.better_rpg_progression.*` reports one INFO startup/providers/own
source status (UNKNOWN when absent). DEBUG distinguishes manual allocation rejection versus
actual state update/sync submission, pre-XP-removal death capture, canceled capture discard and
actual entitlement enqueue versus duplicate/empty refusal. It never logs per delivery tick,
full arbitrary-precision values, raw requested IDs, player names, inventories, NBT or personal
episode tokens. No additional gameplay RNG, packet fields, entitlement clamps or persistence
changes are introduced. One mod-wide 120/minute burst20 budget, 256 aggregate keys,
2048-character record bound and 30-second summaries limit output; disconnect/server stop clears
keys without bucket refill. Standalone remains INFO; the pack selects scoped DEBUG. Enqueue is
not item delivery, and sync submission is not native StatsScreen presentation acceptance.


Pack-owned RPG stats and diminishing-returns system for Forge `1.20.1`.

## Life aspects

Players commit Life points irreversibly during a life to Strength, Hand Speed, Tool Handling, Athletics, Endurance, Hardiness, Preservation, and Marksmanship. Each stat maps respectively to the Impact, Tempo, Work, Mobility, Endurance, Robustness, Renewal, and Control aspects. One aspect may project into several concrete attributes owned by vanilla, Epic Fight, TConstruct, TACZ, Goety, Cold Sweat, or this mod; those integrations do not share a global aspect meter. Twenty points reach half of each configured cap through the `cap × points / (points + 20)` curve, and death clears the allocation ledger. Authored Life projections complement nutrition rather than duplicate its temporary primary bonuses.

The capability names describe their practical use; body-part badges, stable stat IDs, aspect colors, and audio motifs retain the shared identity with nutrition and ore geology. Strength increases knockback and Epic Fight impact, not damage; Hand Speed increases attack and item-use speed; Tool Handling increases block reach, not mining speed; Athletics increases step height and air capacity, not movement/swimming speed; Endurance increases Epic Fight stamina capacity, not resource efficiency; Hardiness increases knockback resistance, not temperature resistance; Preservation extends beneficial effects and protects them during milk drinking, not harmful-effect recovery or healing; Marksmanship improves projectile handling and supported spell range. Any positive beneficial-duration attribute enables milk preservation by cancelling native beneficial-effect removal, without restoring/reapplying an effect or scaling its duration twice. Without investment, milk retains its normal behaviour. Stat IDs remain unchanged (`liver` now displays Preservation), and the remaining authored curve caps are unchanged. Generic data-driven attribute machinery is retained, but the bundled definitions no longer project nutrition's primary effects. Reprojection clears owned modifiers independently of the new definitions so removed effects cannot survive a live data reload. Athletics also increases underwater air capacity toward twice vanilla capacity. The full-screen allocation screen presents all eight capabilities in a four-by-two learning grid. Each card explains its purpose and shows every active effect even before investment, with the Life-point bonus and marginal gain from one more point side by side. Effect names and units are visible without hovering. Pigment strips and body badges identify the capability; brighter, hue-preserving headings and neutral body text keep dark identities readable. The screen fits its own readable canvas without changing the player's global GUI scale, and uses the full viewport on larger displays.

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
