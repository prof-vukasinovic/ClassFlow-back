package com.eidd.dto;

import java.util.List;

public record GroupeDto(long id, String nom, List<EleveRemarquesDto> eleves) {
}
