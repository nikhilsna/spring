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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/api/capstone-projects")
public class CapstoneProjectApiController {

    @Autowired
    private CapstoneProjectJpaRepository capstoneProjectRepository;

    // ===== DTOs =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapstoneProjectDto {
        private Long id;
        private String title;
        private String subtitle;
        private String description;
        private String about;
        private String courseCode;
        private String status;
        private String imageUrl;
        private String pageUrl;
        private String frontendUrl;
        private String backendUrl;
        private String alt;
        private List<String> teamMembers;
        private List<String> tech;
        private List<String> keyPoints;
        private List<String> impact;
        private Long groupId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCapstoneProjectDto {
        private String title;
        private String subtitle;
        private String description;
        private String about;
        private String courseCode;
        private String status;
        private String imageUrl;
        private String pageUrl;
        private String frontendUrl;
        private String backendUrl;
        private String alt;
        private List<String> teamMembers;
        private List<String> tech;
        private List<String> keyPoints;
        private List<String> impact;
        private Long groupId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCapstoneProjectDto {
        private String title;
        private String subtitle;
        private String description;
        private String about;
        private String courseCode;
        private String status;
        private String imageUrl;
        private String pageUrl;
        private String frontendUrl;
        private String backendUrl;
        private String alt;
        private List<String> teamMembers;
        private List<String> tech;
        private List<String> keyPoints;
        private List<String> impact;
        private Long groupId;
    }

    // ===== Helper Methods =====
    private Map<String, Object> buildProjectResponse(CapstoneProject project) {
        Map<String, Object> projectMap = new LinkedHashMap<>();
        projectMap.put("id", project.getId());
        projectMap.put("title", project.getTitle());
        projectMap.put("subtitle", project.getSubtitle());
        projectMap.put("description", project.getDescription());
        projectMap.put("about", project.getAbout());
        projectMap.put("courseCode", project.getCourseCode());
        projectMap.put("status", project.getStatus());
        projectMap.put("imageUrl", project.getImageUrl());
        projectMap.put("pageUrl", project.getPageUrl());
        projectMap.put("frontendUrl", project.getFrontendUrl());
        projectMap.put("backendUrl", project.getBackendUrl());
        projectMap.put("alt", project.getAlt());
        projectMap.put("teamMembers", project.getTeamMembers());
        projectMap.put("tech", project.getTech());
        projectMap.put("keyPoints", project.getKeyPoints());
        projectMap.put("impact", project.getImpact());
        projectMap.put("groupId", project.getGroupId());
        return projectMap;
    }

    // ===== GET Operations =====

    /**
     * GET /api/capstone-projects - Get all capstone projects
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllCapstoneProjects(
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        List<CapstoneProject> projects;
        
        if (search != null && !search.trim().isEmpty()) {
            projects = capstoneProjectRepository.searchByTitle(search);
        } else if (courseCode != null && !courseCode.trim().isEmpty()) {
            projects = capstoneProjectRepository.findByCourseCode(courseCode);
        } else if (status != null && !status.trim().isEmpty()) {
            projects = capstoneProjectRepository.findByStatus(status);
        } else {
            projects = capstoneProjectRepository.findAll();
        }
        
        List<Map<String, Object>> response = new ArrayList<>();
        for (CapstoneProject project : projects) {
            response.add(buildProjectResponse(project));
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/capstone-projects/{id} - Get a specific capstone project
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCapstoneProject(@PathVariable Long id) {
        Optional<CapstoneProject> projectOpt = capstoneProjectRepository.findById(id);
        
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(buildProjectResponse(projectOpt.get()));
    }

    /**
     * GET /api/capstone-projects/group/{groupId} - Get projects by group
     */
    @GetMapping("/group/{groupId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getCapstoneProjectsByGroup(@PathVariable Long groupId) {
        List<CapstoneProject> projects = capstoneProjectRepository.findByGroupId(groupId);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (CapstoneProject project : projects) {
            response.add(buildProjectResponse(project));
        }
        
        return ResponseEntity.ok(response);
    }

    // ===== POST Operations =====

    /**
     * POST /api/capstone-projects - Create a new capstone project
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createCapstoneProject(@RequestBody CreateCapstoneProjectDto dto) {
        // Validate required fields
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Project title is required"));
        }

        // Check if project with same name exists
        if (capstoneProjectRepository.findByTitle(dto.getTitle().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A project with this title already exists"));
        }

        CapstoneProject project = new CapstoneProject();
        project.setTitle(dto.getTitle().trim());
        project.setSubtitle(dto.getSubtitle());
        project.setDescription(dto.getDescription());
        project.setAbout(dto.getAbout());
        project.setCourseCode(dto.getCourseCode() != null ? dto.getCourseCode() : "CSA");
        project.setStatus(dto.getStatus() != null ? dto.getStatus() : "In Development");
        project.setImageUrl(dto.getImageUrl());
        project.setPageUrl(dto.getPageUrl());
        project.setFrontendUrl(dto.getFrontendUrl());
        project.setBackendUrl(dto.getBackendUrl());
        project.setAlt(dto.getAlt());
        project.setGroupId(dto.getGroupId());
        
        if (dto.getTeamMembers() != null) {
            project.setTeamMembers(new ArrayList<>(dto.getTeamMembers()));
        }
        if (dto.getTech() != null) {
            project.setTech(new ArrayList<>(dto.getTech()));
        }
        if (dto.getKeyPoints() != null) {
            project.setKeyPoints(new ArrayList<>(dto.getKeyPoints()));
        }
        if (dto.getImpact() != null) {
            project.setImpact(new ArrayList<>(dto.getImpact()));
        }

        CapstoneProject savedProject = capstoneProjectRepository.save(project);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(buildProjectResponse(savedProject));
    }

    // ===== PUT Operations =====

    /**
     * PUT /api/capstone-projects/{id} - Update a capstone project
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateCapstoneProject(@PathVariable Long id, @RequestBody UpdateCapstoneProjectDto dto) {
        Optional<CapstoneProject> projectOpt = capstoneProjectRepository.findById(id);
        
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CapstoneProject project = projectOpt.get();

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            // Check if title is taken by another project
            Optional<CapstoneProject> existingProject = capstoneProjectRepository.findByTitle(dto.getTitle().trim());
            if (existingProject.isPresent() && !existingProject.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "A project with this title already exists"));
            }
            project.setTitle(dto.getTitle().trim());
        }

        if (dto.getSubtitle() != null) project.setSubtitle(dto.getSubtitle());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getAbout() != null) project.setAbout(dto.getAbout());
        if (dto.getCourseCode() != null) project.setCourseCode(dto.getCourseCode());
        if (dto.getStatus() != null) project.setStatus(dto.getStatus());
        if (dto.getImageUrl() != null) project.setImageUrl(dto.getImageUrl());
        if (dto.getPageUrl() != null) project.setPageUrl(dto.getPageUrl());
        if (dto.getFrontendUrl() != null) project.setFrontendUrl(dto.getFrontendUrl());
        if (dto.getBackendUrl() != null) project.setBackendUrl(dto.getBackendUrl());
        if (dto.getAlt() != null) project.setAlt(dto.getAlt());
        if (dto.getGroupId() != null) project.setGroupId(dto.getGroupId());
        
        if (dto.getTeamMembers() != null) project.setTeamMembers(new ArrayList<>(dto.getTeamMembers()));
        if (dto.getTech() != null) project.setTech(new ArrayList<>(dto.getTech()));
        if (dto.getKeyPoints() != null) project.setKeyPoints(new ArrayList<>(dto.getKeyPoints()));
        if (dto.getImpact() != null) project.setImpact(new ArrayList<>(dto.getImpact()));

        CapstoneProject savedProject = capstoneProjectRepository.save(project);
        
        return ResponseEntity.ok(buildProjectResponse(savedProject));
    }

    // ===== DELETE Operations =====

    /**
     * DELETE /api/capstone-projects/{id} - Delete a capstone project
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteCapstoneProject(@PathVariable Long id) {
        Optional<CapstoneProject> projectOpt = capstoneProjectRepository.findById(id);
        
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        capstoneProjectRepository.delete(projectOpt.get());
        
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }
}