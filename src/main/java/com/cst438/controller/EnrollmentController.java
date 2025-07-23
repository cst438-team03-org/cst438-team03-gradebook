package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.RegistrarServiceProxy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final RegistrarServiceProxy registrar;

    public EnrollmentController (
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            RegistrarServiceProxy registrar
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.registrar = registrar;
    }


    // instructor gets student enrollments with grades for a section
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/sections/{sectionNo}/enrollments")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo, Principal principal ) {

        Section section = sectionRepository.findById(sectionNo).orElse(null);
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found.");
        }
        // check that the sectionNo belongs to the logged in instructor.
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not instructor for this section.");
        }
		// use the EnrollmentRepository findEnrollmentsBySectionNoOrderByStudentName
		// to get a list of Enrollments for the given sectionNo.
		// Return a list of EnrollmentDTOs

        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);
        // Convert to DTOs
        return enrollments.stream().map(e -> new EnrollmentDTO(
                e.getEnrollmentId(),
                e.getGrade(),
                e.getStudent().getId(),
                e.getStudent().getName(),
                e.getStudent().getEmail(),
                section.getCourse().getCourseId(),
                section.getCourse().getTitle(),
                section.getSectionId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                section.getCourse().getCredits(),
                section.getTerm().getYear(),
                section.getTerm().getSemester()
        )).collect(Collectors.toList());
    }

    // instructor updates enrollment grades
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @PutMapping("/enrollments")
    public void updateEnrollmentGrade(@Valid @RequestBody List<EnrollmentDTO> dtoList, Principal principal) {

        String instructorEmail = principal.getName();
        // for each EnrollmentDTO
        for (EnrollmentDTO dto : dtoList) {
            Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId()).orElse(null);

            if (enrollment == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment not found: " + dto.enrollmentId());
            }
            //    check that logged in user is instructor for the section
            Section section = enrollment.getSection();
            if (section == null || !section.getInstructorEmail().equals(instructorEmail)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not authorized to update grades for this section.");
            }

            //    update the enrollment grade
            enrollment.setGrade(dto.grade());
            enrollmentRepository.save(enrollment);

            //    send message to Registrar service for grade update
            Map<String, Object> message = new HashMap<>();
            message.put("enrollmentId", dto.enrollmentId());
            message.put("grade", dto.grade());
            registrar.sendMessage("updateEnrollment", message);
        }
       
    }
}
