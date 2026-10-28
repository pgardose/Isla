package isla.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WalkAnimatorTest {

    @Test
    void standingStillShowsTheFirstFrame() {
        WalkAnimator w = new WalkAnimator();
        for (int frame = 0; frame < 200; frame++) {
            assertEquals(0, w.frameFor(5, 5, frame, 2));
        }
    }

    @Test
    void movingMakesTheSpriteBobBetweenBothFrames() {
        WalkAnimator w = new WalkAnimator();
        w.frameFor(5, 5, 0, 2);
        Set<Integer> seen = new HashSet<>();
        int col = 5;
        for (int frame = 1; frame < 60; frame++) {
            if (frame % 7 == 0) {
                col++; // keeps walking
            }
            seen.add(w.frameFor(col, 5, frame, 2));
        }
        assertEquals(Set.of(0, 1), seen);
    }

    @Test
    void stopsBobbingShortlyAfterTheLastStep() {
        WalkAnimator w = new WalkAnimator();
        w.frameFor(5, 5, 0, 2);
        w.frameFor(6, 5, 1, 2);
        assertEquals(0, w.frameFor(6, 5, 1 + WalkAnimator.WALK_HOLD_TICKS + 5, 2));
    }

    @Test
    void aSingleFrameSpriteNeverBobs() {
        WalkAnimator w = new WalkAnimator();
        w.frameFor(5, 5, 0, 1);
        for (int frame = 1; frame < 40; frame++) {
            assertEquals(0, w.frameFor(5 + frame, 5, frame, 1));
        }
    }

    @Test
    void returnedFrameIsAlwaysInRange() {
        WalkAnimator w = new WalkAnimator();
        for (int frame = 0; frame < 100; frame++) {
            int f = w.frameFor(frame, frame, frame, 3);
            assertTrue(f >= 0 && f < 3);
        }
    }
}
