package com.open.spring.mvc.leaderboard;

import com.open.spring.mvc.PauseMenu.ScoreCounter;
import com.open.spring.mvc.PauseMenu.ScorePauseMenuRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LeaderboardAntiCheatService {

    private final GameAttemptRepository gameAttemptRepository;
    private final GameEventLogRepository gameEventLogRepository;
    private final ScorePauseMenuRepo scorePauseMenuRepo;

    public LeaderboardAntiCheatService(GameAttemptRepository gameAttemptRepository,
                                       GameEventLogRepository gameEventLogRepository,
                                       ScorePauseMenuRepo scorePauseMenuRepo) {
        this.gameAttemptRepository = gameAttemptRepository;
        this.gameEventLogRepository = gameEventLogRepository;
        this.scorePauseMenuRepo = scorePauseMenuRepo;
    }

    public GameAttempt startAttempt(String username, String gameName, String variableName) {
        String normalizedGameName = normalize(gameName, "unknown");
        String normalizedVariableName = normalize(variableName, "unknown");
        Instant now = Instant.now();

        GameAttempt attempt = new GameAttempt();
        attempt.setAttemptId(UUID.randomUUID().toString());
        attempt.setUsername(username);
        attempt.setGameName(normalizedGameName);
        attempt.setVariableName(normalizedVariableName);
        attempt.setStatus(GameAttemptStatus.STARTED);
        attempt.setScoreSubmitted(false);
        attempt.setSubmittedScore(null);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);

        gameAttemptRepository.save(attempt);
        logEvent(attempt, GameEvent.LOGIN, now);
        logEvent(attempt, GameEvent.START_GAME, now);
        return attempt;
    }

    public GameAttempt recordProgress(String username, String attemptId, String gameName, String variableName) {
        GameAttempt attempt = loadAttempt(username, attemptId, gameName, variableName);
        if (attempt.getStatus() == GameAttemptStatus.LOCKED) {
            throw forbidden("Attempt is locked. Start a new attempt and complete it again.");
        }

        Instant now = Instant.now();
        attempt.setStatus(GameAttemptStatus.IN_PROGRESS);
        attempt.setUpdatedAt(now);
        gameAttemptRepository.save(attempt);
        logEvent(attempt, GameEvent.PROGRESS, now);
        return attempt;
    }

    public GameAttempt recordComplete(String username, String attemptId, String gameName, String variableName) {
        GameAttempt attempt = loadAttempt(username, attemptId, gameName, variableName);
        if (attempt.getStatus() == GameAttemptStatus.LOCKED) {
            throw forbidden("Attempt is locked. Start a new attempt before completing again.");
        }

        Instant now = Instant.now();
        attempt.setStatus(GameAttemptStatus.COMPLETE);
        attempt.setUpdatedAt(now);
        gameAttemptRepository.save(attempt);
        logEvent(attempt, GameEvent.COMPLETE, now);
        return attempt;
    }

    public ScoreCounter submitScore(String username, String attemptId, String gameName, String variableName, int score) {
        GameAttempt attempt = loadAttempt(username, attemptId, gameName, variableName);
        List<GameEvent> events = gameEventLogRepository.findByAttemptIdOrderByIdAsc(attempt.getAttemptId())
            .stream()
            .map(GameEventLog::getEventType)
            .toList();

        if (!GameFlowPolicy.canSubmitScore(events)) {
            throw forbidden("Invalid event sequence. login -> start_game -> progress -> complete is required before score submission.");
        }

        if (attempt.isScoreSubmitted() || attempt.getStatus() == GameAttemptStatus.LOCKED) {
            throw forbidden("This attempt is already locked. Start a new attempt and complete it again.");
        }

        String normalizedGameName = normalize(gameName, attempt.getGameName());
        String normalizedVariableName = normalize(variableName, attempt.getVariableName());
        Instant now = Instant.now();

        ScoreCounter savedScore = upsertHighestScore(username, normalizedGameName, normalizedVariableName, score);

        attempt.setSubmittedScore(score);
        attempt.setScoreSubmitted(true);
        attempt.setStatus(GameAttemptStatus.LOCKED);
        attempt.setUpdatedAt(now);
        gameAttemptRepository.save(attempt);
        logEvent(attempt, GameEvent.UPDATE_SCORE, now);

        return savedScore;
    }

    private ScoreCounter upsertHighestScore(String username, String gameName, String variableName, int score) {
        Optional<ScoreCounter> existing = scorePauseMenuRepo.findFirstByUserAndGameNameAndVariableNameOrderByIdDesc(username, gameName, variableName);

        if (existing.isPresent()) {
            ScoreCounter current = existing.get();
            if (score > current.getScore()) {
                current.setScore(score);
                current.setGameName(gameName);
                current.setVariableName(variableName);
                return scorePauseMenuRepo.save(current);
            }
            return current;
        }

        ScoreCounter newScore = new ScoreCounter();
        newScore.setUser(username);
        newScore.setScore(score);
        newScore.setGameName(gameName);
        newScore.setVariableName(variableName);
        return scorePauseMenuRepo.save(newScore);
    }

    private GameAttempt loadAttempt(String username, String attemptId, String gameName, String variableName) {
        String normalizedGameName = normalize(gameName, "unknown");
        String normalizedVariableName = normalize(variableName, "unknown");

        if (attemptId != null && !attemptId.isBlank()) {
            return gameAttemptRepository.findByAttemptIdAndUsernameAndGameNameAndVariableName(
                    attemptId.trim(), username, normalizedGameName, normalizedVariableName)
                .orElseThrow(() -> forbidden("Attempt not found for this user and game."));
        }

        return gameAttemptRepository.findFirstByUsernameAndGameNameAndVariableNameOrderByCreatedAtDesc(
                username, normalizedGameName, normalizedVariableName)
            .orElseThrow(() -> forbidden("Missing attemptId. Start a new game attempt first."));
    }

    private void logEvent(GameAttempt attempt, GameEvent eventType, Instant createdAt) {
        GameEventLog log = new GameEventLog();
        log.setAttemptId(attempt.getAttemptId());
        log.setUsername(attempt.getUsername());
        log.setGameName(attempt.getGameName());
        log.setEventType(eventType);
        log.setCreatedAt(createdAt);
        gameEventLogRepository.save(log);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}