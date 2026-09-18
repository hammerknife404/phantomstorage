# Phantom Storage — Session Hand-off

Written at the end of a Claude Code web session so work can continue at a
real terminal. Read this first, then `git log --oneline` for the exact
commit trail — this doc summarizes and explains, the log is ground truth.

## TL;DR to get moving

```
git clone <your fork/repo url>
cd phantomstorage
git checkout master          # everything below is already merged here
```

You need a **local JEI jar** to build — it is not committed and not
pulled from any Maven repo:

```
build.gradle line 45-46:
    compileOnly files('jei-1.21.1-neoforge-19.27.0.340.jar')
    runtimeOnly files('jei-1.21.1-neoforge-19.27.0.340.jar')
```

Download that exact JEI build (or update the filename/version to whatever
you have) and drop it in the repo root before running Gradle. This is the
**one thing the cloud sandbox this session ran in could never verify** —
no network access to `maven.neoforged.net` at all (blocked the Gradle
plugin itself) and no access to fetch JEI either. Every change this
session made was reviewed by hand, never compiled. **Your first job at a
real terminal: `./gradlew build` and fix whatever doesn't compile.** I
was careful, but I was flying blind the whole time — verify before you
trust any of it.

## What Phantom Storage is

A NeoForge 1.21.1 mod: a floating, semi-transparent "Phantom Chest" pet
entity that follows the player Allay-style, plus wrench-based remote
linking to external storage, and (new this session) a dockable Anchor
block. Mod ID `phantomstorage`, package `com.phantomstorage`.

## Session timeline (all merged to `master`)

Worked in rough dependency order — later items build on earlier ones:

1. **Tier-scaled link logistics + Allay-style idle drift** — wrench link
   cap/range/speed now scale per chest tier instead of flat constants;
   added an ambient-drift goal so the chest flutters near its owner
   instead of freezing when idle.
2. **Anchor Mode** — sneak+right-click the chest to lock it in place
   (temporary base station). `PhantomChestEntity.anchored` /
   `anchorPos` fields, gates `FollowOwnerGoal` and the teleport-snap.
3. **Logistics tab** — 4th GUI tab, lists all wrench links with
   mode-cycle/unlink controls, no need to hold the wrench.
4. **Wrench link-ownership fix** — first-linker-wins; a second player
   can't link a storage another online player already has linked.
   (Online-only check — no persistent cross-session registry.)
5. **Phantom Anchor block** (Phase A) — new placeable block +
   `PhantomAnchorBlockEntity` + its own small GUI. Docks the chest at a
   fixed point with a configurable 1–16 block roam radius, reusing the
   Anchor Mode machinery from #2. Sneak+right-click quick-toggles;
   plain right-click opens the GUI (status, radius ±, eject).
6. **Anchor redstone I/O** (Phase B) — input modes (active high/low,
   pulse toggle) and comparator output (presence/fullness). Anti-spam
   is a **self-contained** rate limit on the block itself, deliberately
   *not* sharing the owner's summon-item cooldown (that idea was in the
   original design doc but breaks the moment the owner logs off).
7. **`IItemHandler` capability** on the chest's 54-slot main inventory
   only (not craft grid/filter/refill) — for sorting-mod compatibility
   (Inventory Tweaks, Inventory Profiles Next, etc.).
8. **Single-chunk force-loading** for the Anchor block — forces exactly
   its own chunk, only while a chest is docked, never a radius.
9. **Auto-Refill tab** — 5th GUI tab, 9-slot ghost grid defining items
   to keep stocked; tops up the owner's hotbar from the chest's main
   inventory every second.
10. **Repo cleanup** — removed unreferenced pre-rename concept art
    (`spooky_friendly_phantom_ender_chest_*.png`,
    `phantom_chest_texture_assets/`) that predated the mod's rename
    from "phantomchest" to "phantomstorage"; added to `.gitignore`.

Each of the above landed as its own PR (#1–#4 on GitHub, some batching
several commits when a PR sat open across the session). Commit messages
are detailed — read them for the "why," not just the "what."

## Known gaps / open decisions (nothing built yet)

- **REI/EMI support** — explicitly not built. JEI itself is a
  locally-supplied jar (see above), and REI/EMI have no configured Maven
  repo in this project either. Before building either plugin: confirm
  you actually want it, and get the appropriate jar in place the same
  way JEI's is. Don't just add a `repositories { maven { url ... } }`
  block and hope — verify it resolves at a real terminal first.
- **Phantom Link vs. Wrench unification** — the old `PhantomLinkBlock`
  (channel-paired teleport pipe) is still in the codebase but disabled
  (no texture/model — see `ModItems.java` comment). Whether it gets
  reactivated as a higher-tier logistics option, or stays dead code, was
  explicitly deferred twice this session. Still open.
- **Forced-chunk edge case**: if a `PhantomAnchorBlockEntity`'s
  `dockedChestId` ever goes stale (entity gone but the block still
  thinks it's docked — shouldn't happen given the `remove()` override
  added this session, but hasn't been tested in a real world), the
  forced chunk could theoretically outlive its purpose until the next
  interaction with that block. Worth an eye during testing, not treated
  as a known bug.
- **Fullness comparator mode** only refreshes every 8 ticks while
  docked+selected (a lightweight ticker, not fully vanilla-chest-live).
  Untested against a real comparator circuit.

## Things I could not verify in this environment

The cloud sandbox blocked `maven.neoforged.net` outright (the NeoForge
Gradle plugin repo), so `./gradlew build`/`test`/anything Gradle-based
**never ran successfully once, all session**. Concretely unverified:

- All Java changes compile (checked manually: brace-balance, import
  correctness, method-signature matches against known NeoForge/vanilla
  1.21.1 API — but never compiler-checked).
- The generated placeholder texture for the Phantom Anchor block
  (`textures/block/phantom_anchor.png`) — a small hand-written PNG
  encoder script generated it since no image tooling was available. It's
  a simple violet/cyan rune on a dark base. **Replace with real art** —
  it was always meant as a placeholder, not final.
- Any in-game behavior at all — GUI layouts, click regions, redstone
  timing, chunk-loading — all designed against known vanilla/NeoForge
  patterns but never loaded in an actual client.

**Test plan, roughly in order of risk:**
1. `./gradlew build` — fix compile errors first.
2. Launch a client, summon a chest, open its GUI — confirm all 5 tabs
   render and switch correctly (Chest/Craft/Filter/Logistics/Refill).
3. Wrench-link a few containers, check the Logistics tab matches.
4. Place a Phantom Anchor, dock/undock via sneak-click and via its GUI,
   confirm radius roam and the eject button.
5. Redstone: active-high/low/pulse-toggle against the Anchor, comparator
   presence/fullness readings.
6. Auto-Refill: define an item, empty your hotbar, confirm it tops back
   up within ~1s from the chest inventory.
7. Multiplayer, if you can: two players, one tries to link a storage the
   other already has linked — should be rejected with a message.

## Architecture quick-reference

- `entity/PhantomChestEntity.java` — the big one. Owns the 54-slot main
  inventory + filter + refill slots, tier logic, anchor/dock state,
  linked-storage transfer, and the follow/drift AI goals (nested classes
  at the bottom of the file).
- `inventory/PhantomChestMenu.java` — all 5 tabs' slot layout lives here
  as named constants (`CHEST_SIZE`, `FILTER_START`, `REFILL_START`,
  etc.) — **JEI's plugin reads these by name, not magic numbers**, so if
  you ever reorder tabs again, JEI needs no changes, just keep the
  constants right.
- `block/PhantomAnchorBlock*.java` — the dockable block + its BE
  (owner, radius, redstone modes, chunk-forcing, dock/undock logic).
- `item/PhantomWrenchItem.java` — remote-link creation/cycling, now with
  the first-linker-wins check.
- `client/PhantomStorageJeiPlugin.java` — recipe transfer for the craft
  grid tab. Confirmed still correct after last session's slot
  renumbering; no changes needed unless you touch slot layout again.
- `events/` — `DimensionEvents` (dismiss/save on dimension change,
  login/logout sweep, respawn data copy), `ModCapabilities` (the new
  IItemHandler registration), `ServerEvents` (attribute registration).

## GitHub access note

This session pushed via a GitHub App connection that had to be granted
mid-session (first push attempt 403'd until you/an org admin installed
it). If you're setting up CI or another bot account, that's a one-time
per-repo grant, not a recurring issue.
