package com.open.spring.mvc.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameEventLogRepository extends JpaRepository<GameEventLog, Long> {

    List<GameEventLog> findByAttemptIdOrderByIdAsc(String attemptId);
}