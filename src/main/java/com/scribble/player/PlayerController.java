package com.scribble.player;

import com.scribble.auth.PlayerPrincipal;
import com.scribble.common.dto.ApiResponse;
import com.scribble.player.dto.LeaderboardEntry;
import com.scribble.player.dto.PlayerProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Player profile and leaderboard")
@SecurityRequirement(name = "bearerAuth")
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/me")
    @Operation(summary = "Get current player's profile")
    public ApiResponse<PlayerProfile> getMyProfile(
            @AuthenticationPrincipal PlayerPrincipal principal) {
        return ApiResponse.success(playerService.getProfile(principal.getId()));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get global leaderboard")
    public ApiResponse<Page<LeaderboardEntry>> getLeaderboard(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(playerService.getLeaderboard(pageable));
    }
}
