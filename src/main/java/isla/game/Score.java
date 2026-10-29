package isla.game;

/**
 * The final score: start at 1000, lose points for every wrong answer and every death. A perfect run (no mistakes,
 * no deaths) is rank S. The ending shows it, and the best rank is remembered on this computer, so there is a reason
 * to play again and do better.
 *
 * OOP note (ENCAPSULATION): the scoring rules live in one immutable value; nothing else knows the numbers.
 */
public record Score(int wrongAnswers, int deaths) {

    public static final int START = 1000;
    public static final int WRONG_ANSWER_PENALTY = 40;
    public static final int DEATH_PENALTY = 150;

    public Score {
        if (wrongAnswers < 0 || deaths < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
    }

    public int points() {
        return Math.max(0, START - wrongAnswers * WRONG_ANSWER_PENALTY - deaths * DEATH_PENALTY);
    }

    /** S, A, B, C or D. */
    public String rank() {
        return rankFor(points(), wrongAnswers == 0 && deaths == 0);
    }

    public String title() {
        return titleFor(rank());
    }

    /** The rank for a number of points (S needs a flawless run, i.e. the full 1000). */
    public static String rankFor(int points, boolean flawless) {
        if (flawless && points == START) {
            return "S";
        }
        if (points >= 850) {
            return "A";
        }
        if (points >= 650) {
            return "B";
        }
        if (points >= 400) {
            return "C";
        }
        return "D";
    }

    public static String titleFor(String rank) {
        return switch (rank) {
            case "S" -> "Master Debugger";
            case "A" -> "Sharp-Eyed Coder";
            case "B" -> "Steady Fixer";
            case "C" -> "Lucky Guesser";
            default -> "Glitch Magnet";
        };
    }
}
