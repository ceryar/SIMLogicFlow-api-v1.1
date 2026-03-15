package com.simlogicflow.repository;

import com.simlogicflow.model.ProCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProCourseRepository extends JpaRepository<ProCourse, Long> {

        @org.springframework.data.jpa.repository.Query("SELECT p FROM ProCourse p " +
                        "WHERE p.fecha = :fecha " +
                        "AND p.horaini < :horafin AND p.horafin > :horaini " +
                        "AND (:excludeId IS NULL OR p.id != :excludeId)")
        java.util.List<ProCourse> findTimeOverlappingSessions(
                        @org.springframework.data.repository.query.Param("fecha") java.time.LocalDate fecha,
                        @org.springframework.data.repository.query.Param("horaini") java.time.LocalTime horaini,
                        @org.springframework.data.repository.query.Param("horafin") java.time.LocalTime horafin,
                        @org.springframework.data.repository.query.Param("excludeId") Long excludeId);
}
