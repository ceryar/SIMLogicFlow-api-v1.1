package com.simlogicflow.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.simlogicflow.dto.CourseDto;
import com.simlogicflow.model.Course;
import com.simlogicflow.model.Room;
import com.simlogicflow.model.Simulator;
import com.simlogicflow.repository.CourseRepository;
import com.simlogicflow.repository.RoomRepository;
import com.simlogicflow.repository.SimulatorRepository;
import com.simlogicflow.repository.UserRepository;
import com.simlogicflow.model.User;
import com.simlogicflow.model.ProCourse;
import com.simlogicflow.repository.ProCourseRepository;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SimulatorRepository simulatorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProCourseRepository proCourseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course createCourse(CourseDto dto) {
        if (courseRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Ya existe un curso con el nombre: " + dto.getName());
        }
        Course course = new Course();
        updateCourseFromDto(course, dto);
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, CourseDto dto) {
        Optional<Course> optionalCourse = courseRepository.findById(id);
        if (optionalCourse.isPresent()) {
            Course course = optionalCourse.get();

            if (!course.getName().equals(dto.getName()) && courseRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Ya existe un curso con el nombre: " + dto.getName());
            }

            updateCourseFromDto(course, dto);
            return courseRepository.save(course);
        }
        throw new RuntimeException("Course not found with id " + id);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    private void updateCourseFromDto(Course course, CourseDto dto) {
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setFecInicio(dto.getFecInicio());
        course.setFecFin(dto.getFecFin());
        course.setHoras(dto.getHoras());

        if (dto.getRoomIds() != null && !dto.getRoomIds().isEmpty()) {
            java.util.List<Room> rooms = roomRepository.findAllById(dto.getRoomIds());
            for (Room room : rooms) {
                if (room.getActive() != null && !room.getActive()) {
                    throw new RuntimeException(
                            "El aula '" + room.getName() + "' está inactiva y no se puede asignar al curso.");
                }
            }
            course.setRooms(new java.util.HashSet<>(rooms));
        }

        if (dto.getSimulatorId() != null) {
            Simulator simulator = simulatorRepository.findById(dto.getSimulatorId())
                    .orElseThrow(() -> new RuntimeException("Simulator not found with id " + dto.getSimulatorId()));
            course.setSimulator(simulator);
        }

        if (dto.getCoordinatorId() != null) {
            User coordinator = userRepository.findById(dto.getCoordinatorId())
                    .orElseThrow(() -> new RuntimeException("Coordinator not found with id " + dto.getCoordinatorId()));
            validateUserAvailability(coordinator, course);
            course.setCoordinator(coordinator);
        } else {
            course.setCoordinator(null);
        }

        if (dto.getPseudoPilotId() != null) {
            User pseudoPilot = userRepository.findById(dto.getPseudoPilotId())
                    .orElseThrow(() -> new RuntimeException("PseudoPilot not found with id " + dto.getPseudoPilotId()));
            validateUserAvailability(pseudoPilot, course);
            course.setPseudoPilot(pseudoPilot);
        } else {
            course.setPseudoPilot(null);
        }

        if (dto.getInstructorId() != null) {
            User instructor = userRepository.findById(dto.getInstructorId())
                    .orElseThrow(() -> new RuntimeException("Instructor not found with id " + dto.getInstructorId()));
            validateUserAvailability(instructor, course);
            course.setInstructor(instructor);
        } else {
            course.setInstructor(null);
        }
    }

    private void validateUserAvailability(User user, Course course) {
        if (user == null || course.getId() == null || course.getProCourses() == null
                || course.getProCourses().isEmpty()) {
            return;
        }

        for (ProCourse session : course.getProCourses()) {
            List<ProCourse> conflicts = proCourseRepository.findUserOverlappingSessions(
                    user.getId(), session.getFecha(), session.getHoraini(), session.getHorafin());

            for (ProCourse conflict : conflicts) {
                if (!conflict.getCourse().getId().equals(course.getId())) {
                    throw new com.simlogicflow.exceptions.ScheduleConflictException(
                            "Conflicto de disponibilidad: " + user.getFirstName() + " " + user.getLastname() +
                                    " ya tiene sesión en el curso \"" + conflict.getCourse().getName() +
                                    "\" el " + session.getFecha() + " de " + conflict.getHoraini() +
                                    " a " + conflict.getHorafin());
                }
            }
        }
    }
}
