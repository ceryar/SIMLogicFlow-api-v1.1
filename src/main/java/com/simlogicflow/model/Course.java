package com.simlogicflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "fec_inicio", nullable = false)
    private java.time.LocalDate fecInicio;

    @Column(name = "fec_fin", nullable = false)
    private java.time.LocalDate fecFin;

    @Column(name = "horas")
    private Integer horas;

    @ManyToMany
    @JoinTable(name = "course_rooms", joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "room_id"))
    private Set<Room> rooms;

    @ManyToOne
    @JoinColumn(name = "simulator_id", nullable = false)
    private Simulator simulator;

    @JsonIgnore
    @ManyToMany(mappedBy = "courses")
    private Set<User> users;

    @ManyToOne
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @ManyToOne
    @JoinColumn(name = "pseudopilot_id")
    private User pseudoPilot;

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private User instructor;

    @JsonIgnore
    @OneToMany(mappedBy = "course", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProCourse> proCourses;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public java.time.LocalDate getFecInicio() {
        return fecInicio;
    }

    public java.time.LocalDate getFecFin() {
        return fecFin;
    }

    public Integer getHoras() {
        return horas;
    }

    public java.util.Set<Room> getRooms() {
        return rooms;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public java.util.Set<User> getUsers() {
        return users;
    }

    public User getCoordinator() {
        return coordinator;
    }

    public User getPseudoPilot() {
        return pseudoPilot;
    }

    public User getInstructor() {
        return instructor;
    }

    public java.util.List<ProCourse> getProCourses() {
        return proCourses;
    }
}
