package com.open.spring.mvc.leaderboard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameFlowPolicyTest {

    @Test
    void allowsRequiredEventSequence() {
        assertTrue(GameFlowPolicy.canSubmitScore(List.of(
            GameEvent.LOGIN,
            GameEvent.START_GAME,
            GameEvent.PROGRESS,
            GameEvent.COMPLETE
        )));
    }

    @Test
    void rejectsUpdateScoreWithoutCompleteFlow() {
        assertFalse(GameFlowPolicy.canSubmitScore(List.of(
            GameEvent.LOGIN,
            GameEvent.START_GAME,
            GameEvent.UPDATE_SCORE
        )));
    }
}