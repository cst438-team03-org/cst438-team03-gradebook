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
        // print out the sections for debugging
        System.out.println("Sections for Fall 2025:");
        for (SectionDTO sdto : sectionList) {
            System.out.println("Section ID: " + sdto.secId() + ", Course ID: " + sdto.courseId() + ", Instructor Email: " + sdto.instructorEmail());
        }

        assertTrue(sectionList.length >= 1, "There should be at least 1 section for the Fall 2025 semester.");
    }

    @Test
    void getAssignments() {
    }

    @Test
    void createAssignment() {
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