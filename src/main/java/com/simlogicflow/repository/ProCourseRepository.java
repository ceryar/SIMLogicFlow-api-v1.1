package com.simlogicflow.repository;

import com.simlogicflow.model.ProCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProCourseRepository extends JpaRepository<ProCourse, Long> {

        @org.springframework.data.jpa.repository.Query("SELECT p FROM ProCourse p JOIN FETCH p.course c JOIN FETCH c.simulator s LEFT JOIN FETCH c.instructor i LEFT JOIN FETCH c.pseudoPilot pp LEFT JOIN FETCH c.coordinator co")
        java.util.List<ProCourse> findAll();

        @org.springframework.data.jpa.repository.Query("SELECT p FROM ProCourse p " +
                        "WHERE p.course.simulator.id = :simulatorId " +
                        "AND p.fecha = :fecha " +
                        "AND p.horaini < :horafin AND p.horafin > :horaini " +
                        "AND (:excludeId IS NULL OR p.id != :excludeId)")
        java.util.List<ProCourse> findOverlappingProCourses(
                        @org.springframework.data.repository.query.Param("simulatorId") Long simulatorId,
                        @org.springframework.data.repository.query.Param("fecha") java.time.LocalDate fecha,
                        @org.springframework.data.repository.query.Param("horaini") java.time.LocalTime horaini,
                        @org.springframework.data.repository.query.Param("horafin") java.time.LocalTime horafin,
                        @org.springframework.data.repository.query.Param("excludeId") Long excludeId);

        @org.springframework.data.jpa.repository.Query("SELECT p FROM ProCourse p " +
                        "WHERE p.fecha = :fecha " +
                        "AND p.horaini < :horafin AND p.horafin > :horaini " +
                        "AND (:excludeId IS NULL OR p.id != :excludeId)")
        java.util.List<ProCourse> findTimeOverlappingSessions(
                        @org.springframework.data.repository.query.Param("fecha") java.time.LocalDate fecha,
                        @org.springframework.data.repository.query.Param("horaini") java.time.LocalTime horaini,
                        @org.springframework.data.repository.query.Param("horafin") java.time.LocalTime horafin,
                        @org.springframework.data.repository.query.Param("excludeId") Long excludeId);

        @org.springframework.data.jpa.repository.Query("SELECT p FROM ProCourse p " +
                        "JOIN p.course c " +
                        "LEFT JOIN c.users u " +
                        "WHERE (u.id = :userId OR c.instructor.id = :userId OR c.pseudoPilot.id = :userId OR c.coordinator.id = :userId) "
                        +
                        "AND p.fecha = :fecha " +
                        "AND p.horaini < :horafin AND p.horafin > :horaini")
        java.util.List<ProCourse> findUserOverlappingSessions(
                        @org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("fecha") java.time.LocalDate fecha,
                        @org.springframework.data.repository.query.Param("horaini") java.time.LocalTime horaini,
                        @org.springframework.data.repository.query.Param("horafin") java.time.LocalTime horafin);
}
