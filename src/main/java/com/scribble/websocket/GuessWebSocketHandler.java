package com.scribble.websocket;

import com.scribble.guess.GuessService;
import com.scribble.websocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GuessWebSocketHandler {

    private final GuessService guessService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomCode}/guess")
    public void handleGuess(
            @DestinationVariable String roomCode,
            @Payload ChatMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = headerAccessor.getSessionAttributes().get("playerId").toString();
        String username = headerAccessor.getSessionAttributes().get("username").toString();

        GuessService.GuessResult result = guessService.processGuess(
                roomCode, UUID.fromString(playerId), message.text());

        if (result.correct()) {
            // Broadcast correct guess notification
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomCode + "/guess-result",
                    Map.of(
                        "event", "correct_guess",
                        "playerId", playerId,
                        "username", username
                    ));
        } else {
            // Broadcast as regular chat message (don't reveal close guesses)
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomCode + "/chat",
                    Map.of(
                        "playerId", playerId,
                        "username", username,
                        "text", message.text(),
                        "timestamp", System.currentTimeMillis()
                    ));
        }
    }
}
