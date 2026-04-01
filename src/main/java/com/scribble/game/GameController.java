package com.scribble.game;

import com.scribble.auth.PlayerPrincipal;
import com.scribble.common.dto.ApiResponse;
import com.scribble.game.dto.CreateRoomRequest;
import com.scribble.game.dto.RoomInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Games", description = "Room creation and listing")
@SecurityRequirement(name = "bearerAuth")
public class GameController {

    private final GameService gameService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new game room")
    public ApiResponse<RoomInfo> createRoom(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @Valid @RequestBody CreateRoomRequest request) {
        RoomInfo room = gameService.createRoom(principal.getId(), request);
        return ApiResponse.success(room);
    }

    @GetMapping("/public")
    @Operation(summary = "List public waiting rooms")
    public ApiResponse<Page<RoomInfo>> listPublicRooms(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(gameService.listPublicRooms(pageable));
    }

    @GetMapping("/{roomCode}")
    @Operation(summary = "Get room info by room code")
    public ApiResponse<RoomInfo> getRoomInfo(
            @PathVariable String roomCode) {
        return ApiResponse.success(gameService.getRoomInfo(roomCode));
    }
}
