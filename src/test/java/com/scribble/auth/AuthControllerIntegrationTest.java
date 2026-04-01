package com.scribble.auth;

import com.scribble.testconfig.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterAndLogin() {
        String unique = String.valueOf(System.nanoTime()).substring(8);
        String username = "integ" + unique;

        String registerBody = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"password123\"}",
                username, username);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> registerResp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(registerBody, headers),
                String.class);

        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).contains("\"success\":true");
        assertThat(registerResp.getBody()).contains("\"token\":");

        // Login
        String loginBody = String.format(
                "{\"username\":\"%s\",\"password\":\"password123\"}", username);
        ResponseEntity<String> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(loginBody, headers),
                String.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).contains("\"success\":true");
        assertThat(loginResp.getBody()).contains("\"token\":");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        String unique = String.valueOf(System.nanoTime()).substring(8);
        String username = "dupe" + unique;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"password123\"}",
                username, username);
        restTemplate.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(body, headers), String.class);

        String body2 = String.format(
                "{\"username\":\"%s\",\"email\":\"%s2@test.com\",\"password\":\"password123\"}",
                username, username);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(body2, headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Username already taken");
    }

    @Test
    void shouldRejectInvalidLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"username":"noexist","password":"wrong"}
                """;
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid username or password");
    }
}
