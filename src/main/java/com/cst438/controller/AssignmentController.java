package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    // get Sections for an instructor
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year ,
            @RequestParam("semester") String semester,
            Principal principal) {

        String instructorEmail = principal.getName();
        // Find the instructor's name based on their email
        User instructor = userRepository.findByEmail(instructorEmail);

        // return the Sections that have instructorEmail for the
        // logged in instructor user for the given term.
        List<Section> sections = sectionRepository.findByInstructorEmailAndYearAndSemester(instructorEmail, year, semester);

        // If no sections are found, return an empty list
        if (sections.isEmpty()){
            return new ArrayList<>();
        }

        // Uses a stream to map each Section entity to a SectionDTO
        return sections.stream()
                .map(s -> new SectionDTO(
                        s.getSectionNo(),
                        s.getTerm().getYear(),
                        s.getTerm().getSemester(),
                        s.getCourse().getCourseId(),
                        s.getCourse().getTitle(),
                        s.getSectionId(),
                        s.getBuilding(),
                        s.getRoom(),
                        s.getTimes(),
                        instructor.getName(),
                        s.getInstructorEmail()))
                .toList();
    }

    // instructor lists assignments for a section.
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {
        // TODO: List assignments for a section
        // verify that user is the instructor for the section
        //  return list of assignments for the Section
        return null;
    }


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {
        // TODO: Create an assignment

        //  user must be the instructor for the Section
		//  check that assignment dueDate is between start date and 
		//  end date of the term
		//  create and save an Assignment entity
        //  return AssignmentDTO with database generated primary key
        return null;
    }


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        // TODO: Update an assignment

        //  update Assignment Entity.  only title and dueDate fields can be changed.
        //  user must be instructor of the Section
        return null;
    }


    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // TODO: Delete an assignment

        // verify that user is the instructor of the section
        // delete the Assignment entity
        
    }

    // student lists their assignments/grades  ordered by due date
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {
        // TODO: List assignments for a student

        //  return AssignmentStudentDTOs with scores of a 
		//  Grade entity exists.
		//  hint: use the GradeRepository findByStudentEmailAndAssignmentId
        //  If assignment has not been graded, return a null score.
        return null;
    }
}
