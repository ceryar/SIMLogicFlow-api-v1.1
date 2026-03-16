package com.simlogicflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
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
@Table(name = "pro_courses")
public class ProCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horaini", nullable = false)
    private LocalTime horaini;

    @Column(name = "horafin", nullable = false)
    private LocalTime horafin;

    @Column(name = "horas", nullable = false)
    private Integer horas;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public java.time.LocalTime getHoraini() {
        return horaini;
    }

    public java.time.LocalTime getHorafin() {
        return horafin;
    }

    public java.time.LocalDate getFecha() {
        return fecha;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }
}
