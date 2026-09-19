# Phantom Storage — Session 2 Hand-off

Session 1's hand-off (cloning, JEI jar setup, build verification) is done and
still accurate for environment setup — see git history for that trail. This
doc covers session 2, run interactively at a real terminal with a live
Minecraft client the whole time (not the blind cloud-sandbox session that
wrote the original hand-off), so everything below **was actually tested
in-game** unless marked otherwise.

## State of the repo right now

**Nothing from tonight is committed.** `git status` shows a long list of
modified/new files, all still sitting in the working tree. First job at the
next session: decide whether to commit (probably as several logical commits
matching the sections below, mirroring how session 1 did one PR per feature)
before doing anything else, so this work doesn't ride along uncommitted with
whatever comes next.

The dev client was left running at the end of this session — check for a
stray `BootstrapLauncher` process before launching a new one.

## What actually got done tonight (all verified in-game)

1. **Three UI text-overlap bugs, all fixed and confirmed:**
   - Links tab title overlapped its own header text (`PhantomChestScreen`
     `LOGI_HEADER_Y`/`LOGI_ROWS_Y` were too close to the vanilla title
     position).
   - JEI's "Show Recipes" click-area tooltip leaked onto the Chest tab
     because it was registered as a screen-static rectangle with no
     tab-awareness — switched to `addGuiContainerHandler` with a dynamic,
     tab-and-tier-gated `getGuiClickableAreas`.
   - The wrench's "Linked: X / Y" HUD text collided first with the
     item-name popup, then with vanilla's action-bar messages (which the
     wrench itself uses for every link/unlink/limit message) — settled at
     `h - 88`, clear of the whole vanilla bottom-HUD stack.

2. **Dimension-change / Anchor interaction fixed:** a chest docked to a
   Phantom Anchor no longer despawns when the owner changes dimension.
   `DimensionEvents.onPlayerChangeDimension` now skips discard entirely for
   an anchored chest — `PhantomChestEntity.remove()`'s existing
   anchor-release-on-any-removal-path logic still keeps the Anchor block
   correctly in sync (no stale `dockedChestId`, no permanently-forced
   chunk). Confirmed intended per user: a docked chest should only ever
   despawn on logout or by reusing the summoner item, never just from
   crossing dimensions. **Still on the user's list to personally verify
   in-game** (they asked for a reminder — hasn't been done yet as of this
   write-up).

3. **Anchor VFX pass**, confirmed working in-game:
   - Texture (`textures/block/phantom_anchor.png`) reworked: kept the
     purple ring and glowing core the user liked, added a smooth radial
     gradient on the inner glow and a new thin phantom-teal accent ring
     between the glow and the purple ring.
   - Light level bumped from 6 to 10 to match soul lantern.
   - New particle type `phantomstorage:anchor_soul` (`ModParticles`,
     `client/particle/AnchorSoulParticle`) — visually identical to vanilla's
     soul particle (reuses the same sprite frames) but with lifetime scaled
     ×1.15 so it travels ~15% farther without any velocity change. The
     Anchor block spawns these while a chest is docked
     (`PhantomAnchorBlockEntity.spawnDockedParticle`, called from
     `serverTick`), and **the chest itself also switches to this particle
     while docked** — required adding a new synced `DOCKED_TO_ANCHOR`
     entity-data flag on `PhantomChestEntity` since `anchored`/
     `dockedBlockPos` were plain server-only fields the client couldn't see.

4. **Tier system reworked** — confirmed working in-game:
   - Only one summoner item now (`phantom_chest_summoner`, always tier 0).
   - The old tier-1/tier-2 summoner items are now
     `PhantomChestUpgradeTokenItem`s ("Chest Upgrade Token (Tier 2)"/
     "(Tier 3)") — right-click your already-active chest with one to
     upgrade it in place. Rejected with a message if already at/above that
     tier; consumed on success (skipped in creative).
   - Recipes unchanged, only the resulting items' behavior changed.
   - Fixed a regression this same rework would have introduced:
     `PhantomChestSummonerItem.use()` used to hardcode the tier it set on
     summon; with only one summoner left that would've reset an upgraded
     chest back to tier 0 on every dismiss/resummon. It now reads
     `PhantomChestEntity.getSavedTier(player)` instead.

5. **Crafting grid merged into the Chest tab** — confirmed working in-game,
   including JEI recipe transfer into the merged grid. The Craft tab is
   gone; the 3×3 grid + result now render under the main inventory
   whenever tier ≥ 1. Window grew taller (`PhantomChestMenu`
   `PLAYER_INV_Y`/`HOTBAR_Y` shifted +55, `CRAFT_GRID_Y`/`CRAFT_RESULT_Y`
   added) to fit it. Tab IDs renumbered (`TAB_CRAFT` removed,
   `TAB_FILTER`/`TAB_LOGISTICS`/`TAB_REFILL` shifted down by one).

6. **Filter tab renamed to "Trash"**, with a short, word-wrapped, edge-safe
   warning under the grid ("Matching items are destroyed, not stored. This
   cannot be undone.") — confirmed rendering correctly, contained within
   the panel.

7. **Anchor redstone control was cut, then reverted at the user's request**
   — net effect on the repo is **zero change** to redstone behavior, but
   worth knowing the history: input modes (ACTIVE_HIGH/ACTIVE_LOW/
   PULSE_TOGGLE) and the sneak+right-click dock toggle were removed for a
   few iterations, then fully restored via `git checkout` (for the files
   that had no other changes mixed in) and a manual line-by-line re-merge
   for `PhantomAnchorBlockEntity.java` (which also picked up the particle
   VFX addition in the same window — diffed against git afterward to
   confirm only the particle code remained as a net change). Comparator
   output (Presence/Fullness) was never touched either way.

## Next session priorities (in the order the user asked for them)

### 1. Audit the Anchor's redstone functions — user reports "most seem to not work"

Not yet investigated in-game this session (came up right as we were
wrapping). I did a static-code read of the current (fully restored, see
above) `PhantomAnchorBlock.neighborChanged` → `PhantomAnchorBlockEntity
.onRedstoneChanged` wiring and found nothing obviously broken — the block
has no custom shape/occlusion override that would block neighbor-signal
detection, `hasNeighborSignal(pos)` is the standard vanilla call, and the
mode-switch logic (`ACTIVE_HIGH`/`ACTIVE_LOW`/`PULSE_TOGGLE`) looks
sound on paper.

One real *candidate* explanation, not confirmed: **`inputMode` defaults to
`NONE`** on every anchor, and the only way to change it is the GUI's
"cycle input mode" button. If that button isn't obviously doing anything
(no visible mode name change, or requires several clicks to notice), a
user could easily conclude "redstone doesn't work" while actually still
sitting on `NONE` the whole time. Worth checking first, before assuming a
logic bug.

Test plan for next session (mirrors the original hand-off's style):
1. Open the Anchor GUI, confirm the input-mode button's label actually
   changes text on each click, cycling NONE → ACTIVE_HIGH → ACTIVE_LOW →
   PULSE_TOGGLE → NONE.
2. For each non-NONE mode, wire a lever directly against the block and
   confirm dock/undock follows the signal correctly (including the
   10-tick rate limit — rapid flips shouldn't spam-toggle).
3. Confirm PULSE_TOGGLE only fires on the rising edge (power on), not
   falling edge.
4. Confirm comparator output still works independently of all the above —
   PRESENCE (15 when docked, 0 otherwise) and FULLNESS (scales with the
   docked chest's inventory, refreshes every 8 ticks) — since this half
   was never touched by the cut-then-revert.
5. If a real bug turns up, check whether it predates tonight's
   revert-surgery on `PhantomAnchorBlockEntity.java` — diff against
   `git log` on that file once this session's work is committed, to rule
   out something introduced by the merge rather than pre-existing.

### 2. New "Storage" link type + "Storage" tab — a networked storage terminal

This is a **major new feature**, not a small addition — it needs design
decisions made explicitly before implementation starts, not discovered
mid-build. What the user asked for, verbatim intent:

- A new wrench link type/mode called **"Storage"**, distinct from the
  existing INPUT/OUTPUT (`DesignationMode`). Exact semantics still
  undefined — does it replace the INPUT/OUTPUT cycle with a third option,
  or is it an orthogonal flag ("also expose this link's contents in the
  network view") layered on top of the existing push/pull modes? **Needs
  to be pinned down with the user before writing code.**
- A new **"Storage" tab** in the main Phantom Chest GUI: a searchable,
  scrollable view aggregating the contents of *every* "Storage"-linked
  container, across dimensions (the user's phrasing: "cross dimension
  'cuz ender'"), modeled on Refined Storage / Applied Energistics 2 / Tom's
  Simple Storage Network's crafting terminal — item grid + search bar +
  crafting grid that auto-pulls ingredients from the aggregated network.
- This tab should become the **default tab on open** once unlocked, gated
  behind a new **Tier 4** upgrade token crafted with a nether star or
  similar end-game ingredient.

Open questions to resolve with the user before starting:
- Link semantics (above) — the single biggest unknown.
- Live-sync model: does the terminal poll all linked containers
  continuously (network-wide tick scan, potentially expensive across
  many dimensions), or fetch fresh on open/interaction (cheaper, matches
  how AE2/RS actually behave — they cache and push deltas, which is a much
  bigger undertaking than a naive per-tick scan)? Given this mod's existing
  transfer logic is a simple periodic tick scan per chest
  (`tickLinkedStorages`), a naive extension of that pattern to "scan N
  containers across M dimensions every tick to keep a UI live" will not
  scale — this needs actual design thought, not just more of the same
  pattern.
- Crafting-terminal auto-fill needs recipe-lookup + network-extraction
  logic that doesn't exist anywhere in the codebase yet (the existing
  crafting grid is a plain vanilla `TransientCraftingContainer, ` manually
  filled).
- Tier 4 numerically extends `LINK_CAP_BY_TIER` / `TRANSFER_RANGE_BY_TIER`
  / `TRANSFER_INTERVAL_BY_TIER` (currently length-3 arrays, indices 0–2)
  to a 4th entry — needs actual balance numbers, not just "bigger."
  Also needs a new recipe file, lang entries, and a `PhantomChestUpgradeTokenItem`
  registration at tier 3 (0-indexed).
- Whether "default tab" means *every* time the chest is opened at tier 4+,
  or just the first time it's revealed — the user's wording ("so ... is
  the default when opening the phantom chest") reads as permanent, but
  worth a one-line confirmation before building it that way.

Recommend starting next session with a planning pass on this feature
(Plan mode or an explicit design discussion) before touching code — it's
the largest single feature this mod has taken on yet, and the "AE2/RS/Simple
Storage" comparison implies a fair amount of UI and networking
infrastructure that doesn't exist in this codebase at all currently.

## Architecture quick-reference (updated)

Everything from session 1's architecture section still applies. Additions
from tonight:

- `ModParticles.java` / `client/particle/AnchorSoulParticle.java` — the
  boosted-lifetime soul-particle variant shared by the Anchor and a docked
  chest.
- `item/PhantomChestUpgradeTokenItem.java` — the tier-upgrade tokens
  (right-click-the-active-chest-to-upgrade items).
- `PhantomChestMenu.java` — tab IDs are now `TAB_CHEST=0, TAB_FILTER=1,
  TAB_LOGISTICS=2, TAB_REFILL=3` (no `TAB_CRAFT` — merged into
  `TAB_CHEST`, gated by `TabSlot`'s new `minTier` parameter).
- `PhantomChestEntity.DOCKED_TO_ANCHOR` — synced entity data flag, the
  only client-visible signal of "docked to a block" (as opposed to
  `anchored`/`dockedBlockPos`, which remain server-only plain fields).
