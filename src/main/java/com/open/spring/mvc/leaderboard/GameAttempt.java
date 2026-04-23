package com.open.spring.mvc.leaderboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "game_attempt")
public class GameAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false, unique = true, length = 64)
    private String attemptId;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "game_name", nullable = false, length = 128)
    private String gameName;

    @Column(name = "variable_name", nullable = false, length = 128)
    private String variableName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GameAttemptStatus status;

    @Column(name = "submitted_score")
    private Integer submittedScore;

    @Column(name = "score_submitted")
    private boolean scoreSubmitted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}