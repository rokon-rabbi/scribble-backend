package com.scribble.websocket;

import com.scribble.game.GameFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameFlowHandler {

    private final GameFlowService gameFlowService;

    @MessageMapping("/room/{roomCode}/start")
    public void startGame(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = headerAccessor.getSessionAttributes().get("playerId").toString();
        log.info("Start game requested in room {} by {}", roomCode, playerId);
        gameFlowService.startGame(roomCode, UUID.fromString(playerId));
    }

    @MessageMapping("/room/{roomCode}/choose-word")
    public void chooseWord(
            @DestinationVariable String roomCode,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = headerAccessor.getSessionAttributes().get("playerId").toString();
        UUID wordId = UUID.fromString(payload.get("wordId"));

        log.info("Word chosen in room {} by {}", roomCode, playerId);
        gameFlowService.onDrawerPickedWord(roomCode, UUID.fromString(playerId), wordId);
    }
}
