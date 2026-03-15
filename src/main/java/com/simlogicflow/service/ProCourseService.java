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

        validateNoOverlap(proCourse, proCourse.getId());
    }

    private void validateNoOverlap(ProCourse session, Long excludeId) {
        Course currentCourse = session.getCourse();
        if (currentCourse == null)
            return;

        List<ProCourse> overlaps = proCourseRepository.findTimeOverlappingSessions(
                session.getFecha(), session.getHoraini(), session.getHorafin(), excludeId);

        for (ProCourse other : overlaps) {
            Course otherCourse = other.getCourse();
            if (otherCourse == null)
                continue;

            // 1. Room Conflict (except Pseudo rooms)
            Set<Room> currentRooms = currentCourse.getRooms();
            Set<Room> otherRooms = otherCourse.getRooms();

            if (currentRooms != null && otherRooms != null) {
                for (Room room : currentRooms) {
                    String roomName = (room.getName() != null) ? room.getName().toUpperCase() : "";
                    if (roomName.contains("PSEUDO")) {
                        continue;
                    }
                    for (Room oRoom : otherRooms) {
                        if (room.getId().equals(oRoom.getId())) {
                            throw new ScheduleConflictException(
                                    "Conflicto de AULA: La " + room.getName() + " ya está ocupada por el curso \""
                                            + otherCourse.getName() + "\" en este horario.");
                        }
                    }
                }
            }

            // 2. Personnel Conflict
            // Instructor
            User currentInstr = currentCourse.getInstructor();
            User otherInstr = otherCourse.getInstructor();
            if (currentInstr != null && otherInstr != null && currentInstr.getId().equals(otherInstr.getId())) {
                throw new ScheduleConflictException(
                        "Conflicto de INSTRUCTOR: " + currentInstr.getFirstName()
                                + " ya tiene una sesión asignada en el curso \"" + otherCourse.getName()
                                + "\" en este horario.");
            }

            // Coordinator
            User currentCoord = currentCourse.getCoordinator();
            User otherCoord = otherCourse.getCoordinator();
            if (currentCoord != null && otherCoord != null && currentCoord.getId().equals(otherCoord.getId())) {
                throw new ScheduleConflictException(
                        "Conflicto de COORDINADOR: " + currentCoord.getFirstName()
                                + " ya tiene una sesión asignada en el curso \"" + otherCourse.getName()
                                + "\" en este horario.");
            }

            // 3. Student Conflict
            Set<User> currentUsers = currentCourse.getUsers();
            Set<User> otherUsers = otherCourse.getUsers();
            if (currentUsers != null && otherUsers != null) {
                for (User student : currentUsers) {
                    for (User oStudent : otherUsers) {
                        if (student.getId().equals(oStudent.getId())) {
                            String name = student.getFirstName()
                                    + (student.getLastname() != null ? " " + student.getLastname() : "");
                            throw new ScheduleConflictException(
                                    "Conflicto de ESTUDIANTE: " + name + " ya tiene clase en el curso \""
                                            + otherCourse.getName() + "\" en este horario.");
                        }
                    }
                }
            }
        }

        // 4. Maintenance (Simulator level)
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
