package com.open.spring.mvc.capstone;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CapstoneProjectJpaRepository extends JpaRepository<CapstoneProject, Long> {
    
    Optional<CapstoneProject> findById(Long id);
    
    List<CapstoneProject> findAll();
    List<CapstoneProject> findAllByOrderByTitleAsc();

    Optional<CapstoneProject> findByTitle(String title);

    List<CapstoneProject> findByCourseCode(String courseCode);
    
    List<CapstoneProject> findByGroupId(Long groupId);

    @Query("SELECT c FROM CapstoneProject c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<CapstoneProject> searchByTitle(@Param("searchTerm") String searchTerm);

    @Query("SELECT c FROM CapstoneProject c WHERE c.status = :status")
    List<CapstoneProject> findByStatus(@Param("status") String status);
}