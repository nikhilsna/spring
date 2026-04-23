package com.open.spring.mvc.capstone;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.open.spring.mvc.person.Person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "capstone_groups")
@Getter
@Setter
public class CapstoneGroups extends Submitter {
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "capstone_group_members", 
        joinColumns = @JoinColumn(name = "group_id"), 
        inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    @JsonIgnore
    private List<Person> members = new ArrayList<>();

    private String name;
    
    @Column(name = "project_title")
    private String projectTitle;
    
    @Column(columnDefinition = "text")
    private String description;
    
    @Column(name = "owner_id")
    private Long ownerId;

    public CapstoneGroups() {
    }

    public CapstoneGroups(String name, String projectTitle, String description, Long ownerId) {
        this.name = name;
        this.projectTitle = projectTitle;
        this.description = description;
        this.ownerId = ownerId;
    }

    public void addMember(Person person) {
        if (!this.members.contains(person)) {
            this.members.add(person);
        }
    }

    public void removeMember(Person person) {
        if (this.members.contains(person)) {
            this.members.remove(person);
        }
    }

    public boolean isOwner(Long userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    public boolean isMember(Long userId) {
        return this.members.stream().anyMatch(m -> m.getId().equals(userId));
    }
}