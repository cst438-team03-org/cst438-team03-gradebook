package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class GradeController {
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;

    public GradeController (
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
    }
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // Check that the Section of the assignment belongs to the 
		// logged in instructor 
        // return a list of GradeDTOs containing student scores for an assignment
        // if a Grade entity does not exist, then create the Grade entity 
		// with a null score and return the gradeId. 

        // Find assignment or return 400 Bad Request if not found
		Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment not found");
        }

        // Check if section of the assignment belongs to the logged in instructor
        String instructorEmail = assignment.getSection().getInstructorEmail();
        if (!instructorEmail.equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        List<GradeDTO> result = new ArrayList<>();
        // Get all enrollments in the section
        List<Enrollment> enrollments = assignment.getSection().getEnrollments();

        for (Enrollment enrollment : enrollments) {
            String studentEmail = enrollment.getStudent().getEmail();
            // Find grade by student email and assignment Id
            Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(studentEmail, assignmentId);

            // If grade does not exist yet, create one with null score
            if (grade == null) {
                grade = new Grade();
                grade.setAssignment(assignment);
                grade.setEnrollment(enrollment);
                grade.setScore(null);
                grade = gradeRepository.save(grade);
            }
            // Create DTO to return
            GradeDTO dto = new GradeDTO(
                grade.getGradeId(),
                grade.getEnrollment().getStudent().getName(),
                grade.getEnrollment().getStudent().getEmail(),
                assignment.getTitle(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSectionId(),
                grade.getScore()
            );
            result.add(dto);
        }


        return result;
    }


    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(@Valid @RequestBody List<GradeDTO> dtoList, Principal principal) {
		// for each GradeDTO
		// check that the logged in instructor is the owner of the section
        // update the assignment score
        for(GradeDTO dto : dtoList) {
            // Find the grade by gradeId or return 400 if not found
            Grade grade = gradeRepository.findById(dto.gradeId()).orElse(null);
            if (grade == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade not found for id " + dto.gradeId());
            }
            
            // check that the logged in instructor is the owner of the section
            String instructorEmail = grade.getAssignment().getSection().getInstructorEmail();
            if (!instructorEmail.equals(principal.getName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to modify grade " + dto.gradeId());
            }
            
            // Update the score and save
            grade.setScore(dto.score());
            gradeRepository.save(grade);

        }
        
    }
}
