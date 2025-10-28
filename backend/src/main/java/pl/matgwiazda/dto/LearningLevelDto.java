package pl.matgwiazda.dto;

import java.time.Instant;
import java.util.UUID;

public record LearningLevelDto(
    short level,
    String title,
    String description,
    UUID createdBy,
    Instant createdAt,
    UUID modifiedBy,
    Instant modifiedAt
) {}

