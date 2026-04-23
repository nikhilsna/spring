package com.open.spring.mvc.PauseMenu;

import com.open.spring.mvc.leaderboard.GameAttempt;
import com.open.spring.mvc.leaderboard.LeaderboardAntiCheatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * API Controller for Pause Menu Score Management
 */
@RestController
@RequestMapping("/api/pausemenu/score")
public class PauseMenuApiController {

    @Autowired
    private ScorePauseMenuRepo scoreRepository;

    @Autowired
    private LeaderboardAntiCheatService leaderboardAntiCheatService;

    /**
     * DTO for receiving score data from the frontend
     */
    @Data
    public static class ScorePauseMenuRequest {
        private String attemptId;
        private String gameName;
        private String variableName;
        private int score;
    }

    @Data
    public static class GameAttemptRequest {
        private String attemptId;
        private String gameName;
        private String variableName;
    }

    /**
     * Save a new score
     * POST /api/pausemenu/score/save
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ScorePauseMenuRequest request) {
        try {
            String username = requireUsername(userDetails);
            ScoreCounter saved = leaderboardAntiCheatService.submitScore(
                username,
                request.getAttemptId(),
                request.getGameName(),
                request.getVariableName(),
                request.getScore()
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

    /**
     * Get all scores
     * GET /api/pausemenu/score/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<ScoreCounter>> getAllScores() {
        List<ScoreCounter> scores = scoreRepository.findAll();
        return ResponseEntity.ok(scores);
    }

    /**
     * Start a new server-tracked attempt.
     * POST /api/pausemenu/score/start
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAttempt(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GameAttemptRequest request) {
        String username = requireUsername(userDetails);
        GameAttempt attempt = leaderboardAntiCheatService.startAttempt(username, request.getGameName(), request.getVariableName());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("attemptId", attempt.getAttemptId());
        response.put("status", attempt.getStatus().name());
        response.put("message", "Attempt started");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Record a progress event.
     * POST /api/pausemenu/score/progress
     */
    @PostMapping("/progress")
    public ResponseEntity<Map<String, Object>> recordProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GameAttemptRequest request) {
        String username = requireUsername(userDetails);
        GameAttempt attempt = leaderboardAntiCheatService.recordProgress(
            username,
            request.getAttemptId(),
            request.getGameName(),
            request.getVariableName()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("attemptId", attempt.getAttemptId());
        response.put("status", attempt.getStatus().name());
        response.put("message", "Progress recorded");
        return ResponseEntity.ok(response);
    }

    /**
     * Record completion before score submission.
     * POST /api/pausemenu/score/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> recordComplete(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GameAttemptRequest request) {
        String username = requireUsername(userDetails);
        GameAttempt attempt = leaderboardAntiCheatService.recordComplete(
            username,
            request.getAttemptId(),
            request.getGameName(),
            request.getVariableName()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("attemptId", attempt.getAttemptId());
        response.put("status", attempt.getStatus().name());
        response.put("message", "Completion recorded");
        return ResponseEntity.ok(response);
    }

    /**
     * Get scores for a specific user
     * GET /api/pausemenu/score/user/{user}
     */
    @GetMapping("/user/{user}")
    public ResponseEntity<List<ScoreCounter>> getScoresByUser(@PathVariable String user) {
        List<ScoreCounter> scores = scoreRepository.findByUser(user);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get a specific score by ID
     * GET /api/pausemenu/score/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getScoreById(@PathVariable Long id) {
        return scoreRepository.findById(id)
            .map(score -> ResponseEntity.ok((Object) score))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Score not found")));
    }

    /**
     * Delete a score
     * DELETE /api/pausemenu/score/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteScore(@PathVariable Long id) {
        if (scoreRepository.existsById(id)) {
            scoreRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Score deleted"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Score not found"));
    }

    /**
     * Alternate endpoint used by frontend
     * POST /api/gamer/score
     * Accepts JSON: { "score": number, "user": string? }
     * If user is missing, defaults to "guest"
     */
    @PostMapping(path = "/api/gamer/score", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> saveGamerScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> payload) {
        try {
            int score = 0;
            Object scoreObj = payload.get("score");
            if (scoreObj instanceof Number) {
                score = ((Number) scoreObj).intValue();
            }

            String gameName = (payload.get("gameName") instanceof String) ? (String) payload.get("gameName") : null;
            String variableName = (payload.get("variableName") instanceof String) ? (String) payload.get("variableName") : null;
            String attemptId = (payload.get("attemptId") instanceof String) ? (String) payload.get("attemptId") : null;
            String username = requireUsername(userDetails);

            ScoreCounter saved = leaderboardAntiCheatService.submitScore(username, attemptId, gameName, variableName, score);

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

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        return userDetails.getUsername();
    }
}
