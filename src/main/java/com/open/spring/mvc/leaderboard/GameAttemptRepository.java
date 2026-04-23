package com.open.spring.mvc.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameAttemptRepository extends JpaRepository<GameAttempt, Long> {

    Optional<GameAttempt> findByAttemptIdAndUsernameAndGameNameAndVariableName(String attemptId, String username, String gameName, String variableName);

    List<GameAttempt> findByUsernameAndGameNameAndVariableNameOrderByCreatedAtDesc(String username, String gameName, String variableName);

    Optional<GameAttempt> findFirstByUsernameAndGameNameAndVariableNameOrderByCreatedAtDesc(String username, String gameName, String variableName);
}