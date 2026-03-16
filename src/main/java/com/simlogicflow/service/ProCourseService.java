package com.simlogicflow.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        throw new RuntimeException("Programación de curso no encontrada con ID " + id);
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
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado con ID " + dto.getCourseId()));
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

            // 2. Conflicto de Personal y Estudiantes (Identidad-céntrico)
            Set<Long> currentParticipants = getAllParticipantIds(currentCourse);
            Set<Long> otherParticipants = getAllParticipantIds(otherCourse);

            for (Long userId : currentParticipants) {
                if (otherParticipants.contains(userId)) {
                    User user = currentCourse.getUsers().stream()
                            .filter(u -> u.getId().equals(userId))
                            .findFirst()
                            .orElseGet(() -> findUserInRoles(currentCourse, userId));

                    String userName = (user != null) ? user.getFirstName() + " " + user.getLastname() : "Un usuario";
                    throw new ScheduleConflictException(
                            "Conflicto de HORARIO: " + userName + " ya tiene una sesión asignada en el curso \""
                                    + otherCourse.getName()
                                    + "\" en este horario (mismo usuario, diferentes roles posibles).");
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

    private Set<Long> getAllParticipantIds(Course course) {
        Set<Long> ids = new HashSet<>();
        if (course.getUsers() != null) {
            ids.addAll(course.getUsers().stream().map(User::getId).collect(Collectors.toSet()));
        }
        if (course.getInstructor() != null)
            ids.add(course.getInstructor().getId());
        if (course.getCoordinator() != null)
            ids.add(course.getCoordinator().getId());
        if (course.getPseudoPilot() != null)
            ids.add(course.getPseudoPilot().getId());
        return ids;
    }

    private User findUserInRoles(Course course, Long userId) {
        if (course.getInstructor() != null && course.getInstructor().getId().equals(userId))
            return course.getInstructor();
        if (course.getCoordinator() != null && course.getCoordinator().getId().equals(userId))
            return course.getCoordinator();
        if (course.getPseudoPilot() != null && course.getPseudoPilot().getId().equals(userId))
            return course.getPseudoPilot();
        return null;
    }
}
