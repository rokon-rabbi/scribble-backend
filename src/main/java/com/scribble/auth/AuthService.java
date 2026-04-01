package com.scribble.auth;

import com.scribble.auth.dto.AuthResponse;
import com.scribble.auth.dto.LoginRequest;
import com.scribble.auth.dto.RegisterRequest;
import com.scribble.player.Player;
import com.scribble.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (playerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Player player = new Player();
        player.setUsername(request.username());
        player.setEmail(request.email());
        player.setPasswordHash(passwordEncoder.encode(request.password()));
        player = playerRepository.save(player);

        String token = jwtTokenProvider.generateToken(player.getId(), player.getUsername());
        return new AuthResponse(token, player.getId(), player.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        Player player = playerRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), player.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(player.getId(), player.getUsername());
        return new AuthResponse(token, player.getId(), player.getUsername());
    }
}
