package com.scribble.stroke;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StrokeRepository extends JpaRepository<Stroke, UUID> {

    List<Stroke> findByRoundIdOrderByStrokeOrder(UUID roundId);
}
