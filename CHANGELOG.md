# Changelog

## Unreleased

### Fixed

- Made the Still-Beating Heart's double-pulse item animation visible and reliable at normal GUI scale.
- Made Mining Speed apply through a native synchronized attribute and Forge break-speed hook, including Tinkers' Construct tools.

### Changed

- Removed the bundled 50-point stat caps while retaining optional datapack-defined caps.
- Locked committed allocations until death; the stats screen can now undo only points added in its current draft.
- Rebalanced Temperature Resistance to provide a stronger early benefit with uncapped diminishing returns.
- Standardized the project as **RPG Stats** with mod ID `better_rpg_progression`, artifact `better-rpg-progression`, and package `com.bettercontent.betterrpgprogression`.
- Adopted Java 17 and Forge 1.20.1-47.4.13 as the build baseline without changing the project version.
- This is a clean break; legacy worlds, configurations, and integrations are not migrated.
