package com.eidd.dto;

import java.util.List;

public record GroupeDto(long id, List<EleveRemarquesDto> eleves) {
}
