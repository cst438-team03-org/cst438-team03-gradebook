package com.cst438.controller;

import com.cst438.domain.EnrollmentRepository;
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

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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
    //TODO COMPLETE THIS
        //Login as ted
        //check that logged in user is instructor for the section
        //      Check that sams grade is 'B'
        //      Update sams grade to be 'D'
        //      Check that sams grade is now 'D'
        //      example json
//        {
//            "enrollmentId": 0,
//                "grade": "C"
//        }
        //  Verify that send message to Registrar service for grade update

        // Try to update Joes grade to 'F'
        // Check that the response is "You are not authorized to update grades for this section."
    }

}