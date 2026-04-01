package com.scribble.stroke;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strokes")
@Getter
@Setter
@NoArgsConstructor
public class Stroke {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "stroke_order", nullable = false)
    private int strokeOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "points_data", nullable = false, columnDefinition = "jsonb")
    private String pointsData;

    @Column(nullable = false, length = 7)
    private String color = "#000000";

    @Column(name = "brush_size", nullable = false)
    private int brushSize = 5;

    @Column(nullable = false, length = 10)
    private String tool = "BRUSH";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
