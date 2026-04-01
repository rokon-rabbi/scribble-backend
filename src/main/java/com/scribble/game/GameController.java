package com.scribble.game;

import com.scribble.auth.PlayerPrincipal;
import com.scribble.common.dto.ApiResponse;
import com.scribble.game.dto.CreateRoomRequest;
import com.scribble.game.dto.RoomInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomInfo> createRoom(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @Valid @RequestBody CreateRoomRequest request) {
        RoomInfo room = gameService.createRoom(principal.getId(), request);
        return ApiResponse.success(room);
    }

    @GetMapping("/public")
    public ApiResponse<Page<RoomInfo>> listPublicRooms(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(gameService.listPublicRooms(pageable));
    }

    @GetMapping("/{roomCode}")
    public ApiResponse<RoomInfo> getRoomInfo(
            @PathVariable String roomCode) {
        return ApiResponse.success(gameService.getRoomInfo(roomCode));
    }
}
