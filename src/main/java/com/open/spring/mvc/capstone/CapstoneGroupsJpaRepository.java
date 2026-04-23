package com.open.spring.mvc.capstone;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapstoneGroupsJpaRepository extends JpaRepository<CapstoneGroups, Long> {
    
    Optional<CapstoneGroups> findById(Long id);
    
    List<CapstoneGroups> findAll();
    List<CapstoneGroups> findAllByOrderByNameAsc();

    @Query("SELECT DISTINCT g FROM CapstoneGroups g LEFT JOIN FETCH g.members ORDER BY g.id")
    List<CapstoneGroups> findAllWithMembers();
    
    @Query("SELECT g FROM CapstoneGroups g JOIN g.members p WHERE p.id = :personId")
    List<CapstoneGroups> findGroupsByPersonId(@Param("personId") Long personId);

    @Query("SELECT DISTINCT g FROM CapstoneGroups g LEFT JOIN FETCH g.members WHERE g.id = :groupId")
    Optional<CapstoneGroups> findByIdWithMembers(@Param("groupId") Long groupId);
    
    @Query(value = "SELECT p.id, p.uid, p.name, p.email FROM capstone_group_members cgm " +
                   "JOIN person p ON cgm.person_id = p.id " +
                   "WHERE cgm.group_id = :groupId " +
                   "ORDER BY p.id", nativeQuery = true)
    List<Object[]> findGroupMembersRaw(@Param("groupId") Long groupId);

    Optional<CapstoneGroups> findByName(String name);

    @Query("SELECT g FROM CapstoneGroups g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY g.name")
    List<CapstoneGroups> searchByName(@Param("searchTerm") String searchTerm);

    @Query("SELECT g FROM CapstoneGroups g WHERE g.ownerId = :ownerId")
    List<CapstoneGroups> findByOwnerId(@Param("ownerId") Long ownerId);
}