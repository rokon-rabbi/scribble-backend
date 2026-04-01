package com.scribble.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_GUESSES_PER_SECOND = 2;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SEND.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.contains("/guess")) {
            return message;
        }

        var sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs == null || !sessionAttrs.containsKey("playerId")) {
            return message;
        }

        String playerId = sessionAttrs.get("playerId").toString();
        String rateLimitKey = "ratelimit:guess:" + playerId;

        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofSeconds(1));
        }

        if (count != null && count > MAX_GUESSES_PER_SECOND) {
            log.warn("Rate limit exceeded for player {} on guess endpoint", playerId);
            return null; // drop the message
        }

        return message;
    }
}
