package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.RegistrarServiceProxy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final RegistrarServiceProxy registrarService;

    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository,
            RegistrarServiceProxy registrarService) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.registrarService = registrarService;
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
        // logged-in instructor user for the given term.
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

        String instructorEmail = principal.getName();

        // verify that user is the instructor for the section
        Section section = sectionRepository.findBySectionNo(secNo);
        if (section == null || !section.getInstructorEmail().equals(instructorEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view assignments for this section.");
        }

        // Find the section by section number
        List<Assignment> assignments = assignmentRepository.findBySectionNo(secNo);

        // If no sections are found, return an empty list
        if (assignments.isEmpty()){
            return new ArrayList<>();
        }

        //  return list of assignments for the Section
        return assignments.stream()
                .map(a -> new AssignmentDTO(
                        a.getAssignmentId(),
                        a.getTitle(),
                        a.getDueDate().toString(),
                        a.getSection().getCourse().getCourseId(),
                        a.getSection().getSectionId(),
                        a.getSection().getSectionNo()))
                .toList();
    }


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

        //  user must be the instructor for the Section
        String email = principal.getName();
        Section section = sectionRepository.findBySectionNo(dto.secNo());
        if (section == null || !section.getInstructorEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create assignments for this section.");
        }

		//  check that assignment dueDate is between start date and 
		//  end date of the term
        if (dto.dueDate() == null ||
                section.getTerm().getStartDate().after(Date.valueOf(dto.dueDate())) ||
                section.getTerm().getEndDate().before(Date.valueOf(dto.dueDate()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be within the term's start and end dates.");
        }

		//  create and save an Assignment entity
        Assignment assignment = new Assignment();
        assignment.setTitle(dto.title());
        assignment.setDueDate(Date.valueOf(dto.dueDate()));
        assignment.setSection(section);
        assignmentRepository.save(assignment);

        //  return AssignmentDTO with database generated primary key
        AssignmentDTO result = new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                section.getSectionNo()
        );
        registrarService.sendMessage("addAssignment", result);
        return result;
    }


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        //  user must be instructor of the Section
        String email = principal.getName();
        Section section = sectionRepository.findBySectionNo(dto.secNo());
        if (section == null || !section.getInstructorEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update assignments for this section.");
        }

        //  update Assignment Entity. only title and dueDate fields can be changed.
        Assignment assignment = assignmentRepository.findById(dto.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        assignment.setTitle(dto.title());
        if (dto.dueDate() != null) {
            Date dueDate = Date.valueOf(dto.dueDate());
            if (section.getTerm().getStartDate().after(dueDate) || section.getTerm().getEndDate().before(dueDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be within the term's start and end dates.");
            }
            assignment.setDueDate(dueDate);
        }
        assignmentRepository.save(assignment);
        //  return AssignmentDTO with database generated primary key
        AssignmentDTO result = new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                section.getSectionNo()
        );
        registrarService.sendMessage("updateAssignment", result);
        return result;
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // verify that user is the instructor of the section
        String email = principal.getName();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        Section section = assignment.getSection();
        if (section == null || !section.getInstructorEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete assignments for this section.");
        }

        // delete the Assignment entity
        assignmentRepository.deleteById(assignmentId);
        registrarService.sendMessage("deleteAssignment", assignmentId);
    }

    // student lists their assignments/grades ordered by due date
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        String email = principal.getName();
        // Assignments are already sorted by due date in the database query
        List<Assignment> assignments = assignmentRepository.findByStudentEmailAndYearAndSemester(email,year, semester);
        if (assignments.isEmpty()) {
            return new ArrayList<>();
        }

        //  Return AssignmentStudentDTOs with scores of a
        //  Grade entity exists.
        //  If assignment has not been graded, return a null score.
        List<AssignmentStudentDTO> assignmentStudentDTOs = new ArrayList<>();
        for (Assignment a : assignments) {
            // Check if a grade exists for this assignment
            // If no grade exists, it will be null
            Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(email, a.getAssignmentId());
            Integer score = null;
            if (grade != null){
                score = grade.getScore();
            }
            AssignmentStudentDTO dto = new AssignmentStudentDTO(
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getDueDate(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSectionId(),
                    score
            );
            assignmentStudentDTOs.add(dto);
        }

        return assignmentStudentDTOs;
    }
}
