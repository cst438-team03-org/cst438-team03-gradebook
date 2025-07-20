package com.cst438.controller;


import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnrollmentControllerUnitTest {

    @Autowired
    private WebTestClient webTestClient;


    @MockitoBean
    RegistrarServiceProxy registrarServiceProxy;

    @Test
    public void getEnrollmentsTest() throws Exception {
        // Login as ted (instructor)
        String email = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> loginResult = webTestClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        LoginDTO login = loginResult.getResponseBody();
        assertNotNull(login, "LoginDTO should not be null");
        String jwt = login.jwt();
        assertNotNull(jwt, "JWT token should not be null");

        // Navigate to /sections/1/enrollments
        EntityExchangeResult<EnrollmentDTO[]> enrollmentsResult = webTestClient.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class)
                .returnResult();

        EnrollmentDTO[] dtos = enrollmentsResult.getResponseBody();
        assertNotNull(dtos, "Enrollments response should not be null");
        assertNotEquals(0, dtos.length, "There should be at least one enrollment for section 1");

    }

    @Test
    public void updateEnrollmentGradeTest() throws Exception {
        // Login as Ted (instructor)
        String email = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> loginResult = webTestClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        LoginDTO login = loginResult.getResponseBody();
        assertNotNull(login, "LoginDTO should not be null");
        String jwt = login.jwt();
        assertNotNull(jwt, "JWT token should not be null");

        // Get enrollments for section 1 (Ted's section)
        EntityExchangeResult<EnrollmentDTO[]> enrollmentsResult = webTestClient.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class)
                .returnResult();

        EnrollmentDTO[] enrollments = enrollmentsResult.getResponseBody();
        assertNotNull(enrollments);
        assertTrue(enrollments.length > 0);

        // Find Sam's enrollment by studentId=2
        EnrollmentDTO samsEnrollment = null;
        for (EnrollmentDTO dto : enrollments) {
            if (dto.studentId() == 2) {
                samsEnrollment = dto;
            }
        }
        assertNotNull(samsEnrollment, "Sam's enrollment (id=2) should exist");
        assertEquals("B", samsEnrollment.grade(), "Sam's initial grade should be B");

        // Update Sam's grade to 'D'
        EnrollmentDTO updatedSam = new EnrollmentDTO(
                samsEnrollment.enrollmentId(), "D", samsEnrollment.studentId(),
                samsEnrollment.name(), samsEnrollment.email(),
                samsEnrollment.courseId(), samsEnrollment.title(),
                samsEnrollment.sectionId(), samsEnrollment.sectionNo(),
                samsEnrollment.building(), samsEnrollment.room(),
                samsEnrollment.times(), samsEnrollment.credits(),
                samsEnrollment.year(), samsEnrollment.semester()
        );

        webTestClient.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Collections.singletonList(updatedSam))
                .exchange()
                .expectStatus().isOk();

        // Verify grade was updated by fetching enrollments again
        EntityExchangeResult<EnrollmentDTO[]> enrollmentsAfterUpdate = webTestClient.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class)
                .returnResult();

        EnrollmentDTO[] updatedEnrollments = enrollmentsAfterUpdate.getResponseBody();
        EnrollmentDTO updatedSamEnrollment = null;
        for (EnrollmentDTO dto : updatedEnrollments) {
            if (dto.studentId() == 2) {
                updatedSamEnrollment = dto;
            }
        }
        assertNotNull(updatedSamEnrollment, "Sam's updated enrollment (id=2) should exist");
        assertEquals("D", updatedSamEnrollment.grade(), "Sam's grade should be updated to D");

        // Verify message sent to registrar
        org.mockito.Mockito.verify(registrarServiceProxy)
                .sendMessage(org.mockito.ArgumentMatchers.eq("finalGrade"), org.mockito.ArgumentMatchers.any(EnrollmentDTO.class));

        // Attempt to update Joe's enrollment (enrollmentId=101, studentId=4, sectionNo=2, which Ted does NOT teach)
        EnrollmentDTO joesEnrollment = new EnrollmentDTO(
                101, "F", 4, "joe", "joe@csumb.edu",
                "cst363", "Introduction to Database",
                2, 2, "30", "B420", "T H 3-5", 4, 2025, "Spring"
        );

        webTestClient.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Collections.singletonList(joesEnrollment))
                .exchange()
                .expectStatus().isBadRequest();
    }
}