# Presentation notes

## The pitch in three sentences
Isla is a top-down island mystery where the island runs on code and someone has broken it. You fix cursed code
stones (real Java bugs), and every fix changes the world, returns a shard of the Golden Idol or a clue. The clues
point to the culprit, and your rank at the end depends on how few mistakes you made and how rarely you died.

## Demo route (about 5 minutes)
Prepare a save in Chapter 2 beforehand (play up to the east bridge), so the demo doesn't start with walking.

1. **Main menu**: point out the best rank. Open *How to Play* for a few seconds.
2. **Continue**: the objective strip at the top says what to do next. Press **Q** for the journal (shards, clues, rank so far).
3. **Walk next to the broken south bridge or the red vines**: the glitch static stings (-2 stamina per step).
4. **Fix the village stone**, picking a **wrong answer first**: it explains why it is wrong and shocks you (-10).
   Then the right one: the villagers wake up and the Moon shard drops.
5. **Talk to Fisher Lio**: a clue appears in the journal.
6. **Show dying** (optional): walk around with low stamina or take a shock when nearly empty. YOU DIED, then Z
   wakes you at the last save, fully rested.
7. **Show the ending** from a save made just before Elder Kai's last questions: the accusation (a wrong suspect gives
   an alibi), the confession, the final bug, then the ending screen with the **rank**.
8. **Run `mvn test`**: 381 tests, including one that plays the whole story by itself.

## Who explains what

| Topic | What to say | Where in the code |
|---|---|---|
| Story and dialogue | All story text, puzzles and rewards are data in one class; the database only stores progress | `game/SeedData.java` |
| Inheritance and polymorphism | `Accusation` extends `DebugQuest` extends `Quest`, and `GatherQuest` extends `Quest`; each overrides only what differs | `Quest`, `DebugQuest`, `GatherQuest`, `Accusation` |
| Story flags | Quests *grant* flags, stages *require* them, glitch tiles open with them, all saved in `unlocked_areas` | `Quest.grants`, `QuestGiver.Stage`, `TileType` |
| Model/UI split | Only `isla.ui` touches JavaFX; the game draws through the `Renderer` interface, so it is testable without a window | `Renderer`, `JavaFxRenderer`, `GameSession` |
| Database | Same SQL on H2 and MySQL; DAO interfaces with in-memory and JDBC versions; one transaction per save | `isla.dao`, `db/schema.sql` |
| Hazards, death, score | The danger loop: static and shocks drain stamina, at 0 you die, deaths and mistakes lower the rank | `GameSession.walk`, `checkStamina`, `Score` |
| Testing | 381 tests; the playthrough test walks the map with pathfinding and real key presses | `BrokenSourceStoryTest` |

## Likely questions
- **Why multiple choice?** It keeps the focus on reading code and spotting the bug, and every wrong option has an explanation, so a mistake teaches something.
- **What happens if MySQL is down?** The game says so and uses the built-in H2 database, and the player can still save.
- **Can you get stuck?** No. Waking after death restores full stamina, every locked stone says what it needs, and the tests check that every part of the map opens in story order.
