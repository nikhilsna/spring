package com.open.spring.mvc.leaderboard;

import java.util.List;

public final class GameFlowPolicy {

    private static final List<GameEvent> REQUIRED_FLOW = List.of(
        GameEvent.LOGIN,
        GameEvent.START_GAME,
        GameEvent.PROGRESS,
        GameEvent.COMPLETE
    );

    private GameFlowPolicy() {
    }

    public static boolean canSubmitScore(List<GameEvent> observedEvents) {
        if (observedEvents == null || observedEvents.isEmpty()) {
            return false;
        }

        int index = 0;
        for (GameEvent event : observedEvents) {
            if (event == GameEvent.UPDATE_SCORE) {
                return false;
            }

            if (index < REQUIRED_FLOW.size() && event == REQUIRED_FLOW.get(index)) {
                index++;
            }
        }

        return index == REQUIRED_FLOW.size();
    }
}