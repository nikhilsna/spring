package com.open.spring.mvc.PauseMenu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

/**
 * Repository for ScoreCounter entity
 */
@Repository
public interface ScorePauseMenuRepo extends JpaRepository<ScoreCounter, Long> {

    /**
     * Find all scores for a specific user
     */
    List<ScoreCounter> findByUser(String user);

    /**
     * Find the latest score for a user/game combination.
     */
    Optional<ScoreCounter> findFirstByUserAndGameNameAndVariableNameOrderByIdDesc(String user, String gameName, String variableName);

    /**
     * Find the latest score for a user/game combination when the variable name is not important.
     */
    Optional<ScoreCounter> findFirstByUserAndGameNameOrderByIdDesc(String user, String gameName);
}
