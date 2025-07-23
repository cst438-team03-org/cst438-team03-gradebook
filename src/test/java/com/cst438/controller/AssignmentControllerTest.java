package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.Date;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssignmentControllerTest {

    @Autowired
    private WebTestClient client;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SectionRepository sectionRepository;

    // default behavior for a Mock bean
    // return 0 or null for a method that returns a value
    // for method that returns void, the mock method records the call but does nothing
    @MockitoBean
    RegistrarServiceProxy registrarService;

    @Test
    public void getSectionsForInstructor() throws Exception {
        // Login as instructor Ted
        String email = "ted@csumb.edu";
        String password = "ted2025";
        String ted = login(email, password);

        // Verify that the currently logged-in instructor can see their sections.
        EntityExchangeResult<SectionDTO[]> sections = client.get().uri("/sections?year=2025&semester=Fall")
                .headers(headers -> headers.setBearerAuth(ted))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SectionDTO[].class).returnResult();
        SectionDTO[] sectionList = sections.getResponseBody();
        assertNotNull(sectionList);
//        // print out the sections for debugging
//        System.out.println("Sections for Fall 2025:");
//        for (SectionDTO sdto : sectionList) {
//            System.out.println("Section ID: " + sdto.secId() + ", Course ID: " + sdto.courseId() + ", Instructor Email: " + sdto.instructorEmail());
//        }

        assertTrue(sectionList.length >= 1, "There should be at least 1 section for the Fall 2025 semester.");
    }

    @Test
    void getAssignments() {
        // Login as instructor Ted
        String email = "ted@csumb.edu";
        String password = "ted2025";
        String ted = login(email, password);

        // Save a new assignment to the assignmentRepository for secNo 1 for testing
        Section section = sectionRepository.findBySectionNo(1);
        assertNotNull(section, "Section with secNo 1 should exist for testing.");
        Assignment a = new Assignment();
        a.setSection(section);
        a.setTitle("Test Assignment");
        // set due date as today for testing
        a.setDueDate(Date.valueOf(java.time.LocalDate.now()));
        assignmentRepository.save(a);

        // Verify that the returned assignments for section 1 include the test assignment
        EntityExchangeResult<AssignmentDTO[]> assignments = client.get().uri("/sections/1/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO[].class).returnResult();

        // Assert that the size of the returned list is at least 1
        AssignmentDTO[] assignmentList = assignments.getResponseBody();
        assertNotNull(assignmentList);
        assertTrue(assignmentList.length >=1, "There should be at least 1 assignment for section 1.");

//        // Print out the assignments for debugging
//        for (AssignmentDTO adto : assignmentList) {
//            System.out.println("Assignment ID: " + adto.secId() + ", Title: " + adto.title() + ", Due Date: " + adto.dueDate());
//        }
    }

    @Test
    void createAssignment() {
        // Login as instructor Ted
        String email = "ted@csumb.edu";
        String password = "ted2025";
        String ted = login(email, password);

        // Attempt to create an assignment for a nonexistent section
        AssignmentDTO badSectionAssignment = new AssignmentDTO(0, "New Assignment", "2025-10-01", "cst489", 1, 2);
        // This should fail because section 2 does not exist
        client.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(badSectionAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        // Attempt to create an assignment with a due date outside the term's start and end dates
        AssignmentDTO badDateAssignment = new AssignmentDTO(0, "New Assignment", "1900-01-01", "cst489", 1, 1);
        // This should fail because the due date is outside the term's start and end dates
        client.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(badDateAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        // Create a valid assignment for section 1
        AssignmentDTO validAssignment = new AssignmentDTO(0, "New Assignment", "2025-10-01", "cst489", 1, 1);
        EntityExchangeResult<AssignmentDTO> createdAssignment = client.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();

        // check that the sendMessage from gradebook to registrar was called as expected
        verify(registrarService).sendMessage(eq("addAssignment"), any());

        // check that the new assignment exists in the database
        Assignment a = assignmentRepository.findById(createdAssignment.getResponseBody().id()).orElse(null);
        assertNotNull(a, "Assignment was added but not found in the database");
    }

    @Test
    void updateAssignment() {
    }

    @Test
    void deleteAssignment() {
    }

    @Test
    void getStudentAssignments() {
    }

    private String login(String email, String password) {
        EntityExchangeResult<LoginDTO> login =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        String jwt = login.getResponseBody().jwt();
        assertNotNull(jwt, "JWT token should not be null after login.");
        System.out.println("Login successful for " + email);
        return jwt;
    }
}