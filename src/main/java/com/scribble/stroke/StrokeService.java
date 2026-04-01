package com.scribble.stroke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StrokeService {

    private final StrokeRepository strokeRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Flush strokes from Redis buffer to PostgreSQL, then clear the buffer.
     */
    @Transactional
    public int flushStrokesToDatabase(String roomCode, UUID roundId) {
        String strokeKey = "room:" + roomCode + ":strokes";
        List<String> strokeJsonList = redisTemplate.opsForList().range(strokeKey, 0, -1);

        if (strokeJsonList == null || strokeJsonList.isEmpty()) {
            log.debug("No strokes to flush for room {} round {}", roomCode, roundId);
            return 0;
        }

        int order = 0;
        for (String strokeJson : strokeJsonList) {
            try {
                JsonNode node = objectMapper.readTree(strokeJson);

                Stroke stroke = new Stroke();
                stroke.setRoundId(roundId);
                stroke.setStrokeOrder(order++);
                stroke.setPointsData(node.has("points") ? node.get("points").toString() : "[]");
                stroke.setColor(node.has("color") ? node.get("color").asText() : "#000000");
                stroke.setBrushSize(node.has("brushSize") ? node.get("brushSize").asInt() : 5);
                stroke.setTool(node.has("tool") ? node.get("tool").asText() : "BRUSH");
                strokeRepository.save(stroke);
            } catch (Exception e) {
                log.warn("Failed to parse stroke JSON: {}", strokeJson, e);
            }
        }

        // Clear the Redis buffer
        redisTemplate.delete(strokeKey);

        log.info("Flushed {} strokes from Redis to PostgreSQL for room {} round {}",
                order, roomCode, roundId);
        return order;
    }

    /**
     * Replay strokes for reconnection — fetch from Redis buffer (current round)
     * or from PostgreSQL (completed rounds).
     */
    public List<String> getStrokesForReplay(String roomCode, UUID roundId) {
        // Try Redis first (active round)
        String strokeKey = "room:" + roomCode + ":strokes";
        List<String> redisStrokes = redisTemplate.opsForList().range(strokeKey, 0, -1);
        if (redisStrokes != null && !redisStrokes.isEmpty()) {
            return redisStrokes;
        }

        // Fall back to PostgreSQL (completed round)
        return strokeRepository.findByRoundIdOrderByStrokeOrder(roundId).stream()
                .map(s -> {
                    try {
                        return objectMapper.writeValueAsString(new StrokeDto(
                                s.getPointsData(), s.getColor(), s.getBrushSize(), s.getTool()));
                    } catch (Exception e) {
                        return "{}";
                    }
                })
                .toList();
    }

    private record StrokeDto(String points, String color, int brushSize, String tool) {}
}
