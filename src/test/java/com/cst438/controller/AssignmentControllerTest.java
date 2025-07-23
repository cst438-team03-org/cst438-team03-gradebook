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
    public void getSectionsForInstructor(){
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
        validAssignment = createAssignment(validAssignment, ted);

        // check that the sendMessage from gradebook to registrar was called as expected
        verify(registrarService).sendMessage(eq("addAssignment"), any());

        // check that the new assignment exists in the database
        Assignment a = assignmentRepository.findById(validAssignment.id()).orElse(null);
        assertNotNull(a, "Assignment was added but not found in the database");
    }

    @Test
    void updateAssignment() {
        // Login as instructor Ted
        String email = "ted@csumb.edu";
        String password = "ted2025";
        String ted = login(email, password);

        // Create a valid assignment for section 1
        AssignmentDTO validAssignment = new AssignmentDTO(0, "Sample Assignment", "2025-10-01", "cst489", 1, 1);
        validAssignment = createAssignment(validAssignment, ted);

        int assignmentId = validAssignment.id();
        assertNotEquals(0, assignmentId, "Assignment ID should not be 0 after creation.");

        // Attempt to update an assignment with a nonexistent section
        AssignmentDTO badSectionAssignment = new AssignmentDTO(assignmentId, "Updated Assignment", "2025-10-15", "cst489", 1, 2);
        // This should fail because section 2 does not exist
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(badSectionAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        // Attempt to update a nonexistent assignment
        AssignmentDTO badAssignment = new AssignmentDTO(9999, "Updated Assignment", "2025-10-15", "cst489", 1, 1);
        // This should fail because the assignment does not exist
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(badAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        // Attempt to update an assignment with a due date outside the term's start and end dates
        AssignmentDTO badDateAssignment = new AssignmentDTO(assignmentId, "Updated Assignment", "1900-01-01", "cst489", 1, 1);
        // This should fail because the due date is outside the term's start and end dates
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(badDateAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        // Update the assignment with valid data
        AssignmentDTO updatedAssignment = new AssignmentDTO(assignmentId, "Updated Assignment", "2025-09-13", "cst489", 1, 1);
        EntityExchangeResult<AssignmentDTO> updatedResult = client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(ted))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedAssignment)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();
        updatedAssignment = updatedResult.getResponseBody();
        assertNotNull(updatedAssignment);

        // check that the sendMessage from gradebook to registrar was called as expected
        verify(registrarService).sendMessage(eq("updateAssignment"), any());

        Assignment updatedEntity = assignmentRepository.findById(updatedAssignment.id()).orElse(null);
        assertNotNull(updatedEntity, "Assignment was updated but not found in the database");

        // Print out the updated assignment for debugging
//        System.out.println("Updated Assignment ID: " + updatedEntity.getAssignmentId() + ", Title: " + updatedEntity.getTitle() + ", Due Date: " + updatedEntity.getDueDate());
    }

    @Test
    void deleteAssignment() {
        // Login as instructor Ted
        String email = "ted@csumb.edu";
        String password = "ted2025";
        String ted = login(email, password);
        // Create a valid assignment for section 1
        AssignmentDTO validAssignment = new AssignmentDTO(0, "Assignment to Delete", "2025-10-01", "cst489", 1, 1);
        validAssignment = createAssignment(validAssignment, ted);

        // Attempt to delete an invalid assignment
        int nonexistentAssignmentId = 9999;
        client.delete().uri("/assignments/" + nonexistentAssignmentId)
                .headers(headers -> headers.setBearerAuth(ted))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        // Delete the valid assignment
        EntityExchangeResult<AssignmentDTO> deleteAssignment = client.delete().uri("/assignments/" + validAssignment.id())
                .headers(headers -> headers.setBearerAuth(ted))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();
        // check that the sendMessage from gradebook to registrar was called as expected
        verify(registrarService).sendMessage(eq("deleteAssignment"), any());
        // Check that the assignment was deleted
        Assignment deletedAssignment = assignmentRepository.findById(validAssignment.id()).orElse(null);
        assertNull(deletedAssignment, "Assignment was deleted but still found in the database");
    }

    @Test
    void getStudentAssignments() {
    }

    // Helper methods
    /**
     * Login as a user and return the JWT token.
     * This method assumes that the user exists in the database.
     * @param email the email of the user
     * @param password the password of the user
     * @return the JWT token for the logged-in user
     */
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

    /**Create a new AssignmentDTO and insert it into the database
        This method assumes that the section exists and the due date is valid.
        * @param assignmentDTO the AssignmentDTO to create
        * @param jwt the JWT token for authentication
        * @return the created AssignmentDTO with the database generated primary key
     **/
    private AssignmentDTO createAssignment(AssignmentDTO assignmentDTO, String jwt) {
        EntityExchangeResult<AssignmentDTO> result = client.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();
        return result.getResponseBody();
    }
}