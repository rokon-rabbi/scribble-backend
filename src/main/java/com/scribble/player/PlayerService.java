package com.scribble.player;

import com.scribble.common.exception.ResourceNotFoundException;
import com.scribble.player.dto.LeaderboardEntry;
import com.scribble.player.dto.PlayerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerProfile getProfile(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));
        return PlayerProfile.from(player);
    }

    public Page<LeaderboardEntry> getLeaderboard(Pageable pageable) {
        return playerRepository.findLeaderboard(pageable)
                .map(LeaderboardEntry::from);
    }
}
