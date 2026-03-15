package com.simlogicflow.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.simlogicflow.dto.ProCourseDto;
import com.simlogicflow.model.Course;
import com.simlogicflow.model.ProCourse;
import com.simlogicflow.model.Room;
import com.simlogicflow.model.User;
import com.simlogicflow.model.Maintenance;
import com.simlogicflow.repository.CourseRepository;
import com.simlogicflow.repository.ProCourseRepository;
import com.simlogicflow.repository.MaintenanceRepository;
import com.simlogicflow.exceptions.ScheduleConflictException;

@Service
public class ProCourseService {

    @Autowired
    private ProCourseRepository proCourseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    public List<ProCourse> getAllProCourses() {
        return proCourseRepository.findAll();
    }

    public ProCourse createProCourse(ProCourseDto dto) {
        ProCourse proCourse = new ProCourse();
        updateProCourseFromDto(proCourse, dto);
        return proCourseRepository.save(proCourse);
    }

    public ProCourse updateProCourse(Long id, ProCourseDto dto) {
        Optional<ProCourse> optionalProCourse = proCourseRepository.findById(id);
        if (optionalProCourse.isPresent()) {
            ProCourse proCourse = optionalProCourse.get();
            updateProCourseFromDto(proCourse, dto);
            return proCourseRepository.save(proCourse);
        }
        throw new RuntimeException("ProCourse not found with id " + id);
    }

    public void deleteProCourse(Long id) {
        proCourseRepository.deleteById(id);
    }

    private void updateProCourseFromDto(ProCourse proCourse, ProCourseDto dto) {
        proCourse.setHoraini(dto.getHoraini());
        proCourse.setHorafin(dto.getHorafin());
        proCourse.setHoras(dto.getHoras());
        proCourse.setFecha(dto.getFecha());

        if (dto.getCourseId() != null) {
            Course course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found with id " + dto.getCourseId()));
            proCourse.setCourse(course);
        }

        validateNoOverlap(proCourse);
    }

    private void validateNoOverlap(ProCourse session) {
        Course currentCourse = session.getCourse();
        if (currentCourse == null)
            return;

        List<ProCourse> overlaps = proCourseRepository.findTimeOverlappingSessions(
                session.getFecha(), session.getHoraini(), session.getHorafin(), session.getId());

        for (ProCourse other : overlaps) {
            Course otherCourse = other.getCourse();
            if (otherCourse == null)
                continue;

            // 1. Conflicto de Sala (excepto Pseudopilotos)
            Set<Room> currentRooms = currentCourse.getRooms();
            Set<Room> otherRooms = otherCourse.getRooms();

            if (currentRooms != null && otherRooms != null) {
                for (Room room : currentRooms) {
                    String name = room.getName();
                    if (name != null && name.toUpperCase().contains("PSEUDO"))
                        continue;

                    for (Room oRoom : otherRooms) {
                        if (room.getId().equals(oRoom.getId())) {
                            throw new ScheduleConflictException(
                                    "Conflicto de AULA: La " + room.getName() + " ya está ocupada por el curso \""
                                            + otherCourse.getName() + "\" en este horario.");
                        }
                    }
                }
            }

            // 2. Conflicto de Personal
            // Instructor
            User curInstr = currentCourse.getInstructor();
            User othInstr = otherCourse.getInstructor();
            if (curInstr != null && othInstr != null && curInstr.getId().equals(othInstr.getId())) {
                throw new ScheduleConflictException(
                        "Conflicto de INSTRUCTOR: " + curInstr.getFirstName()
                                + " ya tiene una sesión asignada en el curso \"" + otherCourse.getName()
                                + "\" en este horario.");
            }

            // Coordinador
            User curCoord = currentCourse.getCoordinator();
            User othCoord = otherCourse.getCoordinator();
            if (curCoord != null && othCoord != null && curCoord.getId().equals(othCoord.getId())) {
                throw new ScheduleConflictException(
                        "Conflicto de COORDINADOR: " + curCoord.getFirstName()
                                + " ya tiene una sesión asignada en el curso \"" + otherCourse.getName()
                                + "\" en este horario.");
            }

            // 3. Estudiantes
            Set<User> curUsers = currentCourse.getUsers();
            Set<User> othUsers = otherCourse.getUsers();
            if (curUsers != null && othUsers != null) {
                for (User student : curUsers) {
                    for (User oStudent : othUsers) {
                        if (student.getId().equals(oStudent.getId())) {
                            String fullName = student.getFirstName()
                                    + (student.getLastname() != null ? " " + student.getLastname() : "");
                            throw new ScheduleConflictException(
                                    "Conflicto de ESTUDIANTE: " + fullName + " ya tiene clase en el curso \""
                                            + otherCourse.getName() + "\" en este horario.");
                        }
                    }
                }
            }
        }

        // 4. Mantenimiento (Nivel de Simulador completo)
        if (currentCourse.getSimulator() != null) {
            List<Maintenance> maintenanceOverlaps = maintenanceRepository
                    .findOverlappingMaintenances(currentCourse.getSimulator().getId(), session.getFecha(),
                            session.getFecha(), null);
            if (!maintenanceOverlaps.isEmpty()) {
                throw new ScheduleConflictException(
                        "El simulador se encuentra en mantenimiento en la fecha especificada.");
            }
        }
    }
}
