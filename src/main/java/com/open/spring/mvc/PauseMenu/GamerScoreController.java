package com.open.spring.mvc.PauseMenu;

import com.open.spring.mvc.leaderboard.LeaderboardAntiCheatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight controller to handle gamer score submissions from the frontend.
 * Exposed at /api/gamer/score (public, no auth required).
 */
@RestController
@RequestMapping("/api/pausemenu")
public class GamerScoreController {

    @Autowired
    private LeaderboardAntiCheatService leaderboardAntiCheatService;

    public static class GamerScoreRequest {
        public String attemptId;
        public Integer score;
        public String gameName;
        public String variableName;
    }

    @PostMapping("/score")
    public ResponseEntity<Map<String, Object>> saveGamerScore(@AuthenticationPrincipal UserDetails userDetails, @RequestBody GamerScoreRequest payload) {
        try {
            if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Authenticated user required"));
            }

            int score = payload != null && payload.score != null ? payload.score : 0;
            String gameName = payload != null ? payload.gameName : null;
            String variableName = payload != null ? payload.variableName : null;
            String attemptId = payload != null ? payload.attemptId : null;

            ScoreCounter saved = leaderboardAntiCheatService.submitScore(
                userDetails.getUsername(),
                attemptId,
                gameName,
                variableName,
                score
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("message", "Score saved successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error saving score: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
