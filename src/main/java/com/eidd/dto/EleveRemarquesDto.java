package com.eidd.dto;

import java.util.List;

public record EleveRemarquesDto(long id, String nom, String prenom, List<RemarqueDto> remarques) {
}
