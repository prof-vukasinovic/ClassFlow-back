package com.eidd.dto;

import java.time.Instant;

public record RemarqueDto(long id, String intitule, Long eleveId, Long classRoomId, Instant createdAt) {
}
