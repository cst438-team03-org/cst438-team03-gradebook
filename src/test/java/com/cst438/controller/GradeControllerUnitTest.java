package com.cst438.controller;

import com.cst438.domain.Grade;
import com.cst438.domain.GradeRepository;
import com.cst438.dto.GradeDTO;
import com.cst438.dto.LoginDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GradeControllerUnitTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GradeRepository gradeRepository;

    @Test
    public void getAssignmentGradesTest() throws Exception {
        String email = "ted@csumb.edu";
        String password = "ted2025";
        // Login and get JWT
        EntityExchangeResult<LoginDTO> loginResult = webTestClient.get().uri("/login")
                .headers(h -> h.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();
        
        LoginDTO login = loginResult.getResponseBody();
        assertNotNull(login, "LoginDTO should not be null");
        String jwt = login.jwt();
        assertNotNull(jwt, "JWT token should not be null");

        // GET /assignments/1/grades
        EntityExchangeResult<GradeDTO[]> result = webTestClient.get()
                .uri("/assignments/1/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class)
                .returnResult();

        GradeDTO[] grades = result.getResponseBody();
        assertNotNull(grades, "Grades response should not be null");
        assertTrue(grades.length > 0, "There should be at least one grade for assignment 1");
    }

    @Test
    public void updateGradeTest() throws Exception {
        // Login and get JWT
        String email = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> loginResult = webTestClient.get().uri("/login")
                .headers(h -> h.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        LoginDTO login = loginResult.getResponseBody();
        assertNotNull(login, "LoginDTO should not be null");
        String jwt = login.jwt();
        assertNotNull(jwt, "JWT token should not be null");

        // GET existing grades
        GradeDTO[] grades = webTestClient.get()
                .uri("/assignments/1/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(grades);
        assertTrue(grades.length > 0);

        // Pick first grade, compute a new score
        GradeDTO original = grades[0];
        int newScore = ((original.score() == null ? 0 : original.score()) + 10) % 101;

        GradeDTO updatedDto = new GradeDTO(
                original.gradeId(),
                original.studentName(),
                original.studentEmail(),
                original.assignmentTitle(),
                original.courseId(),
                original.sectionId(),
                newScore
        );

        // PUT /grades
        webTestClient.put().uri("/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(updatedDto))
                .exchange()
                .expectStatus().isOk();

        // Confirm via endpoint
        GradeDTO[] after = webTestClient.get()
                .uri("/assignments/1/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class)
                .returnResult()
                .getResponseBody();

        GradeDTO found = null;
        for (GradeDTO g : after) {
            if (g.gradeId() == original.gradeId()) {
                found = g;
                break;
            }
        }
        assertNotNull(found, "Updated grade should be present");
        assertEquals(newScore, found.score(), "Grade score should be updated in response");

        // Confirm in repository
        Grade persisted = gradeRepository.findById(original.gradeId()).orElse(null);
        assertNotNull(persisted, "Grade must still exist in DB");
        assertEquals(newScore, persisted.getScore().intValue(), "Grade score should be updated in DB");
    }
}
