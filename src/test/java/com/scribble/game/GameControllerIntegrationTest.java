package com.scribble.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scribble.testconfig.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class GameControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Register a unique user for each test
        String username = "gametest" + System.nanoTime();
        String body = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"password123\"}",
                username, username);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(body, headers),
                String.class);

        JsonNode json = objectMapper.readTree(resp.getBody());
        token = json.get("data").get("token").asText();
    }

    @Test
    void shouldCreateRoom() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String body = "{\"maxPlayers\":8,\"roundsPerPlayer\":2,\"turnTimeSeconds\":60}";

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/games",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = objectMapper.readTree(resp.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("roomCode").asText()).hasSize(6);
        assertThat(json.get("data").get("status").asText()).isEqualTo("WAITING");
        assertThat(json.get("data").get("currentPlayerCount").asInt()).isEqualTo(1);
    }

    @Test
    void shouldListPublicRooms() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Create a room first
        String body = "{\"maxPlayers\":8,\"roundsPerPlayer\":2,\"turnTimeSeconds\":60}";
        restTemplate.postForEntity("/api/v1/games",
                new HttpEntity<>(body, headers), String.class);

        // List public rooms
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/games/public",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(resp.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("content").isArray()).isTrue();
    }

    @Test
    void shouldRejectUnauthenticatedRoomCreation() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"maxPlayers\":8,\"roundsPerPlayer\":2,\"turnTimeSeconds\":60}";
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/games",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
