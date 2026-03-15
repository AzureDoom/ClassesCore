package com.azuredoom.classescore.api.model;

import java.util.UUID;

public record PlayerClassState(
    UUID playerId,
    String classId,
    long createdAt,
    long updatedAt
) {}
