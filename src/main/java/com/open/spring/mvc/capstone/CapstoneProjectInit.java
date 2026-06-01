package com.open.spring.mvc.capstone;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Example JPA entity representing a tag that can be attached to capstone projects.
 * Demonstrates a ManyToMany relationship (unidirectional) with `CapstoneProject`.
 */
@Entity
@Table(name = "capstone_tags")
public class CapstoneProjectInit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(name = "capstone_tag_projects",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = @JoinColumn(name = "capstone_project_id"))
    private Set<CapstoneProject> projects = new HashSet<>();

    public CapstoneProjectInit() {}

    public CapstoneProjectInit(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<CapstoneProject> getProjects() { return projects; }
    public void setProjects(Set<CapstoneProject> projects) { this.projects = projects; }
}
