# Isla: "The Broken Source" story design

Written 2026-10-29. Replaces the coconut-and-idol fetch story with a mystery campaign built on debugging.

## Why
The game had solid engineering but nothing to play for: two fetch quests within ten tiles of the spawn, two empty
islands, stamina and money that never forced a choice, and one quiz question as the only challenge.

## The idea
Debugging is the core mechanic and the mystery is the reason to keep going. The island runs on code (the Source, in
the Golden Idol). Someone cut the Idol; the curse shows up as bugs you can see on the map. Fixing a bug changes the
world (a bridge appears, a village wakes, jungle withers) and turns up shards and clues. The clues point to the culprit.

## Structure
- Chapter 1 (home): Kai's intro, Nia's practice puzzle (optional), machete, Jungle Shrine stone -> east bridge, Sun shard, clue.
- Chapter 2 (east): village stone -> villagers wake, Moon shard; Lio and Ula give clues; Ula trades the password for a necklace.
- Chapter 3 (south): South Bridge stone (needs the password), lantern, Overgrowth stone (needs light), temple altar -> Sea shard, clue.
- Finale: shards to Kai, accusation (wrong guesses get the suspect's alibi), mend the Idol, ending.

## Mechanics
- Story flags (areas, shards, clues, password) live in `unlocked_areas`: no schema change.
- `QuestGiver.Stage` can require a flag (locked text until then) and has its own done text.
- `Quest.grants(...)`, `GatherQuest`, `Accusation`, `DebugQuest` feedback and wrong-answer cost.
- Glitch tiles (`e`, `v`, `o`) block until their flag is set, then draw as path or grass.
- Wrong code fix: -10 stamina. Walking: 1 stamina per 4 steps. Rest only next to a hut, or when collapsed (+25).
- Objective strip, journal (Q), paged dialogue.

## Non-goals
Combat, new save tables, continuing saves from the old story.
