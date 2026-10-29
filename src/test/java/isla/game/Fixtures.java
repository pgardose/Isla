package isla.game;

import isla.Quest;
import isla.QuestGiver;
import java.util.List;

/**
 * Test-only NPCs from the game's first version (Elder Kai's coconut and Golden Idol fetch quests, a one-stage
 * Nia). The engine tests use them as small, stable fixtures for walking, talking, quest stages and saving;
 * the real story lives in SeedData and is tested by BrokenSourceStoryTest.
 */
public final class Fixtures {

    private Fixtures() {
    }

    public static Quest elderQuest() {
        return new Quest(1, 1, "Bring a coconut to Elder Kai", 25, "Coconut");
    }

    public static Quest idolQuest() {
        return new Quest(3, 1, "Bring the Golden Idol to Elder Kai", 100, "Golden Idol");
    }

    public static QuestGiver elder() {
        return elder(elderQuest());
    }

    public static QuestGiver elder(Quest quest) {
        return new QuestGiver(1, 20, 9, "elder_sprite",
                "Could you bring me a coconut? My legs are not what they were.", quest);
    }

    public static QuestGiver elderWithIdol(Quest coconut, Quest idol) {
        return new QuestGiver(1, 20, 9, "elder_sprite", List.of(
                new QuestGiver.Stage(coconut,
                        "Could you bring me a coconut? My legs are not what they were.",
                        "Bring Elder Kai a coconut"),
                new QuestGiver.Stage(idol,
                        "That coconut was lovely! One last wish: a Golden Idol. Yara sells one deep in the jungle.",
                        "Find the Golden Idol in the jungle")),
                "Thank you, traveler. The island is at peace because of you.",
                "Done: Elder Kai has his Golden Idol").endsTheGame();
    }

    public static QuestGiver coder() {
        return coder(SeedData.debugQuest());
    }

    public static QuestGiver coder(Quest quest) {
        return new QuestGiver(2, 24, 14, "coder_sprite", List.of(new QuestGiver.Stage(quest,
                "My code is broken and I cannot see why. Can you debug it?",
                "Help Coder Nia fix her code")), null, "Done: Coder Nia's code is fixed");
    }
}
