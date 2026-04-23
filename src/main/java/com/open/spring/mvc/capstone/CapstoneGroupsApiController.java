package com.open.spring.mvc.capstone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/api/capstone-groups")
public class CapstoneGroupsApiController {

    @Autowired
    private CapstoneGroupsJpaRepository capstoneGroupsRepository;

    @Autowired
    private PersonJpaRepository personRepository;

    // ===== DTOs =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapstoneGroupCreateDto {
        private String name;
        private String projectTitle;
        private String description;
        private Long ownerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapstoneGroupUpdateDto {
        private String name;
        private String projectTitle;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinGroupDto {
        private Long userId;
    }

    // ===== Helper Methods =====
    private Map<String, Object> buildGroupResponse(CapstoneGroups group) {
        Map<String, Object> groupMap = new LinkedHashMap<>();
        groupMap.put("id", group.getId());
        groupMap.put("name", group.getName());
        groupMap.put("projectTitle", group.getProjectTitle());
        groupMap.put("description", group.getDescription());
        groupMap.put("ownerId", group.getOwnerId());

        List<Map<String, Object>> membersList = new ArrayList<>();
        List<Object[]> memberRows = capstoneGroupsRepository.findGroupMembersRaw(group.getId());

        for (Object[] row : memberRows) {
            Map<String, Object> member = new LinkedHashMap<>();
            member.put("id", ((Number) row[0]).longValue());
            member.put("uid", (String) row[1]);
            member.put("name", (String) row[2]);
            member.put("email", (String) row[3]);
            membersList.add(member);
        }

        groupMap.put("members", membersList);
        return groupMap;
    }

    // ===== GET Operations =====

    /**
     * GET /api/capstone-groups - Get all capstone groups with their members
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllCapstoneGroups() {
        List<CapstoneGroups> groups = capstoneGroupsRepository.findAllWithMembers();
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (CapstoneGroups group : groups) {
            response.add(buildGroupResponse(group));
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/capstone-groups/{id} - Get a specific capstone group
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCapstoneGroup(@PathVariable Long id) {
        Optional<CapstoneGroups> groupOpt = capstoneGroupsRepository.findByIdWithMembers(id);
        
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(buildGroupResponse(groupOpt.get()));
    }

    /**
     * GET /api/capstone-groups/user/{userId} - Get groups for a specific user
     */
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getUserCapstoneGroups(@PathVariable Long userId) {
        List<CapstoneGroups> groups = capstoneGroupsRepository.findGroupsByPersonId(userId);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (CapstoneGroups group : groups) {
            response.add(buildGroupResponse(group));
        }
        
        return ResponseEntity.ok(response);
    }

    // ===== POST Operations =====

    /**
     * POST /api/capstone-groups - Create a new capstone group
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createCapstoneGroup(@RequestBody CapstoneGroupCreateDto dto) {
        // Validate required fields
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Group name is required"));
        }

        // Check if group name already exists
        if (capstoneGroupsRepository.findByName(dto.getName().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A group with this name already exists"));
        }

        CapstoneGroups group = new CapstoneGroups();
        group.setName(dto.getName().trim());
        group.setProjectTitle(dto.getProjectTitle());
        group.setDescription(dto.getDescription());
        group.setOwnerId(dto.getOwnerId());

        // Add owner as first member if ownerId is provided
        if (dto.getOwnerId() != null) {
            personRepository.findById(dto.getOwnerId()).ifPresent(group::addMember);
        }

        CapstoneGroups savedGroup = capstoneGroupsRepository.save(group);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(buildGroupResponse(savedGroup));
    }

    /**
     * POST /api/capstone-groups/{id}/join - Join a capstone group
     */
    @PostMapping("/{id}/join")
    @Transactional
    public ResponseEntity<Map<String, Object>> joinCapstoneGroup(@PathVariable Long id, @RequestBody JoinGroupDto dto) {
        Optional<CapstoneGroups> groupOpt = capstoneGroupsRepository.findById(id);
        
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CapstoneGroups group = groupOpt.get();
        
        // Check if user is already a member
        if (group.isMember(dto.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You are already a member of this group"));
        }

        // Add user to group
        Optional<Person> personOpt = personRepository.findById(dto.getUserId());
        if (personOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        group.addMember(personOpt.get());
        capstoneGroupsRepository.save(group);
        
        return ResponseEntity.ok(buildGroupResponse(group));
    }

    // ===== PUT Operations =====

    /**
     * PUT /api/capstone-groups/{id} - Update a capstone group
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateCapstoneGroup(@PathVariable Long id, @RequestBody CapstoneGroupUpdateDto dto) {
        Optional<CapstoneGroups> groupOpt = capstoneGroupsRepository.findById(id);
        
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CapstoneGroups group = groupOpt.get();

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            // Check if name is taken by another group
            Optional<CapstoneGroups> existingGroup = capstoneGroupsRepository.findByName(dto.getName().trim());
            if (existingGroup.isPresent() && !existingGroup.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "A group with this name already exists"));
            }
            group.setName(dto.getName().trim());
        }

        if (dto.getProjectTitle() != null) {
            group.setProjectTitle(dto.getProjectTitle());
        }

        if (dto.getDescription() != null) {
            group.setDescription(dto.getDescription());
        }

        CapstoneGroups savedGroup = capstoneGroupsRepository.save(group);
        
        return ResponseEntity.ok(buildGroupResponse(savedGroup));
    }

    // ===== DELETE Operations =====

    /**
     * DELETE /api/capstone-groups/{id} - Delete a capstone group
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteCapstoneGroup(@PathVariable Long id) {
        Optional<CapstoneGroups> groupOpt = capstoneGroupsRepository.findById(id);
        
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        capstoneGroupsRepository.delete(groupOpt.get());
        
        return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
    }

    /**
     * POST /api/capstone-groups/{id}/leave - Leave a capstone group
     */
    @PostMapping("/{id}/leave")
    @Transactional
    public ResponseEntity<Map<String, Object>> leaveCapstoneGroup(@PathVariable Long id, @RequestBody JoinGroupDto dto) {
        Optional<CapstoneGroups> groupOpt = capstoneGroupsRepository.findByIdWithMembers(id);
        
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CapstoneGroups group = groupOpt.get();
        
        // Check if user is a member
        if (!group.isMember(dto.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You are not a member of this group"));
        }

        // Check if user is the owner (owner cannot leave their own group)
        if (group.isOwner(dto.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Group owner cannot leave. Transfer ownership or delete the group instead."));
        }

        // Remove user from group
        personRepository.findById(dto.getUserId()).ifPresent(group::removeMember);
        capstoneGroupsRepository.save(group);
        
        return ResponseEntity.ok(buildGroupResponse(group));
    }
}