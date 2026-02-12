package com.eidd.dto;

import java.util.List;

import com.eidd.model.Table;

public record ClassRoomRemarquesDto(long id, String nom, List<EleveRemarquesDto> eleves, List<Table> tables) {
}
