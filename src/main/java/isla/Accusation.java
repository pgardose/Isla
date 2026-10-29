package isla;

import java.util.List;

/**
 * The mystery's big question: who broke the Golden Idol? It works like a debug puzzle (pick one of several
 * answers) but the "options" are suspects and the "snippet" is the question.
 *
 * OOP note (INHERITANCE + POLYMORPHISM): an Accusation IS a DebugQuest, so the puzzle screen, saving and
 * turning in all work unchanged. It overrides the panel heading and the cost of a wrong answer: naming the
 * wrong person costs no stamina, the suspect's alibi (the per-option feedback) is the price.
 */
public class Accusation extends DebugQuest {

    public Accusation(int id, int giverNpcId, String description, int reward, String question,
                      List<String> suspects, int culpritIndex) {
        super(id, giverNpcId, description, reward, question, suspects, culpritIndex);
    }

    @Override
    public String getHeading() {
        return "WHO BROKE THE IDOL?";
    }

    @Override
    public int wrongAnswerStaminaCost() {
        return 0;
    }
}
