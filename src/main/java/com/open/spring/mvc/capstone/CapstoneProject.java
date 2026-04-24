package com.open.spring.mvc.capstone;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "capstone_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapstoneProject extends Submitter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capstone_team_members", joinColumns = @JoinColumn(name = "capstone_id"))
    @Column(name = "member")
    private List<String> teamMembers = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capstone_tech", joinColumns = @JoinColumn(name = "capstone_id"))
    @Column(name = "tech")
    private List<String> tech = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capstone_key_points", joinColumns = @JoinColumn(name = "capstone_id"))
    @Column(name = "key_point")
    private List<String> keyPoints = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capstone_impact", joinColumns = @JoinColumn(name = "capstone_id"))
    @Column(name = "impact")
    private List<String> impact = new ArrayList<>();
    
    // For linking to capstone group
    @Column(name = "group_id")
    private Long groupId;
    
    public CapstoneProject(String title, String subtitle, String description, String courseCode) {
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.courseCode = courseCode;
    }
}